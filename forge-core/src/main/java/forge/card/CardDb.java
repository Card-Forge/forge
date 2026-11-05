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
import forge.ImageKeys;
import forge.StaticData;
import forge.card.CardEdition.EditionEntry;
import forge.card.CardEdition.Type;
import forge.deck.generation.IDeckGenPool;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.util.Lang;
import forge.util.TextUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class CardDb implements ICardDatabase, IDeckGenPool {
    public final static String foilSuffix = "+";
    public final static char NameSetSeparator = '|';
    public final static String FlagPrefix = "#";
    public static final String FlagSeparator = "\t";
    public static final Comparator<CardRules> CARD_RULES_NAME_COMPARATOR = Comparator.comparing(CardRules::getPreInitName, String.CASE_INSENSITIVE_ORDER);

    // need this to obtain cardReference by name+set+artindex
    // Lazy card loading appends to these maps mid-game, serialized by StaticData's load
    // lock; concurrent map implementations keep lock-free point reads safe against those writes.
    private final ListMultimap<String, PaperCard> allCardsByName = Multimaps.newListMultimap(new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER), Lists::newArrayList);
    private final Map<String, CardRules> rulesByPrimaryName;
    private final Map<String, CardRules> rulesByAltName = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);
    private final ListMultimap<CardRules, PaperCard> allCardsByRules = Multimaps.newListMultimap(new ConcurrentSkipListMap<>(CARD_RULES_NAME_COMPARATOR), Lists::newArrayList);
    private final Map<CardRules, PaperCard> uniqueCardsByRules = new ConcurrentSkipListMap<>(CARD_RULES_NAME_COMPARATOR);
    private final Map<String, ICardFace> facesByName = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, String> normalizedNames = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);
    private static final Map<String, String> artPrefs = Maps.newHashMap();
    /**
     * Map of flavor names to the identifier of the functional variant on which they appear in their respective card rules.
     */
    private final Map<String, String> flavorNameMappings = new ConcurrentHashMap<>();
    private final Map<String, PaperCard> uniqueCardsByFlavorName = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);

    private final Map<String, Integer> artIds = Maps.newHashMap();

    private final CardEdition.Collection editions;
    private final Set<String> filtered;

    private final Map<String, Boolean> nonLegendaryCreatureNames = Maps.newHashMap();

    public enum CardArtPreference implements Comparator<CardEdition> {
        LATEST_ART_ALL_EDITIONS(false, true, false),
        LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY(true, true, false),
        ORIGINAL_ART_ALL_EDITIONS(false, false, false),
        ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY(true, false, false),
        /** Oldest print that is not white-bordered (falls back to any print if a card only exists with another border). */
        ORIGINAL_ART_BLACK_BORDER_ALL_EDITIONS(false, false, true),
        ORIGINAL_ART_BLACK_BORDER_CORE_EXPANSIONS_REPRINT_ONLY(true, false, true);

        public final boolean filterSets;
        public final boolean latestFirst;
        public final boolean blackBorderOnly;

        CardArtPreference(boolean filterIrregularSets, boolean latestSetFirst, boolean blackBorder) {
            filterSets = filterIrregularSets;
            latestFirst = latestSetFirst;
            blackBorderOnly = blackBorder;
        }

        private static final EnumSet<Type> ALLOWED_SET_TYPES = EnumSet.of(Type.CORE, Type.EXPANSION, Type.REPRINT);

        public static CardArtPreference of(boolean latest, boolean coreExpansionOnly, boolean blackBorder) {
            if (latest)
                return coreExpansionOnly ? LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY : LATEST_ART_ALL_EDITIONS;
            if (blackBorder)
                return coreExpansionOnly ? ORIGINAL_ART_BLACK_BORDER_CORE_EXPANSIONS_REPRINT_ONLY : ORIGINAL_ART_BLACK_BORDER_ALL_EDITIONS;
            return coreExpansionOnly ? ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY : ORIGINAL_ART_ALL_EDITIONS;
        }

        private boolean isBlackBordered(CardEdition ed) {
            return ed.getBorderColor() == CardEdition.BorderColor.BLACK;
        }

        /** Whether the set type of the edition is allowed by this preference (Core, Expansion and Reprint sets only for the filtered ones). */
        public boolean acceptsSetType(CardEdition ed) {
            return ed != null && (!filterSets || ALLOWED_SET_TYPES.contains(ed.getType()));
        }

        public boolean accept(CardEdition ed) {
            if (ed == null) return false;
            if (blackBorderOnly && !isBlackBordered(ed)) return false;
            return !filterSets || ALLOWED_SET_TYPES.contains(ed.getType());
        }

        /** Edition rank (see {@link #getEditionRank}): the original Core and Expansion releases. */
        public static final int RANK_ORIGINAL = 0;
        /** Edition rank: any other regular product (Draft sets, Commander decks, Starter sets, ...). */
        public static final int RANK_OTHER = 1;
        /** Edition rank: promos and compilation sets, only used when nothing better exists. */
        public static final int RANK_COMPILATION = 2;

        /** Set types that are promos or compilations of cards printed elsewhere, rather than original releases. */
        private static final EnumSet<Type> COMPILATION_SET_TYPES = EnumSet.of(Type.REPRINT, Type.PROMO,
                Type.COLLECTOR_EDITION, Type.ONLINE, Type.BOXED_SET, Type.DUEL_DECK);

        /**
         * How "original" an edition is, used by the Original Art (Black Border) preferences, where a lower rank
         * always wins over the release date: Core and Expansion sets first, then any other regular product,
         * and promos and compilation sets (The List, Masters sets, MTGO promos, Secret Lair...) last.
         * Other preferences do not rank editions: every edition gets <code>RANK_ORIGINAL</code>.
         */
        public int getEditionRank(CardEdition ed) {
            if (!blackBorderOnly || ed == null)
                return RANK_ORIGINAL;
            Type type = ed.getType();
            if (type == Type.CORE || type == Type.EXPANSION)
                return RANK_ORIGINAL;
            return COMPILATION_SET_TYPES.contains(type) ? RANK_COMPILATION : RANK_OTHER;
        }

        @Override
        public int compare(CardEdition o1, CardEdition o2) {
            if (o1 == o2)
                return 0;
            if(filterSets && (ALLOWED_SET_TYPES.contains(o1.getType()) != ALLOWED_SET_TYPES.contains(o2.getType())))
                return ALLOWED_SET_TYPES.contains(o1.getType()) ? -1 : 1;
            if (blackBorderOnly && isBlackBordered(o1) != isBlackBordered(o2))
                return isBlackBordered(o1) ? -1 : 1;
            int rankDiff = Integer.compare(getEditionRank(o1), getEditionRank(o2));
            if (rankDiff != 0)
                return rankDiff;
            return (latestFirst ? -1 : 1) * o1.getDate().compareTo(o2.getDate());
        }
    }

    // Placeholder to setup default art Preference - to be moved from Static Data!
    private CardArtPreference defaultCardArtPreference;
    private BiPredicate<String, String> preferredLanguageAvailability;
    // (edition, collector number) of prints to avoid when the card has another print
    private BiPredicate<CardEdition, String> printExclusion;
    private int printExclusionVersion;
    private boolean initialized;

    public static class CardRequest {
        public String cardName;
        public String edition;
        public int artIndex;
        public boolean isFoil;
        public String collectorNumber;
        public Map<String, String> flags;

        private CardRequest(String name, String edition, int artIndex, boolean isFoil, String collectorNumber, Map<String, String> flags) {
            cardName = name;
            this.edition = edition;
            this.artIndex = artIndex;
            this.isFoil = isFoil;
            this.collectorNumber = collectorNumber;
            this.flags = flags;
        }

        public static boolean isFoilCardName(final String cardName){
            return cardName.trim().endsWith(foilSuffix);
        }

        public static String compose(String cardName, boolean isFoil){
            if (isFoil){
                return isFoilCardName(cardName) ? cardName : cardName+foilSuffix;
            }
            return isFoilCardName(cardName) ? cardName.substring(0, cardName.length() - foilSuffix.length()) : cardName;
        }

        public static String compose(String cardName, String setCode) {
            if(setCode == null || StringUtils.isBlank(setCode) || setCode.equals(CardEdition.UNKNOWN_CODE))
                setCode = "";
            cardName = cardName != null ? cardName : "";
            if (cardName.indexOf(NameSetSeparator) != -1)
                // If cardName is another RequestString, just get card name and forget about the rest.
                cardName = CardRequest.fromString(cardName).cardName;
            return cardName + NameSetSeparator + setCode;
        }

        public static String compose(String cardName, String setCode, int artIndex) {
            String requestInfo = compose(cardName, setCode);
            artIndex = Math.max(artIndex, IPaperCard.DEFAULT_ART_INDEX);
            return requestInfo + NameSetSeparator + artIndex;
        }

        public static String compose(String cardName, String setCode, String collectorNumber) {
            String requestInfo = compose(cardName, setCode);
            // CollectorNumber will be wrapped in square brackets
            collectorNumber = preprocessCollectorNumber(collectorNumber);
            return requestInfo + NameSetSeparator + collectorNumber;
        }

        public static String compose(String cardName, String setCode, int artIndex, Map<String, String> flags) {
            String requestInfo = compose(cardName, setCode);
            artIndex = Math.max(artIndex, IPaperCard.DEFAULT_ART_INDEX);
            if(flags == null)
                return requestInfo + NameSetSeparator + artIndex;
            return requestInfo + NameSetSeparator + artIndex + getFlagSegment(flags);
        }

        public static String compose(String cardName, String setCode, String collectorNumber, Map<String, String> flags) {
            String requestInfo = compose(cardName, setCode);
            collectorNumber = preprocessCollectorNumber(collectorNumber);
            if(flags == null || flags.isEmpty())
                return requestInfo + NameSetSeparator + collectorNumber;
            return requestInfo + NameSetSeparator + collectorNumber + getFlagSegment(flags);
        }

        public static String compose(PaperCard card) {
            String name = compose(card.getName(), card.isFoil());
            return compose(name, card.getEdition(), card.getCollectorNumber(), card.getMarkedFlags().toMap());
        }

        public static String compose(String cardName, String setCode, int artIndex, String collectorNumber) {
            String requestInfo = compose(cardName, setCode, artIndex);
            // CollectorNumber will be wrapped in square brackets
            collectorNumber = preprocessCollectorNumber(collectorNumber);
            return requestInfo + NameSetSeparator + collectorNumber;
        }

        private static String preprocessCollectorNumber(String collectorNumber) {
            if (collectorNumber == null)
                return "";
            collectorNumber = collectorNumber.trim();
            if (!collectorNumber.startsWith("["))
                collectorNumber = "[" + collectorNumber;
            if (!collectorNumber.endsWith("]"))
                collectorNumber += "]";
            return collectorNumber;
        }

        private static String getFlagSegment(Map<String, String> flags) {
            if(flags == null)
                return "";
            String flagText = flags.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(FlagSeparator));
            return NameSetSeparator + FlagPrefix + "{" + flagText + "}";
        }

        private static boolean isCollectorNumber(String s) {
            return s.startsWith("[") && s.endsWith("]");
        }

        private static boolean isFlagSegment(String s) {
            return s.startsWith(FlagPrefix);
        }

        private static boolean isArtIndex(String s) {
            return StringUtils.isNumeric(s) && s.length() <= 2; // only artIndex between 1-99
        }

        private static boolean isSetCode(String s) {
            return !StringUtils.isNumeric(s);
        }

        private static CardRequest fromPreferredArtEntry(String preferredArt, boolean isFoil){
            // Preferred Art Entry are supposed to be cardName|setCode|artIndex only
            String[] info = TextUtil.split(preferredArt, NameSetSeparator);
            if (info.length != 3)
                return null;
            try {
                String cardName = info[0];
                String setCode = info[1];
                int artIndex = Integer.parseInt(info[2]);
                return new CardRequest(cardName, setCode, artIndex, isFoil, IPaperCard.NO_COLLECTOR_NUMBER, null);
            } catch (NumberFormatException ex){ return null; }
        }

        public static CardRequest fromString(String reqInfo) {
            if (reqInfo == null)
                return null;

            String[] info = TextUtil.split(reqInfo, NameSetSeparator);
            int index = 1;
            String cardName = info[0];
            boolean isFoil = false;
            int artIndex = IPaperCard.NO_ART_INDEX;
            String setCode = null;
            String collectorNumber = IPaperCard.NO_COLLECTOR_NUMBER;
            Map<String, String> flags = null;
            if (isFoilCardName(cardName)) {
                cardName = cardName.substring(0, cardName.length() - foilSuffix.length());
                isFoil = true;
            }

            if(info.length > index && isSetCode(info[index])) {
                setCode = info[index];
                index++;
            }
            if(info.length > index && isArtIndex(info[index].replace(ImageKeys.BACKFACE_POSTFIX, ""))) {
                artIndex = Integer.parseInt(info[index].replace(ImageKeys.BACKFACE_POSTFIX, ""));
                index++;
            }
            if(info.length > index && isCollectorNumber(info[index])) {
                collectorNumber = info[index].substring(1, info[index].length() - 1);
                index++;
            }
            if (info.length > index && isFlagSegment(info[index])) {
                String flagText = info[index].substring(FlagPrefix.length());
                flags = parseRequestFlags(flagText);
            }

            if (CardEdition.UNKNOWN_CODE.equals(setCode)) {  // ???
                setCode = null;
            }
            if (setCode == null) {
                String preferredArt = artPrefs.get(cardName);
                if (preferredArt != null) { //account for preferred art if needed
                    CardRequest request = fromPreferredArtEntry(preferredArt, isFoil);
                    if (request != null)  // otherwise, simply discard it and go on.
                        return request;
                    System.err.printf("[LOG]: Faulty Entry in Preferred Art for Card %s - Please check!%n", cardName);
                }
            }
            // finally, check whether any between artIndex and CollectorNumber has been set
            if (collectorNumber.equals(IPaperCard.NO_COLLECTOR_NUMBER) && artIndex == IPaperCard.NO_ART_INDEX)
                artIndex = IPaperCard.DEFAULT_ART_INDEX;
            return new CardRequest(cardName, setCode, artIndex, isFoil, collectorNumber, flags);
        }

        private static Map<String, String> parseRequestFlags(String flagText) {
            flagText = flagText.trim();
            if(flagText.isEmpty())
                return null;
            if(!flagText.startsWith("{")) {
                //Legacy form for marked colors. They'll be of the form "W#B#R"
                Map<String, String> flags = new HashMap<>();
                String normalizedColorString = ColorSet.fromNames(flagText.split(FlagPrefix)).toString();
                flags.put("markedColors", String.join("", normalizedColorString));
                return flags;
            }
            flagText = flagText.substring(1, flagText.length() - 1); //Trim the braces.
            //List of flags, a series of "key=value" text broken up by tabs.
            return Arrays.stream(flagText.split(FlagSeparator))
                    .map(f -> f.split("=", 2))
                    .filter(f -> f.length > 0)
                    .collect(Collectors.toMap(
                            entry -> entry[0],
                            entry -> entry.length > 1 ? entry[1] : "true" //If there's no '=' in the entry, treat it as a boolean flag.
                    ));
        }
    }

    public CardDb(Map<String, CardRules> rules, CardEdition.Collection editions0, Set<String> filteredCards) {
        this.filtered = filteredCards;
        this.rulesByPrimaryName = rules;
        this.editions = editions0;

        List<CardRules> needsPlaceholderFaces = new ArrayList<>();

        // create faces list from rules
        for (final CardRules rule : rules.values()) {
            // Collect placeholder-face rules unconditionally so they get supplied
            // even when filtered out and remain reachable via rulesByName.
            if (rule.hasPlaceholderFaces()) {
                needsPlaceholderFaces.add(rule);
            }
            if (filteredCards.contains(rule.getPreInitName()))
                continue;
            for (ICardFace face : rule.getAllFaces()) {
                addFaceToDbNames(face, rule);
            }
            if (rule.hasFunctionalVariants()) {
                cacheRuleFlavorNames(rule);
            }
        }

        //Fill in the missing faces for cards that use other cards as one of their faces.
        for(CardRules rule : needsPlaceholderFaces) {
            rule.supplyPlaceholderFaces(this.facesByName);
        }
    }

    private void addFaceToDbNames(ICardFace face, CardRules rules) {
        if (face == null) {
            return;
        }
        final String name = face.getName();
        facesByName.put(name, face);
        //Stash names of alternate faces for loose name-to-rules lookups. Technically ambiguous; first come, first served.
        if (!rulesByPrimaryName.containsKey(name))
            rulesByAltName.putIfAbsent(name, rules);
        final String normalName = StringUtils.stripAccents(name);
        if (!normalName.equals(name)) {
            normalizedNames.put(normalName, name);
        }

        if (face.hasFunctionalVariants()) {
            for (ICardFace varFace : face.getFunctionalVariants().values())
                cacheFaceFlavorName(varFace);
        }
        if (face.getFlavorName() != null) //Probably shouldn't be putting a flavor name on the main print?
            cacheFaceFlavorName(face);
    }

    private void cacheFaceFlavorName(ICardFace face) {
        String altName = face.getFlavorName();
        if(altName == null)
            return;
        facesByName.putIfAbsent(altName, face);
        final String normalAltName = StringUtils.stripAccents(altName);
        if (!normalAltName.equals(altName)) {
            normalizedNames.put(normalAltName, altName);
        }
    }

    private void cacheRuleFlavorNames(CardRules rules) {
        if (rules.getSupportedFunctionalVariants() == null)
            return;
        boolean hasFlavorName = false;
        String baseName = rules.getName();
        for (String variantName : rules.getSupportedFunctionalVariants()) {
            String name = rules.getDisplayNameForVariant(variantName);
            if (baseName.equals(name))
                continue;
            hasFlavorName = true;
            rulesByAltName.put(name, rules);
            flavorNameMappings.put(name, variantName);
        }
        if (hasFlavorName)
            flavorNameMappings.put(baseName, IPaperCard.NO_FUNCTIONAL_VARIANT);
    }

    private void addSetCard(CardEdition e, EditionEntry cis, CardRules cr) {
        int artIdx = IPaperCard.DEFAULT_ART_INDEX;
        String key = e.getCode() + "/" + cis.name();
        if (artIds.containsKey(key)) {
            artIdx = artIds.get(key) + 1;
        }
        artIds.put(key, artIdx);

        String variantName = cis.getFunctionalVariantName();
        String flavorName = cis.getFlavorName();
        assert(variantName == null || flavorName == null); //Can't currently assign both this way.

        if (variantName == null && !cr.getName().equals(cis.name())) {
            //If an edition entry uses a known flavor name without specifying the variant, swap to that variant.
            variantName = flavorNameMappings.get(cis.name());
            //System.out.printf("Auto-mapping flavor name \"%s\" -> \"%s\" $%s\n", cis.name(), cr.getName(), variantName);
        }
        if (flavorName != null) {
            String suggestedFlavorName = e.getCode().startsWith("OM") ? "Alchemy"
                    : e.getCode().equals("SLX") ? "UniversesWithin"
                    : null;
            variantName = cr.findOrCreateVariantForFlavorName(flavorName, suggestedFlavorName);
            String normalizedFlavorName = cr.getDisplayNameForVariant(variantName);
            if(!flavorNameMappings.containsKey(normalizedFlavorName)) {
                flavorNameMappings.put(normalizedFlavorName, variantName);
                flavorNameMappings.put(cr.getName(), IPaperCard.NO_FUNCTIONAL_VARIANT);
                rulesByAltName.put(normalizedFlavorName, cr);
                cacheFaceFlavorName(cr.getMainPart().getFunctionalVariant(variantName));
                if(cr.getOtherPart() != null)
                    cacheFaceFlavorName(cr.getOtherPart().getFunctionalVariant(variantName));
            }
        }

        addCard(new PaperCard(cr, e.getCode(), cis.rarity(), artIdx, false, cis.collectorNumber(), cis.artistName(), variantName));
    }

    private boolean addFromSetByName(String cardName, CardEdition ed, CardRules cr) {
        List<EditionEntry> cardsInSet = ed.getCardInSet(cardName);
        if (cr.hasFunctionalVariants()) {
            cardsInSet = cardsInSet.stream().filter(c -> StringUtils.isEmpty(c.getFunctionalVariantName())
                    || cr.getSupportedFunctionalVariants().contains(c.getFunctionalVariantName())
            ).collect(Collectors.toList());
        }
        if (cardsInSet.isEmpty())
            return false;
        boolean added = false;
        for (EditionEntry cis : cardsInSet) {
            if (containsPrinting(cr, ed.getCode(), cis.collectorNumber())) {
                continue;
            }
            addSetCard(ed, cis, cr);
            added = true;
        }
        return added;
    }

    private boolean containsPrinting(CardRules cr, String setCode, String collectorNumber) {
        for (PaperCard pc : allCardsByRules.get(cr)) {
            if (pc.getEdition().equals(setCode) && pc.getCollectorNumber().equals(collectorNumber)) {
                return true;
            }
        }
        return false;
    }

    // Only safe while no game is reading this db; callers guarantee quiescence.
    public void clearLoadedCards() {
        allCardsByName.clear();
        allCardsByRules.clear();
        uniqueCardsByRules.clear();
        uniqueCardsByFlavorName.clear();
        facesByName.clear();
        rulesByAltName.clear();
        rulesByPrimaryName.clear();
        normalizedNames.clear();
        flavorNameMappings.clear();
        artIds.clear();
        nonLegendaryCreatureNames.clear();
    }

    public void loadCard(String cardName, String setCode, CardRules cr) {
        // @leriomaggio: This method is called when lazy-loading is set
        // OR if a card is trying to load from an edition its not from
        //System.out.println("[LOG]: (Lazy) Loading Card: " + cardName);
        if (cr.hasPlaceholderFaces()) {
            try {
                cr.supplyPlaceholderFaces(facesByName);
            } catch (NoSuchElementException e) {
                e.printStackTrace();
                return;
            }
        }
        if (rulesByPrimaryName.putIfAbsent(cr.getName(), cr) == null
                && !filtered.contains(cr.getPreInitName())) {
            for (ICardFace face : cr.getAllFaces()) {
                addFaceToDbNames(face, cr);
            }
            if (cr.hasFunctionalVariants()) {
                cacheRuleFlavorNames(cr);
            }
        }
        boolean reIndexNecessary = false;
        CardEdition ed = editions.get(setCode);
        if (ed == null || ed.equals(CardEdition.UNKNOWN)) {
            // look for all possible editions
            for (CardEdition e : editions) {
                reIndexNecessary |= addFromSetByAnyName(cardName, e, cr);
            }
        } else {
            reIndexNecessary |= addFromSetByAnyName(cardName, ed, cr);
        }

        if (reIndexNecessary) {
            reIndex(cr);
        }
    }

    private void reIndex(CardRules cr) {
        List<PaperCard> prints = allCardsByRules.get(cr);
        if (!prints.isEmpty()) {
            PaperCard best = getBestUniquePrint(prints);
            String preferredRequest = artPrefs.get(cr.getName());
            if (preferredRequest != null) {
                PaperCard preferred = this.getCard(preferredRequest);
                if (preferred != null && preferred.getRules().equals(cr))
                    best = preferred;
            }
            uniqueCardsByRules.put(cr, best);
        }
        if (!cr.hasFunctionalVariants() || cr.getSupportedFunctionalVariants() == null) {
            return;
        }
        for (String variantName : cr.getSupportedFunctionalVariants()) {
            String flavorName = cr.getDisplayNameForVariant(variantName);
            if (flavorName == null || flavorName.equals(cr.getName())) {
                continue;
            }
            String variant = flavorNameMappings.get(flavorName);
            if (variant == null || variant.equals(IPaperCard.NO_FUNCTIONAL_VARIANT)) {
                continue;
            }
            List<PaperCard> flavorPrints = allCardsByName.get(flavorName);
            if (!flavorPrints.isEmpty()) {
                uniqueCardsByFlavorName.put(flavorName, getBestUniquePrint(flavorPrints));
            }
        }
    }

    private boolean addFromSetByAnyName(String cardName, CardEdition ed, CardRules cr) {
        boolean added = addFromSetByName(cr.getName(), ed, cr);
        if (!cardName.equalsIgnoreCase(cr.getName())) {
            added |= addFromSetByName(cardName, ed, cr);
        }
        if (cr.hasFunctionalVariants() && cr.getSupportedFunctionalVariants() != null) {
            for (String variantName : cr.getSupportedFunctionalVariants()) {
                String flavorName = cr.getDisplayNameForVariant(variantName);
                if (flavorName != null && !flavorName.equalsIgnoreCase(cr.getName()) && !flavorName.equalsIgnoreCase(cardName)) {
                    added |= addFromSetByName(flavorName, ed, cr);
                }
            }
        }
        return added;
    }

    public void initialize(boolean logMissingPerEdition, boolean logMissingSummary, boolean enableUnknownCards) {
        Set<String> allMissingCards = new LinkedHashSet<>();
        List<String> missingCards = new ArrayList<>();
        CardEdition upcomingSet = null;
        Date today = new Date();

        for (CardEdition e : editions.getOrderedEditions()) {
            boolean coreOrExpSet = e.getType() == CardEdition.Type.CORE || e.getType() == CardEdition.Type.EXPANSION;
            boolean isCoreExpSet = coreOrExpSet || e.getType() == CardEdition.Type.REPRINT;
            if (logMissingPerEdition && isCoreExpSet) {
                System.out.print(e.getName() + " (" + e.getAllCardsInSet().size() + " cards)");
            }
            if (coreOrExpSet && e.getDate().after(today) && upcomingSet == null) {
                upcomingSet = e;
            }

            for (CardEdition.EditionEntry cis : e.getAllCardsInSet()) {
                CardRules cr = rulesByPrimaryName.get(cis.name());
                if (cr == null)
                    cr = rulesByAltName.get(cis.name()); //Entry written using a flavor name
                if (cr == null) {
                    missingCards.add(cis.name());
                    continue;
                }
                if (cr.hasFunctionalVariants()) {
                    if (StringUtils.isNotEmpty(cis.getFunctionalVariantName())
                        && !cr.getSupportedFunctionalVariants().contains(cis.getFunctionalVariantName())) {
                        //Supported card, unsupported variant.
                        //Could note the card as missing but since these are often un-cards,
                        //it's likely absent because it does something out of scope.
                        continue;
                    }
                }
                addSetCard(e, cis, cr);
            }
            if (isCoreExpSet && logMissingPerEdition) {
                if (missingCards.isEmpty()) {
                    System.out.println(" ... 100% ");
                } else {
                    int missing = (e.getAllCardsInSet().size() - missingCards.size()) * 10000 / e.getAllCardsInSet().size();
                    System.out.printf(" ... %.2f%% (%s missing: %s)%n", missing * 0.01f, Lang.nounWithAmount(missingCards.size(), "card"), StringUtils.join(missingCards, " | "));
                }
            }
            if (isCoreExpSet && logMissingSummary) {
                allMissingCards.addAll(missingCards);
            }
            missingCards.clear();
            artIds.clear();
        }

        if (logMissingSummary) {
            System.out.printf("Totally %d cards not implemented: %s\n", allMissingCards.size(), StringUtils.join(allMissingCards, " | "));
        }

        addUnassignedCardPrints(enableUnknownCards, upcomingSet);

        initialized = true;
        reIndex();
    }

    public void addUnassignedCardPrints(boolean enableUnknownCards) {
        CardEdition upcomingSet = null;
        Date today = new Date();
        for (CardEdition e : editions.getOrderedEditions()) {
            boolean coreOrExpSet = e.getType() == CardEdition.Type.CORE || e.getType() == CardEdition.Type.EXPANSION;
            if (coreOrExpSet && e.getDate().after(today)) {
                upcomingSet = e;
                break;
            }
        }
        addUnassignedCardPrints(enableUnknownCards, upcomingSet);
    }

    private void addUnassignedCardPrints(boolean enableUnknownCards, CardEdition upcomingSet) {
        if (upcomingSet != null) {
            System.err.println("Upcoming set " + upcomingSet + " dated in the future. All `upcoming` cards will be added to this set with unknown rarity.");
        }

        for (CardRules cr : rulesByPrimaryName.values()) {
            if (!contains(cr.getName())) {
                if (!cr.isCustom()) {
                    if (upcomingSet != null && cr.getPath() != null && cr.getPath().contains("upcoming/")) {
                        addCard(new PaperCard(cr, upcomingSet.getCode(), CardRarity.Unknown));
                        reIndex(cr);
                    } else if (enableUnknownCards && !this.filtered.contains(cr.getName())) {
                        System.err.println("The card " + cr.getName() + " was not assigned to any set. Adding it to UNKNOWN set... to fix see res/editions/ folder. ");
                        addCard(new PaperCard(cr, CardEdition.UNKNOWN_CODE, CardRarity.Special));
                        reIndex(cr);
                    }
                } else {
                    System.err.println("The custom card " + cr.getName() + " was not assigned to any set. Adding it to custom USER set, and will try to load custom art from USER edition.");
                    addCard(new PaperCard(cr, "USER", CardRarity.Special));
                    reIndex(cr);
                }
            }
        }
    }

    public void addCard(PaperCard paperCard) {
        if (filtered.contains(paperCard.getName())) {
            return;
        }

        String mainName = paperCard.getName();
        allCardsByName.put(mainName, paperCard);

        CardRules rules = paperCard.getRules();
        allCardsByRules.put(rules, paperCard);
        if (rules.getSplitType() == CardSplitType.None && !rules.hasFunctionalVariants()) {
            return;
        }

        //Card may have multiple names. Add it under all of them.

        List<ICardFace> allFaces = paperCard.getAllFaces();
        Set<String> namesToAdd = new HashSet<>();
        allFaces.stream().map(ICardCharacteristics::getName).forEach(namesToAdd::add);
        allFaces.stream().map(ICardFace::getFlavorName).filter(Objects::nonNull).forEach(namesToAdd::add);
        namesToAdd.remove(mainName);
        for(String name : namesToAdd)
            allCardsByName.put(name, paperCard);
    }

    private void reIndex() {
        uniqueCardsByRules.clear();
        uniqueCardsByFlavorName.clear();
        for (Entry<CardRules, Collection<PaperCard>> kv : allCardsByRules.asMap().entrySet()) {
            PaperCard pc = getBestUniquePrint(kv.getValue());
            uniqueCardsByRules.put(kv.getKey(), pc);
        }
        for (Entry<String, String> kv : flavorNameMappings.entrySet()) {
            if (kv.getValue().equals(IPaperCard.NO_FUNCTIONAL_VARIANT))
                continue;
            String flavorName = kv.getKey();
            PaperCard pc = getBestUniquePrint(allCardsByName.get(flavorName));
            uniqueCardsByFlavorName.put(flavorName, pc);
        }
        restorePreferredArts();
    }

    /**
     * Re-apply the per-card preferred art (chosen by the user) on top of the unique prints index,
     * so that it always wins over the default Card Art Preference after the index is rebuilt.
     */
    private void restorePreferredArts() {
        for (String preferredRequest : new ArrayList<>(artPrefs.values())) {
            if (preferredRequest == null)
                continue;
            PaperCard pc = this.getCard(preferredRequest);
            if (pc != null && allCardsByRules.containsKey(pc.getRules()))
                uniqueCardsByRules.put(pc.getRules(), pc);
        }
    }

    /**
     * Card art chosen by preference should have the traditional frame. If the given print is a special version
     * (borderless, extended art, showcase, etched, promo...) and the same card also has a print in the main
     * "cards" section of the same edition, that print is returned instead. When there are several traditional
     * arts (e.g. basic lands) the one at the same position as the art index is used, so the variety is kept.
     */
    public PaperCard getTraditionalFramePrint(PaperCard pc) {
        if (pc == null || isTraditionalFramePrint(pc))
            return pc;
        List<PaperCard> traditional = getTraditionalFramePrints(pc);
        if (traditional.isEmpty())
            return pc;  // the card has no traditional print in this edition
        if (hasPrintExclusion() && !isPrintExcluded(pc) && traditional.stream().allMatch(this::isPrintExcluded))
            return pc;  // the frame never wins over the player's exclusion
        PaperCard result = traditional.get((Math.max(pc.getArtIndex(), 1) - 1) % traditional.size());
        return pc.isFoil() ? result.getFoiled() : result;
    }

    /**
     * All the traditional frame prints of the same card (and functional variant) in the edition of the given
     * print, ordered by art index. It is empty when the card has no print in the main "cards" section of
     * its edition, for example when the whole edition is made of special prints.
     */
    public List<PaperCard> getTraditionalFramePrints(PaperCard pc) {
        if (pc == null)
            return Collections.emptyList();
        CardEdition ed = editions.get(pc.getEdition());
        if (ed == null)
            return Collections.emptyList();
        List<PaperCard> traditional = new ArrayList<>();
        for (PaperCard other : allCardsByRules.get(pc.getRules())) {
            if (other.getEdition().equals(pc.getEdition()) && ed.isTraditionalFrame(other.getCollectorNumber())
                    && Objects.equals(other.getFunctionalVariant(), pc.getFunctionalVariant()))
                traditional.add(other);
        }
        if (canExcludePrints(traditional))
            traditional.removeIf(this::isPrintExcluded);
        if (!pc.getRules().getType().isBasicLand()) {
            /* Some products (Universes Beyond, Secret Lair, Commander decks...) list the special versions of a card
               in the main section too, with much higher collector numbers (e.g. 79 / 171 / 265). The standard
               print is the lowest one. Letter variants of the same number (38a, 38b...) are different arts of the
               same frame and are kept, and so are all the arts of basic lands. */
            int lowest = Integer.MAX_VALUE;
            for (PaperCard other : traditional)
                lowest = Math.min(lowest, collectorNumberBase(other.getCollectorNumber()));
            final int standardNumber = lowest;
            traditional.removeIf(other -> {
                int number = collectorNumberBase(other.getCollectorNumber());
                return number != Integer.MAX_VALUE && number != standardNumber;
            });
        }
        traditional.sort(Comparator.comparingInt(PaperCard::getArtIndex));
        return traditional;
    }

    /* The number at the start of a collector number ("38b" -> 38), or MAX_VALUE if it does not start with digits. */
    private static int collectorNumberBase(String collectorNumber) {
        if (collectorNumber == null)
            return Integer.MAX_VALUE;
        int end = 0;
        while (end < collectorNumber.length() && end < 9 && Character.isDigit(collectorNumber.charAt(end)))
            end++;
        return end == 0 ? Integer.MAX_VALUE : Integer.parseInt(collectorNumber.substring(0, end));
    }

    public boolean isTraditionalFramePrint(PaperCard pc) {
        CardEdition ed = editions.get(pc.getEdition());
        if (ed == null || !ed.isTraditionalFrame(pc.getCollectorNumber()))
            return false;
        if (pc.getRules().getType().isBasicLand())
            return true;
        for (PaperCard traditional : getTraditionalFramePrints(pc))
            if (traditional.getCollectorNumber().equalsIgnoreCase(pc.getCollectorNumber()))
                return true;
        return false;
    }

    private PaperCard getBestUniquePrint(final Collection<PaperCard> prints) {
        final Collection<PaperCard> cards = canExcludePrints(prints)
                ? prints.stream().filter(pc -> !isPrintExcluded(pc)).collect(Collectors.toList())
                : prints;
        return cards.stream()
                .filter(pc -> !pc.getRarity().equals(CardRarity.Special))
                // A Black Border preference is a hard requirement: a translated white-bordered print must not
                // beat a black-bordered one. The preferred language only decides among equally acceptable borders.
                .min(Comparator.comparing((PaperCard pc) -> isBorderMismatch(pc) ? 1 : 0)
                        .thenComparing((PaperCard pc) -> isPreferredLanguagePrint(pc) ? 0 : 1)
                        .thenComparing((PaperCard pc) -> editions.get(pc.getEdition()), defaultCardArtPreference)
                        .thenComparing((PaperCard pc) -> isTraditionalFramePrint(pc) ? 0 : 1)
                        .thenComparing(PaperCard::getCollectorNumber))
                .orElseGet(() -> cards.iterator().next());
    }

    private boolean isBorderMismatch(PaperCard pc) {
        if (defaultCardArtPreference == null || !defaultCardArtPreference.blackBorderOnly)
            return false;
        CardEdition edition = editions.get(pc.getEdition());
        return edition != null && edition.getBorderColor() != CardEdition.BorderColor.BLACK;
    }

    private boolean isPreferredLanguagePrint(PaperCard pc) {
        if (preferredLanguageAvailability == null) {
            return false;
        }
        CardEdition edition = editions.get(pc.getEdition());
        if (edition == null) {
            return false;
        }
        return preferredLanguageAvailability.test(edition.getScryfallCode(), pc.getCollectorNumber());
    }

    public boolean setPreferredArt(String cardName, String setCode, int artIndex) {
        String cardRequestForPreferredArt = CardRequest.compose(cardName, setCode, artIndex);
        PaperCard pc = this.getCard(cardRequestForPreferredArt);
        if (pc != null) {
            artPrefs.put(cardName, cardRequestForPreferredArt);
            uniqueCardsByRules.put(pc.getRules(), pc);
            return true;
        }
        return false;
    }

    public boolean hasPreferredArt(String cardName){
        return artPrefs.getOrDefault(cardName, null) != null;
    }

    @Override
    public CardArtPreference getCardArtPreference(){ return this.defaultCardArtPreference; }
    public void setCardArtPreference(boolean latestArt, boolean coreExpansionOnly){
        setCardArtPreference(latestArt, coreExpansionOnly, false);
    }
    public void setCardArtPreference(boolean latestArt, boolean coreExpansionOnly, boolean blackBorder){
        CardArtPreference newPreference = CardArtPreference.of(latestArt, coreExpansionOnly, blackBorder);
        boolean changed = newPreference != this.defaultCardArtPreference;
        this.defaultCardArtPreference = newPreference;
        // The unique prints index depends on the preference: rebuild it when changed at runtime.
        if (changed && initialized) {
            reIndex();
        }
    }

    public void setPreferredLanguageAvailability(BiPredicate<String, String> availability) {
        this.preferredLanguageAvailability = availability;
        if (initialized) {
            reIndex();
        }
    }

    public void setPrintExclusion(BiPredicate<CardEdition, String> exclusion) {
        if (exclusion == null && printExclusion == null)
            return;  // keeps the version at 0, so decks loaded with nothing excluded never resolve again
        this.printExclusion = exclusion;
        printExclusionVersion++;
        if (initialized) {
            reIndex();
        }
    }

    /** Changes whenever the print exclusion is replaced, so cached print choices can tell they are stale. */
    public int getPrintExclusionVersion() {
        return printExclusionVersion;
    }

    public boolean hasPrintExclusion() {
        return printExclusion != null;
    }

    public boolean isPrintExcluded(PaperCard pc) {
        if (printExclusion == null)
            return false;
        CardEdition edition = editions.get(pc.getEdition());
        return edition != null && printExclusion.test(edition, pc.getCollectorNumber());
    }

    /** True when an exclusion is set and at least one of the prints passes it, so the excluded ones can be dropped. */
    public boolean canExcludePrints(Collection<PaperCard> prints) {
        return printExclusion != null && prints.stream().anyMatch(pc -> !isPrintExcluded(pc));
    }

    /**
     * Retrieves a CardRules matching the provided name.
     * @param allowAltNames If false, the name must be the exact name of the card in its default state. If true, flavor
     *                      names and alternate face names can be used, though an exact match will be preferred.
     * @see #getAllCards(String)
     * @see #getAllCardsNoAlt(String)
     */
    public CardRules getRules(String cardName, boolean allowAltNames) {
        CardRules result = getLoadedRules(cardName, allowAltNames);
        if (result == null && lazyLoad(cardName, null)) {
            result = getLoadedRules(cardName, allowAltNames);
        }
        return result;
    }

    private CardRules getLoadedRules(String cardName, boolean allowAltNames) {
        cardName = getNormalizedName(cardName);
        CardRules result = rulesByPrimaryName.get(cardName);
        if (result != null || !allowAltNames)
            return result;
        return rulesByAltName.get(cardName);
    }

    private boolean lazyLoad(String cardName, String setCode) {
        StaticData data = StaticData.instance();
        return data != null && data.lazyLoadCard(cardName, setCode);
    }

    public CardRules getRulesOrElseUnsupported(String cardName) {
        CardRules rules = getRules(cardName, false);
        if (rules == null)
            return CardRules.getUnsupportedCardNamed(cardName);
        return rules;
    }

    public void setCardArtPreference(String artPreference) {
        artPreference = artPreference.toLowerCase().trim();
        boolean isLatest = artPreference.contains("latest");
        // additional check in case of unrecognised values wrt. to legacy opts
        if (!artPreference.contains("original") && !artPreference.contains("earliest"))
            isLatest = true;  // this must be default
        boolean hasFilter = artPreference.contains("core");
        this.setCardArtPreference(isLatest, hasFilter, artPreference.contains("black"));
    }

    /*
     * ======================
     * 1. CARD LOOKUP METHODS
     * ======================
     */
    @Override
    public PaperCard getCard(String cardName) {
        CardRequest request = CardRequest.fromString(cardName);
        return tryGetCard(request);
    }

    @Override
    public PaperCard getCard(final String cardName, String setCode) {
        CardRequest request = CardRequest.fromString(CardRequest.compose(cardName, setCode));
        return tryGetCard(request);
    }

    @Override
    public PaperCard getCard(final String cardName, String setCode, int artIndex) {
        String reqInfo = CardRequest.compose(cardName, setCode, artIndex);
        CardRequest request = CardRequest.fromString(reqInfo);
        return tryGetCard(request);
    }

    @Override
    public PaperCard getCard(final String cardName, String setCode, String collectorNumber) {
        String reqInfo = CardRequest.compose(cardName, setCode, collectorNumber);
        CardRequest request = CardRequest.fromString(reqInfo);
        return tryGetCard(request);
    }

    @Override
    public PaperCard getCard(final String cardName, String setCode, int artIndex, Map<String, String> flags) {
        String reqInfo = CardRequest.compose(cardName, setCode, artIndex, flags);
        CardRequest request = CardRequest.fromString(reqInfo);
        return tryGetCard(request);
    }

    @Override
    public PaperCard getCard(final String cardName, String setCode, String collectorNumber, Map<String, String> flags) {
        String reqInfo = CardRequest.compose(cardName, setCode, collectorNumber, flags);
        CardRequest request = CardRequest.fromString(reqInfo);
        return tryGetCard(request);
    }

    private PaperCard tryGetCard(CardRequest request) {
        // Before doing anything, check that a non-null request has been provided
        if (request == null)
            return null;
        // 1. First off, try using all possible search parameters, to narrow down the actual cards looked for.
        String reqEditionCode = request.edition;
        if (reqEditionCode != null && !reqEditionCode.isEmpty()) {
            // This get is robust even against expansion aliases (e.g. TE and TMP both valid for Tempest) -
            // MOST of the extensions have two short codes, 141 out of 221 (so far)
            // ALSO: Set Code are always UpperCase
            CardEdition edition = editions.get(reqEditionCode.toUpperCase());

            PaperCard cardFromSet = this.getCardFromSet(request.cardName, edition, request.artIndex, request.collectorNumber, request.isFoil);
            if(cardFromSet != null && request.flags != null)
                cardFromSet = cardFromSet.copyWithFlags(request.flags);

            if (cardFromSet != null)
                return cardFromSet;
        }

        // 2. Card lookup in edition with specified filter didn't work.
        // So now check whether the cards exist in the DB first,
        // and select pick the card based on current SetPreference policy as a fallback
        Collection<PaperCard> cards = getAllCards(request.cardName);
        if (cards.isEmpty())  // Never null being this a view in MultiMap
            return null;
        // Either No Edition has been specified OR as a fallback in case of any error!
        // get card using the default card art preference
        String cardRequest = CardRequest.compose(request.cardName, request.isFoil);
        return getCardFromEditions(cardRequest, this.defaultCardArtPreference, request.artIndex);
    }

    /*
     * ==========================================
     * 2. CARD LOOKUP FROM A SINGLE EXPANSION SET
     * ==========================================
     *
     * NOTE: All these methods always try to return a PaperCard instance
     * that has an Image (if any).
     * Therefore, the single Edition request can be overruled if no image is found
     * for the corresponding requested edition.
     */

    @Override
    public PaperCard getCardFromSet(String cardName, CardEdition edition, int artIndex, String collectorNumber, boolean isFoil) {
        if (edition == null || cardName == null)  // preview cards
            return null;  // No cards will be returned

        // Allow to pass in cardNames with foil markers, and adapt accordingly
        CardRequest cardNameRequest = CardRequest.fromString(cardName);
        cardName = cardNameRequest.cardName;
        isFoil = isFoil || cardNameRequest.isFoil;

        String code1 = edition.getCode(), code2 = edition.getCode2();

        Predicate<PaperCard> filter = (c) -> {
            String ed = c.getEdition();
            return ed.equalsIgnoreCase(code1) || ed.equalsIgnoreCase(code2);
        };
        if (artIndex > 0)
            filter = filter.and((c) -> artIndex == c.getArtIndex());
        if (collectorNumber != null && !collectorNumber.isEmpty() && !collectorNumber.equals(IPaperCard.NO_COLLECTOR_NUMBER))
            filter = filter.and((c) -> collectorNumber.equals(c.getCollectorNumber()));

        List<PaperCard> candidates = getAllCards(cardName, filter);
        if (candidates.isEmpty())
            return null;

        if (candidates.stream().map(PaperCard::getRules).distinct().count() > 1)
        {
            //We've run into either an ambiguous alt-face or an Emeritus situation. Can't do anything for the former,
            //but we can bias towards the main face if it's the latter.
            String finalCardName = cardName;
            if (candidates.stream().map(PaperCard::getName).anyMatch(n -> n.equalsIgnoreCase(finalCardName))) {
                candidates.removeIf(c -> !c.getName().equalsIgnoreCase(finalCardName));
            }
        }

        Iterator<PaperCard> candidatesIterator = candidates.iterator();
        PaperCard candidate = candidatesIterator.next();
        // Before returning make sure that actual candidate has Image.
        // If not, try to replace current candidate with one having image,
        // so to align this implementation with old one.
        // If none will have image, the original candidate will be retained!
        PaperCard firstCandidate = candidate;
        while (!candidate.hasImage() && candidatesIterator.hasNext())
            candidate = candidatesIterator.next();
        candidate = candidate.hasImage() ? candidate : firstCandidate;
        return isFoil ? candidate.getFoiled() : candidate;
    }

    /*
     * ====================================================
     * 3. CARD LOOKUP BASED ON CARD ART PREFERENCE OPTION
     * ====================================================
     */

    @Override
    public PaperCard getCardFromEditions(final String cardInfo, final CardArtPreference artPreference, int artIndex, Predicate<PaperCard> filter) {
        return this.tryToGetCardFromEditions(cardInfo, artPreference, artIndex, filter);
    }

    /*
     * ===============================================
     * 4. SPECIALISED CARD LOOKUP BASED ON
     *    CARD ART PREFERENCE AND EDITION RELEASE DATE
     * ===============================================
     */

    @Override
    public PaperCard getCardFromEditionsReleasedBefore(String cardName, CardArtPreference artPreference, int artIndex, Date releaseDate, Predicate<PaperCard> filter){
        return this.tryToGetCardFromEditions(cardName, artPreference, artIndex, releaseDate, true, filter);
    }

    @Override
    public PaperCard getCardFromEditionsReleasedAfter(String cardName, CardArtPreference artPreference, int artIndex, Date releaseDate, Predicate<PaperCard> filter){
        return this.tryToGetCardFromEditions(cardName, artPreference, artIndex, releaseDate, false, filter);
    }

    // Override when there is no date
    private PaperCard tryToGetCardFromEditions(String cardInfo, CardArtPreference artPreference, int artIndex, Predicate<PaperCard> filter){
        return this.tryToGetCardFromEditions(cardInfo, artPreference, artIndex, null, false, filter);
    }

    private PaperCard tryToGetCardFromEditions(String cardInfo, CardArtPreference artPreference, int artIndex,
                                               Date releaseDate, boolean releasedBeforeFlag, Predicate<PaperCard> filter) {
        if (cardInfo == null)
            return null;
        final CardRequest cr = CardRequest.fromString(cardInfo);
        // Check whether input `frame` is null. In that case, fallback to default SetPreference !-)
        final CardArtPreference artPref = artPreference != null ? artPreference : this.defaultCardArtPreference;
        cr.artIndex = Math.max(cr.artIndex, IPaperCard.DEFAULT_ART_INDEX);
        if (cr.artIndex != artIndex && artIndex > IPaperCard.DEFAULT_ART_INDEX )
            cr.artIndex = artIndex;  // 2nd cond. is to verify that some actual value has been passed in.

        List<PaperCard> cards;
        Predicate<PaperCard> cardQueryFilter;
        filter = filter != null ? filter : (x -> true);
        if (releaseDate != null) {
            cardQueryFilter = c -> {
                if (c.getArtIndex() != cr.artIndex)
                    return false;  // not interested anyway!
                CardEdition ed = editions.get(c.getEdition());
                if (ed == null) return false;
                if (releasedBeforeFlag)
                    return ed.getDate().before(releaseDate);
                else
                    return ed.getDate().after(releaseDate);
            };
        } else  // filter candidates based on requested artIndex
            cardQueryFilter = card -> card.getArtIndex() == cr.artIndex;
        cardQueryFilter = cardQueryFilter.and(filter);

        // Check no-alt cards first. Exact name matches are prioritized more highly than an alt face or half of a split
        // card. That way "Ancestral Recall" won't return Emeritus of Ideation but "Wild Idea" can return Sanar, Unfinished Genius.
        cards = getAllCardsNoAlt(cr.cardName, cardQueryFilter);
        if(cards.isEmpty())
            cards = getAllCards(cr.cardName, cardQueryFilter);

        if (cards.isEmpty())
            return null;
        if (cards.size() == 1)  // if only one candidate, there's not much else we should do
            return preferTraditionalFrame(cards.get(0), cr);

        if (flavorNameMappings.containsKey(cr.cardName)) {
            Collection<PaperCard> matchingNames = cards.stream().filter(c -> c.getDisplayName().equals(cr.cardName)).collect(Collectors.toSet());
            if(!matchingNames.isEmpty())
                cards.retainAll(matchingNames);
        }
        if (canExcludePrints(cards))
            cards.removeIf(this::isPrintExcluded);

        /* 2. Retrieve cards based of [Frame]Set Preference
           ================================================ */
        // Collect the list of all editions found for target card
        List<CardEdition> cardEditions = new ArrayList<>();
        Map<String, PaperCard> candidatesCard = new HashMap<>();
        for (PaperCard card : cards) {
            String setCode = card.getEdition();
            CardEdition ed;
            if (setCode.equals(CardEdition.UNKNOWN_CODE))
                ed = CardEdition.UNKNOWN;
            else
                ed = editions.get(card.getEdition());
            if (ed != null) {
                cardEditions.add(ed);
                candidatesCard.put(setCode, card);
            }
        }
        if (cardEditions.isEmpty())
            return null;  // nothing to do

        // Filter Cards Editions based on set preferences
        List<CardEdition> acceptedEditions = cardEditions.stream().filter(artPref::accept).collect(Collectors.toList());

        /* At this point, it may be possible that Art Preference is too-strict for the requested card!
            i.e. acceptedEditions.size() == 0!
            This may be the case of Cards Only available in NON-CORE/EXPANSIONS/REPRINT sets.
            (NOTE: We've already checked that any print of the request card exists in the DB)
            If this happens, we won't try to iterate over an empty list. Instead, we will fall back
            to original lists of editions (unfiltered, of course) AND STILL sorted according to chosen art preference.
         */
        if (acceptedEditions.isEmpty())
            acceptedEditions.addAll(cardEditions);

        if (acceptedEditions.size() > 1) {
            Collections.sort(acceptedEditions);  // CardEdition correctly sort by (release) date
            if (artPref.latestFirst)
                Collections.reverse(acceptedEditions);  // newest editions first
            // Black Border: original Core/Expansion releases first, promos and compilations last. The sort is
            // stable, so editions of the same rank stay in release-date order.
            if (artPref.blackBorderOnly)
                acceptedEditions.sort(Comparator.comparingInt(artPref::getEditionRank));
        }

        // The Card Art Preference is always enforced: the first accepted edition wins, even when its image
        // is not on disk yet (images are downloaded on demand). Walking back to an older edition just because
        // it happens to have a local image made the preference look ignored.
        PaperCard candidate = candidatesCard.get(acceptedEditions.get(0).getCode());
        return preferTraditionalFrame(candidate, cr);
    }

    /* Picks by Card Art Preference give the traditional frame, unless a specific print was requested. */
    private PaperCard preferTraditionalFrame(PaperCard candidate, CardRequest cr) {
        boolean specificPrintRequested = cr.collectorNumber != null && !cr.collectorNumber.equals(IPaperCard.NO_COLLECTOR_NUMBER);
        PaperCard result = specificPrintRequested ? candidate : getTraditionalFramePrint(candidate);
        return cr.isFoil ? result.getFoiled() : result;
    }

    @Override
    public int getMaxArtIndex(String cardName) {
        if (cardName == null)
            return IPaperCard.NO_ART_INDEX;
        int max = IPaperCard.NO_ART_INDEX;
        for (PaperCard pc : getAllCards(cardName)) {
            if (max < pc.getArtIndex())
                max = pc.getArtIndex();
        }
        return max;
    }

    @Override
    public int getArtCount(String cardName, String setCode) {
        return getArtCount(cardName, setCode, null);
    }
    public int getArtCount(String cardName, String setCode, String functionalVariantName) {
        if (cardName == null || setCode == null)
            return 0;
        Predicate<PaperCard> predicate = card -> card.getEdition().equalsIgnoreCase(setCode);
        if(functionalVariantName != null && !functionalVariantName.equals(IPaperCard.NO_FUNCTIONAL_VARIANT)) {
            predicate = predicate.and(card -> functionalVariantName.equals(card.getFunctionalVariant()));
        }
        Collection<PaperCard> cardsInSet = getAllCardsNoAlt(cardName, predicate);
        return cardsInSet.size();
    }

    public boolean isNonLegendaryCreatureName(final String name) {
        Boolean bool = nonLegendaryCreatureNames.get(name);
        if (bool != null) {
            return bool;
        }
        // check if the name is from a face
        // in general token creatures does not have this
        final ICardFace face = StaticData.instance().getCommonCards().getFaceByName(name);
        if (face == null) {
            nonLegendaryCreatureNames.put(name, false);
            return false;
        }
        // TODO add check if face is legal in the format of the game
        // name does need to be a non-legendary creature
        final CardType type = face.getType();
        bool = type.isCreature() && !type.isLegendary();
        nonLegendaryCreatureNames.put(name, bool);
        return bool;
    }

    @Override
    public Collection<PaperCard> getAllCards() {
        return Collections.unmodifiableCollection(allCardsByRules.values());
    }

    // returns a list of all cards from their respective latest (or preferred) editions
    @Override
    public Collection<PaperCard> getUniqueCards() {
        return uniqueCardsByRules.values();
    }
    @Override
    public Collection<ICardFace> getAllFaces() {
        return facesByName.values();
    }

    @Override
    public Stream<PaperCard> streamAllCards() {
        return allCardsByRules.values().stream();
    }
    @Override
    public Stream<PaperCard> streamUniqueCards() {
        return uniqueCardsByRules.values().stream();
    }
    @Override
    public Stream<ICardFace> streamAllFaces() {
        return facesByName.values().stream();
    }

    public static final Predicate<PaperCard> EDITION_NON_PROMO = paperCard -> {
        String code = paperCard.getEdition();
        CardEdition edition = StaticData.instance().getCardEdition(code);
        if(edition == null && code.equals(CardEdition.UNKNOWN_CODE))
            return true;
        return edition != null && edition.getType() != Type.PROMO;
    };

    public static final Predicate<PaperCard> EDITION_NON_REPRINT = paperCard -> {
        String code = paperCard.getEdition();
        CardEdition edition = StaticData.instance().getCardEdition(code);
        if(edition == null && code.equals(CardEdition.UNKNOWN_CODE))
            return true;
        return edition != null && !Type.REPRINT_SET_TYPES.contains(edition.getType());
    };

    public Collection<PaperCard> getAllNonPromosNonReprintsNoAlt() {
        return streamAllCards().filter(EDITION_NON_REPRINT).collect(Collectors.toList());
    }

    public String getNormalizedName(final String cardName) {
        // normalize Names first
        return normalizedNames.getOrDefault(cardName, cardName);
    }

    /**
     * Returns all printings of cards that *have* the given name. This includes as alternate faces and flavor names.
     * <ul>
     *     <li>"Gift of the Fae" (an Adventure name) will return printings of "Faerie Guidemother".</li>
     *     <li>"Ironfang" (Transformed card back face) will return printings of "Village Ironsmith".</li>
     *     <li>"Ancestral Recall" will return printings of "Ancestral Recall" and printings of "Emeritus of Ideation".</li>
     *     <li>"Fire" will return printings of "Fire // Ice" and of "Start // Fire".</li>
     *     <li>"Fire // Ice" will only return "Fire // Ice".</li>
     *     <li>"SpongeBob SquarePants" will only return the appropriately flavored printing of "Jodah, the Unifier".</li>
     *     <li>"Jodah, the Unifier" will return all Jodah printings, including "Spongebob Squarepants".</li>
     * </ul>
     * @see #getAllCardsNoAlt(String)
     */
    @Override
    public List<PaperCard> getAllCards(String cardName) {
        List<PaperCard> cards = allCardsByName.get(getNormalizedName(cardName));
        if (cards.isEmpty() && lazyLoad(cardName, null)) {
            return allCardsByName.get(getNormalizedName(cardName));
        }
        return cards;
    }

    /**
     * Returns all printings cards that exactly match the given name. The name must be the primary name of the card.
     * <ul>
     *     <li>"Gift of the Fae" (an Adventure name) will return nothing.</li>
     *     <li>"Ironfang" (Transformed card back face) will return nothing.</li>
     *     <li>"Ancestral Recall" will only return printings of "Ancestral Recall", not "Emeritus of Ideation".</li>
     *     <li>"Fire" will return nothing. You must specify "Fire // Ice" or "Start // Fire".</li>
     *     <li>"SpongeBob SquarePants" will return nothing.</li>
     *     <li>"Jodah, the Unifier" will return all Jodah printings, including "SpongeBob SquarePants".</li>
     * </ul>
     * @see #getAllCards(String)
     */
    public List<PaperCard> getAllCardsNoAlt(String rulesName) {
        CardRules rules = getRules(rulesName, false);
        if(rules == null)
            return List.of();
        return allCardsByRules.get(rules);
    }

    public List<PaperCard> getAllCards(CardRules rules) {
        return allCardsByRules.get(rules);
    }

    public List<PaperCard> getAllCards(PaperCard card) {
        return getAllCards(card.getRules());
    }

    /**
     * Returns a modifiable list of cards matching the given predicate
     */
    @Override
    public List<PaperCard> getAllCards(Predicate<PaperCard> predicate) {
        return streamAllCards().filter(predicate).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public List<PaperCard> getAllCards(final String cardName, Predicate<PaperCard> predicate){
        return getAllCards(cardName).stream().filter(predicate).collect(Collectors.toCollection(ArrayList::new));
    }

    public List<PaperCard> getAllCards(PaperCard card, Predicate<PaperCard> predicate){
        return getAllCards(card).stream().filter(predicate).collect(Collectors.toCollection(ArrayList::new));
    }

    public List<PaperCard> getAllCards(CardRules card, Predicate<PaperCard> predicate){
        return getAllCards(card).stream().filter(predicate).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public List<PaperCard> getAllCardsNoAlt(final String rulesName, Predicate<PaperCard> predicate){
        return getAllCardsNoAlt(rulesName).stream().filter(predicate).collect(Collectors.toCollection(ArrayList::new));
    }

    // Do I want a foiled version of these cards?
    @Override
    public Collection<PaperCard> getAllCards(CardEdition edition) {
        List<PaperCard> cards = Lists.newArrayList();

        for (EditionEntry cis : edition.getAllCardsInSet()) {
            PaperCard card = this.getCard(cis.name(), edition.getCode());
            if (card == null) {
                // Just in case the card is listed in the edition file but Forge doesn't support it
                continue;
            }

            cards.add(card);
        }
        return cards;
    }

    @Override
    public PaperCard getUniqueByName(String cardName) {
        if (uniqueCardsByFlavorName.containsKey(cardName))
            return uniqueCardsByFlavorName.get(cardName);
        CardRules rules = getRules(cardName, true);
        if(rules == null)
            return null;
        return uniqueCardsByRules.get(rules);
    }

    @Override
    public PaperCard getUniqueByNameNoAlt(String rulesName) {
        CardRules rules = getRules(rulesName, false);
        if(rules == null)
            return null;
        return uniqueCardsByRules.get(rules);
    }

    @Override
    public ICardFace getFaceByName(String faceName) {
        ICardFace face = facesByName.get(getNormalizedName(faceName));
        if (face == null && lazyLoad(faceName, null)) {
            face = facesByName.get(getNormalizedName(faceName));
        }
        return face;
    }

    @Override
    public boolean contains(String name) {
        return allCardsByName.containsKey(getNormalizedName(name));
    }

    @Override
    public Iterator<PaperCard> iterator() {
        return getAllCards().iterator();
    }

    @Override
    public Predicate<? super PaperCard> wasPrintedInSets(Collection<String> setCodes) {
        return paperCard -> getAllCards(paperCard).stream()
                .map(PaperCard::getEdition).anyMatch(editionCode ->
                    setCodes.contains(editionCode) &&
                        StaticData.instance().getCardEdition(editionCode).isCardObtainable(paperCard.getName())
                );
    }

    // This Predicate validates if a card is legal in a given format (identified by the list of allowed sets)
    @Override
    public Predicate<? super PaperCard> isLegal(Collection<String> allowedSetCodes) {
        return paperCard -> paperCard != null && allowedSetCodes.contains(paperCard.getEdition());
    }

    // This Predicate validates if a card was printed at [rarity], on any of its printings
    @Override
    public Predicate<? super PaperCard> wasPrintedAtRarity(CardRarity rarity) {
        return paperCard -> getAllCards(paperCard).stream()
                .map(PaperCard::getRarity)
                .anyMatch(rarity::equals);
    }

    public PaperCard createUnsupportedCard(String cardRequest) {
        CardRequest request = CardRequest.fromString(cardRequest);
        CardEdition cardEdition = CardEdition.UNKNOWN;
        CardRarity cardRarity = CardRarity.Unknown;

        // May iterate over editions and find out if there is any card named 'cardRequest' but not implemented with Forge script.
        if (StringUtils.isBlank(request.edition)) {
            for (CardEdition edition : editions) {
                for (EditionEntry cardInSet : edition.getAllCardsInSet()) {
                    if (cardInSet.name().equals(request.cardName)) {
                        cardEdition = edition;
                        cardRarity = cardInSet.rarity();
                        break;
                    }
                }
                if (cardEdition != CardEdition.UNKNOWN) {
                    break;
                }
            }
        } else {
            cardEdition = editions.get(request.edition);
            if (cardEdition != null) {
                for (EditionEntry cardInSet : cardEdition.getAllCardsInSet()) {
                    if (cardInSet.name().equals(request.cardName)) {
                        cardRarity = cardInSet.rarity();
                        break;
                    }
                }
            } else {
                cardEdition = CardEdition.UNKNOWN;
            }
        }

        // Note for myself: no localisation needed here as this goes in logs
        if (cardRarity == CardRarity.Unknown) {
            System.err.println("Forge could not find this card in the Database. Any chance you might have mistyped the card name?");
        } else {
            System.err.println("We're sorry, but this card is not supported yet.");
        }

        return new PaperCard(CardRules.getUnsupportedCardNamed(request.cardName), cardEdition.getCode(), cardRarity);
    }

    private final Editor editor = new Editor();

    public Editor getEditor() {
        return editor;
    }

    public class Editor {
        private boolean immediateReindex = true;

        public CardRules putCard(CardRules rules) {
            return putCard(rules, null); /* will use data from editions folder */
        }

        public CardRules putCard(CardRules rules, List<Pair<String, CardRarity>> whenItWasPrinted) {
            // works similarly to Map<K,V>, returning prev. value
            String cardName = rules.getName();

            CardRules result = rulesByPrimaryName.get(cardName);
            if (result != null && result.getName().equals(cardName)) { // change properties only
                result.reinitializeFromRules(rules);
                return result;
            }

            result = rulesByPrimaryName.put(cardName, rules);

            // 1. generate all paper cards from edition data we have (either explicit, or found in res/editions, or add to unknown edition)
            List<PaperCard> paperCards = new ArrayList<>();
            if (null == whenItWasPrinted || whenItWasPrinted.isEmpty()) {
                // @friarsol: Not performant Each time we "putCard" we loop through ALL CARDS IN ALL editions
                // @leriomaggio: DONE! re-using here the same strategy implemented for lazy-loading!
                for (CardEdition e : editions.getOrderedEditions()) {
                    int artIdx = IPaperCard.DEFAULT_ART_INDEX;
                    for (EditionEntry cis : e.getCardInSet(cardName))
                        paperCards.add(new PaperCard(rules, e.getCode(), cis.rarity(), artIdx++, false,
                                                     cis.collectorNumber(), cis.artistName(), cis.getFunctionalVariantName()));
                }
            } else {
                String lastEdition = null;
                int artIdx = 0;
                for (Pair<String, CardRarity> tuple : whenItWasPrinted) {
                    if (!tuple.getKey().equals(lastEdition)) {
                        artIdx = IPaperCard.DEFAULT_ART_INDEX;  // reset artIndex
                        lastEdition = tuple.getKey();
                    }
                    CardEdition ed = editions.get(lastEdition);
                    if (ed == null) {
                        continue;
                    }
                    List<EditionEntry> cardsInSet = ed.getCardInSet(cardName);
                    if (cardsInSet.isEmpty())
                        continue;
                    int cardInSetIndex = Math.max(artIdx-1, 0); // make sure doesn't go below zero
                    EditionEntry cds = cardsInSet.get(cardInSetIndex);  // use ArtIndex to get the right Coll. Number
                    paperCards.add(new PaperCard(rules, lastEdition, tuple.getValue(), artIdx++, false,
                                                 cds.collectorNumber(), cds.artistName(), cds.getFunctionalVariantName()));
                }
            }
            if (paperCards.isEmpty()) {
                paperCards.add(new PaperCard(rules, CardEdition.UNKNOWN_CODE, CardRarity.Special));
            }
            // 2. add them to db
            for (PaperCard paperCard : paperCards) {
                addCard(paperCard);
            }
            // 3. reindex can be temporary disabled and run after the whole batch of rules is added to db.
            if (immediateReindex) {
                reIndex();
            }
            return result;
        }

        public boolean isImmediateReindex() {
            return immediateReindex;
        }

        public void setImmediateReindex(boolean immediateReindex) {
            this.immediateReindex = immediateReindex;
        }
    }
}
