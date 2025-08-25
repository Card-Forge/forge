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

import com.google.common.collect.*;

import forge.StaticData;
import forge.card.CardDb.CardArtPreference;
import forge.deck.CardPool;
import forge.item.BoosterSlot;
import forge.item.PaperCard;
import forge.item.SealedTemplate;
import forge.item.SealedTemplateWithSlots;
import forge.util.*;
import forge.util.storage.StorageBase;
import forge.util.storage.StorageReaderBase;
import forge.util.storage.StorageReaderFolder;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>
 * CardSet class.
 * </p>
 *
 * @author Forge
 * @version $Id: CardSet.java 9708 2011-08-09 19:34:12Z jendave $
 */
public final class CardEdition implements Comparable<CardEdition> {

    public DraftOptions getDraftOptions() {
        return draftOptions;
    }

    public void setDraftOptions(DraftOptions draftOptions) {
        this.draftOptions = draftOptions;
    }

    // immutable
    public enum Type {
        UNKNOWN,
        CORE,
        EXPANSION,
        STARTER,
        REPRINT,
        BOXED_SET,
        COLLECTOR_EDITION,
        DUEL_DECK,
        PROMO,
        ONLINE,
        DRAFT,
        COMMANDER,
        MULTIPLAYER,
        FUNNY,
        OTHER,  // FALLBACK CATEGORY
        CUSTOM_SET; // custom sets

        public static final EnumSet<Type> REPRINT_SET_TYPES = EnumSet.of(REPRINT, PROMO, COLLECTOR_EDITION);

        public String getBoosterBoxDefault() {
            switch (this) {
                case CORE:
                case EXPANSION:
                    return "36";
                default:
                    return "0";
            }
        }

        public String getFatPackDefault() {
            switch (this) {
                case CORE:
                case EXPANSION:
                    return "10";
                default:
                    return "0";
            }
        }

        public String toString(){
            String[] names = TextUtil.splitWithParenthesis(this.name().toLowerCase(), '_');
            for (int i = 0; i < names.length; i++)
                names[i] = TextUtil.capitalize(names[i]);
            return TextUtil.join(Arrays.asList(names), " ");
        }

        public static Type fromString(String label){
            List<String> names = Arrays.asList(TextUtil.splitWithParenthesis(label.toUpperCase(), ' '));
            String value = TextUtil.join(names, "_");
            return Type.valueOf(value);
        }
    }

    public enum FoilType {
        NOT_SUPPORTED, // sets before Urza's Legacy
        OLD_STYLE, // sets between Urza's Legacy and 8th Edition
        MODERN // 8th Edition and newer
    }

    public enum BorderColor {
        WHITE,
        BLACK,
        SILVER,
        GOLD
    }

    // reserved names of sections inside edition files, that are not parsed as cards
    private static final List<String> reservedSectionNames = ImmutableList.of("metadata", "tokens", "other");

    // commonly used printsheets with collector number
    public enum EditionSectionWithCollectorNumbers {
        CARDS("cards"),
        SPECIAL_SLOT("special slot"), //to help with convoluted boosters
        PRECON_PRODUCT("precon product"),
        BORDERLESS("borderless"),
        ETCHED("etched"),
        SHOWCASE("showcase"),
        FULL_ART("full art"),
        EXTENDED_ART("extended art"),
        ALTERNATE_ART("alternate art"),
        RETRO_FRAME("retro frame"),
        BUY_A_BOX("buy a box"),
        PROMO("promo"),
        PRERELEASE_PROMO("prerelease promo"),
        BUNDLE("bundle"),
        BOX_TOPPER("box topper"),
        DUNGEONS("dungeons"),
        JUMPSTART("jumpstart"),
        REBALANCED("rebalanced"),
        ETERNAL("eternal"),
        CONJURED("conjured"),
        SCHEME("scheme");

        private final String name;

        EditionSectionWithCollectorNumbers(final String n) { this.name = n; }

        public String getName() {
            return name;
        }

        public static List<String> getNames() {
            List<String> list = new ArrayList<>();
            for (EditionSectionWithCollectorNumbers s : EditionSectionWithCollectorNumbers.values()) {
                String sName = s.getName();
                list.add(sName);
            }
            return list;
        }
    }

