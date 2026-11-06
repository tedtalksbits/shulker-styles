package dev.whizzle.shulker;

import dev.whizzle.shulker.model.BoxInfo;
import dev.whizzle.shulker.model.Layer;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Locale;

/** Shulker Styles: banner patterns and labels on shulker boxes, drawn by the client mod. */
public class ShulkerStylesPlugin extends JavaPlugin implements Listener, TabExecutor {
    private ClientBridge bridge;
    private Keeper keeper;
    private BukkitTask tick, poll;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        Styles.log = getLogger();
        bridge = new ClientBridge((p, channel, data) -> p.sendPluginMessage(this, channel, data),
            Math.clamp(getConfig().getInt("range", 64), 8, 256));
        keeper = new Keeper(this);
        var pm = getServer().getPluginManager();
        pm.registerEvents(this, this);
        pm.registerEvents(keeper, this);
        pm.registerEvents(new BannerRecipe(), this);
        pm.registerEvents(new Washing(), this);
        pm.registerEvents(bridge, this);
        for (String c : ClientBridge.INCOMING) getServer().getMessenger().registerIncomingPluginChannel(this, c, bridge);
        for (String c : ClientBridge.OUTGOING) getServer().getMessenger().registerOutgoingPluginChannel(this, c);
        BannerRecipe.register();
        PluginCommand command = getCommand("shulkerstyles");
        if (command == null) throw new IllegalStateException("shulkerstyles command missing from plugin.yml");
        command.setExecutor(this);
        command.setTabCompleter(this);
        tick = getServer().getScheduler().runTaskTimer(this, keeper::tick, 1, 1);
        poll = getServer().getScheduler().runTaskTimer(this, bridge::poll, 5, 5);
    }

    @Override
    public void onDisable() {
        if (tick != null) tick.cancel();
        if (poll != null) poll.cancel();
        Bukkit.removeRecipe(BannerRecipe.KEY);
    }

    public ClientBridge bridge() { return bridge; }

    @EventHandler public void onJoin(PlayerJoinEvent e) { e.getPlayer().discoverRecipe(BannerRecipe.KEY); }

    @EventHandler public void onReloaded(ServerResourcesReloadedEvent e) {
        if (Bukkit.getRecipe(BannerRecipe.KEY) != null) return;
        BannerRecipe.register();
        for (Player p : Bukkit.getOnlinePlayers()) p.discoverRecipe(BannerRecipe.KEY);
    }

    // ---- /shulkerstyles ----

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "icon" -> icon(p, args.length > 1 && args[1].equalsIgnoreCase("clear"));
            case "info" -> info(p);
            default -> {
                p.sendMessage(Component.text("Shulker Styles", NamedTextColor.AQUA));
                p.sendMessage(Component.text("  Craft a shulker box with a banner to put the banner's patterns on it.", NamedTextColor.GRAY));
                p.sendMessage(Component.text("  Use it on a water cauldron to wash off the top layer.", NamedTextColor.GRAY));
                p.sendMessage(Component.text("  /" + label + " icon: hold a box, and in your off hand the item to show as its label icon.", NamedTextColor.GRAY));
                p.sendMessage(Component.text("  /" + label + " icon clear: back to the item it holds most of.", NamedTextColor.GRAY));
                p.sendMessage(Component.text("  /" + label + " info: what the box you hold or look at shows.", NamedTextColor.GRAY));
            }
        }
        return true;
    }

    private void icon(Player p, boolean clear) {
        ItemStack box = p.getInventory().getItemInMainHand();
        if (!Styles.isBox(box)) { p.sendMessage(Component.text("Hold a shulker box in your main hand.", NamedTextColor.RED)); return; }
        if (clear) {
            Styles.setChosenIcon(box, null);
            p.sendMessage(Component.text("Label icon: automatic (the item it holds most of).", NamedTextColor.GREEN));
            return;
        }
        ItemStack icon = p.getInventory().getItemInOffHand();
        if (icon.getType().isAir()) {
            p.sendMessage(Component.text("Put the item to show as its icon in your off hand, then run this again.", NamedTextColor.RED));
            return;
        }
        Styles.setChosenIcon(box, icon.getType().getKey().toString());
        p.sendMessage(Component.text("Label icon: " + icon.getType().getKey().getKey() + ".", NamedTextColor.GREEN));
    }

    private void info(Player p) {
        ItemStack held = p.getInventory().getItemInMainHand();
        BoxInfo info;
        String what;
        if (Styles.isBox(held)) {
            Styles.Style s = Styles.of(held);
            var meta = held.getItemMeta();
            String name = meta != null && meta.hasCustomName() ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(meta.customName()) : "";
            info = new BoxInfo(Styles.toLayers(s.patterns()), name, s.icon() == null ? "" : s.icon(), s.icon() != null);
            what = "The box in your hand";
        } else {
            Block b = p.getTargetBlockExact(8);
            info = b == null ? null : Styles.info(b);
            if (info == null) { p.sendMessage(Component.text("Hold a shulker box or look at one (within 8 blocks).", NamedTextColor.RED)); return; }
            what = "That box";
        }
        p.sendMessage(Component.text(what + ":", NamedTextColor.AQUA));
        p.sendMessage(Component.text("  Patterns: " + (info.layers().isEmpty() ? "none" : describe(info.layers())), NamedTextColor.GRAY));
        p.sendMessage(Component.text("  Name: " + (info.name().isEmpty() ? "none (rename it in an anvil)" : info.name()), NamedTextColor.GRAY));
        String icon = info.icon().isEmpty() ? (info.iconChosen() ? "?" : "automatic") : info.icon() + (info.iconChosen() ? " (chosen)" : " (most common inside)");
        p.sendMessage(Component.text("  Icon: " + icon, NamedTextColor.GRAY));
    }

    static String describe(List<Layer> layers) {
        StringBuilder sb = new StringBuilder();
        for (Layer l : layers) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(l.colorName()).append(' ').append(l.pattern().replace("minecraft:", ""));
        }
        return sb.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("icon", "info").stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        if (args.length == 2 && args[0].equalsIgnoreCase("icon")) return "clear".startsWith(args[1].toLowerCase(Locale.ROOT)) ? List.of("clear") : List.of();
        return List.of();
    }
}
