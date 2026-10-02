package nightshift.encounter;

/** Gameplay rules without Minecraft dependencies. Times are server game ticks. */
public final class EncounterRules {
    public static final int LIFETIME = 30 * 20;
    public static final int HOLLOW_DURATION = 30 * 20;
    public static final int SEEN_DURATION = 90 * 20;

    private EncounterRules() {}

    public static int encounterDelay(boolean seen, double random) {
        return (seen ? 120 : 180) * 20 + (int) (random * (seen ? 60 : 120) * 20);
    }

    public static boolean inView(double facingDot, boolean lineOfSight) {
        // A deliberately generous field of view also covers the edge of the screen.
        return facingDot > 0.1 && lineOfSight;
    }

    public static boolean shouldVanish(double distanceSquared, int age) {
        return distanceSquared <= 64 || age >= LIFETIME;
    }
}
