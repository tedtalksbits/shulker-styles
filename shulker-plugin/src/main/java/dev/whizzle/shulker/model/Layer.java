package dev.whizzle.shulker.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One banner pattern layer: the pattern's registry id (e.g. {@code minecraft:creeper}) and its dye colour as
 * Minecraft's dye id (0 white ... 15 black, {@link #COLORS}). Pure Java, shared by the plugin and the client mod.
 */
public record Layer(String pattern, int color) {
    /** Minecraft's dye colours in id order (DyeColor ids 0-15, the same order as Bukkit's DyeColor). */
    public static final List<String> COLORS = List.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
        "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black");
    /** Most layers stored or sent. A loom makes at most 6; commands can make more. */
    public static final int MAX_LAYERS = 16;

    public Layer {
        if (pattern == null || pattern.isBlank() || pattern.length() > 128) throw new IllegalArgumentException("Bad pattern id");
        if (color < 0 || color >= COLORS.size()) throw new IllegalArgumentException("Bad colour " + color);
    }

    public String colorName() { return COLORS.get(color); }

    /**
     * Text form for the placed box's persistent data: {@code minecraft:creeper red,minecraft:border blue}. Readable
     * with {@code /data get block}, and independent of Minecraft's own encodings.
     */
    public static String toText(List<Layer> layers) {
        StringBuilder sb = new StringBuilder();
        for (Layer l : layers) {
            if (!sb.isEmpty()) sb.append(',');
            sb.append(l.pattern()).append(' ').append(l.colorName());
        }
        return sb.toString();
    }

    public static List<Layer> fromText(String text) {
        List<Layer> out = new ArrayList<>();
        if (text == null || text.isBlank()) return out;
        for (String part : text.split(",")) {
            String[] pc = part.trim().split(" ");
            if (pc.length != 2) throw new IllegalArgumentException("Bad layer '" + part + "'");
            int color = COLORS.indexOf(pc[1].toLowerCase(Locale.ROOT));
            if (color < 0) throw new IllegalArgumentException("Bad colour '" + pc[1] + "'");
            out.add(new Layer(pc[0], color));
            if (out.size() > MAX_LAYERS) throw new IllegalArgumentException("Too many layers");
        }
        return out;
    }
}
