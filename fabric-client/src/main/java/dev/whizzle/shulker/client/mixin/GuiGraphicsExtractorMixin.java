package dev.whizzle.shulker.client.mixin;

import dev.whizzle.shulker.client.render.ClientIcons;
import dev.whizzle.shulker.client.Settings;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A small label icon in the bottom-right corner of box items in inventories and the hotbar (boxes don't stack). */
@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin {
	@Inject(method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("TAIL"))
	private void shulkerstyles$icon(Font font, ItemStack stack, int x, int y, String countText, CallbackInfo ci) {
		if (!Settings.inventoryIcons || stack.isEmpty() || !ClientIcons.isBox(stack)) return;
		ItemStack icon = ClientIcons.stack(ClientIcons.iconId(stack));
		if (icon.isEmpty()) return;
		GuiGraphicsExtractor g = (GuiGraphicsExtractor) (Object) this;
		g.pose().pushMatrix();
		g.pose().translate(x + 8, y + 8);
		g.pose().scale(0.5f, 0.5f);
		g.item(icon, 0, 0);
		g.pose().popMatrix();
	}
}
