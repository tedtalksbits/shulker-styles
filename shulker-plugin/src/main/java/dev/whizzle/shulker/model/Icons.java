package dev.whizzle.shulker.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The automatic label icon: the item a box holds most of (pure Java, shared). */
public final class Icons {
    private Icons() {}

    /** One inventory slot: item id and count; null or count 0 for an empty slot. */
    public record Slot(String id, int count) {}

    /**
     * The item id with the highest total count across the slots; on a tie, the one that appears first. "" if empty.
     */
    public static String mostCommon(List<Slot> slots) {
        Map<String, Integer> totals = new LinkedHashMap<>();
        for (Slot s : slots) if (s != null && s.count() > 0 && s.id() != null && !s.id().isEmpty()) totals.merge(s.id(), s.count(), Integer::sum);
        String best = "";
        int most = 0;
        for (Map.Entry<String, Integer> e : totals.entrySet()) if (e.getValue() > most) { best = e.getKey(); most = e.getValue(); }
        return best;
    }
}
