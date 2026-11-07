package dev.whizzle.shulker.selftest;

import dev.whizzle.shulker.Styles;
import dev.whizzle.shulker.Styles.Style;
import dev.whizzle.shulker.model.BoxInfo;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemContainerContents;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Crafter;
import org.bukkit.block.Dispenser;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Item;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Supplier;

/**
 * Development-only: checks on a real Paper server that a box's style survives every way it moves without a player
 * (crafter, dispenser, breakNaturally, piston, explosions), and that the protocol's answers read real blocks. Writes
 * report.txt ending "SELFTEST PASSED"; with -Dshulker.selftest.exit=true it stops the server when done.
 * A player breaking a box, the 2x2/3x3 grid, washing and Ctrl+middle-click need a real client: docs/TEST_PLAN.md.
 */
public final class SelfTestPlugin extends JavaPlugin {
    record Step(String name, Runnable action, int ticks, Supplier<String> check) {}

    private final List<Step> steps = new ArrayList<>();
    private final List<String> report = new ArrayList<>();
    private World w;
    private static final int Y = -60;
    private int failures;
    static final List<Pattern> PATTERNS = List.of(new Pattern(DyeColor.RED, PatternType.CREEPER), new Pattern(DyeColor.BLUE, PatternType.BORDER));
    static final String ICON = "minecraft:diamond";

    @Override public void onEnable() { getServer().getScheduler().runTaskLater(this, this::start, 40); }

    Block at(int x, int z) { return w.getBlockAt(x, Y, z); }

    static ItemStack styledBox(Material type) {
        ItemStack box = new ItemStack(type);
        Styles.setPatterns(box, PATTERNS);
        Styles.setChosenIcon(box, ICON);
        return box;
    }

    /** A placed box with the test style and some contents, as the plugin leaves it after a player places one. */
    Block placedBox(int x, int z, Material type) {
        Block b = at(x, z);
        b.setType(type);
        Styles.store(b, new Style(PATTERNS, ICON));
        ShulkerBox box = (ShulkerBox) b.getState(false);
        box.getInventory().setItem(0, new ItemStack(Material.COBBLESTONE, 40));
        box.getInventory().setItem(5, new ItemStack(Material.IRON_INGOT, 7));
        return b;
    }

    List<Item> itemsNear(int x, int z, double r) {
        Location c = at(x, z).getLocation().add(0.5, 0.5, 0.5);
        return new ArrayList<>(w.getNearbyEntitiesByType(Item.class, c, r));
    }

    void clearItems(int x, int z) { for (Item i : itemsNear(x, z, 6)) i.remove(); }

    /** Problem with the dropped box near (x, z), or null if exactly one dropped with the full style and contents. */
    String droppedStyled(int x, int z, Material type, boolean contents) {
        List<Item> boxes = itemsNear(x, z, 4).stream().filter(i -> i.getItemStack().getType() == type).toList();
        if (boxes.size() != 1) return boxes.size() + " " + type.getKey().getKey() + " items dropped, expected 1";
        ItemStack s = boxes.getFirst().getItemStack();
        if (!Styles.patterns(s).equals(PATTERNS)) return "dropped box patterns " + Styles.patterns(s) + ", expected " + PATTERNS;
        if (!ICON.equals(Styles.chosenIcon(s))) return "dropped box icon " + Styles.chosenIcon(s) + ", expected " + ICON;
        if (contents) {
            ItemContainerContents c = s.getData(DataComponentTypes.CONTAINER);
            if (c == null || c.contents().size() < 6 || c.contents().get(0).getAmount() != 40 || c.contents().get(5).getType() != Material.IRON_INGOT)
                return "dropped box lost its contents: " + (c == null ? "none" : c.contents());
        }
        return null;
    }

    static String all(String... problems) { for (String p : problems) if (p != null) return p; return null; }

