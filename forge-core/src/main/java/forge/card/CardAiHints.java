package forge.card;

import java.util.function.Predicate;

/** 
 * CardAiHints holds all the different types of card hints for AI decks.
 *
 */
public class CardAiHints {

    private final boolean isRemovedFromAIDecks;
    private final boolean isRemovedFromRandomDecks;
    private final boolean isRemovedFromNonCommanderDecks;

    private final DeckHints deckHints;
    private final DeckHints deckNeeds;
    private final DeckHints deckHas;

    public CardAiHints(boolean remAi, boolean remRandom, boolean remUnlessCommander, DeckHints dh, DeckHints dn, DeckHints has) {
        isRemovedFromAIDecks = remAi;
        isRemovedFromRandomDecks = remRandom;
        isRemovedFromNonCommanderDecks = remUnlessCommander;
        deckHints = dh;
        deckNeeds = dn;
        deckHas = has;
    }

    /**
     * Gets the rem ai decks.
     * 
     * @return the rem ai decks
     */
    public boolean getRemAIDecks() {
        return this.isRemovedFromAIDecks;
    }

    /**
     * Cards marked AI:RemoveDeck:All that the Flat Monte Carlo AI does play (the cantrips and mana of the combo decks,
     * the pieces of its combos): decks built for the AI keep them. Set by the model at start up; nothing is kept
     * until then, or when Flat MC is off.
     */
    private static volatile Predicate<String> keptForFlatMc = name -> false;

    public static void setKeptForFlatMc(final Predicate<String> kept) {
        keptForFlatMc = kept == null ? name -> false : kept;
    }

    /** Whether decks built at random (for the AI as well) leave this card out: AI:RemoveDeck:Random, and not one Flat MC plays. */
    public static boolean isRemovedFromRandomDecks(final CardRules rules) {
        return rules.getAiHints().getRemRandomDecks() && !keptForFlatMc.test(rules.getName());
    }

    /** Whether the AI's decks leave this card out: marked AI:RemoveDeck:All, and not one Flat MC plays. */
    public static boolean isRemovedFromAiDecks(final CardRules rules) {
        return rules.getAiHints().getRemAIDecks() && !keptForFlatMc.test(rules.getName());
    }

    /**
     * Gets the rem random decks.
     * 
     * @return the rem random decks
     */
    public boolean getRemRandomDecks() {
        return this.isRemovedFromRandomDecks;
    }

    /**
     * Gets the rem random decks.
     *
     * @return the rem random decks
     */
    public boolean getRemNonCommanderDecks() {
        return this.isRemovedFromNonCommanderDecks;
    }

        /**
         * @return the deckHints
         */
    public DeckHints getDeckHints() {
        return deckHints;
    }

    /**
     * @return the deckHints
     */
    public DeckHints getDeckNeeds() {
        return deckNeeds;
    }

    /**
     * @return the deckHints
     */
    public DeckHints getDeckHas() {
        return deckHas;
    }

    /**
     * Gets the ai status comparable.
     * 
     * @return the ai status comparable
     */
    public Integer getAiStatusComparable() {
        if (this.isRemovedFromAIDecks && this.isRemovedFromRandomDecks) {
            return 3;
        } else if (this.isRemovedFromAIDecks) {
            return 4;
        } else if (this.isRemovedFromRandomDecks) {
            return 2;
        } else {
            return 1;
        }
    }

}
