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
package forge.gamemodes.quest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import forge.card.*;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.GameFormat;
import forge.gamemodes.quest.bazaar.QuestItemType;
import forge.gamemodes.quest.data.GameFormatQuest;
import forge.gamemodes.quest.data.QuestAssets;
import forge.gamemodes.quest.data.QuestPreferences;
import forge.gamemodes.quest.data.QuestPreferences.DifficultyPrefs;
import forge.gamemodes.quest.data.QuestPreferences.QPref;
import forge.item.*;
import forge.item.generation.BoosterSlots;
import forge.item.generation.UnOpenedProduct;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is a helper class to execute operations on QuestData. It has been
 * created to decrease complexity of questData class
 */
public final class QuestUtilCards {

    private final QuestController  questController;
    private final QuestPreferences questPreferences;
    private final QuestAssets      questAssets;

    public QuestUtilCards(final QuestController questController) {
        this.questController = questController;
        questAssets = questController.getAssets();
        questPreferences = FModel.getQuestPreferences();
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
        List<String> wastesCodes = new ArrayList<>();

        if (usedFormat != null) {
            List<String> availableEditions = usedFormat.getAllowedSetCodes();

            for (String edCode : availableEditions) {
                CardEdition ed = FModel.getMagicDb().getEditions().get(edCode);
                // Duel decks might have only 2 types of basic lands
                if (CardEdition.Predicates.hasBasicLands.test(ed)) {
                    landCodes.add(edCode);
                }
            }
            if (usedFormat.isSetLegal("ICE")) {
                snowLandCodes.add("ICE");
            }
            if (usedFormat.isSetLegal("CSP")) {
                snowLandCodes.add("CSP");
            }
            if (usedFormat.isSetLegal("KHM")) {
                snowLandCodes.add("KHM");
            }

            if (usedFormat.isSetLegal("OGW")) {
                wastesCodes.add("OGW");
            }
        } else {
            FModel.getMagicDb().getEditions().stream()
                    .filter(CardEdition.Predicates.hasBasicLands)
                    .map(CardEdition::getCode)
                    .forEach(landCodes::add);
            snowLandCodes.add("ICE");
            snowLandCodes.add("CSP");
            snowLandCodes.add("KHM");
            wastesCodes.add("OGW");
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

        if (!wastesCodes.isEmpty()) {
            String wasteCode = Aggregates.random(wastesCodes);
            pool.add(db.getCard("Wastes", wasteCode), 5);
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
        return new UnOpenedProduct(getBoosterTemplate(), fSets).get();
    }

    /**
     * Adds the all cards.
     *
     * @param newCards
     *            the new cards
     */
    public void addAllCards(final Iterable<PaperCard> newCards) {
        for (final PaperCard card : newCards) {
            addSingleCard(card, 1);
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
        questAssets.getCardPool().add(card, qty);

        // register card into that list so that it would appear as a new one.
        questAssets.getNewCardList().add(card, qty);
    }

    /**
     * A predicate that takes into account the Quest Format (if any).
     * @param source
     *  the predicate to be added to the format predicate.
     * @return the composite predicate.
     */
    public Predicate<PaperCard> applyFormatFilter(Predicate<PaperCard> source) {
        return questController.getFormat() == null ? source : source.and(questController.getFormat().getFilterPrinted());
    }

    /**
     * @return a stream of cards that can be rewarded according to the quest format and preferences.
     */
    private Stream<PaperCard> getQuestCardPool() {
        Stream<PaperCard> pool = FModel.getMagicDb().getCommonCards().streamAllCards();
        if(!(questPreferences.getPrefInt(QPref.EXCLUDE_PROMOS_FROM_POOL) == 0))
            pool = pool.filter(CardDb.EDITION_NON_PROMO);
        if(questController.getFormat() != null)
            pool = pool.filter(questController.getFormat().getFilterPrinted());
        return pool;
    }

    /**
     * Adds the random rare.
     *
     * @return the card printed
     */
    public PaperCard addRandomRare() {
        Stream<PaperCard> pool = getQuestCardPool();
        final PaperCard card = pool
                .filter(applyFormatFilter(PaperCardPredicates.IS_RARE_OR_MYTHIC))
                .collect(StreamUtil.random()).get();

        addSingleCard(card, 1);
        return card;
    }

    /**
     * Adds n random cards that match the given predicate (and all other format restrictions)
     * to the player card pool.
     *
     * @param n         Number of cards to add from the reward pool.
     * @param predicate Filter to select possible new cards from.
     * @return the list of cards added to the card pool.
     */
    public List<PaperCard> addRandomCards(final int n, Predicate<PaperCard> predicate) {
        Stream<PaperCard> pool = getQuestCardPool();
        final List<PaperCard> newCards = pool.filter(predicate).collect(StreamUtil.random(n));

        addAllCards(newCards);
        return newCards;
    }

    /**
     * Setup new game card pool.
     *
     * @param formatStartingPool
     *            the starting pool format for the new quest
     * @param idxDifficulty
     *            the idx difficulty
     * @param userPrefs
     *            user preferences
     */
    public void setupNewGameCardPool(final GameFormat formatStartingPool, final int idxDifficulty, final StartingPoolPreferences userPrefs) {
        //Add additional cards to the starter card pool based on variant if applicable
        double variantModifier = 1;
        switch(FModel.getQuest().getDeckConstructionRules()){
            case Default: break;
            case Commander: variantModifier = 2; break;
        }

        final int nC = (int)(questPreferences.getPrefInt(DifficultyPrefs.STARTING_COMMONS, idxDifficulty) * variantModifier);
        final int nU = (int)(questPreferences.getPrefInt(DifficultyPrefs.STARTING_UNCOMMONS, idxDifficulty) * variantModifier);
        final int nR = (int)(questPreferences.getPrefInt(DifficultyPrefs.STARTING_RARES, idxDifficulty) * variantModifier);

        addAllCards(BoosterUtils.getQuestStarterDeck(formatStartingPool, nC, nU, nR, userPrefs));

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
        if (questAssets.getCredits() >= totalCost) {
            questAssets.setCredits(questAssets.getCredits() - totalCost);
            addSingleCard(card, qty);
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
        if (questAssets.getCredits() >= value) {
            questAssets.setCredits(questAssets.getCredits() - value);
            addAllCards(booster.getCards());
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
        if (questAssets.getCredits() >= value) {
            questAssets.subtractCredits(value);
            addDeck(precon.getDeck());
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
        questController.getMyDecks().add(fromDeck);
        addAllCards(fromDeck.getAllCardsInASinglePool().toFlatList());
    }

    /**
     * Removes the list of cards from the card pool and returns them to the card shop if Cash Stakes are owned.
     * @param cards The cards to lose.
     */
    public void loseCards(final List<PaperCard> cards) {

        for(PaperCard card : cards) {

	        removeCard(card, 1);

	        if (questAssets.getItemLevel(QuestItemType.CASH_STAKES) > 0) {
		        addCardToShop(card);
	        }

        }

    }

    public void addCardToShop(final PaperCard card) {
    	questAssets.getShopList().add(card);
    }

    /**
     * This removes a card from the quest pool and any decks that use it.
     * @param card The card to remove.
     * @param qty The quantity of the card to remove.
     */
    public void removeCard(final PaperCard card, int qty) {

        questAssets.getCardPool().remove(card, qty);

        final int leftInPool = questAssets.getCardPool().count(card);

        // If card is a nonfoil basic land of the "free" kind, do not remove from the deck
        // but pretend as if it was added through "Add Basic Land".
        if ((!card.isFoil())
                && (card.isVeryBasicLand())) {
            return;
        }

        // remove sold cards from all decks:
        for (final Deck deck : questController.getMyDecks()) {

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
                if (nToRemoveFromThisDeck <= 0) {
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
        if (questAssets.getShopList() != null) {
            questAssets.getShopList().clear();
        }
    }

    /**
     * Gets the sell mutliplier.
     *
     * @return the sell mutliplier
     */
    public double getSellMultiplier() {

        double baseMultiplier = Double.parseDouble(questPreferences.getPref(QPref.SHOP_SELLING_PERCENTAGE_BASE))/100.0;
        double maxMultiplier = Double.parseDouble(questPreferences.getPref(QPref.SHOP_SELLING_PERCENTAGE_MAX))/100.0;

        double multi = baseMultiplier + (0.001 * questController.getAchievements().getWin());
        if (maxMultiplier > 0 && multi > maxMultiplier) {
            multi = maxMultiplier;
        }

        final int lvlEstates = questController.getMode() == QuestMode.Fantasy ? questAssets.getItemLevel(QuestItemType.ESTATES) : 0;

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
        int winsNoLimit = questPreferences.getPrefInt(QPref.SHOP_WINS_FOR_NO_SELL_LIMIT);
        int maxPrice = questPreferences.getPrefInt(QPref.SHOP_MAX_SELLING_PRICE);

        return questController.getAchievements().getWin() < winsNoLimit ? maxPrice : Integer.MAX_VALUE;
    }

    /**
     * Helper predicate for shops: is legal in quest format.
     *
     * @param qFormat
     *            the quest format
     * @return the predicate
     */
    public static Predicate<CardEdition> isLegalInQuestFormat(final GameFormatQuest qFormat) {
        if(qFormat == null)
            return x -> true;
        return edition -> qFormat.isSetLegal(edition.getCode());
    }


    /**
     * Generates a number of special booster packs from random editions using
     * the current quest's prize pool format.
     *
     * @param quantity The number of booster packs to generate
     * @return A list containing the booster packs
     */
    private List<InventoryItem> generateRandomSpecialBoosterPacks(final int quantity) {

        List<InventoryItem> output = new ArrayList<>();

        for (int i = 0; i < quantity; i++) {
            String color = SealedProduct.specialSets.get(MyRandom.getRandom().nextInt(SealedProduct.specialSets.size()));
            output.add(new BoosterPack(color, getColoredBoosterTemplate(color)));
        }

        return output;

    }

    /**
     * Generate boosters in shop.
     *
     * @param quantity the count
     */
    private void generateBoostersInShop(final int quantity) {

    	questAssets.getShopList().addAllFlat(BoosterUtils.generateRandomBoosterPacks(quantity, questController));

        if (questPreferences.getPrefInt(QPref.SPECIAL_BOOSTERS) == 1) {
        	questAssets.getShopList().addAllFlat(generateRandomSpecialBoosterPacks(quantity));
        }

    }

    /**
     * Generate boosters in shop.
     *
     * @param quantity the count
     */
    private void generateSinglesInShop(final int quantity) {
        // This is the spot we need to change
        SealedTemplate boosterTemplate = getShopBoosterTemplate();
        if (questController.getFormat() == null) {
		    for (int i = 0; i < quantity; i++) {
			    questAssets.getShopList().addAllOfTypeFlat(new UnOpenedProduct(boosterTemplate).get());
		    }
		    return;
	    } else {
            for (int i = 0; i < quantity; i++) {
                // Unopened product based on format of the cards?
                questAssets.getShopList().addAllOfTypeFlat(new UnOpenedProduct(boosterTemplate, questController.getFormat().getFilterPrinted()).get());
            }
        }
    }

    private static int getRandomCardFromBooster(final List<PaperCard> cards, final Predicate<PaperCard> predicate, final List<PaperCard> toAddTo, final int amount) {

    	if (amount <= 0) {
    		return 0;
	    }

    	//TODO Replace me with Java 8 streams and filters
    	List<PaperCard> temp = new ArrayList<>();

	    for (PaperCard card : cards) {
		    if (predicate.test(card)) {
		    	temp.add(card);
		    }
	    }

	    if (!temp.isEmpty()) {
		    toAddTo.add(temp.get((int) (MyRandom.getRandom().nextDouble() * temp.size())));
		    return amount - 1;
	    }

	    return amount;

    }

    /**
     * Generate precons in shop.
     *
     * @param count
     *            the count
     */
    private void generateTournamentsInShop(final int count) {
        List<TournamentPack> packs = FModel.getMagicDb().getEditions().stream()
                .filter(CardEdition.Predicates.HAS_TOURNAMENT_PACK)
                .filter(isLegalInQuestFormat(questController.getFormat()))
                .map(TournamentPack::fromSet)
                .collect(StreamUtil.random(count));
        questAssets.getShopList().addAllOfTypeFlat(packs);
    }

    /**
     * Generate precons in shop.
     *
     * @param count
     *            the count
     */
    private void generateFatPacksInShop(final int count) {
        List<FatPack> packs = FModel.getMagicDb().getEditions().stream()
                .filter(CardEdition.Predicates.HAS_FAT_PACK)
                .filter(isLegalInQuestFormat(questController.getFormat()))
                .map(FatPack::fromSet)
                .collect(StreamUtil.random(count));
        questAssets.getShopList().addAllOfTypeFlat(packs);
    }

    private void generateBoosterBoxesInShop(final int count) {

        if (count == 0) {
            return;
        }

        Predicate<CardEdition> formatFilter = CardEdition.Predicates.HAS_BOOSTER_BOX;
        if (questController.getFormat() != null) {
            formatFilter = formatFilter.and(isLegalInQuestFormat(questController.getFormat()));
        }

        List<CardEdition> editions = FModel.getMagicDb().getEditions().stream()
                .filter(formatFilter).collect(Collectors.toList());

        Collections.shuffle(editions);

        int numberOfBoxes = Math.min(Math.max(count / 2, 1), editions.size());

        if (numberOfBoxes == 0) {
            return;
        }

        editions = editions.subList(0, numberOfBoxes);

        List<BoosterBox> output = new ArrayList<>();
        for (CardEdition e : editions) {
            output.add(BoosterBox.fromSet(e));
        }

        questAssets.getShopList().addAllOfTypeFlat(output);

    }

    /**
     * Generate precons in shop.
     *
     * @param count
     *            the count
     */
    private void generatePreconsInShop(final int count) {
        Predicate<PreconDeck> formatFilter = deck -> QuestController.getPreconDeals(deck).meetsRequiremnts(questController.getAchievements());
        if (questController.getFormat() != null) {
            formatFilter = formatFilter.and(deck -> questController.getFormat().isSetLegal(deck.getEdition()));
        }
        final List<PreconDeck> decks = QuestController.getPrecons().stream()
                .filter(formatFilter)
                .collect(StreamUtil.random(count));
        questAssets.getShopList().addAllOfTypeFlat(decks);
    }

    @SuppressWarnings("unchecked")
    private SealedTemplate getShopBoosterTemplate() {
        return new SealedTemplate(Lists.newArrayList(
            Pair.of(BoosterSlots.COMMON, questPreferences.getPrefInt(QPref.SHOP_SINGLES_COMMON)),
            Pair.of(BoosterSlots.UNCOMMON, questPreferences.getPrefInt(QPref.SHOP_SINGLES_UNCOMMON)),
            Pair.of(BoosterSlots.RARE_MYTHIC, questPreferences.getPrefInt(QPref.SHOP_SINGLES_RARE))
        ));
    }

    private SealedTemplate getBoosterTemplate() {
        return new SealedTemplate(ImmutableList.of(
            Pair.of(BoosterSlots.COMMON, questPreferences.getPrefInt(QPref.BOOSTER_COMMONS)),
            Pair.of(BoosterSlots.UNCOMMON, questPreferences.getPrefInt(QPref.BOOSTER_UNCOMMONS)),
            Pair.of(BoosterSlots.RARE_MYTHIC, questPreferences.getPrefInt(QPref.BOOSTER_RARES))
        ));
    }

    public static SealedTemplate getColoredBoosterTemplate(final String color) {
        if (FModel.getQuest().getFormat() == null) {
            return new SealedTemplate("?", ImmutableList.of(
                    Pair.of(BoosterSlots.COMMON + ":color(\"" + color + "\"):!" + BoosterSlots.LAND, 11),
                    Pair.of(BoosterSlots.UNCOMMON + ":color(\"" + color + "\"):!" + BoosterSlots.LAND, 3),
                    Pair.of(BoosterSlots.RARE_MYTHIC + ":color(\"" + color + "\"):!" + BoosterSlots.LAND, 1),
                    Pair.of(BoosterSlots.LAND + ":color(\"" + color + "\")", 1))
            );
        } else {
            StringBuilder restrictions    = new StringBuilder();
            List<String>  allowedSetCodes = FModel.getQuest().getFormat().getAllowedSetCodes();
            if (allowedSetCodes.isEmpty()) {
                for (String restrictedCard : FModel.getQuest().getFormat().getRestrictedCards()) {
                    restrictions.append(":!name(\"").append(restrictedCard).append("\")");
                }
            } else {
                restrictions.append(":fromSets(\"");
                for (String set : allowedSetCodes) {
                    restrictions.append(set).append(",");
                }
                restrictions.append(")");
            }
            return new SealedTemplate("?", ImmutableList.of(
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
        final int startPacks = questPreferences.getPrefInt(QPref.SHOP_STARTING_PACKS);
        final int winsForPack = questPreferences.getPrefInt(QPref.SHOP_WINS_FOR_ADDITIONAL_PACK);
        final int maxPacks = questPreferences.getPrefInt(QPref.SHOP_MAX_PACKS);
        final int minPacks = questPreferences.getPrefInt(QPref.SHOP_MIN_PACKS);

        int level = questController.getAchievements().getLevel();
        final int levelPacks = level > 0 ? startPacks / level : startPacks;
        final int winPacks = questController.getAchievements().getWin() / winsForPack;
        final int totalPacks = Math.min(Math.max(levelPacks + winPacks, minPacks), maxPacks);

        generateSinglesInShop(totalPacks);

        generateBoostersInShop(totalPacks);
        generatePreconsInShop(totalPacks);
        generateTournamentsInShop(totalPacks);
        generateFatPacksInShop(totalPacks);
        generateBoosterBoxesInShop(totalPacks);

        if (questController.getFormat() == null || questController.getFormat().hasSnowLands()) {
	        // Spell shop no longer sells basic lands (we use "Add Basic Lands" instead)
	        questAssets.getShopList().addAllOfType(generateBasicLands(0, 5, questController.getFormat()));
        }

    }

    /**
     * Gets the cardpool.
     *
     * @return the cardpool
     */
    public ItemPool<PaperCard> getCardpool() {
        return questAssets.getCardPool();
    }

    /**
     * Gets the shop list.
     *
     * @return the shop list
     */
    public ItemPool<InventoryItem> getShopList() {
        if (questAssets.getShopList().isEmpty()) {
            generateCardsInShop();
        }
        return questAssets.getShopList();
    }

    /**
     * Gets the new cards.
     *
     * @return the new cards
     */
    public ItemPool<InventoryItem> getNewCards() {
        return questAssets.getNewCardList();
    }

    /**
     * Reset new list.
     */
    public void resetNewList() {
        questAssets.getNewCardList().clear();
    }

    public Function<Entry<InventoryItem, Integer>, Comparable<?>> getFnNewCompare() {
        return fnNewCompare;
    }

    public Function<Entry<? extends InventoryItem, Integer>, Object> getFnNewGet() {
        return fnNewGet;
    }

    public boolean isNew(InventoryItem item) {
        return questAssets.getNewCardList().contains(item);
    }

    // These functions provide a way to sort and compare cards in a table
    // according to their new-ness
    // It might be a good idea to store them in a base class for both quest-mode
    // deck editors
    // Maybe we should consider doing so later
    /** The fn new compare. */
    private final Function<Entry<InventoryItem, Integer>, Comparable<?>> fnNewCompare = from -> isNew(from.getKey()) ? Integer.valueOf(1) : Integer.valueOf(0);

    /** The fn new get. */
    private final Function<Entry<? extends InventoryItem, Integer>, Object> fnNewGet = from -> isNew(from.getKey()) ? "NEW" : "";

    public Function<Entry<InventoryItem, Integer>, Comparable<?>> getFnOwnedCompare() {
        return fnOwnedCompare;
    }

    public Function<Entry<? extends InventoryItem, Integer>, Object> getFnOwnedGet() {
        return fnOwnedGet;
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
        Predicate<PaperCard> filter = PaperCardPredicates.printedInSet(edition);
        Iterable<PaperCard> editionCards = IterableUtil.filter(FModel.getMagicDb().getCommonCards().getAllCards(), filter);

        ItemPool<PaperCard> ownedCards = questAssets.getCardPool();
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
    private final Function<Entry<InventoryItem, Integer>, Comparable<?>> fnOwnedCompare = new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
        @Override
        public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
            InventoryItem i = from.getKey();
            if (i instanceof PaperCard) {
                return questAssets.getCardPool().count((PaperCard) i);
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

    private final Function<Entry<? extends InventoryItem, Integer>, Object> fnOwnedGet = new Function<Entry<? extends InventoryItem, Integer>, Object>() {
        @Override
        public Object apply(final Entry<? extends InventoryItem, Integer> from) {
            InventoryItem i = from.getKey();
            if (i instanceof PaperCard) {
                return questAssets.getCardPool().count((PaperCard) i);
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
