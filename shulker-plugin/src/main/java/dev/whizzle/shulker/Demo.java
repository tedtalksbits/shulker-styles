package dev.whizzle.shulker;

import dev.whizzle.shulker.Styles.Style;
import net.kyori.adventure.text.Component;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /shulkerstyles demo} (creative only): a wall of example boxes in front of the player, with patterns,
 * contents, names and one chosen icon, a water cauldron for washing, and a kit for trying the recipe yourself.
 */
public final class Demo {
    private Demo() {}

    /** One example box: colour, patterns, name ("" none), contents, chosen icon (null: automatic). */
    record Example(Material box, List<Pattern> patterns, String name, List<ItemStack> contents, String icon) {}

    static Pattern p(DyeColor c, PatternType t) { return new Pattern(c, t); }
    static List<ItemStack> items(Object... typeAmount) {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < typeAmount.length; i += 2) out.add(new ItemStack((Material) typeAmount[i], (Integer) typeAmount[i + 1]));
        return out;
    }

    /** Top row first, left to right as the player sees them. */
    static final List<Example> WALL = List.of(
        new Example(Material.LIME_SHULKER_BOX, List.of(p(DyeColor.RED, PatternType.CREEPER)), "Mob drops",
            items(Material.GUNPOWDER, 64, Material.BONE, 20, Material.STRING, 12), null),
        new Example(Material.WHITE_SHULKER_BOX, List.of(p(DyeColor.BLUE, PatternType.BORDER), p(DyeColor.LIGHT_BLUE, PatternType.RHOMBUS)), "Diamonds",
            items(Material.DIAMOND, 30, Material.EMERALD, 8), null),
        new Example(Material.YELLOW_SHULKER_BOX, List.of(p(DyeColor.BLACK, PatternType.HALF_HORIZONTAL)), "Farm",
            items(Material.WHEAT, 64, Material.CARROT, 40, Material.POTATO, 40), null),
        new Example(Material.LIGHT_BLUE_SHULKER_BOX, List.of(p(DyeColor.WHITE, PatternType.FLOWER)), "",
            items(Material.STRING, 64, Material.WHITE_WOOL, 16), null),
        new Example(Material.RED_SHULKER_BOX, List.of(p(DyeColor.WHITE, PatternType.SKULL)), "Redstone",
            items(Material.REDSTONE, 64, Material.REPEATER, 10, Material.COMPARATOR, 6), null),
        new Example(Material.PURPLE_SHULKER_BOX, List.of(p(DyeColor.YELLOW, PatternType.STRIPE_TOP), p(DyeColor.YELLOW, PatternType.STRIPE_BOTTOM)), "Metals",
            items(Material.IRON_INGOT, 64, Material.GOLD_INGOT, 20, Material.COPPER_INGOT, 64), "minecraft:iron_ingot"),
        new Example(Material.GRAY_SHULKER_BOX, List.of(p(DyeColor.LIGHT_BLUE, PatternType.GRADIENT)), "Stone",
            items(Material.COBBLESTONE, 64, Material.STONE, 64, Material.DEEPSLATE, 32), null),
        new Example(Material.ORANGE_SHULKER_BOX, List.of(p(DyeColor.BLACK, PatternType.GLOBE)), "Wood",
            items(Material.OAK_LOG, 64, Material.SPRUCE_LOG, 32), null));

    public static final int WIDTH = 4, HEIGHT = 2, DISTANCE = 3;

    /** The wall's bottom-left box (as the player sees it), {@link #DISTANCE} blocks in front of the player's feet. */
    static Block origin(Location feet, BlockFace facing) {
        BlockFace left = leftOf(facing);
        return feet.getBlock().getRelative(facing, DISTANCE).getRelative(left, WIDTH / 2);
    }

    static BlockFace leftOf(BlockFace facing) {
        return switch (facing) {
            case NORTH -> BlockFace.WEST; case SOUTH -> BlockFace.EAST; case EAST -> BlockFace.NORTH; default -> BlockFace.SOUTH;
        };
    }

    /** The blocks the demo uses: the wall, plus the cauldron one block to its right. */
    static List<Block> footprint(Block origin, BlockFace facing) {
        BlockFace right = leftOf(facing).getOppositeFace();
        List<Block> out = new ArrayList<>();
        for (int row = 0; row < HEIGHT; row++) for (int col = 0; col < WIDTH; col++) out.add(origin.getRelative(right, col).getRelative(BlockFace.UP, row));
        out.add(cauldron(origin, facing));
        return out;
    }

    static Block cauldron(Block origin, BlockFace facing) { return origin.getRelative(leftOf(facing).getOppositeFace(), WIDTH + 1); }

    /** Builds the wall; null if done, or why not (nothing is changed then). */
    public static String build(Location feet, BlockFace facing) {
        Block origin = origin(feet, facing);
        for (Block b : footprint(origin, facing))
            if (!b.getType().isAir()) return "Not enough room: the demo needs an empty space " + (WIDTH + 2) + " wide and " + HEIGHT + " high, " + DISTANCE + " blocks in front of you.";
        BlockFace right = leftOf(facing).getOppositeFace();
        for (int i = 0; i < WALL.size(); i++) {
            int row = HEIGHT - 1 - i / WIDTH, col = i % WIDTH;
            place(origin.getRelative(right, col).getRelative(BlockFace.UP, row), WALL.get(i));
        }
        Block c = cauldron(origin, facing);
        c.setType(Material.WATER_CAULDRON);
        if (c.getBlockData() instanceof Levelled l) { l.setLevel(l.getMaximumLevel()); c.setBlockData(l); }
        return null;
    }

    static void place(Block b, Example e) {
        b.setType(e.box());
        if (b.getBlockData() instanceof Directional d) { d.setFacing(BlockFace.UP); b.setBlockData(d); }
        ShulkerBox box = (ShulkerBox) b.getState(false);
        for (int i = 0; i < e.contents().size(); i++) box.getInventory().setItem(i, e.contents().get(i).clone());
        Styles.store(b, new Style(e.patterns(), e.icon()));
        if (!e.name().isEmpty()) {
            ShulkerBox named = (ShulkerBox) b.getState();
            named.customName(Component.text(e.name()));
            named.update(true, false);
        }
    }

    /** Banners and boxes for trying the recipe, the cauldron and the icon slot. */
    public static List<ItemStack> kit() {
        List<ItemStack> out = new ArrayList<>();
        out.add(banner(Material.WHITE_BANNER, p(DyeColor.RED, PatternType.CREEPER)));
        out.add(banner(Material.WHITE_BANNER, p(DyeColor.BLUE, PatternType.BORDER), p(DyeColor.BLUE, PatternType.CROSS)));
        out.add(banner(Material.BLACK_BANNER, p(DyeColor.ORANGE, PatternType.GRADIENT_UP), p(DyeColor.YELLOW, PatternType.CIRCLE)));
        out.add(banner(Material.WHITE_BANNER, p(DyeColor.GREEN, PatternType.STRIPE_DOWNRIGHT), p(DyeColor.GREEN, PatternType.STRIPE_DOWNLEFT), p(DyeColor.BLACK, PatternType.BORDER)));
        out.add(new ItemStack(Material.SHULKER_BOX));
        out.add(new ItemStack(Material.CYAN_SHULKER_BOX));
        out.add(new ItemStack(Material.MAGENTA_SHULKER_BOX));
        ItemStack ready = new ItemStack(Material.BLUE_SHULKER_BOX);
        Styles.setPatterns(ready, List.of(p(DyeColor.WHITE, PatternType.STRIPE_TOP), p(DyeColor.WHITE, PatternType.CREEPER), p(DyeColor.YELLOW, PatternType.BORDER)));
        out.add(ready);
        return out;
    }

    static ItemStack banner(Material type, Pattern... patterns) {
        ItemStack b = new ItemStack(type);
        Styles.setPatterns(b, List.of(patterns));
        return b;
    }

    /** Gives the kit; whatever doesn't fit drops at the player's feet. */
    public static void give(Player p) {
        for (ItemStack overflow : p.getInventory().addItem(kit().toArray(ItemStack[]::new)).values())
            p.getWorld().dropItemNaturally(p.getLocation(), overflow);
    }
}
