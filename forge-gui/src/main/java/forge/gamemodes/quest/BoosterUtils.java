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

import static forge.gamemodes.quest.QuestUtilCards.isLegalInQuestFormat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.card.CardEdition;
import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.card.MagicColor;
import forge.card.PrintSheet;
import forge.game.GameFormat;
import forge.gamemodes.quest.data.QuestPreferences.QPref;
import forge.item.BoosterPack;
import forge.item.IPaperCard;
import forge.item.IPaperCard.Predicates.Presets;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.item.TournamentPack;
import forge.model.FModel;
import forge.util.Aggregates;
import forge.util.MyRandom;
import forge.util.PredicateString.StringOp;

/**
 * <p>
 * QuestBoosterPack class. Generates cards for the Card Pool in Quest Mode
 * </p>
 *
 * @author Forge
 * @version $Id$
 */
public final class BoosterUtils {

    private static final List<Byte> possibleColors = new ArrayList<>();

    private static final int RARES_PER_MYTHIC = 8;
    private static final int MAX_BIAS = 100; //Bias is a percentage; this is 100%

    private static final int[] COLOR_COUNT_PROBABILITIES = new int[] {
            1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 4, 4, 5, 6
    };

    private static final GameFormat.Collection  formats   = FModel.getFormats();
    private static final Predicate<CardEdition> filterPioneer = formats.getPioneer().editionLegalPredicate;
    private static final Predicate<CardEdition> filterModern= formats.getModern().editionLegalPredicate;

    private static final Predicate<CardEdition> filterStandard = Predicates.and(CardEdition.Predicates.CAN_MAKE_BOOSTER,
            formats.getStandard().editionLegalPredicate);

    private static final Predicate<CardEdition> filterPioneerNotStandard = Predicates.and(
            CardEdition.Predicates.CAN_MAKE_BOOSTER,
            Predicates.and(filterPioneer, Predicates.not(formats.getStandard().editionLegalPredicate)));

    private static final Predicate<CardEdition> filterModernNotPioneer = Predicates.and(
            CardEdition.Predicates.CAN_MAKE_BOOSTER,
            Predicates.and(filterModern, Predicates.not(filterPioneer)));

    /** The filter not ext. */
    private static final Predicate<CardEdition> filterNotModern = Predicates.and(CardEdition.Predicates.CAN_MAKE_BOOSTER,
            Predicates.not(filterModern));

    /**
     * Gets the quest starter deck.
     *
     * @param formatStartingPool
     *            the filter
     * @param numCommons
     *            the num common
     * @param numUncommons
     *            the num uncommon
     * @param numRares
     *            the num rare
     * @param userPrefs
     *            the starting pool preferences
     * @return the quest starter deck
     */
    public static List<PaperCard> getQuestStarterDeck(final GameFormat formatStartingPool, final int numCommons,
            final int numUncommons, final int numRares, final StartingPoolPreferences userPrefs) {

        if (possibleColors.isEmpty()) {
            possibleColors.add(MagicColor.BLACK);
            possibleColors.add(MagicColor.BLUE);
            possibleColors.add(MagicColor.GREEN);
            possibleColors.add(MagicColor.RED);
            possibleColors.add(MagicColor.WHITE);
            possibleColors.add(MagicColor.COLORLESS);
        }

        final List<PaperCard> cards = new ArrayList<>();

        if (userPrefs != null && userPrefs.getPoolType() == StartingPoolPreferences.PoolType.BOOSTERS) {

            for (InventoryItem inventoryItem : generateRandomBoosterPacks(userPrefs.getNumberOfBoosters(), formatStartingPool.editionLegalPredicate)) {
                cards.addAll(((BoosterPack) inventoryItem).getCards());
            }

            return cards;

        }

        Predicate<PaperCard> filter = Predicates.alwaysTrue();
        if (formatStartingPool != null) {
            filter = formatStartingPool.getFilterPrinted();
        }

        final List<PaperCard> cardPool = Lists.newArrayList(Iterables.filter(FModel.getMagicDb().getCommonCards().getAllNonPromoCards(), filter));

        if (userPrefs != null && userPrefs.grantCompleteSet()) {
            for (PaperCard card : cardPool) {
                cards.add(card);
                cards.add(card);
                cards.add(card);
                cards.add(card);
            }
            return cards;
        }

        final boolean allowDuplicates = userPrefs != null && userPrefs.allowDuplicates();
        final boolean mythicsAvailable = Iterables.any(cardPool, Presets.IS_MYTHIC_RARE);
        final int numMythics = mythicsAvailable ? numRares / RARES_PER_MYTHIC : 0;
        final int adjustedRares = numRares - numMythics;

        final List<Predicate<CardRules>> colorFilters = getColorFilters(userPrefs, cardPool);

        cards.addAll(BoosterUtils.generateCards(cardPool, Presets.IS_COMMON, numCommons, colorFilters, allowDuplicates));
        cards.addAll(BoosterUtils.generateCards(cardPool, Presets.IS_UNCOMMON, numUncommons, colorFilters, allowDuplicates));
        cards.addAll(BoosterUtils.generateCards(cardPool, Presets.IS_RARE, adjustedRares, colorFilters, allowDuplicates));

        if (numMythics > 0) {
            cards.addAll(BoosterUtils.generateCards(cardPool, Presets.IS_MYTHIC_RARE, numMythics, colorFilters, allowDuplicates));
        }

        return cards;

    }

