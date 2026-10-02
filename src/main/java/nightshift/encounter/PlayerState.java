package nightshift.encounter;

/** Immutable hidden state; Minecraft persistence owns all mutations. */
public record PlayerState(int anger, int theftCount, boolean bedTheft) {
    public static final PlayerState EMPTY = new PlayerState(0, 0, false);

    public PlayerState {
        if (anger < 0 || theftCount < 0) {
            throw new IllegalArgumentException("Anger and theft count must be nonnegative");
        }
    }

    public PlayerState withAnger(int value) {
        return new PlayerState(value, theftCount, bedTheft);
    }
}
