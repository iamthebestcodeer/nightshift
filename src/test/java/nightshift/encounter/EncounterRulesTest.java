package nightshift.encounter;

/** Boundary checks run by Gradle without an added testing dependency. */
public final class EncounterRulesTest {
    public static void main(String[] args) {
        check(EncounterRules.encounterDelay(false, 0) == 3600, "ordinary minimum gap");
        check(EncounterRules.encounterDelay(false, Math.nextDown(1.0)) < 6000, "ordinary maximum gap");
        check(EncounterRules.encounterDelay(true, 0) == 2400, "Seen minimum gap");
        check(EncounterRules.encounterDelay(true, Math.nextDown(1.0)) < 3600, "Seen maximum gap");
        check(!EncounterRules.inView(1, false), "walls hide the entity");
        check(!EncounterRules.inView(-1, true), "looking away hides the entity");
        check(EncounterRules.inView(0.11, true), "peripheral view freezes movement");
        check(!EncounterRules.inView(0.1, true), "view boundary");
        check(EncounterRules.shouldVanish(64, 1), "vanish at eight blocks");
        check(!EncounterRules.shouldVanish(64.01, 599), "stay outside approach boundary before timeout");
        check(EncounterRules.shouldVanish(10000, 600), "vanish at thirty seconds");
        SceneRulesTest.verify();
        EncounterSavedDataTest.verify();
        System.out.println("Encounter rule boundary checks passed");
    }

    private static void check(boolean passed, String message) {
        if (!passed) throw new AssertionError(message);
    }
}
