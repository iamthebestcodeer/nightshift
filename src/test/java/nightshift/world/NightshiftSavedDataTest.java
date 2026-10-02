package nightshift.world;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.storage.SavedDataStorage;
import nightshift.encounter.PlayerState;
import nightshift.encounter.WorldPhase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NightshiftSavedDataTest {
    @TempDir
    Path directory;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void savesReloadsAndUpdatesTwoIndependentPlayerRecords() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        PlayerState firstState = new PlayerState(17, 3, true);
        PlayerState secondState = new PlayerState(42, 8, false);
        try (SavedDataStorage storage = storage(directory)) {
            NightshiftSavedData data = storage.computeIfAbsent(NightshiftSavedData.TYPE);
            data.setPlayerState(first, firstState);
            data.setPlayerState(second, secondState);
            data.setPhase(WorldPhase.GRUDGE);
            storage.saveAndJoin();
            assertFalse(data.isDirty());
        }
        assertTrue(Files.isRegularFile(directory.resolve("nightshift/state.dat")));
        try (SavedDataStorage storage = storage(directory)) {
            NightshiftSavedData data = storage.get(NightshiftSavedData.TYPE);
            assertNotNull(data, "Loading must succeed rather than silently creating defaults");
            assertEquals(firstState, data.playerState(first));
            assertEquals(secondState, data.playerState(second));
            assertEquals(WorldPhase.GRUDGE, data.phase());
            assertEquals(PlayerState.EMPTY, data.playerState(UUID.randomUUID()));
            assertFalse(data.isDirty());
            data.setAnger(first, 0);
            assertTrue(data.isDirty());
            data.setPhase(WorldPhase.RITUAL);
        }
        try (SavedDataStorage storage = storage(directory)) {
            NightshiftSavedData data = storage.get(NightshiftSavedData.TYPE);
            assertNotNull(data);
            assertEquals(new PlayerState(0, 3, true), data.playerState(first));
            assertEquals(secondState, data.playerState(second));
            assertEquals(WorldPhase.RITUAL, data.phase());
        }
        try (SavedDataStorage otherWorld = storage(directory.resolve("another-world"))) {
            NightshiftSavedData data = otherWorld.computeIfAbsent(NightshiftSavedData.TYPE);
            assertEquals(PlayerState.EMPTY, data.playerState(first));
            assertEquals(WorldPhase.ODDITIES, data.phase());
        }
    }

    @Test
    void readsAndUnchangedWritesDoNotDirtyData() {
        NightshiftSavedData data = new NightshiftSavedData();
        UUID player = UUID.randomUUID();
        assertEquals(PlayerState.EMPTY, data.playerState(player));
        data.setPlayerState(player, PlayerState.EMPTY);
        data.setPhase(WorldPhase.ODDITIES);
        assertFalse(data.isDirty());
        data.setPlayerState(player, new PlayerState(0, 1, true));
        assertTrue(data.isDirty());
        data.setDirty(false);
        data.setAnger(player, 0);
        assertFalse(data.isDirty());
    }

    @Test
    void missingFieldsLoadDefaultsAndEveryPhaseRoundTrips() {
        NightshiftSavedData empty = NightshiftSavedData.CODEC.parse(NbtOps.INSTANCE, new CompoundTag()).getOrThrow();
        assertEquals(WorldPhase.ODDITIES, empty.phase());
        assertEquals(PlayerState.EMPTY, empty.playerState(UUID.randomUUID()));
        for (WorldPhase phase : WorldPhase.values()) {
            empty.setPhase(phase);
            var encoded = NightshiftSavedData.CODEC.encodeStart(NbtOps.INSTANCE, empty).getOrThrow();
            assertEquals(phase, NightshiftSavedData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow().phase());
        }
    }

    @Test
    void unknownPhaseLoadsDefaultsWithoutLosingPlayerRecords() throws Exception {
        UUID player = UUID.randomUUID();
        PlayerState state = new PlayerState(17, 3, true);
        try (SavedDataStorage storage = storage(directory)) {
            NightshiftSavedData data = storage.computeIfAbsent(NightshiftSavedData.TYPE);
            data.setPlayerState(player, state);
            data.setPhase(WorldPhase.GRUDGE);
        }
        Path file = directory.resolve("nightshift/state.dat");
        CompoundTag saved = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
        saved.getCompoundOrEmpty("data").putString("phase", "unknown_phase");
        NightshiftSavedData decoded = NightshiftSavedData.CODEC
                .parse(NbtOps.INSTANCE, saved.getCompoundOrEmpty("data")).getOrThrow();
        assertEquals(WorldPhase.ODDITIES, decoded.phase());
        assertEquals(state, decoded.playerState(player));
        NbtIo.writeCompressed(saved, file);
        try (SavedDataStorage storage = storage(directory)) {
            NightshiftSavedData data = storage.get(NightshiftSavedData.TYPE);
            assertNotNull(data, "An unknown phase must not invalidate the save");
            assertEquals(WorldPhase.ODDITIES, data.phase());
            assertEquals(state, data.playerState(player));
        }
    }

    @Test
    void rejectsInvalidStateWithoutChangingExistingRecords() {
        NightshiftSavedData data = new NightshiftSavedData();
        UUID player = UUID.randomUUID();
        data.setPlayerState(player, new PlayerState(7, 2, true));
        data.setDirty(false);
        assertThrows(IllegalArgumentException.class, () -> data.setAnger(player, -1));
        assertThrows(IllegalArgumentException.class, () -> new PlayerState(0, -1, false));
        assertThrows(NullPointerException.class, () -> data.setPlayerState(player, null));
        assertThrows(NullPointerException.class, () -> data.setPhase(null));
        assertEquals(new PlayerState(7, 2, true), data.playerState(player));
        assertFalse(data.isDirty());
    }

    private static SavedDataStorage storage(Path path) {
        return new SavedDataStorage(path, DataFixers.getDataFixer(), HolderLookup.Provider.create(Stream.empty()));
    }
}