    private static final Map<String, String> sortableCollNumberLookup = new HashMap<>();
    /**
     * This method implements the main strategy to allow for natural ordering of collectorNumber
     * (i.e. "1" < "10"), overloading the default lexicographic order (i.e. "10" < "1").
     * Any non-numerical parts in the input collectorNumber will be also accounted for, and attached to the
     * resulting sorting key, accordingly.
     *
     * @param collectorNumber: Input collectorNumber tro transform in a Sorting Key
     * @return A 5-digits zero-padded collector number + any non-numerical parts attached.
     */
    public static String getSortableCollectorNumber(final String collectorNumber){
        String inputCollNumber = collectorNumber;
        if (collectorNumber == null || collectorNumber.isEmpty())
            inputCollNumber = "50000";  // very big number of 5 digits to have them in last positions

        String matchedCollNr = sortableCollNumberLookup.getOrDefault(inputCollNumber, null);
        if (matchedCollNr != null)
            return  matchedCollNr;

        // Now, for proper sorting, let's zero-pad the collector number (if integer)
        int collNr;
        String sortableCollNr;
        try {
            collNr = Integer.parseInt(inputCollNumber);
            sortableCollNr = String.format("%05d", collNr);
        } catch (NumberFormatException ex) {
            String nonNumSub = inputCollNumber.replaceAll("[0-9]", "");
            String onlyNumSub = inputCollNumber.replaceAll("[^0-9]", "");
            try {
                collNr = Integer.parseInt(onlyNumSub);
            } catch (NumberFormatException exon) {
                collNr = 0; // this is the case of ONLY-letters collector numbers
            }
            if ((collNr > 0) && (inputCollNumber.startsWith(onlyNumSub))) // e.g. 12a, 37+, 2018f,
                sortableCollNr = String.format("%05d", collNr) + nonNumSub;
            else // e.g. WS6, S1
                sortableCollNr = nonNumSub + String.format("%05d", collNr);
        }
        sortableCollNumberLookup.put(inputCollNumber, sortableCollNr);
        return sortableCollNr;
    }

    public record EditionEntry(String name, String collectorNumber, CardRarity rarity, String artistName, String functionalVariantName) implements Comparable<EditionEntry> {

        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (collectorNumber != null) {
                sb.append(collectorNumber);
                sb.append(' ');
            }
            if (rarity != CardRarity.Unknown && rarity != CardRarity.Token) {
                sb.append(rarity);
                sb.append(' ');
            }
            sb.append(name);
            if (artistName != null) {
                sb.append(" @");
                sb.append(artistName);
            }
            if (functionalVariantName != null) {
                sb.append(" $");
                sb.append(functionalVariantName);
            }
            return sb.toString();
        }

