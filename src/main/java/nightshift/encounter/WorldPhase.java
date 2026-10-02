package nightshift.encounter;

import java.util.Locale;

/** Debug progression labels only; no automatic transitions yet. */
public enum WorldPhase {
    ODDITIES,
    WATCHING,
    STALKING,
    GRUDGE,
    SIEGE_NIGHTS,
    BED_THEFT,
    RITUAL;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