    /**
     * Generates a number of booster packs from random editions using the current quest's prize pool format.
     * @param quantity The number of booster packs to generate
     * @return A list containing the booster packs
     */
    public static List<InventoryItem> generateRandomBoosterPacks(final int quantity, final QuestController questController) {
        if (questController.getFormat() != null) {
            return generateRandomBoosterPacks(quantity, isLegalInQuestFormat(questController.getFormat()));
        } else {
            final int rollD100 = MyRandom.getRandom().nextInt(100);
            // 30% Standard, 20% Pioneer, pre-standard -> 20% Modern, pre-pioneer -> 30% Pre-modern
            Predicate<CardEdition> rolledFilter;
            if (rollD100 < 30) {
                rolledFilter = filterStandard;
            } else if (rollD100 < 50) {
                rolledFilter = filterPioneerNotStandard;
            } else if (rollD100 < 70) {
                rolledFilter = filterModernNotPioneer;
            } else {
                rolledFilter = filterNotModern;
            }

            return generateRandomBoosterPacks(quantity, rolledFilter);
        }
    }

    /**
     * Generates a number of booster packs from random editions using the specified edition predicate.
     * @param quantity The number of booster packs to generate
     * @param editionFilter The filter to use for picking booster editions
     * @return A list containing the booster packs
     */
    public static List<InventoryItem> generateRandomBoosterPacks(final int quantity, final Predicate<CardEdition> editionFilter) {

        List<InventoryItem> output = new ArrayList<>();

        Predicate<CardEdition> filter = Predicates.and(CardEdition.Predicates.CAN_MAKE_BOOSTER, editionFilter);
        Iterable<CardEdition> possibleEditions = Iterables.filter(FModel.getMagicDb().getEditions(), filter);

        if (!possibleEditions.iterator().hasNext()) {
            System.err.println("No sets found in starting pool that can create boosters.");
            return output;
        }

        for (int i = 0; i < quantity; i++) {

            CardEdition edition = Aggregates.random(possibleEditions);
            BoosterPack pack = BoosterPack.FN_FROM_SET.apply(edition);

            if (pack != null) {
                output.add(pack);
            } else {
                System.err.println("Could not create booster of edition: " + edition);
            }

        }

        return output;

    }

    private static List<Predicate<CardRules>> getColorFilters(final StartingPoolPreferences userPrefs, final List<PaperCard> cardPool) {

        final List<Predicate<CardRules>> colorFilters = new ArrayList<>();

        if (userPrefs != null) {

            boolean includeArtifacts = userPrefs.includeArtifacts();

            final List<Byte> preferredColors = userPrefs.getPreferredColors();

            switch (userPrefs.getPoolType()) {

                case RANDOM_BALANCED:
                    preferredColors.clear();
                    int numberOfColors = COLOR_COUNT_PROBABILITIES[(int) (MyRandom.getRandom().nextDouble() * COLOR_COUNT_PROBABILITIES.length)];
                    if (numberOfColors < 6) {
                        Collections.shuffle(possibleColors);
                        for (int i = 0; i < numberOfColors; i++) {
                            preferredColors.add(possibleColors.get(i));
                        }
                    } else {
                        preferredColors.addAll(possibleColors);
                    }
                    includeArtifacts = MyRandom.getRandom().nextDouble() < 0.5;
                    // Fall through
                case BALANCED:
                    populateBalancedFilters(colorFilters, preferredColors, cardPool, includeArtifacts);
                    break;
                case RANDOM:
                    populateRandomFilters(colorFilters);
                    break;
                default:
                    // Do nothing

            }

        }

        return colorFilters;

    }

