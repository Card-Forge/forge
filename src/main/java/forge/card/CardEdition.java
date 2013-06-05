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

import java.io.File;
import java.io.FilenameFilter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import forge.Singletons;
import forge.game.GameFormat;
import forge.util.FileSection;
import forge.util.FileUtil;
import forge.util.storage.StorageReaderFolder;


/**
 * <p>
 * CardSet class.
 * </p>
 * 
 * @author Forge
 * @version $Id: CardSet.java 9708 2011-08-09 19:34:12Z jendave $
 */
public final class CardEdition implements Comparable<CardEdition> { // immutable
    public enum Type {
        UNKNOWN,
        
        CORE,
        EXPANSION,
        
        REPRINT,
        STARTER,
        
        DUEL_DECKS,
        PREMIUM_DECK_SERIES,
        FROM_THE_VAULT,
        
        OTHER,
        THIRDPARTY // custom sets
    }
    
    public static class CardInSet {
        public final CardRarity rarity;
        public final String name;

        public CardInSet(final String name, final CardRarity rarity) {
            this.rarity = rarity;
            this.name = name;
        }
    }

    
    /** The Constant unknown. */
    private final static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    
    public static final CardEdition UNKNOWN = new CardEdition("1990-01-01", "??", "???", Type.UNKNOWN, "Undefined", new CardInSet[]{});

    private Date date;
    private String code2;
    private String code;
    private Type   type;
    private String name;
    private String alias = null;
    private boolean whiteBorder = false;
    private final CardInSet[] cards;
    
    
    private int boosterArts = 1;
    private SealedProductTemplate boosterTpl = null;

    private CardEdition(CardInSet[] cards) {
        this.cards = cards;
    }
    
    /**
     * Instantiates a new card set.
     * 
     * @param index indicates order of set release date
     * @param code2 the 2 (usually) letter code used for image filenames/URLs distributed by the HQ pics team that
     *   use Magic Workstation-type edition codes. Older sets only had 2-letter codes, and some of the 3-letter
     *   codes they use now aren't the same as the official list of 3-letter codes.  When Forge downloads set-pics,
     *   it uses the 3-letter codes for the folder no matter the age of the set.
     * @param code the MTG 3-letter set code
     * @param type the set type
     * @param name the name of the set
     * @param an optional secondary code alias for the set
     */
    private CardEdition(String date, String code2, String code, Type type, String name, CardInSet[] cards) {
        this(cards);
        this.code2 = code2;
        this.code  = code;
        this.type  = type;
        this.name  = name;
        this.date = parseDate(date);

    }
    
    private static Date parseDate(String date) {
        if( date.length() <= 7 ) 
            date = date + "-01";
        try {
            return formatter.parse(date);
        } catch (ParseException e) {
            return new Date();
        }
    }

    public Date getDate()  { return date;  }
    public String getCode2() { return code2; }
    public String getCode()  { return code;  }
    public Type   getType()  { return type;  }
    public String getName()  { return name;  }
    public String getAlias() { return alias; }
    public CardInSet[] getCards() { return cards; }

    /** The Constant fnGetName. */
    public static final Function<CardEdition, String> FN_GET_CODE = new Function<CardEdition, String>() {
        @Override
        public String apply(final CardEdition arg1) {
            return arg1.getCode();
        }
    };

    @Override
    public int compareTo(final CardEdition o) {
        if (o == null) {
            return 1;
        }
        return date.compareTo(o.date);
    }

    @Override
    public int hashCode() {
        return (this.code.hashCode() * 17) + this.name.hashCode();
    }

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

    @Override
    public String toString() {
        return this.name + " (set)";
    }


    /**
     * @return the whiteBorder
     */
    public boolean isWhiteBorder() {
        return whiteBorder;
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
                return subject.boosterTpl != null;
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

        public static final Predicate<CardEdition> hasBasicLands = new Predicate<CardEdition>() {
            @Override
            public boolean apply(CardEdition ed) {
                return null != CardDb.instance().tryGetCard("Plains", ed.getCode(), 0);
            };
        };

    }

    public static class EditionReader extends StorageReaderFolder<CardEdition> {
        public EditionReader(File path) {
            super(path, CardEdition.FN_GET_CODE);
        }
        
        public final static CardInSet[] arrCards = new CardInSet[] {};

        @Override
        protected CardEdition read(File file) {
            final Map<String, List<String>> contents = FileSection.parseSections(FileUtil.readFile(file));

            List<CardEdition.CardInSet> processedCards = new ArrayList<CardEdition.CardInSet>();
            for(String line : contents.get("cards")) {
                if (StringUtils.isBlank(line))
                    continue;

                // You may omit rarity for early development
                CardRarity r = CardRarity.smartValueOf(line.substring(0, 1));
                boolean hadRarity = r != CardRarity.Unknown && line.charAt(1) == ' ';
                String cardName = hadRarity ? line.substring(2) : line; 
                CardInSet cis = new CardInSet(cardName, r);
                processedCards.add(cis);
            }

            CardEdition res = new CardEdition(processedCards.toArray(arrCards));
            
            
            FileSection section = FileSection.parse(contents.get("metadata"), "=");
            res.name  = section.get("name");
            res.date  = parseDate(section.get("date"));
            res.code  = section.get("code");
            res.code2 = section.get("code2");
            if( res.code2 == null ) 
                res.code2 = res.code;
            
            res.boosterArts = section.getInt("BoosterCovers", 1);
            String boosterDesc = section.get("Booster");
            res.boosterTpl = boosterDesc == null ? null : new SealedProductTemplate(res.code, SealedProductTemplate.Reader.parseSlots(boosterDesc));
            
            res.alias = section.get("alias");
            res.whiteBorder = "white".equalsIgnoreCase(section.get("border"));
            String type  = section.get("type");
            Type enumType = Type.UNKNOWN;
            if (null != type && !type.isEmpty()) {
                try {
                    enumType = Type.valueOf(type.toUpperCase(Locale.ENGLISH));
                } catch (IllegalArgumentException e) {
                    // ignore; type will get UNKNOWN
                    System.err.println(String.format("Ignoring unknown type in set definitions: name: %s; type: %s", res.name, type));
                }
            }
            res.type = enumType;
            return res;
        }

        @Override
        protected FilenameFilter getFileFilter() {
            return TXT_FILE_FILTER;
        }


        public static final FilenameFilter TXT_FILE_FILTER = new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return name.endsWith(".txt");
            }
        };
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public int getCntBoosterPictures() {
        return boosterArts;
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public SealedProductTemplate getBoosterTemplate() {
        return boosterTpl;
    }
}
