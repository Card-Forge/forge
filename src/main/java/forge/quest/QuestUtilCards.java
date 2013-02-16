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

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.Singletons;
import forge.card.BoosterGenerator;
import forge.card.CardEdition;
import forge.card.CardRarity;
import forge.card.FormatCollection;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.item.BoosterPack;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.FatPack;
import forge.item.InventoryItem;
import forge.item.ItemPool;
import forge.item.ItemPoolView;
import forge.item.OpenablePack;
import forge.item.PreconDeck;
import forge.item.TournamentPack;
import forge.quest.bazaar.QuestItemType;
import forge.quest.data.GameFormatQuest;
import forge.quest.data.QuestAssets;
import forge.quest.data.QuestPreferences;
import forge.quest.data.QuestPreferences.QPref;
import forge.util.Aggregates;
import forge.util.MyRandom;

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
     * Adds the basic lands (from random sets as limited by the format).
     *
     * @param nBasic the n basic
     * @param nSnow the n snow
     * @param usedFormat currently enforced game format, if any
     * @return the item pool view
     */
    public static ItemPoolView<CardPrinted> generateBasicLands(final int nBasic, final int nSnow, final GameFormatQuest usedFormat) {
        final CardDb db = CardDb.instance();
        final ItemPool<CardPrinted> pool = new ItemPool<CardPrinted>(CardPrinted.class);

        List<String> landCodes = new ArrayList<String>();
        List<String> snowLandCodes = new ArrayList<String>();

        if (usedFormat != null) {
            List<String> availableEditions = usedFormat.getAllowedSetCodes();
            for (String edition : availableEditions) {
                if (null != db.tryGetCard("Plains", edition)) {
                    landCodes.add(edition);
                }
            }
            if (usedFormat.isSetLegal("ICE")) {
                snowLandCodes.add("ICE");
            }
            if (usedFormat.isSetLegal("CSP")) {
                snowLandCodes.add("CSP");
            }
        } else {
            Iterable<CardEdition> allEditions = Singletons.getModel().getEditions();
            for (CardEdition edition : Iterables.filter(allEditions, CardEdition.Predicates.hasBasicLands)) {
                landCodes.add(edition.getCode());
            }
            snowLandCodes.add("ICE");
            snowLandCodes.add("CSP");
        }

        String landCode = Aggregates.random(landCodes);
        if (null == landCode) {
            landCode = "M10";
        }

        pool.add(db.getCard("Forest", landCode), nBasic);
        pool.add(db.getCard("Mountain", landCode), nBasic);
        pool.add(db.getCard("Swamp", landCode), nBasic);
        pool.add(db.getCard("Island", landCode), nBasic);
        pool.add(db.getCard("Plains", landCode), nBasic);

        if (!snowLandCodes.isEmpty()) {
            String snowLandCode = Aggregates.random(snowLandCodes);
            pool.add(db.getCard("Snow-Covered Forest", snowLandCode), nSnow);
            pool.add(db.getCard("Snow-Covered Mountain", snowLandCode), nSnow);
            pool.add(db.getCard("Snow-Covered Swamp", snowLandCode), nSnow);
            pool.add(db.getCard("Snow-Covered Island", snowLandCode), nSnow);
            pool.add(db.getCard("Snow-Covered Plains", snowLandCode), nSnow);
        }

        return pool;
    }

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
        Predicate<CardPrinted> predCommons = Predicates.and(fSets, CardPrinted.Predicates.Presets.IS_COMMON);
        newCards.addAll(BoosterUtils.generateDistinctCards(predCommons, nCommon));
        Predicate<CardPrinted> predUncommons = Predicates.and(fSets, CardPrinted.Predicates.Presets.IS_UNCOMMON);
        newCards.addAll(BoosterUtils.generateDistinctCards(predUncommons, nUncommon));
        Predicate<CardPrinted> predRareMythics = Predicates.and(fSets, CardPrinted.Predicates.Presets.IS_RARE_OR_MYTHIC);
        newCards.addAll(BoosterUtils.generateDistinctCards(predRareMythics, nRare));

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
     * A predicate that takes into account the Quest Format (if any).
     * @param source
     *  the predicate to be added to the format predicate.
     * @return the composite predicate.
     */
    public Predicate<CardPrinted> applyFormatFilter(Predicate<CardPrinted> source) {
       return qc.getFormat() == null ? source : Predicates.and(source, qc.getFormat().getFilterPrinted());
    }

    /**
     * Adds the random rare.
     * 
     * @return the card printed
     */
    public CardPrinted addRandomRare() {

        final Predicate<CardPrinted> myFilter = applyFormatFilter(QuestUtilCards.RARE_PREDICATE);

        final CardPrinted card = Aggregates.random(Iterables.filter(CardDb.instance().getAllCards(), myFilter));
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
        final Predicate<CardPrinted> myFilter = applyFormatFilter(QuestUtilCards.RARE_PREDICATE);

        final List<CardPrinted> newCards = Aggregates.random(Iterables.filter(CardDb.instance().getAllCards(), myFilter), n);
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
            this.addDeck(precon.getDeck());
        }
    }

    /**
     * Import an existing deck.
     * 
     * @param fromDeck
     *            Deck, deck to import
     */
    void addDeck(final Deck fromDeck) {
        if (fromDeck == null) {
            return;
        }
        this.qc.getMyDecks().add(fromDeck);
        this.addAllCards(fromDeck.getMain().toFlatList());
        if (fromDeck.has(DeckSection.Sideboard))
            this.addAllCards(fromDeck.get(DeckSection.Sideboard).toFlatList());
    }

    /**
     * Sell card.
     * 
     * @param card
     *            the card
     * @param price
     *            the price
     */
    public void sellCard(final CardPrinted card, int qty, final int pricePerCard) {
        this.sellCard(card, qty, pricePerCard, true);
    }

    public void loseCard(final CardPrinted card, int qty) {
        this.sellCard(card, qty, 0, false);
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
    private void sellCard(final CardPrinted card, int qty, final int pricePerCard, final boolean addToShop) {
        if (pricePerCard > 0) {
            this.qa.setCredits(this.qa.getCredits() + (qty * pricePerCard));
        }
        this.qa.getCardPool().remove(card, qty);
        if (addToShop) {
            this.qa.getShopList().add(card, qty);
        }

        // remove card being sold from all decks
        final int leftInPool = this.qa.getCardPool().count(card);
        // remove sold cards from all decks:
        for (final Deck deck : this.qc.getMyDecks()) {
            int cntInMain = deck.getMain().count(card);
            int cntInSb = deck.has(DeckSection.Sideboard) ? deck.get(DeckSection.Sideboard).count(card) : 0;
            int nToRemoveFromThisDeck = cntInMain + cntInSb - leftInPool;
            if ( nToRemoveFromThisDeck <= 0 ) continue; // this is not the deck you are looking for

            int nToRemoveFromSb = cntInSb - nToRemoveFromThisDeck;
            if( nToRemoveFromSb > 0 ) {
                deck.get(DeckSection.Sideboard).remove(card, nToRemoveFromSb);
                nToRemoveFromThisDeck -= cntInSb; // actual removed count should be, but I take upper bound here
                if ( nToRemoveFromThisDeck <= 0 ) continue; // done here
            }

            deck.getMain().remove(card, nToRemoveFromThisDeck);
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
    private final Predicate<CardEdition> filterT2booster = Predicates.and(CardEdition.Predicates.CAN_MAKE_BOOSTER,
            CardEdition.Predicates.isLegalInFormat(this.formats.getStandard()));

    /** The filter ext but t2. */
    private final Predicate<CardEdition> filterExtButT2 = Predicates.and(
            CardEdition.Predicates.CAN_MAKE_BOOSTER,
            Predicates.and(this.filterExt,
                    Predicates.not(CardEdition.Predicates.isLegalInFormat(this.formats.getStandard()))));

    /** The filter not ext. */
    private final Predicate<CardEdition> filterNotExt = Predicates.and(CardEdition.Predicates.CAN_MAKE_BOOSTER,
            Predicates.not(this.filterExt));

    /**
     * Helper predicate for shops: is legal in quest format.
     * 
     * @param qFormat
     *            the quest format
     * @return the predicate
     */
    public static Predicate<CardEdition> isLegalInQuestFormat(final GameFormatQuest qFormat) {
        return GameFormatQuest.Predicates.isLegalInFormatQuest(qFormat);
    }

    /**
     * Generate boosters in shop.
     * 
     * @param count
     *            the count
     */
    public void generateBoostersInShop(final int count) {
        for (int i = 0; i < count; i++) {
            final int rollD100 = MyRandom.getRandom().nextInt(100);
            Predicate<CardEdition> filter = rollD100 < 40 ? this.filterT2booster
                    : (rollD100 < 75 ? this.filterExtButT2 : this.filterNotExt);
            if (qc.getFormat() != null) {
                filter = Predicates.and(CardEdition.Predicates.CAN_MAKE_BOOSTER, isLegalInQuestFormat(qc.getFormat()));
            }
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
        Predicate<CardEdition> formatFilter = CardEdition.Predicates.HAS_TOURNAMENT_PACK;
        if (qc.getFormat() != null) {
            formatFilter = Predicates.and(formatFilter, isLegalInQuestFormat(qc.getFormat()));
        }
        Iterable<CardEdition> rightEditions = Iterables.filter(Singletons.getModel().getEditions(), formatFilter);
        this.qa.getShopList().addAllFlat(Aggregates.random(Iterables.transform(rightEditions, TournamentPack.FN_FROM_SET), count));
    }

    /**
     * Generate precons in shop.
     * 
     * @param count
     *            the count
     */
    public void generateFatPacksInShop(final int count) {
        Predicate<CardEdition> formatFilter = CardEdition.Predicates.HAS_FAT_PACK;
        if (qc.getFormat() != null) {
            formatFilter = Predicates.and(formatFilter, isLegalInQuestFormat(qc.getFormat()));
        }
        Iterable<CardEdition> rightEditions = Iterables.filter(Singletons.getModel().getEditions(), formatFilter);
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

        Iterable<CardPrinted> cardList = null;
        if (qc.getFormat() == null) {
              cardList = CardDb.instance().getAllCards(); }
        else {
            cardList = Iterables.filter(CardDb.instance().getAllCards(),
                    qc.getFormat().getFilterPrinted());
        }

        final BoosterGenerator pack = new BoosterGenerator(cardList);

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
        int numberSnowLands = 5;
        if (qc.getFormat() != null && !qc.getFormat().hasSnowLands()) {
            numberSnowLands = 0;
        }
        this.qa.getShopList().addAll(QuestUtilCards.generateBasicLands(10, numberSnowLands, qc.getFormat()));
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

    public Function<Entry<InventoryItem, Integer>, Comparable<?>> getFnNewCompare() {
        return this.fnNewCompare;
    }

    public Function<Entry<InventoryItem, Integer>, Object> getFnNewGet() {
        return this.fnNewGet;
    }

    // These functions provide a way to sort and compare cards in a table
    // according to their new-ness
    // It might be a good idea to store them in a base class for both quest-mode
    // deck editors
    // Maybe we should consider doing so later
    /** The fn new compare. */
    private final Function<Entry<InventoryItem, Integer>, Comparable<?>> fnNewCompare =
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
        @Override
        public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
            return QuestUtilCards.this.qa.getNewCardList().contains(from.getKey()) ? Integer.valueOf(1) : Integer
                    .valueOf(0);
        }
    };

    /** The fn new get. */
    private final Function<Entry<InventoryItem, Integer>, Object> fnNewGet =
            new Function<Entry<InventoryItem, Integer>, Object>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            return QuestUtilCards.this.qa.getNewCardList().contains(from.getKey()) ? "NEW" : "";
        }
    };
    
    public Function<Entry<InventoryItem, Integer>, Comparable<?>> getFnOwnedCompare() {
        return this.fnOwnedCompare;
    }

    public Function<Entry<InventoryItem, Integer>, Object> getFnOwnedGet() {
        return this.fnOwnedGet;
    }
    
    public int getCompletionPercent(String edition) {
        // get all cards in the specified edition
        Predicate<CardPrinted> filter = CardPrinted.Predicates.printedInSets(edition);
        Iterable<CardPrinted> editionCards = Iterables.filter(CardDb.instance().getAllCards(), filter);

        ItemPool<CardPrinted> ownedCards = qa.getCardPool();
        // 100% means at least one of every basic land and at least 4 of every other card in the set
        int completeCards = 0;
        int numOwnedCards = 0;
        for (CardPrinted card : editionCards) {
            final int target = CardRarity.BasicLand == card.getRarity() ? 1 : 4;
            
            completeCards += target;
            numOwnedCards += Math.min(target, ownedCards.count(card));
        }
        
        return (numOwnedCards * 100) / completeCards;
    }

    // These functions provide a way to sort and compare items in the spell shop according to how many are already owned
    private final Function<Entry<InventoryItem, Integer>, Comparable<?>> fnOwnedCompare =
            new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
        @Override
        public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
            InventoryItem i = from.getKey();
            if (i instanceof CardPrinted) {
                return QuestUtilCards.this.qa.getCardPool().count((CardPrinted)i);
            } else if (i instanceof PreconDeck) {
                PreconDeck pDeck = (PreconDeck)i;
                return Singletons.getModel().getQuest().getMyDecks().contains(pDeck.getName()) ? -1 : -2;
            } else if (i instanceof OpenablePack) {
                OpenablePack oPack = (OpenablePack)i;
                return getCompletionPercent(oPack.getEdition()) - 103;
            }
            return null;
        }
    };

    private final Function<Entry<InventoryItem, Integer>, Object> fnOwnedGet =
            new Function<Entry<InventoryItem, Integer>, Object>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            InventoryItem i = from.getKey();
            if (i instanceof CardPrinted) {
                return QuestUtilCards.this.qa.getCardPool().count((CardPrinted)i);
            } else if (i instanceof PreconDeck) {
                PreconDeck pDeck = (PreconDeck)i;
                return Singletons.getModel().getQuest().getMyDecks().contains(pDeck.getName()) ? "YES" : "NO";
            } else if (i instanceof OpenablePack) {
                OpenablePack oPack = (OpenablePack)i;
                return String.format("%d%%", getCompletionPercent(oPack.getEdition()));
            }
            return null;
        }
    };
}
