package forge.item;

import java.io.File;
import java.util.List;
import java.util.Map;

import forge.SetUtils;
import forge.deck.Deck;
import forge.deck.DeckIO;
import forge.quest.SellRules;
import forge.util.FileUtil;
import forge.util.SectionUtil;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class PreconDeck implements InventoryItemFromSet {

    private final Deck deck;
    private final String imageFilename;
    private final String set;
    private final SellRules recommendedDeals;

    @Override
    public String getName() {
        return deck.getName();
    }


    @Override
    public String getImageFilename() {
        return "precons/" + this.imageFilename;
    }


    @Override
    public String getType() {
        return "Prebuilt Deck";
    }

    public PreconDeck(final File f) {
        List<String> deckLines = FileUtil.readFile(f);
        Map<String, List<String>> sections = SectionUtil.parseSections(deckLines);
        deck = DeckIO.readDeck(deckLines);

        String filenameProxy = null;
        String setProxy = "n/a";
        List<String> metadata = sections.get("metadata");
        if (null != metadata && !metadata.isEmpty()) {
            for (String s : metadata) {
                String[] kv = s.split("=");
                if ("Image".equalsIgnoreCase(kv[0])) {
                    filenameProxy = kv[1];
                }
                if ("set".equalsIgnoreCase(kv[0]) && SetUtils.getSetByCode(kv[1].toUpperCase()) != null) {
                    setProxy = kv[1];
                }
            }
        }
        imageFilename = filenameProxy;
        set = setProxy;
        recommendedDeals = new SellRules(sections.get("shop"));
    }


    public final Deck getDeck() {
        return deck;
    }


    public final SellRules getRecommendedDeals() {
        return recommendedDeals;
    }

    @Override
    public String getSet() {
        return set;
    }
}
