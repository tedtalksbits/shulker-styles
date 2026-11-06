package dev.whizzle.shulker.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.whizzle.shulker.client.ShulkerStylesClient;
import dev.whizzle.shulker.model.BoxInfo;
import dev.whizzle.shulker.model.Layer;
import dev.whizzle.shulker.model.Protocol;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

import java.io.InputStream;
import java.util.*;

/**
 * Draws pattern layers over a shulker box model, as shields and banners draw theirs: one pass of the same model per
 * layer, with a mask texture (the pattern laid out on the box's texture, {@link MaskLayout}) tinted with the dye
 * colour. Called from the shulker box renderer (mixin), which is used for placed boxes and for box items.
 */
public final class Patterns {
	private Patterns() {}

	/** The layers for the box being drawn on this thread, set around the shulker box renderer's call. */
	public static final ThreadLocal<List<PatternLayer>> CURRENT = new ThreadLocal<>();

	private static final Map<Identifier, Identifier> MASKS = new HashMap<>();
	/** Its cut-outs are the same in every colour. */
	private static final Identifier BOX_TEXTURE = Identifier.withDefaultNamespace("textures/entity/shulker/shulker.png");
	private static final Map<List<Layer>, List<PatternLayer>> FROM_SERVER = new HashMap<>();

	/** The layers the server reported for the placed box at {@code pos}; null if none. */
	public static List<PatternLayer> forBlock(BlockPos pos) {
		BoxInfo info = ShulkerStylesClient.BOXES.get(new Protocol.Pos(pos.getX(), pos.getY(), pos.getZ()));
		if (info == null || info.layers().isEmpty()) return null;
		List<PatternLayer> cached = FROM_SERVER.get(info.layers());
		if (cached != null) return cached;
		var level = Minecraft.getInstance().level;
		if (level == null) return null;
		var registry = level.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN);
		List<PatternLayer> out = new ArrayList<>();
		for (Layer l : info.layers()) {
			Identifier id = Identifier.tryParse(l.pattern());
			if (id == null) continue;
			registry.get(ResourceKey.create(Registries.BANNER_PATTERN, id))
				.ifPresent(h -> out.add(layer(h, DyeColor.byId(l.color()))));
		}
		List<PatternLayer> result = out.isEmpty() ? null : List.copyOf(out);
		if (FROM_SERVER.size() > 512) FROM_SERVER.clear();
		FROM_SERVER.put(info.layers(), result);
		return result;
	}

	/** The layers on a box item; null if none. */
	public static List<PatternLayer> forItem(BannerPatternLayers patterns) {
		if (patterns == null || patterns.layers().isEmpty()) return null;
		List<PatternLayer> out = new ArrayList<>(patterns.layers().size());
		for (BannerPatternLayers.Layer l : patterns.layers()) {
			if (out.size() == Layer.MAX_LAYERS) break;
			out.add(layer(l.pattern(), l.color()));
		}
		return out;
	}

	private static PatternLayer layer(Holder<BannerPattern> pattern, DyeColor color) {
		return new PatternLayer(Sheets.getBannerSprite(pattern).texture(), color.getTextureDiffuseColor());
	}

	/** Submits the layers over {@code model}, which the caller has posed (lid opening included). */
	public static <S> void submit(List<PatternLayer> layers, Model<? super S> model, S state, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay) {
		for (int i = 0; i < layers.size(); i++) {
			PatternLayer l = layers.get(i);
			Identifier mask = mask(l.texture());
			if (mask == null) continue;
			collector.order(i + 2).submitModel(model, state, poseStack, RenderTypes.bannerPattern(mask), light, overlay, l.color(), null, 0);
		}
	}

	/** The mask texture for a banner pattern texture, made on first use; null if the pattern texture can't be read. */
	static Identifier mask(Identifier patternTexture) {
		if (MASKS.containsKey(patternTexture)) return MASKS.get(patternTexture);
		Identifier id = null;
		Identifier file = patternTexture.withPath(p -> "textures/" + p + ".png");
		var resources = Minecraft.getInstance().getResourceManager();
		try (InputStream in = resources.open(file); InputStream boxIn = resources.open(BOX_TEXTURE)) {
			NativeImage pattern = NativeImage.read(in), box = NativeImage.read(boxIn);
			NativeImage mask = new NativeImage(MaskLayout.SIZE, MaskLayout.SIZE, true);
			try (pattern; box) { MaskLayout.build(pattern::getPixel, box::getPixel, mask::setPixel); }
			id = Identifier.fromNamespaceAndPath("shulkerstyles", "mask/" + patternTexture.getNamespace() + "/" + patternTexture.getPath());
			Identifier label = id;
			Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(label::toString, mask));
		} catch (Exception e) {
			ShulkerStylesClient.LOG.warn("Can't make the shulker box mask for {}: {}", patternTexture, e.toString());
		}
		MASKS.put(patternTexture, id);
		return id;
	}

	/** Resource packs changed: masks are made again from the new pattern textures. */
	public static void reload() {
		for (Identifier id : MASKS.values()) if (id != null) Minecraft.getInstance().getTextureManager().release(id);
		MASKS.clear();
	}
}
