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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.StaticData;
import forge.card.CardDb.SetPreference;
import forge.deck.CardPool;
import forge.item.PaperCard;
import forge.item.SealedProduct;
import forge.util.Aggregates;
import forge.util.FileSection;
import forge.util.FileUtil;
import forge.util.IItemReader;
import forge.util.MyRandom;
import forge.util.storage.StorageBase;
import forge.util.storage.StorageReaderBase;
import forge.util.storage.StorageReaderFolder;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;


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
        ONLINE,
        STARTER,

        DUEL_DECKS,
        PREMIUM_DECK_SERIES,
        FROM_THE_VAULT,

        OTHER,
        THIRDPARTY // custom sets
    }

    public enum FoilType {
        NOT_SUPPORTED, // sets before Urza's Legacy
        OLD_STYLE, // sets between Urza's Legacy and 8th Edition
        MODERN // 8th Edition and newer
    }

    public static class CardInSet {
        public final CardRarity rarity;
        public final int collectorNumber;
        public final String name;

        public CardInSet(final String name, final int collectorNumber, final CardRarity rarity) {
            this.name = name;
            this.collectorNumber = collectorNumber;
            this.rarity = rarity;
        }
 
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (collectorNumber != -1) {
                sb.append(collectorNumber);
                sb.append(' ');
            }
            if (rarity != CardRarity.Unknown) {
                sb.append(rarity);
                sb.append(' ');
            }
            sb.append(name);
            return sb.toString();
        }
    }

    private final static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

    public static final CardEdition UNKNOWN = new CardEdition("1990-01-01", "???", "??", "??", Type.UNKNOWN, "Undefined", FoilType.NOT_SUPPORTED, new CardInSet[]{});

    private Date date;
    private String code;
    private String code2;
    private String mciCode;
    private Type   type;
    private String name;
    private String alias = null;
    private boolean whiteBorder = false;
    private FoilType foilType = FoilType.NOT_SUPPORTED;
    private double foilChanceInBooster = 0;
    private boolean foilAlwaysInCommonSlot = false;
    private double chanceReplaceCommonWith = 0;
    private String slotReplaceCommonWith = "Common";
    private String additionalSheetForFoils = "";
    private String additionalUnlockSet = "";
    private boolean smallSetOverride = false;
    private final CardInSet[] cards;

    private int boosterArts = 1;
    private SealedProduct.Template boosterTpl = null;

    private CardEdition(CardInSet[] cards) {
        this.cards = cards;
    }

    /**
     * Instantiates a new card set.
     *
     * @param date indicates order of set release date
     * @param code the MTG 3-letter set code
     * @param code2 the 2 (usually) letter code used for image filenames/URLs distributed by the HQ pics team that
     *   use Magic Workstation-type edition codes. Older sets only had 2-letter codes, and some of the 3-letter
     *   codes they use now aren't the same as the official list of 3-letter codes.  When Forge downloads set-pics,
     *   it uses the 3-letter codes for the folder no matter the age of the set.
     * @param mciCode the code used by magiccards.info website.
     * @param type the set type
     * @param name the name of the set
     * @param cards the cards in the set
     */
    private CardEdition(String date, String code, String code2, String mciCode, Type type, String name, FoilType foil, CardInSet[] cards) {
        this(cards);
        this.code  = code;
        this.code2 = code2;
        this.mciCode = mciCode;
        this.type  = type;
        this.name  = name;
        this.date = parseDate(date);
        this.foilType = foil;
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
    public String getCode()  { return code;  }
    public String getCode2() { return code2; }
    public String getMciCode() { return mciCode; }
    public Type   getType()  { return type;  }
    public String getName()  { return name;  }
    public String getAlias() { return alias; }
    public FoilType getFoilType() { return foilType; }
    public double getFoilChanceInBooster() { return foilChanceInBooster; }
    public boolean getFoilAlwaysInCommonSlot() { return foilAlwaysInCommonSlot; }
    public double getChanceReplaceCommonWith() { return chanceReplaceCommonWith; }
    public String getSlotReplaceCommonWith() { return slotReplaceCommonWith; }
    public String getAdditionalSheetForFoils() { return additionalSheetForFoils; }
    public String getAdditionalUnlockSet() { return additionalUnlockSet; }
    public boolean getSmallSetOverride() { return smallSetOverride; }
    public CardInSet[] getCards() { return cards; }

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
        return this.name + " (" + this.code + ")";
    }

    public boolean isWhiteBorder() {
        return whiteBorder;
    }

    public boolean isLargeSet() {
        return cards.length > 200 && !smallSetOverride;
    }

    public int getCntBoosterPictures() {
        return boosterArts;
    }

    public SealedProduct.Template getBoosterTemplate() {
        return boosterTpl;
    }

    public boolean hasBoosterTemplate() {
        return boosterTpl != null;
    }

    public static class Reader extends StorageReaderFolder<CardEdition> {
        public Reader(File path) {
            super(path, CardEdition.FN_GET_CODE);
        }

        @Override
        protected CardEdition read(File file) {
            final Map<String, List<String>> contents = FileSection.parseSections(FileUtil.readFile(file));

            List<CardEdition.CardInSet> processedCards = new ArrayList<>();
            for(String line : contents.get("cards")) {
                if (StringUtils.isBlank(line))
                    continue;

                // Optional collector number at the start.
                String[] split = line.split(" ", 2);
                int collectorNumber = -1;
                if (split.length >= 2 && StringUtils.isNumeric(split[0])) {
                    collectorNumber = Integer.parseInt(split[0]);
                    line = split[1];
                }

                // You may omit rarity for early development
                CardRarity r = CardRarity.smartValueOf(line.substring(0, 1));
                boolean hadRarity = r != CardRarity.Unknown && line.charAt(1) == ' ';
                String cardName = hadRarity ? line.substring(2) : line;
                CardInSet cis = new CardInSet(cardName, collectorNumber, r);
                processedCards.add(cis);
            }

            CardEdition res = new CardEdition(processedCards.toArray(new CardInSet[processedCards.size()]));

            FileSection section = FileSection.parse(contents.get("metadata"), "=");
            res.name  = section.get("name");
            res.date  = parseDate(section.get("date"));
            res.code  = section.get("code");
            res.code2 = section.get("code2");
            if (res.code2 == null) {
                res.code2 = res.code;
            }
            res.mciCode = section.get("MciCode");
            if (res.mciCode == null) {
                res.mciCode = res.code2.toLowerCase();
            }

            res.boosterArts = section.getInt("BoosterCovers", 1);
            String boosterDesc = section.get("Booster");
            res.boosterTpl = boosterDesc == null ? null : new SealedProduct.Template(res.code, SealedProduct.Template.Reader.parseSlots(boosterDesc));

            res.alias = section.get("alias");
            res.whiteBorder = "white".equalsIgnoreCase(section.get("border"));
            String type  = section.get("type");
            Type enumType = Type.UNKNOWN;
            if (null != type && !type.isEmpty()) {
                try {
                    enumType = Type.valueOf(type.toUpperCase(Locale.ENGLISH));
                } catch (IllegalArgumentException ignored) {
                    // ignore; type will get UNKNOWN
                    System.err.println(String.format("Ignoring unknown type in set definitions: name: %s; type: %s", res.name, type));
                }
            }
            res.type = enumType;

            switch(section.get("foil", "newstyle").toLowerCase()) {
                case "notsupported":
                    res.foilType = FoilType.NOT_SUPPORTED;
                    break;
                case "oldstyle":
                case "classic":
                    res.foilType = FoilType.OLD_STYLE;
                    break;
                case "newstyle":
                case "modern":
                    res.foilType = FoilType.MODERN;
                    break;
                default:
                    res.foilType = FoilType.NOT_SUPPORTED;
                    break;
            }
            String[] replaceCommon = section.get("ChanceReplaceCommonWith", "0F Common").split(" ", 2);
            res.chanceReplaceCommonWith = Double.parseDouble(replaceCommon[0]);
            res.slotReplaceCommonWith = replaceCommon[1];

            res.foilChanceInBooster = section.getDouble("FoilChanceInBooster", 21.43F) / 100.0F;

            res.foilAlwaysInCommonSlot = section.getBoolean("FoilAlwaysInCommonSlot", true);
            res.additionalSheetForFoils = section.get("AdditionalSheetForFoils", "");

            res.additionalUnlockSet = section.get("AdditionalSetUnlockedInQuest", ""); // e.g. Time Spiral Timeshifted (TSB) for Time Spiral

            res.smallSetOverride = section.getBoolean("TreatAsSmallSet", false); // for "small" sets with over 200 cards (e.g. Eldritch Moon)

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

    public static class Collection extends StorageBase<CardEdition> {
        private final Map<String, CardEdition> aliasToEdition = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        public Collection(IItemReader<CardEdition> reader) {
            super("Card editions", reader);

            for (CardEdition ee : this) {
                String alias = ee.getAlias();
                if (null != alias) {
                    aliasToEdition.put(alias, ee);
                }
                aliasToEdition.put(ee.getCode2(), ee);
            }
        }

        //Gets a sets by code.  It will search first by three letter codes, then by aliases and two-letter codes.
        @Override
        public CardEdition get(final String code) {
            if (code == null) {
                return null;
            }

            CardEdition baseResult = super.get(code);
            return baseResult == null ? aliasToEdition.get(code) : baseResult;
        }

        public Iterable<CardEdition> getOrderedEditions() {
            List<CardEdition> res = Lists.newArrayList(this);
            Collections.sort(res);
            Collections.reverse(res);
            return res;
        }

        public CardEdition getEditionByCodeOrThrow(final String code) {
            final CardEdition set = this.get(code);
            if (null == set) {
                throw new RuntimeException(String.format("Edition with code '%s' not found", code));
            }
            return set;
        }

        // used by image generating code
        public String getCode2ByCode(final String code) {
            final CardEdition set = this.get(code);
            return set == null ? "" : set.getCode2();
        }

        // used by image generating code
        public String getMciCodeByCode(final String code) {
            final CardEdition set = this.get(code);
            return set == null ? "" : set.getMciCode();
        }

        public final Function<String, CardEdition> FN_EDITION_BY_CODE = new Function<String, CardEdition>() {
            @Override
            public CardEdition apply(String code) {
                return Collection.this.get(code);
            }
        };

        public final Comparator<PaperCard> CARD_EDITION_COMPARATOR = new Comparator<PaperCard>() {
            @Override
            public int compare(PaperCard c1, PaperCard c2) {
                return Collection.this.get(c1.getEdition()).compareTo(Collection.this.get(c2.getEdition()));
            }
        };

        public IItemReader<SealedProduct.Template> getBoosterGenerator() {
            // TODO Auto-generated method stub
            return new StorageReaderBase<SealedProduct.Template>(null) {
                @Override
                public Map<String, SealedProduct.Template> readAll() {
                    Map<String, SealedProduct.Template> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                    for(CardEdition ce : Collection.this) {
                         map.put(ce.getCode(), ce.getBoosterTemplate());
                    }
                    return map;
                }

                @Override
                public String getItemKey(SealedProduct.Template item) {
                    return item.getEdition();
                }

                @Override
                public String getFullPath() {
                    return null;
                }
            };
        }

        public CardEdition getEarliestEditionWithAllCards(CardPool cards) {
            Set<String> minEditions = new HashSet<>();

            SetPreference strictness = SetPreference.EarliestCoreExp;

            for (Entry<PaperCard, Integer> k : cards) {
                PaperCard cp = StaticData.instance().getCommonCards().getCardFromEdition(k.getKey().getName(), strictness);
                if( cp == null && strictness == SetPreference.EarliestCoreExp) {
                    strictness = SetPreference.Earliest; // card is not found in core and expansions only (probably something CMD or C13)
                    cp = StaticData.instance().getCommonCards().getCardFromEdition(k.getKey().getName(), strictness);
                }
                if ( cp == null )
                    cp = k.getKey(); // it's unlikely, this code will ever run

                minEditions.add(cp.getEdition());
            }

            for(CardEdition ed : getOrderedEditions()) {
                if(minEditions.contains(ed.getCode()))
                    return ed;
            }
            return UNKNOWN;
        }
    }

    public static class Predicates {
        public static final Predicate<CardEdition> CAN_MAKE_BOOSTER = new CanMakeBooster();

        private static class CanMakeBooster implements Predicate<CardEdition> {
            @Override
            public boolean apply(final CardEdition subject) {
                return subject.hasBoosterTemplate();
            }
        }

        public static CardEdition getRandomSetWithAllBasicLands(Iterable<CardEdition> allEditions) {
            return Aggregates.random(Iterables.filter(allEditions, hasBasicLands));
        }

        public static final Predicate<CardEdition> HAS_TOURNAMENT_PACK = new CanMakeStarter();
        private static class CanMakeStarter implements Predicate<CardEdition> {
            @Override
            public boolean apply(final CardEdition subject) {
                return StaticData.instance().getTournamentPacks().contains(subject.getCode());
            }
        }

        public static final Predicate<CardEdition> HAS_FAT_PACK = new CanMakeFatPack();
        private static class CanMakeFatPack implements Predicate<CardEdition> {
            @Override
            public boolean apply(final CardEdition subject) {
                return StaticData.instance().getFatPacks().contains(subject.getCode());
            }
        }

        public static final Predicate<CardEdition> HAS_BOOSTER_BOX = new CanMakeBoosterBox();
        private static class CanMakeBoosterBox implements Predicate<CardEdition> {
            @Override
            public boolean apply(final CardEdition subject) {
                return StaticData.instance().getBoosterBoxes().contains(subject.getCode());
            }
        }

        public static final Predicate<CardEdition> hasBasicLands = new Predicate<CardEdition>() {
            @Override
            public boolean apply(CardEdition ed) {
                for(String landName : MagicColor.Constant.BASIC_LANDS) {
                    if (null == StaticData.instance().getCommonCards().getCard(landName, ed.getCode(), 0))
                        return false;
                }
                return true;
            }
        };
    }

    public static int getRandomFoil(final String setCode) {
        FoilType foilType = FoilType.NOT_SUPPORTED;
        if (setCode != null
                && StaticData.instance().getEditions().get(setCode) != null) {
            foilType = StaticData.instance().getEditions().get(setCode)
                    .getFoilType();
        }
        if (foilType != FoilType.NOT_SUPPORTED) {
            return foilType == FoilType.MODERN
                    ? MyRandom.getRandom().nextInt(9) +  1
                    : MyRandom.getRandom().nextInt(9) + 11;
        }
        return 0;
    }
}
