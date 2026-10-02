package nightshift.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import nightshift.Nightshift;

public final class NightshiftEntities {
    public static final EntityType<Understudy> UNDERSTUDY = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            Nightshift.id("understudy"),
            EntityType.Builder.of(Understudy::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .eyeHeight(1.62F)
                    .noLootTable()
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Nightshift.id("understudy"))));

    private NightshiftEntities() {
    }

    public static void register() {
        FabricDefaultAttributeRegistry.register(UNDERSTUDY, Understudy.createMobAttributes());
    }
}
