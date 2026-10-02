package nightshift.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import nightshift.Nightshift;
import nightshift.effect.NightshiftEffects;
import nightshift.entity.Understudy;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.server.permissions.LevelBasedPermissionSet;

public final class CommandClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().adjustSettings(settings -> {
            settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            settings.setAllowCommands(true);
        }).create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("tp @a 0.5 -60 0.5 0 0");
            context.waitTicks(40);
            world.getServer().runOnServer(server -> {
                var source = server.getPlayerList().getPlayers().getFirst().createCommandSourceStack();
                var root = server.getCommands().getDispatcher().getRoot().getChild("nightshift");
                check(!root.canUse(source.withPermission(net.minecraft.server.permissions.PermissionSet.NO_PERMISSIONS)), "ordinary players cannot use test commands");
                check(root.canUse(source.withPermission(LevelBasedPermissionSet.GAMEMASTER)), "operators can use test commands");
            });
            command(world, "watch");
            context.waitTicks(5);
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                var entities = player.level().getEntitiesOfClass(Understudy.class, player.getBoundingBox().inflate(96));
                check(entities.size() == 1, "watch creates one sighting");
                var direction = entities.getFirst().position().subtract(player.position()).normalize();
                check(player.getLookAngle().dot(direction) > 0.9, "test sighting is ahead of player");
            });
            command(world, "watch");
            context.waitTicks(5);
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                check(player.level().getEntitiesOfClass(Understudy.class, player.getBoundingBox().inflate(96)).size() == 1,
                        "repeat watch replaces previous sighting");
            });
            verifyLookBackSequence(context, world);
            command(world, "effects");
            context.waitFor(client -> client.player.hasEffect(NightshiftEffects.HOLLOW) && client.player.hasEffect(NightshiftEffects.SEEN), 60);
            command(world, "clear");
            context.waitFor(client -> !client.player.hasEffect(NightshiftEffects.HOLLOW) && !client.player.hasEffect(NightshiftEffects.SEEN), 60);
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                check(player.level().getEntitiesOfClass(Understudy.class, player.getBoundingBox().inflate(96)).isEmpty(), "clear removes test sightings");
            });
            Nightshift.LOGGER.info("In-world command test: all permission, watch, repeat, effects, and clear checks passed");
        }
    }

    private static void verifyLookBackSequence(ClientGameTestContext context, TestSingleplayerContext world) {
        int entityId = world.getServer().computeOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            return player.level().getEntitiesOfClass(Understudy.class, player.getBoundingBox().inflate(96)).getFirst().getId();
        });
        context.waitFor(client -> client.level.getEntity(entityId) != null, 60);
        context.getInput().lookAt(0, 0);
        context.waitTicks(10);
        var visiblePosition = context.computeOnClient(client -> client.level.getEntity(entityId).position());
        context.waitTicks(20);
        context.runOnClient(client -> check(client.level.getEntity(entityId).position().distanceTo(visiblePosition) < 0.01,
                "client sees no movement while watching"));

        // Return to the original view, rather than aiming at the entity's server position.
        context.getInput().lookAt(180, 0);
        context.waitTicks(5);
        context.waitTicks(160);
        context.getInput().lookAt(0, 0);
        context.waitTicks(10);
        context.runOnClient(client -> {
            var entity = client.level.getEntity(entityId);
            check(entity != null, "ordinary look-back keeps the entity visible");
            check(entity.position().distanceTo(client.player.position()) < visiblePosition.distanceTo(client.player.position()) - 1,
                    "client sees it closer after looking away");
        });
        context.takeScreenshot("watch-command-after-short-look-away");

        context.getInput().lookAt(180, 0);
        context.waitTicks(5);
        context.waitTicks(200);
        context.runOnClient(client -> check(client.level.getEntity(entityId) != null,
                "entity remains alive through ten seconds unseen"));
        context.getInput().lookAt(0, 0);
        context.waitTicks(10);
        context.runOnClient(client -> check(client.level.getEntity(entityId) != null,
                "long look-back remains an ordinary encounter without the retired jump scare"));
        context.takeScreenshot("watch-command-quiet-long-look-back");
        Nightshift.LOGGER.info("In-world regression: watch command, client freeze/movement, and quiet look-back passed");
    }

    private static void command(TestSingleplayerContext world, String action) {
        world.getServer().runOnServer(server -> check(execute(server, action) == 1, "command succeeds: " + action));
    }

    private static int execute(net.minecraft.server.MinecraftServer server, String action) {
        var player = server.getPlayerList().getPlayers().getFirst();
        try {
            return server.getCommands().getDispatcher().execute("nightshift test " + action,
                    player.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER));
        } catch (CommandSyntaxException exception) { throw new AssertionError(exception); }
    }

    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
    }
}
