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
     * The entries for a set picker's marks. An excluded tag is stored as the tag, so sets tagged later are excluded
     * too. A set the tag covers as a whole is stored as kept when unticked, and not stored at all when ticked. A set
     * the tag covers only in part is not stored when marked partly, stored as a code when ticked, and stored as kept
     * when unmarked.
     */
    public static Set<String> entriesFor(Iterable<String> tickedSets, Iterable<String> partlyTickedSets,
                                         Iterable<String> excludedTags, Function<String, Set<String>> setsWithTag,
                                         Function<String, Set<String>> setsWithTaggedPrints) {
        Set<String> ticked = newCodeSet();
        tickedSets.forEach(ticked::add);
        Set<String> partlyTicked = newCodeSet();
        partlyTickedSets.forEach(partlyTicked::add);
        Set<String> covered = newCodeSet();
        Set<String> partlyCovered = newCodeSet();
        Set<String> entries = newCodeSet();
        for (String tag : excludedTags) {
            entries.add(tagEntry(tag));
            covered.addAll(setsWithTag.apply(tag));
            partlyCovered.addAll(setsWithTaggedPrints.apply(tag));
        }
        partlyCovered.removeAll(covered);
        for (String code : covered) {
            if (!ticked.contains(code)) {
                entries.add(KEPT_ENTRY + code);
            }
        }
        for (String code : partlyCovered) {
            if (!ticked.contains(code) && !partlyTicked.contains(code)) {
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

    public static String tagEntry(String tag) {
        return TAG_ENTRY + tag;
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

    /** True for a Funny print outside a kept set, whose card is left out of the card pools once Funny is excluded. */
    public boolean hides(CardEdition edition, String collectorNumber) {
        return !kept.contains(edition.getCode()) && edition.hasTag(CardEdition.FUNNY_TAG, collectorNumber);
    }

    /** True when only some prints of the edition are excluded, through a tag on those prints. */
    public boolean excludesSomePrints(CardEdition edition) {
        if (excludes(edition, null) || kept.contains(edition.getCode())) {
            return false;
        }
        return edition.getPrintTags().stream().anyMatch(tags::contains);
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
