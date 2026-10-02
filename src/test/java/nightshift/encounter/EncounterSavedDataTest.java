package nightshift.encounter;

import com.mojang.serialization.JsonOps;
import java.util.UUID;
import nightshift.world.EncounterSavedData;

public final class EncounterSavedDataTest {
    private EncounterSavedDataTest() {}

    public static void verify() {
        UUID first = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID second = UUID.fromString("00000000-0000-0000-0000-000000000002");
        var original = new EncounterSavedData();
        original.set(first, 4000, 12000);
        original.set(second, 6000, 18000);
        var encoded = EncounterSavedData.TYPE.codec().encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        var restored = EncounterSavedData.TYPE.codec().parse(JsonOps.INSTANCE, encoded).getOrThrow();
        if (!original.isDirty() || restored.isDirty()) throw new AssertionError("saved data dirty tracking");
        if (!restored.get(first).equals(original.get(first)) || !restored.get(second).equals(original.get(second))) {
            throw new AssertionError("per-player deadlines survive serialization");
        }
        restored.set(first, 8000, 20000);
        if (restored.get(second).encounter() != 6000 || restored.get(second).scare() != 18000) {
            throw new AssertionError("changing one player must not affect another");
        }
        System.out.println("Encounter persistence and player isolation checks passed");
    }
}
