package forge.quest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import forge.Singletons;
import forge.gui.GuiChoose;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.item.ItemPool;

/** 
 * Resolves a card chooser InventoryItem into a CardPrinted.
 * The initial version includes "duplicate", other type may be added later.
 *
 */
public class QuestRewardCardChooser implements InventoryItem  {

    private String type;

    /** The constructor.
     * The parameter indicates the more specific type.
     * @param setType String, the type of the choosable card.
     */
    public QuestRewardCardChooser(final String setType) {
        type = setType;
    }

    /**
     * The name (this should not be displayed anywhere).
     * 
     * @return the name
     */
    @Override
    public String getName() {
        return "a chosen card";
    }


    /**
     * A QuestRewardCardChooser ought to always be resolved to an actual card, hence no images.
     * 
     * @return an empty string
     */
    @Override
    public String getImageFilename() {
        return "";
    }

    /**
     * The item type.
     * 
     * @return item type
     */
    @Override
    public String getItemType() {
        return type;
    }

    /**
     * Produces a CardPrinted of the player's choice.
     * 
     * @return a CardPrinted or null if could not choose one.
     */
    public final CardPrinted toCardPrinted() {
        if ("duplicate".equals(type)) {
            final ItemPool<CardPrinted> playerCards = Singletons.getModel().getQuest().getAssets().getCardPool();
            if (!playerCards.isEmpty()) { // Maybe a redundant check since it's hard to win a duel without any cards...

                List<CardPrinted> cardChoices = new ArrayList<CardPrinted>();
                for (final Map.Entry<CardPrinted, Integer> card : playerCards) {
                    cardChoices.add(card.getKey());
                }
                Collections.sort(cardChoices);

                final CardPrinted duplicate = GuiChoose.one("You have won a duplicate card!", cardChoices);
                return duplicate;
            }

        } else {
            throw new RuntimeException("Unknown QuestRewardCardType: " + type);
        }
        return null;
    }

}
