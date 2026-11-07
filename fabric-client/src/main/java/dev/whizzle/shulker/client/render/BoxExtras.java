package dev.whizzle.shulker.client.render;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * What the mod adds to each shulker box's render state (by a mixin): its pattern layers and its label. Filled when the
 * frame's state is extracted, drawn when it's submitted. Lives outside the mixin package, which may hold only mixins
 * (Mixin refuses to load any other class from it).
 */
public interface BoxExtras {
	/** Pattern layers; null: none. */
	List<PatternLayer> shulkerstyles$layers();
	void shulkerstyles$setLayers(List<PatternLayer> layers);

	/** The label for this frame (reused between frames); never null. */
	Label shulkerstyles$label();

	/** A box's label: the icon item on each open face, and the name if it's the box you're looking at. */
	final class Label {
		public final ItemStackRenderState icon = new ItemStackRenderState();
		/** Faces to draw the icon on, one bit per {@code Direction.ordinal()}; 0: no icon. */
		public int faces;
		/** Light on each face (from the block in front of it), by {@code Direction.ordinal()}. */
		public final int[] light = new int[6];
		/** Shown over the box; null: not looked at, or nothing to show. */
		public Component name;

		public void clear() { faces = 0; name = null; icon.clear(); }
	}
}
