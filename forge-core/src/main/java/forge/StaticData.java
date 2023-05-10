package forge;

import com.google.common.base.Predicate;
import forge.card.CardDb;
import forge.card.CardEdition;
import forge.card.CardRules;
import forge.card.PrintSheet;
import forge.item.*;
import forge.token.TokenDb;
import forge.util.FileUtil;
import forge.util.ImageUtil;
import forge.util.TextUtil;
import forge.util.storage.IStorage;
import forge.util.storage.StorageBase;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.*;


/**
 * The class holding game invariants, such as cards, editions, game formats. All that data, which is not supposed to be changed by player
 *
 * @author Max
 */
public class StaticData {
    private final CardStorageReader cardReader;
    private final CardStorageReader tokenReader;
    private final CardStorageReader customCardReader;

    private final String blockDataFolder;
    private final CardDb commonCards;
    private final CardDb variantCards;
    private final TokenDb allTokens;
    private final CardEdition.Collection editions;

    private Predicate<PaperCard> standardPredicate;
    private Predicate<PaperCard> brawlPredicate;
    private Predicate<PaperCard> pioneerPredicate;
    private Predicate<PaperCard> modernPredicate;
    private Predicate<PaperCard> commanderPredicate;
    private Predicate<PaperCard> oathbreakerPredicate;

    private boolean filteredHandsEnabled = false;

    private MulliganDefs.MulliganRule mulliganRule = MulliganDefs.getDefaultRule();

    private boolean allowCustomCardsInDecksConformance;
    private boolean enableSmartCardArtSelection;
    private boolean loadNonLegalCards;

    // Loaded lazily:
    private IStorage<SealedProduct.Template> boosters;
    private IStorage<SealedProduct.Template> specialBoosters;
    private IStorage<SealedProduct.Template> tournaments;
    private IStorage<FatPack.Template> fatPacks;
    private IStorage<BoosterBox.Template> boosterBoxes;
    private IStorage<PrintSheet> printSheets;
    private final Map<String, List<String>> setLookup = new HashMap<>();
    private List<String> blocksLandCodes = new ArrayList<>();

    private static StaticData lastInstance = null;

    public StaticData(CardStorageReader cardReader, CardStorageReader customCardReader, String editionFolder, String customEditionsFolder, String blockDataFolder, String cardArtPreference, boolean enableUnknownCards, boolean loadNonLegalCards) {
        this(cardReader, null, customCardReader, null, editionFolder, customEditionsFolder, blockDataFolder, "", cardArtPreference, enableUnknownCards, loadNonLegalCards, false, false);
    }

    public StaticData(CardStorageReader cardReader, CardStorageReader tokenReader, CardStorageReader customCardReader, CardStorageReader customTokenReader, String editionFolder, String customEditionsFolder, String blockDataFolder, String setLookupFolder, String cardArtPreference, boolean enableUnknownCards, boolean loadNonLegalCards, boolean allowCustomCardsInDecksConformance){
        this(cardReader, tokenReader, customCardReader, customTokenReader, editionFolder, customEditionsFolder, blockDataFolder, setLookupFolder, cardArtPreference, enableUnknownCards, loadNonLegalCards, allowCustomCardsInDecksConformance, false);
    }

    public StaticData(CardStorageReader cardReader, CardStorageReader tokenReader, CardStorageReader customCardReader, CardStorageReader customTokenReader, String editionFolder, String customEditionsFolder, String blockDataFolder, String setLookupFolder, String cardArtPreference, boolean enableUnknownCards, boolean loadNonLegalCards, boolean allowCustomCardsInDecksConformance, boolean enableSmartCardArtSelection) {
        this.cardReader = cardReader;
        this.tokenReader = tokenReader;
        this.editions = new CardEdition.Collection(new CardEdition.Reader(new File(editionFolder)));
        this.blockDataFolder = blockDataFolder;
        this.customCardReader = customCardReader;
        this.allowCustomCardsInDecksConformance = allowCustomCardsInDecksConformance;
        this.enableSmartCardArtSelection = enableSmartCardArtSelection;
        this.loadNonLegalCards = loadNonLegalCards;
        lastInstance = this;
        List<String> funnyCards = new ArrayList<>();
        List<String> filtered = new ArrayList<>();
        editions.append(new CardEdition.Collection(new CardEdition.Reader(new File(customEditionsFolder), true)));

        {
            final Map<String, CardRules> regularCards = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            final Map<String, CardRules> variantsCards = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            if (!loadNonLegalCards) {
                for (CardEdition e : editions) {
                    if (e.getType() == CardEdition.Type.FUNNY || e.getBorderColor() == CardEdition.BorderColor.SILVER) {
                        for (CardEdition.CardInSet cis : e.getAllCardsInSet()) {
                            funnyCards.add(cis.name);
                        }
                    }
                }
            }

            for (CardRules card : cardReader.loadCards()) {
                if (null == card) continue;

                final String cardName = card.getName();

                if (!loadNonLegalCards && !card.getType().isLand() && funnyCards.contains(cardName))
                    filtered.add(cardName);

                if (card.isVariant()) {
                    variantsCards.put(cardName, card);
                } else {
                    regularCards.put(cardName, card);
                }
            }
            if (customCardReader != null) { //Load user's custom cards.
                for (CardRules card : customCardReader.loadCards()) {
                    if (null == card) continue;

                    final String cardName = card.getName();
                    card.setCustom();
                    if(card.isVariant()) { //Append loaded custom cards to the respective list.
                        variantsCards.put(cardName, card);
                    } else {
                        regularCards.put(cardName, card);
                    }
                }
            }

            if (!filtered.isEmpty()) {
                Collections.sort(filtered);
            }

            commonCards = new CardDb(regularCards, editions, filtered, cardArtPreference);
            variantCards = new CardDb(variantsCards, editions, filtered, cardArtPreference);

            //must initialize after establish field values for the sake of card image logic
            commonCards.initialize(false, false, enableUnknownCards);
            variantCards.initialize(false, false, enableUnknownCards);
        }

        if (this.tokenReader != null){
            final Map<String, CardRules> tokens = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            for (CardRules card : this.tokenReader.loadCards()) {
                if (null == card) continue;
                tokens.put(card.getNormalizedName(), card);
            }
            if (customTokenReader != null){
                for (CardRules card : customTokenReader.loadCards()){
                    if (null == card) continue;
                    card.setCustom();
                    tokens.put(card.getNormalizedName(), card);
                }
            }
            allTokens = new TokenDb(tokens, editions);
        } else {
            allTokens = null;
        }

        //initialize setLookup
        if (FileUtil.isDirectoryWithFiles(setLookupFolder)){
            for (File f : Objects.requireNonNull(new File(setLookupFolder).listFiles())){
                if (f.isFile()) {
                    setLookup.put(f.getName().replace(".txt",""), FileUtil.readFile(f));
                }
            }
        }
    }

