package nightshift.encounter;

import java.util.ArrayList;
import java.util.List;

/** Bounded geometry and eligibility for passive environmental encounters. */
public final class SceneRules {
    public static final int MAX_PLACEMENTS = 512;
    public static final int MAX_COPY_VOLUME = 4096;
    public static final int BLOCKS_PER_TICK = 2;
    public record Cell(int x, int y, int z) {}

    private SceneRules() {}

    /** Allows the passive construction exception only while the actor is not attacking. */
    public static boolean mayBuild(boolean attacking) { return !attacking; }

    /**
     * Returns an immutable, inclusive horizontal line in endpoint travel order.
     *
     * @throws IllegalArgumentException if the endpoints are not cardinal and level, or the line exceeds 64 cells
     */
    public static List<Cell> bridge(Cell from, Cell to) {
        if (from.y != to.y || (from.x != to.x && from.z != to.z)) {
            throw new IllegalArgumentException("Bridge endpoints must form a horizontal, straight line.");
        }
        long length = Math.abs((long) to.x - from.x) + Math.abs((long) to.z - from.z) + 1;
        if (length > 64) throw new IllegalArgumentException("Bridge is limited to 64 blocks.");
        var cells = new ArrayList<Cell>();
        int dx = Integer.compare(to.x, from.x), dz = Integer.compare(to.z, from.z);
        for (int i = 0; i < length; i++) cells.add(new Cell(from.x + dx * i, from.y, from.z + dz * i));
        return List.copyOf(cells);
    }

    /**
     * Returns the inclusive cells of a vertical rectangle, ordered by ascending y, x, then z.
     *
     * @throws IllegalArgumentException if the corners are not in one vertical plane, or exceed 32 cells wide or 16 high
     */
    public static List<Cell> wall(Cell from, Cell to) {
        if (from.x != to.x && from.z != to.z) throw new IllegalArgumentException("Wall corners must lie in one vertical plane.");
        if (Math.abs((long) from.y - to.y) >= 16) throw new IllegalArgumentException("Wall height is limited to 16 blocks.");
        if (Math.abs((long) from.x - to.x) >= 32 || Math.abs((long) from.z - to.z) >= 32)
            throw new IllegalArgumentException("Wall width is limited to 32 blocks.");
        return region(from, to, MAX_PLACEMENTS);
    }

    /**
     * Returns an immutable, inclusive cuboid ordered by ascending y, x, then z.
     * Checks dimensions before multiplying to avoid overflow for extreme coordinates.
     *
     * @param limit maximum permitted number of cells
     * @throws IllegalArgumentException if the limit is nonpositive or the cuboid exceeds it
     */
    public static List<Cell> region(Cell from, Cell to, int limit) {
        long widthLong = Math.abs((long) from.x - to.x) + 1;
        long heightLong = Math.abs((long) from.y - to.y) + 1;
        long depthLong = Math.abs((long) from.z - to.z) + 1;
        // Check each dimension before multiplication, including extreme integer coordinates.
        if (limit < 1 || widthLong > limit || heightLong > limit || depthLong > limit
                || widthLong * heightLong > limit || widthLong * heightLong * depthLong > limit) {
            throw new IllegalArgumentException("Marked area exceeds the scene size limit (" + limit + " cells).");
        }
        long volume = widthLong * heightLong * depthLong;
        var cells = new ArrayList<Cell>((int) volume);
        int minX = Math.min(from.x, to.x), minY = Math.min(from.y, to.y), minZ = Math.min(from.z, to.z);
        int width = (int) (Math.abs((long) from.x - to.x) + 1);
        int height = (int) (Math.abs((long) from.y - to.y) + 1);
        int depth = (int) (Math.abs((long) from.z - to.z) + 1);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < depth; z++) cells.add(new Cell(minX + x, minY + y, minZ + z));
            }
        }
        return List.copyOf(cells);
    }
}
