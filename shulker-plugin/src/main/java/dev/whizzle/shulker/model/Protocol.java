package dev.whizzle.shulker.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The client-mod protocol (docs/PROTOCOL.md): channel names and every message's bytes, shared by the plugin and the
 * mod so both encode and decode with the same code. Bump {@link #VERSION} when any message's bytes change.
 */
public final class Protocol {
    private Protocol() {}

    public static final int VERSION = 1;
    public static final String NAMESPACE = "shulkerstyles";
    public static final String HELLO = "hello", WELCOME = "welcome", QUERY = "query", BOX = "box";
    public static final List<String> SERVERBOUND = List.of(HELLO, QUERY);
    public static final List<String> CLIENTBOUND = List.of(WELCOME, BOX);
    /** Most positions in one query. */
    public static final int MAX_QUERY = 64;

    public static String channel(String name) { return NAMESPACE + ":" + name; }

    public record Pos(int x, int y, int z) {
        public long asLong() { return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF); }
    }

    /** The server's welcome: its protocol, and how far from the player (in blocks) it answers queries. */
    public record Welcome(int protocol, int range) {}

    /** The answer about one position. {@code info} is null when there's no shulker box there (forget it). */
    public record Box(Pos pos, BoxInfo info) {}

    // ---- hello (C->S): VarInt protocol ----
    public static byte[] hello() { return new Wire.Writer().varInt(VERSION).bytes(); }
    public static int readHello(byte[] data) { Wire.Reader in = new Wire.Reader(data); int v = in.varInt(); in.end(); return v; }

    // ---- welcome (S->C): VarInt protocol, VarInt range ----
    public static byte[] welcome(int range) { return new Wire.Writer().varInt(VERSION).varInt(range).bytes(); }
    public static Welcome readWelcome(byte[] data) {
        Wire.Reader in = new Wire.Reader(data);
        Welcome w = new Welcome(in.varInt(), in.varInt());
        in.end();
        return w;
    }

    // ---- query (C->S): VarInt n (1-64), n x (Int32 x, y, z) ----
    public static byte[] query(List<Pos> positions) {
        if (positions.isEmpty() || positions.size() > MAX_QUERY) throw new IllegalArgumentException("1-" + MAX_QUERY + " positions per query");
        Wire.Writer out = new Wire.Writer().varInt(positions.size());
        for (Pos p : positions) out.int32(p.x()).int32(p.y()).int32(p.z());
        return out.bytes();
    }
    public static List<Pos> readQuery(byte[] data) {
        Wire.Reader in = new Wire.Reader(data);
        int n = in.varInt();
        if (n < 1 || n > MAX_QUERY) throw new IllegalArgumentException("Bad query size " + n);
        List<Pos> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(new Pos(in.int32(), in.int32(), in.int32()));
        in.end();
        return out;
    }

    // ---- box (S->C): Int32 x, y, z, Bool is a box, then (if so) the BoxInfo ----
    public static byte[] box(Pos pos, BoxInfo info) {
        Wire.Writer out = new Wire.Writer().int32(pos.x()).int32(pos.y()).int32(pos.z()).bool(info != null);
        if (info != null) info.write(out);
        return out.bytes();
    }
    public static Box readBox(byte[] data) {
        Wire.Reader in = new Wire.Reader(data);
        Pos pos = new Pos(in.int32(), in.int32(), in.int32());
        BoxInfo info = in.bool() ? BoxInfo.read(in) : null;
        in.end();
        return new Box(pos, info);
    }
}
