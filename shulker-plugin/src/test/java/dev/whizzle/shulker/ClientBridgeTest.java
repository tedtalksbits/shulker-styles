package dev.whizzle.shulker;

import dev.whizzle.shulker.model.BoxInfo;
import dev.whizzle.shulker.model.Layer;
import dev.whizzle.shulker.model.Protocol;
import dev.whizzle.shulker.model.Protocol.Pos;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** The bridge against a fake world: {@code boxes} stands in for what Styles.info reads from real blocks. */
class ClientBridgeTest {
    ServerMock server; WorldMock world; PlayerMock player; ClientBridge bridge;
    final Map<Pos, BoxInfo> boxes = new HashMap<>();
    final Map<Pos, Optional<String>> icons = new HashMap<>();
    Pos open;
    final List<String[]> sent = new ArrayList<>();
    final List<byte[]> bodies = new ArrayList<>();
    static final BoxInfo CREEPER = new BoxInfo(List.of(new Layer("minecraft:creeper", 5)), "Farm", "minecraft:wheat", false);

    @BeforeEach void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("bridge");
        player = server.addPlayer();
        player.teleport(new Location(world, 0.5, 70, 0.5));
        bridge = new ClientBridge((Player p, String channel, byte[] data) -> { sent.add(new String[]{p.getName(), channel}); bodies.add(data); }, 64,
            new ClientBridge.Boxes() {
                @Override public BoxInfo info(Block b) { return boxes.get(new Pos(b.getX(), b.getY(), b.getZ())); }
                @Override public Block openBox(Player p) { return open == null ? null : world.getBlockAt(open.x(), open.y(), open.z()); }
                @Override public void setIcon(Block b, String item) { icons.put(new Pos(b.getX(), b.getY(), b.getZ()), Optional.ofNullable(item)); }
                @Override public boolean isItem(String id) { return id.equals("minecraft:diamond") || id.equals("diamond"); }
            });
        for (int cx = -5; cx <= 5; cx++) for (int cz = -5; cz <= 5; cz++) world.loadChunk(cx, cz);
    }
    @AfterEach void tearDown() { MockBukkit.unmock(); }

    void query(Pos... ps) { bridge.onPluginMessageReceived(ClientBridge.QUERY, player, Protocol.query(List.of(ps))); }
    Protocol.Box lastBox() { assertEquals(ClientBridge.BOX, sent.getLast()[1]); return Protocol.readBox(bodies.getLast()); }

    @Test void helloGetsWelcomeWithRange() {
        bridge.onPluginMessageReceived(ClientBridge.HELLO, player, Protocol.hello());
        assertEquals(ClientBridge.WELCOME, sent.getLast()[1]);
        assertEquals(new Protocol.Welcome(Protocol.VERSION, 64), Protocol.readWelcome(bodies.getLast()));
    }

    @Test void answersEachPosition() {
        Pos a = new Pos(3, 70, 3), b = new Pos(4, 70, 3);
        boxes.put(a, CREEPER);
        query(a, b);
        assertEquals(2, sent.size());
        assertEquals(new Protocol.Box(a, CREEPER), Protocol.readBox(bodies.get(0)));
        assertEquals(new Protocol.Box(b, null), Protocol.readBox(bodies.get(1)), "no box there: forget it");
    }

    @Test void silentOutOfRange() {
        query(new Pos(70, 70, 0));
        assertTrue(sent.isEmpty());
        assertEquals(0, bridge.watched(player));
    }

    @Test void pushesChangesAtWatchedPositions() {
        Pos a = new Pos(3, 70, 3);
        query(a);                                   // nothing there yet
        sent.clear();
        bridge.poll();
        assertTrue(sent.isEmpty(), "no change, nothing sent");
        boxes.put(a, BoxInfo.PLAIN);                // a box is placed
        bridge.poll();
        assertEquals(new Protocol.Box(a, BoxInfo.PLAIN), lastBox());
        boxes.put(a, CREEPER);                      // its icon or patterns change
        bridge.poll();
        assertEquals(new Protocol.Box(a, CREEPER), lastBox());
        boxes.remove(a);                            // broken
        bridge.poll();
        assertEquals(new Protocol.Box(a, null), lastBox());
        assertEquals(3, sent.size());
    }

    @Test void stopsWatchingWhenThePlayerLeavesTheArea() {
        Pos a = new Pos(3, 70, 3);
        boxes.put(a, CREEPER);
        query(a);
        player.teleport(new Location(world, 200, 70, 0));
        bridge.poll();
        assertEquals(0, bridge.watched(player));
        boxes.put(a, BoxInfo.PLAIN);
        sent.clear();
        bridge.poll();
        assertTrue(sent.isEmpty());
    }

    @Test void positionBudgetPerSecond() {
        List<Pos> batch = new ArrayList<>();
        for (int i = 0; i < Protocol.MAX_QUERY; i++) batch.add(new Pos(i % 30, 70, i / 30));
        bridge.clock = () -> 1000;
        int asked = 0;
        while (asked + Protocol.MAX_QUERY <= ClientBridge.POSITIONS_PER_SECOND + 2 * Protocol.MAX_QUERY) {
            bridge.onPluginMessageReceived(ClientBridge.QUERY, player, Protocol.query(batch));
            asked += Protocol.MAX_QUERY;
        }
        assertEquals(ClientBridge.POSITIONS_PER_SECOND, sent.size());
        bridge.clock = () -> 1001;
        bridge.onPluginMessageReceived(ClientBridge.QUERY, player, Protocol.query(batch));
        assertEquals(ClientBridge.POSITIONS_PER_SECOND + Protocol.MAX_QUERY, sent.size(), "a new second, a new budget");
    }

    @Test void malformedIgnored() {
        bridge.onPluginMessageReceived(ClientBridge.QUERY, player, new byte[]{5, 1});
        bridge.onPluginMessageReceived(ClientBridge.HELLO, player, new byte[0]);
        assertTrue(sent.isEmpty());
    }

    void setIcon(Pos pos, String item) { bridge.onPluginMessageReceived(ClientBridge.SETICON, player, Protocol.setIcon(pos, item)); }

    @Test void setIconOnlyForTheOpenBox() {
        Pos a = new Pos(3, 70, 3), b = new Pos(4, 70, 3);
        setIcon(a, "minecraft:diamond");
        assertTrue(icons.isEmpty(), "no box open");
        open = a;
        setIcon(b, "minecraft:diamond");
        assertTrue(icons.isEmpty(), "a box other than the open one");
        setIcon(a, "minecraft:not_an_item");
        assertTrue(icons.isEmpty(), "not a real item");
        setIcon(a, "diamond");
        assertEquals(Optional.of("minecraft:diamond"), icons.get(a), "stored as a full id");
        setIcon(a, "");
        assertEquals(Optional.empty(), icons.get(a), "empty: back to automatic");
    }

    @Test void openedTellsTheModWhichBox() {
        Pos a = new Pos(3, 70, 3);
        boxes.put(a, CREEPER);
        bridge.opened(player, a);
        assertEquals(new Protocol.Box(a, CREEPER), Protocol.readBox(bodies.get(0)), "the box itself first, so the mod knows its icon");
        assertEquals(ClientBridge.OPENED, sent.get(1)[1]);
        assertEquals(a, Protocol.readOpened(bodies.get(1)));
        assertEquals(1, bridge.watched(player), "and it's watched from now on");
    }
}
