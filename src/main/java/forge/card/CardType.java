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
package forge.card;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import forge.Constant;

/**
 * <p>
 * Immutable Card type. Can be build only from parsing a string.
 * </p>
 * 
 * @author Forge
 * @version $Id: CardType.java 9708 2011-08-09 19:34:12Z jendave $
 */

public final class CardType implements Comparable<CardType> {
    private final List<String> subType = new ArrayList<String>();
    private final EnumSet<CardCoreType> coreType = EnumSet.noneOf(CardCoreType.class);
    private final EnumSet<CardSuperType> superType = EnumSet.noneOf(CardSuperType.class);
    private String calculatedType = null; // since obj is immutable, this is
                                          // calc'd once

    // This will be useful for faster parses
    private static HashMap<String, CardCoreType> stringToCoreType = new HashMap<String, CardCoreType>();
    private static HashMap<String, CardSuperType> stringToSuperType = new HashMap<String, CardSuperType>();
    static {
        for (final CardSuperType st : CardSuperType.values()) {
            CardType.stringToSuperType.put(st.name(), st);
        }
        for (final CardCoreType ct : CardCoreType.values()) {
            CardType.stringToCoreType.put(ct.name(), ct);
        }
    }

    private CardType() { }

    // TODO: Debug this code
    public static CardType parse(final String typeText) {
        // Most types and subtypes, except "Serra's Realm" and
        // "Bolas's Meditation Realm" consist of only one word
        final char space = ' ';
        final CardType result = new CardType();

        int iTypeStart = 0;
        int iSpace = typeText.indexOf(space);
        boolean hasMoreTypes = typeText.length() > 0;
        while (hasMoreTypes) {
            final String type = typeText.substring(iTypeStart, iSpace == -1 ? typeText.length() : iSpace);
            hasMoreTypes = iSpace != -1;
            if (!CardType.isMultiwordType(type) || !hasMoreTypes) {
                iTypeStart = iSpace + 1;
                result.parseAndAdd(type);
            }
            iSpace = typeText.indexOf(space, iSpace + 1);
        }
        return result;
    }

    public static CardType combine(final CardType a, final CardType b) {
        CardType result = new CardType();
        result.superType.addAll(a.superType);
        result.superType.addAll(b.superType);
        result.coreType.addAll(a.coreType);
        result.coreType.addAll(b.coreType);
        result.subType.addAll(a.subType);
        result.subType.addAll(b.subType);
        return result;
    }
    
    private static boolean isMultiwordType(final String type) {
        final String[] multiWordTypes = { "Serra's Realm", "Bolas's Meditation Realm" };
        // no need to loop for only 2 exceptions!
        if (multiWordTypes[0].startsWith(type) && !multiWordTypes[0].equals(type)) {
            return true;
        }
        if (multiWordTypes[1].startsWith(type) && !multiWordTypes[1].equals(type)) {
            return true;
        }
        return false;
    }

    private void parseAndAdd(final String type) {
        if ("-".equals(type)) {
            return;
        }

        final CardCoreType ct = CardType.stringToCoreType.get(type);
        if (ct != null) {
            this.coreType.add(ct);
            return;
        }

        final CardSuperType st = CardType.stringToSuperType.get(type);
        if (st != null) {
            this.superType.add(st);
            return;
        }

        // If not recognized by super- and core- this must be subtype
        this.subType.add(type);
    }

    public boolean subTypeContains(final String operand) {
        return this.subType.contains(operand);
    }

    public boolean typeContains(final CardCoreType operand) {
        return this.coreType.contains(operand);
    }

    public boolean superTypeContains(final CardSuperType operand) {
        return this.superType.contains(operand);
    }

    public boolean isCreature() {
        return this.coreType.contains(CardCoreType.Creature);
    }

    public boolean isPlaneswalker() {
        return this.coreType.contains(CardCoreType.Planeswalker);
    }

    public boolean isLand() {
        return this.coreType.contains(CardCoreType.Land);
    }

    public boolean isArtifact() {
        return this.coreType.contains(CardCoreType.Artifact);
    }

    public boolean isInstant() {
        return this.coreType.contains(CardCoreType.Instant);
    }

