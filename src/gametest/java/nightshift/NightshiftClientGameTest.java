package nightshift;

import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.phys.Vec3;
import nightshift.client.render.UnderstudyRenderer;
import nightshift.encounter.PlayerState;
import nightshift.encounter.WorldPhase;
import nightshift.entity.NightshiftEntities;
import nightshift.entity.Understudy;
import nightshift.world.NightshiftSavedData;

/** Runs in a real client and integrated server, then reopens the same world. */
public final class NightshiftClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        UUID otherPlayer = UUID.randomUUID();
        UUID playerId;
        UUID entityId;
        TestWorldSave save;
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            save = world.getWorldSave();
            playerId = world.getServer().computeOnServer(server -> world.getConnection().getServerPlayer().getUUID());
            world.getServer().runOnServer(server -> {
                boolean[] completed = {false};
                var source = world.getConnection().getServerPlayer().createCommandSourceStack()
                        .withPermission(LevelBasedPermissionSet.GAMEMASTER)
                        .withCallback((success, result) -> {
                            check(success, "Zero anger query must still report command success");
                            check(result == 0, "Anger query result must contain the anger value");
                            completed[0] = true;
                        });
                server.getCommands().performPrefixedCommand(source, "nightshift anger get");
                check(completed[0], "Anger query must invoke its result callback");
                var operator = world.getConnection().getServerPlayer().createCommandSourceStack()
                        .withPermission(LevelBasedPermissionSet.GAMEMASTER);
                server.getCommands().performPrefixedCommand(operator,
                        "execute store success storage nightshift:command_test success int 1 run nightshift anger get");
                server.getCommands().performPrefixedCommand(operator,
                        "execute store result storage nightshift:command_test result int 1 run nightshift anger get");
                var stored = server.getCommandStorage().get(Nightshift.id("command_test"));
                check(stored.getIntOr("success", -1) == 1, "Zero anger query must store success 1");
                check(stored.getIntOr("result", -1) == 0, "Zero anger query must store result 0");
            });
            world.getServer().runCommand("fill -5 -61 -5 5 -61 10 minecraft:stone");
            world.getServer().runCommand("tp @a 0 -60 0 0 0");
            world.getServer().runCommand("execute rotated 180 0 run nightshift spawn 0 -60 4");
            world.getServer().runCommand("nightshift phase set watching");
            String playerName = world.getServer().computeOnServer(server -> world.getConnection().getServerPlayer().getScoreboardName());
            world.getServer().runCommand("execute as " + playerName + " run nightshift anger set 5");
            world.getServer().runCommand("execute as " + playerName + " run nightshift anger get");
            world.getServer().runCommand("nightshift anger set 23 " + playerName);
            world.getServer().runCommand("nightshift phase");
            world.getServer().runOnServer(server -> {
                NightshiftSavedData data = NightshiftSavedData.get(server);
                check(data.playerState(playerId).anger() == 23, "Anger command must update the target player");
                check(data.phase() == WorldPhase.WATCHING, "Phase command must update the world");
                data.setPlayerState(playerId, new PlayerState(23, 2, true));
                data.setPlayerState(otherPlayer, new PlayerState(4, 1, false));
            });
            entityId = world.getServer().computeOnServer(server -> {
                var entities = server.overworld().getEntities(NightshiftEntities.UNDERSTUDY, entity -> true);
                check(entities.size() == 1, "Spawn command must create one Understudy");
                Understudy entity = entities.getFirst();
                check(entity.isNoAi() && entity.isSilent() && entity.isPermanentlyInvulnerable(), "Checkpoint entity flags");
                return entity.getUUID();
            });
            world.getConnection().waitForClientboundEntityUpdates(NightshiftEntities.UNDERSTUDY);
            context.runOnClient(client -> {
                Understudy entity = null;
                for (Entity candidate : world.getConnection().getClientLevel().entitiesForRendering()) {
                    if (candidate instanceof Understudy understudy && candidate.getUUID().equals(entityId)) {
                        entity = understudy;
                        break;
                    }
                }
                check(entity != null, "Spawned entity must reach the client");
                var renderer = client.getEntityRenderDispatcher().getRenderer(entity);
                check(renderer instanceof UnderstudyRenderer, "Entity must use the Understudy renderer");
                var renderState = ((UnderstudyRenderer) renderer).createRenderState(entity, 0);
                check(client.getEntityRenderDispatcher().getRenderer(renderState) instanceof UnderstudyRenderer,
                        "Render state must also dispatch to the Understudy renderer");
            });
            world.getConnection().waitForChunksRender();
            context.getInput().lookAt(0, 0);
            context.waitTicks(40);
            world.getServer().runOnServer(server -> {
                Entity entity = server.overworld().getEntityInAnyDimension(entityId);
                check(entity != null && entity.position().distanceTo(new Vec3(0.5, -60, 4.5)) < 0.01,
                        "Understudy must remain stationary");
            });
            context.takeScreenshot("understudy");
        }
        try (TestSingleplayerContext reopened = save.open()) {
            reopened.getServer().runOnServer(server -> {
                NightshiftSavedData data = NightshiftSavedData.get(server);
                check(data.phase() == WorldPhase.WATCHING, "World phase must survive restart");
                check(data.playerState(playerId).equals(new PlayerState(23, 2, true)), "All player fields must survive restart");
                check(data.playerState(otherPlayer).equals(new PlayerState(4, 1, false)), "Other UUID must remain independent");
            });
            reopened.getServer().waitFor(server -> server.overworld().getEntityInAnyDimension(entityId) != null);
            reopened.getConnection().waitForClientboundEntityUpdates(NightshiftEntities.UNDERSTUDY);
            context.waitTicks(10);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
