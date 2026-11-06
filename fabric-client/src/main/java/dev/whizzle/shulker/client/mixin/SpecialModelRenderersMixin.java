package dev.whizzle.shulker.client.mixin;

import com.mojang.serialization.MapCodec;
import dev.whizzle.shulker.client.render.PatternedShulkerBoxRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Registers the item model type {@code shulkerstyles:shulker_box} next to vanilla's. */
@Mixin(SpecialModelRenderers.class)
public abstract class SpecialModelRenderersMixin {
	@Shadow @Final private static ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends SpecialModelRenderer.Unbaked<?>>> ID_MAPPER;

	@Inject(method = "bootstrap", at = @At("TAIL"))
	private static void shulkerstyles$register(CallbackInfo ci) {
		ID_MAPPER.put(PatternedShulkerBoxRenderer.TYPE, PatternedShulkerBoxRenderer.Unbaked.MAP_CODEC);
	}
}
