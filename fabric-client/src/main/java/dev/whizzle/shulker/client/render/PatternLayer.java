package dev.whizzle.shulker.client.render;

import net.minecraft.resources.Identifier;

/** One layer to draw: the banner pattern's texture (e.g. minecraft:entity/banner/creeper) and the dye colour (ARGB). */
public record PatternLayer(Identifier texture, int color) {}