    public static StaticData instance() {
        return lastInstance;
    }

    public Map<String, List<String>> getSetLookup() {
        return setLookup;
    }

    public final CardEdition.Collection getEditions() {
        return this.editions;
    }

    private List<CardEdition> sortedEditions;
    public final List<CardEdition> getSortedEditions() {
        if (sortedEditions == null) {
            sortedEditions = new ArrayList<>();
            for (CardEdition set : editions) {
                sortedEditions.add(set);
            }
            Collections.sort(sortedEditions);
            Collections.reverse(sortedEditions); //put newer sets at the top
        }
        return sortedEditions;
    }

    private TreeMap<CardEdition.Type, List<CardEdition>> editionsTypeMap;
    public final Map<CardEdition.Type, List<CardEdition>> getEditionsTypeMap() {
        if (editionsTypeMap == null) {
            editionsTypeMap = new TreeMap<>();
            for (CardEdition.Type editionType : CardEdition.Type.values()) {
                editionsTypeMap.put(editionType, new ArrayList<>());
            }
            for (CardEdition edition : this.getSortedEditions()) {
                CardEdition.Type key = edition.getType();
                List<CardEdition> editionsOfType = editionsTypeMap.get(key);
                editionsOfType.add(edition);
            }
        }
        return editionsTypeMap;
    }

    public CardEdition getCardEdition(String setCode) {
        CardEdition edition = this.editions.get(setCode);
        return edition;
    }

    public PaperCard getOrLoadCommonCard(String cardName, String setCode, int artIndex, boolean foil) {
        PaperCard card = commonCards.getCard(cardName, setCode, artIndex);
        if (card == null) {
            attemptToLoadCard(cardName, setCode);
            card = commonCards.getCard(cardName, setCode, artIndex);
        }
        if (card == null)
            card = commonCards.getCard(cardName, setCode);
        if (card == null)
            return null;
        return foil ? card.getFoiled() : card;
    }

    public void attemptToLoadCard(String cardName) {
        this.attemptToLoadCard(cardName, null);
    }
    public void attemptToLoadCard(String cardName, String setCode) {
        CardRules rules = cardReader.attemptToLoadCard(cardName);
        if (rules != null) {
            if (rules.isVariant()) {
                variantCards.loadCard(cardName, setCode, rules);
            } else {
                commonCards.loadCard(cardName, setCode, rules);
            }
        }
    }

    /**
     * Retrieve a PaperCard by looking at all available card databases;
     * @param cardName The name of the card
     * @param setCode The card Edition code
     * @param collectorNumber Card's collector Number
     * @return PaperCard instance found in one of the available CardDb databases, or <code>null</code> if not found.
     */
    public PaperCard fetchCard(final String cardName, final String setCode, final String collectorNumber) {
        PaperCard card = null;
        for (CardDb db : this.getAvailableDatabases().values()) {
            card = db.getCard(cardName, setCode, collectorNumber);
            if (card != null)
                break;
        }
        return card;
    }

