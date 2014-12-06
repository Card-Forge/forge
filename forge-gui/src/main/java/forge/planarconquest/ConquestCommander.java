package forge.planarconquest;

import forge.deck.Deck;
import forge.deck.generation.DeckGenPool;
import forge.item.PaperCard;

public class ConquestCommander {
    private final PaperCard card;
    private final Deck deck;

    private ConquestAction currentDayAction;

    public ConquestCommander(PaperCard card0, DeckGenPool cardPool0) {
        card = card0;
        deck = ConquestUtil.generateHumanDeck(card0, cardPool0);
    }

    public PaperCard getCard() {
        return card;
    }

    public Deck getDeck() {
        return deck;
    }

    public ConquestAction getCurrentDayAction() {
        return currentDayAction;
    }
    public void setCurrentDayAction(ConquestAction currentDayAction0) {
        currentDayAction = currentDayAction0;
    }

    public String toString() {
        return card.getName();
    }
}