    public boolean isSorcery() {
        return this.coreType.contains(CardCoreType.Sorcery);
    }

    public boolean isVanguard() {
        return this.coreType.contains(CardCoreType.Vanguard);
    }

    public boolean isScheme() {
        return this.coreType.contains(CardCoreType.Scheme);
    }

    public boolean isEnchantment() {
        return this.coreType.contains(CardCoreType.Enchantment);
    }

    public boolean isBasic() {
        return this.superType.contains(CardSuperType.Basic);
    }

    public boolean isLegendary() {
        return this.superType.contains(CardSuperType.Legendary);
    }

    public boolean isBasicLand() {
        return this.isBasic() && this.isLand();
    }

    @Override
    public String toString() {
        if (null == this.calculatedType) {
            this.calculatedType = this.toStringImpl();
        }
        return this.calculatedType;
    }

    private String toStringImpl() {
        if (this.subType.isEmpty()) {
            return StringUtils.join(this.getTypesBeforeDash(), ' ');
        } else {
            return String.format("%s - %s", StringUtils.join(this.getTypesBeforeDash(), ' '), StringUtils.join(this.subType, " "));
        }
    }

    public List<String> getTypesBeforeDash() {
        final ArrayList<String> types = new ArrayList<String>();
        for (final CardSuperType st : this.superType) {
            types.add(st.name());
        }
        for (final CardCoreType ct : this.coreType) {
            types.add(ct.name());
        }
        return types;
    }

    @Override
    public int compareTo(final CardType o) {
        return this.toString().compareTo(o.toString());
    }

    public List<String> getSubTypes() {
        return this.subType;
    }

    public boolean sharesSubTypeWith(CardType ctOther) {
        for (String t : ctOther.getSubTypes()) {
            if (this.subTypeContains(t)) {
                return true;
            }
        }

        return false;
    }

    public boolean isPlane() {
        return this.coreType.contains(CardCoreType.Plane);
    }
    
    public boolean isPhenomenon() {
        return this.coreType.contains(CardCoreType.Phenomenon);
    }

    ///////// Utility methods
    public static boolean isACardType(final String cardType) {
        return CardType.getAllCardTypes().contains(cardType);
    }

    public static ArrayList<String> getAllCardTypes() {
        final ArrayList<String> types = new ArrayList<String>();
    
        // types.addAll(getCardTypes());
        types.addAll(Constant.CardTypes.CARD_TYPES);
    
        // not currently used by Forge
        types.add("Plane");
        types.add("Scheme");
        types.add("Vanguard");
    
        return types;
    }

    public static ArrayList<String> getBasicTypes() {
        final ArrayList<String> types = new ArrayList<String>();
    
        types.addAll(Constant.CardTypes.BASIC_TYPES);
    
        return types;
    }

    public static ArrayList<String> getLandTypes() {
        final ArrayList<String> types = new ArrayList<String>();
    
        types.addAll(Constant.CardTypes.BASIC_TYPES);
        types.addAll(Constant.CardTypes.LAND_TYPES);
    
        return types;
    }

    public static ArrayList<String> getCreatureTypes() {
        final ArrayList<String> types = new ArrayList<String>();
    
        types.addAll(Constant.CardTypes.CREATURE_TYPES);
    
        return types;
    }

    public static boolean isASuperType(final String cardType) {
        return (Constant.CardTypes.SUPER_TYPES.contains(cardType));
    }

    public static boolean isASubType(final String cardType) {
        return (!CardType.isASuperType(cardType) && !CardType.isACardType(cardType));
    }

    public static boolean isACreatureType(final String cardType) {
        return (Constant.CardTypes.CREATURE_TYPES.contains(cardType));
    }

    public static boolean isALandType(final String cardType) {
        return (Constant.CardTypes.LAND_TYPES.contains(cardType));
    }

    public static boolean isAPlaneswalkerType(final String cardType) {
        return (Constant.CardTypes.WALKER_TYPES.contains(cardType));
    }

    public static boolean isABasicLandType(final String cardType) {
        return (Constant.CardTypes.BASIC_TYPES.contains(cardType));
    }
}
