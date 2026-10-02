package nightshift.effect;

import nightshift.Nightshift;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public final class NightshiftEffects {
    public static final Holder<MobEffect> HOLLOW = register("hollow", 0x81858a);
    public static final Holder<MobEffect> SEEN = register("seen", 0xb8b3a2);

    private NightshiftEffects() {}
    public static void initialize() {}

    private static Holder<MobEffect> register(String name, int color) {
        return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Nightshift.id(name), new QuietEffect(color));
    }

    private static final class QuietEffect extends MobEffect {
        private QuietEffect(int color) { super(MobEffectCategory.HARMFUL, color); }
    }
}
