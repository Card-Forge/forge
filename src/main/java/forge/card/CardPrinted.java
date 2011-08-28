package forge.card;

import java.util.Arrays;

import net.slightlymagic.braids.util.lambda.Lambda1;
import net.slightlymagic.maxmtg.Predicate;
import forge.AllZone;
import forge.Card;

/**
 * <p>CardReference class.</p>
 *
 * @author Forge
 * @version $Id: CardReference.java 9708 2011-08-09 19:34:12Z jendave $
 */
public final class CardPrinted implements Comparable<CardPrinted> {
    // Reference to rules
    private final transient CardRules card;

    // These fields are kinda PK for PrintedCard
    private final String name;
    private final String cardSet;
    private final int artIndex;
    private final boolean foiled;

    // Calculated fields are below:
    private final transient CardRarity rarity; // rarity is given in ctor when set is assigned

    // need this to be sure that different cased names won't break the system (and create uniqie cardref entries)
    private final transient String _name_lcase;

    // field RO accessors
    public String getName() { return name; }
    public String getSet() { return cardSet; }
    public int getArtIndex() { return artIndex; }
    public boolean isFoil() { return foiled; }
    public CardRules getCard() { return card; }
    public CardRarity getRarity() { return rarity; }
    

    // Lambda to get rules for selects from list of printed cards 
    public static final Lambda1<CardRules, CardPrinted> fnGetRules = new Lambda1<CardRules, CardPrinted>() {
        @Override public CardRules apply(final CardPrinted from) { return from.card; }
    };

    // Constructor is private. All non-foiled instances are stored in CardDb
    private CardPrinted(final CardRules c, final String set, final CardRarity rare, final int index, boolean foil) {
        card = c;
        name = c.getName();
        cardSet = set;
        artIndex = index;
        foiled = foil;
        rarity = rare;
        _name_lcase = name.toLowerCase();
    }

    /* package visibility */
    static CardPrinted build(final CardRules c, final String set, final CardRarity rare, final int index) {
        return new CardPrinted(c, set, rare, index, false);
    }

    /* foiled don't need to stay in CardDb's structures, so u'r free to create */
    public static CardPrinted makeFoiled(final CardPrinted c) {
        return new CardPrinted(c.card, c.cardSet, c.rarity, c.artIndex, true);
    }

    // Want this class to be a key for HashTable
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) { return true; }
        if (obj == null) { return false; }
        if (getClass() != obj.getClass()) { return false; }

        CardPrinted other = (CardPrinted) obj;
        if (!name.equals(other.name)) { return false; }
        if (!cardSet.equals(other.cardSet)) { return false; }
        if (other.foiled != this.foiled || other.artIndex != this.artIndex) { return false; }

        return true;
    }

    @Override
    public int hashCode() {
        int code = _name_lcase.hashCode() * 11 + cardSet.hashCode() * 59 + artIndex * 2;
        if (foiled) { return code + 1; }
        return code;
    }

    @Override
    public String toString() {
        return String.format("%s|%s", name, cardSet);
    }

    public Card toForgeCard() {
        Card c = AllZone.getCardFactory().getCard(name, null);
        c.setCurSetCode(getSet());
        return c;
    }

    @Override
    public int compareTo(final CardPrinted o) {
        int nameCmp = _name_lcase.compareTo(o._name_lcase);
        if (0 != nameCmp) { return nameCmp; }
        // TODO: compare sets properly
        return cardSet.compareTo(o.cardSet);
    }


    public static abstract class Predicates {
        public static Predicate<CardPrinted> rarity(final boolean isEqual, final CardRarity value)
        {
            return new PredicateRarity(value, isEqual);
        }
        public static Predicate<CardPrinted> printedInSets(final String[] value)
        {
            return new PredicateSets(value);
        }

        private static class PredicateRarity extends Predicate<CardPrinted> {
            private final CardRarity operand;
            private final boolean shouldBeEqual;

            @Override
            public boolean isTrue(final CardPrinted card) {
                return card.rarity.equals(operand) == shouldBeEqual;
            }

            public PredicateRarity(final CardRarity type, final boolean wantEqual) {
                operand = type;
                shouldBeEqual = wantEqual;
            }
        }

        private static class PredicateSets extends Predicate<CardPrinted> {
            private final String[] sets;
            @Override public boolean isTrue(final CardPrinted card) {
                return Arrays.binarySearch(sets, card.rarity) >= 0;
            }
            public PredicateSets(final String[] wantSets) {
                sets = wantSets.clone();
                Arrays.sort(sets);
            }
        }

        public abstract static class Presets {
            // Think twice before using these, since rarity is a prop of printed card.
            public static final Predicate<CardPrinted> isCommon = rarity(true, CardRarity.Common);
            public static final Predicate<CardPrinted> isUncommon = rarity(true, CardRarity.Uncommon);
            public static final Predicate<CardPrinted> isRare = rarity(true, CardRarity.Rare);
            public static final Predicate<CardPrinted> isMythicRare = rarity(true, CardRarity.MythicRare);
            public static final Predicate<CardPrinted> isRareOrMythic = Predicate.or(isRare, isMythicRare);
            
            public static final Predicate<CardPrinted> isSpecial = rarity(true, CardRarity.Special);

            public static final Predicate<CardPrinted> exceptLands = rarity(false, CardRarity.BasicLand);

            // TODO: Update this code on each rotation (or move this list to a file)
            public static final Predicate<CardPrinted> isStandard = printedInSets(
                new String[] {"M12", "NPH", "MBS", "SOM", "M11", "ROE", "WWK", "ZEN"});

        }
    }
}
