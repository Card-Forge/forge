package forge.quest.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import net.slightlymagic.braids.util.lambda.Lambda1;
import net.slightlymagic.maxmtg.Predicate;
import forge.MyRandom;
import forge.SetUtils;
import forge.card.BoosterGenerator;
import forge.card.BoosterUtils;
import forge.card.CardRarity;
import forge.card.CardSet;
import forge.deck.Deck;
import forge.item.BoosterPack;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.item.ItemPool;
import forge.item.ItemPoolView;

/**
 * This is a helper class to execute operations on QuestData. It has been
 * created to decrease complexity of questData class
 */
public final class QuestUtilCards {
    private final QuestData q;

    /**
     * Instantiates a new quest util cards.
     * 
     * @param qd
     *            the qd
     */
    public QuestUtilCards(final QuestData qd) {
        this.q = qd;
    }

    /**
     * Adds the basic lands.
     * 
     * @param pool
     *            the pool
     * @param nBasic
     *            the n basic
     * @param nSnow
     *            the n snow
     */
    public void addBasicLands(final ItemPool<InventoryItem> pool, final int nBasic, final int nSnow) {
        final CardDb db = CardDb.instance();
        pool.add(db.getCard("Forest", "M10"), nBasic);
        pool.add(db.getCard("Mountain", "M10"), nBasic);
        pool.add(db.getCard("Swamp", "M10"), nBasic);
        pool.add(db.getCard("Island", "M10"), nBasic);
        pool.add(db.getCard("Plains", "M10"), nBasic);

        pool.add(db.getCard("Snow-Covered Forest", "ICE"), nSnow);
        pool.add(db.getCard("Snow-Covered Mountain", "ICE"), nSnow);
        pool.add(db.getCard("Snow-Covered Swamp", "ICE"), nSnow);
        pool.add(db.getCard("Snow-Covered Island", "ICE"), nSnow);
        pool.add(db.getCard("Snow-Covered Plains", "ICE"), nSnow);
    }

    // adds 11 cards, to the current card pool
    // (I chose 11 cards instead of 15 in order to make things more challenging)

    /**
     * <p>
     * addCards.
     * </p>
     * 
     * @param fSets
     *            the f sets
     * @return the array list
     */
    public ArrayList<CardPrinted> addCards(final Predicate<CardPrinted> fSets) {
        final int nCommon = QuestPreferences.getNumCommon();
        final int nUncommon = QuestPreferences.getNumUncommon();
        final int nRare = QuestPreferences.getNumRare();

        final ArrayList<CardPrinted> newCards = new ArrayList<CardPrinted>();
        newCards.addAll(BoosterUtils.generateCards(fSets, nCommon, CardRarity.Common, null));
        newCards.addAll(BoosterUtils.generateCards(fSets, nUncommon, CardRarity.Uncommon, null));
        newCards.addAll(BoosterUtils.generateCards(fSets, nRare, CardRarity.Rare, null));

        this.addAllCards(newCards);
        return newCards;
    }

    /**
     * Adds the all cards.
     * 
     * @param newCards
     *            the new cards
     */
    public void addAllCards(final Iterable<CardPrinted> newCards) {
        for (final CardPrinted card : newCards) {
            this.addSingleCard(card);
        }
    }

    /**
     * Adds the single card.
     * 
     * @param card
     *            the card
     */
    public void addSingleCard(final CardPrinted card) {
        this.q.getCardPool().add(card);

        // register card into that list so that it would appear as a new one.
        this.q.getNewCardList().add(card);
    }

    private static final Predicate<CardPrinted> RARE_PREDICATE = CardPrinted.Predicates.Presets.IS_RARE_OR_MYTHIC;

    /**
     * Adds the random rare.
     * 
     * @return the card printed
     */
    public CardPrinted addRandomRare() {
        final CardPrinted card = QuestUtilCards.RARE_PREDICATE.random(CardDb.instance().getAllCards());
        this.addSingleCard(card);
        return card;
    }

    /**
     * Adds the random rare.
     * 
     * @param n
     *            the n
     * @return the list
     */
    public List<CardPrinted> addRandomRare(final int n) {
        final List<CardPrinted> newCards = QuestUtilCards.RARE_PREDICATE.random(CardDb.instance().getAllCards(), n);
        this.addAllCards(newCards);
        return newCards;
    }

    /**
     * Setup new game card pool.
     * 
     * @param filter
     *            the filter
     * @param idxDifficulty
     *            the idx difficulty
     */
    public void setupNewGameCardPool(final Predicate<CardPrinted> filter, final int idxDifficulty) {
        final int nC = QuestPreferences.getStartingCommons(idxDifficulty);
        final int nU = QuestPreferences.getStartingUncommons(idxDifficulty);
        final int nR = QuestPreferences.getStartingRares(idxDifficulty);

        this.addAllCards(BoosterUtils.getQuestStarterDeck(filter, nC, nU, nR));
    }

    /**
     * Buy card.
     * 
     * @param card
     *            the card
     * @param value
     *            the value
     */
    public void buyCard(final CardPrinted card, final int value) {
        if (this.q.getCredits() >= value) {
            this.q.setCredits(this.q.getCredits() - value);
            this.q.getShopList().remove(card);
            this.addSingleCard(card);
        }
    }

    /**
     * Buy booster.
     * 
     * @param booster
     *            the booster
     * @param value
     *            the value
     */
    public void buyBooster(final BoosterPack booster, final int value) {
        if (this.q.getCredits() >= value) {
            this.q.setCredits(this.q.getCredits() - value);
            this.q.getShopList().remove(booster);
            this.addAllCards(booster.getCards());
        }
    }

