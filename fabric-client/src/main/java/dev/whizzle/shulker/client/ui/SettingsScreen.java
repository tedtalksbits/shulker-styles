package dev.whizzle.shulker.client.ui;

import dev.whizzle.shulker.client.Settings;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** Each player's label settings. Changes apply at once and are saved when the screen closes. */
public final class SettingsScreen extends Screen {
	private static final int AQUA = 0xFF6FD3FF, GRAY = 0xFFB8C0CC;
	private final @Nullable Screen parent;

	public SettingsScreen(@Nullable Screen parent) {
		super(Component.literal("Shulker Styles"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int w = 220, x = (width - w) / 2, y = 44;
		addRenderableWidget(Button.builder(onOff("Labels on boxes", Settings.labels), b -> {
			Settings.labels = !Settings.labels;
			b.setMessage(onOff("Labels on boxes", Settings.labels));
		}).pos(x, y).size(w, 20).build());
		addRenderableWidget(new DistanceSlider(x, y + 24, w));
		addRenderableWidget(Button.builder(onOff("Icons on box items", Settings.inventoryIcons), b -> {
			Settings.inventoryIcons = !Settings.inventoryIcons;
			b.setMessage(onOff("Icons on box items", Settings.inventoryIcons));
		}).pos(x, y + 56).size(w, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).pos(x, height - 32).size(w, 20).build());
	}

	private static Component onOff(String what, boolean on) { return Component.literal(what + ": " + (on ? "On" : "Off")); }

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		g.centeredText(font, title, width / 2, 14, AQUA);
		g.centeredText(font, Component.literal("Labels: an icon on each box, the name over the box you look at."), width / 2, 28, GRAY);
		g.centeredText(font, Component.literal("Patterns always show."), width / 2, 44 + 82, GRAY);
	}

	@Override
	public void onClose() {
		Settings.save();
		minecraft.gui.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() { return false; }

	private static final class DistanceSlider extends AbstractSliderButton {
		DistanceSlider(int x, int y, int w) {
			super(x, y, w, 20, Component.empty(), (Settings.labelDistance - Settings.MIN_DISTANCE) / (double) (Settings.MAX_DISTANCE - Settings.MIN_DISTANCE));
			updateMessage();
		}

		private int current() { return (int) Math.round(Settings.MIN_DISTANCE + value * (Settings.MAX_DISTANCE - Settings.MIN_DISTANCE)); }

		@Override protected void updateMessage() { setMessage(Component.literal("Label distance: " + current() + " blocks")); }
		@Override protected void applyValue() { Settings.labelDistance = current(); }
	}
}
