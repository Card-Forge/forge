package forge.card;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * <p>Immutable Card type. Can be build only from parsing a string.</p>
 *
 * @author Forge
 * @version $Id: CardType.java 9708 2011-08-09 19:34:12Z jendave $
 */

public final class CardType implements Comparable<CardType> {
    private List<String> subType = new ArrayList<String>();
    private EnumSet<CardCoreType> coreType = EnumSet.noneOf(CardCoreType.class);
    private EnumSet<CardSuperType> superType = EnumSet.noneOf(CardSuperType.class);
    private String calculatedType = null; // since obj is immutable, this is calc'd once

    // This will be useful for faster parses
    private static HashMap<String, CardCoreType> stringToCoreType = new HashMap<String, CardCoreType>();
    private static HashMap<String, CardSuperType> stringToSuperType = new HashMap<String, CardSuperType>();
    static {
        for (CardSuperType st : CardSuperType.values()) { stringToSuperType.put(st.name(), st); }
        for (CardCoreType ct : CardCoreType.values()) { stringToCoreType.put(ct.name(), ct); }
    }

    private CardType() { } // use static ctors!

    // TODO: Debug this code
    public static CardType parse(final String typeText) {
        // Most types and subtypes, except "Serra�s Realm" and "Bolas�s Meditation Realm" consist of only one word
        final char space = ' ';
        CardType result = new CardType();

        int iTypeStart = 0;
        int iSpace = typeText.indexOf(space);
        boolean hasMoreTypes = typeText.length() > 0;
        while (hasMoreTypes) {
            String type = typeText.substring(iTypeStart, iSpace == -1 ? typeText.length() : iSpace );
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
        final String[] multiWordTypes = {"Serra's Realm", "Bolas's Meditation Realm"};
        // no need to loop for only 2 exceptions!
        if (multiWordTypes[0].startsWith(type) && !multiWordTypes[0].equals(type)) { return true; }
        if (multiWordTypes[1].startsWith(type) && !multiWordTypes[1].equals(type)) { return true; }
        return false;
    }

    private void parseAndAdd(final String type) {
        if ("-".equals(type)) { return; }

        CardCoreType ct = stringToCoreType.get(type);
        if (ct != null) { coreType.add(ct); return; }

        CardSuperType st = stringToSuperType.get(type);
        if (st != null) { superType.add(st); return; }

        // If not recognized by super- and core- this must be subtype
        subType.add(type);
    }

    public boolean subTypeContains(final String operand) { return subType.contains(operand); }
    public boolean typeContains(final CardCoreType operand) { return coreType.contains(operand); }
    public boolean superTypeContains(final CardSuperType operand) { return superType.contains(operand); }

    public boolean isCreature() { return coreType.contains(CardCoreType.Creature); }
    public boolean isPlaneswalker() { return coreType.contains(CardCoreType.Planeswalker); }
    public boolean isLand() { return coreType.contains(CardCoreType.Land); }
    public boolean isArtifact() { return coreType.contains(CardCoreType.Artifact); }
    public boolean isInstant() { return coreType.contains(CardCoreType.Instant); }
    public boolean isSorcery() { return coreType.contains(CardCoreType.Sorcery); }
    public boolean isEnchantment() { return coreType.contains(CardCoreType.Enchantment); }

    public boolean isBasic() { return superType.contains(CardSuperType.Basic); }
    public boolean isLegendary() { return superType.contains(CardSuperType.Legendary); }

    public String getTypesBeforeDash() {
        ArrayList<String> types = new ArrayList<String>();
        for (CardSuperType st : superType) { types.add(st.name()); }
        for (CardCoreType ct : coreType) { types.add(ct.name()); }
        return StringUtils.join(types, ' ');
    }

    public String getTypesAfterDash() {
        return StringUtils.join(subType, " ");
    }

    @Override
    public String toString() {
        if (null == calculatedType) { calculatedType = toStringImpl(); }
        return calculatedType;
    }

    private String toStringImpl() {
        if (subType.isEmpty()) { return getTypesBeforeDash(); }
        else { return String.format("%s - %s", getTypesBeforeDash(), getTypesAfterDash()); }
    }

    @Override
    public int compareTo(final CardType o) {
        return toString().compareTo(o.toString());
    }
    
}

