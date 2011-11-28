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
    public static final CardSet UNKNOWN = new CardSet(-1, "Undefined", "???", "??");

    /**
     * Gets the name.
     * 
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the code.
     * 
     * @return the code
     */
    public String getCode() {
        return this.code;
    }

    /**
     * Gets the code2.
     * 
     * @return the code2
     */
    public String getCode2() {
        return this.code2;
    }

    /**
     * Gets the index.
     * 
     * @return the index
     */
    public int getIndex() {
        return this.index;
    }

    /**
     * Can generate booster.
     * 
     * @return true, if successful
     */
    public boolean canGenerateBooster() {
        return this.boosterData != null;
    }

    /**
     * Gets the booster data.
     * 
     * @return the booster data
     */
    public BoosterData getBoosterData() {
        return this.boosterData;
    }

    /** The Constant fnGetName. */
    public static final Lambda1<String, CardSet> FN_GET_NAME = new Lambda1<String, CardSet>() {
        @Override
        public String apply(final CardSet arg1) {
            return arg1.name;
        }
    };

    /** The Constant fn1. */
    public static final Lambda1<CardSet, CardSet> FN1 = new Lambda1<CardSet, CardSet>() {
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
        return (this.code.hashCode() * 17) + this.name.hashCode();
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
        if (this.getClass() != obj.getClass()) {
            return false;
        }

        final CardSet other = (CardSet) obj;
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
            this(nC, nU, nR, nS, nDF, (nC + nR + nU + nS + nDF) > 10 ? BoosterData.CARDS_PER_BOOSTER - nC - nR - nU
                    - nS - nDF : 0, 68);
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
            this.nCommon = nC;
            this.nUncommon = nU;
            this.nRare = nR;
            this.nSpecial = nS;
            this.nDoubleFaced = nDF;
            this.nLand = nL > 0 ? nL : 0;
            this.foilRate = oneFoilPer;
        }

        /**
         * Gets the common.
         * 
         * @return the common
         */
        public final int getCommon() {
            return this.nCommon;
        }

        /**
         * Gets the uncommon.
         * 
         * @return the uncommon
         */
        public final int getUncommon() {
            return this.nUncommon;
        }

        /**
         * Gets the rare.
         * 
         * @return the rare
         */
        public final int getRare() {
            return this.nRare;
        }

        /**
         * Gets the special.
         * 
         * @return the special
         */
        public final int getSpecial() {
            return this.nSpecial;
        }

        /**
         * Gets the double faced.
         * 
         * @return the double faced
         */
        public final int getDoubleFaced() {
            return this.nDoubleFaced;
        }

        /**
         * Gets the land.
         * 
         * @return the land
         */
        public final int getLand() {
            return this.nLand;
        }

        /**
         * Gets the foil chance.
         * 
         * @return the foil chance
         */
        public final int getFoilChance() {
            return this.foilRate;
        }
    }

    /**
     * The Class Predicates.
     */
    public abstract static class Predicates {

        /** The Constant canMakeBooster. */
        public static final Predicate<CardSet> CAN_MAKE_BOOSTER = new CanMakeBooster();

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
            @Override
            public boolean isTrue(final CardSet subject) {
                return subject.canGenerateBooster();
            }
        }

        private static class LegalInFormat extends Predicate<CardSet> {
            private final GameFormat format;

            public LegalInFormat(final GameFormat fmt) {
                this.format = fmt;
            }

            @Override
            public boolean isTrue(final CardSet subject) {
                return this.format.isSetLegal(subject.getCode());
            }
        }

        /**
         * The Class Presets.
         */
        public abstract static class Presets {

            /** The Constant setsInT2. */
            public static final Predicate<CardSet> SETS_IN_STANDARD = Predicates
                    .isLegalInFormat(SetUtils.getStandard());

            /** The Constant setsInExt. */
            public static final Predicate<CardSet> SETS_IN_EXT = Predicates.isLegalInFormat(SetUtils.getExtended());

            /** The Constant setsInModern. */
            public static final Predicate<CardSet> SET_IN_MODERN = Predicates.isLegalInFormat(SetUtils.getModern());
        }
    }
}
