/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import forge.util.TextUtil;
import org.apache.commons.lang3.tuple.ImmutablePair;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.card.CardEdition;
import forge.gamemodes.quest.data.QuestPreferences.QPref;
import forge.gamemodes.quest.io.ReadPriceList;
import forge.gui.GuiBase;
import forge.gui.util.SGuiChoose;
import forge.gui.util.SOptionPane;
import forge.item.PaperCard;
import forge.item.SealedProduct;
import forge.item.generation.UnOpenedProduct;
import forge.model.FModel;
import forge.util.storage.IStorage;

/** 
 * This is a helper class for unlocking new sets during a format-limited
 * quest.
 *
 */
public class QuestUtilUnlockSets {
    private static int UNLOCK_COST = 4000;

    /**
     * Consider unlocking a new expansion in limited quest format.
     * @param qData the QuestController for the current quest
     * @param freeUnlock this unlock is free (e.g., a challenge reward), NOT IMPLEMENTED YET
     * @param presetChoices List<CardEdition> a pregenerated list of options, NOT IMPLEMENTED YET
     * @return CardEdition, the unlocked edition if any.
     */
    public static ImmutablePair<CardEdition, Integer> chooseSetToUnlock(final QuestController qData, final boolean freeUnlock,
            List<CardEdition> presetChoices) {

        if (qData.getFormat() == null || !qData.getFormat().canUnlockSets()) {
            return null;
        }

        final ReadPriceList prices = new ReadPriceList();
        final Map<String, Integer> mapPrices = prices.getPriceList();
        final List<ImmutablePair<CardEdition, Integer>> setPrices = new ArrayList<>();

        Double multiplier = 1d;
        int j = 0;
        for (CardEdition ed : getUnlockableEditions(qData)) {
            j++;
            if (j > 5) {
                multiplier = multiplier * Double.parseDouble(FModel.getQuestPreferences().getPref(QPref.UNLOCK_DISTANCE_MULTIPLIER));
                // prevent overflow
                if (multiplier >= 500) {
                    multiplier = 500d;
                }
            }
            int price = UNLOCK_COST;
            if (mapPrices.containsKey(TextUtil.concatNoSpace(ed.getName(), " Booster Pack"))) {
                price = Math.max(Double.valueOf(30 * Math.pow(Math.sqrt(mapPrices.get(TextUtil.concatNoSpace(ed.getName(),
                     " Booster Pack"))), 1.70)).intValue(), UNLOCK_COST);
            }
            price = (int) ((double) price * multiplier);
            setPrices.add(ImmutablePair.of(ed, price));
        }

        final String setPrompt = "You have " + qData.getAssets().getCredits() + " credits. Unlock:";
        List<String> options = new ArrayList<>();
        for (ImmutablePair<CardEdition, Integer> ee : setPrices) {
            options.add(TextUtil.concatNoSpace(ee.left.getName()," [PRICE: ", String.valueOf(ee.right), " credits]"));
        }

        int index = options.indexOf(SGuiChoose.oneOrNone(setPrompt, options));
        if (index < 0 || index >= options.size()) {
            return null;
        }

        ImmutablePair<CardEdition, Integer> toBuy = setPrices.get(index);

        int price = toBuy.right;
        CardEdition choosenEdition = toBuy.left;

        if (qData.getAssets().getCredits() < price) {
            SOptionPane.showMessageDialog(
                    "Unfortunately, you cannot afford that set yet.\n"
                    + "To unlock " + choosenEdition.getName() + ", you need " + price + " credits.\n"
                    + "You have only " + qData.getAssets().getCredits() + " credits.",
                    "Failed to unlock " + choosenEdition.getName(),
                    null);
            return null;
        }

        if (!SOptionPane.showConfirmDialog(
                "Unlocking " + choosenEdition.getName() + " will cost you " + price + " credits.\n"
                + "You have " + qData.getAssets().getCredits() + " credits.\n\n"
                + "Are you sure you want to unlock " + choosenEdition.getName() + "?",
                "Confirm Unlocking " + choosenEdition.getName())) {
            return null;
        }
        return toBuy;
    }

    /**
     * Helper function for unlockSet().
     * 
     * @return unmodifiable list, assorted sets that are not currently in the format.
     */
    private static final List<CardEdition> emptyEditions = ImmutableList.of();
    private static final EnumSet<CardEdition.Type> unlockableSetTypes =
        EnumSet.of(CardEdition.Type.CORE, CardEdition.Type.EXPANSION, CardEdition.Type.REPRINT, CardEdition.Type.STARTER);