    private static void populateRandomFilters(final List<Predicate<CardRules>> colorFilters) {

        for (int i = 0; i < MAX_BIAS; i++) {
            Predicate<CardRules> predicate;
            byte color = possibleColors.get((int) (MyRandom.getRandom().nextDouble() * 6));
            if (MyRandom.getRandom().nextDouble() < 0.6) {
                predicate = CardRulesPredicates.isMonoColor(color);
            } else {
                predicate = CardRulesPredicates.hasColor(color);
            }
            if (MyRandom.getRandom().nextDouble() < 0.1) {
                predicate = Predicates.and(predicate, CardRulesPredicates.Presets.IS_MULTICOLOR);
            }
            colorFilters.add(predicate);
        }

    }

    private static void populateBalancedFilters(final List<Predicate<CardRules>> colorFilters, final List<Byte> preferredColors, final List<PaperCard> cardPool, final boolean includeArtifacts) {

        final List<Byte> otherColors = new ArrayList<>(possibleColors);
        otherColors.removeAll(preferredColors);

        int colorBias = FModel.getQuestPreferences().getPrefInt(QPref.STARTING_POOL_COLOR_BIAS);
        double preferredBias = 0;

        if (preferredColors.isEmpty()) {
            colorBias = 0;
        } else {
            preferredBias = (double) colorBias / preferredColors.size();
        }

        int usedMulticolor = 0, usedPhyrexian = 0;

        for (int i = 0; i < MAX_BIAS; i++) {

            if (i < colorBias) {

                int index = (int) ((double) i / preferredBias);
                for (@SuppressWarnings("unused") Byte ignored : otherColors) {

                    //Add artifacts here if there's no colorless selection
                    if (i % 8 == 0 && !preferredColors.contains(MagicColor.COLORLESS) && includeArtifacts) {
                        colorFilters.add(CardRulesPredicates.Presets.IS_ARTIFACT);
                    } else if (i % 5 == 0) {

                        //If colorless is the only color selected, add a small chance to get Phyrexian mana cost cards.
                        if (preferredColors.contains(MagicColor.COLORLESS) && preferredColors.size() == 1) {

                            Predicate<CardRules> predicateRules =  CardRulesPredicates.cost(StringOp.CONTAINS_IC, "p/");
                            Predicate<PaperCard> predicateCard = Predicates.compose(predicateRules, PaperCard.FN_GET_RULES);

                            int size = Iterables.size(Iterables.filter(cardPool, predicateCard));
                            int totalSize = cardPool.size();

                            double phyrexianAmount = (double) size / totalSize;
                            phyrexianAmount *= 125;

                            if (usedPhyrexian < Math.min(1, phyrexianAmount)) {
                                colorFilters.add(predicateRules);
                                usedPhyrexian++;
                                continue;
                            }

                        }

                        //Try to get multicolored cards that fit into the preferred colors.
                        Predicate<CardRules> predicateRules = Predicates.and(
                                CardRulesPredicates.isColor(preferredColors.get(index)),
                                CardRulesPredicates.Presets.IS_MULTICOLOR
                        );
                        Predicate<PaperCard> predicateCard = Predicates.compose(predicateRules, PaperCard.FN_GET_RULES);

                        //Adjust for the number of multicolored possibilities. This prevents flooding of non-selected
                        //colors if multicolored cards aren't in the selected sets. The more multi-colored cards in the
                        //sets, the more that will be selected.
                        if (usedMulticolor / 8 < Iterables.size(Iterables.filter(cardPool, predicateCard))) {
                            colorFilters.add(predicateRules);
                            usedMulticolor++;
                        } else {
                            //Exceeded multicolor-specific ratio, so here we add a more generic filter.
                            colorFilters.add(CardRulesPredicates.isColor(preferredColors.get(index)));
                        }

                    } else {
                        colorFilters.add(CardRulesPredicates.isMonoColor(preferredColors.get(index)));
                    }
                }

            } else {

                for (Byte color : otherColors) {
                    if (i % 6 == 0) {
                        colorFilters.add(Predicates.and(CardRulesPredicates.isColor(color), CardRulesPredicates.Presets.IS_MULTICOLOR));
                    } else {
                        colorFilters.add(CardRulesPredicates.isMonoColor(color));
                    }
                }

            }

        }

    }

