package forge.planarconquest;

import forge.deck.Deck;
import forge.deck.generation.DeckGenPool;
import forge.item.PaperCard;

public class ConquestCommander {
    private final PaperCard card;
    private final Deck deck;

    public ConquestCommander(PaperCard card0, DeckGenPool cardPool0, boolean forAi) {
        card = card0;
        deck = ConquestUtil.generateDeck(card0, cardPool0, forAi);
    }

    public String getName() {
        return card.getName();
    }

    public String getPlayerName() {
        String name = card.getName();
        int idx = name.indexOf(' ');
        if (idx != -1) {
            name = name.substring(0, idx);
        }
        if (name.endsWith(",") || name.endsWith("-")) {
            name = name.substring(0, name.length() - 1).trim();
        }
        return name;
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
