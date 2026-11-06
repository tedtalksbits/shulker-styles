package dev.whizzle.shulker.client;

import dev.whizzle.shulker.client.render.MaskLayout;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MaskLayoutTest {
	/** A fake pattern whose every pixel encodes its own position: alpha 255, red = x, green = y. */
	static int encoded(int x, int y) { return 0xFF000000 | (x << 16) | (y << 8); }

	static final MaskLayout.Source SOLID = (x, y) -> 0xFF000000;

	static Map<Long, Integer> build() {
		Map<Long, Integer> out = new HashMap<>();
		MaskLayout.build(MaskLayoutTest::encoded, SOLID, (x, y, argb) -> {
			assertTrue(x >= 0 && y >= 0 && x < MaskLayout.SIZE && y < MaskLayout.SIZE, "inside the texture");
			out.put((long) x << 32 | y, argb);
		});
		return out;
	}

	/** The banner pixel shown at box-texture pixel (bx, by), sampled at its centre texel; null if left bare. */
	static int[] at(Map<Long, Integer> m, int bx, int by) {
		Integer argb = m.get((long) (bx * MaskLayout.SCALE + 1) << 32 | (by * MaskLayout.SCALE + 1));
		return argb == null ? null : new int[]{(argb >> 16) & 0xFF, (argb >> 8) & 0xFF};
	}

	@Test void sidesShowTheMiddleSquareContinuouslyFromLidToBase() {
		Map<Long, Integer> m = build();
		// Lid front side (texture 16,16, 16 x 12): top-left box pixel -> banner (1, 11) (rows 10-29 of the front).
		assertArrayEquals(new int[]{1, 11}, at(m, 16, 16));
		// Bottom row of the lid side is box row 11; sampled at 11.375 -> square row 14 (banner y 11 + 14).
		assertArrayEquals(new int[]{1, 11 + 14}, at(m, 16, 27));
		// Base front side (texture 16,44, 16 x 8) starts at box row 8: banner row 10 (+ 11), overlapping the lid.
		assertArrayEquals(new int[]{1, 11 + 10}, at(m, 16, 44));
		// Bottom row of the base is box row 15 -> the last row of the square (banner y 30).
		assertArrayEquals(new int[]{1, 30}, at(m, 16, 51));
		// Right edge of the face is banner column 20.
		assertArrayEquals(new int[]{20, 11}, at(m, 31, 16));
	}

	@Test void topAndBottomShowTheSquare() {
		Map<Long, Integer> m = build();
		assertArrayEquals(new int[]{1, 11}, at(m, 16, 0), "lid top");
		assertArrayEquals(new int[]{20, 30}, at(m, 31, 15), "lid top, far corner");
		assertArrayEquals(new int[]{1, 11}, at(m, 32, 28), "base bottom");
	}

	@Test void insidesAndUnusedAreaStayBare() {
		Map<Long, Integer> m = build();
		assertNull(at(m, 32, 0), "inside of the lid");
		assertNull(at(m, 16, 28), "inside of the base");
		assertNull(at(m, 0, 0), "unused corner");
		assertNull(at(m, 0, 60), "below the base");
	}

	@Test void transparentPatternPixelsStayBare() {
		Map<Long, Integer> out = new HashMap<>();
		MaskLayout.build((x, y) -> 0, SOLID, (x, y, argb) -> out.put((long) x << 32 | y, argb));
		assertTrue(out.isEmpty());
	}

	@Test void gapsInTheBoxTextureStayBare() {
		Map<Long, Integer> out = new HashMap<>();
		// A box texture with one see-through pixel at (20, 18) on the lid's front side.
		MaskLayout.build(MaskLayoutTest::encoded, (x, y) -> x == 20 && y == 18 ? 0 : 0xFF000000, (x, y, argb) -> out.put((long) x << 32 | y, argb));
		assertNull(at(out, 20, 18));
		assertNotNull(at(out, 21, 18));
	}
}
