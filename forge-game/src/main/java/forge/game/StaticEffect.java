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
package forge.game;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardUtil;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.AbilityStatic;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * <p>
 * StaticEffect class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class StaticEffect {

    private final Card source;
    private int keywordNumber = 0;
    private CardCollectionView affectedCards = new CardCollection();
    private List<Player> affectedPlayers = Lists.newArrayList();
    private int xValue = 0;
    private int yValue = 0;
    private long timestamp = -1;
    private Map<Card, Integer> xValueMap = Maps.newTreeMap();

    private String chosenType;
    private Map<String, String> mapParams = Maps.newTreeMap();

    // for P/T
    private final Map<Card, String> originalPT = Maps.newTreeMap();

    // for types
    private boolean overwriteTypes = false;
    private boolean keepSupertype = false;
    private boolean removeSubTypes = false;
    private final Map<Card, List<String>> types = Maps.newTreeMap();
    private final Map<Card, List<String>> originalTypes = Maps.newTreeMap();

    // keywords
    private boolean overwriteKeywords = false;
    private final Map<Card, List<String>> originalKeywords = Maps.newTreeMap();

    // for abilities
    private boolean overwriteAbilities = false;
    private final Map<Card, List<SpellAbility>> originalAbilities = Maps.newTreeMap();

    // for colors
    private String colorDesc = "";
    private boolean overwriteColors = false;

    StaticEffect(final Card source) {
        this.source = source;
    }

    private StaticEffect makeMappedCopy(GameObjectMap map) {
        StaticEffect copy = new StaticEffect(map.map(this.source));
        copy.keywordNumber = this.keywordNumber;
        copy.affectedCards = map.mapCollection(this.affectedCards);
        copy.affectedPlayers  = map.mapList(this.affectedPlayers);
        copy.xValue = this.xValue;
        copy.yValue = this.yValue;
        copy.timestamp = this.timestamp;
        copy.xValueMap = this.xValueMap;
        copy.chosenType = this.chosenType;
        copy.mapParams = this.mapParams;
        map.fillKeyedMap(copy.originalPT, this.originalPT);
        copy.overwriteTypes = this.overwriteTypes;
        copy.keepSupertype = this.keepSupertype;
        copy.removeSubTypes = this.removeSubTypes;
        map.fillKeyedMap(this.types, this.types);
        map.fillKeyedMap(this.originalTypes, this.originalTypes);
        copy.overwriteKeywords = this.overwriteKeywords;
        map.fillKeyedMap(this.originalKeywords, this.originalKeywords);
        copy.overwriteAbilities = this.overwriteAbilities;
        map.fillKeyedMap(this.originalAbilities, this.originalAbilities);
        copy.colorDesc = this.colorDesc;
        copy.overwriteColors = this.overwriteColors;
        return copy;
    }

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
     *            a {@link forge.game.card.Card} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     */
    public final void addOriginalAbilities(final Card c, final SpellAbility sa) {
        if (!this.originalAbilities.containsKey(c)) {
            final List<SpellAbility> list = new ArrayList<SpellAbility>();
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
     *            a {@link forge.game.card.Card} object.
     * @param s
     *            a {@link java.util.List} object.
     */
    public final void addOriginalAbilities(final Card c, final List<SpellAbility> s) {
        final List<SpellAbility> list = new ArrayList<SpellAbility>(s);
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
     *            a {@link forge.game.card.Card} object.
     * @return a {@link java.util.List} object.
     */
    public final List<SpellAbility> getOriginalAbilities(final Card c) {
        final List<SpellAbility> returnList = new ArrayList<SpellAbility>();
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
     *            a {@link forge.game.card.Card} object.
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
     *            a {@link forge.game.card.Card} object.
     * @param s
     *            a {@link java.lang.String} object.
     */
    public final void addOriginalKeyword(final Card c, final String s) {
        if (!this.originalKeywords.containsKey(c)) {
            final List<String> list = new ArrayList<String>();
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
     *            a {@link forge.game.card.Card} object.
     * @param s
     *            a {@link List} object.
     */
    public final void addOriginalKeywords(final Card c, final List<String> s) {
        final List<String> list = new ArrayList<String>(s);
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
     *            a {@link forge.game.card.Card} object.
     * @return a {@link List} object.
     */
    public final List<String> getOriginalKeywords(final Card c) {
        final List<String> returnList = new ArrayList<String>();
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
     *            a {@link forge.game.card.Card} object.
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
     *            a {@link forge.game.card.Card} object.
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
     *            a {@link forge.game.card.Card} object.
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
     *            a {@link forge.game.card.Card} object.
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
     *            a {@link forge.game.card.Card} object.
     * @param s
     *            a {@link java.lang.String} object.
     */
    public final void addOriginalType(final Card c, final String s) {
        if (!this.originalTypes.containsKey(c)) {
            final List<String> list = new ArrayList<String>();
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
     *            a {@link forge.game.card.Card} object.
     * @param s
     *            a {@link java.util.ArrayList} object.
     */
    public final void addOriginalTypes(final Card c, final List<String> s) {
        final List<String> list = new ArrayList<String>(s);
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
     *            a {@link forge.game.card.Card} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public final List<String> getOriginalTypes(final Card c) {
        final List<String> returnList = new ArrayList<String>();
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
     *            a {@link forge.game.card.Card} object.
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
     *            a {@link forge.game.card.Card} object.
     * @param s
     *            a {@link java.lang.String} object.
     */
    public final void addType(final Card c, final String s) {
        if (!this.types.containsKey(c)) {
            final List<String> list = new ArrayList<String>();
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
     *            a {@link forge.game.card.Card} object.
     * @return a {@link java.util.List} object.
     */
    public final List<String> getTypes(final Card c) {
        final List<String> returnList = new ArrayList<String>();
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
     *            a {@link forge.game.card.Card} object.
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
     *            a {@link forge.game.card.Card} object.
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
     * Getter for the field <code>source</code>.
     * </p>
     * 
     * @return a {@link forge.game.card.Card} object.
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
    public final CardCollectionView getAffectedCards() {
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
    public final void setAffectedCards(final CardCollectionView list) {
        affectedCards = list;
    }

    /**
     * Gets the affected players.
     * 
     * @return the affected players
     */
    public final List<Player> getAffectedPlayers() {
        return this.affectedPlayers;
    }

    /**
     * Sets the affected players.
     * 
     * @param list
     *            the new affected players
     */
    public final void setAffectedPlayers(final List<Player> list) {
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
     * Store xValue relative to a specific card.
     * @param affectedCard the card affected
     * @param xValue the xValue
     */
    public final void addXMapValue(final Card affectedCard, final Integer xValue) {
        if (this.xValueMap.containsKey(affectedCard)) {
            if (!this.xValueMap.get(affectedCard).equals(xValue)) {
                this.xValueMap.remove(affectedCard);
            }
        }
        this.xValueMap.put(affectedCard, xValue);
    }

    /**
     * Get the xValue for specific card.
     * @param affectedCard the affected card
     * @return an int.
     */
    public int getXMapValue(Card affectedCard) {
        return this.xValueMap.get(affectedCard);
    }

    /**
     * setParams. TODO Write javadoc for this method.
     * 
     * @param params
     *            a HashMap
     */
    public final void setParams(final Map<String, String> params) {
        this.mapParams = params;
    }

    /**
     * Gets the params.
     * 
     * @return the params
     */
    public final Map<String, String> getParams() {
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

    /**
     * Undo everything that was changed by this effect.
     * 
     * @return a {@link CardCollectionView} of all affected cards.
     */
    final CardCollectionView remove() {
        final CardCollectionView affectedCards = getAffectedCards();
        final List<Player> affectedPlayers = getAffectedPlayers();
        final Player controller = getSource().getController();
        final Map<String, String> params = getParams();

        String changeColorWordsTo = null;

        int powerBonus = 0;
        String addP = "";
        int toughnessBonus = 0;
        String addT = "";
        int keywordMultiplier = 1;
        boolean setPT = false;
        String[] addHiddenKeywords = null;
        String addColors = null;
        boolean removeMayLookAt = false, removeMayPlay = false;

        if (params.containsKey("ChangeColorWordsTo")) {
            changeColorWordsTo = params.get("ChangeColorWordsTo");
        }

        if (params.containsKey("SetPower") || params.containsKey("SetToughness")) {
            setPT = true;
        }

        if (params.containsKey("AddPower")) {
            addP = params.get("AddPower");
            if (addP.matches("[0-9][0-9]?")) {
                powerBonus = Integer.valueOf(addP);
            } else if (addP.equals("AffectedX")) {
                // gets calculated at runtime
            } else {
                powerBonus = getXValue();
            }
        }

        if (params.containsKey("AddToughness")) {
            addT = params.get("AddToughness");
            if (addT.matches("[0-9][0-9]?")) {
                toughnessBonus = Integer.valueOf(addT);
            } else if (addT.equals("AffectedX")) {
                // gets calculated at runtime
            } else {
                toughnessBonus = getYValue();
            }
        }

        if (params.containsKey("KeywordMultiplier")) {
            String multiplier = params.get("KeywordMultiplier");
            if (multiplier.equals("X")) {
                keywordMultiplier = getXValue();
            } else {
                keywordMultiplier = Integer.valueOf(multiplier);
            }
        }

        if (params.containsKey("AddHiddenKeyword")) {
            addHiddenKeywords = params.get("AddHiddenKeyword").split(" & ");
        }

        if (params.containsKey("AddColor")) {
            final String colors = params.get("AddColor");
            if (colors.equals("ChosenColor")) {
                addColors = CardUtil.getShortColorsString(getSource().getChosenColors());
            } else {
                addColors = CardUtil.getShortColorsString(new ArrayList<String>(Arrays.asList(colors.split(" & "))));
            }
        }

        if (params.containsKey("SetColor")) {
            final String colors = params.get("SetColor");
            if (colors.equals("ChosenColor")) {
                addColors = CardUtil.getShortColorsString(getSource().getChosenColors());
            } else {
                addColors = CardUtil.getShortColorsString(new ArrayList<String>(Arrays.asList(colors.split(" & "))));
            }
        }

        if (params.containsKey("MayLookAt")) {
            removeMayLookAt = true;
        }
        if (params.containsKey("MayPlay")) {
            removeMayPlay = true;
        }

        if (params.containsKey("IgnoreEffectCost")) {
            for (final SpellAbility s : getSource().getSpellAbilities()) {
                if (s instanceof AbilityStatic && s.isTemporary()) {
                    getSource().removeSpellAbility(s);
                }
            }
        }

        // modify players
        for (final Player p : affectedPlayers) {
            p.setUnlimitedHandSize(false);
            p.setMaxHandSize(p.getStartingHandSize());
            p.removeChangedKeywords(getTimestamp());
        }

        // modify the affected card
        for (final Card affectedCard : affectedCards) {
            // Gain control
            if (params.containsKey("GainControl")) {
                affectedCard.removeTempController(getTimestamp());
            }

            // Revert changed color words
            if (changeColorWordsTo != null) {
                affectedCard.removeChangedTextColorWord(getTimestamp());
            }

            // remove set P/T
            if (!params.containsKey("CharacteristicDefining") && setPT) {
                affectedCard.removeNewPT(getTimestamp());
            }

            // remove P/T bonus
            if (addP.startsWith("AffectedX")) {
                powerBonus = getXMapValue(affectedCard);
            }
            if (addT.startsWith("AffectedX")) {
                toughnessBonus = getXMapValue(affectedCard);
            }
            affectedCard.addSemiPermanentPowerBoost(powerBonus * -1);
            affectedCard.addSemiPermanentToughnessBoost(toughnessBonus * -1);

            // remove keywords
            // TODO regular keywords currently don't try to use keyword multiplier
            // (Although nothing uses it at this time)
            if (params.containsKey("AddKeyword") || params.containsKey("RemoveKeyword")
                    || params.containsKey("RemoveAllAbilities")) {
                affectedCard.removeChangedCardKeywords(getTimestamp());
            }

            // remove abilities
            if (params.containsKey("AddAbility") || params.containsKey("GainsAbilitiesOf")) {
                for (final SpellAbility s : affectedCard.getSpellAbilities().threadSafeIterable()) {
                    if (s.isTemporary()) {
                        affectedCard.removeSpellAbility(s);
                    }
                }
            }

            if (addHiddenKeywords != null) {
                for (final String k : addHiddenKeywords) {
                    for (int j = 0; j < keywordMultiplier; j++) {
                        affectedCard.removeHiddenExtrinsicKeyword(k);
                    }
                }
            }

            // remove abilities
            if (params.containsKey("RemoveAllAbilities")) {
                for (final SpellAbility ab : affectedCard.getSpellAbilities()) {
                    ab.setTemporarilySuppressed(false);
                }
                for (final StaticAbility stA : affectedCard.getStaticAbilities()) {
                    stA.setTemporarilySuppressed(false);
                }
                for (final ReplacementEffect rE : affectedCard.getReplacementEffects()) {
                    rE.setTemporarilySuppressed(false);
                }
            }

            // remove Types
            if (params.containsKey("AddType") || params.containsKey("RemoveType")) {
                affectedCard.removeChangedCardTypes(getTimestamp());
            }

            // remove colors
            if (addColors != null) {
                affectedCard.removeColor(getTimestamp());
            }

            // remove may look at
            if (removeMayLookAt) {
                affectedCard.setMayLookAt(controller, false);
            }
            if (removeMayPlay) {
                affectedCard.removeMayPlay(source);
            }
            affectedCard.updateStateForView();
        }
        return affectedCards;
    }

    public void removeMapped(GameObjectMap map) {
        makeMappedCopy(map).remove();
    }

} // end class StaticEffect
