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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import forge.card.*;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.GameFormat;
import forge.item.*;
import forge.item.SealedProduct.Template;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.quest.bazaar.QuestItemType;
import forge.quest.data.GameFormatQuest;
import forge.quest.data.QuestAssets;
import forge.quest.data.QuestPreferences;
import forge.quest.data.QuestPreferences.DifficultyPrefs;
import forge.quest.data.QuestPreferences.QPref;
import forge.util.Aggregates;
import forge.util.ItemPool;
import forge.util.MyRandom;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

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
        this.qpref = FModel.getQuestPreferences();
    }

    /**
     * Adds the basic lands (from random sets as limited by the format).
     *
     * @param nBasic the n basic
     * @param nSnow the n snow
     * @param usedFormat currently enforced game format, if any
     * @return the item pool view
     */
    public static ItemPool<PaperCard> generateBasicLands(final int nBasic, final int nSnow, final GameFormatQuest usedFormat) {
        final ICardDatabase db = FModel.getMagicDb().getCommonCards();
        final ItemPool<PaperCard> pool = new ItemPool<>(PaperCard.class);

        List<String> landCodes = new ArrayList<>();
        List<String> snowLandCodes = new ArrayList<>();

        if (usedFormat != null) {
            List<String> availableEditions = usedFormat.getAllowedSetCodes();

            for (String edCode : availableEditions) {
                CardEdition ed = FModel.getMagicDb().getEditions().get(edCode);
                // Duel decks might have only 2 types of basic lands
                if (CardEdition.Predicates.hasBasicLands.apply(ed)) {
                    landCodes.add(edCode);
                }
            }
            if (usedFormat.isSetLegal("ICE")) {
                snowLandCodes.add("ICE");
            }
            if (usedFormat.isSetLegal("CSP")) {
                snowLandCodes.add("CSP");
            }
        } else {
            Iterable<CardEdition> allEditions = FModel.getMagicDb().getEditions();
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

        final boolean isZendikarSet = landCode.equals("ZEN"); // we want to generate one kind of Zendikar lands at a time only
        final boolean zendikarSetMode = MyRandom.getRandom().nextBoolean();

        for (String landName : MagicColor.Constant.BASIC_LANDS) {
            int artCount = db.getArtCount(landName, landCode);

            if (FModel.getPreferences().getPrefBoolean(FPref.UI_RANDOM_ART_IN_POOLS)) {
                int[] artGroups = MyRandom.splitIntoRandomGroups(nBasic, isZendikarSet ? 4 : artCount);

                for (int i = 1; i <= artGroups.length; i++) {
                    pool.add(db.getCard(landName, landCode, isZendikarSet ? (zendikarSetMode ? i : i + 4) : i), artGroups[i - 1]);
                }
            } else {
                pool.add(db.getCard(landName, landCode, artCount > 1 ? MyRandom.getRandom().nextInt(artCount) + 1 : 1), nBasic);
            }
        }


        if (!snowLandCodes.isEmpty()) {
            String snowLandCode = Aggregates.random(snowLandCodes);
            for (String landName : MagicColor.Constant.SNOW_LANDS) {
                pool.add(db.getCard(landName, snowLandCode), nSnow);
            }
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
    public List<PaperCard> generateQuestBooster(final Predicate<PaperCard> fSets) {
        UnOpenedProduct unopened = new UnOpenedProduct(getBoosterTemplate(), fSets);
        return unopened.get();
    }

    /**
     * Adds the all cards.
     *
     * @param newCards
     *            the new cards
     */
    public void addAllCards(final Iterable<PaperCard> newCards) {
        for (final PaperCard card : newCards) {
            this.addSingleCard(card, 1);
        }
    }

    /**
     * Adds the single card.
     *
     * @param card
     *            the card
     * @param qty
     *          quantity
     */
    public void addSingleCard(final PaperCard card, int qty) {
        this.qa.getCardPool().add(card, qty);

        // register card into that list so that it would appear as a new one.
        this.qa.getNewCardList().add(card, qty);
    }

    private static final Predicate<PaperCard> COMMON_PREDICATE = IPaperCard.Predicates.Presets.IS_COMMON;
    private static final Predicate<PaperCard> UNCOMMON_PREDICATE = IPaperCard.Predicates.Presets.IS_UNCOMMON;
    private static final Predicate<PaperCard> RARE_PREDICATE = IPaperCard.Predicates.Presets.IS_RARE_OR_MYTHIC;
    private static final Predicate<PaperCard> ONLY_RARE_PREDICATE = IPaperCard.Predicates.Presets.IS_RARE;
    private static final Predicate<PaperCard> MYTHIC_PREDICATE = IPaperCard.Predicates.Presets.IS_MYTHIC_RARE;


    /**
     * A predicate that takes into account the Quest Format (if any).
     * @param source
     *  the predicate to be added to the format predicate.
     * @return the composite predicate.
     */
    public Predicate<PaperCard> applyFormatFilter(Predicate<PaperCard> source) {
       return qc.getFormat() == null ? source : Predicates.and(source, qc.getFormat().getFilterPrinted());
    }

    /**
     * Adds the random rare.
     *
     * @return the card printed
     */
    public PaperCard addRandomRare() {

        final Predicate<PaperCard> myFilter = applyFormatFilter(QuestUtilCards.RARE_PREDICATE);

        final PaperCard card = Aggregates.random(Iterables.filter(FModel.getMagicDb().getCommonCards().getAllCards(), myFilter));
        this.addSingleCard(card, 1);
        return card;
    }

    /**
     * Adds a random common.
     *
     * @param n the number of cards to add
     * @return the list of cards added
     */
    public List<PaperCard> addRandomCommon(final int n) {
        final Predicate<PaperCard> myFilter = applyFormatFilter(QuestUtilCards.COMMON_PREDICATE);
        final List<PaperCard> newCards = Aggregates.random(Iterables.filter(FModel.getMagicDb().getCommonCards().getAllCards(), myFilter), n);
        this.addAllCards(newCards);
        return newCards;
    }

    /**
     * Adds a random uncommon.
     *
     * @param n the number of cards to add
     * @return the list of cards added
     */
    public List<PaperCard> addRandomUncommon(final int n) {
        final Predicate<PaperCard> myFilter = applyFormatFilter(QuestUtilCards.UNCOMMON_PREDICATE);
        final List<PaperCard> newCards = Aggregates.random(Iterables.filter(FModel.getMagicDb().getCommonCards().getAllCards(), myFilter), n);
        this.addAllCards(newCards);
        return newCards;
    }

    /**
     * Adds the random rare.
     *
     * @param n
     *            the n
     * @return the list
     */
    public List<PaperCard> addRandomRare(final int n) {
        final Predicate<PaperCard> myFilter = applyFormatFilter(QuestUtilCards.RARE_PREDICATE);

        final List<PaperCard> newCards = Aggregates.random(Iterables.filter(FModel.getMagicDb().getCommonCards().getAllCards(), myFilter), n);
        this.addAllCards(newCards);
        return newCards;
    }

    /**
     * Adds the random rare.
     *
     * @param n
     *            the n
     * @return the list
     */
    public List<PaperCard> addRandomRareNotMythic(final int n) {
        final Predicate<PaperCard> myFilter = applyFormatFilter(QuestUtilCards.ONLY_RARE_PREDICATE);

        final List<PaperCard> newCards = Aggregates.random(Iterables.filter(FModel.getMagicDb().getCommonCards().getAllCards(), myFilter), n);
        this.addAllCards(newCards);
        return newCards;
    }

    /**
     * Adds the random rare.
     *
     * @param n
     *            the n
     * @return the list
     */
    public List<PaperCard> addRandomMythicRare(final int n) {
        final Predicate<PaperCard> myFilter = applyFormatFilter(QuestUtilCards.MYTHIC_PREDICATE);

        final Iterable<PaperCard> cardPool = Iterables.filter(FModel.getMagicDb().getCommonCards().getAllCards(), myFilter);

        if (!cardPool.iterator().hasNext()) {
            return null;
        }

        final List<PaperCard> newCards = Aggregates.random(cardPool, n);
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
     * @param userPrefs
     *            user preferences
     */
    public void setupNewGameCardPool(final Predicate<PaperCard> filter, final int idxDifficulty, final StartingPoolPreferences userPrefs) {
        final int nC = this.qpref.getPrefInt(DifficultyPrefs.STARTING_COMMONS, idxDifficulty);
        final int nU = this.qpref.getPrefInt(DifficultyPrefs.STARTING_UNCOMMONS, idxDifficulty);
        final int nR = this.qpref.getPrefInt(DifficultyPrefs.STARTING_RARES, idxDifficulty);

        this.addAllCards(BoosterUtils.getQuestStarterDeck(filter, nC, nU, nR, userPrefs));
    }

    /**
     * Buy card.
     *
     * @param card
     *            the card
     * @param qty
     *          quantity
     * @param value
     *            the value
     */
    public void buyCard(final PaperCard card, int qty, final int value) {
        int totalCost = qty * value;
        if (this.qa.getCredits() >= totalCost) {
            this.qa.setCredits(this.qa.getCredits() - totalCost);
            this.addSingleCard(card, qty);
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
    public void buyPack(final SealedProduct booster, final int value) {
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
        if (fromDeck.has(DeckSection.Sideboard)) {
            this.addAllCards(fromDeck.get(DeckSection.Sideboard).toFlatList());
        }
    }

    /**
     * Sell card.
     *
     * @param card
     *            the card
     * @param qty
     *          quantity
     * @param pricePerCard
     *            the price per card
     */
    public void sellCard(final PaperCard card, int qty, final int pricePerCard) {
        this.sellCard(card, qty, pricePerCard, true);
    }

    /**
     * Lose card.
     * @param cards The cards to lose
     */
    public void loseCards(final List<PaperCard> cards) {
        for(PaperCard pc: cards)
            this.sellCard(pc, 1, 0, this.qc.getAssets().getItemLevel(QuestItemType.CASH_STAKES) > 0);
    }

    /**
     * Sell card.
     * @param card The card to sell.
     * @param qty The quantity of the card to sell.
     * @param pricePerCard The price of each card.
     * @param addToShop If true, this adds the sold cards to the shop's inventory.
     */
    private void sellCard(final PaperCard card, int qty, final int pricePerCard, final boolean addToShop) {
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
            if (nToRemoveFromThisDeck <= 0) {
                continue; // this is not the deck you are looking for
            }

            int nToRemoveFromSb = Math.min(cntInSb, nToRemoveFromThisDeck);
            if (nToRemoveFromSb > 0) {
                deck.get(DeckSection.Sideboard).remove(card, nToRemoveFromSb);
                nToRemoveFromThisDeck -= nToRemoveFromSb;
                if (0 >= nToRemoveFromThisDeck) {
                    continue; // done here
                }
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
        int winsNoLimit = FModel.getQuestPreferences().getPrefInt(QPref.SHOP_WINS_FOR_NO_SELL_LIMIT);
        int maxPrice = FModel.getQuestPreferences().getPrefInt(QPref.SHOP_MAX_SELLING_PRICE);

        return this.qc.getAchievements().getWin() < winsNoLimit ? maxPrice : Integer.MAX_VALUE;
    }

    /**
     * Generate cards in shop.
     */
    private final GameFormat.Collection formats = FModel.getFormats();
    private final Predicate<CardEdition> filterExt = this.formats.getExtended().editionLegalPredicate;

    /** The filter t2booster. */
    private final Predicate<CardEdition> filterT2booster = Predicates.and(CardEdition.Predicates.CAN_MAKE_BOOSTER,
            this.formats.getStandard().editionLegalPredicate);

    /** The filter ext but t2. */
    private final Predicate<CardEdition> filterExtButT2 = Predicates.and(
            CardEdition.Predicates.CAN_MAKE_BOOSTER,
            Predicates.and(this.filterExt, this.formats.getStandard().editionLegalPredicate));

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
    private void generateBoostersInShop(final int count) {
        for (int i = 0; i < count; i++) {
            final int rollD100 = MyRandom.getRandom().nextInt(100);
            Predicate<CardEdition> filter = rollD100 < 40 ? this.filterT2booster
                    : (rollD100 < 75 ? this.filterExtButT2 : this.filterNotExt);
            if (qc.getFormat() != null) {
                filter = Predicates.and(CardEdition.Predicates.CAN_MAKE_BOOSTER, isLegalInQuestFormat(qc.getFormat()));
            }
            Iterable<CardEdition> rightEditions = Iterables.filter(FModel.getMagicDb().getEditions(), filter);
            if (!rightEditions.iterator().hasNext()) {
                continue;
            }
            this.qa.getShopList().add(BoosterPack.FN_FROM_SET.apply(Aggregates.random(rightEditions)));
        }

        if (qpref.getPrefInt(QPref.SPECIAL_BOOSTERS) == 1) {
            for (String color : SealedProduct.specialSets) {
                for (int i = 0; i < count; i++) {
                    this.qa.getShopList().add(new BoosterPack(color, getColoredBoosterTemplate(color)));
                }
            }
        }

    }

    /**
     * Generate precons in shop.
     *
     * @param count
     *            the count
     */
    private void generateTournamentsInShop(final int count) {
        Predicate<CardEdition> formatFilter = CardEdition.Predicates.HAS_TOURNAMENT_PACK;
        if (qc.getFormat() != null) {
            formatFilter = Predicates.and(formatFilter, isLegalInQuestFormat(qc.getFormat()));
        }
        Iterable<CardEdition> rightEditions = Iterables.filter(FModel.getMagicDb().getEditions(), formatFilter);
        this.qa.getShopList().addAllFlat(Aggregates.random(Iterables.transform(rightEditions, TournamentPack.FN_FROM_SET), count));
    }

    /**
     * Generate precons in shop.
     *
     * @param count
     *            the count
     */
    private void generateFatPacksInShop(final int count) {
        Predicate<CardEdition> formatFilter = CardEdition.Predicates.HAS_FAT_PACK;
        if (qc.getFormat() != null) {
            formatFilter = Predicates.and(formatFilter, isLegalInQuestFormat(qc.getFormat()));
        }
        Iterable<CardEdition> rightEditions = Iterables.filter(FModel.getMagicDb().getEditions(), formatFilter);
        this.qa.getShopList().addAllFlat(Aggregates.random(Iterables.transform(rightEditions, FatPack.FN_FROM_SET), count));
    }

    private void generateBoosterBoxesInShop(final int count) {

        if (count == 0) {
            return;
        }

        Predicate<CardEdition> formatFilter = CardEdition.Predicates.HAS_BOOSTER_BOX;
        if (qc.getFormat() != null) {
            formatFilter = Predicates.and(formatFilter, isLegalInQuestFormat(qc.getFormat()));
        }
        Iterable<CardEdition> rightEditions = Iterables.filter(FModel.getMagicDb().getEditions(), formatFilter);

        List<CardEdition> editions = new ArrayList<>();
        for (CardEdition e : rightEditions) {
            editions.add(e);
        }

        Collections.shuffle(editions);

        int numberOfBoxes = Math.min(Math.max(count / 2, 1), editions.size());

        if (numberOfBoxes == 0) {
            return;
        }

        editions = editions.subList(0, numberOfBoxes);

        List<BoosterBox> output = new ArrayList<>();
        for (CardEdition e : editions) {
            output.add(BoosterBox.FN_FROM_SET.apply(e));
        }

        this.qa.getShopList().addAllFlat(output);

    }

    /**
     * Generate precons in shop.
     *
     * @param count
     *            the count
     */
    private void generatePreconsInShop(final int count) {
        final List<PreconDeck> meetRequirements = new ArrayList<>();
        for (final PreconDeck deck : QuestController.getPrecons()) {
            if (QuestController.getPreconDeals(deck).meetsRequiremnts(this.qc.getAchievements())
                    && (null == qc.getFormat() || qc.getFormat().isSetLegal(deck.getEdition()))) {
                meetRequirements.add(deck);
            }
        }
        this.qa.getShopList().addAllFlat(Aggregates.random(meetRequirements, count));
    }

    @SuppressWarnings("unchecked")
    private SealedProduct.Template getShopBoosterTemplate() {
        return new SealedProduct.Template(Lists.newArrayList(
            Pair.of(BoosterSlots.COMMON, this.qpref.getPrefInt(QPref.SHOP_SINGLES_COMMON)),
            Pair.of(BoosterSlots.UNCOMMON, this.qpref.getPrefInt(QPref.SHOP_SINGLES_UNCOMMON)),
            Pair.of(BoosterSlots.RARE_MYTHIC, this.qpref.getPrefInt(QPref.SHOP_SINGLES_RARE))
        ));
    }

    private SealedProduct.Template getBoosterTemplate() {
        return new SealedProduct.Template(ImmutableList.of(
            Pair.of(BoosterSlots.COMMON, this.qpref.getPrefInt(QPref.BOOSTER_COMMONS)),
            Pair.of(BoosterSlots.UNCOMMON, this.qpref.getPrefInt(QPref.BOOSTER_UNCOMMONS)),
            Pair.of(BoosterSlots.RARE_MYTHIC, this.qpref.getPrefInt(QPref.BOOSTER_RARES))
        ));
    }

    public static SealedProduct.Template getColoredBoosterTemplate(final String color) {
        if (FModel.getQuest().getFormat() == null) {
            return new Template("?", ImmutableList.of(
                    Pair.of(BoosterSlots.COMMON + ":color(\"" + color + "\"):!" + BoosterSlots.LAND, 11),
                    Pair.of(BoosterSlots.UNCOMMON + ":color(\"" + color + "\"):!" + BoosterSlots.LAND, 3),
                    Pair.of(BoosterSlots.RARE_MYTHIC + ":color(\"" + color + "\"):!" + BoosterSlots.LAND, 1),
                    Pair.of(BoosterSlots.LAND + ":color(\"" + color + "\")", 1))
            );
        } else {
            String restrictions = "";
            List<String> allowedSetCodes = FModel.getQuest().getFormat().getAllowedSetCodes();
            if (allowedSetCodes.isEmpty()) {
                for (String restrictedCard : FModel.getQuest().getFormat().getRestrictedCards()) {
                    restrictions += ":!name(\"" + restrictedCard + "\")";
                }
            } else {
                restrictions += ":fromSets(\"";
                for (String set : allowedSetCodes) {
                    restrictions += set + ",";
                }
                restrictions += ")";
            }
            return new Template("?", ImmutableList.of(
                    Pair.of(BoosterSlots.COMMON + ":color(\"" + color + "\"):!" + BoosterSlots.LAND + restrictions, 11),
                    Pair.of(BoosterSlots.UNCOMMON + ":color(\"" + color + "\"):!" + BoosterSlots.LAND + restrictions, 3),
                    Pair.of(BoosterSlots.RARE_MYTHIC + ":color(\"" + color + "\"):!" + BoosterSlots.LAND + restrictions, 1),
                    Pair.of(BoosterSlots.LAND + ":color(\"" + color + "\")" + restrictions, 1))
            );
        }
    }

    /**
     * Generate cards in shop.
     */
    private void generateCardsInShop() {
        // Preferences
        final int startPacks = this.qpref.getPrefInt(QPref.SHOP_STARTING_PACKS);
        final int winsForPack = this.qpref.getPrefInt(QPref.SHOP_WINS_FOR_ADDITIONAL_PACK);
        final int maxPacks = this.qpref.getPrefInt(QPref.SHOP_MAX_PACKS);
        final int minPacks = this.qpref.getPrefInt(QPref.SHOP_MIN_PACKS);

        int level = this.qc.getAchievements().getLevel();
        final int levelPacks = level > 0 ? startPacks / level : startPacks;
        final int winPacks = this.qc.getAchievements().getWin() / winsForPack;
        final int totalPacks = Math.min(Math.max(levelPacks + winPacks, minPacks), maxPacks);

        SealedProduct.Template tpl = getShopBoosterTemplate();
        UnOpenedProduct unopened = qc.getFormat() == null ?  new UnOpenedProduct(tpl) : new UnOpenedProduct(tpl, qc.getFormat().getFilterPrinted());

        for (int i = 0; i < totalPacks; i++) {
            this.qa.getShopList().addAllFlat(unopened.get());
        }

        this.generateBoostersInShop(totalPacks);
        this.generatePreconsInShop(totalPacks);
        this.generateTournamentsInShop(totalPacks);
        this.generateFatPacksInShop(totalPacks);
        this.generateBoosterBoxesInShop(totalPacks);
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
    public ItemPool<PaperCard> getCardpool() {
        return this.qa.getCardPool();
    }

    /**
     * Gets the shop list.
     *
     * @return the shop list
     */
    public ItemPool<InventoryItem> getShopList() {
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
    public ItemPool<InventoryItem> getNewCards() {
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

    public Function<Entry<? extends InventoryItem, Integer>, Object> getFnNewGet() {
        return this.fnNewGet;
    }

    public boolean isNew(InventoryItem item) {
        return qa.getNewCardList().contains(item);
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
            return isNew(from.getKey()) ? Integer.valueOf(1) : Integer.valueOf(0);
        }
    };

    /** The fn new get. */
    private final Function<Entry<? extends InventoryItem, Integer>, Object> fnNewGet =
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
        @Override
        public Object apply(final Entry<? extends InventoryItem, Integer> from) {
            return isNew(from.getKey()) ? "NEW" : "";
        }
    };

    public Function<Entry<InventoryItem, Integer>, Comparable<?>> getFnOwnedCompare() {
        return this.fnOwnedCompare;
    }

    public Function<Entry<? extends InventoryItem, Integer>, Object> getFnOwnedGet() {
        return this.fnOwnedGet;
    }

    public int getCompletionPercent(String edition) {

        for (String color : SealedProduct.specialSets) {
            if (color.equals(edition)) {
                return 0;
            }
        }

        if (edition.equals("?")) {
            return 0;
        }

        // get all cards in the specified edition
        Predicate<PaperCard> filter = IPaperCard.Predicates.printedInSet(edition);
        Iterable<PaperCard> editionCards = Iterables.filter(FModel.getMagicDb().getCommonCards().getAllCards(), filter);

        ItemPool<PaperCard> ownedCards = qa.getCardPool();
        // 100% means at least one of every basic land and at least 4 of every other card in the set
        int completeCards = 0;
        int numOwnedCards = 0;
        for (PaperCard card : editionCards) {
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
            if (i instanceof PaperCard) {
                return QuestUtilCards.this.qa.getCardPool().count((PaperCard) i);
            } else if (i instanceof PreconDeck) {
                PreconDeck pDeck = (PreconDeck) i;
                return FModel.getQuest().getMyDecks().contains(pDeck.getName()) ? -1 : -2;
            } else if (i instanceof SealedProduct) {
                SealedProduct oPack = (SealedProduct) i;
                return getCompletionPercent(oPack.getEdition()) - 103;
            }
            return null;
        }
    };

    private final Function<Entry<? extends InventoryItem, Integer>, Object> fnOwnedGet =
            new Function<Entry<? extends InventoryItem, Integer>, Object>() {
        @Override
        public Object apply(final Entry<? extends InventoryItem, Integer> from) {
            InventoryItem i = from.getKey();
            if (i instanceof PaperCard) {
                return QuestUtilCards.this.qa.getCardPool().count((PaperCard) i);
            } else if (i instanceof PreconDeck) {
                PreconDeck pDeck = (PreconDeck) i;
                return FModel.getQuest().getMyDecks().contains(pDeck.getName()) ? "YES" : "NO";
            } else if (i instanceof SealedProduct) {
                SealedProduct oPack = (SealedProduct) i;
                return String.format("%d%%", getCompletionPercent(oPack.getEdition()));
            }
            return null;
        }
    };
}
