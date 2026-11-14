package forge.card;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * The sets a player excludes, read from a comma separated list. A set code excludes that set. "#Tag" excludes every
 * print carrying the tag, so a tagged set added in a later update is excluded without the player choosing it.
 * "!code" keeps a set that a tag would exclude.
 */
public final class SetExclusions {
    private static final String TAG_ENTRY = "#";
    private static final String KEPT_ENTRY = "!";

    private final Set<String> codes = newCodeSet();
    private final Set<String> tags = newCodeSet();
    private final Set<String> kept = newCodeSet();

    public SetExclusions(Iterable<String> entries) {
        for (String entry : entries) {
            if (entry.startsWith(TAG_ENTRY)) {
                addTrimmed(tags, entry.substring(TAG_ENTRY.length()));
            } else if (entry.startsWith(KEPT_ENTRY)) {
                addTrimmed(kept, entry.substring(KEPT_ENTRY.length()));
            } else {
                addTrimmed(codes, entry);
            }
        }
    }

    private static void addTrimmed(Set<String> target, String value) {
        if (!value.isBlank())
            target.add(value.trim());
    }

    /**
     * The entries for a set picker's ticks. An excluded tag is stored as the tag, so sets tagged later are excluded
     * too. A set the tag covers is stored as kept when unticked, and not stored at all when ticked.
     */
    public static Set<String> entriesFor(Iterable<String> tickedSets, Iterable<String> excludedTags,
                                         Function<String, Set<String>> setsWithTag) {
        Set<String> ticked = newCodeSet();
        tickedSets.forEach(ticked::add);
        Set<String> covered = newCodeSet();
        Set<String> entries = newCodeSet();
        for (String tag : excludedTags) {
            entries.add(TAG_ENTRY + tag);
            covered.addAll(setsWithTag.apply(tag));
        }
        for (String code : covered) {
            if (!ticked.contains(code)) {
                entries.add(KEPT_ENTRY + code);
            }
        }
        for (String code : ticked) {
            if (!covered.contains(code)) {
                entries.add(code);
            }
        }
        return entries;
    }

    public static Set<String> newCodeSet() {
        return new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    }

    public boolean isEmpty() {
        return codes.isEmpty() && tags.isEmpty();
    }

    public Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    /** A null collector number asks about the edition as a whole. */
    public boolean excludes(CardEdition edition, String collectorNumber) {
        if (codes.contains(edition.getCode())) {
            return true;
        }
        if (kept.contains(edition.getCode())) {
            return false;
        }
        for (String tag : tags) {
            if (edition.hasTag(tag, collectorNumber)) {
                return true;
            }
        }
        return false;
    }
}