    /**
     * Create the list of card names at random from the given pool.
     *
     * @param source
     *            an Iterable<CardPrinted>
     * @param filter
     *            Predicate<CardPrinted>
     * @param cntNeeded
     *            an int
     * @param allowedColors
     *            a List<Predicate<CardRules>>
     * @param allowDuplicates
     *            If true, multiple copies of the same card will be allowed to be generated.
     * @return a list of card names
     */
    private static List<PaperCard> generateCards(
            final Iterable<PaperCard> source, final Predicate<PaperCard> filter, final int cntNeeded,
            final List<Predicate<CardRules>> allowedColors, final boolean allowDuplicates) {

        //If color is null, use colorOrder progression to grab cards
        final List<PaperCard> result = new ArrayList<>();

        final int size = allowedColors == null ? 0 : allowedColors.size();
        if (allowedColors != null) {
            Collections.shuffle(allowedColors);
        }

        int cntMade = 0, iAttempt = 0;

        //This will prevent endless loop @ wh
        int allowedMisses = (size + 4) * cntNeeded;
        int nullMisses = 0;

        while (cntMade < cntNeeded && allowedMisses > 0) {
            PaperCard card = null;

            if (size > 0) {
                final Predicate<CardRules> color2 = allowedColors.get(iAttempt % size);
                int colorMisses = 0;
                //Try a few times to get a card using the available filter. This is important for sets with only a small
                //handful of multi-colored cards.
                do {
                    if (color2 != null) {
                        Predicate<PaperCard> color2c = Predicates.compose(color2, PaperCard.FN_GET_RULES);
                        card = Aggregates.random(Iterables.filter(source, Predicates.and(filter, color2c)));
                    }
                } while (card == null && colorMisses++ < 10);
            }

            if (card == null) {
                //We can't decide on a color. We're going to try very hard to pick a color within the current filters.
                if (nullMisses++ < 10) {
                    iAttempt++;
                    continue;
                }
                nullMisses = 0;
                //Still no luck. We're going to skip generating this card. This will very, very rarely result in fewer
                //cards than expected; however, it will keep unselected colors out of the pool.
            }

            if ((card != null) && (allowDuplicates || !result.contains(card))) {
                result.add(card);
                cntMade++;
            } else {
                allowedMisses--;
            }
            iAttempt++;
        }

        return result;
    }


