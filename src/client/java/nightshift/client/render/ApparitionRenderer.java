package nightshift.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.ArrayList;
import nightshift.entity.Apparition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;

public final class ApparitionRenderer extends EntityRenderer<Apparition, ApparitionRenderer.State> {
    public record Shape(BlockPos offset, BlockModelRenderState model) {}
    public static final class State extends EntityRenderState { private List<Shape> shapes = List.of(); }
    private final BlockModelResolver models;
    private record Cached(List<Apparition.GhostBlock> blocks, List<Shape> shapes) {}
    private final java.util.Map<Apparition, Cached> cache = new java.util.WeakHashMap<>();

    /** Creates a renderer that resolves apparition shapes through the client block model resolver. */
    public ApparitionRenderer(EntityRendererProvider.Context context) {
        super(context);
        models = context.getBlockModelResolver();
    }

    /** Creates an empty shape snapshot for one apparition render state. */
    @Override public State createRenderState() { return new State(); }
    /** Disables origin-box culling because apparition geometry can extend far beyond that box. */
    @Override protected boolean affectedByCulling(Apparition entity) { return false; }

    /** Resolves block models when the immutable shape snapshot changes and reuses cached models otherwise. */
    @Override public void extractRenderState(Apparition entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        // Resolve immutable block models only when a different shape snapshot is encountered.
        var cached = cache.get(entity);
        if (cached == null || entity.blocks() != cached.blocks) {
            var resolved = new ArrayList<Shape>();
            for (var block : entity.blocks()) {
                var model = new BlockModelRenderState();
                models.update(model, block.state(), DisplayRenderer.BLOCK_DISPLAY_CONTEXT);
                resolved.add(new Shape(block.offset(), model));
            }
            cached = new Cached(entity.blocks(), List.copyOf(resolved));
            cache.put(entity, cached);
        }
        state.shapes = cached.shapes;
    }

    /** Submits each ghost block model at its relative offset with the entity lighting. */
    @Override public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        for (var shape : state.shapes) {
            poses.pushPose();
            poses.translate(shape.offset.getX(), shape.offset.getY(), shape.offset.getZ());
            shape.model.submit(poses, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poses.popPose();
        }
    }
}
