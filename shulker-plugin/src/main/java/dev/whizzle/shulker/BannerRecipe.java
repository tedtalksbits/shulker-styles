package dev.whizzle.shulker;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.banner.Pattern;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * Shulker box + banner (shapeless) gives the box the banner's pattern layers and uses up the banner, as a shield does
 * in vanilla. The box keeps its own colour; the banner's base colour is ignored. A box that already has patterns, or a
 * banner without any, makes no result. Works in crafting tables, the 2x2 grid and crafters.
 */
public final class BannerRecipe implements Listener {
    public static final NamespacedKey KEY = new NamespacedKey("shulkerstyles", "banner_on_box");

    public static void register() {
        if (Bukkit.getRecipe(KEY) != null) return;
        ShapelessRecipe r = new ShapelessRecipe(KEY, new ItemStack(Material.SHULKER_BOX));
        r.addIngredient(new RecipeChoice.MaterialChoice(new ArrayList<>(Tag.ITEMS_SHULKER_BOXES.getValues())));
        r.addIngredient(new RecipeChoice.MaterialChoice(new ArrayList<>(Tag.ITEMS_BANNERS.getValues())));
        Bukkit.addRecipe(r);
    }

    static boolean ours(Recipe recipe) { return recipe instanceof Keyed k && KEY.equals(k.getKey()); }

    /** The patterned box for this grid, or null when it doesn't qualify. */
    public static ItemStack result(ItemStack[] grid) {
        ItemStack box = null, banner = null;
        for (ItemStack it : grid) {
            if (it == null || it.getType().isAir()) continue;
            if (Styles.isBox(it)) { if (box != null) return null; box = it; }
            else if (Tag.ITEMS_BANNERS.isTagged(it.getType())) { if (banner != null) return null; banner = it; }
            else return null;
        }
        if (box == null || banner == null) return null;
        List<Pattern> patterns = Styles.patterns(banner);
        if (patterns.isEmpty() || !Styles.patterns(box).isEmpty()) return null;
        ItemStack out = box.asOne();
        Styles.setPatterns(out, patterns.size() > dev.whizzle.shulker.model.Layer.MAX_LAYERS
            ? patterns.subList(0, dev.whizzle.shulker.model.Layer.MAX_LAYERS) : patterns);
        return out;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepare(PrepareItemCraftEvent e) {
        if (!ours(e.getRecipe())) return;
        e.getInventory().setResult(result(e.getInventory().getMatrix()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCrafter(CrafterCraftEvent e) {
        if (!ours(e.getRecipe())) return;
        if (!(e.getBlock().getState(false) instanceof org.bukkit.block.Crafter crafter)) { e.setCancelled(true); return; }
        ItemStack out = result(crafter.getInventory().getContents());
        if (out == null) e.setCancelled(true); else e.setResult(out);
    }
}
