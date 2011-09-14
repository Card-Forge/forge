package forge.quest.data;

import forge.Card;
import forge.CardList;
import forge.Constant;
import forge.Quest_Assignment;
import forge.card.CardRarity;
import forge.card.QuestBoosterPack;
import forge.item.CardPrinted;

import java.util.List;

/**
 * <p>QuestUtil class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class QuestUtil {

    /**
     * <p>getComputerStartingCards.</p>
     *
     * @param qd a {@link forge.quest.data.QuestData} object.
     * @return a {@link forge.CardList} object.
     */
    public static CardList getComputerStartingCards(final QuestData qd) {
        return new CardList();
    }

    /**
     * <p>getComputerStartingCards.</p>
     * Returns extra AI cards in play at start of quest.
     *
     * @param qd a {@link forge.quest.data.QuestData} object.
     * @param qa a {@link forge.Quest_Assignment} object.
     * @return a {@link forge.CardList} object.
     */
    public static CardList getComputerStartingCards(final QuestData qd, Quest_Assignment qa) {
        CardList list = new CardList();
        
        if (qa != null) {
            list.addAll(qa.getAIExtraCards());
        }
        return list;
    }

    /**
     * <p>getHumanStartingCards.</p>
     * Returns list of current plant/pet configuration only.
     *
     * @param qd a {@link forge.quest.data.QuestData} object.
     * @return a {@link forge.CardList} object.
     */
    public static CardList getHumanStartingCards(final QuestData qd) {
        CardList list = new CardList();

        if (qd.getPetManager().shouldPetBeUsed()) {
            list.add(qd.getPetManager().getSelectedPet().getPetCard());
        }

        if (qd.getPetManager().shouldPlantBeUsed()) {
            list.add(qd.getPetManager().getPlant().getPetCard());
        }

        return list;
    }

    /**
     * <p>getHumanStartingCards.</p>
     * Returns extra human cards, including current plant/pet configuration,
     * and cards in play at start of quest.
     *
     * @param qd a {@link forge.quest.data.QuestData} object.
     * @param qa a {@link forge.Quest_Assignment} object.
     * @return a {@link forge.CardList} object.
     */
    public static CardList getHumanStartingCards(final QuestData qd, Quest_Assignment qa) {
        CardList list = getHumanStartingCards(qd);
        
        if (qa != null) {
            list.addAll(qa.getHumanExtraCards());
        }

        return list;
    }
    
    /**
     * <p>createToken.</p>
     * Creates a card instance for token defined by property string.
     * 
     * @param s Properties string of token (TOKEN;W;1;1;sheep;type;type;type...)
     * @return token Card
     */
    public static Card createToken(String s) {
        String[] properties = s.split(";");;
        
        Card c = new Card();
        c.setToken(true);
        
        //c.setManaCost(properties[1]);
        c.addColor(properties[1]);
        c.setBaseAttack(Integer.parseInt(properties[2]));
        c.setBaseDefense(Integer.parseInt(properties[3]));
        c.setName(properties[4]);
        
        c.setImageName(
            properties[1]+" "+
            properties[2]+" "+
            properties[3]+" "+
            properties[4]
        );   

        int x = 5;
        while(x != properties.length) {
            c.addType(properties[x++]);
        }                                

        return c;
    }
    
    /**
     * <p>generateCardRewardList.</p>
     * Takes a reward list string, parses, and returns list of cards rewarded.
     * 
     * @param s Properties string of reward (97 multicolor rares)
     * @return CardList
     */
    public static List<CardPrinted> generateCardRewardList(String s) {
        QuestBoosterPack pack = new QuestBoosterPack();
        String[] temp = s.split(" ");
        
        int qty = Integer.parseInt(temp[0]);
        
        // Determine rarity
        CardRarity rar = CardRarity.Uncommon;
        if(temp[1].equals("rare") || temp[1].equals("rares")) {
            rar = CardRarity.Rare;
        }
        
        // Determine color ("random" defaults to null color)
        String col = null;
        if(temp[2].toLowerCase().equals("black")) {
            col = Constant.Color.Black;
        }
        else if(temp[2].toLowerCase().equals("blue")) {
            col = Constant.Color.Blue;
        }
        else if(temp[2].toLowerCase().equals("colorless")) {
            col = Constant.Color.Colorless;
        }
        else if(temp[2].toLowerCase().equals("green")) {
            col = Constant.Color.Green;
        }
        else if(temp[2].toLowerCase().equals("multicolor")) {
            col = "Multicolor"; // Note: No constant color for this??
        }
        else if(temp[2].toLowerCase().equals("red")) {
            col = Constant.Color.Red;
        }
        else if(temp[2].toLowerCase().equals("white")) {
            col = Constant.Color.White;
        }
        
        return pack.generateCards(qty, rar, col);
    }
    
    /**
     * <p>setupQuest.</p>
     * Assembled hard-coded quest options.
     * All non-deck-specific handling now takes place in quests.txt.
     * 
     * @deprecated 
     * @param qa
     */
    public static void setupQuest(Quest_Assignment qa) {
    
    }

} //QuestUtil
