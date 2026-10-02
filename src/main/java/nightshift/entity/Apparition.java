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

    /** Creates an apparition with physics and gravity disabled. */
    public Apparition(EntityType<? extends Apparition> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    /**
     * Assigns the owner and 30-second lifetime, then synchronizes relative block offsets and state IDs.
     *
     * @throws IllegalArgumentException if the supplied shape count exceeds the scene placement limit
     */
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

    /** Returns the immutable block-shape snapshot decoded from synchronized entity data. */
    public List<GhostBlock> blocks() { return blocks; }
    /** Returns whether the supplied player UUID owns this apparition. */
    public boolean belongsTo(UUID player) { return player.equals(owner); }

    // Render range must cover the whole scene rather than the tiny non-colliding origin box.
    /** Tests squared camera distance against the render radius expanded to include all ghost geometry. */
    @Override public boolean shouldRenderAtSqrDistance(double distanceSquared) { return distanceSquared <= renderRadius * renderRadius; }

    /** Defines the initially empty shape payload synchronized to clients. */
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(SHAPES, ""); }
    /** Restores no custom state because the apparition entity type is registered without saving. */
    @Override protected void readAdditionalSaveData(ValueInput input) {}
    /** Writes no custom state because apparitions are temporary and excluded from saves. */
    @Override protected void addAdditionalSaveData(ValueOutput output) {}
    /** Rejects server damage so the visual apparition cannot be attacked. */
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) { return false; }

    /** Decodes a changed shape payload and expands the render radius to cover its furthest block. */
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

    /** Discards the apparition when its owner becomes ineligible, approaches within eight blocks, or its deadline expires. */
    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel world)) return;
        ServerPlayer player = owner == null ? null : world.getServer().getPlayerList().getPlayer(owner);
        if (player == null || player.level() != world || !player.isAlive() || player.isSpectator()
                || world.getServer().overworld().getGameTime() >= expiresAt || distanceToSqr(player) <= 64) discard();
    }
}
