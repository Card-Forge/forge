package forge.web.match;

import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** Checks and converts the browser's answers to the game's questions. These are pure functions, so each can be tested without a game. */
final class Answers {
    private Answers() {
    }

    static <T> List<Integer> indicesOf(final List<T> items, final Collection<T> subset) {
        final List<Integer> out = new ArrayList<>();
        if (subset != null) {
            for (int i = 0; i < items.size(); i++) {
                if (subset.contains(items.get(i))) {
                    out.add(i);
                }
            }
        }
        return out;
    }

    static Predicate<JsonElement> indexList(final int size, final int min, final int max) {
        return v -> {
            if (!v.isJsonArray()) {
                return false;
            }
            final Set<Integer> seen = new HashSet<>();
            for (final JsonElement e : v.getAsJsonArray()) {
                if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) {
                    return false;
                }
                final int i = e.getAsInt();
                if (i < 0 || i >= size || !seen.add(i)) {
                    return false;
                }
            }
            return seen.size() >= min && (max < 0 || seen.size() <= max);
        };
    }

    static Predicate<JsonElement> singleIndex(final int size) {
        return v -> v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber() && v.getAsInt() >= 0 && v.getAsInt() < size;
    }

    static Predicate<JsonElement> amounts(final int count, final int total, final int perMin, final boolean maySkip) {
        return v -> {
            if (v.isJsonNull()) {
                return maySkip;
            }
            if (!v.isJsonArray() || v.getAsJsonArray().size() != count) {
                return false;
            }
            int sum = 0;
            for (final JsonElement e : v.getAsJsonArray()) {
                if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber() || e.getAsInt() < perMin) {
                    return false;
                }
                sum += e.getAsInt();
            }
            return sum == total;
        };
    }

    static <T> List<T> pick(final List<T> items, final JsonElement indices) {
        final List<T> out = new ArrayList<>();
        for (final JsonElement e : indices.getAsJsonArray()) {
            out.add(items.get(e.getAsInt()));
        }
        return out;
    }

    /** Gives each blocker in turn the damage that kills it, given in {@code lethal}, and the rest to the last recipient. */
    static int[] defaultCombatSplit(final List<Integer> lethal, final int damage, final boolean hasDefender) {
        final int n = lethal.size() + (hasDefender ? 1 : 0);
        final int[] split = new int[n];
        int remaining = damage;
        for (int i = 0; i < lethal.size() && remaining > 0; i++) {
            final int assigned = Math.min(remaining, lethal.get(i));
            split[i] = assigned;
            remaining -= assigned;
        }
        if (n > 0) {
            split[n - 1] += remaining;
        }
        return split;
    }
}
