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
package forge.quest;

import forge.Singletons;
import forge.card.BoosterGenerator;
import forge.card.CardEdition;
import forge.card.FormatCollection;
import forge.deck.Deck;
import forge.item.*;
import forge.quest.bazaar.QuestItemType;
import forge.quest.data.QuestAssets;
import forge.quest.data.QuestPreferences;
import forge.quest.data.QuestPreferences.QPref;
import forge.util.Aggregates;
import forge.util.MyRandom;
import forge.util.closures.Lambda1;
import forge.util.closures.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.google.common.collect.Iterables;

/**
 * This is a helper class to execute operations on QuestData. It has been
 * created to decrease complexity of questData class
 */
public final class QuestUtilCards {
    private final QuestController qc;
    private final QuestPreferences qpref;
    private final QuestAssets qa;

    /**
     * Instantiates a new quest util cards.
     * 
     * @param qd
     *            the qd
     */
    public QuestUtilCards(final QuestController qd) {
        this.qc = qd;
        this.qa = qc.getAssets();
        this.qpref = Singletons.getModel().getQuestPreferences();
    }

    /**
     * Adds the basic lands.
     *
     * @param nBasic the n basic
     * @param nSnow the n snow
     * @return the item pool view
     */
    public static ItemPoolView<CardPrinted> generateBasicLands(final int nBasic, final int nSnow) {
        final CardDb db = CardDb.instance();
        final ItemPool<CardPrinted> pool = new ItemPool<CardPrinted>(CardPrinted.class);
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
        return pool;
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
        final int nCommon = this.qpref.getPreferenceInt(QPref.BOOSTER_COMMONS);
        final int nUncommon = this.qpref.getPreferenceInt(QPref.BOOSTER_UNCOMMONS);
        final int nRare = this.qpref.getPreferenceInt(QPref.BOOSTER_RARES);

        final ArrayList<CardPrinted> newCards = new ArrayList<CardPrinted>();
        newCards.addAll(BoosterUtils.generateDistinctCards(Predicate.and(fSets, CardPrinted.Predicates.Presets.IS_COMMON), nCommon));
        newCards.addAll(BoosterUtils.generateDistinctCards(Predicate.and(fSets, CardPrinted.Predicates.Presets.IS_UNCOMMON), nUncommon));
        newCards.addAll(BoosterUtils.generateDistinctCards(Predicate.and(fSets, CardPrinted.Predicates.Presets.IS_RARE_OR_MYTHIC), nRare));

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
        this.qa.getCardPool().add(card);

        // register card into that list so that it would appear as a new one.
        this.qa.getNewCardList().add(card);
    }

    private static final Predicate<CardPrinted> RARE_PREDICATE = CardPrinted.Predicates.Presets.IS_RARE_OR_MYTHIC;

