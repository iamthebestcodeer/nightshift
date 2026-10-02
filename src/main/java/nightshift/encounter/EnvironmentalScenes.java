package nightshift.encounter;

import java.util.ArrayList;
import java.util.List;
import nightshift.entity.Apparition;
import nightshift.entity.NightshiftEntities;
import nightshift.entity.Understudy;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import java.util.Set;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Integration for explicit, marked scenes. No automatic base or construction inference. */
public final class EnvironmentalScenes {
    // Explicit inert masonry only: collision shape alone says nothing about placement callbacks.
    private static final Set<Block> MATERIALS = Set.of(Blocks.STONE, Blocks.COBBLESTONE, Blocks.STONE_BRICKS,
            Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE, Blocks.POLISHED_DEEPSLATE,
            Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_TILES, Blocks.BRICKS);

    private EnvironmentalScenes() {}

    public static void construct(ServerPlayer player, List<SceneRules.Cell> cells, BlockState material) {
        requirePlayer(player);
        ServerLevel world = (ServerLevel) player.level();
        var positions = cells.stream().map(cell -> new BlockPos(cell.x(), cell.y(), cell.z())).toList();
        validate(world, player, positions);
        if (!MATERIALS.contains(material.getBlock())) {
            throw new IllegalArgumentException("Choose stone, cobblestone, stone bricks, bricks, or full deepslate masonry.");
        }
        Understudy actor = new Understudy(NightshiftEntities.UNDERSTUDY, world);
        var start = positions.stream().map(PassiveConstruction::standingPosition)
                .filter(position -> PassiveConstruction.canStandAt(world, actor, position)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No clear space for the builder above the marked cells."));
        actor.setPos(start);
        actor.buildScene(player, new PassiveConstruction(positions, material));
        removeActors(player);
        world.addFreshEntity(actor);
    }

    public static void tunnel(ServerPlayer player) {
        requirePlayer(player);
        ServerLevel world = (ServerLevel) player.level();
        Vec3 back = player.getLookAngle().multiply(1, 0, 1).normalize().scale(-1);
        if (back.lengthSqr() < 0.5) throw new IllegalArgumentException("Look along the tunnel rather than straight up or down.");
        for (int distance = 4; distance <= 6; distance++) {
            Vec3 position = player.position().add(back.scale(distance));
            Understudy actor = new Understudy(NightshiftEntities.UNDERSTUDY, world);
            actor.setPos(position);
            if (!world.hasChunkAt(actor.blockPosition()) || !world.getWorldBorder().isWithinBounds(actor.blockPosition())
                    || !world.noCollision(actor) || world.containsAnyLiquid(actor.getBoundingBox())
                    || world.noCollision(actor, actor.getBoundingBox().move(0, -0.2, 0))) continue;
            actor.quietScene(player, 2);
            removeActors(player);
            world.addFreshEntity(actor);
            return;
        }
        throw new IllegalArgumentException("No clear, supported space 4–6 blocks behind you.");
    }

    public static void replica(ServerPlayer player, BlockPos from, BlockPos to, BlockPos destination) {
        requirePlayer(player);
        ServerLevel world = (ServerLevel) player.level();
        var cells = SceneRules.region(cell(from), cell(to), SceneRules.MAX_COPY_VOLUME);
        var source = cells.stream().map(cell -> new BlockPos(cell.x(), cell.y(), cell.z())).toList();
        validate(world, player, source);
        int minX = Math.min(from.getX(), to.getX()), minY = Math.min(from.getY(), to.getY()), minZ = Math.min(from.getZ(), to.getZ());
        var shapes = new ArrayList<Apparition.GhostBlock>();
        for (var position : source) {
            var state = world.getBlockState(position);
            if (state.isAir()) continue;
            var offset = position.offset(-minX, -minY, -minZ);
            validate(world, player, List.of(destination.offset(offset)));
            // Only block shape/state is copied: inventories, signs, and other block data are never copied.
            shapes.add(new Apparition.GhostBlock(offset, state));
            if (shapes.size() > SceneRules.MAX_PLACEMENTS) throw new IllegalArgumentException("Apparition is limited to 512 non-air blocks.");
        }
        if (shapes.isEmpty()) throw new IllegalArgumentException("The marked reference area is empty.");
        Apparition ghost = new Apparition(NightshiftEntities.APPARITION, world);
        // Anchor tracking/retreat on actual geometry, not an empty distant minimum corner.
        var anchor = shapes.getFirst().offset();
        var origin = destination.offset(anchor);
        ghost.setPos(origin.getX(), origin.getY(), origin.getZ());
        var anchoredShapes = shapes.stream().map(shape -> new Apparition.GhostBlock(shape.offset().subtract(anchor), shape.state())).toList();
        ghost.configure(player, anchoredShapes);
        world.getEntitiesOfClass(Apparition.class, player.getBoundingBox().inflate(128),
                existing -> existing.belongsTo(player.getUUID())).forEach(Apparition::discard);
        world.addFreshEntity(ghost);
    }

    public static SceneRules.Cell cell(BlockPos position) { return new SceneRules.Cell(position.getX(), position.getY(), position.getZ()); }

    private static void requirePlayer(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) throw new IllegalArgumentException("Use a living player outside spectator mode.");
    }

    private static void removeActors(ServerPlayer player) {
        player.level().getEntitiesOfClass(Understudy.class, player.getBoundingBox().inflate(128),
                actor -> actor.isWatching(player.getUUID())).forEach(Understudy::discard);
    }

    private static void validate(ServerLevel world, ServerPlayer player, List<BlockPos> positions) {
        for (var position : positions) {
            if (!world.hasChunkAt(position) || !world.getWorldBorder().isWithinBounds(position)
                    || position.getY() < world.getMinY() || position.getY() > world.getMaxY()
                    || position.distToCenterSqr(player.getX(), player.getY(), player.getZ()) > 96 * 96) {
                throw new IllegalArgumentException("Scene cells must be inside loaded world bounds and within 96 blocks of you.");
            }
        }
    }
}
