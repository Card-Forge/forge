package forge.gamemodes.planarconquest;

import com.google.common.base.Predicate;

import forge.card.CardRules;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.deck.generation.DeckGenPool;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.model.FModel;
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
        this(card0, null, null);
    }
    public ConquestCommander(PaperCard card0, ConquestPlane startingPlane) {
        this(card0, ConquestUtil.generateDeck(card0, new DeckGenPool(startingPlane.getCardPool().getAllCards(new Predicate<PaperCard>() {
            @Override
            public boolean apply(PaperCard pc) {
                CardRules rules = pc.getRules();
                return !rules.canBeCommander() && !rules.getType().isPlaneswalker(); //prevent including additional commanders or planeswalkers in starting deck
            }
        })), false), null);
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
        for (ConquestPlane plane : FModel.getPlanes()) {
            if (plane.getCommanders().contains(card)) {
                originPlane0 = plane;
                for (ConquestRegion region : plane.getRegions()) {
                    if (region.getCardPool().contains(card)) {
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
        int idx = name.indexOf(',');
        if (idx != -1) { //trim everything after the comma
            name = name.substring(0, idx);
        }
        return name;
    }

    public PaperCard getCard() {
        return card;
    }

    public Deck getDeck() {
        if (deck == null) { //if deck not yet initialized, attempt to load deck file
            reloadDeck();
        }
        return deck;
    }

    public void reloadDeck() {
        deck = FModel.getConquest().getDecks().get(card.getName());
        if (deck == null) {
            deck = new Deck(card.getName());
            deck.getOrCreate(DeckSection.Commander).add(card);
            FModel.getConquest().getDecks().add(deck);
        }
    }

    public ConquestRecord getRecord() {
        return record;
    }

    public String getOrigin() {
        if (originPlane == null) {
            return "";
        }
        return originPlane.getName().replace("_", " ") + " - " + originRegionName;
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
