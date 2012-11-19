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

    private CardType() {
    } // use static ctors!

    // TODO: Debug this code
    /**
     * Parses the.
     * 
     * @param typeText
     *            the type text
     * @return the card type
     */
    public static CardType parse(final String typeText) {
        // Most types and subtypes, except "Serra�s Realm" and
        // "Bolas�s Meditation Realm" consist of only one word
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

    /**
     * Sub type contains.
     * 
     * @param operand
     *            the operand
     * @return true, if successful
     */
    public boolean subTypeContains(final String operand) {
        return this.subType.contains(operand);
    }

    /**
     * Type contains.
     * 
     * @param operand
     *            the operand
     * @return true, if successful
     */
    public boolean typeContains(final CardCoreType operand) {
        return this.coreType.contains(operand);
    }

    /**
     * Super type contains.
     * 
     * @param operand
     *            the operand
     * @return true, if successful
     */
    public boolean superTypeContains(final CardSuperType operand) {
        return this.superType.contains(operand);
    }

    /**
     * Checks if is creature.
     * 
     * @return true, if is creature
     */
    public boolean isCreature() {
        return this.coreType.contains(CardCoreType.Creature);
    }

    /**
     * Checks if is planeswalker.
     * 
     * @return true, if is planeswalker
     */
    public boolean isPlaneswalker() {
        return this.coreType.contains(CardCoreType.Planeswalker);
    }

    /**
     * Checks if is land.
     * 
     * @return true, if is land
     */
    public boolean isLand() {
        return this.coreType.contains(CardCoreType.Land);
    }

    /**
     * Checks if is artifact.
     * 
     * @return true, if is artifact
     */
    public boolean isArtifact() {
        return this.coreType.contains(CardCoreType.Artifact);
    }

    /**
     * Checks if is instant.
     * 
     * @return true, if is instant
     */
    public boolean isInstant() {
        return this.coreType.contains(CardCoreType.Instant);
    }

    /**
     * Checks if is sorcery.
     * 
     * @return true, if is sorcery
     */
    public boolean isSorcery() {
        return this.coreType.contains(CardCoreType.Sorcery);
    }
    
    /**
     * Checks if is vanguard.
     * 
     * @return true if vanguard
     */
    public boolean isVanguard() {
        return this.coreType.contains(CardCoreType.Vanguard);
    }

    /**
     * Checks if is enchantment.
     * 
     * @return true, if is enchantment
     */
    public boolean isEnchantment() {
        return this.coreType.contains(CardCoreType.Enchantment);
    }

    /**
     * Checks if is basic.
     * 
     * @return true, if is basic
     */
    public boolean isBasic() {
        return this.superType.contains(CardSuperType.Basic);
    }

    /**
     * Checks if is legendary.
     * 
     * @return true, if is legendary
     */
    public boolean isLegendary() {
        return this.superType.contains(CardSuperType.Legendary);
    }

    /**
     * Checks if is basic land.
     * 
     * @return true, if is basic land
     */
    public boolean isBasicLand() {
        return this.isBasic() && this.isLand();
    }

    /**
     * Gets the types before dash.
     * 
     * @return the types before dash
     */
    public String getTypesBeforeDash() {
        final ArrayList<String> types = new ArrayList<String>();
        for (final CardSuperType st : this.superType) {
            types.add(st.name());
        }
        for (final CardCoreType ct : this.coreType) {
            types.add(ct.name());
        }
        return StringUtils.join(types, ' ');
    }

    /**
     * Gets the types after dash.
     * 
     * @return the types after dash
     */
    public String getTypesAfterDash() {
        return StringUtils.join(this.subType, " ");
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        if (null == this.calculatedType) {
            this.calculatedType = this.toStringImpl();
        }
        return this.calculatedType;
    }

    private String toStringImpl() {
        if (this.subType.isEmpty()) {
            return this.getTypesBeforeDash();
        } else {
            return String.format("%s - %s", this.getTypesBeforeDash(), this.getTypesAfterDash());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final CardType o) {
        return this.toString().compareTo(o.toString());
    }

    /**
     * Gets the sub types.
     *
     * @return the sub types
     */
    public List<String> getSubTypes() {
        return this.subType;
    }

    /**
     * Shares sub type with.
     *
     * @param ctOther the ct other
     * @return true, if successful
     */
    public boolean sharesSubTypeWith(CardType ctOther) {
        for (String t : ctOther.getSubTypes()) {
            if (this.subTypeContains(t)) {
                return true;
            }
        }

        return false;
    }
}
