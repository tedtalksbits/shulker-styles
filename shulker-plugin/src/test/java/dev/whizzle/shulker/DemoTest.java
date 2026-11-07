package dev.whizzle.shulker;

import org.bukkit.Tag;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The wall itself is checked on a real server (self-test "demo:" steps). */
class DemoTest {
    @BeforeEach void setUp() { MockBukkit.mock(); }
    @AfterEach void tearDown() { MockBukkit.unmock(); }

    @Test void kitHasPatternedBannersAndBoxesThatCraftTogether() {
        List<ItemStack> kit = Demo.kit();
        List<ItemStack> banners = kit.stream().filter(i -> Tag.ITEMS_BANNERS.isTagged(i.getType())).toList();
        List<ItemStack> plainBoxes = kit.stream().filter(i -> Styles.isBox(i) && Styles.patterns(i).isEmpty()).toList();
        assertEquals(4, banners.size());
        assertTrue(banners.stream().allMatch(b -> !Styles.patterns(b).isEmpty()), "every banner has patterns");
        assertEquals(3, plainBoxes.size());
        assertNotNull(BannerRecipe.result(new ItemStack[]{plainBoxes.getFirst(), banners.getFirst()}), "a kit box and banner craft");
        assertEquals(1, kit.stream().filter(i -> Styles.isBox(i) && !Styles.patterns(i).isEmpty()).count(), "one ready patterned box");
    }

    @Test void wallExamples() {
        assertEquals(Demo.WIDTH * Demo.HEIGHT, Demo.WALL.size());
        assertTrue(Demo.WALL.stream().allMatch(e -> !e.patterns().isEmpty() && !e.contents().isEmpty()));
        assertEquals(1, Demo.WALL.stream().filter(e -> e.icon() != null).count(), "one box shows a chosen icon");
        assertEquals(1, Demo.WALL.stream().filter(e -> e.name().isEmpty()).count(), "one box is unnamed (its icon's name shows)");
    }

    @Test void leftOfEachFacing() {
        assertEquals(BlockFace.WEST, Demo.leftOf(BlockFace.NORTH));
        assertEquals(BlockFace.EAST, Demo.leftOf(BlockFace.SOUTH));
        assertEquals(BlockFace.NORTH, Demo.leftOf(BlockFace.EAST));
        assertEquals(BlockFace.SOUTH, Demo.leftOf(BlockFace.WEST));
    }
}
