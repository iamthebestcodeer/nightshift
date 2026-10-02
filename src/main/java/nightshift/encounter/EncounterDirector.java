package nightshift.encounter;

import nightshift.effect.NightshiftEffects;
import nightshift.entity.NightshiftEntities;
import nightshift.entity.Understudy;
import nightshift.world.EncounterSavedData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

public final class EncounterDirector {
    private EncounterDirector() {}

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(EncounterDirector::tick);
    }

    private static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        if (now % 20 != 0) return;
        var data = server.getDataStorage().computeIfAbsent(EncounterSavedData.TYPE);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isSpectator()) continue;
            var state = data.get(player.getUUID());
            boolean seen = player.hasEffect(NightshiftEffects.SEEN);
            int delay = EncounterRules.encounterDelay(seen, player.getRandom().nextDouble());
            if (state == null) {
                // Reserve the initial calm gap when first joining a new world.
                data.set(player.getUUID(), now + delay, 0);
            } else if (seen && state.encounter() > now + 180 * 20) {
                data.set(player.getUUID(), now + delay, state.scare());
            } else if (now >= state.encounter()) {
                if (spawn(player)) {
                    // Reserve the complete calm gap now, so reloads cannot reset it.
                    data.set(player.getUUID(), now + delay, state.scare());
                } else {
                    data.set(player.getUUID(), now + 20 * 20, state.scare());
                }
            }
        }
    }

    /** Explicit test action: place a watching entity ahead of the player. */
    public static boolean startTestEncounter(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || !spawn(player, true)) return false;
        var server = player.level().getServer();
        long now = server.overworld().getGameTime();
        var data = server.getDataStorage().computeIfAbsent(EncounterSavedData.TYPE);
        int delay = EncounterRules.encounterDelay(player.hasEffect(NightshiftEffects.SEEN), player.getRandom().nextDouble());
        var previous = data.get(player.getUUID());
        data.set(player.getUUID(), now + delay, previous == null ? 0 : previous.scare());
        return true;
    }

    private static boolean spawn(ServerPlayer player) { return spawn(player, false); }

    private static boolean spawn(ServerPlayer player, boolean ahead) {
        ServerLevel world = (ServerLevel) player.level();
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = ahead
                    ? Math.atan2(player.getLookAngle().z, player.getLookAngle().x) + (attempt % 3 - 1) * 0.15
                    : player.getRandom().nextDouble() * Math.PI * 2;
            int distance = ahead ? 24 + attempt : 24 + player.getRandom().nextInt(13);
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
            if (!world.hasChunkAt(new BlockPos(x, player.getBlockY(), z))) continue;
            int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (Math.abs(y - player.getY()) > 12) continue;
            Understudy entity = new Understudy(NightshiftEntities.UNDERSTUDY, world);
            entity.setPos(x + 0.5, y, z + 0.5);
            if (!world.getWorldBorder().isWithinBounds(entity.blockPosition())
                    || !world.noCollision(entity) || world.containsAnyLiquid(entity.getBoundingBox())
                    || world.noCollision(entity, entity.getBoundingBox().move(0, -0.2, 0))) continue;
            entity.watch(player);
            return world.addFreshEntity(entity);
        }
        return false;
    }
}
