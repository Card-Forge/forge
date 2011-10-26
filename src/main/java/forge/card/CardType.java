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
    private List<String> subType = new ArrayList<String>();
    private EnumSet<CardCoreType> coreType = EnumSet.noneOf(CardCoreType.class);
    private EnumSet<CardSuperType> superType = EnumSet.noneOf(CardSuperType.class);
    private String calculatedType = null; // since obj is immutable, this is
                                          // calc'd once

    // This will be useful for faster parses
    private static HashMap<String, CardCoreType> stringToCoreType = new HashMap<String, CardCoreType>();
    private static HashMap<String, CardSuperType> stringToSuperType = new HashMap<String, CardSuperType>();
    static {
        for (CardSuperType st : CardSuperType.values()) {
            stringToSuperType.put(st.name(), st);
        }
        for (CardCoreType ct : CardCoreType.values()) {
            stringToCoreType.put(ct.name(), ct);
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
        CardType result = new CardType();

        int iTypeStart = 0;
        int iSpace = typeText.indexOf(space);
        boolean hasMoreTypes = typeText.length() > 0;
        while (hasMoreTypes) {
            String type = typeText.substring(iTypeStart, iSpace == -1 ? typeText.length() : iSpace);
            hasMoreTypes = iSpace != -1;
            if (!isMultiwordType(type) || !hasMoreTypes) {
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

        CardCoreType ct = stringToCoreType.get(type);
        if (ct != null) {
            coreType.add(ct);
            return;
        }

        CardSuperType st = stringToSuperType.get(type);
        if (st != null) {
            superType.add(st);
            return;
        }

        // If not recognized by super- and core- this must be subtype
        subType.add(type);
    }

    /**
     * Sub type contains.
     * 
     * @param operand
     *            the operand
     * @return true, if successful
     */
    public boolean subTypeContains(final String operand) {
        return subType.contains(operand);
    }

    /**
     * Type contains.
     * 
     * @param operand
     *            the operand
     * @return true, if successful
     */
    public boolean typeContains(final CardCoreType operand) {
        return coreType.contains(operand);
    }

    /**
     * Super type contains.
     * 
     * @param operand
     *            the operand
     * @return true, if successful
     */
    public boolean superTypeContains(final CardSuperType operand) {
        return superType.contains(operand);
    }

    /**
     * Checks if is creature.
     * 
     * @return true, if is creature
     */
    public boolean isCreature() {
        return coreType.contains(CardCoreType.Creature);
    }

    /**
     * Checks if is planeswalker.
     * 
     * @return true, if is planeswalker
     */
    public boolean isPlaneswalker() {
        return coreType.contains(CardCoreType.Planeswalker);
    }

    /**
     * Checks if is land.
     * 
     * @return true, if is land
     */
    public boolean isLand() {
        return coreType.contains(CardCoreType.Land);
    }

    /**
     * Checks if is artifact.
     * 
     * @return true, if is artifact
     */
    public boolean isArtifact() {
        return coreType.contains(CardCoreType.Artifact);
    }

    /**
     * Checks if is instant.
     * 
     * @return true, if is instant
     */
    public boolean isInstant() {
        return coreType.contains(CardCoreType.Instant);
    }

    /**
     * Checks if is sorcery.
     * 
     * @return true, if is sorcery
     */
    public boolean isSorcery() {
        return coreType.contains(CardCoreType.Sorcery);
    }

    /**
     * Checks if is enchantment.
     * 
     * @return true, if is enchantment
     */
    public boolean isEnchantment() {
        return coreType.contains(CardCoreType.Enchantment);
    }

    /**
     * Checks if is basic.
     * 
     * @return true, if is basic
     */
    public boolean isBasic() {
        return superType.contains(CardSuperType.Basic);
    }

    /**
     * Checks if is legendary.
     * 
     * @return true, if is legendary
     */
    public boolean isLegendary() {
        return superType.contains(CardSuperType.Legendary);
    }

    /**
     * Checks if is basic land.
     * 
     * @return true, if is basic land
     */
    public boolean isBasicLand() {
        return isBasic() && isLand();
    }

    /**
     * Gets the types before dash.
     * 
     * @return the types before dash
     */
    public String getTypesBeforeDash() {
        ArrayList<String> types = new ArrayList<String>();
        for (CardSuperType st : superType) {
            types.add(st.name());
        }
        for (CardCoreType ct : coreType) {
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
        return StringUtils.join(subType, " ");
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        if (null == calculatedType) {
            calculatedType = toStringImpl();
        }
        return calculatedType;
    }

    private String toStringImpl() {
        if (subType.isEmpty()) {
            return getTypesBeforeDash();
        } else {
            return String.format("%s - %s", getTypesBeforeDash(), getTypesAfterDash());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final CardType o) {
        return toString().compareTo(o.toString());
    }

}
