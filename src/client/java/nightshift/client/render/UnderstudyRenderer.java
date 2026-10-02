package nightshift.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import nightshift.Nightshift;
import nightshift.entity.Understudy;

public final class UnderstudyRenderer extends HumanoidMobRenderer<Understudy, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
    private static final Identifier SKIN = Nightshift.id("textures/entity/understudy.png");

    public UnderstudyRenderer(EntityRendererProvider.Context context) {
        // Player geometry with a mob state: AvatarRenderState is dispatched to the player renderer.
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return SKIN;
    }
}