    /**
     * Parse a limitation for a reward or chosen card.
     * @param input
     *      String, the limitation as text.
     * @return Predicate<CardRules> the text parsed into a CardRules predicate.
     *
     */
    public static Predicate<CardRules> parseRulesLimitation(final String input) {
        if (null == input || "random".equalsIgnoreCase(input)) {
            return Predicates.alwaysTrue();
        }

        if (input.equalsIgnoreCase("black"))          return CardRulesPredicates.Presets.IS_BLACK;
        if (input.equalsIgnoreCase("blue"))           return CardRulesPredicates.Presets.IS_BLUE;
        if (input.equalsIgnoreCase("green"))          return CardRulesPredicates.Presets.IS_GREEN;
        if (input.equalsIgnoreCase("red"))            return CardRulesPredicates.Presets.IS_RED;
        if (input.equalsIgnoreCase("white"))          return CardRulesPredicates.Presets.IS_WHITE;
        if (input.equalsIgnoreCase("colorless"))      return CardRulesPredicates.Presets.IS_COLORLESS;
        if (input.equalsIgnoreCase("multicolor"))     return CardRulesPredicates.Presets.IS_MULTICOLOR;

        if (input.equalsIgnoreCase("land"))           return CardRulesPredicates.Presets.IS_LAND;
        if (input.equalsIgnoreCase("creature"))       return CardRulesPredicates.Presets.IS_CREATURE;
        if (input.equalsIgnoreCase("artifact"))       return CardRulesPredicates.Presets.IS_ARTIFACT;
        if (input.equalsIgnoreCase("planeswalker"))   return CardRulesPredicates.Presets.IS_PLANESWALKER;
        if (input.equalsIgnoreCase("instant"))        return CardRulesPredicates.Presets.IS_INSTANT;
        if (input.equalsIgnoreCase("sorcery"))        return CardRulesPredicates.Presets.IS_SORCERY;
        if (input.equalsIgnoreCase("enchantment"))    return CardRulesPredicates.Presets.IS_ENCHANTMENT;

        throw new IllegalArgumentException("No CardRules limitations could be parsed from: " + input);
    }
    /**
     * parseReward - used internally to parse individual items in a challenge reward definition.
     * @param s
     *      String, the reward to parse
     * @return List<CardPrinted>
     */
    private static List<InventoryItem> parseReward(final String s) {

        String[] temp = s.split(" ");
        List<InventoryItem> rewards = new ArrayList<>();

        // last word starts with 'rare' ignore case
        if (temp.length > 1 && temp[temp.length - 1].regionMatches(true, 0, "rare", 0, 4)) {
            // Type 1: 'n [color] rares'
            final int qty = Integer.parseInt(temp[0]);

            List<Predicate<PaperCard>> preds = new ArrayList<>();
            preds.add(IPaperCard.Predicates.Presets.IS_RARE_OR_MYTHIC); // Determine rarity

            if (temp.length > 2) {
                Predicate<CardRules> cr = parseRulesLimitation(temp[1]);
                //noinspection RedundantCast
                if (Predicates.alwaysTrue() != (Object) cr) { // guava has a single instance for always-const predicates
                    preds.add(Predicates.compose(cr, PaperCard.FN_GET_RULES));
                }
            }

            if (FModel.getQuest().getFormat() != null) {
                preds.add(FModel.getQuest().getFormat().getFilterPrinted());
            }

            PrintSheet ps = new PrintSheet("Quest rewards");
            Predicate<PaperCard> predicate = preds.size() == 1 ? preds.get(0) : Predicates.and(preds);
            ps.addAll(Iterables.filter(FModel.getMagicDb().getCommonCards().getAllNonPromoCards(), predicate));
            rewards.addAll(ps.random(qty, true));
        } else if (temp.length == 2 && temp[0].equalsIgnoreCase("duplicate") && temp[1].equalsIgnoreCase("card")) {
            // Type 2: a duplicate card of the players choice
            rewards.add(new QuestRewardCardDuplicate());
        } else if (temp.length >= 2 && temp[0].equalsIgnoreCase("chosen") && temp[1].equalsIgnoreCase("card")) {
            // Type 3: a duplicate card of the players choice
            rewards.add(new QuestRewardCardFiltered(temp));
        } else if (temp.length >= 3 && temp[0].equalsIgnoreCase("booster") && temp[1].equalsIgnoreCase("pack")) {
            // Type 4: a predetermined extra booster pack
            rewards.add(BoosterPack.FN_FROM_SET.apply(FModel.getMagicDb().getEditions().get(temp[2])));
        } else if (temp.length >= 3 && temp[0].equalsIgnoreCase("tournament") && temp[1].equalsIgnoreCase("pack")) {
            // Type 5: a predetermined extra tournament ("starter") pack
            rewards.add(TournamentPack.FN_FROM_SET.apply(FModel.getMagicDb().getEditions().get(temp[2])));
        }
        else if (temp.length > 0) {
            // default: assume we are asking for a single copy of a specific card
            final PaperCard specific = FModel.getMagicDb().getCommonCards().getCard(s);
            if (specific != null) {
                rewards.add(specific);
            }
        }
        // Return the duplicate, a specified card, or an empty list
        return rewards;
    }


    /**
     * <p>
     * generateCardRewardList.
     * </p>
     * Takes a reward list string, parses, and returns list of cards rewarded.
     *
     * @param s
     *            Properties string of reward (97 multicolor rares)
     * @return List<CardPrinted>
     */
    public static List<InventoryItem> generateCardRewardList(final String s) {
        if (StringUtils.isBlank(s)) {
            return null;
        }

        final String[] items = s.split(";");
        final List<InventoryItem> rewards = new ArrayList<>();

        for (final String item : items) {

            String input = null;

            if (item.contains("%")) {
                String[] tmp = item.split("%");
                final int chance = Integer.parseInt(tmp[0].trim());
                if (chance > 0 && tmp.length > 1 && MyRandom.percentTrue(chance)) {
                    input = tmp[1].trim();
                }
            } else {
                input = item;
            }
            if (input != null) {
                List<InventoryItem> reward = parseReward(input);

                if (reward != null) {
                    rewards.addAll(reward);
                }
            }
        }

        return rewards;
    }

    public static void sort(List<PaperCard> cards) {
        //sort cards alphabetically so colors appear together and rares appear on top
        Collections.sort(cards, new Comparator<PaperCard>() {
            @Override
            public int compare(PaperCard c1, PaperCard c2) {
                return c1.getName().compareTo(c2.getName());
            }
        });
        Collections.sort(cards, new Comparator<PaperCard>() {
            @Override
            public int compare(PaperCard c1, PaperCard c2) {
                return c1.getRules().getColor().compareTo(c2.getRules().getColor());
            }
        });
        Collections.sort(cards, new Comparator<PaperCard>() {
            @Override
            public int compare(PaperCard c1, PaperCard c2) {
                return c2.getRarity().compareTo(c1.getRarity());
            }
        });
    }
}
