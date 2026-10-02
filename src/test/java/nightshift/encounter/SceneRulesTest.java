package nightshift.encounter;

import nightshift.encounter.SceneRules.Cell;

final class SceneRulesTest {
    /** Checks scene geometry limits, endpoint ordering, extreme coordinates, and attack eligibility. */
    static void verify() {
        var reverse = SceneRules.bridge(new Cell(3, 0, 0), new Cell(-3, 0, 0));
        check(reverse.size() == 7 && reverse.getLast().x() == -3, "reverse bridge preserves travel direction");
        check(SceneRules.wall(new Cell(3, 4, 0), new Cell(0, 0, 0)).size() == 20, "wall fills marked rectangle");
        reject(() -> SceneRules.bridge(new Cell(0, 0, 0), new Cell(1, 0, 1)));
        reject(() -> SceneRules.bridge(new Cell(0, 0, 0), new Cell(64, 0, 0)));
        reject(() -> SceneRules.wall(new Cell(0, 0, 0), new Cell(0, 16, 0)));
        reject(() -> SceneRules.region(new Cell(0, 0, 0), new Cell(31, 15, 31), 4096));
        check(SceneRules.region(new Cell(Integer.MAX_VALUE, 0, 0), new Cell(Integer.MAX_VALUE, 0, 0), 1).size() == 1,
                "extreme coordinates terminate");
        check(SceneRules.region(new Cell(0, 0, 0), new Cell(39, 0, 0), 4096).size() == 40, "long replica fits volume cap");
        check(SceneRules.region(new Cell(0, 0, 0), new Cell(0, 39, 0), 4096).size() == 40, "tall replica fits volume cap");
        reject(() -> SceneRules.wall(new Cell(0, 0, 0), new Cell(39, 0, 0)));
        reject(() -> SceneRules.region(new Cell(Integer.MIN_VALUE, 0, 0), new Cell(Integer.MAX_VALUE, 0, 0), 4096));
        check(SceneRules.mayBuild(false) && !SceneRules.mayBuild(true), "passive exception stops on attack");
    }
    /** Requires the action to throw {@link IllegalArgumentException}; fails if invalid geometry is accepted. */
    private static void reject(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("invalid scene accepted");
    }
    /** Throws an assertion failure with the supplied message when the tested condition is false. */
    private static void check(boolean passed, String message) {
        if (!passed) throw new AssertionError(message);
    }
}
