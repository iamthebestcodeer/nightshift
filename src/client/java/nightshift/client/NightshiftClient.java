package nightshift.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import nightshift.client.effect.EncounterPresentation;
import nightshift.client.render.UnderstudyRenderer;
import nightshift.entity.NightshiftEntities;

public final class NightshiftClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        EntityRenderers.register(NightshiftEntities.UNDERSTUDY, UnderstudyRenderer::new);
        EntityRenderers.register(NightshiftEntities.APPARITION, nightshift.client.render.ApparitionRenderer::new);
        EncounterPresentation.initialize();
    }
}
