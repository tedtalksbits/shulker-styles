package dev.whizzle.shulker.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.whizzle.shulker.client.ShulkerStylesClient;
import dev.whizzle.shulker.client.mixin.ContainerScreenAccessor;
import dev.whizzle.shulker.client.net.Net;
import dev.whizzle.shulker.client.render.ClientIcons;
import dev.whizzle.shulker.model.BoxInfo;
import dev.whizzle.shulker.model.Protocol;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The label icon slot next to an open shulker box's screen (the owner's pick). Click it, then click any item (in the
 * box or your inventory) to make that the box's icon; click it with an item on the cursor to use that item; right-click
 * it for automatic. The server is told with {@code seticon} and the change comes back like any other.
 */
public final class IconSlot {
	private IconSlot() {}

	static final int SIZE = 22;
	private static final int BG = 0xFF8B8B8B, DARK = 0xFF373737, LIGHT = 0xFFFFFFFF, PICKING = 0xFFFFD84A, PICKING_BG = 0xFFE8C34A;

	/** The placed box the open screen belongs to (the server's {@code opened}); null when unknown. */
	private static Protocol.Pos open;
	private static boolean picking;

	public static void opened(Protocol.Pos pos) { open = pos; picking = false; }

	public static void register() {
		ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> {
			if (!(screen instanceof ShulkerBoxScreen)) return;
			// After the box's slots, before the carried item and tooltips (afterExtract would be too late: the
			// frame's tooltips are already drawn by then, so ours never showed and the slot covered item tooltips).
			ScreenEvents.afterForeground(screen).register(IconSlot::draw);
			ScreenMouseEvents.allowMouseClick(screen).register(IconSlot::click);
			ScreenEvents.remove(screen).register(s -> { open = null; picking = false; });
		});
	}

	private static boolean shown() { return open != null && ShulkerStylesClient.active(); }

	private static int x(Screen s) { ContainerScreenAccessor a = (ContainerScreenAccessor) s; return a.shulkerstyles$leftPos() + a.shulkerstyles$imageWidth() + 4; }
	private static int y(Screen s) { return ((ContainerScreenAccessor) s).shulkerstyles$topPos() + 4; }
	private static boolean over(Screen s, double mx, double my) { int x = x(s), y = y(s); return mx >= x && my >= y && mx < x + SIZE && my < y + SIZE; }

	private static void draw(Screen screen, GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		if (!shown()) return;
		int x = x(screen), y = y(screen);
		BoxInfo info = ShulkerStylesClient.BOXES.get(open);
		// A slot like the inventory's; all yellow while waiting for an item, with a hint under it.
		g.fill(x, y, x + SIZE, y + SIZE, DARK);
		g.fill(x + 1, y + 1, x + SIZE, y + SIZE, picking ? PICKING : LIGHT);
		g.fill(x + 1, y + 1, x + SIZE - 1, y + SIZE - 1, picking ? PICKING_BG : (over(screen, mouseX, mouseY) ? 0xFFA8A8A8 : BG));
		ItemStack icon = info == null ? ItemStack.EMPTY : ClientIcons.stack(info.icon());
		if (!icon.isEmpty()) g.item(icon, x + 3, y + 3);
		else g.centeredText(Minecraft.getInstance().font, Component.literal("?"), x + SIZE / 2, y + 7, 0xFFE0E0E0);

		var font = Minecraft.getInstance().font;
		if (picking) {
			// Text under the slot (a tooltip would be hidden by the item tooltips while choosing).
			g.text(font, "Now click", x - 4, y + SIZE + 3, PICKING);
			g.text(font, "an item", x - 4, y + SIZE + 13, PICKING);
		} else {
			g.text(font, "Icon", x + (SIZE - font.width("Icon")) / 2, y + SIZE + 3, 0xFFE0E0E0);
		}
		if (!picking && over(screen, mouseX, mouseY)) {
			List<Component> lines = new ArrayList<>();
			Component what = icon.isEmpty() ? Component.literal("none (the box is empty)") : icon.getHoverName();
			lines.add(Component.literal("Label icon: ").append(what));
			lines.add(Component.literal(info != null && info.iconChosen() ? "Chosen" : "Automatic: the item it holds most of").withStyle(ChatFormatting.GRAY));
			lines.add(Component.literal("Click, then click any item to choose it").withStyle(ChatFormatting.DARK_GRAY));
			lines.add(Component.literal("Click holding an item: use that one").withStyle(ChatFormatting.DARK_GRAY));
			lines.add(Component.literal("Right-click: back to automatic").withStyle(ChatFormatting.DARK_GRAY));
			g.setComponentTooltipForNextFrame(Minecraft.getInstance().font, lines, mouseX, mouseY);
		}
	}

	/** False cancels the click (it was ours). */
	private static boolean click(Screen screen, MouseButtonEvent e) {
		if (!shown()) return true;
		if (over(screen, e.x(), e.y())) {
			// 26.x numbers mouse buttons like SDL (left 1, right 3): use the constants, never 0/1.
			if (e.button() == InputConstants.MOUSE_BUTTON_RIGHT) { send(""); picking = false; return false; }
			if (e.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
			ItemStack carried = ((ShulkerBoxScreen) screen).getMenu().getCarried();
			if (!carried.isEmpty()) { send(id(carried)); picking = false; }
			else picking = !picking;
			return false;
		}
		if (!picking) return true;
		picking = false;
		Slot hovered = ((ContainerScreenAccessor) screen).shulkerstyles$hoveredSlot();
		if (hovered != null && hovered.hasItem()) send(id(hovered.getItem()));
		return false;   // the click only picked (or cancelled picking); it doesn't move items
	}

	private static String id(ItemStack stack) { return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); }

	private static void send(String item) {
		if (open != null && Net.canSend(Protocol.SETICON)) Net.send(Protocol.SETICON, Protocol.setIcon(open, item));
	}
}
