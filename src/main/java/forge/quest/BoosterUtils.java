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
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.Singletons;
import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.card.MagicColor;
import forge.item.BoosterPack;
import forge.item.CardDb;
import forge.item.PaperCard;
import forge.item.IPaperCard;
import forge.item.InventoryItem;
import forge.item.PrintSheet;
import forge.item.TournamentPack;
import forge.util.Aggregates;
import forge.util.MyRandom;

// The BoosterPack generates cards for the Card Pool in Quest Mode
/**
 * <p>
 * QuestBoosterPack class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class BoosterUtils {

    /**
     * Gets the quest starter deck.
     * 
     * @param filter
     *            the filter
     * @param numCommon
     *            the num common
     * @param numUncommon
     *            the num uncommon
     * @param numRare
     *            the num rare
     * @return the quest starter deck
     */
    public static List<PaperCard> getQuestStarterDeck(final Predicate<PaperCard> filter, final int numCommon,
            final int numUncommon, final int numRare) {
        final ArrayList<PaperCard> cards = new ArrayList<PaperCard>();

        // Each color should have around the same amount of monocolored cards
        // There should be 3 Colorless cards for every 4 cards in a single color
        // There should be 1 Multicolor card for every 4 cards in a single color

        final List<Predicate<CardRules>> colorFilters = new ArrayList<Predicate<CardRules>>();
        colorFilters.add(CardRulesPredicates.Presets.IS_MULTICOLOR);

        for (int i = 0; i < 4; i++) {
            if (i != 2) {
                colorFilters.add(CardRulesPredicates.Presets.IS_COLORLESS);
            }

            colorFilters.add(CardRulesPredicates.isMonoColor(MagicColor.WHITE));
            colorFilters.add(CardRulesPredicates.isMonoColor(MagicColor.RED));
            colorFilters.add(CardRulesPredicates.isMonoColor(MagicColor.BLUE));
            colorFilters.add(CardRulesPredicates.isMonoColor(MagicColor.BLACK));
            colorFilters.add(CardRulesPredicates.isMonoColor(MagicColor.GREEN));
        }

        // This will save CPU time when sets are limited
        final List<PaperCard> cardpool = Lists.newArrayList(Iterables.filter(CardDb.instance().getAllCards(), filter));

        final Predicate<PaperCard> pCommon = IPaperCard.Predicates.Presets.IS_COMMON;
        cards.addAll(BoosterUtils.generateDefinetlyColouredCards(cardpool, pCommon, numCommon, colorFilters));

        final Predicate<PaperCard> pUncommon = IPaperCard.Predicates.Presets.IS_UNCOMMON;
        cards.addAll(BoosterUtils.generateDefinetlyColouredCards(cardpool, pUncommon, numUncommon, colorFilters));

        int nRares = numRare, nMythics = 0;
        final Predicate<PaperCard> filterMythics = IPaperCard.Predicates.Presets.IS_MYTHIC_RARE;
        final boolean haveMythics = Iterables.any(cardpool, filterMythics);
        for (int iSlot = 0; haveMythics && (iSlot < numRare); iSlot++) {
            if (MyRandom.getRandom().nextInt(10) < 1) {
                // 10% chance of upgrading a Rare into a Mythic
                nRares--;
                nMythics++;
            }
        }

        final Predicate<PaperCard> pRare = IPaperCard.Predicates.Presets.IS_RARE;
        cards.addAll(BoosterUtils.generateDefinetlyColouredCards(cardpool, pRare, nRares, colorFilters));
        if (nMythics > 0) {
            cards.addAll(BoosterUtils.generateDefinetlyColouredCards(cardpool, filterMythics, nMythics, colorFilters));
        }
        return cards;
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
     * @return a list of card names
     */
    private static ArrayList<PaperCard> generateDefinetlyColouredCards(final Iterable<PaperCard> source,
            final Predicate<PaperCard> filter, final int cntNeeded, final List<Predicate<CardRules>> allowedColors) {
        // If color is null, use colorOrder progression to grab cards
        final ArrayList<PaperCard> result = new ArrayList<PaperCard>();

        final int size = allowedColors == null ? 0 : allowedColors.size();
        Collections.shuffle(allowedColors);

        int cntMade = 0, iAttempt = 0;

        // This will prevent endless loop @ wh
        int allowedMisses = (2 + size + 2) * cntNeeded; // lol, 2+2 is not magic
                                                        // constant!

        while ((cntMade < cntNeeded) && (allowedMisses > 0)) {
            PaperCard card = null;

            if (size > 0) {
                final Predicate<CardRules> color2 = allowedColors.get(iAttempt % size);
                if (color2 != null) {
                    Predicate<PaperCard> color2c = Predicates.compose(color2, PaperCard.FN_GET_RULES);
                    card = Aggregates.random(Iterables.filter(source, Predicates.and(filter, color2c)));
                }
            }

            if (card == null) {
                // We can't decide on a color, so just pick a card.
                card = Aggregates.random(Iterables.filter(source, filter));
            }

            if ((card != null) && !result.contains(card)) {
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
        if (null == input || "random".equalsIgnoreCase(input))
            return Predicates.alwaysTrue();

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
        List<InventoryItem> rewards = new ArrayList<InventoryItem>();

        // last word starts with 'rare' ignore case
        if (temp.length > 1 && temp[temp.length-1].regionMatches(true, 0, "rare", 0, 4)) {
            // Type 1: 'n [color] rares'
            final int qty = Integer.parseInt(temp[0]);

            List<Predicate<PaperCard>> preds = new ArrayList<Predicate<PaperCard>>();
            preds.add(IPaperCard.Predicates.Presets.IS_RARE_OR_MYTHIC); // Determine rarity

            if (temp.length > 2) {
                Predicate<CardRules> cr = parseRulesLimitation(temp[1]);
                if(Predicates.alwaysTrue() != (Object)cr) // guava has a single instance for always-const predicates
                    preds.add(Predicates.compose(cr, PaperCard.FN_GET_RULES));
            }
            
            if (Singletons.getModel().getQuest().getFormat() != null) {
                preds.add(Singletons.getModel().getQuest().getFormat().getFilterPrinted());
            }

            PrintSheet ps = new PrintSheet("Quest rewards");
            Predicate<PaperCard> predicate = preds.size() == 1 ? preds.get(0) : Predicates.and(preds);  
            ps.addAll(Iterables.filter(CardDb.instance().getAllCards(), predicate));
            rewards.addAll(ps.random(qty, true));
        } else if (temp.length == 2 && temp[0].equalsIgnoreCase("duplicate") && temp[1].equalsIgnoreCase("card")) {
            // Type 2: a duplicate card of the players choice
            rewards.add(new QuestRewardCardDuplicate());
        } else if (temp.length >= 2 && temp[0].equalsIgnoreCase("chosen") && temp[1].equalsIgnoreCase("card")) {
            // Type 3: a duplicate card of the players choice
            rewards.add(new QuestRewardCardFiltered(temp));
        } else if (temp.length >= 3 && temp[0].equalsIgnoreCase("booster") && temp[1].equalsIgnoreCase("pack")) {
            // Type 4: a predetermined extra booster pack
            rewards.add(BoosterPack.FN_FROM_SET.apply(Singletons.getModel().getEditions().get(temp[2])));
        } else if (temp.length >= 3 && temp[0].equalsIgnoreCase("tournament") && temp[1].equalsIgnoreCase("pack")) {
            // Type 5: a predetermined extra tournament ("starter") pack
            rewards.add(TournamentPack.FN_FROM_SET.apply(Singletons.getModel().getEditions().get(temp[2])));
        }
        else if (temp.length > 0) {
            // default: assume we are asking for a single copy of a specific card
            final PaperCard specific = CardDb.instance().getCard(s);
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
        final List<InventoryItem> rewards = new ArrayList<InventoryItem>();

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
}
