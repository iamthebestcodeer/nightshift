package nightshift.encounter;

/** Gameplay rules without Minecraft dependencies. Times are server game ticks. */
public final class EncounterRules {
    public static final int LIFETIME = 30 * 20;
    public static final int HOLLOW_DURATION = 30 * 20;
    public static final int SEEN_DURATION = 90 * 20;

    private EncounterRules() {}

    /**
     * Returns the calm gap in server ticks, shortened while Seen is active.
     *
     * @param seen whether the player has Seen
     * @param random a uniform sample in the range [0, 1)
     * @return a delay in [2400, 3600) ticks when Seen, or [3600, 6000) otherwise
     */
    public static int encounterDelay(boolean seen, double random) {
        return (seen ? 120 : 180) * 20 + (int) (random * (seen ? 60 : 120) * 20);
    }

    /**
     * Tests the generous visibility cone, requiring an unobstructed line of sight.
     *
     * @param facingDot dot product of the normalized look and target direction vectors
     * @param lineOfSight whether the player has line of sight to the target
     * @return whether the dot product exceeds 0.1 and line of sight is clear
     */
    public static boolean inView(double facingDot, boolean lineOfSight) {
        // A deliberately generous field of view also covers the edge of the screen.
        return facingDot > 0.1 && lineOfSight;
    }

    /** Returns whether distance squared is at most 64 blocks squared or age reaches the 600-tick lifetime. */
    public static boolean shouldVanish(double distanceSquared, int age) {
        return distanceSquared <= 64 || age >= LIFETIME;
    }
}
