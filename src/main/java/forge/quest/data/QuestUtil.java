package forge.quest.data;

import forge.Card;
import forge.CardList;
import forge.Constant;
import forge.card.CardRarity;
import forge.card.BoosterUtils;
import forge.item.CardPrinted;
import forge.quest.gui.main.QuestQuest;

import java.util.List;

/**
 * <p>QuestUtil class.</p>
 * MODEL - Static utility methods to help with minor tasks around Quest.
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
    public static CardList getComputerStartingCards(final QuestData qd, QuestQuest qq) {
        CardList list = new CardList();
        
        if (qq != null) {
            list.addAll(qq.getAIExtraCards());
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
    public static CardList getHumanStartingCards(final QuestData qd, QuestQuest qq) {
        CardList list = getHumanStartingCards(qd);
        
        if (qq != null) {
            list.addAll(qq.getHumanExtraCards());
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
        String[] temp = s.split(" ");
        
        int qty = Integer.parseInt(temp[0]);
        // Determine rarity
        CardRarity rar = CardRarity.Uncommon;
        if(temp[2].equalsIgnoreCase("rare") || temp[2].equalsIgnoreCase("rares")) {
            rar = CardRarity.Rare;
        }
        
        // Determine color ("random" defaults to null color)
        String col = null;
        if(temp[1].equalsIgnoreCase("black")) {
            col = Constant.Color.Black;
        }
        else if(temp[1].equalsIgnoreCase("blue")) {
            col = Constant.Color.Blue;
        }
        else if(temp[1].equalsIgnoreCase("colorless")) {
            col = Constant.Color.Colorless;
        }
        else if(temp[1].equalsIgnoreCase("green")) {
            col = Constant.Color.Green;
        }
        else if(temp[1].equalsIgnoreCase("multicolor")) {
            col = "Multicolor"; // Note: No constant color for this??
        }
        else if(temp[1].equalsIgnoreCase("red")) {
            col = Constant.Color.Red;
        }
        else if(temp[1].equalsIgnoreCase("white")) {
            col = Constant.Color.White;
        }
        
        System.out.println(rar+" "+col+" "+qty);
        return BoosterUtils.generateCards(qty, rar, col);
    }

} //QuestUtil