    private static List<CardEdition> getUnlockableEditions(final QuestController qData) {
        if (qData.getFormat() == null || !qData.getFormat().canUnlockSets()) {
            return emptyEditions;
        }

        if (qData.getUnlocksTokens() < 1) { // Should never happen if we made it this far but better safe than sorry...
            throw new RuntimeException("BUG? Could not find unlockable sets even though we should.");
        }
        List<CardEdition> options = new ArrayList<>();

        // Sort current sets by date
        List<CardEdition> allowedSets = Lists.newArrayList(Iterables.transform(qData.getFormat().getAllowedSetCodes(), FModel.getMagicDb().getEditions().FN_EDITION_BY_CODE));
        Collections.sort(allowedSets);
        
        // Sort unlockable sets by date
        List<CardEdition> excludedSets = Lists.newArrayList(Iterables.transform(qData.getFormat().getLockedSets(), FModel.getMagicDb().getEditions().FN_EDITION_BY_CODE));
        Collections.sort(excludedSets);
        
        // get a number of sets between an excluded and any included set
        List<ImmutablePair<CardEdition, Long>> excludedWithDistances = new ArrayList<>();
        for (CardEdition ex : excludedSets) {
            if (!unlockableSetTypes.contains(ex.getType())) // don't add non-traditional sets
                continue;
            long distance = Long.MAX_VALUE;
            for (CardEdition in : allowedSets) {
                long d = (Math.abs(ex.getDate().getTime() - in.getDate().getTime()));
                if (d < distance) {
                    distance = d;
                }
            }
            excludedWithDistances.add(ImmutablePair.of(ex, distance));
        }

        // sort by distance, then by code desc
        Collections.sort(excludedWithDistances, new Comparator<ImmutablePair<CardEdition, Long>>() {
            @Override
            public int compare(ImmutablePair<CardEdition, Long> o1, ImmutablePair<CardEdition, Long> o2) {
                long delta = o2.right - o1.right;
                return delta < 0 ? -1 : delta == 0 ? 0 : 1;
            }
        });

        for (ImmutablePair<CardEdition, Long> set : excludedWithDistances) {
            options.add(set.left);
            // System.out.println("Padded with: " + fillers.get(i).getName());
        }
        Collections.reverse(options);

        if (FModel.getQuestPreferences().getPrefInt(QPref.UNLIMITED_UNLOCKING) == 0) {
            return options.subList(0, Math.min(options.size(), Math.min(8, 2 + ((qData.getAchievements().getWin()) / 50))));
        } else {
            return options.subList(0, options.size());
        }
    }

    /**
     * 
     * Unlock a set and get some free cards, if a tournament pack or boosters are available.
     * @param qData the quest controller
     * @param unlockedSet the edition to unlock
     */
    public static void doUnlock(final QuestController qData, final CardEdition unlockedSet) {
        IStorage<SealedProduct.Template> starters = FModel.getMagicDb().getTournamentPacks();
        IStorage<SealedProduct.Template> boosters = FModel.getMagicDb().getBoosters();
        qData.getFormat().unlockSet(unlockedSet.getCode());

        String additionalSet = unlockedSet.getAdditionalUnlockSet();
        if (!additionalSet.isEmpty()) {
            qData.getFormat().unlockSet(additionalSet);
        }

        List<PaperCard> cardsWon = new ArrayList<>();

        if (starters.contains(unlockedSet.getCode())) {
            UnOpenedProduct starter = new UnOpenedProduct(starters.get(unlockedSet.getCode()));
            cardsWon.addAll(starter.get());
        }
        else if (boosters.contains(unlockedSet.getCode())) {
            UnOpenedProduct booster = new UnOpenedProduct(boosters.get(unlockedSet.getCode()));
            cardsWon.addAll(booster.get());
            cardsWon.addAll(booster.get());
            cardsWon.addAll(booster.get());
        }

        if (!cardsWon.isEmpty()) {
            qData.getCards().addAllCards(cardsWon);
            GuiBase.getInterface().showCardList(unlockedSet.getName(), "You get the following bonus cards:", cardsWon);
        }

        qData.save();
    }
}
