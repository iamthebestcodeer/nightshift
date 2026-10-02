package nightshift.client.effect;

import nightshift.Nightshift;
import nightshift.effect.NightshiftEffects;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.ResourceReloaderKeys;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Local audio gains never change the player's saved volume preferences. */
public final class EncounterPresentation {
    private static int phantomTicks;
    private static int heartbeatTicks;
    private static float worldGain = 1;
    private static SimpleSoundInstance phantom;
    private static SimpleSoundInstance heartbeat;

    private EncounterPresentation() {}

    /** Registers audio ticks and invalidates cached gain after sound resources reload. */
    public static void initialize() {
        var resources = ResourceLoader.get(PackType.CLIENT_RESOURCES);
        var audioReload = Nightshift.id("encounter_audio");
        resources.registerReloadListener(audioReload, (ResourceManagerReloadListener) manager -> invalidateGain());
        resources.addListenerOrdering(ResourceReloaderKeys.Client.SOUNDS, audioReload);
        ClientTickEvents.END_CLIENT_TICK.register(EncounterPresentation::tick);
    }

    /** Updates Hollow gain and effect cues, restoring audio when the client leaves a world. */
    private static void tick(Minecraft client) {
        if (client.player == null || client.level == null) {
            stopCues(client);
            phantomTicks = heartbeatTicks = 0;
            applyGain(client, 1);
            return;
        }
        boolean hollow = client.player.hasEffect(NightshiftEffects.HOLLOW);
        applyGain(client, hollow ? 0.25f : 1);
        if (client.isPaused()) return;
        if (hollow) {
            if (--phantomTicks <= 0) {
                var random = client.player.getRandom();
                double angle = random.nextDouble() * Math.PI * 2;
                var position = client.player.position().add(Math.cos(angle) * 7, 1, Math.sin(angle) * 7);
                if (phantom != null) client.getSoundManager().stop(phantom);
                phantom = new SimpleSoundInstance(SoundEvents.ENDERMAN_STARE, SoundSource.UI,
                        0.18f, 0.65f, random, position.x, position.y, position.z);
                client.getSoundManager().play(phantom);
                phantomTicks = 100 + random.nextInt(81);
            }
        } else {
            phantomTicks = 60;
            if (phantom != null) {
                client.getSoundManager().stop(phantom);
                phantom = null;
            }
        }
        if (client.player.hasEffect(NightshiftEffects.SEEN)) {
            if (--heartbeatTicks <= 0) {
                heartbeat = SimpleSoundInstance.forUI(SoundEvents.WARDEN_HEARTBEAT, 0.8f, 0.15f);
                client.getSoundManager().play(heartbeat);
                heartbeatTicks = 100;
            }
        } else heartbeatTicks = 0;
    }

    /** Stops the tracked phantom and heartbeat sounds and clears their references. */
    private static void stopCues(Minecraft client) {
        if (phantom != null) client.getSoundManager().stop(phantom);
        if (heartbeat != null) client.getSoundManager().stop(heartbeat);
        phantom = heartbeat = null;
    }

    /** Forces the next client tick to reapply category gain after an audio reset. */
    public static void invalidateGain() { worldGain = Float.NaN; }

    /** Updates world sound categories only when gain changes, leaving master and UI gain untouched. */
    private static void applyGain(Minecraft client, float gain) {
        if (worldGain == gain) return;
        worldGain = gain;
        for (SoundSource source : SoundSource.values()) {
            if (source != SoundSource.MASTER && source != SoundSource.UI) {
                client.getSoundManager().updateCategoryVolume(source, gain);
            }
        }
    }

}
