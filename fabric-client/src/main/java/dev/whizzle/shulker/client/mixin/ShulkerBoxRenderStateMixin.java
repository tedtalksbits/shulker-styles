package dev.whizzle.shulker.client.mixin;

import dev.whizzle.shulker.client.render.BoxExtras;
import dev.whizzle.shulker.client.render.PatternLayer;
import net.minecraft.client.renderer.blockentity.state.ShulkerBoxRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(ShulkerBoxRenderState.class)
public class ShulkerBoxRenderStateMixin implements BoxExtras {
	@Unique private List<PatternLayer> shulkerstyles$layers;
	@Unique private final BoxExtras.Label shulkerstyles$label = new BoxExtras.Label();

	@Override public List<PatternLayer> shulkerstyles$layers() { return shulkerstyles$layers; }
	@Override public void shulkerstyles$setLayers(List<PatternLayer> layers) { shulkerstyles$layers = layers; }
	@Override public BoxExtras.Label shulkerstyles$label() { return shulkerstyles$label; }
}
