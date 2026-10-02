package nightshift.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import nightshift.encounter.EncounterRules;
import nightshift.encounter.SceneRules;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** One non-colliding, unsaved entity renders a bounded set of block shapes. No world blocks change. */
public final class Apparition extends Entity {
    public record GhostBlock(BlockPos offset, BlockState state) {}
    private static final EntityDataAccessor<String> SHAPES = SynchedEntityData.defineId(Apparition.class, EntityDataSerializers.STRING);
    private List<GhostBlock> blocks = List.of();
    private double renderRadius = 128;
    private UUID owner;
    private long expiresAt;

    public Apparition(EntityType<? extends Apparition> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public void configure(ServerPlayer player, List<GhostBlock> shapes) {
        if (shapes.size() > SceneRules.MAX_PLACEMENTS) throw new IllegalArgumentException("Too many apparition blocks.");
        owner = player.getUUID();
        expiresAt = player.level().getServer().overworld().getGameTime() + EncounterRules.LIFETIME;
        var encoded = new StringBuilder();
        for (var shape : shapes) {
            encoded.append(shape.offset.getX()).append(',').append(shape.offset.getY()).append(',')
                    .append(shape.offset.getZ()).append(',').append(Block.getId(shape.state)).append(';');
        }
        entityData.set(SHAPES, encoded.toString());
    }

    public List<GhostBlock> blocks() { return blocks; }
    public boolean belongsTo(UUID player) { return player.equals(owner); }

    // Render range must cover the whole scene rather than the tiny non-colliding origin box.
    @Override public boolean shouldRenderAtSqrDistance(double distanceSquared) { return distanceSquared <= renderRadius * renderRadius; }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(SHAPES, ""); }
    @Override protected void readAdditionalSaveData(ValueInput input) {}
    @Override protected void addAdditionalSaveData(ValueOutput output) {}
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) { return false; }

    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key != SHAPES) return;
        var decoded = new ArrayList<GhostBlock>();
        for (String entry : entityData.get(SHAPES).split(";")) {
            if (entry.isEmpty()) continue;
            var fields = entry.split(",");
            decoded.add(new GhostBlock(new BlockPos(Integer.parseInt(fields[0]), Integer.parseInt(fields[1]), Integer.parseInt(fields[2])),
                    Block.stateById(Integer.parseInt(fields[3]))));
        }
        blocks = List.copyOf(decoded);
        double extent = 0;
        for (var block : blocks) extent = Math.max(extent, Math.sqrt(block.offset.distSqr(BlockPos.ZERO)) + Math.sqrt(3));
        renderRadius = 128 + extent;
    }

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel world)) return;
        ServerPlayer player = owner == null ? null : world.getServer().getPlayerList().getPlayer(owner);
        if (player == null || player.level() != world || !player.isAlive() || player.isSpectator()
                || world.getServer().overworld().getGameTime() >= expiresAt || distanceToSqr(player) <= 64) discard();
    }
}
