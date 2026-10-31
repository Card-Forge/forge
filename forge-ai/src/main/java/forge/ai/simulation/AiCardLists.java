package forge.ai.simulation;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

import forge.game.card.Card;

/**
 * The card names the AI's combo logic knows, read from text files instead of being written into the code, so that new
 * cards can be added without a code change.
 * <ul>
 *   <li><b>Roles</b> ({@code ai-card-roles.txt}): {@code role | Card Name | Card Name ...}, see {@link Role}.</li>
 *   <li><b>Two card combos</b> for Commander: the Commander bracket list ({@code commander-bracket-combos.txt},
 *       {@code category | Card A | Card B}) together with {@code ai-commander-combos.txt}, which has the same format
 *       and holds the pairs that should change how the AI plays but not the bracket of a deck. A pair that is in
 *       both, in either order, counts once.</li>
 * </ul>
 * forge-ai cannot see the resource folder, so the front end hands over the readers with {@link #configure}
 * (FModel does it on start up, the way it does for the AI profiles). Nothing is read before it is needed, and a missing
 * file only means that its list is empty (a warning is printed once). Names are compared ignoring case, accents and
 * the back face of a double faced card.
 */
public final class AiCardLists {
    /** What a card is for. The key in the file is the lower case name of the constant. */
    public enum Role {
        /** Living End and kin: cast from the library by cascade or suspend once the swap is worth it. */
        MASS_PAYOFF,
        /** Spells that bring a creature back from a graveyard. */
        REANIMATION_SPELL,
        /** Creatures worth far more reanimated than cast. */
        BIG_TARGET,
        STORM_PAYOFF,
        /** Put creatures from hand onto the battlefield for free (Show and Tell, Sneak Attack...). */
        CHEAT_SPELL,
        /** Huge creatures worth cheating into play. */
        CHEAT_TARGET,
        /** Overrun finishers that wait in hand until the board makes them lethal. */
        OVERRUN,
        COPY_ENGINE,
        COPY_TARGET,
        MILL_PAYOFF,
        DREDGE_PAYOFF,
        /** Permanents of which a second copy does nothing (ComboGuard). */
        COMBO_ENGINE,
        /** Spells that put cards from hand onto the battlefield when they resolve (ComboGuard). */
        HAND_CHEAT
    }

    /** A two card combo, with the names as they are written in the file. */
    public static final class Pair {
        public final String first;
        public final String second;
        final String firstKey;
        final String secondKey;

        Pair(final String first, final String second) {
            this.first = first;
            this.second = second;
            this.firstKey = normalize(first);
            this.secondKey = normalize(second);
        }

        /** The same for (A, B) and (B, A). */
        String key() {
            return firstKey.compareTo(secondKey) <= 0 ? firstKey + "\u0000" + secondKey : secondKey + "\u0000" + firstKey;
        }

        @Override
        public String toString() {
            return first + " + " + second;
        }
    }

    private static final class Data {
        final Map<Role, Set<String>> roles = new EnumMap<>(Role.class);
        final Map<String, List<Pair>> byCard = new HashMap<>();
        int pairCount;
    }

    private static Supplier<List<String>> rolesFile;
    private static List<Supplier<List<String>>> comboFiles = Collections.emptyList();
    private static Data data;
    private static boolean warned;

    private AiCardLists() {
    }

    /**
     * Says where the lists are. Each supplier returns the lines of one file (an empty list or null for a missing one).
     * The combo files are read in order, the first one wins a duplicate. Forgets what was read before.
     */
    @SafeVarargs
    public static synchronized void configure(final Supplier<List<String>> rolesLines,
                                              final Supplier<List<String>>... comboLines) {
        rolesFile = rolesLines;
        comboFiles = comboLines == null ? Collections.<Supplier<List<String>>>emptyList()
                : java.util.Arrays.asList(comboLines);
        data = null;
    }

    /** Forgets what was read, so the next question reads the files again (after a file was edited, in tests). */
    public static synchronized void reload() {
        data = null;
    }

    private static synchronized Data data() {
        if (data == null) {
            data = load();
        }
        return data;
    }

    // ------------------------------------------------------------------------------------------------- the roles

    /** The names of the role (never null; empty when the file has none). Compared with {@link #has}. */
    public static Set<String> names(final Role role) {
        final Set<String> s = data().roles.get(role);
        return s == null ? Collections.<String>emptySet() : s;
    }

    /** Whether the card with this name has the role. */
    public static boolean has(final Role role, final String cardName) {
        return cardName != null && names(role).contains(normalize(cardName));
    }

    public static boolean has(final Role role, final Card card) {
        return card != null && has(role, card.getName());
    }

