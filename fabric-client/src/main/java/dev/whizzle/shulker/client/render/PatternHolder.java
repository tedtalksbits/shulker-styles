package dev.whizzle.shulker.client.render;

import java.util.List;

/**
 * Added to the shulker box render state by a mixin: the layers for that box (null: none). Lives outside the mixin
 * package, which may hold only mixins (Mixin refuses to load any other class from it).
 */
public interface PatternHolder {
	List<PatternLayer> shulkerstyles$layers();
	void shulkerstyles$setLayers(List<PatternLayer> layers);
}
