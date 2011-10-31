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
    private QuestData q;

    /**
     * Instantiates a new quest util cards.
     * 
     * @param qd
     *            the qd
     */
    public QuestUtilCards(final QuestData qd) {
        q = qd;
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
        CardDb db = CardDb.instance();
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
        int nCommon = QuestPreferences.getNumCommon();
        int nUncommon = QuestPreferences.getNumUncommon();
        int nRare = QuestPreferences.getNumRare();

        ArrayList<CardPrinted> newCards = new ArrayList<CardPrinted>();
        newCards.addAll(BoosterUtils.generateCards(fSets, nCommon, CardRarity.Common, null));
        newCards.addAll(BoosterUtils.generateCards(fSets, nUncommon, CardRarity.Uncommon, null));
        newCards.addAll(BoosterUtils.generateCards(fSets, nRare, CardRarity.Rare, null));

        addAllCards(newCards);
        return newCards;
    }

    /**
     * Adds the all cards.
     * 
     * @param newCards
     *            the new cards
     */
    public void addAllCards(final Iterable<CardPrinted> newCards) {
        for (CardPrinted card : newCards) {
            addSingleCard(card);
        }
    }

    /**
     * Adds the single card.
     * 
     * @param card
     *            the card
     */
    public void addSingleCard(final CardPrinted card) {
        q.cardPool.add(card);

        // register card into that list so that it would appear as a new one.
        q.newCardList.add(card);
    }

    private static final Predicate<CardPrinted> rarePredicate = CardPrinted.Predicates.Presets.isRareOrMythic;

    /**
     * Adds the random rare.
     * 
     * @return the card printed
     */
    public CardPrinted addRandomRare() {
        CardPrinted card = rarePredicate.random(CardDb.instance().getAllCards());
        addSingleCard(card);
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
        List<CardPrinted> newCards = rarePredicate.random(CardDb.instance().getAllCards(), n);
        addAllCards(newCards);
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
        int nC = QuestPreferences.getStartingCommons(idxDifficulty);
        int nU = QuestPreferences.getStartingUncommons(idxDifficulty);
        int nR = QuestPreferences.getStartingRares(idxDifficulty);

        addAllCards(BoosterUtils.getQuestStarterDeck(filter, nC, nU, nR));
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
        if (q.credits >= value) {
            q.credits -= value;
            q.shopList.remove(card);
            addSingleCard(card);
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
        if (q.credits >= value) {
            q.credits -= value;
            q.shopList.remove(booster);
            addAllCards(booster.getCards());
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
            q.credits += price;
        }
        q.cardPool.remove(card);
        q.shopList.add(card);

        // remove card being sold from all decks
        int leftInPool = q.cardPool.count(card);
        // remove sold cards from all decks:
        for (Deck deck : q.myDecks.values()) {
            deck.removeMain(card, deck.getMain().count(card) - leftInPool);
        }
    }

    /**
     * Clear shop list.
     */
    public void clearShopList() {
        if (null != q.shopList) {
            q.shopList.clear();
        }
    }

    /**
     * Gets the sell mutliplier.
     * 
     * @return the sell mutliplier
     */
    public double getSellMutliplier() {
        double multi = 0.20 + (0.001 * q.getWin());
        if (multi > 0.6) {
            multi = 0.6;
        }

        int lvlEstates = q.isFantasy() ? q.inventory.getItemLevel("Estates") : 0;
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
        return q.getWin() <= 50 ? 1000 : Integer.MAX_VALUE;
    }

    /**
     * Generate cards in shop.
     */
    public void generateCardsInShop() {
        BoosterGenerator pack = new BoosterGenerator(CardDb.instance().getAllCards());

        int levelPacks = q.getLevel() > 0 ? 4 / q.getLevel() : 4;
        int winPacks = q.getWin() / 10;
        int totalPacks = Math.min(levelPacks + winPacks, 6);

        final Predicate<CardSet> filterExt = CardSet.Predicates.Presets.SETS_IN_EXT;
        final Predicate<CardSet> filterT2booster = Predicate.and(CardSet.Predicates.CAN_MAKE_BOOSTER,
                CardSet.Predicates.Presets.SETS_IN_STANDARD);
        final Predicate<CardSet> filterExtButT2 = Predicate.and(CardSet.Predicates.CAN_MAKE_BOOSTER,
                Predicate.and(filterExt, Predicate.not(CardSet.Predicates.Presets.SETS_IN_STANDARD)));
        final Predicate<CardSet> filterNotExt = Predicate.and(CardSet.Predicates.CAN_MAKE_BOOSTER,
                Predicate.not(filterExt));

        q.shopList.clear();
        for (int i = 0; i < totalPacks; i++) {
            q.shopList.addAllCards(pack.getBoosterPack(7, 3, 1, 0, 0, 0, 0, 0, 0));

            // add some boosters
            int rollD100 = MyRandom.getRandom().nextInt(100);
            Predicate<CardSet> filter = rollD100 < 40 ? filterT2booster : (rollD100 < 75 ? filterExtButT2
                    : filterNotExt);
            q.shopList.addAllCards(filter.random(SetUtils.getAllSets(), 1, BoosterPack.fnFromSet));
        }

        addBasicLands(q.shopList, 10, 5);
    }

    /**
     * Gets the cardpool.
     * 
     * @return the cardpool
     */
    public ItemPool<InventoryItem> getCardpool() {
        return q.cardPool;
    }

    /**
     * Gets the shop list.
     * 
     * @return the shop list
     */
    public ItemPoolView<InventoryItem> getShopList() {
        if (q.shopList.isEmpty()) {
            generateCardsInShop();
        }
        return q.shopList;
    }

    /**
     * Gets the new cards.
     * 
     * @return the new cards
     */
    public ItemPoolView<InventoryItem> getNewCards() {
        return q.newCardList;
    }

    /**
     * Reset new list.
     */
    public void resetNewList() {
        q.newCardList.clear();
    }

    // These functions provide a way to sort and compare cards in a table
    // according to their new-ness
    // It might be a good idea to store them in a base class for both quest-mode
    // deck editors
    // Maybe we should consider doing so later
    /** The fn new compare. */
    @SuppressWarnings("rawtypes")
    public final Lambda1<Comparable, Entry<InventoryItem, Integer>> fnNewCompare = new Lambda1<Comparable, Entry<InventoryItem, Integer>>() {
        @Override
        public Comparable apply(final Entry<InventoryItem, Integer> from) {
            return q.newCardList.contains(from.getKey()) ? Integer.valueOf(1) : Integer.valueOf(0);
        }
    };

    /** The fn new get. */
    public final Lambda1<Object, Entry<InventoryItem, Integer>> fnNewGet = new Lambda1<Object, Entry<InventoryItem, Integer>>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            return q.newCardList.contains(from.getKey()) ? "NEW" : "";
        }
    };
}
