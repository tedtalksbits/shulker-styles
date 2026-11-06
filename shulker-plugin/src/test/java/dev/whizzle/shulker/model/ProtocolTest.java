package dev.whizzle.shulker.model;

import dev.whizzle.shulker.model.Protocol.Pos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolTest {
    static final BoxInfo FULL = new BoxInfo(List.of(new Layer("minecraft:creeper", 14), new Layer("minecraft:border", 11)),
        "Redstone ✦", "minecraft:redstone", true);

    @Test void helloAndWelcome() {
        assertEquals(Protocol.VERSION, Protocol.readHello(Protocol.hello()));
        assertEquals(new Protocol.Welcome(Protocol.VERSION, 64), Protocol.readWelcome(Protocol.welcome(64)));
    }

    @Test void queryRoundTrip() {
        List<Pos> ps = List.of(new Pos(0, -60, 0), new Pos(-30000000, 319, 29999999), new Pos(5, 70, -5));
        assertEquals(ps, Protocol.readQuery(Protocol.query(ps)));
    }

    @Test void queryLimits() {
        assertThrows(IllegalArgumentException.class, () -> Protocol.query(List.of()));
        List<Pos> many = new ArrayList<>();
        for (int i = 0; i <= Protocol.MAX_QUERY; i++) many.add(new Pos(i, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> Protocol.query(many));
        // A hostile count is rejected before reading positions.
        assertThrows(IllegalArgumentException.class, () -> Protocol.readQuery(new Wire.Writer().varInt(1_000_000).bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.readQuery(new Wire.Writer().varInt(1).int32(1).bytes()));
    }

    @Test void boxRoundTrip() {
        Pos pos = new Pos(1, 2, 3);
        assertEquals(new Protocol.Box(pos, FULL), Protocol.readBox(Protocol.box(pos, FULL)));
        assertEquals(new Protocol.Box(pos, BoxInfo.PLAIN), Protocol.readBox(Protocol.box(pos, BoxInfo.PLAIN)));
        assertEquals(new Protocol.Box(pos, null), Protocol.readBox(Protocol.box(pos, null)));
    }

    @Test void boxBytesAreStable() {
        // Pins the layout in docs/PROTOCOL.md: x, y, z, true, 1 layer ("a", 3), name "n", icon "", chosen false.
        byte[] b = Protocol.box(new Pos(0, 1, -1), new BoxInfo(List.of(new Layer("a", 3)), "n", "", false));
        assertArrayEquals(new byte[]{0, 0, 0, 0, 0, 0, 0, 1, -1, -1, -1, -1, 1, 1, 1, 'a', 3, 1, 'n', 0, 0}, b);
    }

    @Test void badBoxesRejected() {
        Wire.Writer w = new Wire.Writer().int32(0).int32(0).int32(0).bool(true).varInt(Layer.MAX_LAYERS + 1);
        assertThrows(IllegalArgumentException.class, () -> Protocol.readBox(w.bytes()));
        Wire.Writer badColour = new Wire.Writer().int32(0).int32(0).int32(0).bool(true).varInt(1).utf("minecraft:creeper").varInt(16).utf("").utf("").bool(false);
        assertThrows(IllegalArgumentException.class, () -> Protocol.readBox(badColour.bytes()));
        byte[] trailing = new Wire.Writer().int32(0).int32(0).int32(0).bool(false).varInt(0).bytes();
        assertThrows(IllegalArgumentException.class, () -> Protocol.readBox(trailing));
    }

    @Test void layerText() {
        List<Layer> ls = FULL.layers();
        assertEquals("minecraft:creeper red,minecraft:border blue", Layer.toText(ls));
        assertEquals(ls, Layer.fromText(Layer.toText(ls)));
        assertEquals(List.of(), Layer.fromText(""));
        assertThrows(IllegalArgumentException.class, () -> Layer.fromText("minecraft:creeper reddish"));
        assertThrows(IllegalArgumentException.class, () -> Layer.fromText("nonsense"));
    }

    @Test void mostCommonIcon() {
        assertEquals("", Icons.mostCommon(List.of()));
        assertEquals("", Icons.mostCommon(java.util.Arrays.asList(null, null)));
        // Totals count, not stacks: 3 stacks of 10 cobblestone beat one stack of 64 dirt? No: 30 < 64.
        assertEquals("minecraft:dirt", Icons.mostCommon(List.of(new Icons.Slot("minecraft:cobblestone", 10), new Icons.Slot("minecraft:dirt", 64),
            new Icons.Slot("minecraft:cobblestone", 10), new Icons.Slot("minecraft:cobblestone", 10))));
        // A tie goes to the item that comes first.
        assertEquals("minecraft:sand", Icons.mostCommon(java.util.Arrays.asList(null, new Icons.Slot("minecraft:sand", 5), new Icons.Slot("minecraft:gravel", 5))));
    }
}
