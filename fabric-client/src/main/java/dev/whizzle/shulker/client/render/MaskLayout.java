package dev.whizzle.shulker.client.render;

/**
 * Lays one banner pattern out on the shulker box's texture layout, so the box model can draw it as a second skin
 * (pure Java, unit-tested). The output is {@value #SIZE} x {@value #SIZE}: the box texture's 64 x 64 layout at
 * {@value #SCALE}x, so the pattern keeps its own detail.
 * <p>
 * Every outside face shows the middle square of the pattern (the owner's pick, "B: square crop"): banner pattern
 * columns 0-19 across, rows 10-29 down. A closed box's side is 16 pixels high: the lid covers the top 12 (box rows
 * 0-11), the base the bottom 8 (rows 8-15, its top 4 hidden under the lid until it opens), so the picture is
 * continuous on a closed box and the lid carries its part away when it opens. The lid's top and the base's bottom
 * show the same square; the insides stay bare, and so do pixels where the box texture itself is see-through (its
 * cut-outs), so a pattern never fills a gap in the box.
 */
public final class MaskLayout {
	private MaskLayout() {}

	public static final int SCALE = 4, SIZE = 64 * SCALE;
	/** The banner texture's front face: x 1-20, y 1-40 of its 64 x 64 image. */
	static final int BANNER_X = 1, BANNER_Y = 1, BANNER_W = 20, BANNER_H = 40;

	/** ARGB pixel of the banner pattern texture (64 x 64). */
	public interface Source { int argb(int x, int y); }
	/** Receives the mask's pixels (ARGB); pixels never set stay transparent. */
	public interface Target { void set(int x, int y, int argb); }

	/** Shulker box texture regions (in box pixels): x, y, width, height, first box row of the side picture (-1: square). */
	record Region(int x, int y, int w, int h, int firstRow) {}

	static final Region[] REGIONS = {
		// Lid (texture offset 0,0; 16 x 12 x 16): outer top, then the four sides.
		new Region(16, 0, 16, 16, -1),
		new Region(0, 16, 16, 12, 0), new Region(16, 16, 16, 12, 0), new Region(32, 16, 16, 12, 0), new Region(48, 16, 16, 12, 0),
		// Base (texture offset 0,28; 16 x 8 x 16): outer bottom, then the four sides.
		new Region(32, 28, 16, 16, -1),
		new Region(0, 44, 16, 8, 8), new Region(16, 44, 16, 8, 8), new Region(32, 44, 16, 8, 8), new Region(48, 44, 16, 8, 8),
	};

	/** Banner pattern pixel for a point on a box face (u across, row down, both 0-16 in box pixels). */
	static int[] patternPixel(double u, double row) {
		int px = (int) Math.min(BANNER_W - 1, u * BANNER_W / 16);
		int py = (int) Math.min(BANNER_W - 1, row * BANNER_W / 16);   // a 20 x 20 square ...
		return new int[]{BANNER_X + px, BANNER_Y + (BANNER_H - BANNER_W) / 2 + py};   // ... from the middle (rows 10-29)
	}

	/** {@code box} is the shulker box texture (64 x 64), only its alpha is used. */
	public static void build(Source pattern, Source box, Target out) {
		for (Region r : REGIONS)
			for (int y = 0; y < r.h() * SCALE; y++)
				for (int x = 0; x < r.w() * SCALE; x++) {
					if ((box.argb(r.x() + x / SCALE, r.y() + y / SCALE) >>> 24) == 0) continue;
					double u = (x + 0.5) / SCALE, v = (y + 0.5) / SCALE;
					double row = r.firstRow() < 0 ? v : r.firstRow() + v;
					int[] p = patternPixel(u, row);
					int argb = pattern.argb(p[0], p[1]);
					if ((argb >>> 24) != 0) out.set(r.x() * SCALE + x, r.y() * SCALE + y, argb);
				}
	}
}
