package forge.card;

import net.slightlymagic.braids.util.lambda.Lambda1;
import net.slightlymagic.maxmtg.Predicate;
import forge.SetUtils;
import forge.game.GameFormat;

/**
 * <p>
 * CardSet class.
 * </p>
 * 
 * @author Forge
 * @version $Id: CardSet.java 9708 2011-08-09 19:34:12Z jendave $
 */
public final class CardSet implements Comparable<CardSet> { // immutable
    private final int index;
    private final String code;
    private final String code2;
    private final String name;
    private final BoosterData boosterData;

    /**
     * Instantiates a new card set.
     * 
     * @param index
     *            the index
     * @param name
     *            the name
     * @param code
     *            the code
     * @param code2
     *            the code2
     */
    public CardSet(final int index, final String name, final String code, final String code2) {
        this(index, name, code, code2, null);
    }

    /**
     * Instantiates a new card set.
     * 
     * @param index
     *            the index
     * @param name
     *            the name
     * @param code
     *            the code
     * @param code2
     *            the code2
     * @param booster
     *            the booster
     */
    public CardSet(final int index, final String name, final String code, final String code2, final BoosterData booster) {
        this.code = code;
        this.code2 = code2;
        this.index = index;
        this.name = name;
        this.boosterData = booster;
    }

    /** The Constant unknown. */
    public static final CardSet unknown = new CardSet(-1, "Undefined", "???", "??");

    /**
     * Gets the name.
     * 
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the code.
     * 
     * @return the code
     */
    public String getCode() {
        return code;
    }

    /**
     * Gets the code2.
     * 
     * @return the code2
     */
    public String getCode2() {
        return code2;
    }

    /**
     * Gets the index.
     * 
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Can generate booster.
     * 
     * @return true, if successful
     */
    public boolean canGenerateBooster() {
        return boosterData != null;
    }

    /**
     * Gets the booster data.
     * 
     * @return the booster data
     */
    public BoosterData getBoosterData() {
        return boosterData;
    }

    /** The Constant fnGetName. */
    public static final Lambda1<String, CardSet> fnGetName = new Lambda1<String, CardSet>() {
        @Override
        public String apply(final CardSet arg1) {
            return arg1.name;
        }
    };

    /** The Constant fn1. */
    public static final Lambda1<CardSet, CardSet> fn1 = new Lambda1<CardSet, CardSet>() {
        @Override
        public CardSet apply(final CardSet arg1) {
            return arg1;
        }
    };

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final CardSet o) {
        if (o == null) {
            return 1;
        }
        return o.index - this.index;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return code.hashCode() * 17 + name.hashCode();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        CardSet other = (CardSet) obj;
        return other.name.equals(this.name) && this.code.equals(other.code);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.name + " (set)";
    }

    /**
     * The Class BoosterData.
     */
    public static class BoosterData {
        private final int nCommon;
        private final int nUncommon;
        private final int nRare;
        private final int nSpecial;
        private final int nDoubleFaced;
        private final int nLand;
        private final int foilRate;
        private static final int CARDS_PER_BOOSTER = 15;

        // private final String landCode;
        /**
         * Instantiates a new booster data.
         * 
         * @param nC
         *            the n c
         * @param nU
         *            the n u
         * @param nR
         *            the n r
         * @param nS
         *            the n s
         * @param nDF
         *            the n df
         */
        public BoosterData(final int nC, final int nU, final int nR, final int nS, final int nDF) {
            // if this booster has more that 10 cards, there must be a land in
            // 15th slot unless it's already taken
            this(nC, nU, nR, nS, nDF, nC + nR + nU + nS + nDF > 10 ? CARDS_PER_BOOSTER - nC - nR - nU - nS - nDF : 0,
                    68);
        }

        /**
         * Instantiates a new booster data.
         * 
         * @param nC
         *            the n c
         * @param nU
         *            the n u
         * @param nR
         *            the n r
         * @param nS
         *            the n s
         * @param nDF
         *            the n df
         * @param nL
         *            the n l
         * @param oneFoilPer
         *            the one foil per
         */
        public BoosterData(final int nC, final int nU, final int nR, final int nS, final int nDF, final int nL,
                final int oneFoilPer) {
            nCommon = nC;
            nUncommon = nU;
            nRare = nR;
            nSpecial = nS;
            nDoubleFaced = nDF;
            nLand = nL > 0 ? nL : 0;
            foilRate = oneFoilPer;
        }

        /**
         * Gets the common.
         * 
         * @return the common
         */
        public final int getCommon() {
            return nCommon;
        }

        /**
         * Gets the uncommon.
         * 
         * @return the uncommon
         */
        public final int getUncommon() {
            return nUncommon;
        }

        /**
         * Gets the rare.
         * 
         * @return the rare
         */
        public final int getRare() {
            return nRare;
        }

        /**
         * Gets the special.
         * 
         * @return the special
         */
        public final int getSpecial() {
            return nSpecial;
        }

        /**
         * Gets the double faced.
         * 
         * @return the double faced
         */
        public final int getDoubleFaced() {
            return nDoubleFaced;
        }

        /**
         * Gets the land.
         * 
         * @return the land
         */
        public final int getLand() {
            return nLand;
        }

        /**
         * Gets the foil chance.
         * 
         * @return the foil chance
         */
        public final int getFoilChance() {
            return foilRate;
        }
    }

    /**
     * The Class Predicates.
     */
    public abstract static class Predicates {

        /** The Constant canMakeBooster. */
        public static final Predicate<CardSet> canMakeBooster = new CanMakeBooster();

        /**
         * Checks if is legal in format.
         * 
         * @param format
         *            the format
         * @return the predicate
         */
        public static final Predicate<CardSet> isLegalInFormat(final GameFormat format) {
            return new LegalInFormat(format);
        }

        private static class CanMakeBooster extends Predicate<CardSet> {
            public boolean isTrue(final CardSet subject) {
                return subject.canGenerateBooster();
            }
        }

        private static class LegalInFormat extends Predicate<CardSet> {
            private final GameFormat format;

            public LegalInFormat(final GameFormat fmt) {
                format = fmt;
            }

            public boolean isTrue(final CardSet subject) {
                return format.isSetLegal(subject.getCode());
            }
        }

        /**
         * The Class Presets.
         */
        public abstract static class Presets {

            /** The Constant setsInT2. */
            public static final Predicate<CardSet> setsInT2 = isLegalInFormat(SetUtils.getStandard());

            /** The Constant setsInExt. */
            public static final Predicate<CardSet> setsInExt = isLegalInFormat(SetUtils.getExtended());

            /** The Constant setsInModern. */
            public static final Predicate<CardSet> setsInModern = isLegalInFormat(SetUtils.getModern());
        }
    }
}