    /**
     * Attempt to retrieve a Card from a target Card Edition if found in any available card database.
     * Note: Collector Number and Art Index will be used in a mutual exclusive fashion, that is:
     * collector number will be tried first, and then artIndex will be used in alternative.
     * If neither of those would correspond to any card in the database (due to incorrect value), the method will
     * always attempt a last try by just using card name and set.
     * @param cardName Card Name
     * @param edition CardEdition instance to fetch the card from.
     * @param collectorNumber Card Collector Number.
     * @param artIndex Card Art Index. This value will not be considered if it exceeds the Maximum Art Index value
     *                 supported for the given card in the target Card Edition.
     * @param isFoil Flag determining whether requested card should be foil or not.
     * @return <code>null</code> if no card can be found with the given search parameters.
     */
    public PaperCard getCardFromSet(final String cardName, final CardEdition edition,
                                    final String collectorNumber, final int artIndex, boolean isFoil) {
        CardDb.CardRequest cr = CardDb.CardRequest.fromString(cardName);  // accounts for any foil request ending with+
        cr.isFoil = cr.isFoil || isFoil;
        CardDb targetDb = this.matchTargetCardDb(cr.cardName);
        if (targetDb == null)
            return null;
        // Try with collector number first
        PaperCard result = targetDb.getCardFromSet(cardName, edition, collectorNumber, cr.isFoil);
        if (result == null && !collectorNumber.equals(IPaperCard.NO_COLLECTOR_NUMBER)) {
            if (artIndex != IPaperCard.NO_ART_INDEX) {
                // So here we know cardName exists (checked before invoking this method)
                // and also a Collector Number was specified.
                // The only case we would reach this point is either due to a wrong edition-card match
                // (later resulting in Unknown card - e.g. "Counterspell|FEM") or due to the fact that
                // art Index was specified instead of collector number! Let's give it a go with that
                // but only if artIndex is not NO_ART_INDEX (e.g. collectorNumber = "*32")
                int maxArtForCard = targetDb.getMaxArtIndex(cardName);
                if (artIndex <= maxArtForCard) {
                    // if collNr was "78", it's hardly an artIndex. It was just the wrong collNr for the requested card
                    result = targetDb.getCardFromSet(cardName, edition, artIndex, cr.isFoil);
                }
            }
            if (result == null) {
                // Last chance, try without collector number and see if any match is found
                result = targetDb.getCardFromSet(cardName, edition, cr.isFoil);
            }
        }
        return result;
    }

    /**
     * Retrieves a card from supportedEditions considering current default Card Art Preference,
     * and any possible constraint imposed on Game format (allowed sets) or edition release date.
     * @param cardName Name of the card to match
     * @param isFoil Whether the requested card should be foil.
     * @param artPreference The Card Art Preference to use
     * @param allowedSetCodes List of allowed set codes (if any)
     * @param releasedBefore Any constraint on release date for matched editions. If passed,
     *                       only sets released before the given date (if any) will be considered.
     * @return PaperCard matched in any available dataset, <code>null</code> if no card is found.
     */
    public PaperCard getCardFromSupportedEditions(final String cardName, boolean isFoil,
                                                  CardDb.CardArtPreference artPreference,
                                                  List<String> allowedSetCodes, Date releasedBefore) {
        CardDb.CardRequest cr = CardDb.CardRequest.fromString(cardName);  // accounts for any foil request ending with+
        isFoil = cr.isFoil || isFoil;
        CardDb targetDb = this.matchTargetCardDb(cr.cardName);
        if (targetDb == null)
            return null;
        Predicate<PaperCard> filter = null;
        if (allowedSetCodes != null)
            filter = (Predicate<PaperCard>) targetDb.isLegal(allowedSetCodes);
        PaperCard result;
        String cardRequest = CardDb.CardRequest.compose(cardName, isFoil);
        if (releasedBefore != null) {
            result = targetDb.getCardFromEditionsReleasedBefore(cardRequest, artPreference, releasedBefore, filter);
            if (result == null)
                result = targetDb.getCardFromEditions(cardRequest, artPreference, filter);
        } else
            result = targetDb.getCardFromEditions(cardRequest, artPreference, filter);
        return result;
    }

    private CardDb matchTargetCardDb(final String cardName) {
        // NOTE: any foil request in cardName is NOT taken into account here.
        // It's a private method, so it's a fair assumption.
        for (CardDb targetDb : this.getAvailableDatabases().values()){
            if (targetDb.contains(cardName))
                return targetDb;
        }
        return null;
    }

    /**
     * Determines whether the input String corresponds to an MTG Card Name (in any available card database)
     * @param cardName Name of the Card to verify (CASE SENSITIVE)
     * @return True if a card with the given input string can be found. False otherwise.
     */
    public boolean isMTGCard(final String cardName) {
        if (cardName == null || cardName.trim().length() == 0)
            return false;
        CardDb.CardRequest cr = CardDb.CardRequest.fromString(cardName);  // accounts for any foil request ending with +
        return this.commonCards.contains(cr.cardName) || this.variantCards.contains(cr.cardName);
    }

    /** @return {@link forge.util.storage.IStorage}<{@link forge.item.SealedProduct.Template}> */
    public final IStorage<SealedProduct.Template> getTournamentPacks() {
        if (tournaments == null)
            tournaments = new StorageBase<>("Starter sets", new SealedProduct.Template.Reader(new File(blockDataFolder, "starters.txt")));
        return tournaments;
    }

    /** @return {@link forge.util.storage.IStorage}<{@link forge.item.SealedProduct.Template}> */
    public final IStorage<SealedProduct.Template> getBoosters() {
        if (boosters == null)
            boosters = new StorageBase<>("Boosters", editions.getBoosterGenerator());
        return boosters;
    }

