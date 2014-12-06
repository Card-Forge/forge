package forge.planarconquest;

import forge.deck.Deck;
import forge.deck.generation.DeckGenPool;
import forge.item.PaperCard;
import forge.planarconquest.ConquestPlane.Region;

public class ConquestCommander {
    private final PaperCard card;
    private final Deck deck;

    private Region deployedRegion;
    private ConquestAction currentDayAction;

    public ConquestCommander(PaperCard card0, DeckGenPool cardPool0, boolean forAi) {
        card = card0;
        deck = ConquestUtil.generateDeck(card0, cardPool0, forAi);
    }

    public String getName() {
        return card.getName();
    }

    public PaperCard getCard() {
        return card;
    }

    public Deck getDeck() {
        return deck;
    }

    public Region getDeployedRegion() {
        return deployedRegion;
    }
    public void setDeployedRegion(Region deployedRegion0) {
        deployedRegion = deployedRegion0;
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
