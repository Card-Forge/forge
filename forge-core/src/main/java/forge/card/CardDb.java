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

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import forge.StaticData;
import forge.card.CardEdition.EditionEntry;
import forge.card.CardEdition.Type;
import forge.deck.generation.IDeckGenPool;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.util.Lang;
import forge.util.TextUtil;
import forge.util.lang.LangEnglish;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class CardDb implements ICardDatabase, IDeckGenPool {
    public final static String foilSuffix = "+";
    public final static char NameSetSeparator = '|';
    public final static String FlagPrefix = "#";
    public static final String FlagSeparator = "\t";
    private final String exlcudedCardName = "Concentrate";
    private final String exlcudedCardSet = "DS0";

    // need this to obtain cardReference by name+set+artindex
    private final ListMultimap<String, PaperCard> allCardsByName = Multimaps.newListMultimap(new TreeMap<>(String.CASE_INSENSITIVE_ORDER), Lists::newArrayList);
    private final Map<String, PaperCard> uniqueCardsByName = Maps.newTreeMap(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, CardRules> rulesByName;
    private final Map<String, ICardFace> facesByName = Maps.newTreeMap(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, String> normalizedNames = Maps.newTreeMap(String.CASE_INSENSITIVE_ORDER);
    private static Map<String, String> artPrefs = Maps.newHashMap();

    private final Map<String, String> alternateName = Maps.newTreeMap(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, Integer> artIds = Maps.newHashMap();

    private final CardEdition.Collection editions;
    private List<String> filtered;

    private Map<String, Boolean> nonLegendaryCreatureNames = Maps.newHashMap();

    public enum CardArtPreference {
        LATEST_ART_ALL_EDITIONS(false, true),
        LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY(true, true),
        ORIGINAL_ART_ALL_EDITIONS(false, false),
        ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY(true, false);

        public final boolean filterSets;
        public final boolean latestFirst;

        CardArtPreference(boolean filterIrregularSets, boolean latestSetFirst) {
            filterSets = filterIrregularSets;
            latestFirst = latestSetFirst;
        }

        private static final EnumSet<Type> ALLOWED_SET_TYPES = EnumSet.of(Type.CORE, Type.EXPANSION, Type.REPRINT);

        public boolean accept(CardEdition ed) {
            if (ed == null) return false;
            return !filterSets || ALLOWED_SET_TYPES.contains(ed.getType());
        }
    }

    // Placeholder to setup default art Preference - to be moved from Static Data!
    private CardArtPreference defaultCardArtPreference;

    public static class CardRequest {
        public String cardName;
        public String edition;
        public int artIndex;
        public boolean isFoil;
        public String collectorNumber;
        public Map<String, String> flags;

        private CardRequest(String name, String edition, int artIndex, boolean isFoil, String collectorNumber) {
            this(name, edition, artIndex, isFoil, collectorNumber, null);
        }

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
            return StringUtils.isNumeric(s) && s.length() <= 2 ; // only artIndex between 1-99
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
            if(info.length > index && isArtIndex(info[index])) {
                artIndex = Integer.parseInt(info[index]);
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

    public CardDb(Map<String, CardRules> rules, CardEdition.Collection editions0, List<String> filteredCards, String cardArtPreference) {
        this.filtered = filteredCards;
        this.rulesByName = rules;
        this.editions = editions0;

        // create faces list from rules
        for (final CardRules rule : rules.values()) {
            if (filteredCards.contains(rule.getName()) && !exlcudedCardName.equalsIgnoreCase(rule.getName()))
                continue;
            for (ICardFace face : rule.getAllFaces()) {
                addFaceToDbNames(face);
            }
        }
        setCardArtPreference(cardArtPreference);
    }

    private void addFaceToDbNames(ICardFace face) {
        if (face == null) {
            return;
        }
        final String name = face.getName();
        facesByName.put(name, face);
        final String normalName = StringUtils.stripAccents(name);
        if (!normalName.equals(name)) {
            normalizedNames.put(normalName, name);
        }

        final String altName = face.getAltName();
        if (altName != null) {
            alternateName.put(altName, face.getName());
            final String normalAltName = StringUtils.stripAccents(altName);
            if (!normalAltName.equals(altName)) {
                normalizedNames.put(normalAltName, altName);
            }
        }
    }

    private void addSetCard(CardEdition e, EditionEntry cis, CardRules cr) {
        int artIdx = IPaperCard.DEFAULT_ART_INDEX;
        String key = e.getCode() + "/" + cis.name();
        if (artIds.containsKey(key)) {
            artIdx = artIds.get(key) + 1;
        }

        artIds.put(key, artIdx);
        addCard(new PaperCard(cr, e.getCode(), cis.rarity(), artIdx, false, cis.collectorNumber(), cis.artistName(), cis.functionalVariantName()));
    }

    private boolean addFromSetByName(String cardName, CardEdition ed, CardRules cr) {
        List<EditionEntry> cardsInSet = ed.getCardInSet(cardName);  // empty collection if not present
        if (cr.hasFunctionalVariants()) {
            cardsInSet = cardsInSet.stream().filter(c -> StringUtils.isEmpty(c.functionalVariantName())
                    || cr.getSupportedFunctionalVariants().contains(c.functionalVariantName())
            ).collect(Collectors.toList());
        }
        if (cardsInSet.isEmpty())
            return false;
        for (EditionEntry cis : cardsInSet) {
            addSetCard(ed, cis, cr);
        }
        return true;
    }

    public void loadCard(String cardName, String setCode, CardRules cr) {
        // @leriomaggio: This method is called when lazy-loading is set
        // OR if a card is trying to load from an edition its not from
        //System.out.println("[LOG]: (Lazy) Loading Card: " + cardName);
        rulesByName.put(cardName, cr);
        boolean reIndexNecessary = false;
        CardEdition ed = editions.get(setCode);
        if (ed == null || ed.equals(CardEdition.UNKNOWN)) {
            // look for all possible editions
            for (CardEdition e : editions) {
                reIndexNecessary |= addFromSetByName(cardName, e, cr);
            }
        } else {
            reIndexNecessary |= addFromSetByName(cardName, ed, cr);
        }

        if (reIndexNecessary)
            reIndex();
    }

    public void initialize(boolean logMissingPerEdition, boolean logMissingSummary, boolean enableUnknownCards) {
        Set<String> allMissingCards = new LinkedHashSet<>();
        List<String> missingCards = new ArrayList<>();
        CardEdition upcomingSet = null;
        Date today = new Date();

        // do this first so they're not considered missing
        buildRenamedCards();

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
                CardRules cr = rulesByName.get(cis.name());
                if (cr == null) {
                    missingCards.add(cis.name());
                    continue;
                }
                if (cr.hasFunctionalVariants()) {
                    if (StringUtils.isNotEmpty(cis.functionalVariantName())
                        && !cr.getSupportedFunctionalVariants().contains(cis.functionalVariantName())) {
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

        if (upcomingSet != null) {
            System.err.println("Upcoming set " + upcomingSet + " dated in the future. All unaccounted cards will be added to this set with unknown rarity.");
        }

        for (CardRules cr : rulesByName.values()) {
            if (!contains(cr.getName())) {
                if (!cr.isCustom()) {
                    if (upcomingSet != null) {
                        addCard(new PaperCard(cr, upcomingSet.getCode(), CardRarity.Unknown));
                    } else if (enableUnknownCards && !this.filtered.contains(cr.getName())) {
                        System.err.println("The card " + cr.getName() + " was not assigned to any set. Adding it to UNKNOWN set... to fix see res/editions/ folder. ");
                        addCard(new PaperCard(cr, CardEdition.UNKNOWN_CODE, CardRarity.Special));
                    }
                } else {
                    System.err.println("The custom card " + cr.getName() + " was not assigned to any set. Adding it to custom USER set, and will try to load custom art from USER edition.");
                    addCard(new PaperCard(cr, "USER", CardRarity.Special));
                }
            }
        }

        reIndex();
    }

    private void buildRenamedCards() {
        Lang lang = Lang.getInstance();
        if (lang == null) {
            // for some tests
            lang = new LangEnglish();
        }
        // for now just check Universes Within
        for (EditionEntry cis : editions.get("SLX").getCards()) {
            String orgName = alternateName.get(cis.name());
            if (orgName != null) {
                // found original (beyond) print
                CardRules org = getRules(orgName);

                CardFace renamedMain = (CardFace) ((CardFace) org.getMainPart()).clone();
                renamedMain.setName(renamedMain.getAltName());
                renamedMain.setAltName(null);
                // TODO this could mess up some "named ..." cardname literals but there's no printing like that currently
                renamedMain.setOracleText(renamedMain.getOracleText()
                        .replace(orgName, renamedMain.getName())
                        .replace(lang.getNickName(orgName), lang.getNickName(renamedMain.getName()))
                        );
                facesByName.put(renamedMain.getName(), renamedMain);
                CardFace renamedOther = null;
                if (org.getOtherPart() != null) {
                    renamedOther = (CardFace) ((CardFace) org.getOtherPart()).clone();
                    orgName = renamedOther.getName();
                    renamedOther.setName(renamedOther.getAltName());
                    renamedOther.setAltName(null);
                    renamedOther.setOracleText(renamedOther.getOracleText()
                            .replace(orgName, renamedOther.getName())
                            .replace(lang.getNickName(orgName), lang.getNickName(renamedOther.getName()))
                            );
                    facesByName.put(renamedOther.getName(), renamedOther);
                }

                CardRules within = new CardRules(new ICardFace[] { renamedMain, renamedOther, null, null, null, null, null }, org.getSplitType(), org.getAiHints());
                // so workshop can edit same script
                within.setNormalizedName(org.getNormalizedName());
                rulesByName.put(cis.name(), within);
            }
        }
    }

    public void addCard(PaperCard paperCard) {
        if (excludeCard(paperCard.getName(), paperCard.getEdition()))
            return;

        allCardsByName.put(paperCard.getName(), paperCard);

        if (paperCard.getRules().getSplitType() == CardSplitType.None) {
            return;
        }

        if (paperCard.getRules().getOtherPart() != null) {
            //allow looking up card by the name of other faces
            allCardsByName.put(paperCard.getRules().getOtherPart().getName(), paperCard);
        }
        if (paperCard.getRules().getSplitType() == CardSplitType.Split) {
            //also include main part for split cards
            allCardsByName.put(paperCard.getRules().getMainPart().getName(), paperCard);
        } else if (paperCard.getRules().getSplitType() == CardSplitType.Specialize) {
            //also include specialize faces
            for (ICardFace face : paperCard.getRules().getSpecializeParts().values()) allCardsByName.put(face.getName(), paperCard);
        }
    }

    private boolean excludeCard(String cardName, String cardEdition) {
        if (filtered.isEmpty())
            return false;
        if (filtered.contains(cardName)) {
            if (exlcudedCardSet.equalsIgnoreCase(cardEdition) && exlcudedCardName.equalsIgnoreCase(cardName))
                return true;
            else return !exlcudedCardName.equalsIgnoreCase(cardName);
        }
        return false;
    }

    private void reIndex() {
        uniqueCardsByName.clear();
        for (Entry<String, Collection<PaperCard>> kv : allCardsByName.asMap().entrySet()) {
            PaperCard pc = getFirstNonSpeicalWithImage(kv.getValue());
            uniqueCardsByName.put(kv.getKey(), pc);
        }
    }

    private static PaperCard getFirstNonSpeicalWithImage(final Collection<PaperCard> cards) {
        //NOTE: this is written this way to avoid checking final card in list
        final Iterator<PaperCard> iterator = cards.iterator();
        PaperCard pc = iterator.next();
        while (iterator.hasNext()) {
            if (pc.hasImage() && !pc.getRarity().equals(CardRarity.Special)) {
                return pc;
            }
            pc = iterator.next();
        }
        return pc;
    }

    public boolean setPreferredArt(String cardName, String setCode, int artIndex) {
        String cardRequestForPreferredArt = CardRequest.compose(cardName, setCode, artIndex);
        PaperCard pc = this.getCard(cardRequestForPreferredArt);
        if (pc != null) {
            artPrefs.put(cardName, cardRequestForPreferredArt);
            uniqueCardsByName.put(cardName, pc);
            return true;
        }
        return false;
    }

    public boolean hasPreferredArt(String cardName){
        return artPrefs.getOrDefault(cardName, null) != null;
    }

    public CardRules getRules(String cardName) {
        CardRules result = rulesByName.get(cardName);
        if (result != null) {
            return result;
        } else {
            return CardRules.getUnsupportedCardNamed(cardName);
        }
    }

    public CardArtPreference getCardArtPreference(){ return this.defaultCardArtPreference; }
    public void setCardArtPreference(boolean latestArt, boolean coreExpansionOnly){
        if (coreExpansionOnly){
            this.defaultCardArtPreference = latestArt ? CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY : CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY;
        } else {
            this.defaultCardArtPreference = latestArt ? CardArtPreference.LATEST_ART_ALL_EDITIONS : CardArtPreference.ORIGINAL_ART_ALL_EDITIONS;
        }
    }

    public void setCardArtPreference(String artPreference){
        artPreference = artPreference.toLowerCase().trim();
        boolean isLatest = artPreference.contains("latest");
        // additional check in case of unrecognised values wrt. to legacy opts
        if (!artPreference.contains("original") && !artPreference.contains("earliest"))
            isLatest = true;  // this must be default
        boolean hasFilter = artPreference.contains("core");
        this.setCardArtPreference(isLatest, hasFilter);
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
    public PaperCard getCardFromSet(String cardName, CardEdition edition, boolean isFoil) {
        return getCardFromSet(cardName, edition, IPaperCard.NO_ART_INDEX,
                IPaperCard.NO_COLLECTOR_NUMBER, isFoil);
    }

    @Override
    public PaperCard getCardFromSet(String cardName, CardEdition edition, int artIndex, boolean isFoil) {
        return getCardFromSet(cardName, edition, artIndex, IPaperCard.NO_COLLECTOR_NUMBER, isFoil);
    }

    @Override
    public PaperCard getCardFromSet(String cardName, CardEdition edition, String collectorNumber, boolean isFoil) {
        return getCardFromSet(cardName, edition, IPaperCard.NO_ART_INDEX, collectorNumber, isFoil);
    }

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

    /* Get Card from Edition using the default `CardArtPreference`
    NOTE: this method has NOT been included in the Interface API refactoring as it
    relies on a specific (new) attribute included in the `CardDB` that sets the
    default `ArtPreference`. This attribute does not necessarily belongs to any
    class implementing ICardInterface, and so the not inclusion in the API
     */
    public PaperCard getCardFromEditions(final String cardName) {
        return this.getCardFromEditions(cardName, this.defaultCardArtPreference);
    }

    public PaperCard getCardFromEditions(final String cardName, Predicate<PaperCard> filter) {
        return this.getCardFromEditions(cardName, this.defaultCardArtPreference, filter);
    }

    @Override
    public PaperCard getCardFromEditions(final String cardName, CardArtPreference artPreference) {
        return getCardFromEditions(cardName, artPreference, IPaperCard.NO_ART_INDEX);
    }

    @Override
    public PaperCard getCardFromEditions(final String cardName, CardArtPreference artPreference, Predicate<PaperCard> filter) {
        return getCardFromEditions(cardName, artPreference, IPaperCard.NO_ART_INDEX, filter);
    }

    @Override
    public PaperCard getCardFromEditions(final String cardInfo, final CardArtPreference artPreference, int artIndex) {
        return this.tryToGetCardFromEditions(cardInfo, artPreference, artIndex, null);
    }

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

    public PaperCard getCardFromEditionsReleasedBefore(String cardName, Date releaseDate){
        return this.getCardFromEditionsReleasedBefore(cardName, this.defaultCardArtPreference, PaperCard.DEFAULT_ART_INDEX, releaseDate);
    }

    public PaperCard getCardFromEditionsReleasedBefore(String cardName, int artIndex, Date releaseDate){
        return this.getCardFromEditionsReleasedBefore(cardName, this.defaultCardArtPreference, artIndex, releaseDate);
    }

    public PaperCard getCardFromEditionsReleasedBefore(String cardName, Date releaseDate, Predicate<PaperCard> filter){
        return this.getCardFromEditionsReleasedBefore(cardName, this.defaultCardArtPreference, releaseDate, filter);
    }

    @Override
    public PaperCard getCardFromEditionsReleasedBefore(String cardName, CardArtPreference artPreference, Date releaseDate){
        return this.getCardFromEditionsReleasedBefore(cardName, artPreference, PaperCard.DEFAULT_ART_INDEX, releaseDate);
    }

    @Override
    public PaperCard getCardFromEditionsReleasedBefore(String cardName, CardArtPreference artPreference, Date releaseDate, Predicate<PaperCard> filter){
        return this.getCardFromEditionsReleasedBefore(cardName, artPreference, PaperCard.DEFAULT_ART_INDEX, releaseDate, filter);
    }

    @Override
    public PaperCard getCardFromEditionsReleasedBefore(String cardName, CardArtPreference artPreference, int artIndex, Date releaseDate){
        return this.tryToGetCardFromEditions(cardName, artPreference, artIndex, releaseDate, true, null);
    }

    @Override
    public PaperCard getCardFromEditionsReleasedBefore(String cardName, CardArtPreference artPreference, int artIndex, Date releaseDate, Predicate<PaperCard> filter){
        return this.tryToGetCardFromEditions(cardName, artPreference, artIndex, releaseDate, true, filter);
    }

    public PaperCard getCardFromEditionsReleasedAfter(String cardName, Date releaseDate){
        return this.getCardFromEditionsReleasedAfter(cardName, this.defaultCardArtPreference, PaperCard.DEFAULT_ART_INDEX, releaseDate);
    }

    public PaperCard getCardFromEditionsReleasedAfter(String cardName, int artIndex, Date releaseDate){
        return this.getCardFromEditionsReleasedAfter(cardName, this.defaultCardArtPreference, artIndex, releaseDate);
    }

    @Override
    public PaperCard getCardFromEditionsReleasedAfter(String cardName, CardArtPreference artPreference, Date releaseDate){
        return this.getCardFromEditionsReleasedAfter(cardName, artPreference, PaperCard.DEFAULT_ART_INDEX, releaseDate);
    }

    @Override
    public PaperCard getCardFromEditionsReleasedAfter(String cardName, CardArtPreference artPreference, Date releaseDate, Predicate<PaperCard> filter){
        return this.getCardFromEditionsReleasedAfter(cardName, artPreference, PaperCard.DEFAULT_ART_INDEX, releaseDate, filter);
    }

    @Override
    public PaperCard getCardFromEditionsReleasedAfter(String cardName, CardArtPreference artPreference, int artIndex, Date releaseDate){
        return this.tryToGetCardFromEditions(cardName, artPreference, artIndex, releaseDate, false, null);
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
        cards = getAllCards(cr.cardName, cardQueryFilter);
        // Note: No need to check whether "cards" is empty; the next for loop will validate condition at L699
        if (cards.size() == 1)  // if only one candidate, there much else we should do
            return cr.isFoil ? cards.get(0).getFoiled() : cards.get(0);

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
        }

        final Iterator<CardEdition> editionIterator = acceptedEditions.iterator();
        CardEdition ed = editionIterator.next();
        PaperCard candidate = candidatesCard.get(ed.getCode());
        PaperCard firstCandidate = candidate;
        while (!candidate.hasImage() && editionIterator.hasNext()) {
            ed = editionIterator.next();
            candidate = candidatesCard.get(ed.getCode());
        }
        candidate = candidate.hasImage() ? candidate : firstCandidate;
        //If any, we're sure that at least one candidate is always returned despite it having any image
        return cr.isFoil ? candidate.getFoiled() : candidate;
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
        Collection<PaperCard> cardsInSet = getAllCards(cardName, predicate);
        return cardsInSet.size();
    }

    // returns a list of all cards from their respective latest (or preferred) editions
    @Override
    public Collection<PaperCard> getUniqueCards() {
        return uniqueCardsByName.values();
    }

    public Collection<PaperCard> getUniqueCardsNoAlt() {
        return Maps.filterEntries(this.uniqueCardsByName, e -> {
            if (e == null)
                return false;
            return e.getKey().equals(e.getValue().getName());
        }).values();
    }

    public List<PaperCard> getUniqueCardsNoAlt(String cardName) {
        return Lists.newArrayList(Maps.filterEntries(uniqueCardsByName, entry -> entry.getKey().equals(entry.getValue().getName())).get(getName(cardName)));
    }

    public PaperCard getUniqueByName(final String name) {
        return uniqueCardsByName.get(getName(name));
    }

    public Collection<ICardFace> getAllFaces() {
        return facesByName.values();
    }

    public ICardFace getFaceByName(final String name) {
        return facesByName.get(getName(name));
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
        return Collections.unmodifiableCollection(allCardsByName.values());
    }

    public Collection<PaperCard> getAllCardsNoAlt() {
        return Multimaps.filterEntries(allCardsByName, entry -> entry.getKey().equals(entry.getValue().getName())).values();
    }

    @Override
    public Stream<PaperCard> streamAllCards() {
        return allCardsByName.values().stream();
    }
    @Override
    public Stream<PaperCard> streamUniqueCards() {
        return uniqueCardsByName.values().stream();
    }
    public Stream<PaperCard> streamAllCardsNoAlt() {
        return allCardsByName.entries().stream().filter(e -> e.getKey().equals(e.getValue().getName())).map(Entry::getValue);
    }
    public Stream<PaperCard> streamUniqueCardsNoAlt() {
        return uniqueCardsByName.entrySet().stream().filter(e -> e.getKey().equals(e.getValue().getName())).map(Entry::getValue);
    }

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
        return edition != null && Type.REPRINT_SET_TYPES.contains(edition.getType());
    };

    public Collection<PaperCard> getAllNonPromosNonReprintsNoAlt() {
        return streamAllCardsNoAlt().filter(EDITION_NON_REPRINT).collect(Collectors.toList());
    }

    public String getName(final String cardName) {
        return getName(cardName, false);
    }
    public String getName(String cardName, boolean engine) {
        // normalize Names first
        cardName = normalizedNames.getOrDefault(cardName, cardName);
        if (alternateName.containsKey(cardName) && engine) {
            // TODO might want to implement GUI option so it always fetches the Within version
            return alternateName.get(cardName);
        }
        return cardName;
    }

    @Override
    public List<PaperCard> getAllCards(String cardName) {
        return allCardsByName.get(getName(cardName));
    }

    public List<PaperCard> getAllCardsNoAlt(String cardName) {
        return Lists.newArrayList(Multimaps.filterEntries(allCardsByName, entry -> entry.getKey().equals(entry.getValue().getName())).get(getName(cardName)));
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

    /**
     * Returns a modifiable list of cards matching the given predicate
     */
    public List<PaperCard> getAllCardsNoAlt(Predicate<PaperCard> predicate) {
        return streamAllCardsNoAlt().filter(predicate).collect(Collectors.toCollection(ArrayList::new));
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
    public boolean contains(String name) {
        return allCardsByName.containsKey(getName(name));
    }

    @Override
    public Iterator<PaperCard> iterator() {
        return getAllCards().iterator();
    }

    @Override
    public Predicate<? super PaperCard> wasPrintedInSets(Collection<String> setCodes) {
        Set<String> sets = new HashSet<>(setCodes);
        return paperCard -> getAllCards(paperCard.getName()).stream()
                .map(PaperCard::getEdition)
                .anyMatch(sets::contains);
    }

    // This Predicate validates if a card is legal in a given format (identified by the list of allowed sets)
    @Override
    public Predicate<? super PaperCard> isLegal(Collection<String> allowedSetCodes){
        Set<String> sets = new HashSet<>(allowedSetCodes);
        return paperCard -> paperCard != null && sets.contains(paperCard.getEdition());
    }

    // This Predicate validates if a card was printed at [rarity], on any of its printings
    @Override
    public Predicate<? super PaperCard> wasPrintedAtRarity(CardRarity rarity) {
        return paperCard -> getAllCards(paperCard.getName()).stream()
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

            CardRules result = rulesByName.get(cardName);
            if (result != null && result.getName().equals(cardName)) { // change properties only
                result.reinitializeFromRules(rules);
                return result;
            }

            result = rulesByName.put(cardName, rules);

            // 1. generate all paper cards from edition data we have (either explicit, or found in res/editions, or add to unknown edition)
            List<PaperCard> paperCards = new ArrayList<>();
            if (null == whenItWasPrinted || whenItWasPrinted.isEmpty()) {
                // @friarsol: Not performant Each time we "putCard" we loop through ALL CARDS IN ALL editions
                // @leriomaggio: DONE! re-using here the same strategy implemented for lazy-loading!
                for (CardEdition e : editions.getOrderedEditions()) {
                    int artIdx = IPaperCard.DEFAULT_ART_INDEX;
                    for (EditionEntry cis : e.getCardInSet(cardName))
                        paperCards.add(new PaperCard(rules, e.getCode(), cis.rarity(), artIdx++, false,
                                                     cis.collectorNumber(), cis.artistName(), cis.functionalVariantName()));
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
                                                 cds.collectorNumber(), cds.artistName(), cds.functionalVariantName()));
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