    public final IStorage<SealedProduct.Template> getSpecialBoosters() {
        if (specialBoosters == null)
            specialBoosters = new StorageBase<>("Special boosters", new SealedProduct.Template.Reader(new File(blockDataFolder, "boosters-special.txt")));
        return specialBoosters;
    }

    public IStorage<PrintSheet> getPrintSheets() {
        if (printSheets == null)
            printSheets = PrintSheet.initializePrintSheets(new File(blockDataFolder, "printsheets.txt"), getEditions());
        return printSheets;
    }

    /**
     * Get a database of all non-variant cards
     * @return
     */
    public CardDb getCommonCards() {
        return commonCards;
    }

    public CardDb getVariantCards() {
        return variantCards;
    }

    public Map<String, CardDb> getAvailableDatabases(){
        Map<String, CardDb> databases = new LinkedHashMap<>();  // to process dbs in this exact order
        databases.put("Common", commonCards);
        databases.put("Variant", variantCards);
        return databases;
    }

    public List<String> getBlockLands() {
        return blocksLandCodes;
    }

    public TokenDb getAllTokens() { return allTokens; }

    public boolean allowCustomCardsInDecksConformance() {
        return this.allowCustomCardsInDecksConformance;
    }


    public void setStandardPredicate(Predicate<PaperCard> standardPredicate) { this.standardPredicate = standardPredicate; }

    public void setPioneerPredicate(Predicate<PaperCard> pioneerPredicate) { this.pioneerPredicate = pioneerPredicate; }

    public void setModernPredicate(Predicate<PaperCard> modernPredicate) { this.modernPredicate = modernPredicate; }

    public void setCommanderPredicate(Predicate<PaperCard> commanderPredicate) { this.commanderPredicate = commanderPredicate; }

    public void setOathbreakerPredicate(Predicate<PaperCard> oathbreakerPredicate) { this.oathbreakerPredicate = oathbreakerPredicate; }

    public void setBrawlPredicate(Predicate<PaperCard> brawlPredicate) { this.brawlPredicate = brawlPredicate; }

    public Predicate<PaperCard> getStandardPredicate() { return standardPredicate; }
    
    public Predicate<PaperCard> getPioneerPredicate() { return pioneerPredicate; }

    public Predicate<PaperCard> getModernPredicate() { return modernPredicate; }

    public Predicate<PaperCard> getCommanderPredicate() { return commanderPredicate; }

    public Predicate<PaperCard> getOathbreakerPredicate() { return oathbreakerPredicate; }

    public Predicate<PaperCard> getBrawlPredicate() { return brawlPredicate; }

    /**
     * Get an alternative card print for the given card wrt. the input setReleaseDate.
     * The reference release date will be used to retrieve the alternative art, according
     * to the Card Art Preference settings.
     *
     * Note: if input card is Foil, and an alternative card art is found, it will be returned foil too!
     *
     * @param card Input Reference Card
     * @param setReleaseDate reference set release date
     * @return Alternative Card Art (from a different edition) of input card, or null if not found.
     */
    public PaperCard getAlternativeCardPrint(PaperCard card, final Date setReleaseDate) {
        boolean isCardArtPreferenceLatestArt = this.cardArtPreferenceIsLatest();
        boolean cardArtPreferenceHasFilter = this.isCoreExpansionOnlyFilterSet();
        return this.getAlternativeCardPrint(card, setReleaseDate, isCardArtPreferenceLatestArt,
                                            cardArtPreferenceHasFilter, null);
    }

    /**
     * Retrieve an alternative card print for a given card, and the input reference set release date.
     * The <code>setReleaseDate</code> will be used depending on the desired Card Art Preference policy to apply
     * when looking for alternative card, namely <code>Latest Art</code> and <i>with</i> or <i>without</i> filters
     * on editions.
     *
     * In more details:
     * - If card art preference is Latest Art first, the alternative card print will be chosen from
     * the first edition that has been released **after** the reference date.
     * - Conversely, if card art preference is Original Art first, the alternative card print will be
     * chosen from the first edition that has been released **before** the reference date.
     *
     * The rationale behind this strategy is to select an alternative card print from the lower-bound extreme
     * (upper-bound extreme) among the latest (original) editions where the card can be found.
     *
     * @param card  The instance of <code>PaperCard</code> to look for an alternative print
     * @param setReleaseDate  The reference release date used to control the search for alternative card print.
     *                        The chose candidate will be gathered from an edition printed before (upper bound) or
     *                        after (lower bound) the reference set release date.
     * @param isCardArtPreferenceLatestArt  Determines whether "Latest Art" Card Art preference should be used
     *                                      when looking for an alternative candidate print.
     * @param cardArtPreferenceHasFilter    Determines whether the search should only consider
     *                                      Core, Expansions, or Reprints sets when looking for alternative candidates.
     * @return  an instance of <code>PaperCard</code> that is the selected alternative candidate, or <code>null</code>
     * if None could be found.
     */
    public PaperCard getAlternativeCardPrint(PaperCard card, Date setReleaseDate,
                                             boolean isCardArtPreferenceLatestArt,
                                             boolean cardArtPreferenceHasFilter, List<String> allowedSetCodes) {
        Date searchReferenceDate = getReferenceDate(setReleaseDate, isCardArtPreferenceLatestArt);
        CardDb.CardArtPreference searchCardArtStrategy = getSearchStrategyForAlternativeCardArt(isCardArtPreferenceLatestArt,
                                                                          cardArtPreferenceHasFilter);
        return searchAlternativeCardCandidate(card, isCardArtPreferenceLatestArt, searchReferenceDate,
                                              searchCardArtStrategy, allowedSetCodes);
    }