    /**
     * Sell card.
     * 
     * @param card
     *            the card
     * @param price
     *            the price
     */
    public void sellCard(final CardPrinted card, final int price) {
        if (price > 0) {
            this.q.setCredits(this.q.getCredits() + price);
        }
        this.q.getCardPool().remove(card);
        this.q.getShopList().add(card);

        // remove card being sold from all decks
        final int leftInPool = this.q.getCardPool().count(card);
        // remove sold cards from all decks:
        for (final Deck deck : this.q.getMyDecks().values()) {
            deck.removeMain(card, deck.getMain().count(card) - leftInPool);
        }
    }

    /**
     * Clear shop list.
     */
    public void clearShopList() {
        if (null != this.q.getShopList()) {
            this.q.getShopList().clear();
        }
    }

    /**
     * Gets the sell mutliplier.
     * 
     * @return the sell mutliplier
     */
    public double getSellMutliplier() {
        double multi = 0.20 + (0.001 * this.q.getWin());
        if (multi > 0.6) {
            multi = 0.6;
        }

        final int lvlEstates = this.q.isFantasy() ? this.q.getInventory().getItemLevel("Estates") : 0;
        switch (lvlEstates) {
        case 1:
            multi += 0.01;
            break;
        case 2:
            multi += 0.0175;
            break;
        case 3:
            multi += 0.025;
            break;
        default:
            break;
        }

        return multi;
    }

    /**
     * Gets the sell price limit.
     * 
     * @return the sell price limit
     */
    public int getSellPriceLimit() {
        return this.q.getWin() <= 50 ? 1000 : Integer.MAX_VALUE;
    }

    /**
     * Generate cards in shop.
     */
    public void generateCardsInShop() {
        final BoosterGenerator pack = new BoosterGenerator(CardDb.instance().getAllCards());

        final int levelPacks = this.q.getLevel() > 0 ? 4 / this.q.getLevel() : 4;
        final int winPacks = this.q.getWin() / 10;
        final int totalPacks = Math.min(levelPacks + winPacks, 6);

        final Predicate<CardSet> filterExt = CardSet.Predicates.Presets.SETS_IN_EXT;
        final Predicate<CardSet> filterT2booster = Predicate.and(CardSet.Predicates.CAN_MAKE_BOOSTER,
                CardSet.Predicates.Presets.SETS_IN_STANDARD);
        final Predicate<CardSet> filterExtButT2 = Predicate.and(CardSet.Predicates.CAN_MAKE_BOOSTER,
                Predicate.and(filterExt, Predicate.not(CardSet.Predicates.Presets.SETS_IN_STANDARD)));
        final Predicate<CardSet> filterNotExt = Predicate.and(CardSet.Predicates.CAN_MAKE_BOOSTER,
                Predicate.not(filterExt));

        this.q.getShopList().clear();
        for (int i = 0; i < totalPacks; i++) {
            this.q.getShopList().addAllCards(pack.getBoosterPack(7, 3, 1, 0, 0, 0, 0, 0, 0));

            // add some boosters
            final int rollD100 = MyRandom.getRandom().nextInt(100);
            final Predicate<CardSet> filter = rollD100 < 40 ? filterT2booster : (rollD100 < 75 ? filterExtButT2
                    : filterNotExt);
            this.q.getShopList().addAllCards(filter.random(SetUtils.getAllSets(), 1, BoosterPack.FN_FROM_SET));
        }

        this.addBasicLands(this.q.getShopList(), 10, 5);
    }

    /**
     * Gets the cardpool.
     * 
     * @return the cardpool
     */
    public ItemPool<InventoryItem> getCardpool() {
        return this.q.getCardPool();
    }

    /**
     * Gets the shop list.
     * 
     * @return the shop list
     */
    public ItemPoolView<InventoryItem> getShopList() {
        if (this.q.getShopList().isEmpty()) {
            this.generateCardsInShop();
        }
        return this.q.getShopList();
    }

    /**
     * Gets the new cards.
     * 
     * @return the new cards
     */
    public ItemPoolView<InventoryItem> getNewCards() {
        return this.q.getNewCardList();
    }

    /**
     * Reset new list.
     */
    public void resetNewList() {
        this.q.getNewCardList().clear();
    }

    /**
     * @return the fnNewCompare
     */
    public Lambda1<Comparable, Entry<InventoryItem, Integer>> getFnNewCompare() {
        return fnNewCompare;
    }

    /**
     * @return the fnNewGet
     */
    public Lambda1<Object, Entry<InventoryItem, Integer>> getFnNewGet() {
        return fnNewGet;
    }

    // These functions provide a way to sort and compare cards in a table
    // according to their new-ness
    // It might be a good idea to store them in a base class for both quest-mode
    // deck editors
    // Maybe we should consider doing so later
    /** The fn new compare. */
    @SuppressWarnings("rawtypes")
    private final Lambda1<Comparable, Entry<InventoryItem, Integer>> fnNewCompare = new Lambda1<Comparable, Entry<InventoryItem, Integer>>() {
        @Override
        public Comparable apply(final Entry<InventoryItem, Integer> from) {
            return QuestUtilCards.this.q.getNewCardList().contains(from.getKey()) ? Integer.valueOf(1) : Integer.valueOf(0);
        }
    };

    /** The fn new get. */
    private  final Lambda1<Object, Entry<InventoryItem, Integer>> fnNewGet = new Lambda1<Object, Entry<InventoryItem, Integer>>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            return QuestUtilCards.this.q.getNewCardList().contains(from.getKey()) ? "NEW" : "";
        }
    };
}
