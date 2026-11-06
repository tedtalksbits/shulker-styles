package dev.whizzle.shulker.client.mixin;

import dev.whizzle.shulker.client.render.PatternLayer;
import net.minecraft.client.renderer.blockentity.state.ShulkerBoxRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(ShulkerBoxRenderState.class)
public class ShulkerBoxRenderStateMixin implements PatternHolder {
	@Unique private List<PatternLayer> shulkerstyles$layers;

	@Override public List<PatternLayer> shulkerstyles$layers() { return shulkerstyles$layers; }
	@Override public void shulkerstyles$setLayers(List<PatternLayer> layers) { shulkerstyles$layers = layers; }
}
