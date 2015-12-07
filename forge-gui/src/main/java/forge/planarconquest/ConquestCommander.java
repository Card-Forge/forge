package forge.planarconquest;

import forge.deck.Deck;
import forge.deck.generation.DeckGenPool;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.planarconquest.ConquestPlane.Region;

public class ConquestCommander implements InventoryItem {
    private final PaperCard card;
    private final Deck deck;
    private final ConquestRecord record;
    private final ConquestPlane originPlane;
    private final String originRegionName;

    public ConquestCommander(PaperCard card0) {
        this(card0, new Deck(card0.getName()));
    }
    public ConquestCommander(PaperCard card0, DeckGenPool cardPool0, boolean forAi) {
        this(card0, ConquestUtil.generateDeck(card0, cardPool0, forAi));
    }
    private ConquestCommander(PaperCard card0, Deck deck0) {
        card = card0;
        deck = deck0;
        record = new ConquestRecord();

        //determine origin of commander
        ConquestPlane originPlane0 = null;
        String originRegionName0 = null;
        for (ConquestPlane plane : ConquestPlane.values()) {
            if (plane.getCommanders().contains(card)) {
                originPlane0 = plane;
                for (Region region : plane.getRegions()) {
                    if (region.getCommanders().contains(card)) {
                        originRegionName0 = region.getName();
                        break;
                    }
                }
                break;
            }
        }
        originPlane = originPlane0;
        originRegionName = originRegionName0;
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

    public ConquestRecord getRecord() {
        return record;
    }

    public String getOrigin() {
        return originPlane.getName() + " - " + originRegionName;
    }

    public ConquestPlane getOriginPlane() {
        return originPlane;
    }

    @Override
    public String getItemType() {
        return "Commander";
    }

    @Override
    public String getImageKey(boolean altState) {
        return card.getImageKey(altState);
    }

    @Override
    public String toString() {
        return card.getName();
    }
}
