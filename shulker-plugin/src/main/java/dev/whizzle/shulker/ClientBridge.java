package dev.whizzle.shulker;

import dev.whizzle.shulker.model.BoxInfo;
import dev.whizzle.shulker.model.Protocol;
import dev.whizzle.shulker.model.Protocol.Pos;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.*;

/**
 * Server half of the client-mod protocol (docs/PROTOCOL.md). The mod asks about shulker boxes near the player; every
 * position it asked about stays watched, and whenever what the mod would draw there changes (patterns, name, icon,
 * the box broken or placed) the new answer is pushed. Watching stops when the player moves out of range or changes
 * world; the mod asks again when it comes back.
 */
public final class ClientBridge implements PluginMessageListener, Listener {
    public static final List<String> INCOMING = Protocol.SERVERBOUND.stream().map(Protocol::channel).toList();
    public static final List<String> OUTGOING = Protocol.CLIENTBOUND.stream().map(Protocol::channel).toList();
    static final String HELLO = Protocol.channel(Protocol.HELLO), QUERY = Protocol.channel(Protocol.QUERY),
        WELCOME = Protocol.channel(Protocol.WELCOME), BOX = Protocol.channel(Protocol.BOX),
        OPENED = Protocol.channel(Protocol.OPENED), SETICON = Protocol.channel(Protocol.SETICON);
    /** Positions answered per player per second; a mod showing every box in view stays far below this. */
    static final int POSITIONS_PER_SECOND = 512;
    static final int MESSAGES_PER_SECOND = 40;
    /** Watched positions per player; the oldest are dropped first. */
    static final int MAX_WATCHED = 2048;

    public interface Outbox { void send(Player player, String channel, byte[] data); }

    private final Outbox outbox;
    private final int range;
    private final Map<UUID, Watch> watches = new HashMap<>();
    /** The current second, for the rate limits (tests fix it). */
    java.util.function.LongSupplier clock = () -> System.currentTimeMillis() / 1000;

    /** What one player's mod has been told, by position. A null info means "no box there". */
    static final class Watch {
        final UUID world;
        final LinkedHashMap<Pos, Optional<BoxInfo>> sent = new LinkedHashMap<>();
        long second; int messages, positions;
        Watch(UUID world) { this.world = world; }
    }

    /** The world as the bridge sees it; tests substitute it (MockBukkit lacks live block entities). */
    public interface Boxes {
        /** What the mod should draw at a block; null if it isn't a shulker box. */
        BoxInfo info(org.bukkit.block.Block block);
        /** The placed shulker box whose screen the player has open, or null. */
        org.bukkit.block.Block openBox(Player player);
        /** Sets (item id) or clears (null) the chosen label icon of a placed box. */
        void setIcon(org.bukkit.block.Block box, String item);
        /** Whether an item id names a real item. */
        boolean isItem(String id);
    }

    public static final Boxes REAL = new Boxes() {
        @Override public BoxInfo info(org.bukkit.block.Block block) { return Styles.info(block); }
        @Override public org.bukkit.block.Block openBox(Player player) {
            var holder = player.getOpenInventory().getTopInventory().getHolder(false);
            return holder instanceof org.bukkit.block.ShulkerBox box && box.isPlaced() ? box.getBlock() : null;
        }
        @Override public void setIcon(org.bukkit.block.Block box, String item) { Styles.storeIcon(box, item); }
        @Override public boolean isItem(String id) {
            org.bukkit.Material m = org.bukkit.Material.matchMaterial(id);
            return m != null && m.isItem() && !m.isAir();
        }
    };

    private final Boxes boxes;

    public ClientBridge(Outbox outbox, int range) { this(outbox, range, REAL); }

    public ClientBridge(Outbox outbox, int range, Boxes boxes) {
        this.outbox = outbox;
        this.range = range;
        this.boxes = boxes;
    }

