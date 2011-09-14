package forge.quest.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import net.slightlymagic.braids.util.lambda.Lambda1;
import net.slightlymagic.maxmtg.Predicate;

import forge.ReadBoosterPack;
import forge.card.CardRarity;
import forge.deck.Deck;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.item.ItemPool;
import forge.item.ItemPoolView;

/** 
 * This is a helper class to execute operations on QuestData.
 * It has been created to decrease complexity of questData class
 */
public final class QuestUtilCards {
    private QuestData q;
    public QuestUtilCards(final QuestData qd) { q = qd; }

    public void generateBasicLands(final int nBasic, final int nSnow) {
        CardDb db = CardDb.instance();
        q.cardPool.add(db.getCard("Forest", "M10"), nBasic);
        q.cardPool.add(db.getCard("Mountain", "M10"), nBasic);
        q.cardPool.add(db.getCard("Swamp", "M10"), nBasic);
        q.cardPool.add(db.getCard("Island", "M10"), nBasic);
        q.cardPool.add(db.getCard("Plains", "M10"), nBasic);

        q.cardPool.add(db.getCard("Snow-Covered Forest", "ICE"), nSnow);
        q.cardPool.add(db.getCard("Snow-Covered Mountain", "ICE"), nSnow);
        q.cardPool.add(db.getCard("Snow-Covered Swamp", "ICE"), nSnow);
        q.cardPool.add(db.getCard("Snow-Covered Island", "ICE"), nSnow);
        q.cardPool.add(db.getCard("Snow-Covered Plains", "ICE"), nSnow);
    }

    //adds 11 cards, to the current card pool
    //(I chose 11 cards instead of 15 in order to make things more challenging)

    /**
     * <p>addCards.</p>
     */
    public ArrayList<CardPrinted> addCards(final Predicate<CardPrinted> fSets) {
        int nCommon = QuestPreferences.getNumCommon();
        int nUncommon = QuestPreferences.getNumUncommon();
        int nRare = QuestPreferences.getNumRare();

        ArrayList<CardPrinted> newCards = new ArrayList<CardPrinted>();
        newCards.addAll(QuestBoosterPack.generateCards(fSets, nCommon, CardRarity.Common, null));
        newCards.addAll(QuestBoosterPack.generateCards(fSets, nUncommon, CardRarity.Uncommon, null));
        newCards.addAll(QuestBoosterPack.generateCards(fSets, nRare, CardRarity.Rare, null));

        addAllCards(newCards);
        return newCards;
    }

    public void addAllCards(final Iterable<CardPrinted> newCards) {
        for (CardPrinted card : newCards) {
            addSingleCard(card);
        }
    }
    
    public void addSingleCard(CardPrinted card) {
        q.cardPool.add(card);

        // register card into that list so that it would appear as a new one.
        q.newCardList.add(card);
    }

    private static final Predicate<CardPrinted> rarePredicate = CardPrinted.Predicates.Presets.isRareOrMythic;
    public CardPrinted addRandomRare() {
        CardPrinted card = rarePredicate.random(CardDb.instance().getAllCards());
        addSingleCard(card);
        return card;
    }
    public List<CardPrinted> addRandomRare(final int n) {
        List<CardPrinted> newCards = rarePredicate.random(CardDb.instance().getAllCards(), n);
        addAllCards(newCards);
        return newCards;
    }

    public void setupNewGameCardPool(final Predicate<CardPrinted> filter, final int idxDifficulty)
    {
        int nC = QuestPreferences.getStartingCommons(idxDifficulty);
        int nU = QuestPreferences.getStartingUncommons(idxDifficulty);
        int nR = QuestPreferences.getStartingRares(idxDifficulty);

        addAllCards(QuestBoosterPack.getQuestStarterDeck(filter, nC, nU, nR));
    }

    public void buyCard(final CardPrinted card, final int value) {
        if (q.credits >= value) {
            q.credits -= value;
            q.shopList.remove(card);
            addSingleCard(card);
        }
    }

    public void sellCard(final CardPrinted card, final int price) {
        if (price > 0) { q.credits += price; }
        q.cardPool.remove(card);
        q.shopList.add(card);

        // remove card being sold from all decks
        int leftInPool = q.cardPool.count(card);
        // remove sold cards from all decks:
        for (Deck deck : q.myDecks.values()) {
            deck.removeMain(card, deck.getMain().count(card) - leftInPool);
        }
    }

    public void clearShopList() {
        if (null != q.shopList) { q.shopList.clear(); }
    }

    public double getSellMutliplier() {
        double multi = 0.20 + (0.001 * q.getWin());
        if (multi > 0.6) {
            multi = 0.6;
        }

        int lvlEstates = q.isFantasy() ? q.inventory.getItemLevel("Estates") : 0;
        switch (lvlEstates) {
            case 1: multi += 0.01; break;
            case 2: multi += 0.0175; break;
            case 3: multi += 0.025; break;
            default: break;
        }

        return multi;
    }

    public int getSellPriceLimit() {
        return q.getWin() <= 50 ? 1000 : Integer.MAX_VALUE;
    }

    public void generateCardsInShop() {
        ReadBoosterPack pack = new ReadBoosterPack();

        int levelPacks = q.getLevel() > 0 ? 4 / q.getLevel() : 4;
        int winPacks = q.getWin() / 10;
        int totalPacks = Math.min(levelPacks + winPacks, 6);

        ItemPoolView<CardPrinted> fromBoosters = pack.getShopCards(totalPacks);
        q.shopList.clear();
        q.shopList.addAll(fromBoosters);
    }

    public ItemPool<CardPrinted> getCardpool() {
        return q.cardPool;
    }

    public ItemPoolView<CardPrinted> getShopList() {
        if (q.shopList.isEmpty()) {
            generateCardsInShop();
        }
        return q.shopList;
    }

    public ItemPoolView<InventoryItem> getNewCards() {
        return q.newCardList;
    }

    public void resetNewList() {
        q.newCardList.clear();
    }

    // These functions provide a way to sort and compare cards in a table according to their new-ness
    // It might be a good idea to store them in a base class for both quest-mode deck editors
    // Maybe we should consider doing so later
    @SuppressWarnings("rawtypes")
    public final Lambda1<Comparable, Entry<InventoryItem, Integer>> fnNewCompare =
        new Lambda1<Comparable, Entry<InventoryItem, Integer>>() { @Override
            public Comparable apply(final Entry<InventoryItem, Integer> from) {
                return q.newCardList.contains(from.getKey()) ? Integer.valueOf(1) : Integer.valueOf(0);
            } };
    public final Lambda1<Object, Entry<InventoryItem, Integer>> fnNewGet =
        new Lambda1<Object, Entry<InventoryItem, Integer>>() { @Override
            public Object apply(final Entry<InventoryItem, Integer> from) {
                return q.newCardList.contains(from.getKey()) ? "NEW" : "";
            } };
}