    void start() {
        w = getServer().getWorlds().getFirst();
        for (int cx = -2; cx <= 16; cx++) for (int cz = -2; cz <= 2; cz++) w.setChunkForceLoaded(cx, cz, true);
        for (int x = -10; x <= 250; x++) for (int z = -10; z <= 10; z++) {
            w.getBlockAt(x, Y - 1, z).setType(Material.STONE);
            for (int y = Y; y < Y + 5; y++) w.getBlockAt(x, y, z).setType(Material.AIR);
        }

        // 1. Crafter: box + patterned banner gives the patterned box; a plain banner makes nothing.
        int a = 0;
        steps.add(new Step("crafter: box + banner -> box with the banner's patterns, banner used up", () -> {
            Block c = at(a, 0);
            c.setType(Material.CRAFTER);
            org.bukkit.block.data.type.Crafter data = (org.bukkit.block.data.type.Crafter) c.getBlockData();
            data.setOrientation(org.bukkit.block.Orientation.EAST_UP);
            c.setBlockData(data);
            ItemStack banner = new ItemStack(Material.WHITE_BANNER);
            Styles.setPatterns(banner, PATTERNS);
            Crafter crafter = (Crafter) c.getState(false);
            crafter.getInventory().setItem(0, new ItemStack(Material.LIME_SHULKER_BOX));
            crafter.getInventory().setItem(4, banner);
            at(a, -1).setType(Material.REDSTONE_BLOCK);
        }, 12, () -> {
            List<Item> out = itemsNear(a + 1, 0, 2.5).stream().filter(i -> i.getItemStack().getType() == Material.LIME_SHULKER_BOX).toList();
            if (out.size() != 1) return out.size() + " boxes came out, expected 1";
            if (!Styles.patterns(out.getFirst().getItemStack()).equals(PATTERNS)) return "patterns " + Styles.patterns(out.getFirst().getItemStack());
            Crafter crafter = (Crafter) at(a, 0).getState(false);
            return crafter.getInventory().isEmpty() ? null : "crafter still holds " + Arrays.toString(crafter.getInventory().getContents());
        }));
        int b = 12;
        steps.add(new Step("crafter: plain banner -> nothing crafted, items stay", () -> {
            Block c = at(b, 0);
            c.setType(Material.CRAFTER);
            Crafter crafter = (Crafter) c.getState(false);
            crafter.getInventory().setItem(0, new ItemStack(Material.SHULKER_BOX));
            crafter.getInventory().setItem(1, new ItemStack(Material.RED_BANNER));
            at(b, -1).setType(Material.REDSTONE_BLOCK);
        }, 12, () -> {
            Crafter crafter = (Crafter) at(b, 0).getState(false);
            boolean kept = crafter.getInventory().getItem(0) != null && crafter.getInventory().getItem(1) != null;
            return all(itemsNear(b, 0, 3).isEmpty() ? null : "something came out: " + itemsNear(b, 0, 3).stream().map(i -> i.getItemStack() + " at " + i.getLocation().toVector()).toList(), kept ? null : "the crafter used up its items");
        }));

        // 2. Dispenser places a styled box; breaking it keeps the style.
        int d = 20;
        steps.add(new Step("dispenser: places a styled box, style recorded on the block", () -> {
            Block disp = at(d, 0);
            disp.setType(Material.DISPENSER);
            Directional dir = (Directional) disp.getBlockData();
            dir.setFacing(BlockFace.EAST);
            disp.setBlockData(dir);
            ((Dispenser) disp.getState(false)).getInventory().addItem(styledBox(Material.ORANGE_SHULKER_BOX));
            ((Dispenser) disp.getState(false)).dispense();
        }, 4, () -> {
            Block t = at(d + 1, 0);
            if (t.getType() != Material.ORANGE_SHULKER_BOX) return "target is " + t.getType();
            Style s = Styles.of(t);
            return s.patterns().equals(PATTERNS) && ICON.equals(s.icon()) ? null : "block style " + s;
        }));
        steps.add(new Step("dispenser: the dispensed box broken (breakNaturally) drops with its style", () -> {
            clearItems(d + 1, 0);
            at(d + 1, 0).breakNaturally();
        }, 3, () -> droppedStyled(d + 1, 0, Material.ORANGE_SHULKER_BOX, false)));
        int d2 = 30;
        steps.add(new Step("dispenser: into a box that's already there -> that box stays plain", () -> {
            at(d2 + 1, 0).setType(Material.SHULKER_BOX);
            Block disp = at(d2, 0);
            disp.setType(Material.DISPENSER);
            Directional dir = (Directional) disp.getBlockData();
            dir.setFacing(BlockFace.EAST);
            disp.setBlockData(dir);
            ((Dispenser) disp.getState(false)).getInventory().addItem(styledBox(Material.SHULKER_BOX));
            ((Dispenser) disp.getState(false)).dispense();
        }, 4, () -> Styles.of(at(d2 + 1, 0)).isEmpty() ? null : "the box already there got a style"));

        // 3. breakNaturally (the loot table path) with contents.
        int e = 40;
        steps.add(new Step("breakNaturally: drops with style and contents", () -> { placedBox(e, 0, Material.BLUE_SHULKER_BOX); at(e, 0).breakNaturally(); },
            3, () -> droppedStyled(e, 0, Material.BLUE_SHULKER_BOX, true)));

        // 4. Piston breaks it.
        int p = 50;
        steps.add(new Step("piston: breaks the box, drop keeps style and contents", () -> {
            placedBox(p + 1, 0, Material.RED_SHULKER_BOX);
            Block piston = at(p, 0);
            piston.setType(Material.PISTON);
            Directional dir = (Directional) piston.getBlockData();
            dir.setFacing(BlockFace.EAST);
            piston.setBlockData(dir);
            at(p - 1, 0).setType(Material.REDSTONE_BLOCK);
        }, 10, () -> all(at(p + 1, 0).getType() == Material.RED_SHULKER_BOX ? "the box is still there" : null, droppedStyled(p + 1, 0, Material.RED_SHULKER_BOX, true))));

        // 5. Explosions: TNT (entity) and a sourceless explosion (block).
        int t = 80;
        steps.add(new Step("TNT: blows up the box, drop keeps style and contents", () -> {
            placedBox(t, 0, Material.GREEN_SHULKER_BOX);
            TNTPrimed tnt = w.spawn(at(t, 2).getLocation().add(0.5, 0, 0.5), TNTPrimed.class);
            tnt.setFuseTicks(1);
        }, 8, () -> all(at(t, 0).getType().isAir() ? null : "box still there: " + at(t, 0).getType(), droppedStyled(t, 0, Material.GREEN_SHULKER_BOX, true))));
        int x = 110;
        steps.add(new Step("explosion (no entity): drop keeps style and contents", () -> {
            placedBox(x, 0, Material.PURPLE_SHULKER_BOX);
            w.createExplosion(at(x, 2).getLocation().add(0.5, 0, 0.5), 4f, false, true);
        }, 4, () -> all(at(x, 0).getType().isAir() ? null : "box still there", droppedStyled(x, 0, Material.PURPLE_SHULKER_BOX, true))));
        int x2 = 140;
        steps.add(new Step("explosion: two styled boxes side by side each keep their own style", () -> {
            placedBox(x2, 0, Material.CYAN_SHULKER_BOX);
            Block other = at(x2 + 1, 0);
            other.setType(Material.CYAN_SHULKER_BOX);
            Styles.store(other, new Style(List.of(new Pattern(DyeColor.YELLOW, PatternType.STRIPE_TOP)), null));
            w.createExplosion(at(x2, 2).getLocation().add(1, 0, 0.5), 4f, false, true);
        }, 4, () -> {
            List<Item> boxes = itemsNear(x2, 0, 5).stream().filter(i -> i.getItemStack().getType() == Material.CYAN_SHULKER_BOX).toList();
            long full = boxes.stream().filter(i -> Styles.patterns(i.getItemStack()).equals(PATTERNS)).count();
            long stripe = boxes.stream().filter(i -> Styles.patterns(i.getItemStack()).size() == 1 && Styles.chosenIcon(i.getItemStack()) == null).count();
            return boxes.size() == 2 && full == 1 && stripe == 1 ? null : "drops " + boxes.stream().map(i -> Styles.of(i.getItemStack())).toList();
        }));

        // 6. A plain box dropped stays plain (nothing invented).
        int q = 170;
        steps.add(new Step("plain box: breakNaturally drops a plain box", () -> { at(q, 0).setType(Material.SHULKER_BOX); at(q, 0).breakNaturally(); }, 3, () -> {
            List<Item> boxes = itemsNear(q, 0, 3).stream().filter(i -> Styles.isBox(i.getItemStack())).toList();
            return boxes.size() == 1 && Styles.of(boxes.getFirst().getItemStack()).isEmpty() ? null : "drops " + boxes.stream().map(i -> Styles.of(i.getItemStack())).toList();
        }));

        // 7. What the protocol answers about a real box.
        int r = 180;
        steps.add(new Step("info: patterns, name, chosen icon", () -> {
            placedBox(r, 0, Material.YELLOW_SHULKER_BOX);
            ShulkerBox box = (ShulkerBox) at(r, 0).getState();
            box.customName(Component.text("Redstone"));
            box.update();
        }, 2, () -> {
            BoxInfo i = Styles.info(at(r, 0));
            if (i == null) return "no info";
            return all(i.layers().size() == 2 && i.layers().getFirst().pattern().equals("minecraft:creeper") && i.layers().getFirst().colorName().equals("red") ? null : "layers " + i.layers(),
                i.name().equals("Redstone") ? null : "name '" + i.name() + "'",
                i.icon().equals(ICON) && i.iconChosen() ? null : "icon " + i.icon() + " chosen " + i.iconChosen());
        }));
        int r2 = 185;
        steps.add(new Step("info: automatic icon is the most common item; changes with the contents", () -> placedBox(r2, 0, Material.BLACK_SHULKER_BOX), 2, () -> {
            Styles.store(at(r2, 0), new Style(PATTERNS, null));
            BoxInfo i = Styles.info(at(r2, 0));
            if (i == null || !i.icon().equals("minecraft:cobblestone") || i.iconChosen()) return "icon " + (i == null ? null : i.icon());
            ShulkerBox box = (ShulkerBox) at(r2, 0).getState(false);
            box.getInventory().addItem(new ItemStack(Material.IRON_INGOT, 64));
            BoxInfo j = Styles.info(at(r2, 0));
            return j.icon().equals("minecraft:iron_ingot") ? null : "after adding iron: " + j.icon();
        }));
        int r3 = 190;
        steps.add(new Step("icon slot: setting the icon keeps the patterns; clearing goes back to automatic", () -> placedBox(r3, 0, Material.PINK_SHULKER_BOX), 1, () -> {
            Styles.storeIcon(at(r3, 0), "minecraft:emerald");
            BoxInfo i = Styles.info(at(r3, 0));
            if (!i.icon().equals("minecraft:emerald") || !i.iconChosen() || i.layers().size() != 2) return "after setting: " + i;
            Styles.storeIcon(at(r3, 0), null);
            BoxInfo j = Styles.info(at(r3, 0));
            return j.icon().equals("minecraft:cobblestone") && !j.iconChosen() && j.layers().size() == 2 ? null : "after clearing: " + j;
        }));
        steps.add(new Step("info: not a box -> nothing", () -> at(r2 + 2, 0).setType(Material.CHEST), 1,
            () -> Styles.info(at(r2 + 2, 0)) == null ? null : "a chest has info"));

        // 7b. The demo wall: built where there's room, refused where there isn't.
        int dm = 215;
        steps.add(new Step("demo: builds the wall of example boxes and a full water cauldron", () -> {
            String problem = dev.whizzle.shulker.Demo.build(at(dm, 0).getLocation(), BlockFace.NORTH);
            if (problem != null) throw new IllegalStateException(problem);
        }, 2, () -> {
            // Facing north, 3 blocks ahead: z = -3; the wall runs west to east from x = dm - 2.
            int boxes = 0, styled = 0, named = 0;
            for (int bx = dm - 2; bx < dm + 2; bx++) for (int y = Y; y < Y + 2; y++) {
                Block box = w.getBlockAt(bx, y, -3);
                BoxInfo i = Styles.info(box);
                if (i == null) continue;
                boxes++;
                if (!i.layers().isEmpty() && !i.icon().isEmpty()) styled++;
                if (!i.name().isEmpty()) named++;
            }
            Block c = w.getBlockAt(dm + 3, Y, -3);
            int level = c.getBlockData() instanceof Levelled l ? l.getLevel() : -1;
            return all(boxes == 8 ? null : boxes + " boxes", styled == 8 ? null : styled + " with patterns and an icon", named == 7 ? null : named + " named",
                level == 3 ? null : "cauldron " + c.getType() + " level " + level);
        }));
        steps.add(new Step("demo: no room -> refused, nothing changed", () -> {}, 1, () -> {
            String problem = dev.whizzle.shulker.Demo.build(at(dm, 0).getLocation(), BlockFace.NORTH);
            return problem != null && problem.startsWith("Not enough room") ? null : "built over the first wall";
        }));

        // 8. Washing on a real cauldron block (the click itself needs a player: test plan).
        int c = 200;
        steps.add(new Step("wash: top layer off, water level down by one", () -> {
            at(c, 0).setType(Material.WATER_CAULDRON);
            Levelled l = (Levelled) at(c, 0).getBlockData();
            l.setLevel(2);
            at(c, 0).setBlockData(l);
        }, 1, () -> {
            ItemStack washed = dev.whizzle.shulker.Washing.washed(styledBox(Material.SHULKER_BOX));
            dev.whizzle.shulker.Washing.lower(at(c, 0));
            int level = at(c, 0).getBlockData() instanceof Levelled l ? l.getLevel() : -1;
            return all(Styles.patterns(washed).equals(PATTERNS.subList(0, 1)) ? null : "after washing " + Styles.patterns(washed), level == 1 ? null : "level " + level);
        }));

        String only = System.getProperty("shulker.selftest.only", "");
        if (!only.isBlank()) {
            List<String> prefixes = List.of(only.split(","));
            steps.removeIf(s -> prefixes.stream().noneMatch(s.name()::startsWith));
            report.add("INFO only steps starting with " + prefixes + " (" + steps.size() + " steps)");
        }
        run(0);
    }

    void run(int i) {
        if (i >= steps.size()) { finish(); return; }
        Step s = steps.get(i);
        try { s.action().run(); }
        catch (RuntimeException ex) { record(s.name(), "action threw " + ex); run(i + 1); return; }
        getServer().getScheduler().runTaskLater(this, () -> {
            String problem;
            try { problem = s.check().get(); } catch (RuntimeException ex) { problem = "check threw " + ex; }
            record(s.name(), problem);
            run(i + 1);
        }, Math.max(1, s.ticks()));
    }

    void record(String name, String problem) {
        if (problem == null) report.add("PASS " + name);
        else { failures++; report.add("FAIL " + name + ": " + problem); }
    }

    void finish() {
        report.add(failures == 0 ? "SELFTEST PASSED on " + getServer().getVersion() : "SELFTEST FAILED (" + failures + ")");
        try { getDataFolder().mkdirs(); Files.write(getDataFolder().toPath().resolve("report.txt"), report); }
        catch (IOException e) { getLogger().severe("Could not write report: " + e); }
        getLogger().info(String.join("\n", report));
        if (Boolean.getBoolean("shulker.selftest.exit")) getServer().shutdown();
    }
}
