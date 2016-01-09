package forge.planarconquest;

import forge.deck.Deck;
import forge.deck.generation.DeckGenPool;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.planarconquest.ConquestPlane.Region;
import forge.util.XmlReader;
import forge.util.XmlWriter;
import forge.util.XmlWriter.IXmlWritable;

public class ConquestCommander implements InventoryItem, IXmlWritable {
    private final PaperCard card;
    private final ConquestRecord record;
    private final ConquestPlane originPlane;
    private final String originRegionName;

    private Deck deck;

    public ConquestCommander(PaperCard card0) {
        this(card0, new Deck(card0.getName()), null);
    }
    public ConquestCommander(PaperCard card0, DeckGenPool cardPool0, boolean forAi) {
        this(card0, ConquestUtil.generateDeck(card0, cardPool0, forAi), null);
    }
    private ConquestCommander(PaperCard card0, Deck deck0, ConquestRecord record0) {
        card = card0;
        deck = deck0;
        if (record0 == null) {
            record0 = new ConquestRecord();
        }
        record = record0;

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

    public ConquestCommander(XmlReader xml) {
        this(xml.read("card", FModel.getMagicDb().getCommonCards()), null, xml.read("record", ConquestRecord.class));
    }
    @Override
    public void saveToXml(XmlWriter xml) {
        xml.write("card", card);
        xml.write("record", record);
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
        if (deck == null) { //if deck not yet initialized, attempt to load deck file
            deck = FModel.getConquest().getDecks().get(card.getName());
            if (deck == null) {
                deck = new Deck(card.getName());
            }
        }
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
