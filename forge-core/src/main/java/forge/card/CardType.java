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

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

/**
 * <p>
 * Immutable Card type. Can be built only from parsing a string.
 * </p>
 * 
 * @author Forge
 * @version $Id: CardType.java 9708 2011-08-09 19:34:12Z jendave $
 */

public final class CardType implements Comparable<CardType> {
    
    public enum CoreType {

        Artifact(true),
        Conspiracy(false),
        Creature(true),
        Enchantment(true),
        Instant(false),
        Land(true),
        Plane(false),
        Planeswalker(true),
        Scheme(false),
        Sorcery(false),
        Tribal(false),
        Vanguard(false),
        Phenomenon(false);
        
        public final boolean isPermanent;
        
        private CoreType(final boolean permanent) {
            isPermanent = permanent;
        }

        public static CoreType smartValueOf(final String value) { return smartValueOf(value, true); }
        public static CoreType smartValueOf(final String value, boolean throwIfNotFound) {
            if (value == null) {
                return null;
            }
            final String valToCompate = value.trim();
            for (final CoreType v : CoreType.values()) {
                if (v.name().equalsIgnoreCase(valToCompate)) {
                    return v;
                }
            }
            if (throwIfNotFound)
                throw new IllegalArgumentException("No element named " + value + " in enum CoreType");

            return null;
        }

        public static boolean isAPermanentType(final String cardType) {
            CoreType ct = smartValueOf(cardType, false);
            return ct != null && ct.isPermanent;
        }
    }

    public enum SuperType {

        /** The Basic. */
        Basic,
        /** The Legendary. */
        Legendary,
        /** The Snow. */
        Snow,
        /** The Ongoing. */
        Ongoing,
        /** The World. */
        World
    }

    
    private final List<String> subType = new ArrayList<String>();
    private final EnumSet<CardType.CoreType> coreType = EnumSet.noneOf(CardType.CoreType.class);
    private final EnumSet<CardType.SuperType> superType = EnumSet.noneOf(CardType.SuperType.class);
    private String calculatedType = null; // since obj is immutable, this is
                                          // calc'd once

