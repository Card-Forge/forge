package forge.web.deck;

import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckSection;
import forge.game.GameFormat;
import forge.game.GameType;
import forge.gamemodes.limited.CommanderDraftUtil;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.Localizer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/** The engine's verdict on a deck, and each card's problem, marked by the rules the mobile deck editor filters by. */
final class Legality {
    private Legality() {
    }

    /** Each card's problem by name, and the deck's verdict or null when it is legal. */
    record Result(Map<String, String> flags, String verdict) {
    }

    private static CardPool played(final Deck deck) {
        final CardPool cards = new CardPool();
        cards.addAll(deck.getMain());
        if (deck.has(DeckSection.Commander)) {
            cards.addAll(deck.get(DeckSection.Commander));
        }
        return cards;
    }

    /** Whether the format's own rules apply. The engine skips them when deck legality checks are off. A chosen card pool always applies. */
    static boolean enforced() {
        return FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY);
    }

    static Result check(final Deck deck, final LegalityScope check) {
        final DeckFormat df = check.deckFormat();
        // A Commander Draft's sideboard is the rest of the drafted pool. The deck's rules do not apply to it
        final CardPool all = df == DeckFormat.CommanderDraft ? played(deck) : deck.getAllCardsInASinglePool(df.hasCommander(), false);
        final Function<PaperCard, String> problems = cardProblems(check, commanders(deck, check));
        final boolean counted = enforced();
        final Map<String, String> flags = new LinkedHashMap<>();
        for (final Map.Entry<PaperCard, Integer> e : all) {
            final PaperCard card = e.getKey();
            if (flags.containsKey(card.getName())) {
                continue;
            }
            // Count as the engine does. A card with its own copy limit is counted in the main deck only
            final int count = DeckFormat.canHaveSpecificNumberInDeck(card) != null ? deck.getMain().countByName(card.getName())
                    : all.countByName(card.getName());
            final int most = df.getMaxCardCopies(card);
            final String problem = counted && count > most ? Localizer.getInstance().getMessage("lblWebNOfM", count, most) : problems.apply(card);
            if (problem != null) {
                flags.put(card.getName(), problem);
            }
        }
        return new Result(flags, check.unrestricted() ? null : DeckFacts.problem(deck, check.format(), check.pool()));
    }

    /** Why a card cannot go in this deck, or null: the card pool, the format or the commander's colours. Counts are not checked. */
    static Function<PaperCard, String> cardProblems(final LegalityScope check, final List<PaperCard> commanders) {
        final GameFormat pool = check.pool();
        final boolean enforced = !check.unrestricted() && enforced();
        if (pool == null && !enforced) {
            return card -> null;
        }
        final DeckFormat df = check.deckFormat();
        final Predicate<PaperCard> inIdentity = commanders.isEmpty() ? card -> true : df.isLegalCardForCommanderPredicate(commanders);
        final String letters = identityLetters(commanders);
        final String outside = letters.isEmpty() ? Localizer.getInstance().getMessage("lblWebLegalityOutsideColourless")
                : Localizer.getInstance().getMessage("lblWebLegalityOutside", String.join(" ", letters.split("")));
        final String notLegal = Localizer.getInstance().getMessage("lblWebLegalityNotLegalIn", pool != null ? pool.getName() : check.format().toString());
        return card -> {
            if ((pool != null && !pool.getFilterRules().test(card)) || (enforced && !df.isLegalCard(card))) {
                return notLegal;
            }
            return enforced && !commanders.contains(card) && !inIdentity.test(card) ? outside : null;
        };
    }

    /** The commanders' colour identity as WUBRG letters, or "" when there are none. */
    static String identityLetters(final List<PaperCard> commanders) {
        return CardCatalog.wubrg(CommanderDraftUtil.getCommanderColorIdentity(commanders));
    }

    /** The cards whose colours the rest of the deck must share. In Oathbreaker that is the oathbreaker alone, not its spell. */
    static List<PaperCard> commanders(final Deck deck, final LegalityScope check) {
        if (check.unrestricted() || !check.deckFormat().hasCommander()) {
            return List.of();
        }
        if (check.format() == GameType.Oathbreaker) {
            final PaperCard oathbreaker = deck.getOathbreaker();
            return oathbreaker == null ? List.of() : List.of(oathbreaker);
        }
        return deck.getCommanders();
    }
}
