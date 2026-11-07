package dev.whizzle.shulker.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.whizzle.shulker.client.render.BoxExtras;
import dev.whizzle.shulker.client.render.Labels;
import dev.whizzle.shulker.client.render.PatternLayer;
import dev.whizzle.shulker.client.render.Patterns;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer;
import net.minecraft.client.renderer.blockentity.state.ShulkerBoxRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Patterns and labels on shulker boxes. Placed boxes: the layers the server reported and the label are put on the
 * render state; the layers are set for the duration of the draw, the label is drawn after the box. Every box (placed or item) is drawn by the public {@code submit} below; after vanilla's
 * model, the layers are drawn with the same model, already posed, so they follow the lid.
 */
@Mixin(ShulkerBoxRenderer.class)
public abstract class ShulkerBoxRendererMixin {
	@Unique private static Field shulkerstyles$modelField;

	@Inject(method = "extractRenderState(Lnet/minecraft/world/level/block/entity/ShulkerBoxBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/ShulkerBoxRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V", at = @At("TAIL"))
	private void shulkerstyles$extract(ShulkerBoxBlockEntity box, ShulkerBoxRenderState state, float partialTicks, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay crumbling, CallbackInfo ci) {
		BoxExtras extras = (BoxExtras) state;
		extras.shulkerstyles$setLayers(Patterns.forBlock(box.getBlockPos()));
		Labels.extract(state, extras.shulkerstyles$label(), camera);
	}

	@Inject(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/ShulkerBoxRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"))
	private void shulkerstyles$beginBlock(ShulkerBoxRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
		Patterns.CURRENT.set(((BoxExtras) state).shulkerstyles$layers());
	}

	@Inject(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/ShulkerBoxRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("RETURN"))
	private void shulkerstyles$endBlock(ShulkerBoxRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
		Patterns.CURRENT.remove();
		Labels.submit(state, ((BoxExtras) state).shulkerstyles$label(), poseStack, collector, camera);
	}

	@Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIFLnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;Lnet/minecraft/client/resources/model/sprite/SpriteId;I)V", at = @At("TAIL"))
	private void shulkerstyles$layers(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, float progress, ModelFeatureRenderer.CrumblingOverlay crumbling, SpriteId sprite, int outline, CallbackInfo ci) {
		List<PatternLayer> layers = Patterns.CURRENT.get();
		if (layers == null || layers.isEmpty()) return;
		Model<Float> model = shulkerstyles$model();
		if (model != null) Patterns.submit(layers, model, progress, poseStack, collector, light, overlay);
	}

	/** The renderer's own (private) model, already posed by vanilla for this draw. */
	@Unique
	@SuppressWarnings("unchecked")
	private Model<Float> shulkerstyles$model() {
		try {
			if (shulkerstyles$modelField == null) {
				shulkerstyles$modelField = ShulkerBoxRenderer.class.getDeclaredField("model");
				shulkerstyles$modelField.setAccessible(true);
			}
			return (Model<Float>) shulkerstyles$modelField.get(this);
		} catch (ReflectiveOperationException e) {
			return null;
		}
	}
}
