package nightshift.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import nightshift.Nightshift;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** The authoritative per-player encounter deadlines, shared across dimensions. */
public final class EncounterSavedData extends SavedData {
    // The scare field preserves old saves; the retired prototype no longer reads it for gameplay.
    public record Deadlines(long encounter, long scare) {
        private static final Codec<Deadlines> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("encounter").forGetter(Deadlines::encounter),
                Codec.LONG.fieldOf("scare").forGetter(Deadlines::scare)
        ).apply(i, Deadlines::new));
    }

    private static final Codec<EncounterSavedData> CODEC = Codec.unboundedMap(Codec.STRING, Deadlines.CODEC)
            .xmap(EncounterSavedData::new, data -> data.players);
    public static final SavedDataType<EncounterSavedData> TYPE = new SavedDataType<>(
            Nightshift.id("encounters"), EncounterSavedData::new, CODEC, null);
    private final Map<String, Deadlines> players;

    /** Creates an empty encounter schedule without marking it dirty. */
    public EncounterSavedData() { this(Map.of()); }

    /** Copies decoded player deadlines into mutable storage without marking the loaded save dirty. */
    private EncounterSavedData(Map<String, Deadlines> players) { this.players = new HashMap<>(players); }

    /** Returns a player's deadlines, or {@code null} if absent, without creating a record. */
    public Deadlines get(UUID player) { return players.get(player.toString()); }

    /** Stores absolute overworld-tick deadlines for one player, retaining the supplied legacy scare value, and marks the save dirty. */
    public void set(UUID player, long encounter, long scare) {
        players.put(player.toString(), new Deadlines(encounter, scare));
        setDirty();
    }
}
