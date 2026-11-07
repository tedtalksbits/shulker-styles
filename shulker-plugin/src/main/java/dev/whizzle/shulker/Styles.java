package dev.whizzle.shulker;

import dev.whizzle.shulker.model.BoxInfo;
import dev.whizzle.shulker.model.Icons;
import dev.whizzle.shulker.model.Layer;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BannerPatternLayers;
import io.papermc.paper.datacomponent.item.ItemContainerContents;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Where a box's style lives. On the item: vanilla's {@code banner_patterns} component, plus the chosen label icon in
 * the item's persistent data. On a placed box: the block entity's persistent data (patterns as {@link Layer#toText}
 * text, the chosen icon as an item id). The plugin copies one to the other when a box is placed and when it drops.
 */
public final class Styles {
    private Styles() {}

    public static final NamespacedKey LAYERS = new NamespacedKey("shulkerstyles", "layers");
    public static final NamespacedKey ICON = new NamespacedKey("shulkerstyles", "icon");
    static Logger log = Logger.getLogger("ShulkerStyles");

    /** A box's style: its pattern layers and the label icon the player chose (null: automatic). */
    public record Style(List<Pattern> patterns, String icon) {
        public static final Style NONE = new Style(List.of(), null);
        public Style { patterns = List.copyOf(patterns); }
        public boolean isEmpty() { return patterns.isEmpty() && icon == null; }
    }

    public static boolean isBox(Material m) { return m != null && Tag.SHULKER_BOXES.isTagged(m); }
    public static boolean isBox(ItemStack item) { return item != null && isBox(item.getType()); }

    // ---- items ----

    public static List<Pattern> patterns(ItemStack item) {
        BannerPatternLayers layers = item == null ? null : item.getData(DataComponentTypes.BANNER_PATTERNS);
        return layers == null ? List.of() : layers.patterns();
    }

    public static void setPatterns(ItemStack item, List<Pattern> patterns) {
        if (patterns.isEmpty()) item.unsetData(DataComponentTypes.BANNER_PATTERNS);
        else item.setData(DataComponentTypes.BANNER_PATTERNS, BannerPatternLayers.bannerPatternLayers(patterns));
    }

    public static String chosenIcon(ItemStack item) {
        return item == null ? null : item.getPersistentDataContainer().get(ICON, PersistentDataType.STRING);
    }

    public static void setChosenIcon(ItemStack item, String icon) {
        item.editPersistentDataContainer(pdc -> { if (icon == null) pdc.remove(ICON); else pdc.set(ICON, PersistentDataType.STRING, icon); });
    }

    public static Style of(ItemStack item) {
        if (!isBox(item)) return Style.NONE;
        return new Style(patterns(item), chosenIcon(item));
    }

    /** Puts {@code style} on {@code item}, keeping anything the item already has. True if it changed the item. */
    public static boolean apply(ItemStack item, Style style) {
        boolean changed = false;
        if (!style.patterns().isEmpty() && patterns(item).isEmpty()) { setPatterns(item, style.patterns()); changed = true; }
        if (style.icon() != null && chosenIcon(item) == null) { setChosenIcon(item, style.icon()); changed = true; }
        return changed;
    }

    // ---- placed boxes ----

    public static Style of(BlockState state) {
        if (!(state instanceof ShulkerBox box)) return Style.NONE;
        PersistentDataContainer pdc = box.getPersistentDataContainer();
        String text = pdc.get(LAYERS, PersistentDataType.STRING);
        List<Pattern> patterns = List.of();
        if (text != null) {
            try { patterns = toPatterns(Layer.fromText(text)); }
            catch (IllegalArgumentException e) { log.warning("Unreadable patterns at " + state.getLocation() + ": " + e.getMessage()); }
        }
        return new Style(patterns, pdc.get(ICON, PersistentDataType.STRING));
    }

    public static Style of(Block block) { return isBox(block.getType()) ? of(block.getState(false)) : Style.NONE; }

    /** Records {@code style} on a placed box (replacing what it had). */
    public static void store(Block block, Style style) {
        if (!(block.getState(false) instanceof ShulkerBox box)) return;
        PersistentDataContainer pdc = box.getPersistentDataContainer();
        if (style.patterns().isEmpty()) pdc.remove(LAYERS); else pdc.set(LAYERS, PersistentDataType.STRING, Layer.toText(toLayers(style.patterns())));
        if (style.icon() == null) pdc.remove(ICON); else pdc.set(ICON, PersistentDataType.STRING, style.icon());
    }

    /** Sets (item id) or clears (null) the chosen label icon of a placed box, keeping its patterns. */
    public static void storeIcon(Block block, String icon) {
        if (!(block.getState(false) instanceof ShulkerBox box)) return;
        PersistentDataContainer pdc = box.getPersistentDataContainer();
        if (icon == null) pdc.remove(ICON); else pdc.set(ICON, PersistentDataType.STRING, icon);
    }

    /** Everything the mod draws for the box at {@code block}; null if it isn't a shulker box. */
    public static BoxInfo info(Block block) {
        if (!isBox(block.getType()) || !(block.getState(false) instanceof ShulkerBox box)) return null;
        Style style = of(box);
        String name = box.customName() == null ? "" : PlainTextComponentSerializer.plainText().serialize(box.customName());
        if (name.length() > 256) name = name.substring(0, 256);
        String icon = style.icon() != null ? style.icon() : autoIcon(box.getInventory());
        return new BoxInfo(toLayers(style.patterns()), name, icon, style.icon() != null);
    }

    public static String autoIcon(Inventory inv) {
        List<Icons.Slot> slots = new ArrayList<>(inv.getSize());
        for (ItemStack it : inv.getContents()) slots.add(it == null || it.getType().isAir() ? null : new Icons.Slot(it.getType().getKey().toString(), it.getAmount()));
        return Icons.mostCommon(slots);
    }

    /** Whether a dropped box item holds exactly what the placed box holds (to tell which box an item came from). */
    public static boolean sameContents(ItemStack item, Inventory inv) {
        ItemContainerContents c = item.getData(DataComponentTypes.CONTAINER);
        List<ItemStack> items = c == null ? List.of() : c.contents();
        ItemStack[] slots = inv.getContents();
        for (int i = 0; i < Math.max(items.size(), slots.length); i++) {
            ItemStack a = i < items.size() ? items.get(i) : null, b = i < slots.length ? slots[i] : null;
            boolean ea = a == null || a.getType().isAir(), eb = b == null || b.getType().isAir();
            if (ea != eb || (!ea && !a.equals(b))) return false;
        }
        return true;
    }

    // ---- patterns <-> layers ----

    private static Registry<PatternType> patternRegistry() { return RegistryAccess.registryAccess().getRegistry(RegistryKey.BANNER_PATTERN); }

    public static List<Layer> toLayers(List<Pattern> patterns) {
        List<Layer> out = new ArrayList<>(patterns.size());
        Registry<PatternType> registry = patternRegistry();
        for (Pattern p : patterns) {
            if (out.size() == Layer.MAX_LAYERS) break;
            NamespacedKey key = registry.getKey(p.getPattern());
            if (key != null) out.add(new Layer(key.toString(), p.getColor().ordinal()));
        }
        return out;
    }

    public static List<Pattern> toPatterns(List<Layer> layers) {
        List<Pattern> out = new ArrayList<>(layers.size());
        Registry<PatternType> registry = patternRegistry();
        for (Layer l : layers) {
            PatternType type = registry.get(NamespacedKey.fromString(l.pattern()));
            if (type == null) { log.warning("Unknown banner pattern " + l.pattern() + " (skipped)"); continue; }
            out.add(new Pattern(DyeColor.values()[l.color()], type));
        }
        return out;
    }
}
