package forge;

import java.util.ArrayList;
import java.util.HashMap;

import forge.card.spellability.SpellAbility;

/**
 * <p>
 * StaticEffect class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class StaticEffect {
    private Card source = new Card();
    private int keywordNumber = 0;
    private CardList affectedCards = new CardList();
    private ArrayList<Player> affectedPlayers = new ArrayList<Player>();
    private int xValue = 0;
    private int yValue = 0;
    private long timestamp = -1;

    private String chosenType;
    private HashMap<String, String> mapParams = new HashMap<String, String>();

    // for P/T
    private HashMap<Card, String> originalPT = new HashMap<Card, String>();

    // for types
    private boolean overwriteTypes = false;
    private boolean keepSupertype = false;
    private boolean removeSubTypes = false;
    private HashMap<Card, ArrayList<String>> types = new HashMap<Card, ArrayList<String>>();
    private HashMap<Card, ArrayList<String>> originalTypes = new HashMap<Card, ArrayList<String>>();

    // keywords
    private boolean overwriteKeywords = false;
    private HashMap<Card, ArrayList<String>> originalKeywords = new HashMap<Card, ArrayList<String>>();

    // for abilities
    private boolean overwriteAbilities = false;
    private HashMap<Card, ArrayList<SpellAbility>> originalAbilities = new HashMap<Card, ArrayList<SpellAbility>>();

    // for colors
    private String colorDesc = "";
    private boolean overwriteColors = false;
    private HashMap<Card, Long> timestamps = new HashMap<Card, Long>();

    /**
     * setTimestamp TODO Write javadoc for this method.
     * 
     * @param t
     *            a long
     */
    public final void setTimestamp(final long t) {
        timestamp = t;
    }

    /**
     * getTimestamp. TODO Write javadoc for this method.
     * 
     * @return a long
     */
    public final long getTimestamp() {
        return timestamp;
    }

    // overwrite SAs
    /**
     * <p>
     * isOverwriteAbilities.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isOverwriteAbilities() {
        return overwriteAbilities;
    }

    /**
     * <p>
     * Setter for the field <code>overwriteAbilities</code>.
     * </p>
     * 
     * @param overwriteAbilitiesIn
     *            a boolean.
     */
    public final void setOverwriteAbilities(final boolean overwriteAbilitiesIn) {
        this.overwriteAbilities = overwriteAbilitiesIn;
    }

    // original SAs
    /**
     * <p>
     * addOriginalAbilities.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public final void addOriginalAbilities(final Card c, final SpellAbility sa) {
        if (!originalAbilities.containsKey(c)) {
            ArrayList<SpellAbility> list = new ArrayList<SpellAbility>();
            list.add(sa);
            originalAbilities.put(c, list);
        } else {
            originalAbilities.get(c).add(sa);
        }
    }

    /**
     * <p>
     * addOriginalAbilities.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param s
     *            a {@link java.util.ArrayList} object.
     */
    public final void addOriginalAbilities(final Card c, final ArrayList<SpellAbility> s) {
        ArrayList<SpellAbility> list = new ArrayList<SpellAbility>(s);
        if (!originalAbilities.containsKey(c)) {
            originalAbilities.put(c, list);
        } else {
            originalAbilities.remove(c);
            originalAbilities.put(c, list);
        }
    }

    /**
     * <p>
     * Getter for the field <code>originalAbilities</code>.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<SpellAbility> getOriginalAbilities(final Card c) {
        ArrayList<SpellAbility> returnList = new ArrayList<SpellAbility>();
        if (originalAbilities.containsKey(c)) {
            returnList.addAll(originalAbilities.get(c));
        }
        return returnList;
    }

    /**
     * <p>
     * clearOriginalAbilities.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void clearOriginalAbilities(final Card c) {
        if (originalAbilities.containsKey(c)) {
            originalAbilities.get(c).clear();
        }
    }

    /**
     * <p>
     * clearAllOriginalAbilities.
     * </p>
     */
    public final void clearAllOriginalAbilities() {
        originalAbilities.clear();
    }

    // overwrite keywords
    /**
     * <p>
     * isOverwriteKeywords.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isOverwriteKeywords() {
        return overwriteKeywords;
    }

    /**
     * <p>
     * Setter for the field <code>overwriteKeywords</code>.
     * </p>
     * 
     * @param overwriteKeywordsIn
     *            a boolean.
     */
    public final void setOverwriteKeywords(final boolean overwriteKeywordsIn) {
        this.overwriteKeywords = overwriteKeywordsIn;
    }

    // original keywords
    /**
     * <p>
     * addOriginalKeyword.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param s
     *            a {@link java.lang.String} object.
     */
    public final void addOriginalKeyword(final Card c, final String s) {
        if (!originalKeywords.containsKey(c)) {
            ArrayList<String> list = new ArrayList<String>();
            list.add(s);
            originalKeywords.put(c, list);
        } else {
            originalKeywords.get(c).add(s);
        }
    }

    /**
     * <p>
     * addOriginalKeywords.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param s
     *            a {@link java.util.ArrayList} object.
     */
    public final void addOriginalKeywords(final Card c, final ArrayList<String> s) {
        ArrayList<String> list = new ArrayList<String>(s);
        if (!originalKeywords.containsKey(c)) {
            originalKeywords.put(c, list);
        } else {
            originalKeywords.remove(c);
            originalKeywords.put(c, list);
        }
    }

    /**
     * <p>
     * Getter for the field <code>originalKeywords</code>.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<String> getOriginalKeywords(final Card c) {
        ArrayList<String> returnList = new ArrayList<String>();
        if (originalKeywords.containsKey(c)) {
            returnList.addAll(originalKeywords.get(c));
        }
        return returnList;
    }

    /**
     * <p>
     * clearOriginalKeywords.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void clearOriginalKeywords(final Card c) {
        if (originalKeywords.containsKey(c)) {
            originalKeywords.get(c).clear();
        }
    }

    /**
     * <p>
     * clearAllOriginalKeywords.
     * </p>
     */
    public final void clearAllOriginalKeywords() {
        originalKeywords.clear();
    }

    // original power/toughness
    /**
     * <p>
     * addOriginalPT.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param power
     *            a int.
     * @param toughness
     *            a int.
     */
    public final void addOriginalPT(final Card c, final int power, final int toughness) {
        String pt = power + "/" + toughness;
        if (!originalPT.containsKey(c)) {
            originalPT.put(c, pt);
        }
    }

    /**
     * <p>
     * getOriginalPower.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a int.
     */
    public final int getOriginalPower(final Card c) {
        int power = -1;
        if (originalPT.containsKey(c)) {
            power = Integer.parseInt(originalPT.get(c).split("/")[0]);
        }
        return power;
    }

    /**
     * <p>
     * getOriginalToughness.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a int.
     */
    public final int getOriginalToughness(final Card c) {
        int tough = -1;
        if (originalPT.containsKey(c)) {
            tough = Integer.parseInt(originalPT.get(c).split("/")[1]);
        }
        return tough;
    }

    /**
     * <p>
     * clearAllOriginalPTs.
     * </p>
     */
    public final void clearAllOriginalPTs() {
        originalPT.clear();
    }

    // should we overwrite types?
    /**
     * <p>
     * isOverwriteTypes.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isOverwriteTypes() {
        return overwriteTypes;
    }

    /**
     * <p>
     * Setter for the field <code>overwriteTypes</code>.
     * </p>
     * 
     * @param overwriteTypesIn
     *            a boolean.
     */
    public final void setOverwriteTypes(final boolean overwriteTypesIn) {
        this.overwriteTypes = overwriteTypesIn;
    }

    /**
     * <p>
     * isKeepSupertype.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isKeepSupertype() {
        return keepSupertype;
    }

    /**
     * <p>
     * Setter for the field <code>keepSupertype</code>.
     * </p>
     * 
     * @param keepSupertypeIn
     *            a boolean.
     */
    public final void setKeepSupertype(final boolean keepSupertypeIn) {
        this.keepSupertype = keepSupertypeIn;
    }

    // should we overwrite land types?
    /**
     * <p>
     * isRemoveSubTypes.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isRemoveSubTypes() {
        return removeSubTypes;
    }

    /**
     * <p>
     * Setter for the field <code>removeSubTypes</code>.
     * </p>
     * 
     * @param removeSubTypesIn
     *            a boolean.
     */
    public final void setRemoveSubTypes(final boolean removeSubTypesIn) {
        this.removeSubTypes = removeSubTypesIn;
    }

    // original types
    /**
     * <p>
     * addOriginalType.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param s
     *            a {@link java.lang.String} object.
     */
    public final void addOriginalType(final Card c, final String s) {
        if (!originalTypes.containsKey(c)) {
            ArrayList<String> list = new ArrayList<String>();
            list.add(s);
            originalTypes.put(c, list);
        } else {
            originalTypes.get(c).add(s);
        }
    }

    /**
     * <p>
     * addOriginalTypes.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param s
     *            a {@link java.util.ArrayList} object.
     */
    public final void addOriginalTypes(final Card c, final ArrayList<String> s) {
        ArrayList<String> list = new ArrayList<String>(s);
        if (!originalTypes.containsKey(c)) {
            originalTypes.put(c, list);
        } else {
            originalTypes.remove(c);
            originalTypes.put(c, list);
        }
    }

    /**
     * <p>
     * Getter for the field <code>originalTypes</code>.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<String> getOriginalTypes(final Card c) {
        ArrayList<String> returnList = new ArrayList<String>();
        if (originalTypes.containsKey(c)) {
            returnList.addAll(originalTypes.get(c));
        }
        return returnList;
    }

    /**
     * <p>
     * clearOriginalTypes.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void clearOriginalTypes(final Card c) {
        if (originalTypes.containsKey(c)) {
            originalTypes.get(c).clear();
        }
    }

    /**
     * <p>
     * clearAllOriginalTypes.
     * </p>
     */
    public final void clearAllOriginalTypes() {
        originalTypes.clear();
    }

    // statically assigned types
    /**
     * <p>
     * addType.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param s
     *            a {@link java.lang.String} object.
     */
    public final void addType(final Card c, final String s) {
        if (!types.containsKey(c)) {
            ArrayList<String> list = new ArrayList<String>();
            list.add(s);
            types.put(c, list);
        } else {
            types.get(c).add(s);
        }
    }

    /**
     * <p>
     * Getter for the field <code>types</code>.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<String> getTypes(final Card c) {
        ArrayList<String> returnList = new ArrayList<String>();
        if (types.containsKey(c)) {
            returnList.addAll(types.get(c));
        }
        return returnList;
    }

    /**
     * <p>
     * removeType.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param type
     *            a {@link java.lang.String} object.
     */
    public final void removeType(final Card c, final String type) {
        if (types.containsKey(c)) {
            types.get(c).remove(type);
        }
    }

    /**
     * <p>
     * clearTypes.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void clearTypes(final Card c) {
        if (types.containsKey(c)) {
            types.get(c).clear();
        }
    }

    /**
     * <p>
     * clearAllTypes.
     * </p>
     */
    public final void clearAllTypes() {
        types.clear();
    }

    /**
     * <p>
     * Getter for the field <code>colorDesc</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getColorDesc() {
        return colorDesc;
    }

    /**
     * <p>
     * Setter for the field <code>colorDesc</code>.
     * </p>
     * 
     * @param colorDesc
     *            a {@link java.lang.String} object.
     */
    public final void setColorDesc(final String colorDesc) {
        this.colorDesc = colorDesc;
    }

    // overwrite color
    /**
     * <p>
     * isOverwriteColors.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isOverwriteColors() {
        return overwriteColors;
    }

    /**
     * <p>
     * Setter for the field <code>overwriteColors</code>.
     * </p>
     * 
     * @param overwriteColors
     *            a boolean.
     */
    public final void setOverwriteColors(final boolean overwriteColors) {
        this.overwriteColors = overwriteColors;
    }

    /**
     * <p>
     * Getter for the field <code>timestamps</code>.
     * </p>
     * 
     * @return a {@link java.util.HashMap} object.
     */
    public final HashMap<Card, Long> getTimestamps() {
        return timestamps;
    }

    /**
     * <p>
     * getTimestamp.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a long.
     */
    public final long getTimestamp(final Card c) {
        long stamp = -1;
        Long l = timestamps.get(c);
        if (null != l) {
            stamp = l.longValue();
        }
        return stamp;
    }

    /**
     * <p>
     * addTimestamp.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param timestamp
     *            a long.
     */
    public final void addTimestamp(final Card c, final long timestamp) {
        timestamps.put(c, Long.valueOf(timestamp));
    }

    /**
     * <p>
     * clearTimestamps.
     * </p>
     */
    public final void clearTimestamps() {
        timestamps.clear();
    }

    /**
     * <p>
     * Setter for the field <code>source</code>.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     */
    public final void setSource(final Card card) {
        source = card;
    }

    /**
     * <p>
     * Getter for the field <code>source</code>.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public final Card getSource() {
        return source;
    }

    /**
     * <p>
     * Setter for the field <code>keywordNumber</code>.
     * </p>
     * 
     * @param i
     *            a int.
     */
    public final void setKeywordNumber(final int i) {
        keywordNumber = i;
    }

    /**
     * <p>
     * Getter for the field <code>keywordNumber</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getKeywordNumber() {
        return keywordNumber;
    }

    /**
     * <p>
     * Getter for the field <code>affectedCards</code>.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    public final CardList getAffectedCards() {
        return affectedCards;
    }

    /**
     * <p>
     * Setter for the field <code>affectedCards</code>.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    public final void setAffectedCards(final CardList list) {
        affectedCards = list;
    }

    /**
     * Gets the affected players.
     * 
     * @return the affected players
     */
    public final ArrayList<Player> getAffectedPlayers() {
        return affectedPlayers;
    }

    /**
     * Sets the affected players.
     * 
     * @param list
     *            the new affected players
     */
    public final void setAffectedPlayers(final ArrayList<Player> list) {
        affectedPlayers = list;
    }

    /**
     * <p>
     * Setter for the field <code>xValue</code>.
     * </p>
     * 
     * @param x
     *            a int.
     */
    public final void setXValue(final int x) {
        xValue = x;
    }

    /**
     * <p>
     * Getter for the field <code>xValue</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getXValue() {
        return xValue;
    }

    /**
     * <p>
     * Setter for the field <code>yValue</code>.
     * </p>
     * 
     * @param y
     *            a int.
     */
    public final void setYValue(final int y) {
        yValue = y;
    }

    /**
     * <p>
     * Getter for the field <code>yValue</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getYValue() {
        return yValue;
    }

    /**
     * setParams. TODO Write javadoc for this method.
     * 
     * @param params
     *            a HashMap
     */
    public final void setParams(final HashMap<String, String> params) {
        mapParams = params;
    }

    /**
     * Gets the params.
     * 
     * @return the params
     */
    public final HashMap<String, String> getParams() {
        return mapParams;
    }

    /**
     * Sets the chosen type.
     * 
     * @param type
     *            the new chosen type
     */
    public final void setChosenType(final String type) {
        chosenType = type;
    }

    /**
     * getChosenType. TODO Write javadoc for this method.
     * 
     * @return the chosen type
     */
    public final String getChosenType() {
        return chosenType;
    }

} // end class StaticEffect
