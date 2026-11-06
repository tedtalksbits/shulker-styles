package dev.whizzle.shulker;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.block.data.Levelled;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WashingTest {
    static final List<Pattern> PATTERNS = List.of(new Pattern(DyeColor.RED, PatternType.CREEPER), new Pattern(DyeColor.BLUE, PatternType.BORDER));
    ServerMock server; WorldMock world; PlayerMock player; Block cauldron; final Washing washing = new Washing();

    @BeforeEach void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("wash");
        player = server.addPlayer();
        cauldron = world.getBlockAt(0, 70, 0);
        cauldron.setType(Material.WATER_CAULDRON);
        Levelled l = (Levelled) cauldron.getBlockData();
        l.setLevel(3);
        cauldron.setBlockData(l);
    }
    @AfterEach void tearDown() { MockBukkit.unmock(); }

    ItemStack styled(Material type) { ItemStack b = new ItemStack(type); Styles.setPatterns(b, PATTERNS); return b; }

    PlayerInteractEvent use(ItemStack inHand) {
        player.getInventory().setItemInMainHand(inHand);
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, inHand, cauldron, BlockFace.UP, EquipmentSlot.HAND);
        washing.onUse(e);
        return e;
    }

    int level() { return cauldron.getBlockData() instanceof Levelled l ? l.getLevel() : 0; }

    @Test void washesTheTopLayer() {
        ItemStack once = Washing.washed(styled(Material.PINK_SHULKER_BOX));
        assertEquals(Material.PINK_SHULKER_BOX, once.getType(), "colour stays while patterns remain");
        assertEquals(PATTERNS.subList(0, 1), Styles.patterns(once));
        assertTrue(Styles.patterns(Washing.washed(once)).isEmpty());
    }

    // MockBukkit's inventories drop data components when an item is stored, so what ends up in the hand is checked on
    // the real server (docs/TEST_PLAN.md); here: the click is taken over and uses water.
    @Test void clickUsesOneLevelAndStopsVanilla() {
        PlayerInteractEvent e = use(styled(Material.PINK_SHULKER_BOX));
        assertTrue(e.isCancelled(), "vanilla's own wash (undyeing) must not also run");
        assertEquals(Material.PINK_SHULKER_BOX, player.getInventory().getItemInMainHand().getType());
        assertEquals(2, level());
    }

    @Test void lastWaterEmptiesTheCauldron() {
        Levelled l = (Levelled) cauldron.getBlockData();
        l.setLevel(1);
        cauldron.setBlockData(l);
        use(styled(Material.SHULKER_BOX));
        assertEquals(Material.CAULDRON, cauldron.getType());
    }

    @Test void leftAloneForVanilla() {
        assertFalse(use(new ItemStack(Material.PINK_SHULKER_BOX)).isCancelled(), "a plain dyed box: vanilla undyes it");
        player.setSneaking(true);
        assertFalse(use(styled(Material.SHULKER_BOX)).isCancelled(), "sneaking places the box");
        player.setSneaking(false);
        cauldron.setType(Material.CAULDRON);
        assertFalse(use(styled(Material.SHULKER_BOX)).isCancelled(), "an empty cauldron");
    }

    @Test void respectsProtection() {
        ItemStack box = styled(Material.SHULKER_BOX);
        player.getInventory().setItemInMainHand(box);
        PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, box, cauldron, BlockFace.UP, EquipmentSlot.HAND);
        e.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        washing.onUse(e);
        assertEquals(3, level(), "a protected cauldron isn't touched");
    }
}
