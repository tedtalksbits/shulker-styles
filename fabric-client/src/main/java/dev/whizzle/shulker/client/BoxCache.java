package dev.whizzle.shulker.client;

import dev.whizzle.shulker.model.BoxInfo;
import dev.whizzle.shulker.model.Protocol;
import dev.whizzle.shulker.model.Protocol.Pos;

import java.util.*;
import java.util.function.Predicate;

/**
 * What the client knows about shulker boxes around it, and which ones to ask the server about (pure Java,
 * unit-tested). The server answers each position once and then pushes changes, so a known position is never asked
 * again; an unanswered one is asked again after {@link #RETRY_TICKS}.
 */
public final class BoxCache {
	public static final int RETRY_TICKS = 40;
	/** Positions asked per scan, at most; the rest wait for the next scan. */
	public static final int MAX_PER_SCAN = 2 * Protocol.MAX_QUERY;

	private final Map<Pos, BoxInfo> known = new HashMap<>();
	private final Map<Pos, Long> asked = new HashMap<>();

	public BoxInfo get(Pos pos) { return known.get(pos); }
	public int size() { return known.size(); }

	/** The positions among {@code visible} (shulker boxes the client sees) to ask about now, marked as asked. */
	public List<Pos> toAsk(Collection<Pos> visible, long now) {
		List<Pos> out = new ArrayList<>();
		for (Pos p : visible) {
			if (out.size() == MAX_PER_SCAN) break;
			if (known.containsKey(p)) continue;
			Long t = asked.get(p);
			if (t != null && now - t < RETRY_TICKS) continue;
			asked.put(p, now);
			out.add(p);
		}
		return out;
	}

	/** Splits positions into queries of at most {@link Protocol#MAX_QUERY}. */
	public static List<byte[]> queries(List<Pos> positions) {
		List<byte[]> out = new ArrayList<>();
		for (int i = 0; i < positions.size(); i += Protocol.MAX_QUERY)
			out.add(Protocol.query(positions.subList(i, Math.min(positions.size(), i + Protocol.MAX_QUERY))));
		return out;
	}

	/** An answer or a pushed change from the server. */
	public void onBox(Protocol.Box box) {
		asked.remove(box.pos());
		if (box.info() == null) known.remove(box.pos());
		else known.put(box.pos(), box.info());
	}

	/** A box appeared or went away here: forget what was known, so it's asked about again at once. */
	public void forget(Pos pos) {
		known.remove(pos);
		asked.remove(pos);
	}

	/** Forgets positions the server stopped watching (out of range); they're asked again when back in range. */
	public void retain(Predicate<Pos> keep) {
		known.keySet().removeIf(keep.negate());
		asked.keySet().removeIf(keep.negate());
	}

	public void clear() { known.clear(); asked.clear(); }
}
