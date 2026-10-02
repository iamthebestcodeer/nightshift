package nightshift;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Common entrypoint; keep client-only classes out of this source set. */
public final class Nightshift implements ModInitializer {
    public static final String MOD_ID = "nightshift";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Registers common entities, effects, commands, and the server encounter scheduler. */
    @Override
    public void onInitialize() {
        nightshift.entity.NightshiftEntities.initialize();
        nightshift.command.NightshiftCommands.register();
        nightshift.effect.NightshiftEffects.initialize();
        nightshift.encounter.EncounterDirector.initialize();
        nightshift.command.NightshiftTestCommands.initialize();
        LOGGER.info("Initializing Nightshift");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
