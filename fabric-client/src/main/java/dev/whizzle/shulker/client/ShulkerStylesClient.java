package dev.whizzle.shulker.client;

import dev.whizzle.shulker.client.net.Net;
import dev.whizzle.shulker.model.BoxInfo;
import dev.whizzle.shulker.model.Protocol;
import dev.whizzle.shulker.model.Protocol.Pos;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ServerboundPlayChannelEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shulker Styles client. Talks to the plugin (hello, then asks about every shulker box in range and keeps the
 * answers, which the server updates) and draws banner patterns on placed boxes and box items ({@code render/},
 * {@code mixin/}). {@code /shulkermod} shows what the mod knows about the box you look at.
 */
public final class ShulkerStylesClient implements ClientModInitializer {
	public static final Logger LOG = LoggerFactory.getLogger("shulkerstyles");
	public static final BoxCache BOXES = new BoxCache();
	/** From the server's welcome; 0 until it answers (or when the server doesn't have the plugin). */
	static int serverProtocol, range;
	private static long ticks;
	/** Boxes that just appeared (placed, or their chunk loaded): asked about on the next tick, not the next scan. */
	private static final Set<Pos> FRESH = new LinkedHashSet<>();

	@Override
	public void onInitializeClient() {
		Net.register(Map.of(
			Protocol.WELCOME, data -> {
				Protocol.Welcome w = Protocol.readWelcome(data);
				serverProtocol = w.protocol();
				range = w.range();
				if (serverProtocol != Protocol.VERSION) LOG.warn("Server speaks Shulker Styles protocol {}, this mod {}: boxes won't be styled", serverProtocol, Protocol.VERSION);
			},
			Protocol.BOX, data -> BOXES.onBox(Protocol.readBox(data))));
		ClientPlayConnectionEvents.JOIN.register((listener, sender, client) -> { reset(); dev.whizzle.shulker.client.render.Patterns.reload(); });
		ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> reset());
		ServerboundPlayChannelEvents.REGISTER.register((listener, sender, client, channels) -> {
			if (channels.contains(Identifier.fromNamespaceAndPath(Protocol.NAMESPACE, Protocol.HELLO))) Net.send(Protocol.HELLO, Protocol.hello());
		});
		ClientTickEvents.END_CLIENT_TICK.register(ShulkerStylesClient::tick);
		ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register((be, level) -> {
			if (!(be instanceof ShulkerBoxBlockEntity)) return;
			Pos p = pos(be.getBlockPos());
			BOXES.forget(p);
			FRESH.add(p);
		});
		ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((be, level) -> {
			if (be instanceof ShulkerBoxBlockEntity) BOXES.forget(pos(be.getBlockPos()));
		});
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) ->
			dispatcher.register(ClientCommands.literal("shulkermod").executes(c -> { status(c.getSource().getClient()); return 1; })));
	}

	static void reset() { serverProtocol = 0; range = 0; BOXES.clear(); FRESH.clear(); }

	static Pos pos(BlockPos p) { return new Pos(p.getX(), p.getY(), p.getZ()); }

	static boolean active() { return serverProtocol == Protocol.VERSION && range > 0; }

	/** Every tick: ask about boxes that just appeared. Every 10 ticks: forget boxes out of range, ask about the rest. */
	static void tick(Minecraft mc) {
		++ticks;
		if (!active() || mc.level == null || mc.player == null) { FRESH.clear(); return; }
		BlockPos me = mc.player.blockPosition();
		long r2 = (long) range * range;
		if (!FRESH.isEmpty()) {
			List<Pos> near = new ArrayList<>();
			for (Pos p : FRESH) if (me.distSqr(new BlockPos(p.x(), p.y(), p.z())) <= r2) near.add(p);
			FRESH.clear();
			for (byte[] q : BoxCache.queries(BOXES.toAsk(near, ticks))) Net.send(Protocol.QUERY, q);
		}
		if (ticks % 10 != 0) return;
		BOXES.retain(p -> me.distSqr(new BlockPos(p.x(), p.y(), p.z())) <= r2);
		List<Pos> visible = new ArrayList<>();
		int cr = (range >> 4) + 1;
		for (int cx = (me.getX() >> 4) - cr; cx <= (me.getX() >> 4) + cr; cx++)
			for (int cz = (me.getZ() >> 4) - cr; cz <= (me.getZ() >> 4) + cr; cz++) {
				LevelChunk chunk = mc.level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
				if (chunk == null) continue;
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					if (!(be instanceof ShulkerBoxBlockEntity)) continue;
					BlockPos p = be.getBlockPos();
					if (me.distSqr(p) <= r2) visible.add(pos(p));
				}
			}
		for (byte[] q : BoxCache.queries(BOXES.toAsk(visible, ticks))) Net.send(Protocol.QUERY, q);
	}

	static void status(Minecraft mc) {
		if (mc.player == null) return;
		if (serverProtocol == 0) { mc.player.sendSystemMessage(Component.literal("Shulker Styles: this server doesn't run the plugin (no answer).")); return; }
		if (!active()) { mc.player.sendSystemMessage(Component.literal("Shulker Styles: server protocol " + serverProtocol + ", mod " + Protocol.VERSION + ". Update the older one.")); return; }
		mc.player.sendSystemMessage(Component.literal("Shulker Styles: connected (range " + range + ", " + BOXES.size() + " boxes known)."));
		if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
		BlockPos p = hit.getBlockPos();
		BoxInfo info = BOXES.get(new Pos(p.getX(), p.getY(), p.getZ()));
		if (info == null) { mc.player.sendSystemMessage(Component.literal("  Looking at: nothing known (not a shulker box, or not answered yet).")); return; }
		StringBuilder layers = new StringBuilder();
		for (var l : info.layers()) layers.append(layers.isEmpty() ? "" : ", ").append(l.colorName()).append(' ').append(l.pattern().replace("minecraft:", ""));
		mc.player.sendSystemMessage(Component.literal("  Patterns: " + (layers.isEmpty() ? "none" : layers)));
		mc.player.sendSystemMessage(Component.literal("  Name: " + (info.name().isEmpty() ? "none" : info.name())));
		mc.player.sendSystemMessage(Component.literal("  Icon: " + (info.icon().isEmpty() ? "none" : info.icon() + (info.iconChosen() ? " (chosen)" : " (most common inside)"))));
	}
}
