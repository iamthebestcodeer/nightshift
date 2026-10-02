package nightshift.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import nightshift.client.effect.EncounterPresentation;
import nightshift.client.render.UnderstudyRenderer;
import nightshift.entity.NightshiftEntities;

public final class NightshiftClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        EntityRendererRegistry.register(NightshiftEntities.UNDERSTUDY, UnderstudyRenderer::new);
        EntityRendererRegistry.register(NightshiftEntities.APPARITION, nightshift.client.render.ApparitionRenderer::new);
        EncounterPresentation.initialize();
    }
}
