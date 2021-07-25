package forge;

import com.google.common.base.Predicate;
import forge.card.CardDb;
import forge.card.CardEdition;
import forge.card.CardRules;
import forge.card.PrintSheet;
import forge.item.BoosterBox;
import forge.item.FatPack;
import forge.item.PaperCard;
import forge.item.SealedProduct;
import forge.token.TokenDb;
import forge.util.TextUtil;
import forge.util.storage.IStorage;
import forge.util.storage.StorageBase;

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
    private final CardDb customCards;
    private final TokenDb allTokens;
    private final CardEdition.Collection editions;
    private final CardEdition.Collection customEditions;

    private Predicate<PaperCard> standardPredicate;
    private Predicate<PaperCard> brawlPredicate;
    private Predicate<PaperCard> pioneerPredicate;
    private Predicate<PaperCard> modernPredicate;
    private Predicate<PaperCard> commanderPredicate;
    private Predicate<PaperCard> oathbreakerPredicate;

    private boolean filteredHandsEnabled = false;

    private MulliganDefs.MulliganRule mulliganRule = MulliganDefs.getDefaultRule();

    private boolean enableCustomCardsInDecks;  // default
    private boolean enableSmartCardArtSelection;

    // Loaded lazily:
    private IStorage<SealedProduct.Template> boosters;
    private IStorage<SealedProduct.Template> specialBoosters;
    private IStorage<SealedProduct.Template> tournaments;
    private IStorage<FatPack.Template> fatPacks;
    private IStorage<BoosterBox.Template> boosterBoxes;
    private IStorage<PrintSheet> printSheets;

    private static StaticData lastInstance = null;

    public StaticData(CardStorageReader cardReader, CardStorageReader customCardReader, String editionFolder, String customEditionsFolder, String blockDataFolder, String cardArtPreference, boolean enableUnknownCards, boolean loadNonLegalCards) {
        this(cardReader, null, customCardReader, editionFolder, customEditionsFolder, blockDataFolder, cardArtPreference, enableUnknownCards, loadNonLegalCards, false);
    }

    public StaticData(CardStorageReader cardReader, CardStorageReader tokenReader, CardStorageReader customCardReader, String editionFolder, String customEditionsFolder, String blockDataFolder, String cardArtPreference, boolean enableUnknownCards, boolean loadNonLegalCards, boolean enableCustomCardsInDecks){
        this(cardReader, tokenReader, customCardReader, editionFolder, customEditionsFolder, blockDataFolder, cardArtPreference, enableUnknownCards, loadNonLegalCards, enableCustomCardsInDecks, false);
    }

    public StaticData(CardStorageReader cardReader, CardStorageReader tokenReader, CardStorageReader customCardReader, String editionFolder, String customEditionsFolder, String blockDataFolder, String cardArtPreference, boolean enableUnknownCards, boolean loadNonLegalCards, boolean enableCustomCardsInDecks, boolean enableSmartCardArtSelection) {
        this.cardReader = cardReader;
        this.tokenReader = tokenReader;
        this.editions = new CardEdition.Collection(new CardEdition.Reader(new File(editionFolder)));
        this.blockDataFolder = blockDataFolder;
        this.customCardReader = customCardReader;
        this.customEditions = new CardEdition.Collection(new CardEdition.Reader(new File(customEditionsFolder), true));
        this.enableCustomCardsInDecks = enableCustomCardsInDecks;
        this.enableSmartCardArtSelection = enableSmartCardArtSelection;
        lastInstance = this;
        List<String> funnyCards = new ArrayList<>();
        List<String> filtered = new ArrayList<>();

        {
            final Map<String, CardRules> regularCards = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            final Map<String, CardRules> variantsCards = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            final Map<String, CardRules> customizedCards = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            for (CardEdition e : editions) {
                if (e.getType() == CardEdition.Type.FUNNY || e.getBorderColor() == CardEdition.BorderColor.SILVER) {
                    for (CardEdition.CardInSet cis : e.getAllCardsInSet()) {
                        funnyCards.add(cis.name);
                    }
                }
            }

            for (CardRules card : cardReader.loadCards()) {
                if (null == card) continue;

                final String cardName = card.getName();

                if (!loadNonLegalCards && !card.getType().isBasicLand() && funnyCards.contains(cardName))
                    filtered.add(cardName);

                if (card.isVariant()) {
                    variantsCards.put(cardName, card);
                } else {
                    regularCards.put(cardName, card);
                }
            }
            if (customCardReader != null) {
                for (CardRules card : customCardReader.loadCards()) {
                    if (null == card) continue;

                    final String cardName = card.getName();
                    customizedCards.put(cardName, card);
                }
            }

            if (!filtered.isEmpty()) {
                Collections.sort(filtered);
            }

            commonCards = new CardDb(regularCards, editions, filtered, cardArtPreference);
            variantCards = new CardDb(variantsCards, editions, filtered, cardArtPreference);
            customCards = new CardDb(customizedCards, customEditions, filtered, cardArtPreference);

            //must initialize after establish field values for the sake of card image logic
            commonCards.initialize(false, false, enableUnknownCards);
            variantCards.initialize(false, false, enableUnknownCards);
            customCards.initialize(false, false, enableUnknownCards);
        }

        if (this.tokenReader != null){
            final Map<String, CardRules> tokens = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            for (CardRules card : this.tokenReader.loadCards()) {
                if (null == card) continue;

                tokens.put(card.getNormalizedName(), card);
            }
            allTokens = new TokenDb(tokens, editions);
        } else {
            allTokens = null;
        }
    }

    public static StaticData instance() {
        return lastInstance;
    }

    public final CardEdition.Collection getEditions() {
        return this.editions;
    }

    public final CardEdition.Collection getCustomEditions(){
        return this.customEditions;
    }


    private List<CardEdition> sortedEditions;
    public final List<CardEdition> getSortedEditions() {
        if (sortedEditions == null) {
            sortedEditions = new ArrayList<>();
            for (CardEdition set : editions) {
                sortedEditions.add(set);
            }
            if (customEditions.size() > 0){
                for (CardEdition set : customEditions) {
                    sortedEditions.add(set);
                }
            }
            Collections.sort(sortedEditions);
            Collections.reverse(sortedEditions); //put newer sets at the top
        }
        return sortedEditions;
    }

    private TreeMap<CardEdition.Type, List<CardEdition>> editionsTypeMap;
    public final Map<CardEdition.Type, List<CardEdition>> getEditionsTypeMap(){
        if (editionsTypeMap == null){
            editionsTypeMap = new TreeMap<>();
            for (CardEdition.Type editionType : CardEdition.Type.values()){
                editionsTypeMap.put(editionType, new ArrayList<>());
            }
            for (CardEdition edition : this.getSortedEditions()){
                CardEdition.Type key = edition.getType();
                List<CardEdition> editionsOfType = editionsTypeMap.get(key);
                editionsOfType.add(edition);
            }
        }
        return editionsTypeMap;
    }

    public CardEdition getCardEdition(String setCode){
        CardEdition edition = this.editions.get(setCode);
        if (edition == null)  // try custom editions
            edition = this.customEditions.get(setCode);
        return edition;
    }

    public PaperCard getOrLoadCommonCard(String cardName, String setCode, int artIndex, boolean foil) {
        PaperCard card = commonCards.getCard(cardName, setCode, artIndex);
        boolean isCustom = false;
        if (card == null) {
            attemptToLoadCard(cardName);
            card = commonCards.getCard(cardName, setCode, artIndex);
        }
        if (card == null) {
            card = commonCards.getCard(cardName, setCode);
        }
        if (card == null) {
            card = customCards.getCard(cardName, setCode, artIndex);
            if (card != null)
                isCustom = true;
        }
        if (card == null) {
            card = customCards.getCard(cardName, setCode);
            if (card != null)
                isCustom = true;
        }
        if (card == null) {
            return null;
        }
        if (isCustom)
            return foil ? card.getFoiled() : card;
        return foil ? card.getFoiled() : card;
    }

    public void attemptToLoadCard(String cardName){
        CardRules rules = cardReader.attemptToLoadCard(cardName);
        CardRules customRules = null;
        if (customCardReader != null) {
            customRules = customCardReader.attemptToLoadCard(cardName);
        }
        if (rules != null) {
            if (rules.isVariant()) {
                variantCards.loadCard(cardName, rules);
            } else {
                commonCards.loadCard(cardName, rules);
            }
        }
        if (customRules != null) {
            customCards.loadCard(cardName, customRules);
        }
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

    public CardDb getCommonCards() {
        return commonCards;
    }

    public CardDb getCustomCards() {
        return customCards;
    }

    public CardDb getVariantCards() {
        return variantCards;
    }

    public TokenDb getAllTokens() { return allTokens; }

    public boolean isEnableCustomCardsInDecks() {
        return this.enableCustomCardsInDecks;
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

    public void setFilteredHandsEnabled(boolean filteredHandsEnabled){
        this.filteredHandsEnabled = filteredHandsEnabled;
    }

    public PaperCard getAlternativeCardPrint(PaperCard card, final Date setReleasedBefore) {
        // NOTE this method forces the LATEST selection policy since we do always want to pick the
        // edition that is the closest in time to the release date
        CardDb.CardArtPreference artPref;
        if (this.cardArtPreferenceHasFilter())
            artPref = CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY;
        else
            artPref = CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS;
        PaperCard c = this.getCommonCards().getCardFromEditions(card.getName(), artPref, card.getArtIndex(), setReleasedBefore);
        // NOTE: if c is null, is necessarily due to the artIndex, so remove it!
        return c != null ? c : this.getCommonCards().getCardFromEditions(card.getName(), artPref, setReleasedBefore);
    }

    public boolean getFilteredHandsEnabled(){
        return filteredHandsEnabled;
    }

    public void setMulliganRule(MulliganDefs.MulliganRule rule) {
        mulliganRule = rule;
    }

    public MulliganDefs.MulliganRule getMulliganRule() {
        return mulliganRule;
    }

    public void setCardArtPreference(boolean latestArt, boolean coreExpansionOnly){
        this.commonCards.setCardArtPreference(latestArt, coreExpansionOnly);
        this.variantCards.setCardArtPreference(latestArt, coreExpansionOnly);
        this.customCards.setCardArtPreference(latestArt, coreExpansionOnly);
    }

    public String getCardArtPreference(){
        return this.commonCards.getCardArtPreference().toString();
    }

    public boolean cardArtPreferenceHasFilter(){
        return this.commonCards.getCardArtPreference().filterSets;
    }

    public boolean cardArtPreferenceIsLatest(){
        return this.commonCards.getCardArtPreference().latestFirst;
    }

    // === MOBILE APP Alternative Methods (using String Labels, not yet localised!!) ===
    // Note: only used in mobile
    public String[] getCardArtAvailablePreferences(){
        CardDb.CardArtPreference[] preferences = CardDb.CardArtPreference.values();
        String[] preferences_avails = new String[preferences.length];
        for (int i = 0; i < preferences.length; i++) {
            StringBuilder label = new StringBuilder();
            String[] fullNames = preferences[i].toString().split("_");
            for (String name : fullNames)
                label.append(TextUtil.capitalize(name.toLowerCase())).append(" ");
            preferences_avails[i] = label.toString().trim();
        }
        return preferences_avails;
    }
    public void setCardArtPreference(String artPreference){
        this.commonCards.setCardArtPreference(artPreference);
        this.variantCards.setCardArtPreference(artPreference);
        this.customCards.setCardArtPreference(artPreference);
    }

    //
    public boolean smartCardArtSelectionIsEnabled(){
        return this.enableSmartCardArtSelection;
    }
    public void setEnableSmartCardArtSelection(boolean isEnabled){
        this.enableSmartCardArtSelection = isEnabled;
    }

}
