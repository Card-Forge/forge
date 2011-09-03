package forge.quest.data;

import javax.swing.ImageIcon;
import forge.deck.Deck;
import forge.gui.GuiUtils;

/**
 * <p>DeckSingleBattle</p>
 * MODEL - Assembles and stores information from a battle deck.
 *
 * @author Forge
 * @version $Id$
 */
public class DeckSingleBattle {
    private String      deckName;   
    private String      displayName;
    private String      diff;
    private String      desc;
    private String      iconFilename;
    private ImageIcon   icon;
    private Deck        deckObj;
    
    /**
     * <p>Constructor for DeckSingleBattle.</p>
     *
     * @param {@link java.lang.String} storing name of AI deck for this battle
     */
    public DeckSingleBattle(Deck d) {
        // Get deck object and properties for this opponent.
        this.deckObj      = d;
        this.deckName     = d.getName();
        this.displayName  = deckObj.getMetadata("DisplayName");
        this.diff         = deckObj.getMetadata("Difficulty");
        this.desc         = deckObj.getMetadata("Description");
        this.iconFilename = deckObj.getMetadata("Icon");
             
        // Default icon
        this.icon  = GuiUtils.getIconFromFile(displayName + ".jpg");
             
        // If non-default icon defined, use it. Any filetype accepted.      
        if(!iconFilename.equals("")) {
            this.icon = GuiUtils.getIconFromFile(iconFilename);
        }
    }
    
    /**
     * <p>getDifficulty()</p>
     * Retrieve rated difficulty of this battle deck.
     * 
     * @return {@link java.lang.String} 
     */
    public String getDifficulty() {
        return this.diff;
    }
    
    /**
     * <p>getDescription()</p>
     * Retrieve description of this battle deck.
     * 
     * @return {@link java.lang.String} 
     */
    public String getDescription() {
        return this.desc;
    }
    
    /**
     * <p>getDisplayName()</p>
     * Retrieve display name of this battle deck.
     * 
     * @return {@link java.lang.String} 
     */
    public String getDisplayName() {
        return this.displayName;
    }
    
    /**
     * <p>getDeckName()</p>
     * Retrieve file name of this battle deck.
     * 
     * @return {@link java.lang.String} 
     */
    public String getDeckName() {
        return this.deckName;
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
     * <p>getDeck()</p>
     * Retrieve this battle deck.
     * 
     * @return {@link java.lang.String} 
     */
    public Deck getDeck() {
        return this.deckObj;
    }
    
    /**
     * <p>getIcon()</p>
     * Retrieve the icon used with this battle deck.
     * 
     * @return {@link java.lang.String} 
     */
    public ImageIcon getIcon() {
        return this.icon;
    }

}
