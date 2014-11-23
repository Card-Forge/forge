package forge.planarconquest;

import forge.deck.Deck;
import forge.deck.generation.DeckGenPool;
import forge.item.PaperCard;

public class ConquestCommander {
    private final PaperCard card;
    private final Deck deck;

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

    public String toString() {
        return card.getName();
    }
}
