package nightshift.client.render;

import nightshift.Nightshift;
import nightshift.entity.Understudy;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class UnderstudyRenderer extends HumanoidMobRenderer<Understudy, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
    public UnderstudyRenderer(EntityRendererProvider.Context context) {
        super(context, new StillModel(context.bakeLayer(ModelLayers.PLAYER)), 0.35f);
    }

    private static final class StillModel extends HumanoidModel<HumanoidRenderState> {
        private StillModel(net.minecraft.client.model.geom.ModelPart root) { super(root); }
        @Override public void setupAnim(HumanoidRenderState state) {
            super.setupAnim(state);
            rightArm.xRot = rightArm.yRot = rightArm.zRot = 0;
            leftArm.xRot = leftArm.yRot = leftArm.zRot = 0;
        }
    }

    @Override public HumanoidRenderState createRenderState() { return new HumanoidRenderState(); }
    @Override public Identifier getTextureLocation(HumanoidRenderState state) { return Nightshift.id("textures/entity/understudy.png"); }

    @Override public void extractRenderState(Understudy entity, HumanoidRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.bodyRot = entity.getYRot();
        state.yRot = Mth.wrapDegrees(Math.round(entity.yHeadRot / 15) * 15 - state.bodyRot);
        state.xRot = Math.round(entity.getXRot() / 15) * 15;
        // The body stays rigid even while it glides; the head turns independently.
        state.walkAnimationSpeed = 0;
    }
}