    /**
     * This method extends the default <code>getAlternativeCardPrint</code> with extra settings to be used for
     * alternative card print.
     *
     * <p>
     * These options for Alternative Card Print make sense as part of the harmonisation/theme-matching process for
     * cards in Deck Sections (i.e. CardPool). In fact, the values of the provided flags for alternative print
     * for a single card will be determined according to whole card pool (Deck section) the card appears in.
     *
     * @param card  The instance of <code>PaperCard</code> to look for an alternative print
     * @param setReleaseDate  The reference release date used to control the search for alternative card print.
     *                        The chose candidate will be gathered from an edition printed before (upper bound) or
     *                        after (lower bound) the reference set release date.
     * @param isCardArtPreferenceLatestArt  Determines whether or not "Latest Art" Card Art preference should be used
     *                                      when looking for an alternative candidate print.
     * @param cardArtPreferenceHasFilter    Determines whether or not the search should only consider
     *                                      Core, Expansions, or Reprints sets when looking for alternative candidates.
     * @param preferCandidatesFromExpansionSets Whenever the selected Card Art Preference has filter, try to get
     *                                          prefer candidates from Expansion Sets over those in Core or Reprint
     *                                          Editions (whenever possible)
     *                                          e.g. Necropotence from Ice Age rather than 5th Edition (w/ Latest=false)
     * @param preferModernFrame  If True, Modern Card Frame will be preferred over Old Frames.
     * @return an instance of <code>PaperCard</code> that is the selected alternative candidate, or <code>null</code>
     *          if None could be found.
     */
    public PaperCard getAlternativeCardPrint(PaperCard card, Date setReleaseDate, boolean isCardArtPreferenceLatestArt,
                                             boolean cardArtPreferenceHasFilter,
                                             boolean preferCandidatesFromExpansionSets, boolean preferModernFrame) {
        return getAlternativeCardPrint(card, setReleaseDate, isCardArtPreferenceLatestArt, cardArtPreferenceHasFilter,
                                        preferCandidatesFromExpansionSets, preferModernFrame, null);
    }

    /**
     * This method extends the default <code>getAlternativeCardPrint</code> with extra settings to be used for
     * alternative card print.
     *
     * <p>
     * These options for Alternative Card Print make sense as part of the harmonisation/theme-matching process for
     * cards in Deck Sections (i.e. CardPool). In fact, the values of the provided flags for alternative print
     * for a single card will be determined according to whole card pool (Deck section) the card appears in.
     *
     * @param card  The instance of <code>PaperCard</code> to look for an alternative print
     * @param setReleaseDate  The reference release date used to control the search for alternative card print.
     *                        The chose candidate will be gathered from an edition printed before (upper bound) or
     *                        after (lower bound) the reference set release date.
     * @param isCardArtPreferenceLatestArt  Determines whether or not "Latest Art" Card Art preference should be used
     *                                      when looking for an alternative candidate print.
     * @param cardArtPreferenceHasFilter    Determines whether or not the search should only consider
     *                                      Core, Expansions, or Reprints sets when looking for alternative candidates.
     * @param preferCandidatesFromExpansionSets Whenever the selected Card Art Preference has filter, try to get
     *                                          prefer candidates from Expansion Sets over those in Core or Reprint
     *                                          Editions (whenever possible)
     *                                          e.g. Necropotence from Ice Age rather than 5th Edition (w/ Latest=false)
     * @param preferModernFrame  If True, Modern Card Frame will be preferred over Old Frames.
     * @param allowedSetCodes The list of the allowed set codes to consider when looking for alternative card art
     *                        candidates. If the list is not null and not empty, will be used in combination with the
     *                        <code>isLegal</code> predicate.
     * @see CardDb#isLegal(List<String>)
     * @return an instance of <code>PaperCard</code> that is the selected alternative candidate, or <code>null</code>
     *          if None could be found.
     */
    public PaperCard getAlternativeCardPrint(PaperCard card, Date setReleaseDate, boolean isCardArtPreferenceLatestArt,
                                             boolean cardArtPreferenceHasFilter,
                                             boolean preferCandidatesFromExpansionSets, boolean preferModernFrame,
                                             List<String> allowedSetCodes){
        PaperCard altCard = this.getAlternativeCardPrint(card, setReleaseDate, isCardArtPreferenceLatestArt,
                                                          cardArtPreferenceHasFilter, allowedSetCodes);
        if (altCard == null)
            return altCard;
        // from here on, we're sure we do have a candidate already!

        /* Try to refine selection by getting one candidate with frame matching current
           Card Art Preference (that is NOT the lookup strategy!)*/
        PaperCard refinedAltCandidate = this.tryToGetCardPrintWithMatchingFrame(altCard, isCardArtPreferenceLatestArt,
                                                                                cardArtPreferenceHasFilter,
                                                                                preferModernFrame, allowedSetCodes);
        if (refinedAltCandidate != null)
            altCard = refinedAltCandidate;

        if (cardArtPreferenceHasFilter && preferCandidatesFromExpansionSets){
            /* Now try to refine selection by looking for an alternative choice extracted from an Expansion Set.
               NOTE: At this stage, any future selection should be already compliant with previous filter on
               Card Frame (if applied) given that we'll be moving either UP or DOWN the timeline of Card Edition */
            refinedAltCandidate = this.tryToGetCardPrintFromExpansionSet(altCard, isCardArtPreferenceLatestArt,
                                                                            preferModernFrame, allowedSetCodes);
            if (refinedAltCandidate != null)
                altCard = refinedAltCandidate;
        }
        return altCard;
    }

