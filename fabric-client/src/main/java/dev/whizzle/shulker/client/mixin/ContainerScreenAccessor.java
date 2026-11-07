package dev.whizzle.shulker.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Where a container screen sits and which slot is under the mouse, for the icon slot next to the box screen. */
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
	@Accessor("leftPos") int shulkerstyles$leftPos();
	@Accessor("topPos") int shulkerstyles$topPos();
	@Accessor("imageWidth") int shulkerstyles$imageWidth();
	@Accessor("hoveredSlot") Slot shulkerstyles$hoveredSlot();
}
