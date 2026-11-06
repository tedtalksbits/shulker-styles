package dev.whizzle.shulker.client;

import dev.whizzle.shulker.model.BoxInfo;
import dev.whizzle.shulker.model.Protocol;
import dev.whizzle.shulker.model.Protocol.Pos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoxCacheTest {
	final Pos a = new Pos(1, 2, 3), b = new Pos(4, 5, 6);

	@Test void asksOnceThenWaitsForTheAnswer() {
		BoxCache c = new BoxCache();
		assertEquals(List.of(a, b), c.toAsk(List.of(a, b), 0));
		assertEquals(List.of(), c.toAsk(List.of(a, b), 10), "already asked");
		c.onBox(new Protocol.Box(a, BoxInfo.PLAIN));
		assertEquals(BoxInfo.PLAIN, c.get(a));
		assertEquals(List.of(b), c.toAsk(List.of(a, b), BoxCache.RETRY_TICKS), "known positions aren't asked again; unanswered ones are retried");
	}

	@Test void pushedRemovalForgets() {
		BoxCache c = new BoxCache();
		c.onBox(new Protocol.Box(a, BoxInfo.PLAIN));
		c.onBox(new Protocol.Box(a, null));
		assertNull(c.get(a));
		assertEquals(0, c.size());
	}

	@Test void retainForgetsOutOfRange() {
		BoxCache c = new BoxCache();
		c.onBox(new Protocol.Box(a, BoxInfo.PLAIN));
		c.onBox(new Protocol.Box(b, BoxInfo.PLAIN));
		c.retain(p -> p.equals(a));
		assertNotNull(c.get(a));
		assertNull(c.get(b));
		assertEquals(List.of(b), c.toAsk(List.of(b), 0), "back in range: asked again");
	}

	@Test void scansAreCappedAndSplitIntoQueries() {
		List<Pos> many = new ArrayList<>();
		for (int i = 0; i < 300; i++) many.add(new Pos(i, 0, 0));
		List<Pos> ask = new BoxCache().toAsk(many, 0);
		assertEquals(BoxCache.MAX_PER_SCAN, ask.size());
		List<byte[]> qs = BoxCache.queries(ask);
		assertEquals(2, qs.size());
		assertEquals(ask.subList(0, Protocol.MAX_QUERY), Protocol.readQuery(qs.get(0)));
	}

	@Test void forgetAsksAgainAtOnce() {
		BoxCache c = new BoxCache();
		c.onBox(new Protocol.Box(a, BoxInfo.PLAIN));
		c.toAsk(List.of(b), 0);
		c.forget(a);
		c.forget(b);
		assertNull(c.get(a), "a box placed where another was: nothing stale shown");
		assertEquals(List.of(a, b), c.toAsk(List.of(a, b), 1), "asked straight away, not after the retry delay");
	}
}
