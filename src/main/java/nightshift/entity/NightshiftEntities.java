package nightshift.entity;

import nightshift.Nightshift;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;

public final class NightshiftEntities {
    public static final EntityType<Understudy> UNDERSTUDY = Registry.register(BuiltInRegistries.ENTITY_TYPE,
            Nightshift.id("understudy"), EntityType.Builder.of(Understudy::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f).clientTrackingRange(64).updateInterval(1).eyeHeight(1.62f).noLootTable().fireImmune()
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Nightshift.id("understudy"))));

    public static final EntityType<Apparition> APPARITION = Registry.register(BuiltInRegistries.ENTITY_TYPE,
            Nightshift.id("apparition"), EntityType.Builder.of(Apparition::new, MobCategory.MISC)
                    .sized(0.1f, 0.1f).clientTrackingRange(96).noSave()
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Nightshift.id("apparition"))));

    private NightshiftEntities() {}
    public static void initialize() {
        FabricDefaultAttributeRegistry.register(UNDERSTUDY, Mob.createMobAttributes());
    }
}
