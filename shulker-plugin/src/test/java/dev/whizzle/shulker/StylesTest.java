package dev.whizzle.shulker;

import dev.whizzle.shulker.Styles.Style;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StylesTest {
    static final List<Pattern> PATTERNS = List.of(new Pattern(DyeColor.RED, PatternType.CREEPER), new Pattern(DyeColor.BLUE, PatternType.BORDER));
    ServerMock server; WorldMock world;

    @BeforeEach void setUp() { server = MockBukkit.mock(); world = server.addSimpleWorld("styles"); }
    @AfterEach void tearDown() { MockBukkit.unmock(); }

    @Test void itemStyleRoundTrip() {
        ItemStack box = new ItemStack(Material.RED_SHULKER_BOX);
        assertTrue(Styles.of(box).isEmpty());
        Styles.setPatterns(box, PATTERNS);
        Styles.setChosenIcon(box, "minecraft:diamond");
        assertEquals(new Style(PATTERNS, "minecraft:diamond"), Styles.of(box));
        Styles.setChosenIcon(box, null);
        Styles.setPatterns(box, List.of());
        assertTrue(Styles.of(box).isEmpty());
    }

    @Test void applyKeepsWhatTheItemHas() {
        ItemStack box = new ItemStack(Material.SHULKER_BOX);
        Styles.setPatterns(box, PATTERNS.subList(0, 1));
        assertTrue(Styles.apply(box, new Style(PATTERNS, "minecraft:stone")));
        assertEquals(PATTERNS.subList(0, 1), Styles.patterns(box), "patterns already on the item stay");
        assertEquals("minecraft:stone", Styles.chosenIcon(box));
        assertFalse(Styles.apply(box, new Style(PATTERNS, "minecraft:dirt")));
    }

    // Placed boxes (Styles.store, Styles.of(Block), Styles.info) need Paper's live block entities, which MockBukkit
    // doesn't have: the real-server self-test checks them (paper-selftest, "info:" and every drop step).
}
