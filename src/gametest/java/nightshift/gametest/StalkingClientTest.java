package nightshift.gametest;

import java.lang.reflect.Field;
import java.util.UUID;
import nightshift.Nightshift;
import nightshift.client.effect.EncounterPresentation;
import nightshift.effect.NightshiftEffects;
import nightshift.entity.NightshiftEntities;
import nightshift.entity.Understudy;
import nightshift.world.EncounterSavedData;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Drives a real client and integrated server in an isolated, flat test world. */
public final class StalkingClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        TestWorldSave save;
        EncounterSavedData.Deadlines saved;
        try (var world = context.worldBuilder().adjustSettings(settings -> {
            settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            settings.setAllowCommands(true);
        }).create()) {
            save = world.getWorldSave();
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("tp @a 0.5 -60 0.5");
            context.waitTicks(40);
            world.getConnection().waitForChunksRender();
            var initial = deadlines(world);
            long now = world.getServer().computeOnServer(server -> server.overworld().getGameTime());
            check(initial.encounter() - now >= 3500 && initial.encounter() - now <= 6000, "initial 3–5 minute schedule");
            log("Waiting for the naturally scheduled first sighting");
            UUID first = waitForEncounter(context, world, 6500);
            lookAt(context, world, first);
            context.waitFor(client -> client.player.hasEffect(NightshiftEffects.HOLLOW)
                    && client.player.hasEffect(NightshiftEffects.SEEN), 100);
            var still = position(world, first);
            context.takeScreenshot("understudy-watching");
            context.waitTicks(30);
            check(position(world, first).distanceTo(still) < 0.01, "visible entity remains stationary");
            context.runOnClient(client -> {
                check(client.player.getEffect(NightshiftEffects.HOLLOW).getAmplifier() == 0, "Hollow does not stack");
                check(client.player.getEffect(NightshiftEffects.SEEN).getAmplifier() == 0, "Seen does not stack");
                check(value("worldGain", Float.class) == 0.25f, "Hollow reduces world audio gain");
            });
            long remaining = deadlines(world).encounter() - world.getServer().computeOnServer(server -> server.overworld().getGameTime());
            check(remaining >= 2300 && remaining <= 3600, "Seen shortens the next sighting");
            lookAway(context);
            context.waitTicks(30);
            var moved = position(world, first);
            check(moved.distanceTo(still) > 1, "entity moves while unseen");
            lookAt(context, world, first);
            context.waitTicks(10);
            context.takeScreenshot("understudy-after-unseen-movement");
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                var entity = server.overworld().getEntity(first);
                player.teleportTo(entity.getX() + 5, entity.getY(), entity.getZ());
            });
            world.getServer().waitFor(server -> server.overworld().getEntity(first) == null, 60);
            log("PASS: natural sighting, Seen cadence, visible freeze, unseen movement, approach vanish");

            resetPosition(context, world);
            forceEncounter(world);
            UUID timeout = waitForEncounter(context, world, 100);
            lookAt(context, world, timeout);
            context.waitTicks(100);
            check(position(world, timeout) != null, "sighting persists before timeout");
            world.getServer().waitFor(server -> server.overworld().getEntity(timeout) == null, 650);
            log("PASS: sighting timeout");
            context.waitFor(client -> !client.player.hasEffect(NightshiftEffects.HOLLOW), 650);
            // Effect packets and the END_CLIENT_TICK presentation callback can land in adjacent ticks.
            context.waitFor(client -> value("worldGain", Float.class) == 1, 10);
            context.waitFor(client -> !client.player.hasEffect(NightshiftEffects.SEEN), 1900);
            log("PASS: effects expire and audio gain restores");

            saved = deadlines(world);
        }
        context.waitFor(client -> value("worldGain", Float.class) == 1, 10);
        try (var reopened = save.open()) {
            reopened.getConnection().waitForChunksDownload();
            check(deadlines(reopened).equals(saved), "world reload preserves both saved deadlines");
            check(reopened.getServer().computeOnServer(server -> entities(server).isEmpty()), "temporary entities are not saved");
            context.takeScreenshot("understudy-reloaded-calm-gap");
            log("PASS: real world save/reload preserves deadlines and removes temporary entities");
        }
        log("ALL CLIENT-WORLD CHECKS PASSED");
    }

    private static java.util.List<Understudy> entities(MinecraftServer server) {
        return server.overworld().getEntitiesOfClass(Understudy.class, new AABB(-100, -70, -100, 100, 0, 100));
    }

    private static EncounterSavedData.Deadlines deadlines(TestSingleplayerContext world) {
        return world.getServer().computeOnServer(server -> server.getDataStorage().computeIfAbsent(EncounterSavedData.TYPE)
                .get(server.getPlayerList().getPlayers().getFirst().getUUID()));
    }

    private static UUID waitForEncounter(ClientGameTestContext context, TestSingleplayerContext world, int ticks) {
        world.getServer().waitFor(server -> !entities(server).isEmpty(), ticks);
        UUID id = world.getServer().computeOnServer(server -> entities(server).getFirst().getUUID());
        world.getConnection().waitForClientboundEntityUpdates(NightshiftEntities.UNDERSTUDY);
        return id;
    }

    private static Vec3 position(TestSingleplayerContext world, UUID entity) {
        return world.getServer().computeOnServer(server -> {
            var found = server.overworld().getEntity(entity);
            return found == null ? null : found.position();
        });
    }

    private static void lookAt(ClientGameTestContext context, TestSingleplayerContext world, UUID entity) {
        var position = position(world, entity);
        check(position != null, "entity exists before looking at it");
        context.getInput().lookAt(BlockPos.containing(position.add(0, 1.4, 0)));
        context.waitTicks(5);
        world.getConnection().waitForServerboundPackets();
    }

    private static void lookAway(ClientGameTestContext context) {
        float yaw = context.computeOnClient(client -> client.player.getYRot());
        context.getInput().lookAt(yaw + 180, 0);
        context.waitTicks(5);
    }

    private static void resetPosition(ClientGameTestContext context, TestSingleplayerContext world) {
        world.getServer().runCommand("tp @a 0.5 -60 0.5");
        context.waitTicks(10);
    }

    private static void forceEncounter(TestSingleplayerContext world) {
        world.getServer().runOnServer(server -> {
            for (var entity : entities(server)) entity.discard();
            var player = server.getPlayerList().getPlayers().getFirst();
            var data = server.getDataStorage().computeIfAbsent(EncounterSavedData.TYPE);
            var previous = data.get(player.getUUID());
            long now = server.overworld().getGameTime();
            data.set(player.getUUID(), now, previous.scare());
        });
    }

    private static <T> T value(String name, Class<T> type) {
        try {
            Field field = EncounterPresentation.class.getDeclaredField(name);
            field.setAccessible(true);
            return type.cast(field.get(null));
        } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    }

    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
    }

    private static void log(String message) { Nightshift.LOGGER.info("In-world test: {}", message); }
}