    private PaperCard searchAlternativeCardCandidate(PaperCard card, boolean isCardArtPreferenceLatestArt,
                                                     Date searchReferenceDate,
                                                     CardDb.CardArtPreference searchCardArtStrategy,
                                                     List<String> allowedSetCodes) {
        // Note: this won't apply to Custom Nor Variant Cards, so won't bother including it!
        CardDb cardDb = this.commonCards;
        String cardName = card.getName();
        int artIndex = card.getArtIndex();
        PaperCard altCard = null;
        Predicate<PaperCard> filter = null;
        if (allowedSetCodes != null && !allowedSetCodes.isEmpty())
            filter = (Predicate<PaperCard>) cardDb.isLegal(allowedSetCodes);

        if (isCardArtPreferenceLatestArt) {  // RELEASED AFTER REFERENCE DATE
            altCard = cardDb.getCardFromEditionsReleasedAfter(cardName, searchCardArtStrategy, artIndex,
                                                                searchReferenceDate, filter);
            if (altCard == null)  // relax artIndex condition
                altCard = cardDb.getCardFromEditionsReleasedAfter(cardName, searchCardArtStrategy,
                                                                    searchReferenceDate, filter);
        } else {  // RELEASED BEFORE REFERENCE DATE
            altCard = cardDb.getCardFromEditionsReleasedBefore(cardName, searchCardArtStrategy, artIndex,
                                                                searchReferenceDate, filter);
            if (altCard == null)  // relax artIndex constraint
                altCard = cardDb.getCardFromEditionsReleasedBefore(cardName, searchCardArtStrategy,
                                                                    searchReferenceDate, filter);
        }
        if (altCard == null)
            return null;
        return card.isFoil() ? altCard.getFoiled() : altCard;
    }

