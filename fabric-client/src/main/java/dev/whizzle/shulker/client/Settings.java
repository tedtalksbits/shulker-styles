package dev.whizzle.shulker.client;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** The player's own settings, in {@code config/shulkerstyles.properties}. Changed in game: {@code /shulkermod}. */
public final class Settings {
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("shulkerstyles.properties");
	public static final int MIN_DISTANCE = 4, MAX_DISTANCE = 64;

	/** Icons on placed boxes, and the name over the box you look at. */
	public static boolean labels = true;
	/** Labels within this many blocks (patterns show as far as the box does). */
	public static int labelDistance = 16;
	/** A small icon on box items in inventories and the hotbar. */
	public static boolean inventoryIcons = true;

	private Settings() {}

	public static void load() {
		if (!Files.exists(FILE)) return;
		Properties p = new Properties();
		try (Reader in = Files.newBufferedReader(FILE)) { p.load(in); }
		catch (IOException e) { ShulkerStylesClient.LOG.warn("Couldn't read {}: {}", FILE, e.toString()); return; }
		labels = Boolean.parseBoolean(p.getProperty("labels", "true"));
		inventoryIcons = Boolean.parseBoolean(p.getProperty("inventory-icons", "true"));
		try { labelDistance = Math.clamp(Integer.parseInt(p.getProperty("label-distance", "16").trim()), MIN_DISTANCE, MAX_DISTANCE); }
		catch (NumberFormatException e) { labelDistance = 16; }
	}

	public static void save() {
		Properties p = new Properties();
		p.setProperty("labels", Boolean.toString(labels));
		p.setProperty("label-distance", Integer.toString(labelDistance));
		p.setProperty("inventory-icons", Boolean.toString(inventoryIcons));
		try (Writer out = Files.newBufferedWriter(FILE)) { p.store(out, "Shulker Styles client settings (change them in game: /shulkermod)"); }
		catch (IOException e) { ShulkerStylesClient.LOG.warn("Couldn't write {}: {}", FILE, e.toString()); }
	}
}
