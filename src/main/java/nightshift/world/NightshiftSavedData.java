package nightshift.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nightshift.Nightshift;
import nightshift.encounter.PlayerState;
import nightshift.encounter.WorldPhase;

/** One overworld save shared by every dimension, with player records keyed by UUID. */
public final class NightshiftSavedData extends SavedData {
    private static final Codec<PlayerState> PLAYER_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("anger", 0).forGetter(PlayerState::anger),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("theft_count", 0).forGetter(PlayerState::theftCount),
            Codec.BOOL.optionalFieldOf("bed_theft", false).forGetter(PlayerState::bedTheft)
    ).apply(instance, PlayerState::new));
    private static final Codec<WorldPhase> PHASE_CODEC = Codec.stringResolver(WorldPhase::id, id -> {
        for (WorldPhase phase : WorldPhase.values()) {
            if (phase.id().equals(id)) {
                return phase;
            }
        }
        return WorldPhase.ODDITIES;
    });
    public static final Codec<NightshiftSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PHASE_CODEC.optionalFieldOf("phase", WorldPhase.ODDITIES).forGetter(NightshiftSavedData::phase),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, PLAYER_CODEC)
                    .optionalFieldOf("players", Map.of()).forGetter(data -> data.players)
    ).apply(instance, NightshiftSavedData::new));
    public static final SavedDataType<NightshiftSavedData> TYPE = new SavedDataType<>(
            Nightshift.id("state"), NightshiftSavedData::new, CODEC, null);

    private WorldPhase phase;
    private final Map<UUID, PlayerState> players;

    public NightshiftSavedData() {
        this(WorldPhase.ODDITIES, Map.of());
    }

    private NightshiftSavedData(WorldPhase phase, Map<UUID, PlayerState> players) {
        this.phase = phase;
        this.players = new HashMap<>(players);
    }

    public static NightshiftSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public WorldPhase phase() {
        return phase;
    }

    public void setPhase(WorldPhase value) {
        Objects.requireNonNull(value);
        if (phase != value) {
            phase = value;
            setDirty();
        }
    }

    public PlayerState playerState(UUID playerId) {
        return players.getOrDefault(Objects.requireNonNull(playerId), PlayerState.EMPTY);
    }

    public void setPlayerState(UUID playerId, PlayerState value) {
        Objects.requireNonNull(value);
        if (!playerState(playerId).equals(value)) {
            players.put(playerId, value);
            setDirty();
        }
    }

    public void setAnger(UUID playerId, int value) {
        setPlayerState(playerId, playerState(playerId).withAnger(value));
    }
}
