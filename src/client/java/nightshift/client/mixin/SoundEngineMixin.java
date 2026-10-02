package nightshift.client.mixin;

import nightshift.client.effect.EncounterPresentation;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** stopAll resets category gains during both world changes and sound-device reloads. */
@Mixin(SoundEngine.class)
abstract class SoundEngineMixin {
    @Inject(method = "stopAll", at = @At("RETURN"))
    private void nightshift$invalidateGain(CallbackInfo callback) {
        EncounterPresentation.invalidateGain();
    }
}
