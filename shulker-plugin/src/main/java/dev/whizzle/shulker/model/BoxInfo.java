package dev.whizzle.shulker.model;

import java.util.List;

/**
 * What the mod needs to draw one shulker box: its pattern layers (bottom first), its name ("" if not renamed), and its
 * label icon: an item id ("" for none) and whether the player chose it (otherwise it's the most common item inside).
 */
public record BoxInfo(List<Layer> layers, String name, String icon, boolean iconChosen) {
    public static final BoxInfo PLAIN = new BoxInfo(List.of(), "", "", false);
    public static final int MAX_NAME_BYTES = 256 * 4;

    public BoxInfo {
        layers = List.copyOf(layers);
        if (layers.size() > Layer.MAX_LAYERS) throw new IllegalArgumentException("Too many layers");
        name = name == null ? "" : name;
        icon = icon == null ? "" : icon;
    }

    public void write(Wire.Writer out) {
        out.varInt(layers.size());
        for (Layer l : layers) out.utf(l.pattern()).varInt(l.color());
        out.utf(name).utf(icon).bool(iconChosen);
    }

    public static BoxInfo read(Wire.Reader in) {
        int n = in.varInt();
        if (n < 0 || n > Layer.MAX_LAYERS) throw new IllegalArgumentException("Bad layer count " + n);
        Layer[] layers = new Layer[n];
        for (int i = 0; i < n; i++) layers[i] = new Layer(in.utf(128), in.varInt());
        return new BoxInfo(List.of(layers), in.utf(MAX_NAME_BYTES), in.utf(128), in.bool());
    }
}
