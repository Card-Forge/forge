package forge.deck;

import forge.item.PaperCard;
import forge.util.CardTranslation;
import forge.util.Localizer;

import java.util.ArrayList;
import java.util.List;

/**
 * The parts of picking a commander in the lobby that desktop and mobile share:
 * labels, which entry to preselect, and whether a pick still fits a deck.
 * The options themselves come from {@link CommanderOptions}.
 */
public final class CommanderPicks {
    private CommanderPicks() { }

    /** An option's label, tagged "(default)" or "(suggested)" where that applies. */
    public static String describe(final CommanderOptions.Option option) {
        final String names = describe(option.getCommanders());
        switch (option.getKind()) {
            case DEFAULT:
                return tagged(names, "lblDefaultCommanderTag");
            case SUGGESTED:
                return tagged(names, "lblSuggestedCommanderTag");
            default:
                return names;
        }
    }

    /** The commanders' translated names, joined with " + ". */
    public static String describe(final List<PaperCard> commanders) {
        final List<String> names = new ArrayList<>();
        for (final PaperCard commander : commanders) {
            names.add(CardTranslation.getTranslatedName(commander.getName()));
        }
        return String.join(" + ", names);
    }

    /** Who leads the deck right now: the pick, or the deck's own commanders tagged "(default)". */
    public static String describeCurrent(final Deck base, final List<PaperCard> pick) {
        return pick == null ? tagged(describe(base.getCommanders()), "lblDefaultCommanderTag") : describe(pick);
    }

    private static String tagged(final String names, final String tagKey) {
        return names + " (" + Localizer.getInstance().getMessage(tagKey) + ")";
    }

    public static boolean isSame(final List<PaperCard> a, final List<PaperCard> b) {
        return a.size() == b.size() && a.containsAll(b) && b.containsAll(a);
    }

    /** The option to preselect: the one matching the current commanders, or the single card among them. */
    public static int indexOfCurrent(final List<CommanderOptions.Option> options, final List<PaperCard> current) {
        for (int i = 0; i < options.size(); i++) {
            final List<PaperCard> commanders = options.get(i).getCommanders();
            if (isSame(commanders, current) || (commanders.size() == 1 && current.contains(commanders.get(0)))) {
                return i;
            }
        }
        return 0;
    }

    /** The partner to preselect for a commander: its partner among the current commanders, or -1. */
    public static int indexOfCurrentPartner(final List<PaperCard> partners, final PaperCard commander, final List<PaperCard> current) {
        if (!current.contains(commander)) {
            return -1;
        }
        for (int i = 0; i < partners.size(); i++) {
            if (current.contains(partners.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /** True if there is anything to pick: another option, or a partner for a lone default commander. */
    public static boolean hasChoices(final Deck base, final List<CommanderOptions.Option> options, final DeckFormat format) {
        if (options.size() > 1) {
            return true;
        }
        final List<PaperCard> defaults = base.getCommanders();
        return defaults.size() == 1 && !CommanderOptions.getPartnerOptions(base, defaults.get(0), format).isEmpty();
    }

    /** "No partner" is offered unless the commander needs a partner to cover the deck's colors. */
    public static boolean allowsNoPartner(final Deck base, final CommanderOptions.Option option, final DeckFormat format) {
        return option.getKind() == CommanderOptions.Kind.DEFAULT
                || CommanderOptions.canLeadAlone(base, option.getCommanders().get(0), format);
    }

    /** True if the pick still fits the deck, e.g. after the lobby reloaded it. */
    public static boolean isValidPick(final Deck deck, final List<PaperCard> pick, final DeckFormat format) {
        for (final PaperCard card : pick) {
            if (!deck.getMain().contains(card) && !deck.getCommanders().contains(card)) {
                return false;
            }
        }
        return format.getCommanderConformanceProblem(CommanderOptions.withCommanders(deck, pick)) == null;
    }
}