    /**
     * Adds the random rare.
     * 
     * @return the card printed
     */
    public CardPrinted addRandomRare() {
        final CardPrinted card = Aggregates.random(Iterables.filter(CardDb.instance().getAllCards(), QuestUtilCards.RARE_PREDICATE));
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
        final List<CardPrinted> newCards = Aggregates.random(Iterables.filter(CardDb.instance().getAllCards(), QuestUtilCards.RARE_PREDICATE), n); 
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
        final int nC = this.qpref.getPreferenceInt(QPref.STARTING_COMMONS, idxDifficulty);
        final int nU = this.qpref.getPreferenceInt(QPref.STARTING_UNCOMMONS, idxDifficulty);
        final int nR = this.qpref.getPreferenceInt(QPref.STARTING_RARES, idxDifficulty);

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
        if (this.qa.getCredits() >= value) {
            this.qa.setCredits(this.qa.getCredits() - value);
            this.qa.getShopList().remove(card);
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
    public void buyPack(final OpenablePack booster, final int value) {
        if (this.qa.getCredits() >= value) {
            this.qa.setCredits(this.qa.getCredits() - value);
            this.addAllCards(booster.getCards());
        }
    }

    /**
     * Buy precon deck.
     * 
     * @param precon
     *            the precon
     * @param value
     *            the value
     */
    public void buyPreconDeck(final PreconDeck precon, final int value) {
        if (this.qa.getCredits() >= value) {
            this.qa.setCredits(this.qa.getCredits() - value);
            this.qa.getShopList().remove(precon);
            addPreconDeck(precon);
        }
    }

    void addPreconDeck(PreconDeck precon) {
        this.qc.getMyDecks().add(precon.getDeck());
        this.addAllCards(precon.getDeck().getMain().toFlatList());
        this.addAllCards(precon.getDeck().getSideboard().toFlatList());
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
        this.sellCard(card, price, true);
    }

    /**
     * Sell card.
     * 
     * @param card
     *            the card
     * @param price
     *            the price
     * @param addToShop
     *            true if this card should be added to the shop, false otherwise
     */
    public void sellCard(final CardPrinted card, final int price, final boolean addToShop) {
        if (price > 0) {
            this.qa.setCredits(this.qa.getCredits() + price);
        }
        this.qa.getCardPool().remove(card);
        if (addToShop) {
            this.qa.getShopList().add(card);
        }

        // remove card being sold from all decks
        final int leftInPool = this.qa.getCardPool().count(card);
        // remove sold cards from all decks:
        for (final Deck deck : this.qc.getMyDecks()) {
            deck.getMain().remove(card, deck.getMain().count(card) - leftInPool);
        }
    }

    /**
     * Clear shop list.
     */
    public void clearShopList() {
        if (null != this.qa.getShopList()) {
            this.qa.getShopList().clear();
        }
    }

    /**
     * Gets the sell mutliplier.
     * 
     * @return the sell mutliplier
     */
    public double getSellMultiplier() {
        double multi = 0.20 + (0.001 * this.qc.getAchievements().getWin());
        if (multi > 0.6) {
            multi = 0.6;
        }

        final int lvlEstates = this.qc.getMode() == QuestMode.Fantasy ? this.qa.getItemLevel(QuestItemType.ESTATES) : 0;
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
        return this.qc.getAchievements().getWin() <= 50 ? 1000 : Integer.MAX_VALUE;
    }

    /**
     * Generate cards in shop.
     */
    private final FormatCollection formats = Singletons.getModel().getFormats();
    private final Predicate<CardEdition> filterExt = CardEdition.Predicates.isLegalInFormat(this.formats.getExtended());

    /** The filter t2booster. */
    private final Predicate<CardEdition> filterT2booster = Predicate.and(CardEdition.Predicates.CAN_MAKE_BOOSTER,
            CardEdition.Predicates.isLegalInFormat(this.formats.getStandard()));

    /** The filter ext but t2. */
    private final Predicate<CardEdition> filterExtButT2 = Predicate.and(
            CardEdition.Predicates.CAN_MAKE_BOOSTER,
            Predicate.and(this.filterExt,
                    Predicate.not(CardEdition.Predicates.isLegalInFormat(this.formats.getStandard()))));

    /** The filter not ext. */
    private final Predicate<CardEdition> filterNotExt = Predicate.and(CardEdition.Predicates.CAN_MAKE_BOOSTER,
            Predicate.not(this.filterExt));

    /**
     * Generate boosters in shop.
     * 
     * @param count
     *            the count
     */
    public void generateBoostersInShop(final int count) {
        for (int i = 0; i < count; i++) {
            final int rollD100 = MyRandom.getRandom().nextInt(100);
            final Predicate<CardEdition> filter = rollD100 < 40 ? this.filterT2booster
                    : (rollD100 < 75 ? this.filterExtButT2 : this.filterNotExt);
            Iterable<CardEdition> rightEditions = Iterables.filter(Singletons.getModel().getEditions(), filter);
            this.qa.getShopList().add(BoosterPack.FN_FROM_SET.apply(Aggregates.random(rightEditions)));
        }
    }

    /**
     * Generate precons in shop.
     * 
     * @param count
     *            the count
     */
    public void generateTournamentsInShop(final int count) {
        Iterable<CardEdition> rightEditions = Iterables.filter(Singletons.getModel().getEditions(), CardEdition.Predicates.HAS_TOURNAMENT_PACK);
        this.qa.getShopList().addAllFlat(Aggregates.random(Iterables.transform(rightEditions, TournamentPack.FN_FROM_SET), count));
    }

    public void generateFatPacksInShop(final int count) {
        Iterable<CardEdition> rightEditions = Iterables.filter(Singletons.getModel().getEditions(), CardEdition.Predicates.HAS_FAT_PACK);
        this.qa.getShopList().addAllFlat(Aggregates.random(Iterables.transform(rightEditions, FatPack.FN_FROM_SET), count));
    }

    /**
     * Generate precons in shop.
     * 
     * @param count
     *            the count
     */
    public void generatePreconsInShop(final int count) {
        final List<PreconDeck> meetRequirements = new ArrayList<PreconDeck>();
        for (final PreconDeck deck : QuestController.getPrecons()) {
            if (deck.getRecommendedDeals().meetsRequiremnts(this.qc.getAchievements())) {
                meetRequirements.add(deck);
            }
        }
        this.qa.getShopList().addAllFlat(Aggregates.random(meetRequirements, count));
    }

    /**
     * Generate cards in shop.
     */
    public void generateCardsInShop() {
        final BoosterGenerator pack = new BoosterGenerator(CardDb.instance().getAllCards());

        int nLevel = this.qc.getAchievements().getLevel();

        // Preferences
        final int startPacks = this.qpref.getPreferenceInt(QPref.SHOP_STARTING_PACKS);
        final int winsForPack = this.qpref.getPreferenceInt(QPref.SHOP_WINS_FOR_ADDITIONAL_PACK);
        final int maxPacks = this.qpref.getPreferenceInt(QPref.SHOP_MAX_PACKS);
        final int common = this.qpref.getPreferenceInt(QPref.SHOP_SINGLES_COMMON);
        final int uncommon = this.qpref.getPreferenceInt(QPref.SHOP_SINGLES_UNCOMMON);
        final int rare = this.qpref.getPreferenceInt(QPref.SHOP_SINGLES_RARE);

        final int levelPacks = nLevel > 0 ? startPacks / nLevel : startPacks;
        final int winPacks = this.qc.getAchievements().getWin() / winsForPack;
        final int totalPacks = Math.min(levelPacks + winPacks, maxPacks);

        this.qa.getShopList().clear();
        for (int i = 0; i < totalPacks; i++) {
            this.qa.getShopList().addAllFlat(pack.getBoosterPack(common, uncommon, rare, 0, 0, 0, 0, 0, 0));
        }

        this.generateBoostersInShop(totalPacks);
        this.generatePreconsInShop(totalPacks);
        this.generateTournamentsInShop(totalPacks);
        this.generateFatPacksInShop(totalPacks);
        this.qa.getShopList().addAll(QuestUtilCards.generateBasicLands(10, 5));
    }

    /**
     * Gets the cardpool.
     * 
     * @return the cardpool
     */
    public ItemPool<CardPrinted> getCardpool() {
        return this.qa.getCardPool();
    }

    /**
     * Gets the shop list.
     * 
     * @return the shop list
     */
    public ItemPoolView<InventoryItem> getShopList() {
        if (this.qa.getShopList().isEmpty()) {
            this.generateCardsInShop();
        }
        return this.qa.getShopList();
    }

    /**
     * Gets the new cards.
     * 
     * @return the new cards
     */
    public ItemPoolView<InventoryItem> getNewCards() {
        return this.qa.getNewCardList();
    }

    /**
     * Reset new list.
     */
    public void resetNewList() {
        this.qa.getNewCardList().clear();
    }

    /**
     * Gets the fn new compare.
     * 
     * @return the fnNewCompare
     */
    @SuppressWarnings("rawtypes")
    public Lambda1<Comparable, Entry<InventoryItem, Integer>> getFnNewCompare() {
        return this.fnNewCompare;
    }

    /**
     * Gets the fn new get.
     * 
     * @return the fnNewGet
     */
    public Lambda1<Object, Entry<InventoryItem, Integer>> getFnNewGet() {
        return this.fnNewGet;
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
            return QuestUtilCards.this.qa.getNewCardList().contains(from.getKey()) ? Integer.valueOf(1) : Integer
                    .valueOf(0);
        }
    };

    /** The fn new get. */
    private final Lambda1<Object, Entry<InventoryItem, Integer>> fnNewGet = new Lambda1<Object, Entry<InventoryItem, Integer>>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            return QuestUtilCards.this.qa.getNewCardList().contains(from.getKey()) ? "NEW" : "";
        }
    };
}