    public int range() { return range; }
    public int watched(Player p) { Watch w = watches.get(p.getUniqueId()); return w == null ? 0 : w.sent.size(); }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        Watch w = watch(player);
        long now = clock.getAsLong();
        if (w.second != now) { w.second = now; w.messages = 0; w.positions = 0; }
        if (++w.messages > MESSAGES_PER_SECOND) return;
        try {
            if (channel.equals(HELLO)) {
                Protocol.readHello(message);   // any version: the welcome tells the mod ours, and it decides
                outbox.send(player, WELCOME, Protocol.welcome(range));
            } else if (channel.equals(QUERY)) {
                List<Pos> positions = Protocol.readQuery(message);
                if ((w.positions += positions.size()) > POSITIONS_PER_SECOND) return;
                for (Pos pos : positions) answer(player, w, pos);
            } else if (channel.equals(SETICON)) {
                setIcon(player, Protocol.readSetIcon(message));
            }
        } catch (IllegalArgumentException ignored) {
            // A malformed message from a modified client: ignore it.
        }
    }

    private Watch watch(Player p) {
        Watch w = watches.get(p.getUniqueId());
        if (w == null || !w.world.equals(p.getWorld().getUID())) watches.put(p.getUniqueId(), w = new Watch(p.getWorld().getUID()));
        return w;
    }

    private boolean inRange(Player p, Pos pos, double extra) {
        Location l = p.getLocation();
        double dx = l.getX() - (pos.x() + 0.5), dy = l.getY() - (pos.y() + 0.5), dz = l.getZ() - (pos.z() + 0.5);
        double r = range + extra;
        return dx * dx + dy * dy + dz * dz <= r * r;
    }

    /** Answers one position (silent if it's out of range or not loaded) and starts watching it. */
    void answer(Player p, Watch w, Pos pos) {
        World world = p.getWorld();
        if (!inRange(p, pos, 0) || !world.isChunkLoaded(pos.x() >> 4, pos.z() >> 4)) return;
        BoxInfo info = boxes.info(world.getBlockAt(pos.x(), pos.y(), pos.z()));
        w.sent.remove(pos);
        w.sent.put(pos, Optional.ofNullable(info));
        while (w.sent.size() > MAX_WATCHED) w.sent.remove(w.sent.keySet().iterator().next());
        outbox.send(p, BOX, Protocol.box(pos, info));
    }

    /**
     * Sets the label icon of the box the player has open. Ignored unless {@code pos} is that box and the item is real,
     * so a modified client can only change a box it could open anyway. The change reaches every watching mod through
     * the next {@link #poll()}.
     */
    void setIcon(Player p, Protocol.SetIcon request) {
        org.bukkit.block.Block open = boxes.openBox(p);
        Pos pos = request.pos();
        if (open == null || open.getX() != pos.x() || open.getY() != pos.y() || open.getZ() != pos.z()) return;
        String item = request.item().isEmpty() ? null : request.item();
        if (item != null && !boxes.isItem(item)) return;
        boxes.setIcon(open, item == null ? null : org.bukkit.NamespacedKey.fromString(item).toString());
    }

    /** A player opened a placed box: their mod needs its position for the icon slot. */
    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onOpen(org.bukkit.event.inventory.InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (!(e.getInventory().getHolder(false) instanceof org.bukkit.block.ShulkerBox box) || !box.isPlaced()) return;
        opened(p, new Pos(box.getX(), box.getY(), box.getZ()));
    }

    void opened(Player p, Pos pos) {
        answer(p, watch(p), pos);   // make sure the mod knows this box (and keeps it up to date)
        outbox.send(p, OPENED, Protocol.opened(pos));
    }

    /** Runs every few ticks: pushes changes at watched positions and stops watching what's out of range. */
    public void poll() {
        Iterator<Map.Entry<UUID, Watch>> players = watches.entrySet().iterator();
        while (players.hasNext()) {
            Map.Entry<UUID, Watch> pe = players.next();
            Player p = Bukkit.getPlayer(pe.getKey());
            Watch w = pe.getValue();
            if (p == null) { players.remove(); continue; }
            if (!w.world.equals(p.getWorld().getUID())) { players.remove(); continue; }
            World world = p.getWorld();
            Iterator<Map.Entry<Pos, Optional<BoxInfo>>> it = w.sent.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Pos, Optional<BoxInfo>> e = it.next();
                Pos pos = e.getKey();
                if (!inRange(p, pos, 16) || !world.isChunkLoaded(pos.x() >> 4, pos.z() >> 4)) { it.remove(); continue; }
                Optional<BoxInfo> now = Optional.ofNullable(boxes.info(world.getBlockAt(pos.x(), pos.y(), pos.z())));
                if (!now.equals(e.getValue())) {
                    e.setValue(now);
                    outbox.send(p, BOX, Protocol.box(pos, now.orElse(null)));
                }
            }
        }
    }

    @EventHandler public void onQuit(PlayerQuitEvent e) { watches.remove(e.getPlayer().getUniqueId()); }
    @EventHandler public void onWorld(PlayerChangedWorldEvent e) { watches.remove(e.getPlayer().getUniqueId()); }
}
