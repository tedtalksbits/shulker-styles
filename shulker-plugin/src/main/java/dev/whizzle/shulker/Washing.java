package dev.whizzle.shulker;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Using a patterned box on a water cauldron washes off its top pattern layer and uses one level of water, as vanilla
 * does for banners. Only once all layers are gone does vanilla's own wash (dyed box to plain box) apply. Sneaking
 * places the box as usual.
 */
public final class Washing implements Listener {
    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() == null) return;
        Block cauldron = e.getClickedBlock();
        if (cauldron == null || cauldron.getType() != Material.WATER_CAULDRON || e.useInteractedBlock() == Event.Result.DENY) return;
        Player p = e.getPlayer();
        if (p.isSneaking()) return;
        ItemStack item = e.getItem();
        if (!Styles.isBox(item) || Styles.patterns(item).isEmpty()) return;
        e.setCancelled(true);
        p.getInventory().setItem(e.getHand(), washed(item));
        lower(cauldron);
        p.swingHand(e.getHand());
    }

    /** A copy of {@code box} with its top pattern layer removed. */
    public static ItemStack washed(ItemStack box) {
        List<Pattern> patterns = Styles.patterns(box);
        ItemStack out = box.clone();
        Styles.setPatterns(out, patterns.subList(0, patterns.size() - 1));
        return out;
    }

    public static void lower(Block cauldron) {
        if (!(cauldron.getBlockData() instanceof Levelled l)) return;
        if (l.getLevel() <= 1) cauldron.setType(Material.CAULDRON);
        else { l.setLevel(l.getLevel() - 1); cauldron.setBlockData(l); }
    }
}
