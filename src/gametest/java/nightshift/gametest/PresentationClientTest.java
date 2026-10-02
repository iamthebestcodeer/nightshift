package nightshift.gametest;

import java.lang.reflect.Field;
import nightshift.Nightshift;
import nightshift.client.effect.EncounterPresentation;
import nightshift.effect.NightshiftEffects;
import nightshift.entity.NightshiftEntities;
import nightshift.entity.Understudy;
import nightshift.world.EncounterSavedData;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/** Real rendering/audio/resource-reload and dimension checks in a separate world. */
public final class PresentationClientTest implements FabricClientGameTest {
    /** Checks effect icons, actual audio gains and reloads, independent head turns, and dimension cleanup in a client world. */
    @Override public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().adjustSettings(settings -> {
            settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            settings.setAllowCommands(true);
        }).create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("tp @a 0.5 -60 0.5");
            context.waitTicks(20);
            float preference = context.computeOnClient(client -> client.options.getFinalSoundSourceVolume(SoundSource.BLOCKS));
            context.runOnClient(client -> {
                for (String name : new String[]{"hollow", "seen"}) {
                    Identifier icon = Nightshift.id("mob_effect/" + name);
                    var sprite = client.getAtlasManager().getAtlasOrThrow(AtlasIds.GUI).getSprite(icon);
                    check(sprite.contents().name().equals(icon), "effect icon resolves: " + name);
                }
            });
            world.getServer().runCommand("effect give @a nightshift:hollow 60");
            world.getServer().runCommand("effect give @a nightshift:seen 90");
            context.waitFor(client -> value("worldGain", Float.class) == 0.25f, 60);
            context.waitFor(client -> {
                var phantom = value("phantom", SimpleSoundInstance.class);
                return phantom != null && client.getSoundManager().isActive(phantom);
            }, 220);
            context.runOnClient(client -> {
                var phantom = value("phantom", SimpleSoundInstance.class);
                double dx = phantom.getX() - client.player.getX();
                double dz = phantom.getZ() - client.player.getZ();
                check(Math.abs(Math.hypot(dx, dz) - 7) < 0.01, "phantom has an empty-direction sound position");
                check(client.options.getFinalSoundSourceVolume(SoundSource.BLOCKS) == preference, "Hollow preserves saved volume preferences");
            });
            var reload = context.computeOnClient(client -> client.reloadResourcePacks());
            context.waitFor(client -> reload.isDone(), 500);
            reload.join();
            context.waitFor(client -> engineGain(client, SoundSource.BLOCKS) == 0.25f, 100);
            context.runOnClient(client -> client.getSoundManager().stop());
            context.waitFor(client -> engineGain(client, SoundSource.BLOCKS) == 0.25f, 40);
            context.runOnClient(client -> client.getSoundManager().reload());
            context.waitFor(client -> engineGain(client, SoundSource.BLOCKS) == 0.25f, 40);
            log("PASS: sound-engine stop and device-style reload restore actual Hollow category gain");
            log("PASS: valid effect icons, positional sound playback, unchanged volume settings, resource reload during Hollow");

            var id = world.getServer().computeOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                server.getDataStorage().computeIfAbsent(EncounterSavedData.TYPE).set(player.getUUID(), Long.MAX_VALUE, Long.MAX_VALUE);
                Understudy entity = new Understudy(NightshiftEntities.UNDERSTUDY, server.overworld());
                entity.setPos(0.5, -60, 20.5);
                entity.watch(player);
                server.overworld().addFreshEntity(entity);
                return entity.getUUID();
            });
            context.getInput().lookAt(new BlockPos(0, -59, 20));
            context.waitTicks(20);
            world.getServer().waitFor(server -> ((Understudy) server.overworld().getEntity(id)).tickCount >= 10, 60);
            float initialHead = world.getServer().computeOnServer(server -> ((Understudy) server.overworld().getEntity(id)).yHeadRot);
            float body = world.getServer().computeOnServer(server -> server.overworld().getEntity(id).getYRot());
            world.getServer().runCommand("tp @a 10.5 -60 0.5");
            context.getInput().lookAt(new BlockPos(0, -59, 20));
            context.waitTicks(20);
            world.getServer().waitFor(server -> {
                Understudy entity = (Understudy) server.overworld().getEntity(id);
                return entity != null && Math.abs(entity.yHeadRot - initialHead) > 15;
            }, 60);
            world.getServer().runOnServer(server -> {
                Understudy entity = (Understudy) server.overworld().getEntity(id);
                check(Math.abs(entity.yHeadRot - initialHead) > 15, "head turns toward displaced player");
                check(entity.getYRot() == body, "head turns independently of body");
            });
            context.takeScreenshot("understudy-head-turn-and-effect-icons");
            var beforeDimension = world.getServer().computeOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                return server.getDataStorage().computeIfAbsent(EncounterSavedData.TYPE).get(player.getUUID());
            });
            world.getServer().runCommand("execute in minecraft:the_nether run tp @a 0.5 80 0.5");
            context.waitFor(client -> client.level != null && client.level.dimension().equals(Level.NETHER), 600);
            world.getServer().waitFor(server -> server.overworld().getEntity(id) == null, 60);
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                var after = server.getDataStorage().computeIfAbsent(EncounterSavedData.TYPE).get(player.getUUID());
                check(after.equals(beforeDimension), "dimension changes preserve encounter deadlines");
            });
            context.waitFor(client -> client.player.hasEffect(NightshiftEffects.HOLLOW)
                    && engineGain(client, SoundSource.BLOCKS) == 0.25f, 100);
            world.getServer().runCommand("effect clear @a nightshift:hollow");
            context.waitFor(client -> !client.player.hasEffect(NightshiftEffects.HOLLOW) && engineGain(client, SoundSource.BLOCKS) == 1, 100);
            log("PASS: independent head turning, dimension retreat, unchanged deadlines, audio restoration");
        }
        context.waitFor(client -> value("worldGain", Float.class) == 1, 20);
    }

    /** Reads the sound engine's actual category gain via reflection, failing the test if the field is unavailable. */
    private static float engineGain(net.minecraft.client.Minecraft client, SoundSource source) {
        try {
            Field engineField = client.getSoundManager().getClass().getDeclaredField("soundEngine");
            engineField.setAccessible(true);
            Object engine = engineField.get(client.getSoundManager());
            Field gainsField = engine.getClass().getDeclaredField("gainBySource");
            gainsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            var gains = (it.unimi.dsi.fastutil.objects.Object2FloatMap<SoundSource>) gainsField.get(engine);
            return gains.getFloat(source);
        } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    }

    /** Reads a named static presentation field via reflection and casts it to the expected test type. */
    private static <T> T value(String name, Class<T> type) {
        try {
            Field field = EncounterPresentation.class.getDeclaredField(name);
            field.setAccessible(true);
            return type.cast(field.get(null));
        } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    }

    /** Throws an assertion failure with the supplied message when the tested condition is false. */
    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
    }

    /** Writes a labeled progress message to the in-world test log. */
    private static void log(String message) { Nightshift.LOGGER.info("In-world presentation test: {}", message); }
}
