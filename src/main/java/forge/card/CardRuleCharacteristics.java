package forge.card;

import java.util.Map;
import java.util.TreeMap;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class CardRuleCharacteristics {
    private String cardName = null;
    private CardType cardType = null;
    private CardManaCost manaCost = CardManaCost.empty;
    private CardColor color = null;
    private String ptLine = null;
    private String[] cardRules = null;
    private Map<String, CardInSet> setsData = new TreeMap<String, CardInSet>();
    
    /**
     * @return the cardName
     */
    public String getCardName() {
        return cardName;
    }
    /**
     * @param cardName0 the cardName to set
     */
    public void setCardName(String cardName0) {
        this.cardName = cardName0; // TODO: Add 0 to parameter's name.
    }
    /**
     * @return the cardType
     */
    public CardType getCardType() {
        return cardType;
    }
    /**
     * @param cardType0 the cardType to set
     */
    public void setCardType(CardType cardType0) {
        this.cardType = cardType0; // TODO: Add 0 to parameter's name.
    }
    /**
     * @return the manaCost
     */
    public CardManaCost getManaCost() {
        return manaCost;
    }
    /**
     * @param manaCost0 the manaCost to set
     */
    public void setManaCost(CardManaCost manaCost0) {
        this.manaCost = manaCost0; // TODO: Add 0 to parameter's name.
        this.color = new CardColor(this.manaCost);
    }
    /**
     * @return the color
     */
    public CardColor getColor() {
        return color;
    }
    /**
     * @param color0 the color to set
     */
    public void setColor(CardColor color0) {
        this.color = color0; // TODO: Add 0 to parameter's name.
    }
    /**
     * @return the ptLine
     */
    public String getPtLine() {
        return ptLine;
    }
    /**
     * @param ptLine0 the ptLine to set
     */
    public void setPtLine(String ptLine0) {
        this.ptLine = ptLine0; // TODO: Add 0 to parameter's name.
    }
    /**
     * @return the cardRules
     */
    public String[] getCardRules() {
        return cardRules;
    }
    /**
     * @param cardRules0 the cardRules to set
     */
    public void setCardRules(String[] cardRules0) {
        this.cardRules = cardRules0; // TODO: Add 0 to parameter's name.
    }
    /**
     * @return the setsData
     */
    public Map<String, CardInSet> getSetsData() {
        return setsData;
    }
    /**
     * @param setsData0 the setsData to set
     */
    public void setSetsData(Map<String, CardInSet> setsData0) {
        this.setsData = setsData0; // TODO: Add 0 to parameter's name.
    }
}
