package forge.quest.data;

import java.util.ArrayList;
import javax.swing.ImageIcon;

import com.google.code.jyield.Generator;
import com.google.code.jyield.YieldUtils;

import forge.AllZone;
import forge.Card;
import forge.Constant;
import forge.deck.Deck;
import forge.gui.GuiUtils;

/**
 * <p>DeckSingleQuest</p>
 * MODEL - Assembles and stores information from a quest deck.
 *
 * @author Forge
 * @version $Id$
 */
public class DeckSingleQuest { 
    private String      deckName;   
    private String      displayName;
    private String      diff;
    private String      desc;
    private String      iconFilename;
    private String      cardReward;
    
    private int         creditsReward;
    private int         numberWinsRequired;
    private int         AILife;
    private int         id; 
    
    private boolean     repeatable;
    private ImageIcon   icon;
    private Deck        deckObj;

    /**
     * <p>Constructor for DeckSingleQuest</p>
     *
     * @param {@link java.lang.String} storing name of AI deck for this quest
     */
    public DeckSingleQuest(Deck d) {
        // Get deck object and properties for this opponent.
        this.deckObj            = d;
        this.deckName           = d.getName();
        this.id                 = Integer.parseInt(deckObj.getMetadata("ID"));
        this.displayName        = deckObj.getMetadata("DisplayName");
        this.diff               = deckObj.getMetadata("Difficulty");
        this.desc               = deckObj.getMetadata("Description");
        this.iconFilename       = deckObj.getMetadata("Icon");
        this.cardReward         = deckObj.getMetadata("CardReward");
        this.creditsReward      = Integer.parseInt(deckObj.getMetadata("CreditsReward"));
        this.numberWinsRequired = Integer.parseInt(deckObj.getMetadata("NumberWinsRequired"));
        this.AILife             = Integer.parseInt(deckObj.getMetadata("AILife"));
             
        // Default icon
        this.icon  = GuiUtils.getIconFromFile(displayName + ".jpg");
        
        // If non-default icon defined, use it. Any filetype accepted.      
        if(!iconFilename.equals("")) {
            this.icon = GuiUtils.getIconFromFile(iconFilename);
        }
        
        // Repeatability test
        if(deckObj.getMetadata("Repeatable").equals("true")) {
            this.repeatable = true;
        }
        else {
            this.repeatable = false;
        }
        
    }

    /**
     * <p>getID()</p>
     * Retrieve ID number of this quest deck for recordkeeping.
     * 
     * @return {@link java.lang.int} 
     */
    public int getID() {
        return this.id;
    }
    
    /**
     * <p>getDifficulty()</p>
     * Retrieve rated difficulty of this quest deck.
     * 
     * @return {@link java.lang.String} 
     */
    public String getDifficulty() {
        return this.diff;
    }
    
    /**
     * <p>getDescription()</p>
     * Retrieve description of this quest deck.
     * 
     * @return {@link java.lang.String} 
     */
    public String getDescription() {
        return this.desc;
    }
    
    /**
     * <p>getDisplayName()</p>
     * Retrieve display name of this quest deck.
     * 
     * @return {@link java.lang.String} 
     */
    public String getDisplayName() {
        return this.displayName;
    }
    
    /**
     * <p>getDeckName()</p>
     * Retrieve file name of this quest deck.
     * 
     * @return {@link java.lang.String} 
     */
    public String getDeckName() {
        return this.deckName;
    }
    
    /**
     * <p>getCardReward()</p>
     * Retrieve cards rewarded after a win with this quest deck.
     * 
     * @return {@link java.lang.String} 
     */
    private String getCardReward() {
        return this.cardReward;
    }
    
    /**
     * <p>getCardRewardList()</p>
     * Retrieve cards rewarded after a win with this quest deck.
     * 
     * @return String[]
     */
    public ArrayList<String> getCardRewardList() {
        ArrayList<String> cardRewardList = new ArrayList<String>(); 
        
        String[] details = this.getCardReward().split(" ");
        
        // Set quantity, color and rarity from file meta.
        String cardscolor;
        Constant.Rarity rarity;
        int quantity = Integer.parseInt(details[0]);
        
        // Color
        if(details[1].toLowerCase().equals("random")) {
            cardscolor = null;
        }
        else if(details[1].toLowerCase().equals("blue")) {
            cardscolor = Constant.Color.Blue;
        }
        else if(details[1].toLowerCase().equals("black")) {
            cardscolor = Constant.Color.Black;
        }
        else if(details[1].toLowerCase().equals("green")) {
            cardscolor = Constant.Color.Green;
        }
        else if(details[1].toLowerCase().equals("red")) {
            cardscolor = Constant.Color.Red;
        }
        else if(details[1].toLowerCase().equals("white")) {
            cardscolor = Constant.Color.White;
        }
        else if(details[1].toLowerCase().equals("multi-color") ||
                details[1].toLowerCase().equals("multi-colored")) {
            cardscolor = "Multicolor";
        }
        else if(details[1].toLowerCase().equals("colorless")) {
            cardscolor = Constant.Color.Colorless;
        }
        else {
            cardscolor = null;
            System.err.println("DeckSingleQuest > getCardRewardList() reports "+
                    "a badly formed card reward for quest "+
                    this.getID()+".\n The color "+details[1]+" is not permitted.\n"+
                    "Random colors have been substituted.");
        }
        
        // Rarity
        if(details[2].toLowerCase().equals("rares")) {
            rarity = Constant.Rarity.Rare;
        }
        else {
            rarity = Constant.Rarity.Common;
        }
        
        // Generate deck list.
        QuestBoosterPack pack = new QuestBoosterPack();
        pack.generateCards(quantity, rarity, cardscolor);
        
        return cardRewardList;
    }
    
    /**
     * <p>getNumberWinsRequired()</p>
     * Retrieve number of wins required to play against this quest deck.
     * 
     * @return {@link java.lang.int} 
     */
    public int getNumberWinsRequired() {
        return this.numberWinsRequired;
    }
    
    /**
     * <p>getAILife()</p>
     * Retrieve starting value of life for the AI playing this quest deck.
     * 
     * @return {@link java.lang.int} 
     */
    public int getAILife() {
        return this.AILife;
    }
    
    /**
     * <p>getCreditsReward()</p>
     * Retrieve number of credits rewarded after a win against this quest deck.
     * 
     * @return {@link java.lang.int} 
     */
    public int getCreditsReward() {
        return this.creditsReward;
    }
    
    /**
     * <p>getDeck()</p>
     * Retrieve this quest deck.
     * 
     * @return {@link forge.deck.Deck} 
     */
    public Deck getDeck() {
        return this.deckObj;
    }
    
    /**
     * <p>getIconFilename()</p>
     * Retrieve file name of preferred icon
     * 
     * @return {@link java.lang.String} 
     */
    public String getIconFilename() {
        return this.iconFilename;
    }
    
    /**
     * <p>getIcon()</p>
     * Retrieve the icon used with this quest deck.
     * 
     * @return {@link javax.swing.ImageIcon} 
     */
    public ImageIcon getIcon() {
        return this.icon;
    }
    
    /**
     * <p>getRepeatable.</p>
     * Retrieve boolean indicating if this quest is repeatable.
     * 
     */
    public boolean getRepeatable() {
        return this.repeatable;
    }
 
}