    private Date getReferenceDate(Date setReleaseDate, boolean isCardArtPreferenceLatestArt) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(setReleaseDate);
        if (isCardArtPreferenceLatestArt)
            cal.add(Calendar.DATE, -2);  // go two days behind to also include the original reference set
        else
            cal.add(Calendar.DATE, 2);  // go two days ahead to also include the original reference set
        return cal.getTime();
    }

    private CardDb.CardArtPreference getSearchStrategyForAlternativeCardArt(boolean isCardArtPreferenceLatestArt, boolean cardArtPreferenceHasFilter) {
        CardDb.CardArtPreference lookupStrategy;
        if (isCardArtPreferenceLatestArt) {
            // Get Lower bound (w/ Original Art and Edition Released AFTER Pivot Date)
            if (cardArtPreferenceHasFilter)
                lookupStrategy = CardDb.CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY;  // keep the filter
            else
                lookupStrategy = CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS;
        } else {
            // Get Upper bound (w/ Latest Art and Edition released BEFORE Pivot Date)
            if (cardArtPreferenceHasFilter)
                lookupStrategy = CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY;  // keep the filter
            else
                lookupStrategy = CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS;
        }
        return lookupStrategy;
    }

    private PaperCard tryToGetCardPrintFromExpansionSet(PaperCard altCard,
                                                        boolean isCardArtPreferenceLatestArt,
                                                        boolean preferModernFrame,
                                                        List<String> allowedSetCodes) {
        CardEdition altCardEdition = editions.get(altCard.getEdition());
        if (altCardEdition.getType() == CardEdition.Type.EXPANSION)
            return null;  // Nothing to do here!
        boolean searchStrategyFlag = (isCardArtPreferenceLatestArt == preferModernFrame) == isCardArtPreferenceLatestArt;
        // We'll force the filter on to strictly reduce the alternative candidates retrieved to those
        // from Expansions, Core, and Reprint sets.
        CardDb.CardArtPreference searchStrategy = getSearchStrategyForAlternativeCardArt(searchStrategyFlag,
                                                                                         true);
        PaperCard altCandidate = altCard;
        while (altCandidate != null) {
            Date referenceDate = editions.get(altCandidate.getEdition()).getDate();
            altCandidate = this.searchAlternativeCardCandidate(altCandidate, preferModernFrame,
                                                                referenceDate, searchStrategy, allowedSetCodes);
            if (altCandidate != null) {
                CardEdition altCandidateEdition = editions.get(altCandidate.getEdition());
                if (altCandidateEdition.getType() == CardEdition.Type.EXPANSION)
                    break;
            }
        }
        // this will be either a true candidate or null if the cycle broke because of no other suitable candidates
        return altCandidate;
    }

    private PaperCard tryToGetCardPrintWithMatchingFrame(PaperCard altCard,
                                                         boolean isCardArtPreferenceLatestArt,
                                                         boolean cardArtHasFilter,
                                                         boolean preferModernFrame, List<String> allowedSetCodes) {
        CardEdition altCardEdition = editions.get(altCard.getEdition());
        boolean frameIsCompliantAlready = (altCardEdition.isModern() == preferModernFrame);
        if (frameIsCompliantAlready)
            return null;  // Nothing to do here!
        boolean searchStrategyFlag = (isCardArtPreferenceLatestArt == preferModernFrame) == isCardArtPreferenceLatestArt;
        CardDb.CardArtPreference searchStrategy = getSearchStrategyForAlternativeCardArt(searchStrategyFlag,
                                                                                         cardArtHasFilter);
        PaperCard altCandidate = altCard;
        while (altCandidate != null) {
            Date referenceDate = editions.get(altCandidate.getEdition()).getDate();
            altCandidate = this.searchAlternativeCardCandidate(altCandidate, preferModernFrame,
                                                               referenceDate, searchStrategy, allowedSetCodes);
            if (altCandidate != null) {
                CardEdition altCandidateEdition = editions.get(altCandidate.getEdition());
                if (altCandidateEdition.isModern() == preferModernFrame)
                    break;
            }
        }
        // this will be either a true candidate or null if the cycle broke because of no other suitable candidates
        return altCandidate;
    }

    /**
     * Get the Art Count for a given <code>PaperCard</code> looking for a candidate in all
     * available databases.
     *
     * @param card Instance of target <code>PaperCard</code>
     * @return The number of available arts for the given card in the corresponding set, or 0 if not found.
     */
    public int getCardArtCount(PaperCard card) {
        Collection<CardDb> databases = this.getAvailableDatabases().values();
        for (CardDb db: databases){
            int artCount = db.getArtCount(card.getName(), card.getEdition());
            if (artCount > 0)
                return artCount;
        }
        return 0;
    }

    public boolean getFilteredHandsEnabled() {
        return filteredHandsEnabled;
    }
    public void setFilteredHandsEnabled(boolean filteredHandsEnabled) {
        this.filteredHandsEnabled = filteredHandsEnabled;
    }

    public void setMulliganRule(MulliganDefs.MulliganRule rule) {
        mulliganRule = rule;
    }
    public MulliganDefs.MulliganRule getMulliganRule() {
        return mulliganRule;
    }

    public void setCardArtPreference(boolean latestArt, boolean coreExpansionOnly) {
        this.commonCards.setCardArtPreference(latestArt, coreExpansionOnly);
        this.variantCards.setCardArtPreference(latestArt, coreExpansionOnly);
    }

    public String getCardArtPreferenceName() {
        return this.commonCards.getCardArtPreference().toString();
    }

    public CardDb.CardArtPreference getCardArtPreference() {
        return this.commonCards.getCardArtPreference();
    }

    public CardDb.CardArtPreference getCardArtPreference(boolean latestArt, boolean coreExpansionOnly) {
        if (latestArt) {
            return coreExpansionOnly ? CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY : CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS;
        }
        return coreExpansionOnly ? CardDb.CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY : CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS;
    }


    public boolean isCoreExpansionOnlyFilterSet() { return this.commonCards.getCardArtPreference().filterSets; }

    public boolean cardArtPreferenceIsLatest() {
        return this.commonCards.getCardArtPreference().latestFirst;
    }

    // === MOBILE APP Alternative Methods (using String Labels, not yet localised!!) ===
    // Note: only used in mobile
    public String[] getCardArtAvailablePreferences() {
        CardDb.CardArtPreference[] preferences = CardDb.CardArtPreference.values();
        String[] preferences_avails = new String[preferences.length];
        for (int i = 0; i < preferences.length; i++)
            preferences_avails[i] = prettifyCardArtPreferenceName(preferences[i]);
        return preferences_avails;
    }
    public Pair<Integer, Integer> audit(StringBuffer noImageFound, StringBuffer cardNotImplemented) {
        int missingCount = 0;
        int notImplementedCount = 0;
        for (CardEdition e : editions) {
            if (CardEdition.Type.FUNNY.equals(e.getType()))
                continue;
            boolean nifHeader = false;
            boolean cniHeader = false;
            boolean tokenHeader = false;

            String imagePath;
            int artIndex = 1;

            HashMap<String, Pair<Boolean, Integer>> cardCount = new HashMap<>();
            for (CardEdition.CardInSet c : e.getAllCardsInSet()) {
                if (cardCount.containsKey(c.name)) {
                    cardCount.put(c.name, Pair.of(c.collectorNumber.startsWith("F"), cardCount.get(c.name).getRight() + 1));
                } else {
                    cardCount.put(c.name, Pair.of(c.collectorNumber.startsWith("F"), 1));
                }
            }

            // loop through the cards in this edition, considering art variations...
            for (Map.Entry<String, Pair<Boolean, Integer>> entry : cardCount.entrySet()) {
                String c = entry.getKey();
                artIndex = entry.getValue().getRight();

                PaperCard cp = getCommonCards().getCard(c, e.getCode(), artIndex);
                if (cp == null) {
                    cp = getVariantCards().getCard(c, e.getCode(), artIndex);
                }

                if (cp == null) {
                    if (entry.getValue().getLeft()) //skip funny cards
                        continue;
                    if (!loadNonLegalCards && CardEdition.Type.FUNNY.equals(e.getType()))
                        continue;
                    if (!cniHeader) {
                        cardNotImplemented.append("\nEdition: ").append(e.getName()).append(" ").append("(").append(e.getCode()).append("/").append(e.getCode2()).append(")\n");
                        cniHeader = true;
                    }
                    cardNotImplemented.append(" ").append(c).append("\n");
                    notImplementedCount++;
                    continue;
                }

                // check the front image
                imagePath = ImageUtil.getImageRelativePath(cp, "", true, false);
                if (imagePath != null) {
                    File file = ImageKeys.getImageFile(imagePath);
                    if (file == null && ImageKeys.hasSetLookup(imagePath))
                        file = ImageKeys.setLookUpFile(imagePath, imagePath+"border");
                    if (file == null) {
                        if (!nifHeader) {
                            noImageFound.append("Edition: ").append(e.getName()).append(" ").append("(").append(e.getCode()).append("/").append(e.getCode2()).append(")\n");
                            nifHeader = true;
                        }
                        noImageFound.append(" ").append(imagePath).append("\n");
                        missingCount++;
                    }
                }

                // check the back face
                if (cp.hasBackFace()) {
                    imagePath = ImageUtil.getImageRelativePath(cp, "back", true, false);
                    if (imagePath != null) {
                        File file = ImageKeys.getImageFile(imagePath);
                        if (file == null && ImageKeys.hasSetLookup(imagePath))
                            file = ImageKeys.setLookUpFile(imagePath, imagePath+"border");
                        if (file == null) {
                            if (!nifHeader) {
                                noImageFound.append("Edition: ").append(e.getName()).append(" ").append("(").append(e.getCode()).append("/").append(e.getCode2()).append(")\n");
                                nifHeader = true;
                            }
                            noImageFound.append(" ").append(imagePath).append("\n");
                            missingCount++;
                        }
                    }
                }
            }

            // TODO: Audit token images here...
            for(Map.Entry<String, Integer> tokenEntry : e.getTokens().entrySet()) {
                String name = tokenEntry.getKey();
                artIndex = tokenEntry.getValue();
                try {
                    PaperToken token = getAllTokens().getToken(name, e.getCode());
                    if (token == null) {
                        continue;
                    }

                    for(int i = 0; i < artIndex; i++) {
                        String imgKey = token.getImageKey(i);
                        File file = ImageKeys.getImageFile(imgKey);
                        if (file == null) {
                            if (!nifHeader) {
                                noImageFound.append("Edition: ").append(e.getName()).append(" ").append("(").append(e.getCode()).append("/").append(e.getCode2()).append(")\n");
                                nifHeader = true;
                            }
                            if (!tokenHeader) {
                                noImageFound.append("\nTOKENS\n");
                                tokenHeader = true;
                            }
                            noImageFound.append(" ").append(token.getImageFilename(i + 1)).append("\n");
                            missingCount++;
                        }
                    }
                } catch(Exception ex) {
                    System.out.println("No Token found: " + name + " in " + e.getName());
                }
            }
            if (nifHeader)
                noImageFound.append("\n");
        }

        String totalStats = "Missing images: " + missingCount + "\nUnimplemented cards: " + notImplementedCount + "\n";
        cardNotImplemented.append("\n-----------\n");
        cardNotImplemented.append(totalStats);
        cardNotImplemented.append("-----------\n\n");

        noImageFound.append(cardNotImplemented); // combine things together...
        return Pair.of(missingCount, notImplementedCount);
    }

    private String prettifyCardArtPreferenceName(CardDb.CardArtPreference preference) {
        StringBuilder label = new StringBuilder();
        String[] fullNames = preference.toString().split("_");
        for (String name : fullNames)
            label.append(TextUtil.capitalize(name.toLowerCase())).append(" ");
        return label.toString().trim();
    }

    public void setCardArtPreference(String artPreference) {
        this.commonCards.setCardArtPreference(artPreference);
        this.variantCards.setCardArtPreference(artPreference);
    }

    public boolean isEnabledCardArtSmartSelection() {
        return this.enableSmartCardArtSelection;
    }
    public void setEnableSmartCardArtSelection(boolean isEnabled) {
        this.enableSmartCardArtSelection = isEnabled;
    }

    public boolean isRebalanced(String name)
    {
        if (!name.startsWith("A-")) {
            return false;
        }
        for(PaperCard pc : this.getCommonCards().getAllCards(name)) {
            CardEdition e = this.editions.get(pc.getEdition());
            if (e != null && e.isRebalanced(name)) {
                return true;
            }
        }
        return false;
    }
}
