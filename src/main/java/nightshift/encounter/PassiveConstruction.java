package nightshift.encounter;

import java.util.List;
import java.util.ArrayDeque;
import nightshift.entity.Understudy;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** An explicit placement-only exception, stopped immediately if the actor starts attacking. */
public final class PassiveConstruction {
    private final ArrayDeque<BlockPos> pending;
    private final BlockState material;

    /** Copies the ordered placement positions into a pending queue and retains the chosen block state. */
    public PassiveConstruction(List<BlockPos> positions, BlockState material) {
        this.pending = new ArrayDeque<>(positions);
        this.material = material;
    }

    /** Returns the centered foot position one block above a placement cell. */
    static Vec3 standingPosition(BlockPos cell) {
        return new Vec3(cell.getX() + 0.5, cell.getY() + 1, cell.getZ() + 0.5);
    }

    /** Checks loaded bounds, body clearance, and liquid avoidance at the proposed actor position; does not require support. */
    static boolean canStandAt(ServerLevel world, Understudy actor, Vec3 position) {
        var box = actor.getBoundingBox().move(position.subtract(actor.position()));
        var bottom = BlockPos.containing(box.minX, box.minY, box.minZ);
        var top = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
        return world.getChunkSource().hasChunk(bottom.getX() >> 4, bottom.getZ() >> 4) && world.getChunkSource().hasChunk(top.getX() >> 4, top.getZ() >> 4)
                && world.getWorldBorder().isWithinBounds(box) && box.minY >= world.getMinY() && box.maxY <= world.getMaxY() + 1
                && world.noCollision(actor, box) && !world.containsAnyLiquid(box);
    }

    /**
     * Processes at most two pending cells, placing only into air and retrying entity obstructions.
     * Discards an attacking actor immediately and moves it above a placed block only when body space is clear.
     */
    public void tick(ServerLevel world, Understudy actor) {
        if (!SceneRules.mayBuild(actor.isAggressive() || actor.getTarget() != null)) {
            actor.discard();
            return;
        }
        for (int budget = 0; budget < SceneRules.BLOCKS_PER_TICK && !pending.isEmpty(); budget++) {
            BlockPos position = pending.removeFirst();
            if (!world.getChunkSource().hasChunk(position.getX() >> 4, position.getZ() >> 4) || !world.getWorldBorder().isWithinBounds(position)
                    || !world.getBlockState(position).isAir()) continue;
            if (!world.getEntities(actor, new AABB(position)).isEmpty()) {
                pending.addLast(position);
                continue;
            }
            Vec3 destination = standingPosition(position);
            boolean clear = canStandAt(world, actor, destination);
            // Never place inside our own body if there is nowhere clear to move.
            if (!clear && actor.getBoundingBox().intersects(new AABB(position))) {
                pending.addLast(position);
                continue;
            }
            if (world.setBlock(position, material, 3) && clear) actor.setPos(destination);
        }
    }
}
