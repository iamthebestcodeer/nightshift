package nightshift;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import nightshift.command.NightshiftCommands;
import nightshift.entity.NightshiftEntities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Common entrypoint; keep client-only classes out of this source set. */
public final class Nightshift implements ModInitializer {
    public static final String MOD_ID = "nightshift";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        NightshiftEntities.register();
        NightshiftCommands.register();
        LOGGER.info("Initializing Nightshift");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
