/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import forge.card.spellability.SpellAbility;
import forge.game.player.Player;

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
    private List<Card> affectedCards = new ArrayList<Card>();
    private ArrayList<Player> affectedPlayers = new ArrayList<Player>();
    private int xValue = 0;
    private int yValue = 0;
    private long timestamp = -1;

    private String chosenType;
    private HashMap<String, String> mapParams = new HashMap<String, String>();

    // for P/T
    private final HashMap<Card, String> originalPT = new HashMap<Card, String>();

    // for types
    private boolean overwriteTypes = false;
    private boolean keepSupertype = false;
    private boolean removeSubTypes = false;
    private final HashMap<Card, ArrayList<String>> types = new HashMap<Card, ArrayList<String>>();
    private final HashMap<Card, ArrayList<String>> originalTypes = new HashMap<Card, ArrayList<String>>();

    // keywords
    private boolean overwriteKeywords = false;
    private final HashMap<Card, ArrayList<String>> originalKeywords = new HashMap<Card, ArrayList<String>>();

    // for abilities
    private boolean overwriteAbilities = false;
    private final HashMap<Card, ArrayList<SpellAbility>> originalAbilities = new HashMap<Card, ArrayList<SpellAbility>>();

    // for colors
    private String colorDesc = "";
    private boolean overwriteColors = false;
    private final HashMap<Card, Long> timestamps = new HashMap<Card, Long>();

    /**
     * setTimestamp TODO Write javadoc for this method.
     * 
     * @param t
     *            a long
     */
    public final void setTimestamp(final long t) {
        this.timestamp = t;
    }

    /**
     * getTimestamp. TODO Write javadoc for this method.
     * 
     * @return a long
     */
    public final long getTimestamp() {
        return this.timestamp;
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
        return this.overwriteAbilities;
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
        if (!this.originalAbilities.containsKey(c)) {
            final ArrayList<SpellAbility> list = new ArrayList<SpellAbility>();
            list.add(sa);
            this.originalAbilities.put(c, list);
        } else {
            this.originalAbilities.get(c).add(sa);
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
        final ArrayList<SpellAbility> list = new ArrayList<SpellAbility>(s);
        if (!this.originalAbilities.containsKey(c)) {
            this.originalAbilities.put(c, list);
        } else {
            this.originalAbilities.remove(c);
            this.originalAbilities.put(c, list);
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
        final ArrayList<SpellAbility> returnList = new ArrayList<SpellAbility>();
        if (this.originalAbilities.containsKey(c)) {
            returnList.addAll(this.originalAbilities.get(c));
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
        if (this.originalAbilities.containsKey(c)) {
            this.originalAbilities.get(c).clear();
        }
    }

    /**
     * <p>
     * clearAllOriginalAbilities.
     * </p>
     */
    public final void clearAllOriginalAbilities() {
        this.originalAbilities.clear();
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
        return this.overwriteKeywords;
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
        if (!this.originalKeywords.containsKey(c)) {
            final ArrayList<String> list = new ArrayList<String>();
            list.add(s);
            this.originalKeywords.put(c, list);
        } else {
            this.originalKeywords.get(c).add(s);
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
        final ArrayList<String> list = new ArrayList<String>(s);
        if (!this.originalKeywords.containsKey(c)) {
            this.originalKeywords.put(c, list);
        } else {
            this.originalKeywords.remove(c);
            this.originalKeywords.put(c, list);
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
        final ArrayList<String> returnList = new ArrayList<String>();
        if (this.originalKeywords.containsKey(c)) {
            returnList.addAll(this.originalKeywords.get(c));
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
        if (this.originalKeywords.containsKey(c)) {
            this.originalKeywords.get(c).clear();
        }
    }

    /**
     * <p>
     * clearAllOriginalKeywords.
     * </p>
     */
    public final void clearAllOriginalKeywords() {
        this.originalKeywords.clear();
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
        final String pt = power + "/" + toughness;
        if (!this.originalPT.containsKey(c)) {
            this.originalPT.put(c, pt);
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
        if (this.originalPT.containsKey(c)) {
            power = Integer.parseInt(this.originalPT.get(c).split("/")[0]);
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
        if (this.originalPT.containsKey(c)) {
            tough = Integer.parseInt(this.originalPT.get(c).split("/")[1]);
        }
        return tough;
    }

    /**
     * <p>
     * clearAllOriginalPTs.
     * </p>
     */
    public final void clearAllOriginalPTs() {
        this.originalPT.clear();
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
        return this.overwriteTypes;
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
        return this.keepSupertype;
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
        return this.removeSubTypes;
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
        if (!this.originalTypes.containsKey(c)) {
            final ArrayList<String> list = new ArrayList<String>();
            list.add(s);
            this.originalTypes.put(c, list);
        } else {
            this.originalTypes.get(c).add(s);
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
        final ArrayList<String> list = new ArrayList<String>(s);
        if (!this.originalTypes.containsKey(c)) {
            this.originalTypes.put(c, list);
        } else {
            this.originalTypes.remove(c);
            this.originalTypes.put(c, list);
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
        final ArrayList<String> returnList = new ArrayList<String>();
        if (this.originalTypes.containsKey(c)) {
            returnList.addAll(this.originalTypes.get(c));
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
        if (this.originalTypes.containsKey(c)) {
            this.originalTypes.get(c).clear();
        }
    }

    /**
     * <p>
     * clearAllOriginalTypes.
     * </p>
     */
    public final void clearAllOriginalTypes() {
        this.originalTypes.clear();
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
        if (!this.types.containsKey(c)) {
            final ArrayList<String> list = new ArrayList<String>();
            list.add(s);
            this.types.put(c, list);
        } else {
            this.types.get(c).add(s);
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
        final ArrayList<String> returnList = new ArrayList<String>();
        if (this.types.containsKey(c)) {
            returnList.addAll(this.types.get(c));
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
        if (this.types.containsKey(c)) {
            this.types.get(c).remove(type);
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
        if (this.types.containsKey(c)) {
            this.types.get(c).clear();
        }
    }

    /**
     * <p>
     * clearAllTypes.
     * </p>
     */
    public final void clearAllTypes() {
        this.types.clear();
    }

    /**
     * <p>
     * Getter for the field <code>colorDesc</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getColorDesc() {
        return this.colorDesc;
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
        return this.overwriteColors;
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
        return this.timestamps;
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
        final Long l = this.timestamps.get(c);
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
        this.timestamps.put(c, Long.valueOf(timestamp));
    }

    /**
     * <p>
     * clearTimestamps.
     * </p>
     */
    public final void clearTimestamps() {
        this.timestamps.clear();
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
        this.source = card;
    }

    /**
     * <p>
     * Getter for the field <code>source</code>.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public final Card getSource() {
        return this.source;
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
        this.keywordNumber = i;
    }

    /**
     * <p>
     * Getter for the field <code>keywordNumber</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getKeywordNumber() {
        return this.keywordNumber;
    }

    /**
     * <p>
     * Getter for the field <code>affectedCards</code>.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    public final List<Card> getAffectedCards() {
        return this.affectedCards;
    }

    /**
     * <p>
     * Setter for the field <code>affectedCards</code>.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    public final void setAffectedCards(final List<Card> list) {
        this.affectedCards = list;
    }

    /**
     * Gets the affected players.
     * 
     * @return the affected players
     */
    public final ArrayList<Player> getAffectedPlayers() {
        return this.affectedPlayers;
    }

    /**
     * Sets the affected players.
     * 
     * @param list
     *            the new affected players
     */
    public final void setAffectedPlayers(final ArrayList<Player> list) {
        this.affectedPlayers = list;
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
        this.xValue = x;
    }

    /**
     * <p>
     * Getter for the field <code>xValue</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getXValue() {
        return this.xValue;
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
        this.yValue = y;
    }

    /**
     * <p>
     * Getter for the field <code>yValue</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getYValue() {
        return this.yValue;
    }

    /**
     * setParams. TODO Write javadoc for this method.
     * 
     * @param params
     *            a HashMap
     */
    public final void setParams(final HashMap<String, String> params) {
        this.mapParams = params;
    }

    /**
     * Gets the params.
     * 
     * @return the params
     */
    public final HashMap<String, String> getParams() {
        return this.mapParams;
    }

    /**
     * Sets the chosen type.
     * 
     * @param type
     *            the new chosen type
     */
    public final void setChosenType(final String type) {
        this.chosenType = type;
    }

    /**
     * getChosenType. TODO Write javadoc for this method.
     * 
     * @return the chosen type
     */
    public final String getChosenType() {
        return this.chosenType;
    }

} // end class StaticEffect
