package dev.whizzle.shulker;

import dev.whizzle.shulker.Styles.Style;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import io.papermc.paper.event.block.BlockBreakBlockEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps a box's style through every way it moves. Placing copies the style from the item to the block; anything
 * that turns the block back into an item puts it back:
 * <ul>
 * <li>a player breaking it: {@link BlockDropItemEvent} (creative breaks keep it through vanilla's own copy);</li>
 * <li>a piston breaking it: {@link BlockBreakBlockEvent};</li>
 * <li>explosions: remembered at the explosion, matched when the drop spawns ({@link ItemSpawnEvent});</li>
 * <li>anything else that drops it while the block is still there ({@code breakNaturally}, commands): the drop is
 * matched to the box by position, colour and contents.</li>
 * </ul>
 * Vanilla's shulker box loot table copies only name, contents, lock and loot table, which is why this is needed.
 */
public final class Keeper implements Listener {
    private final Plugin plugin;
    /** Patterned boxes an explosion is destroying this tick: their drops spawn after the blocks are gone. */
    private final Map<String, Pending> exploding = new HashMap<>();
    private long tick;

    record Pending(Material type, Style style, long tick) {}

    public Keeper(Plugin plugin) { this.plugin = plugin; }

    private static String key(World w, int x, int y, int z) { return w.getUID() + "/" + x + "/" + y + "/" + z; }
    private static String key(Block b) { return key(b.getWorld(), b.getX(), b.getY(), b.getZ()); }

    /** Called every tick: forgets explosion records whose drops never came. */
    public void tick() {
        tick++;
        if (!exploding.isEmpty()) exploding.values().removeIf(p -> tick - p.tick() > 2);
    }

    // ---- placing ----

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Style style = Styles.of(e.getItemInHand());
        if (!style.isEmpty() && Styles.isBox(e.getBlockPlaced().getType())) Styles.store(e.getBlockPlaced(), style);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent e) {
        Style style = Styles.of(e.getItem());
        if (style.isEmpty() || !(e.getBlock().getBlockData() instanceof Directional d)) return;
        Block target = e.getBlock().getRelative(d.getFacing());
        // A box already there means this dispense can't place one; don't style the box that was there.
        if (Styles.isBox(target.getType())) return;
        Material type = e.getItem().getType();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (target.getType() == type && Styles.of(target).isEmpty()) Styles.store(target, style);
        });
    }

    // ---- dropping ----

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerBreakDrops(BlockDropItemEvent e) {
        Style style = Styles.of(e.getBlockState());
        if (style.isEmpty()) return;
        for (Item item : e.getItems()) restore(item, style);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreaksBlock(BlockBreakBlockEvent e) {
        Style style = Styles.of(e.getBlock());
        if (style.isEmpty()) return;
        for (ItemStack drop : e.getDrops()) if (Styles.isBox(drop)) Styles.apply(drop, style);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) { remember(e.blockList()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) { remember(e.blockList()); }

    private void remember(List<Block> blocks) {
        for (Block b : blocks) {
            if (!Styles.isBox(b.getType())) continue;
            Style style = Styles.of(b);
            if (!style.isEmpty()) exploding.put(key(b), new Pending(b.getType(), style, tick));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent e) {
        ItemStack stack = e.getEntity().getItemStack();
        if (!Styles.isBox(stack) || !Styles.of(stack).isEmpty()) return;
        Block at = e.getLocation().getBlock();
        Pending p = exploding.get(key(at));
        if (p != null && p.type() == stack.getType()) {
            exploding.remove(key(at));
            restore(e.getEntity(), p.style());
            return;
        }
        // The block is still there while its drops spawn (breakNaturally, pistons, most commands).
        if (at.getType() == stack.getType() && at.getState(false) instanceof ShulkerBox box) {
            Style style = Styles.of((BlockState) box);
            if (!style.isEmpty() && Styles.sameContents(stack, box.getInventory())) restore(e.getEntity(), style);
        }
    }

    private static void restore(Item item, Style style) {
        ItemStack stack = item.getItemStack();
        if (Styles.isBox(stack) && Styles.apply(stack, style)) item.setItemStack(stack);
    }
}
