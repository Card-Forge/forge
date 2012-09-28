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

import com.google.common.base.Predicate;

import forge.Singletons;
import forge.game.GameFormat;
import forge.util.FileSection;
import forge.util.StorageReaderFile;
import forge.util.closures.Lambda1;


/**
 * <p>
 * CardSet class.
 * </p>
 * 
 * @author Forge
 * @version $Id: CardSet.java 9708 2011-08-09 19:34:12Z jendave $
 */
public final class CardEdition implements Comparable<CardEdition> { // immutable
    private final int index;
    private final String code;
    private final String code2;
    private final String name;
    private final String alias;

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
    public CardEdition(final int index, final String name, final String code, final String code2) {
        this(index, name, code, code2, null);
    }

    public CardEdition(final int index, final String name, final String code, final String code2, final String alias0) {
        this.code = code;
        this.code2 = code2;
        this.index = index;
        this.name = name;
        this.alias = alias0;
    }

    /** The Constant unknown. */
    public static final CardEdition UNKNOWN = new CardEdition(-1, "Undefined", "???", "??");

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

    /** The Constant fnGetName. */
    public static final Lambda1<String, CardEdition> FN_GET_CODE = new Lambda1<String, CardEdition>() {
        @Override
        public String apply(final CardEdition arg1) {
            return arg1.getCode();
        }
    };

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final CardEdition o) {
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

        final CardEdition other = (CardEdition) obj;
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

    public String getAlias() {
        return alias;
    }

    /**
     * The Class Predicates.
     */
    public abstract static class Predicates {

        /** The Constant canMakeBooster. */
        public static final Predicate<CardEdition> CAN_MAKE_BOOSTER = new CanMakeBooster();

        private static class CanMakeBooster implements Predicate<CardEdition> {
            @Override
            public boolean apply(final CardEdition subject) {
                return Singletons.getModel().getBoosters().contains(subject.getCode());
            }
        }

        public static final Predicate<CardEdition> HAS_TOURNAMENT_PACK = new CanMakeStarter();
        private static class CanMakeStarter implements Predicate<CardEdition> {
            @Override
            public boolean apply(final CardEdition subject) {
                return Singletons.getModel().getTournamentPacks().contains(subject.getCode());
            }
        }

        public static final Predicate<CardEdition> HAS_FAT_PACK = new CanMakeFatPack();
        private static class CanMakeFatPack implements Predicate<CardEdition> {
            @Override
            public boolean apply(final CardEdition subject) {
                return Singletons.getModel().getFatPacks().contains(subject.getCode());
            }
        }

        /**
         * Checks if is legal in format.
         *
         * @param format the format
         * @return the predicate
         */
        public static final Predicate<CardEdition> isLegalInFormat(final GameFormat format) {
            return new LegalInFormat(format);
        }

        private static class LegalInFormat implements Predicate<CardEdition> {
            private final GameFormat format;

            public LegalInFormat(final GameFormat fmt) {
                this.format = fmt;
            }

            @Override
            public boolean apply(final CardEdition subject) {
                return this.format.isSetLegal(subject.getCode());
            }
        }
    }

    public static class Reader extends StorageReaderFile<CardEdition> {

        public Reader(String pathname) {
            super(pathname, CardEdition.FN_GET_CODE);
        }

        /* (non-Javadoc)
         * @see forge.util.StorageReaderFile#read(java.lang.String)
         */
        @Override
        protected CardEdition read(String line) {
            final FileSection section = FileSection.parse(line, ":", "|");
            final String code = section.get("code3");
            final int index = section.getInt("index", -1);
            final String code2 = section.get("code2");
            final String name = section.get("name");
            final String alias = section.get("alias");

            return new CardEdition(index, name, code, code2, alias);
        }

    }
}
