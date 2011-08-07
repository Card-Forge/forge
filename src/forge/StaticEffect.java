package forge;


import forge.card.spellability.SpellAbility;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * <p>StaticEffect class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class StaticEffect {
    private Card source = new Card();
    private int keywordNumber = 0;
    private CardList affectedCards = new CardList();
    private int xValue = 0;
    private int yValue = 0;
    private long timestamp = -1;
    
    private String chosenType;
    private HashMap<String, String> mapParams = new HashMap<String, String>();

    //for P/T
    private HashMap<Card, String> originalPT = new HashMap<Card, String>();

    //for types
    private boolean overwriteTypes = false;
    private boolean keepSupertype = false;
    private boolean removeSubTypes = false;
    private HashMap<Card, ArrayList<String>> types = new HashMap<Card, ArrayList<String>>();
    private HashMap<Card, ArrayList<String>> originalTypes = new HashMap<Card, ArrayList<String>>();

    //keywords
    private boolean overwriteKeywords = false;
    private HashMap<Card, ArrayList<String>> originalKeywords = new HashMap<Card, ArrayList<String>>();

    //for abilities
    private boolean overwriteAbilities = false;
    private HashMap<Card, ArrayList<SpellAbility>> originalAbilities = new HashMap<Card, ArrayList<SpellAbility>>();

    //for colors
    private String colorDesc = "";
    private boolean overwriteColors = false;
    private HashMap<Card, Long> timestamps = new HashMap<Card, Long>();
    
    public void setTimestamp(long t) {
    	timestamp = t;
    }
    
    public long getTimestamp() {
    	return timestamp;
    }


    //overwrite SAs
    /**
     * <p>isOverwriteAbilities.</p>
     *
     * @return a boolean.
     */
    public boolean isOverwriteAbilities() {
        return overwriteAbilities;
    }

    /**
     * <p>Setter for the field <code>overwriteAbilities</code>.</p>
     *
     * @param overwriteAbilities a boolean.
     */
    public void setOverwriteAbilities(boolean overwriteAbilities) {
        this.overwriteAbilities = overwriteAbilities;
    }

    //original SAs
    /**
     * <p>addOriginalAbilities.</p>
     *
     * @param c a {@link forge.Card} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    public void addOriginalAbilities(Card c, SpellAbility sa) {
        if (!originalAbilities.containsKey(c)) {
            ArrayList<SpellAbility> list = new ArrayList<SpellAbility>();
            list.add(sa);
            originalAbilities.put(c, list);
        } else originalAbilities.get(c).add(sa);
    }

    /**
     * <p>addOriginalAbilities.</p>
     *
     * @param c a {@link forge.Card} object.
     * @param s a {@link java.util.ArrayList} object.
     */
    public void addOriginalAbilities(Card c, ArrayList<SpellAbility> s) {
        ArrayList<SpellAbility> list = new ArrayList<SpellAbility>(s);
        if (!originalAbilities.containsKey(c)) {
            originalAbilities.put(c, list);
        } else {
            originalAbilities.remove(c);
            originalAbilities.put(c, list);
        }
    }

    /**
     * <p>Getter for the field <code>originalAbilities</code>.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public ArrayList<SpellAbility> getOriginalAbilities(Card c) {
        ArrayList<SpellAbility> returnList = new ArrayList<SpellAbility>();
        if (originalAbilities.containsKey(c)) {
            returnList.addAll(originalAbilities.get(c));
        }
        return returnList;
    }

    /**
     * <p>clearOriginalAbilities.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public void clearOriginalAbilities(Card c) {
        if (originalAbilities.containsKey(c)) {
            originalAbilities.get(c).clear();
        }
    }

    /**
     * <p>clearAllOriginalAbilities.</p>
     */
    public void clearAllOriginalAbilities() {
        originalAbilities.clear();
    }

    //overwrite keywords
    /**
     * <p>isOverwriteKeywords.</p>
     *
     * @return a boolean.
     */
    public boolean isOverwriteKeywords() {
        return overwriteKeywords;
    }

    /**
     * <p>Setter for the field <code>overwriteKeywords</code>.</p>
     *
     * @param overwriteKeywords a boolean.
     */
    public void setOverwriteKeywords(boolean overwriteKeywords) {
        this.overwriteKeywords = overwriteKeywords;
    }

    //original keywords
    /**
     * <p>addOriginalKeyword.</p>
     *
     * @param c a {@link forge.Card} object.
     * @param s a {@link java.lang.String} object.
     */
    public void addOriginalKeyword(Card c, String s) {
        if (!originalKeywords.containsKey(c)) {
            ArrayList<String> list = new ArrayList<String>();
            list.add(s);
            originalKeywords.put(c, list);
        } else originalKeywords.get(c).add(s);
    }

    /**
     * <p>addOriginalKeywords.</p>
     *
     * @param c a {@link forge.Card} object.
     * @param s a {@link java.util.ArrayList} object.
     */
    public void addOriginalKeywords(Card c, ArrayList<String> s) {
        ArrayList<String> list = new ArrayList<String>(s);
        if (!originalKeywords.containsKey(c)) {
            originalKeywords.put(c, list);
        } else {
            originalKeywords.remove(c);
            originalKeywords.put(c, list);
        }
    }

    /**
     * <p>Getter for the field <code>originalKeywords</code>.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public ArrayList<String> getOriginalKeywords(Card c) {
        ArrayList<String> returnList = new ArrayList<String>();
        if (originalKeywords.containsKey(c)) {
            returnList.addAll(originalKeywords.get(c));
        }
        return returnList;
    }

    /**
     * <p>clearOriginalKeywords.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public void clearOriginalKeywords(Card c) {
        if (originalKeywords.containsKey(c)) {
            originalKeywords.get(c).clear();
        }
    }

    /**
     * <p>clearAllOriginalKeywords.</p>
     */
    public void clearAllOriginalKeywords() {
        originalKeywords.clear();
    }

    //original power/toughness
    /**
     * <p>addOriginalPT.</p>
     *
     * @param c a {@link forge.Card} object.
     * @param power a int.
     * @param toughness a int.
     */
    public void addOriginalPT(Card c, int power, int toughness) {
        String pt = power + "/" + toughness;
        if (!originalPT.containsKey(c)) {
            originalPT.put(c, pt);
        }
    }

    /**
     * <p>getOriginalPower.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a int.
     */
    public int getOriginalPower(Card c) {
        int power = -1;
        if (originalPT.containsKey(c)) {
            power = Integer.parseInt(originalPT.get(c).split("/")[0]);
        }
        return power;
    }

    /**
     * <p>getOriginalToughness.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a int.
     */
    public int getOriginalToughness(Card c) {
        int tough = -1;
        if (originalPT.containsKey(c)) {
            tough = Integer.parseInt(originalPT.get(c).split("/")[1]);
        }
        return tough;
    }

    /**
     * <p>clearAllOriginalPTs.</p>
     */
    public void clearAllOriginalPTs() {
        originalPT.clear();
    }

    //should we overwrite types?
    /**
     * <p>isOverwriteTypes.</p>
     *
     * @return a boolean.
     */
    public boolean isOverwriteTypes() {
        return overwriteTypes;
    }

    /**
     * <p>Setter for the field <code>overwriteTypes</code>.</p>
     *
     * @param overwriteTypes a boolean.
     */
    public void setOverwriteTypes(boolean overwriteTypes) {
        this.overwriteTypes = overwriteTypes;
    }

    /**
     * <p>isKeepSupertype.</p>
     *
     * @return a boolean.
     */
    public boolean isKeepSupertype() {
        return keepSupertype;
    }

    /**
     * <p>Setter for the field <code>keepSupertype</code>.</p>
     *
     * @param keepSupertype a boolean.
     */
    public void setKeepSupertype(boolean keepSupertype) {
        this.keepSupertype = keepSupertype;
    }

    //should we overwrite land types?
    /**
     * <p>isRemoveSubTypes.</p>
     *
     * @return a boolean.
     */
    public boolean isRemoveSubTypes() {
        return removeSubTypes;
    }

    /**
     * <p>Setter for the field <code>removeSubTypes</code>.</p>
     *
     * @param removeSubTypes a boolean.
     */
    public void setRemoveSubTypes(boolean removeSubTypes) {
        this.removeSubTypes = removeSubTypes;
    }

    //original types
    /**
     * <p>addOriginalType.</p>
     *
     * @param c a {@link forge.Card} object.
     * @param s a {@link java.lang.String} object.
     */
    public void addOriginalType(Card c, String s) {
        if (!originalTypes.containsKey(c)) {
            ArrayList<String> list = new ArrayList<String>();
            list.add(s);
            originalTypes.put(c, list);
        } else originalTypes.get(c).add(s);
    }

    /**
     * <p>addOriginalTypes.</p>
     *
     * @param c a {@link forge.Card} object.
     * @param s a {@link java.util.ArrayList} object.
     */
    public void addOriginalTypes(Card c, ArrayList<String> s) {
        ArrayList<String> list = new ArrayList<String>(s);
        if (!originalTypes.containsKey(c)) {
            originalTypes.put(c, list);
        } else {
            originalTypes.remove(c);
            originalTypes.put(c, list);
        }
    }

    /**
     * <p>Getter for the field <code>originalTypes</code>.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public ArrayList<String> getOriginalTypes(Card c) {
        ArrayList<String> returnList = new ArrayList<String>();
        if (originalTypes.containsKey(c)) {
            returnList.addAll(originalTypes.get(c));
        }
        return returnList;
    }

    /**
     * <p>clearOriginalTypes.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public void clearOriginalTypes(Card c) {
        if (originalTypes.containsKey(c)) {
            originalTypes.get(c).clear();
        }
    }

    /**
     * <p>clearAllOriginalTypes.</p>
     */
    public void clearAllOriginalTypes() {
        originalTypes.clear();
    }

    //statically assigned types
    /**
     * <p>addType.</p>
     *
     * @param c a {@link forge.Card} object.
     * @param s a {@link java.lang.String} object.
     */
    public void addType(Card c, String s) {
        if (!types.containsKey(c)) {
            ArrayList<String> list = new ArrayList<String>();
            list.add(s);
            types.put(c, list);
        } else types.get(c).add(s);
    }

    /**
     * <p>Getter for the field <code>types</code>.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public ArrayList<String> getTypes(Card c) {
        ArrayList<String> returnList = new ArrayList<String>();
        if (types.containsKey(c)) {
            returnList.addAll(types.get(c));
        }
        return returnList;
    }

    /**
     * <p>removeType.</p>
     *
     * @param c a {@link forge.Card} object.
     * @param type a {@link java.lang.String} object.
     */
    public void removeType(Card c, String type) {
        if (types.containsKey(c)) {
            types.get(c).remove(type);
        }
    }

    /**
     * <p>clearTypes.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public void clearTypes(Card c) {
        if (types.containsKey(c)) {
            types.get(c).clear();
        }
    }

    /**
     * <p>clearAllTypes.</p>
     */
    public void clearAllTypes() {
        types.clear();
    }

    /**
     * <p>Getter for the field <code>colorDesc</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getColorDesc() {
        return colorDesc;
    }

    /**
     * <p>Setter for the field <code>colorDesc</code>.</p>
     *
     * @param colorDesc a {@link java.lang.String} object.
     */
    public void setColorDesc(String colorDesc) {
        this.colorDesc = colorDesc;
    }

    //overwrite color
    /**
     * <p>isOverwriteColors.</p>
     *
     * @return a boolean.
     */
    public boolean isOverwriteColors() {
        return overwriteColors;
    }

    /**
     * <p>Setter for the field <code>overwriteColors</code>.</p>
     *
     * @param overwriteColors a boolean.
     */
    public void setOverwriteColors(boolean overwriteColors) {
        this.overwriteColors = overwriteColors;
    }

    /**
     * <p>Getter for the field <code>timestamps</code>.</p>
     *
     * @return a {@link java.util.HashMap} object.
     */
    public HashMap<Card, Long> getTimestamps() {
        return timestamps;
    }

    /**
     * <p>getTimestamp.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a long.
     */
    public long getTimestamp(Card c) {
        long stamp = -1;
        Long l = timestamps.get(c);
        if (null != l) {
            stamp = l.longValue();
        }
        return stamp;
    }

    /**
     * <p>addTimestamp.</p>
     *
     * @param c a {@link forge.Card} object.
     * @param timestamp a long.
     */
    public void addTimestamp(Card c, long timestamp) {
        timestamps.put(c, Long.valueOf(timestamp));
    }

    /**
     * <p>clearTimestamps.</p>
     */
    public void clearTimestamps() {
        timestamps.clear();
    }

    /**
     * <p>Setter for the field <code>source</code>.</p>
     *
     * @param card a {@link forge.Card} object.
     */
    public void setSource(Card card) {
        source = card;
    }

    /**
     * <p>Getter for the field <code>source</code>.</p>
     *
     * @return a {@link forge.Card} object.
     */
    public Card getSource() {
        return source;
    }

    /**
     * <p>Setter for the field <code>keywordNumber</code>.</p>
     *
     * @param i a int.
     */
    public void setKeywordNumber(int i) {
        keywordNumber = i;
    }

    /**
     * <p>Getter for the field <code>keywordNumber</code>.</p>
     *
     * @return a int.
     */
    public int getKeywordNumber() {
        return keywordNumber;
    }

    /**
     * <p>Getter for the field <code>affectedCards</code>.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    public CardList getAffectedCards() {
        return affectedCards;
    }

    /**
     * <p>Setter for the field <code>affectedCards</code>.</p>
     *
     * @param list a {@link forge.CardList} object.
     */
    public void setAffectedCards(CardList list) {
        affectedCards = list;
    }

    /**
     * <p>Setter for the field <code>xValue</code>.</p>
     *
     * @param x a int.
     */
    public void setXValue(int x) {
        xValue = x;
    }

    /**
     * <p>Getter for the field <code>xValue</code>.</p>
     *
     * @return a int.
     */
    public int getXValue() {
        return xValue;
    }

    /**
     * <p>Setter for the field <code>yValue</code>.</p>
     *
     * @param y a int.
     */
    public void setYValue(int y) {
        yValue = y;
    }

    /**
     * <p>Getter for the field <code>yValue</code>.</p>
     *
     * @return a int.
     */
    public int getYValue() {
        return yValue;
    }
    
    public void setParams(HashMap<String, String> params) {
    	mapParams = params;
    }
    
    public HashMap<String, String> getParams() {
    	return mapParams;
    }
    
    public void setChosenType(String type) {
    	chosenType = type;
    }
    
    public String getChosenType() {
    	return chosenType;
    }
    

}//end class StaticEffect