        @Override
        public int compareTo(EditionEntry o) {
            final int nameCmp = name.compareToIgnoreCase(o.name);
            if (0 != nameCmp) {
                return nameCmp;
            }
            String thisCollNr = getSortableCollectorNumber(collectorNumber);
            String othrCollNr = getSortableCollectorNumber(o.collectorNumber);
            final int collNrCmp = thisCollNr.compareTo(othrCollNr);
            if (0 != collNrCmp) {
                return collNrCmp;
            }
            return rarity.compareTo(o.rarity);
        }
    }

    private final static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * Equivalent to the set code of CardEdition.UNKNOWN
     */
    public static final String UNKNOWN_CODE = "???";
    public static final CardEdition UNKNOWN = new CardEdition("1990-01-01", UNKNOWN_CODE, "??", Type.UNKNOWN, "Undefined", FoilType.NOT_SUPPORTED, new EditionEntry[]{});
    private Date date;
    private String code;
    private String code2;
    private String scryfallCode;
    private String tokensCode;
    private String tokenFallbackCode;
    private String cardsLanguage;
    private Type   type;
    private String name;
    private String alias = null;
    private BorderColor borderColor = BorderColor.BLACK;

    // SealedProduct
    private String prerelease = null;
    private int boosterBoxCount = 36;
    private int fatPackCount = 10;
    private String fatPackExtraSlots = "";

    // Booster/draft info
    private List<BoosterSlot> boosterSlots = null;
    private boolean smallSetOverride = false;
    private String additionalUnlockSet = "";
    private FoilType foilType = FoilType.NOT_SUPPORTED;

    // Replace all of these things with booster slots
    private boolean foilAlwaysInCommonSlot = false;
    private double foilChanceInBooster = 0;
    private double chanceReplaceCommonWith = 0;
    private String slotReplaceCommonWith = "Common";
    private String additionalSheetForFoils = "";
    private String boosterMustContain = "";
    private String boosterReplaceSlotFromPrintSheet = "";
    private String sheetReplaceCardFromSheet = "";
    private String sheetReplaceCardFromSheet2 = "";

    // Draft options
    private DraftOptions draftOptions = null;
    private String[] chaosDraftThemes = new String[0];

    private final ListMultimap<String, EditionEntry> cardMap;
    private final List<EditionEntry> cardsInSet;
    private final ListMultimap<String, EditionEntry> tokenMap;
    // custom print sheets that will be loaded lazily
    private final Map<String, List<String>> customPrintSheetsToParse;
    private ListMultimap<String, EditionEntry> otherMap = ArrayListMultimap.create();

    private int boosterArts = 1;
    private SealedTemplate boosterTpl = null;
    private final Map<String, SealedTemplate> boosterTemplates = new HashMap<>();

    private CardEdition(ListMultimap<String, EditionEntry> cardMap, ListMultimap<String, EditionEntry> tokens, Map<String, List<String>> customPrintSheetsToParse) {
        this.cardMap = cardMap;
        this.cardsInSet = new ArrayList<>(cardMap.values());
        Collections.sort(cardsInSet);
        this.tokenMap = tokens;
        this.customPrintSheetsToParse = customPrintSheetsToParse;
    }

    private CardEdition(EditionEntry[] cards, ListMultimap<String, EditionEntry> tokens) {
        List<EditionEntry> cardsList = Arrays.asList(cards);
        this.cardMap = ArrayListMultimap.create();
        this.cardMap.replaceValues("cards", cardsList);
        this.cardsInSet = new ArrayList<>(cardsList);
        Collections.sort(cardsInSet);
        this.tokenMap = tokens;
        this.customPrintSheetsToParse = new HashMap<>();
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
     * @param type the set type
     * @param name the name of the set
     * @param cards the cards in the set
     */
    private CardEdition(String date, String code, String code2, Type type, String name, FoilType foil, EditionEntry[] cards) {
        this(cards, ArrayListMultimap.create());
        this.code  = code;
        this.code2 = code2;
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
        } catch (Exception e) {
            return new Date();
        }
    }

    public Date getDate()  { return date;  }
    public String getCode()  { return code;  }
    public String getCode2() { return code2; }
    public String getScryfallCode() { return scryfallCode.toLowerCase(); }
    public String getTokensCode() { return tokensCode.toLowerCase(); }
    public String getCardsLangCode() { return cardsLanguage.toLowerCase(); }
    public Type   getType()  { return type;  }
    public String getName()  { return name;  }
    public String getAlias() { return alias; }

    public String getPrerelease() { return prerelease; }
    public int getBoosterBoxCount() { return boosterBoxCount; }
    public int getFatPackCount() { return fatPackCount; }
    public String getFatPackExtraSlots() { return fatPackExtraSlots; }

    public FoilType getFoilType() { return foilType; }
    public double getFoilChanceInBooster() { return foilChanceInBooster; }
    public boolean getFoilAlwaysInCommonSlot() { return foilAlwaysInCommonSlot; }
    public double getChanceReplaceCommonWith() { return chanceReplaceCommonWith; }
    public String getSlotReplaceCommonWith() { return slotReplaceCommonWith; }
    public String getAdditionalSheetForFoils() { return additionalSheetForFoils; }
    public String getAdditionalUnlockSet() { return additionalUnlockSet; }
    public String getBoosterMustContain() { return boosterMustContain; }
    public String getBoosterReplaceSlotFromPrintSheet() { return boosterReplaceSlotFromPrintSheet; }
    public String getSheetReplaceCardFromSheet() { return sheetReplaceCardFromSheet; }
    public String getSheetReplaceCardFromSheet2() { return sheetReplaceCardFromSheet2; }
    public String[] getChaosDraftThemes() { return chaosDraftThemes; }

    public List<EditionEntry> getCards() { return cardMap.get(EditionSectionWithCollectorNumbers.CARDS.getName()); }
    public List<EditionEntry> getRebalancedCards() { return cardMap.get(EditionSectionWithCollectorNumbers.REBALANCED.getName()); }
    public List<EditionEntry> getFunnyEternalCards() { return cardMap.get(EditionSectionWithCollectorNumbers.ETERNAL.getName()); }
    public List<EditionEntry> getObtainableCards() { 
        List<EditionEntry> allCards = new ArrayList<>(getAllCardsInSet());
        List<EditionEntry> conjuredCards = cardMap.get(EditionSectionWithCollectorNumbers.CONJURED.getName());
        if (conjuredCards != null) {
            allCards.removeAll(conjuredCards);
        }

        return allCards; 
    }
    public List<EditionEntry> getAllCardsInSet() {
        return cardsInSet;
    }

    private ListMultimap<String, EditionEntry> cardsInSetLookupMap = null;

    /**
     * Get all the CardInSet instances with the input card name.
     * @param cardName Name of the Card to look for.
     * @return A List of all the CardInSet instances for a given name.
     * If not fount, an Empty sequence (view) will be returned instead!
     */
    public List<EditionEntry> getCardInSet(String cardName){
        if (cardsInSetLookupMap == null) {
            // initialise
            cardsInSetLookupMap = Multimaps.newListMultimap(new TreeMap<>(String.CASE_INSENSITIVE_ORDER), Lists::newArrayList);
            List<EditionEntry> cardsInSet = this.getAllCardsInSet();
            for (EditionEntry cis : cardsInSet){
                String key = cis.name;
                cardsInSetLookupMap.put(key, cis);
            }
        }
        return this.cardsInSetLookupMap.get(cardName);
    }

    public EditionEntry getCardFromCollectorNumber(String collectorNumber) {
        if(collectorNumber == null || collectorNumber.isEmpty())
            return null;
        for(EditionEntry c : this.cardsInSet) {
            //Could build a map for this one too if it's used for more than one-offs.
            if (c.collectorNumber.equalsIgnoreCase(collectorNumber))
                return c;
        }
        return null;
    }

    public boolean isRebalanced(String cardName) {
        for (EditionEntry cis : getRebalancedCards()) {
            if (cis.name.equals(cardName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isCardObtainable(String cardName) {
        for (EditionEntry ee : cardMap.get(EditionSectionWithCollectorNumbers.CONJURED.getName())) {
            if (ee.name.equals(cardName)) {
                return false;
            }
        }
        return true;
    }

    public boolean isModern() { return getDate().after(parseDate("2003-07-27")); } //8ED and above are modern except some promo cards and others

    public Multimap<String, EditionEntry> getTokens() { return tokenMap; }

    public String getTokenSet(String token) {
        if (tokenMap.containsKey(token)) {
            return this.getCode();
        }
        if (this.tokenFallbackCode != null) {
            return StaticData.instance().getCardEdition(this.tokenFallbackCode).getTokenSet(token);
        }
        return null;
    }
    public String getOtherSet(String token) {
        if (otherMap.containsKey(token)) {
            return this.getCode();
        }
        if (this.tokenFallbackCode != null) {
            return StaticData.instance().getCardEdition(this.tokenFallbackCode).getOtherSet(token);
        }
        return null;
    }

    public EditionEntry findOther(String name) {
        if (otherMap.containsKey(name)) {
            return Aggregates.random(otherMap.get(name));
        }
        return null;
    }

    @Override
    public int compareTo(final CardEdition o) {
        if (o == null) {
            return 1;
        }
        int dateComp = date.compareTo(o.date);
        if (0 != dateComp)
            return dateComp;
        return name.compareTo(o.name);
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

    public BorderColor getBorderColor() {
        return borderColor;
    }

    public boolean isLargeSet() {
        return this.cardsInSet.size() > 200 && !smallSetOverride;
    }

    public int getCntBoosterPictures() {
        return boosterArts;
    }

    public SealedTemplate getBoosterTemplate() {
        return getBoosterTemplate("Draft");
    }
    public SealedTemplate getBoosterTemplate(String boosterType) {
        return boosterTemplates.get(boosterType);
    }
    public String getRandomBoosterKind() {
        List<String> boosterTypes = Lists.newArrayList(boosterTemplates.keySet());

        if (boosterTypes.isEmpty()) {
            return null;
        }

        Collections.shuffle(boosterTypes);
        return boosterTypes.get(0);
    }

    public Set<String> getAvailableBoosterTypes() {
        return boosterTemplates.keySet();
    }

    public boolean hasBoosterTemplate() {
        return boosterTemplates.containsKey("Draft");
    }

    public List<PrintSheet> getPrintSheetsBySection() {
        final CardDb cardDb = StaticData.instance().getCommonCards();

        List<PrintSheet> sheets = Lists.newArrayList();
        for (Map.Entry<String, java.util.Collection<EditionEntry>> section : cardMap.asMap().entrySet()) {
            if (section.getKey().equals(EditionSectionWithCollectorNumbers.CONJURED.getName())) {
                continue;
            }
            PrintSheet sheet = new PrintSheet(String.format("%s %s", this.getCode(), section.getKey()));

            for (EditionEntry card : section.getValue()) {
                sheet.add(cardDb.getCard(card.name, this.getCode(), card.collectorNumber));
            }

            sheets.add(sheet);
        }

        for (String sheetName : customPrintSheetsToParse.keySet()) {
            List<String> sheetToParse = customPrintSheetsToParse.get(sheetName);
            CardPool sheetPool = CardPool.fromCardList(sheetToParse);
            PrintSheet sheet = new PrintSheet(String.format("%s %s", this.getCode(), sheetName), sheetPool);
            sheets.add(sheet);
        }

        return sheets;
    }

    public static class Reader extends StorageReaderFolder<CardEdition> {
        private final boolean isCustomEditions;

        public Reader(File path) {
            super(path, CardEdition::getCode);
            this.isCustomEditions = false;
        }

        public Reader(File path, boolean isCustomEditions) {
            super(path, CardEdition::getCode);
            this.isCustomEditions = isCustomEditions;
        }

        protected Map<String, CardEdition> createMap() {
            // Create our own map to make it case-insensitive for set codes.
            return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        }

        @Override
        protected CardEdition read(File file) {
            final Map<String, List<String>> contents = FileSection.parseSections(FileUtil.readFile(file));

            final Pattern pattern = Pattern.compile(
            /*
            The following pattern will match the WAR Japanese art entries,
            it should also match the Un-set and older alternate art cards
            like Merseine from FEM.
             */
            // Collector numbers now should allow hyphens for Planeswalker Championship Promos
            //"(^(?<cnum>[0-9]+.?) )?((?<rarity>[SCURML]) )?(?<name>.*)$"
            /*  Ideally we'd use the named group above, but Android 6 and
                earlier don't appear to support named groups.
                So, untill support for those devices is officially dropped,
                we'll have to suffice with numbered groups.
                We are looking for:
                    * cnum - grouping #2
                    * rarity - grouping #4
                    * name - grouping #5
                    * artist name - grouping #7
                    * functional variant name - grouping #9
             */
//                "(^(.?[0-9A-Z]+.?))?(([SCURML]) )?(.*)$"
                    "(^(.?[0-9A-Z-]+\\S?[A-Z]*)\\s)?(([SCURML])\\s)?([^@\\$]*)( @([^\\$]*))?( \\$(.+))?$"
            );

            final Pattern tokenPattern = Pattern.compile(
                    /*
                     * cnum - grouping #2
                     * name - grouping #3
                     * artist name - grouping #5
                     */
                    "(^(.?[0-9A-Z-]+\\S?[A-Z]*)\\s)?([^@]*)( @(.*))?$"
            );

            ListMultimap<String, EditionEntry> cardMap = ArrayListMultimap.create();
            List<BoosterSlot> boosterSlots = null;
            Map<String, List<String>> customPrintSheetsToParse = new HashMap<>();
            List<String> editionSectionsWithCollectorNumbers = EditionSectionWithCollectorNumbers.getNames();

            FileSection metadata = FileSection.parse(contents.get("metadata"), FileSection.EQUALS_KV_SEPARATOR);
            List<String> boosterSlotsToParse = Lists.newArrayList();
            if (metadata.contains("BoosterSlots")) {
                boosterSlotsToParse = Lists.newArrayList(metadata.get("BoosterSlots").split(","));
                boosterSlots = Lists.newArrayList();
            }

            for (String sectionName : contents.keySet()) {
                // skip reserved section names like 'metadata' and 'tokens' that are handled separately
                if (reservedSectionNames.contains(sectionName)) {
                    continue;
                }

                // parse sections of the format "<collector number> <rarity> <name>"
                if (editionSectionsWithCollectorNumbers.contains(sectionName)) {
                    for(String line : contents.get(sectionName)) {
                        Matcher matcher = pattern.matcher(line);

                        if (!matcher.matches()) {
                            continue;
                        }

                        String collectorNumber = matcher.group(2);
                        CardRarity r = CardRarity.smartValueOf(matcher.group(4));
                        String cardName = matcher.group(5);
                        String artistName = matcher.group(7);
                        String functionalVariantName = matcher.group(9);
                        EditionEntry cis = new EditionEntry(cardName, collectorNumber, r, artistName, functionalVariantName);

                        cardMap.put(sectionName, cis);
                    }
                } else if (boosterSlotsToParse.contains(sectionName)) {
                    // parse booster slots of the format "Base=N\n|Replace=<amount> <sheet>"
                    boosterSlots.add(BoosterSlot.parseSlot(sectionName, contents.get(sectionName)));
                } else {
                    // save custom print sheets of the format "<amount> <name>|<setcode>|<art index>"
                    // to parse later when printsheets are loaded lazily (and the cardpool is already initialized)
                    customPrintSheetsToParse.put(sectionName, contents.get(sectionName));
                }
            }

            ListMultimap<String, EditionEntry> tokenMap = ArrayListMultimap.create();
            ListMultimap<String, EditionEntry> otherMap = ArrayListMultimap.create();
            // parse tokens section
            if (contents.containsKey("tokens")) {
                for (String line : contents.get("tokens")) {
                    if (StringUtils.isBlank(line))
                        continue;
                    Matcher matcher = tokenPattern.matcher(line);

                    if (!matcher.matches()) {
                        continue;
                    }

                    String collectorNumber = matcher.group(2);
                    String cardName = matcher.group(3);
                    String artistName = matcher.group(5);
                    // rarity isn't used for this anyway
                    EditionEntry tis = new EditionEntry(cardName, collectorNumber, CardRarity.Token, artistName, null);
                    tokenMap.put(cardName, tis);
                }
            }
            if (contents.containsKey("other")) {
                for (String line : contents.get("other")) {
                    if (StringUtils.isBlank(line))
                        continue;
                    Matcher matcher = tokenPattern.matcher(line);

                    if (!matcher.matches()) {
                        continue;
                    }
                    String collectorNumber = matcher.group(2);
                    String cardName = matcher.group(3);
                    String artistName = matcher.group(5);
                    EditionEntry tis = new EditionEntry(cardName, collectorNumber, CardRarity.Unknown, artistName, null);
                    otherMap.put(cardName, tis);
                }
            }

            CardEdition res = new CardEdition(cardMap, tokenMap, customPrintSheetsToParse);
            res.boosterSlots = boosterSlots;
            // parse metadata section
            res.name  = metadata.get("name");
            res.date  = parseDate(metadata.get("date"));
            res.code  = metadata.get("code");
            res.code2 = metadata.get("code2", res.code);
            res.scryfallCode = metadata.get("ScryfallCode", res.code);
            res.tokensCode = metadata.get("TokensCode", "T" + res.scryfallCode);
            res.tokenFallbackCode = metadata.get("TokenFallbackCode");
            res.cardsLanguage = metadata.get("CardLang", "en");
            res.boosterArts = metadata.getInt("BoosterCovers", 1);

            res.otherMap = otherMap;

            String boosterDesc = metadata.get("Booster");

            if (metadata.contains("Booster")) {
                // Historical naming convention in Forge for "DraftBooster"
                // Do i have access to editions slots?
                if (res.boosterSlots != null) {
                    res.boosterTpl = new SealedTemplateWithSlots(res.code, SealedTemplate.Reader.parseSlots(boosterDesc), res.boosterSlots);
                } else {
                    res.boosterTpl = new SealedTemplate(res.code, SealedTemplate.Reader.parseSlots(boosterDesc));
                }

                res.boosterTemplates.put("Draft", res.boosterTpl);
            }

            String[] boostertype = { "Draft", "Collector", "Set" };
            // Theme boosters aren't here because they are closer to preconstructed decks, and should be treated as such
            for (String type : boostertype) {
                String name = type + "Booster";
                if (metadata.contains(name)) {
                    res.boosterTemplates.put(type, new SealedTemplate(res.code, SealedTemplate.Reader.parseSlots(metadata.get(name))));
                }
            }

            res.alias = metadata.get("alias");
            res.borderColor = BorderColor.valueOf(metadata.get("border", "Black").toUpperCase(Locale.ENGLISH));
            Type enumType = Type.UNKNOWN;
            if (this.isCustomEditions){
                enumType = Type.CUSTOM_SET; // Forcing ThirdParty Edition Type to avoid inconsistencies
            } else {
                String type  = metadata.get("type");
                if (null != type && !type.isEmpty()) {
                    try {
                        enumType = Type.valueOf(type.toUpperCase(Locale.ENGLISH));
                    } catch (IllegalArgumentException ignored) {
                        // ignore; type will get UNKNOWN
                        System.err.println("Ignoring unknown type in set definitions: name: " + res.name + "; type: " + type);
                    }
                }

            }
            res.type = enumType;
            res.prerelease = metadata.get("Prerelease", null);
            res.boosterBoxCount = Integer.parseInt(metadata.get("BoosterBox", enumType.getBoosterBoxDefault()));
            res.fatPackCount = Integer.parseInt(metadata.get("FatPack", enumType.getFatPackDefault()));
            res.fatPackExtraSlots = metadata.get("FatPackExtraSlots", "");

            switch (metadata.get("foil", "newstyle").toLowerCase()) {
                case "oldstyle":
                case "classic":
                    res.foilType = FoilType.OLD_STYLE;
                    break;
                case "newstyle":
                case "modern":
                    res.foilType = FoilType.MODERN;
                    break;
                case "notsupported":
                default:
                    res.foilType = FoilType.NOT_SUPPORTED;
                    break;
            }
            String[] replaceCommon = metadata.get("ChanceReplaceCommonWith", "0F Common").split(" ", 2);
            res.chanceReplaceCommonWith = Double.parseDouble(replaceCommon[0]);
            res.slotReplaceCommonWith = replaceCommon[1];

            res.foilChanceInBooster = metadata.getDouble("FoilChanceInBooster", 21.43F) / 100.0F;

            res.foilAlwaysInCommonSlot = metadata.getBoolean("FoilAlwaysInCommonSlot", true);
            res.additionalSheetForFoils = metadata.get("AdditionalSheetForFoils", "");

            res.additionalUnlockSet = metadata.get("AdditionalSetUnlockedInQuest", ""); // e.g. Time Spiral Timeshifted (TSB) for Time Spiral

            res.smallSetOverride = metadata.getBoolean("TreatAsSmallSet", false); // for "small" sets with over 200 cards (e.g. Eldritch Moon)

            res.boosterMustContain = metadata.get("BoosterMustContain", ""); // e.g. Dominaria guaranteed legendary creature
            res.boosterReplaceSlotFromPrintSheet = metadata.get("BoosterReplaceSlotFromPrintSheet", ""); // e.g. Zendikar Rising guaranteed double-faced card
            res.sheetReplaceCardFromSheet = metadata.get("SheetReplaceCardFromSheet", "");
            res.sheetReplaceCardFromSheet2 = metadata.get("SheetReplaceCardFromSheet2", "");
            res.chaosDraftThemes = metadata.get("ChaosDraftThemes", "").split(";"); // semicolon separated list of theme names

            // Draft options
            String doublePick = metadata.get("DoublePick", "Never");
            int maxPodSize = metadata.getInt("MaxPodSize", 8);
            int recommendedPodSize = metadata.getInt("RecommendedPodSize", 8);
            int maxMatchPlayers = metadata.getInt("MaxMatchPlayers", 2);
            String deckType = metadata.get("DeckType", "Normal");
            String freeCommander = metadata.get("FreeCommander", "");

            res.draftOptions = new DraftOptions(
                    doublePick,
                    maxPodSize,
                    recommendedPodSize,
                    maxMatchPlayers,
                    deckType,
                    freeCommander
            );

            return res;
        }

        @Override
        protected FilenameFilter getFileFilter() {
            return TXT_FILE_FILTER;
        }

        public static final FilenameFilter TXT_FILE_FILTER = (dir, name) -> name.endsWith(".txt");
    }

    public static class Collection extends StorageBase<CardEdition> {
        private final Map<String, CardEdition> aliasToEdition = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        private boolean lock = false; //Lock once custom content has been added.
        public Collection(IItemReader<CardEdition> reader) {
            super("Card editions", reader);

            for (CardEdition ee : this) {
                initAliases(ee);
            }
        }
        private void initAliases(CardEdition E){ //Add the alias to the edition here, to ensure it's always done equally.
            String alias = E.getAlias();
            if (null != alias)
                aliasToEdition.put(alias, E);
            aliasToEdition.put(E.getCode2(), E);
        }
        @Override
        public void add(CardEdition item) { //Even though we want it to be read only, make an exception for custom content.
            if(lock) throw new UnsupportedOperationException("This is a read-only storage");
            else map.put(item.getName(), item);
        }
        public void append(CardEdition.Collection C){ //Append custom editions
            if (lock) throw new UnsupportedOperationException("This is a read-only storage");
            for(CardEdition E : C){ //Update the alias list as above or else it'll fail to look up.
                this.add(E);
                initAliases(E); //Made a method in case the system changes, so it's consistent.
            }
            CardEdition customBucket = new CardEdition("2990-01-01", "USER", "USER",
                    Type.CUSTOM_SET, "USER", FoilType.NOT_SUPPORTED, new EditionEntry[]{});
            this.add(customBucket);
            initAliases(customBucket);
            this.lock = true; //Consider it initialized and prevent from writing any more data.
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

        public Iterable<CardEdition> getPrereleaseEditions() {
            return this.stream()
                    .filter(edition -> edition.getPrerelease() != null)
                    .collect(Collectors.toList());
        }

        public CardEdition getEditionByCodeOrThrow(final String code) {
            final CardEdition set = this.get(code);
            if (null == set && code.equals(UNKNOWN_CODE)) //Hardcoded set ??? is not with the others, needs special check.
                return UNKNOWN;
            if (null == set) {
                throw new RuntimeException("Edition with code '" + code + "' not found");
            }
            return set;
        }

        // used by image generating code
        public String getCode2ByCode(final String code) {
            final CardEdition set = this.get(code);
            return set == null ? "" : set.getCode2();
        }

        public final Comparator<PaperCard> CARD_EDITION_COMPARATOR = Comparator.comparing(c -> Collection.this.get(c.getEdition()));

        public IItemReader<SealedTemplate> getBoosterGenerator() {
            return new StorageReaderBase<SealedTemplate>(null) {
                @Override
                public Map<String, SealedTemplate> readAll() {
                    Map<String, SealedTemplate> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                    for (CardEdition ce : Collection.this) {
                        List<String> boosterTypes = Lists.newArrayList(ce.getAvailableBoosterTypes());
                        for (String type : boosterTypes) {
                            String setAffix = type.equals("Draft") ? "" : type;

                            map.put(ce.getCode() + setAffix, ce.getBoosterTemplate(type));
                        }
                    }
                    return map;
                }

                @Override
                public String getItemKey(SealedTemplate item) {
                    return item.getEdition();
                }

                @Override
                public String getFullPath() {
                    return null;
                }
            };
        }

        /* @leriomaggio
          The original name "getEarliestEditionWithAllCards" was completely misleading, as it did
          not reflect at all what the method really does (and what's the original goal).

          What the method does is to return the **latest** (as in the most recent)
          Card Edition among all the different "Original" sets (as in "first print") were cards
          in the Pool can be found.
          Therefore, nothing to do with an Edition "including" all the cards.
         */
        public CardEdition getTheLatestOfAllTheOriginalEditionsOfCardsIn(CardPool cards) {
            Set<String> minEditions = new HashSet<>();
            CardDb db = StaticData.instance().getCommonCards();
            for (Entry<PaperCard, Integer> k : cards) {
                // NOTE: Even if we do force a very stringent Policy on Editions
                // (which only considers core, expansions, and reprint editions), the fetch method
                // is flexible enough to relax the constraint automatically, if no card can be found
                // under those conditions (i.e. ORIGINAL_ART_ALL_EDITIONS will be automatically used instead).
                PaperCard cp = db.getCardFromEditions(k.getKey().getName(),
                                                      CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY);
                if (cp == null)   // it's unlikely, this code will ever run. Only Happens if card does not exist.
                    cp = k.getKey();
                minEditions.add(cp.getEdition());
            }
            for (CardEdition ed : getOrderedEditions()) {
                if (minEditions.contains(ed.getCode()))
                    return ed;
            }
            return UNKNOWN;
        }
    }

    public static class Predicates {
        public static final Predicate<CardEdition> CAN_MAKE_BOOSTER = CardEdition::hasBoosterTemplate;

        public static CardEdition getRandomSetWithAllBasicLands(Iterable<CardEdition> allEditions) {
            return Aggregates.random(IterableUtil.filter(allEditions, hasBasicLands));
        }

        public static CardEdition getPreferredArtEditionWithAllBasicLands() {
            CardDb.CardArtPreference artPreference = StaticData.instance().getCardArtPreference();
            Iterable<CardEdition> editionsWithBasicLands = IterableUtil.filter(
                    StaticData.instance().getEditions().getOrderedEditions(),
                    hasBasicLands.and(artPreference::accept));
            Iterator<CardEdition> editionsIterator = editionsWithBasicLands.iterator();
            List<CardEdition> selectedEditions = new ArrayList<>();
            while (editionsIterator.hasNext())
                selectedEditions.add(editionsIterator.next());
            if (selectedEditions.isEmpty())
                return null;
            int editionIndex = artPreference.latestFirst ? 0 : selectedEditions.size() - 1;
            return selectedEditions.get(editionIndex);
        }


        public static final Predicate<CardEdition> HAS_TOURNAMENT_PACK = edition -> StaticData.instance().getTournamentPacks().contains(edition.getCode());

        public static final Predicate<CardEdition> HAS_FAT_PACK = edition -> edition.getFatPackCount() > 0;

        public static final Predicate<CardEdition> HAS_BOOSTER_BOX = edition -> edition.getBoosterBoxCount() > 0;

        public static final Predicate<CardEdition> hasBasicLands = ed -> {
            if (ed == null) {
                // Happens for new sets with "???" code
                return false;
            }
            for(String landName : MagicColor.Constant.BASIC_LANDS) {
                if (null == StaticData.instance().getCommonCards().getCard(landName, ed.getCode(), 0))
                    return false;
            }
            return true;
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

    public boolean hasBasicLands() {
        for(String landName : MagicColor.Constant.BASIC_LANDS) {
            if (null == StaticData.instance().getCommonCards().getCard(landName, this.getCode(), 0))
                return false;
        }
        return true;
    }
}