    // This will be useful for faster parses
    private static HashMap<String, CardType.CoreType> stringToCoreType = new HashMap<String, CardType.CoreType>();
    private static HashMap<String, CardType.SuperType> stringToSuperType = new HashMap<String, CardType.SuperType>();
    static {
        for (final CardType.SuperType st : CardType.SuperType.values()) {
            CardType.stringToSuperType.put(st.name(), st);
        }
        for (final CardType.CoreType ct : CardType.CoreType.values()) {
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

        final CardType.CoreType ct = CardType.stringToCoreType.get(type);
        if (ct != null) {
            this.coreType.add(ct);
            return;
        }

        final CardType.SuperType st = CardType.stringToSuperType.get(type);
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

    public boolean typeContains(final CardType.CoreType operand) {
        return this.coreType.contains(operand);
    }

    public boolean superTypeContains(final CardType.SuperType operand) {
        return this.superType.contains(operand);
    }

    public boolean isCreature() {
        return this.coreType.contains(CardType.CoreType.Creature);
    }

    public boolean isPlaneswalker() {
        return this.coreType.contains(CardType.CoreType.Planeswalker);
    }

    public boolean isLand() {
        return this.coreType.contains(CardType.CoreType.Land);
    }

    public boolean isArtifact() {
        return this.coreType.contains(CardType.CoreType.Artifact);
    }

    public boolean isInstant() {
        return this.coreType.contains(CardType.CoreType.Instant);
    }

    public boolean isSorcery() {
        return this.coreType.contains(CardType.CoreType.Sorcery);
    }

    public boolean isConspiracy() {
        return this.coreType.contains(CoreType.Conspiracy);
    }

    public boolean isVanguard() {
        return this.coreType.contains(CardType.CoreType.Vanguard);
    }

    public boolean isScheme() {
        return this.coreType.contains(CardType.CoreType.Scheme);
    }

    public boolean isEnchantment() {
        return this.coreType.contains(CardType.CoreType.Enchantment);
    }

    public boolean isBasic() {
        return this.superType.contains(CardType.SuperType.Basic);
    }

    public boolean isLegendary() {
        return this.superType.contains(CardType.SuperType.Legendary);
    }

    public boolean isSnow() {
        return this.superType.contains(CardType.SuperType.Snow);
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
        for (final CardType.SuperType st : this.superType) {
            types.add(st.name());
        }
        for (final CardType.CoreType ct : this.coreType) {
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
        return this.coreType.contains(CardType.CoreType.Plane);
    }
    
    public boolean isPhenomenon() {
        return this.coreType.contains(CardType.CoreType.Phenomenon);
    }

    

    /**
     * The Interface CardTypes.
     */
    public static class Constant {

        /** The loaded. */
        public static final boolean[] LOADED = { false };

        /** The card types. */
        public static final List<String> CARD_TYPES = new ArrayList<String>();

        /** The super types. */
        public static final List<String> SUPER_TYPES = new ArrayList<String>();

        /** The basic types. */
        public static final List<String> BASIC_TYPES = new ArrayList<String>();

        /** The land types. */
        public static final List<String> LAND_TYPES = new ArrayList<String>();

        /** The creature types. */
        public static final List<String> CREATURE_TYPES = new ArrayList<String>();

        /** The instant types. */
        public static final List<String> INSTANT_TYPES = new ArrayList<String>();

        /** The sorcery types. */
        public static final List<String> SORCERY_TYPES = new ArrayList<String>();

        /** The enchantment types. */
        public static final List<String> ENCHANTMENT_TYPES = new ArrayList<String>();

        /** The artifact types. */
        public static final List<String> ARTIFACT_TYPES = new ArrayList<String>();

        /** The walker types. */
        public static final List<String> WALKER_TYPES = new ArrayList<String>();
    }
    
    ///////// Utility methods
    public static boolean isACardType(final String cardType) {
        return CardType.getAllCardTypes().contains(cardType);
    }

    public static ArrayList<String> getAllCardTypes() {
        final ArrayList<String> types = new ArrayList<String>();
    
        // types.addAll(getCardTypes());
        types.addAll(Constant.CARD_TYPES);
    
        // Variant card types (I don't understand these lines, shouldn't core types be enough?)
        types.add("Plane");
        types.add("Scheme");
        types.add("Vanguard");
        types.add("Conspiracy");
    
        return types;
    }

    public static ArrayList<String> getBasicTypes() {
        final ArrayList<String> types = new ArrayList<String>();
    
        types.addAll(Constant.BASIC_TYPES);
    
        return types;
    }

    public static ArrayList<String> getLandTypes() {
        final ArrayList<String> types = new ArrayList<String>();
    
        types.addAll(Constant.BASIC_TYPES);
        types.addAll(Constant.LAND_TYPES);
    
        return types;
    }

    public static ArrayList<String> getCreatureTypes() {
        final ArrayList<String> types = new ArrayList<String>();
    
        types.addAll(Constant.CREATURE_TYPES);
    
        return types;
    }

    public static boolean isASuperType(final String cardType) {
        return (Constant.SUPER_TYPES.contains(cardType));
    }

    public static boolean isASubType(final String cardType) {
        return (!CardType.isASuperType(cardType) && !CardType.isACardType(cardType));
    }

    public static boolean isACreatureType(final String cardType) {
        return (Constant.CREATURE_TYPES.contains(cardType));
    }

    public static boolean isALandType(final String cardType) {
        return (Constant.LAND_TYPES.contains(cardType));
    }

    public static boolean isAPlaneswalkerType(final String cardType) {
        return (Constant.WALKER_TYPES.contains(cardType));
    }

    public static boolean isABasicLandType(final String cardType) {
        return (Constant.BASIC_TYPES.contains(cardType));
    }
}
