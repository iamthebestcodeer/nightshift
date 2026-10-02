package nightshift.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import nightshift.client.render.UnderstudyRenderer;
import nightshift.entity.NightshiftEntities;

public class NightshiftClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRenderers.register(NightshiftEntities.UNDERSTUDY, UnderstudyRenderer::new);
	}
}
