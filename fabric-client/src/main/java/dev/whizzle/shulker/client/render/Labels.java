package dev.whizzle.shulker.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.whizzle.shulker.client.Settings;
import dev.whizzle.shulker.client.ShulkerStylesClient;
import dev.whizzle.shulker.model.BoxInfo;
import dev.whizzle.shulker.model.Protocol;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.ShulkerBoxRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Labels on placed boxes (the owner's pick "B"): the label icon on every face except the one the box stands on, like
 * an item in an invisible item frame, and the name floating over the box you look at, like a name tag. Only within
 * {@link Settings#labelDistance}; icons hide while the box is open (the lid would carry them away).
 */
public final class Labels {
	private Labels() {}

	/** Fills the label for this frame. Faces against a solid block or another box get no icon. */
	public static void extract(ShulkerBoxRenderState state, BoxExtras.Label label, Vec3 camera) {
		label.clear();
		Minecraft mc = Minecraft.getInstance();
		Level level = mc.level;
		BlockPos pos = state.blockPos;
		if (!Settings.labels || level == null || pos == null) return;
		double d = Settings.labelDistance;
		if (camera.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > d * d) return;
		BoxInfo info = ShulkerStylesClient.BOXES.get(new Protocol.Pos(pos.getX(), pos.getY(), pos.getZ()));
		if (info == null) return;
		ItemStack icon = ClientIcons.stack(info.icon());

		if (!icon.isEmpty() && state.progress <= 0) {
			Direction base = state.direction.getOpposite();
			for (Direction face : Direction.values()) {
				if (face == base) continue;
				BlockPos front = pos.relative(face);
				BlockState there = level.getBlockState(front);
				if (there.isSolidRender() || there.getBlock() instanceof ShulkerBoxBlock) continue;
				label.faces |= 1 << face.ordinal();
				label.light[face.ordinal()] = LightCoordsUtil.getLightCoords(level, front);
			}
			if (label.faces != 0) mc.getItemModelResolver().updateForTopItem(label.icon, icon, ItemDisplayContext.FIXED, level, null, 0);
		}

		if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos)) {
			if (!info.name().isEmpty()) label.name = Component.literal(info.name());
			else if (!icon.isEmpty()) label.name = icon.getHoverName().copy().withStyle(ChatFormatting.GRAY);
		}
	}

	public static void submit(ShulkerBoxRenderState state, BoxExtras.Label label, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
		if (label.faces != 0 && !label.icon.isEmpty()) {
			for (Direction face : Direction.values()) {
				if ((label.faces & (1 << face.ordinal())) == 0) continue;
				pose.pushPose();
				pose.translate(0.5f, 0.5f, 0.5f);
				pose.translate(face.getStepX() * 0.5f, face.getStepY() * 0.5f, face.getStepZ() * 0.5f);
				// As an item frame turns its item to face out (after this, +Z points into the box).
				if (face.getAxis().isHorizontal()) pose.rotateDegrees(Axis.YP, 180.0f - face.toYRot());
				else {
					pose.rotateDegrees(Axis.XP, -90.0f * face.getAxisDirection().getStep());
					pose.rotateDegrees(Axis.YP, 180.0f);
				}
				pose.translate(0.0f, 0.0f, -0.0625f);
				pose.scale(0.5f, 0.5f, 0.5f);
				label.icon.submit(pose, out, label.light[face.ordinal()], OverlayTexture.NO_OVERLAY, 0);
				pose.popPose();
			}
		}
		if (label.name != null && camera != null && camera.initialized) {
			Font font = Minecraft.getInstance().font;
			FormattedCharSequence text = label.name.getVisualOrderText();
			float x = -font.width(text) / 2.0f, y = -4;
			pose.pushPose();
			pose.translate(0.5f, 1.35f, 0.5f);
			pose.rotate(camera.orientation);
			pose.scale(0.025f, -0.025f, 0.025f);
			// Like a name tag: a dim copy with a backing shows through blocks, the bright copy where it's in view.
			out.submitText(pose, x, y, text, false, Font.DisplayMode.SEE_THROUGH, LightCoordsUtil.FULL_BRIGHT, 0x80FFFFFF, 0x40000000, 0);
			out.submitText(pose, x, y, text, false, Font.DisplayMode.NORMAL, LightCoordsUtil.FULL_BRIGHT, 0xFFFFFFFF, 0, 0);
			pose.popPose();
		}
	}
}
