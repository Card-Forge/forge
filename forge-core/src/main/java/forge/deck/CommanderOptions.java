package forge.deck;

import forge.card.CardRules;
import forge.item.PaperCard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Works out which cards of a commander deck could lead it instead of its default commander,
 * and builds a copy of the deck with a different commander. The original deck is never changed.
 */
public final class CommanderOptions {
    private CommanderOptions() { }

    public enum Kind {
        /** The commander(s) the deck was built with. */
        DEFAULT,
        /** An alternate commander suggested by the deck's AltCommanders metadata. */
        SUGGESTED,
        /** Any other card in the deck that is a legal commander for it. */
        OTHER
    }

    public static final class Option {
        private final List<PaperCard> commanders;
        private final Kind kind;

        public Option(final List<PaperCard> commanders, final Kind kind) {
            this.commanders = Collections.unmodifiableList(new ArrayList<>(commanders));
            this.kind = kind;
        }

        public List<PaperCard> getCommanders() {
            return commanders;
        }

        public Kind getKind() {
            return kind;
        }

        public String getName() {
            return commanders.stream().map(PaperCard::getName).collect(Collectors.joining(" + "));
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    /**
     * Returns a copy of the deck led by the given commanders. The deck's current commanders go
     * into the main deck and the new ones are taken out of it, so the card count stays the same.
     */
    public static Deck withCommanders(final Deck base, final List<PaperCard> commanders) {
        final Deck result = new Deck(base);
        final CardPool main = result.getMain();
        for (final PaperCard old : result.getCommanders()) {
            main.add(old);
        }
        final CardPool commanderPool = new CardPool();
        for (final PaperCard commander : commanders) {
            main.remove(commander);
            commanderPool.add(commander);
        }
        result.putSection(DeckSection.Commander, commanderPool);
        return result;
    }

    /**
     * Lists who can lead the deck: its default commander(s) first, then the suggested alternates
     * in metadata order, then every other legal commander in the deck sorted by name.
     * A card that can only lead together with a partner from the deck is listed on its own;
     * {@link #canLeadAlone} tells whether it still needs that partner.
     * When the default is a pair, each half is also listed on its own right after the pair,
     * so it can be given a different partner.
     */
    public static List<Option> getOptions(final Deck base, final DeckFormat format) {
        final List<Option> options = new ArrayList<>();
        final List<PaperCard> defaults = base.getCommanders();
        if (!defaults.isEmpty()) {
            options.add(new Option(defaults, Kind.DEFAULT));
        }
        if (defaults.size() > 1) {
            final List<PaperCard> halves = new ArrayList<>(defaults);
            halves.sort(Comparator.comparing(PaperCard::getName));
            for (final PaperCard commander : halves) {
                if (isCandidate(base, commander, format)) {
                    options.add(new Option(Collections.singletonList(commander), Kind.OTHER));
                }
            }
        }

        final Map<String, PaperCard> candidates = distinctByName(base.getMain());
        for (final PaperCard commander : defaults) {
            candidates.remove(commander.getName().toLowerCase());
        }

        for (final String altName : base.getAltCommanders()) {
            final PaperCard card = candidates.remove(altName.toLowerCase());
            if (card != null && isCandidate(base, card, format)) {
                options.add(new Option(Collections.singletonList(card), Kind.SUGGESTED));
            }
        }

        final List<PaperCard> others = new ArrayList<>();
        for (final PaperCard card : candidates.values()) {
            if (isCandidate(base, card, format)) {
                others.add(card);
            }
        }
        others.sort(Comparator.comparing(PaperCard::getName));
        for (final PaperCard card : others) {
            options.add(new Option(Collections.singletonList(card), Kind.OTHER));
        }
        return options;
    }

    /** Lists the cards of the deck that can partner with the chosen commander, sorted by name. */
    public static List<PaperCard> getPartnerOptions(final Deck base, final PaperCard chosen, final DeckFormat format) {
        final List<PaperCard> partners = new ArrayList<>();
        final CardRules chosenRules = chosen.getRules();
        if (!chosenRules.canBePartnerCommander()) {
            return partners;
        }
        // The deck as led by the chosen card alone, so its old commanders count as candidates too
        final Deck led = withCommanders(base, Collections.singletonList(chosen));
        for (final PaperCard card : distinctByName(led.getMain()).values()) {
            if (!chosenRules.canBePartnerCommanders(card.getRules())) {
                continue;
            }
            final List<PaperCard> pair = Arrays.asList(chosen, card);
            if (format.getCommanderConformanceProblem(withCommanders(base, pair)) == null) {
                partners.add(card);
            }
        }
        partners.sort(Comparator.comparing(PaperCard::getName));
        return partners;
    }

    /** True if the card can lead the deck without a partner. */
    public static boolean canLeadAlone(final Deck base, final PaperCard card, final DeckFormat format) {
        final CardRules rules = card.getRules();
        // A Background can only be a commander next to one that says "Choose a Background"
        if (!format.isLegalCommander(rules) || rules.canBeBackground()) {
            return false;
        }
        return format.getCommanderConformanceProblem(withCommanders(base, Collections.singletonList(card))) == null;
    }

    private static boolean isCandidate(final Deck base, final PaperCard card, final DeckFormat format) {
        if (canLeadAlone(base, card, format)) {
            return true;
        }
        final CardRules rules = card.getRules();
        return format.isLegalCommander(rules) && !rules.canBeBackground()
                && !getPartnerOptions(base, card, format).isEmpty();
    }

    /** One card per name (lower-cased key), keeping the first printing found. */
    private static Map<String, PaperCard> distinctByName(final CardPool pool) {
        final Map<String, PaperCard> result = new LinkedHashMap<>();
        for (final Entry<PaperCard, Integer> entry : pool) {
            result.putIfAbsent(entry.getKey().getName().toLowerCase(), entry.getKey());
        }
        return result;
    }
}
