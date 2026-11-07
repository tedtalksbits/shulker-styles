package dev.whizzle.shulker.client.render;

import dev.whizzle.shulker.model.Icons;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Label icons as items to draw: from an item id, or worked out from a box item (chosen icon, else most common inside). */
public final class ClientIcons {
	private ClientIcons() {}

	private static final Map<String, ItemStack> STACKS = new HashMap<>();

	/** The item to draw for an id; EMPTY for "" or an unknown id. */
	public static ItemStack stack(String id) {
		if (id == null || id.isEmpty()) return ItemStack.EMPTY;
		return STACKS.computeIfAbsent(id, i -> {
			Identifier key = Identifier.tryParse(i);
			return key == null ? ItemStack.EMPTY : BuiltInRegistries.ITEM.getOptional(key).map(ItemStack::new).orElse(ItemStack.EMPTY);
		});
	}

	public static boolean isBox(ItemStack stack) {
		return stack.getItem() instanceof BlockItem b && b.getBlock() instanceof ShulkerBoxBlock;
	}

	/**
	 * The label icon of a box item: the one chosen with the box screen's icon slot or {@code /shulkerstyles icon}
	 * (the plugin keeps it in the item's custom data), else the item it holds most of. "" for none.
	 */
	public static String iconId(ItemStack box) {
		CustomData data = box.get(DataComponents.CUSTOM_DATA);
		if (data != null && !data.isEmpty()) {
			String chosen = data.copyTag().getCompound("PublicBukkitValues").flatMap(t -> t.getString("shulkerstyles:icon")).orElse("");
			if (!chosen.isEmpty()) return chosen;
		}
		ItemContainerContents contents = box.get(DataComponents.CONTAINER);
		if (contents == null) return "";
		List<Icons.Slot> slots = new ArrayList<>();
		for (ItemStackTemplate t : contents.nonEmptyItems()) slots.add(new Icons.Slot(BuiltInRegistries.ITEM.getKey(t.item().value()).toString(), t.count()));
		return Icons.mostCommon(slots);
	}
}
