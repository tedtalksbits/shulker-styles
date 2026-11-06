package dev.whizzle.shulker.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3fc;

import java.util.List;
import java.util.function.Consumer;

/**
 * The shulker box item renderer with patterns: vanilla's shulker box item drawing ({@code minecraft:shulker_box}),
 * plus the item's {@code banner_patterns}. The mod's {@code assets/minecraft/items/*_shulker_box.json} switch the 17
 * box items to this type ({@code shulkerstyles:shulker_box}, same fields as vanilla's).
 */
public final class PatternedShulkerBoxRenderer implements SpecialModelRenderer<List<PatternLayer>> {
	public static final Identifier TYPE = Identifier.fromNamespaceAndPath("shulkerstyles", "shulker_box");

	private final ShulkerBoxRenderer renderer;
	private final float openness;
	private final SpriteId sprite;

	PatternedShulkerBoxRenderer(ShulkerBoxRenderer renderer, float openness, SpriteId sprite) {
		this.renderer = renderer;
		this.openness = openness;
		this.sprite = sprite;
	}

	@Override
	public List<PatternLayer> extractArgument(ItemStack stack) {
		return Patterns.forItem(stack.get(DataComponents.BANNER_PATTERNS));
	}

	@Override
	public void submit(List<PatternLayer> layers, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outline) {
		Patterns.CURRENT.set(layers);
		try { renderer.submit(poseStack, collector, light, overlay, openness, null, sprite, outline); }
		finally { Patterns.CURRENT.remove(); }
	}

	@Override
	public void getExtents(Consumer<Vector3fc> output) { renderer.getExtents(openness, output); }

	public record Unbaked(Identifier texture, float openness) implements SpecialModelRenderer.Unbaked<List<PatternLayer>> {
		public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Identifier.CODEC.fieldOf("texture").forGetter(Unbaked::texture),
			Codec.FLOAT.optionalFieldOf("openness", 0.0f).forGetter(Unbaked::openness)
		).apply(i, Unbaked::new));

		@Override public MapCodec<Unbaked> type() { return MAP_CODEC; }

		@Override
		public PatternedShulkerBoxRenderer bake(SpecialModelRenderer.BakingContext context) {
			return new PatternedShulkerBoxRenderer(new ShulkerBoxRenderer(context), openness, Sheets.SHULKER_MAPPER.apply(texture));
		}
	}
}
