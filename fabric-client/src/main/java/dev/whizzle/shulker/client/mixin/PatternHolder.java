package dev.whizzle.shulker.client.mixin;

import dev.whizzle.shulker.client.render.PatternLayer;

import java.util.List;

/** Added to the shulker box render state: the layers for that box (null: none). */
public interface PatternHolder {
	List<PatternLayer> shulkerstyles$layers();
	void shulkerstyles$setLayers(List<PatternLayer> layers);
}