    // ---------------------------------------------------------------------------------------- the two card combos

    /** How many different two card combos are known. */
    public static int pairCount() {
        return data().pairCount;
    }

    /**
     * The known combos whose two cards are both in the list. The names are already {@link #normalize normalized}
     * (add both halves of a double faced card yourself, see {@link #deckKeys}). Each combo is listed once.
     */
    public static List<Pair> combosIn(final Set<String> normalizedNames) {
        final Map<String, Pair> found = new LinkedHashMap<>();
        final Map<String, List<Pair>> index = data().byCard;
        for (final String n : normalizedNames) {
            final List<Pair> candidates = index.get(n);
            if (candidates == null) {
                continue;
            }
            for (final Pair p : candidates) {
                if (normalizedNames.contains(p.firstKey) && normalizedNames.contains(p.secondKey)) {
                    found.putIfAbsent(p.key(), p);
                }
            }
        }
        return new ArrayList<>(found.values());
    }

    /** The normalized names of cards, with the faces of a card named "Front // Back" as well as the whole name. */
    public static Set<String> deckKeys(final Iterable<String> cardNames) {
        final Set<String> keys = new HashSet<>();
        for (final String name : cardNames) {
            keys.addAll(keysOf(name));
        }
        return keys;
    }

    static Set<String> keysOf(final String cardName) {
        final Set<String> keys = new HashSet<>();
        keys.add(normalize(cardName));
        if (cardName.contains("//")) {
            for (final String face : cardName.split("//")) {
                keys.add(normalize(face));
            }
        }
        return keys;
    }

    /** Lower case, no accents, single spaces: "Éomer,  Marshal" and "eomer, marshal" are the same card. */
    public static String normalize(final String name) {
        final String plain = Normalizer.normalize(name.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return plain.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    // ---------------------------------------------------------------------------------------------- the reading

    private static Data load() {
        final Data d = new Data();
        read(d);
        return d;
    }

    private static void read(final Data d) {
        final List<String> roleLines = lines(rolesFile);
        if (rolesFile == null && comboFiles.isEmpty() && !warned) {
            warned = true;
            System.err.println("AiCardLists: no card list files were configured, the combo plan has no cards to look for.");
        }
        for (final String raw : roleLines) {
            final String line = stripComment(raw);
            if (line.isEmpty()) {
                continue;
            }
            final String[] parts = line.split("\\s*\\|\\s*");
            if (parts.length < 2) {
                continue;
            }
            final Role role = roleOf(parts[0]);
            if (role == null) {
                System.err.println("AiCardLists: unknown role '" + parts[0] + "' ignored");
                continue;
            }
            final Set<String> set = d.roles.computeIfAbsent(role, r -> new TreeSet<String>());
            for (int i = 1; i < parts.length; i++) {
                if (!parts[i].trim().isEmpty()) {
                    set.add(normalize(parts[i]));
                }
            }
        }
        for (final Role r : d.roles.keySet()) {
            d.roles.put(r, Collections.unmodifiableSet(d.roles.get(r)));
        }

        final Set<String> seen = new HashSet<>();
        for (final Supplier<List<String>> file : comboFiles) {
            for (final String raw : lines(file)) {
                final String line = stripComment(raw);
                if (line.isEmpty()) {
                    continue;
                }
                // category | Card A | Card B
                final String[] parts = line.split("\\s*\\|\\s*");
                if (parts.length < 3 || parts[1].trim().isEmpty() || parts[2].trim().isEmpty()) {
                    continue;
                }
                final Pair p = new Pair(parts[1].trim(), parts[2].trim());
                if (p.firstKey.equals(p.secondKey) || !seen.add(p.key())) {
                    continue; // the same card twice, or a pair that an earlier file already has
                }
                d.byCard.computeIfAbsent(p.firstKey, k -> new ArrayList<Pair>()).add(p);
                d.byCard.computeIfAbsent(p.secondKey, k -> new ArrayList<Pair>()).add(p);
                d.pairCount++;
            }
        }
    }

    private static Role roleOf(final String key) {
        final String k = key.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        for (final Role r : Role.values()) {
            if (r.name().equals(k)) {
                return r;
            }
        }
        return null;
    }

    private static List<String> lines(final Supplier<List<String>> file) {
        if (file == null) {
            return Collections.emptyList();
        }
        try {
            final List<String> l = file.get();
            return l == null ? Collections.<String>emptyList() : l;
        } catch (final RuntimeException e) {
            System.err.println("AiCardLists: could not read a card list: " + e);
            return Collections.emptyList();
        }
    }

    private static String stripComment(final String line) {
        final int i = line.indexOf('#');
        return (i < 0 ? line : line.substring(0, i)).trim();
    }
}
