package forge;

import com.google.common.base.Predicate;
import forge.card.CardDb;
import forge.card.CardEdition;
import forge.card.CardRules;
import forge.card.PrintSheet;
import forge.card.CardDb.CardRequest;
import forge.item.BoosterBox;
import forge.item.FatPack;
import forge.item.PaperCard;
import forge.item.SealedProduct;
import forge.token.TokenDb;
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

    // Loaded lazily:
    private IStorage<SealedProduct.Template> boosters;
    private IStorage<SealedProduct.Template> specialBoosters;
    private IStorage<SealedProduct.Template> tournaments;
    private IStorage<FatPack.Template> fatPacks;
    private IStorage<BoosterBox.Template> boosterBoxes;
    private IStorage<PrintSheet> printSheets;

    private static StaticData lastInstance = null;

    public StaticData(CardStorageReader cardReader, String editionFolder, String blockDataFolder, boolean enableUnknownCards) {
        this(cardReader, null, editionFolder, blockDataFolder, enableUnknownCards);
    }

    public StaticData(CardStorageReader cardReader, CardStorageReader tokenReader, String editionFolder, String blockDataFolder, boolean enableUnknownCards) {
        this.cardReader = cardReader;
        this.tokenReader = tokenReader;
        this.editions = new CardEdition.Collection(new CardEdition.Reader(new File(editionFolder)));
        this.blockDataFolder = blockDataFolder;
        lastInstance = this;

        {
            final Map<String, CardRules> regularCards = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            final Map<String, CardRules> variantsCards = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            for (CardRules card : cardReader.loadCards()) {
                if (null == card) continue;

                final String cardName = card.getName();
                if (card.isVariant()) {
                    variantsCards.put(cardName, card);
                } else {
                    regularCards.put(cardName, card);
                }
            }

            commonCards = new CardDb(regularCards, editions);
            variantCards = new CardDb(variantsCards, editions);

            //must initialize after establish field values for the sake of card image logic
            commonCards.initialize(false, false, enableUnknownCards);
            variantCards.initialize(false, false, enableUnknownCards);
        }

        {
            final Map<String, CardRules> tokens = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            for (CardRules card : tokenReader.loadCards()) {
                if (null == card) continue;

                tokens.put(card.getNormalizedName(), card);
            }
            allTokens = new TokenDb(tokens, editions);
        }
    }

    public static StaticData instance() {
        return lastInstance;
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

    public PaperCard getOrLoadCommonCard(String cardName, String setCode, int artIndex, boolean foil) {
        PaperCard card = commonCards.getCard(cardName, setCode, artIndex);
        if (card == null) {
            attemptToLoadCard(cardName, setCode);
            card = commonCards.getCard(cardName, setCode, artIndex);
        }
        if (card == null) {
            card = commonCards.getCard(cardName, setCode, -1);
        }
        if (card == null) {
            return null;
        }
        return foil ? commonCards.getFoiled(card) : card;
    }

    public void attemptToLoadCard(String encodedCardName, String setCode) {
        CardDb.CardRequest r = CardRequest.fromString(encodedCardName);
        String cardName = r.cardName;
        CardRules rules = cardReader.attemptToLoadCard(cardName, setCode);
        if (rules != null) {
            if (rules.isVariant()) {
                variantCards.loadCard(cardName, rules);
            } else {
                commonCards.loadCard(cardName, rules);
            }
        }
    }

    /** @return {@link forge.util.storage.IStorage}<{@link forge.item.SealedProduct.Template}> */
    public IStorage<FatPack.Template> getFatPacks() {
        if (fatPacks == null)
            fatPacks = new StorageBase<>("Fat packs", new FatPack.Template.Reader(blockDataFolder + "fatpacks.txt"));
        return fatPacks;
    }

    public IStorage<BoosterBox.Template> getBoosterBoxes() {
        if (boosterBoxes == null)
            boosterBoxes = new StorageBase<>("Booster boxes", new BoosterBox.Template.Reader(blockDataFolder + "boosterboxes.txt"));
        return boosterBoxes;
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
            printSheets = new StorageBase<>("Special print runs", new PrintSheet.Reader(new File(blockDataFolder, "printsheets.txt")));
        return printSheets;
    }

    public CardDb getCommonCards() {
        return commonCards;
    }

    public CardDb getVariantCards() {
        return variantCards;
    }

    public TokenDb getAllTokens() { return allTokens; }

    

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

    public PaperCard getCardByEditionDate(PaperCard card, Date editionDate) {

        PaperCard c = this.getCommonCards().getCardFromEdition(card.getName(), editionDate, CardDb.SetPreference.LatestCoreExp, card.getArtIndex());

        if (null != c) {
            return c;
        }

        c = this.getCommonCards().getCardFromEdition(card.getName(), editionDate, CardDb.SetPreference.LatestCoreExp, -1);

        if (null != c) {
            return c;
        }

        c = this.getCommonCards().getCardFromEdition(card.getName(), editionDate, CardDb.SetPreference.Latest, -1);

        if (null != c) {
            return c;
        }

        // I give up!
        return card;
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

}
