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
        WELCOME = Protocol.channel(Protocol.WELCOME), BOX = Protocol.channel(Protocol.BOX);
    /** Positions answered per player per second; a mod showing every box in view stays far below this. */
    static final int POSITIONS_PER_SECOND = 512;
    static final int MESSAGES_PER_SECOND = 40;
    /** Watched positions per player; the oldest are dropped first. */
    static final int MAX_WATCHED = 2048;

    public interface Outbox { void send(Player player, String channel, byte[] data); }

    private final Outbox outbox;
    private final int range;
    private final Map<UUID, Watch> watches = new HashMap<>();

    /** What one player's mod has been told, by position. A null info means "no box there". */
    static final class Watch {
        final UUID world;
        final LinkedHashMap<Pos, Optional<BoxInfo>> sent = new LinkedHashMap<>();
        long second; int messages, positions;
        Watch(UUID world) { this.world = world; }
    }

    private final java.util.function.Function<org.bukkit.block.Block, BoxInfo> lookup;

    public ClientBridge(Outbox outbox, int range) { this(outbox, range, Styles::info); }

    /** {@code lookup} reads what the mod should draw at a block; tests substitute it (MockBukkit lacks live block entities). */
    public ClientBridge(Outbox outbox, int range, java.util.function.Function<org.bukkit.block.Block, BoxInfo> lookup) {
        this.outbox = outbox;
        this.range = range;
        this.lookup = lookup;
    }

    public int range() { return range; }
    public int watched(Player p) { Watch w = watches.get(p.getUniqueId()); return w == null ? 0 : w.sent.size(); }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        Watch w = watch(player);
        long now = System.currentTimeMillis() / 1000;
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
        BoxInfo info = lookup.apply(world.getBlockAt(pos.x(), pos.y(), pos.z()));
        w.sent.remove(pos);
        w.sent.put(pos, Optional.ofNullable(info));
        while (w.sent.size() > MAX_WATCHED) w.sent.remove(w.sent.keySet().iterator().next());
        outbox.send(p, BOX, Protocol.box(pos, info));
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
                Optional<BoxInfo> now = Optional.ofNullable(lookup.apply(world.getBlockAt(pos.x(), pos.y(), pos.z())));
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
