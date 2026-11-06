package dev.whizzle.shulker;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BannerRecipeTest {
    static final List<Pattern> PATTERNS = List.of(new Pattern(DyeColor.RED, PatternType.CREEPER), new Pattern(DyeColor.BLUE, PatternType.BORDER));

    @BeforeEach void setUp() { MockBukkit.mock(); }
    @AfterEach void tearDown() { MockBukkit.unmock(); }

    static ItemStack banner(Material type, List<Pattern> patterns) { ItemStack b = new ItemStack(type); Styles.setPatterns(b, patterns); return b; }
    static ItemStack[] grid(ItemStack... items) { ItemStack[] g = new ItemStack[9]; for (int i = 0; i < items.length; i++) g[i * 2] = items[i]; return g; }

    @Test void boxTakesTheBannersPatternsAndKeepsItsColourAndName() {
        ItemStack box = new ItemStack(Material.LIME_SHULKER_BOX);
        box.editMeta(m -> m.customName(net.kyori.adventure.text.Component.text("Wood")));
        ItemStack out = BannerRecipe.result(grid(box, banner(Material.BLACK_BANNER, PATTERNS)));
        assertNotNull(out);
        assertEquals(Material.LIME_SHULKER_BOX, out.getType(), "the box keeps its own colour, not the banner's");
        assertEquals(PATTERNS, Styles.patterns(out));
        assertTrue(out.getItemMeta().hasCustomName(), "the rest of the box (name, contents) carries over");
        assertTrue(Styles.patterns(box).isEmpty(), "the grid's box itself isn't changed");
    }

    @Test void noResult() {
        ItemStack styled = new ItemStack(Material.SHULKER_BOX);
        Styles.setPatterns(styled, PATTERNS);
        assertNull(BannerRecipe.result(grid(styled, banner(Material.RED_BANNER, PATTERNS))), "a box that already has patterns");
        assertNull(BannerRecipe.result(grid(new ItemStack(Material.SHULKER_BOX), new ItemStack(Material.RED_BANNER))), "a plain banner");
        assertNull(BannerRecipe.result(grid(new ItemStack(Material.SHULKER_BOX), new ItemStack(Material.SHULKER_BOX), banner(Material.RED_BANNER, PATTERNS))), "two boxes");
        assertNull(BannerRecipe.result(grid(new ItemStack(Material.SHULKER_BOX), banner(Material.RED_BANNER, PATTERNS), banner(Material.RED_BANNER, PATTERNS))), "two banners");
        assertNull(BannerRecipe.result(grid(new ItemStack(Material.SHULKER_BOX), banner(Material.RED_BANNER, PATTERNS), new ItemStack(Material.STICK))), "something else too");
        assertNull(BannerRecipe.result(grid(banner(Material.RED_BANNER, PATTERNS))), "no box");
    }

    @Test void atMostSixteenLayers() {
        List<Pattern> many = new ArrayList<>();
        for (int i = 0; i < 20; i++) many.add(new Pattern(DyeColor.values()[i % 16], PatternType.STRIPE_TOP));
        ItemStack out = BannerRecipe.result(grid(new ItemStack(Material.SHULKER_BOX), banner(Material.WHITE_BANNER, many)));
        assertEquals(16, Styles.patterns(out).size());
    }
}
