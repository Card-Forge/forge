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
package forge.quest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JOptionPane;

import forge.Singletons;
import forge.card.BoosterData;
import forge.card.CardEdition;
import forge.card.UnOpenedProduct;
import forge.gui.CardListViewer;
import forge.gui.GuiChoose;
import forge.item.CardPrinted;
import forge.quest.io.ReadPriceList;

/** 
 * This is a helper class for unlocking new sets during a format-limited
 * quest.
 *
 */
public class QuestUtilUnlockSets {

    /**
     * Consider unlocking a new expansion in limited quest format.
     * @param qData the QuestController for the current quest
     * @param freeUnlock this unlock is free (e.g., a challenge reward), NOT IMPLEMENTED YET
     * @param presetChoices List<CardEdition> a pregenerated list of options, NOT IMPLEMENTED YET
     * @return CardEdition, the unlocked edition if any.
     */
    public static CardEdition unlockSet(final QuestController qData, final boolean freeUnlock,
            List<CardEdition> presetChoices) {

        if (qData.getFormat() == null || qData.getFormat().getExcludedSetCodes().isEmpty()) {
            return null;
        }
        List<CardEdition> choices = unlockableSets(qData);

        if (choices == null || choices.size() < 1) {
            return null;
        }

        final ReadPriceList prices = new ReadPriceList();
        final Map<String, Integer> mapPrices = prices.getPriceList();

        List<Long> unlockPrices = new ArrayList<Long>();
        for (int i = 0; i < choices.size(); i++) {
            if (mapPrices.containsKey(choices.get(i).getName() + " Booster Pack")) {
                long newPrice = (long) 50 * mapPrices.get(choices.get(i).getName() + " Booster Pack");
                if (newPrice < 10000) { newPrice = 10000; }
                unlockPrices.add(newPrice);
            }
            else {
                unlockPrices.add((long) 10000);
            }
        }

        final String setPrompt = "You have " + qData.getAssets().getCredits() + " credits. Unlock:";
        List<String> options = new ArrayList<String>();
        for (int i = 0; i < choices.size(); i++) {
            options.add(choices.get(i).getName() + " [PRICE: " + unlockPrices.get(i) + " credits]");
        }
        final String choice = GuiChoose.oneOrNone(setPrompt, options);
        CardEdition chooseEd = null;
        long price = 0;

        if (choice == null) {
            return null;
        }

        /* Examine choice */
        for (int i = 0; i < options.size(); i++) {
            if (choice.equals(options.get(i))) {
                chooseEd = choices.get(i);
                price = unlockPrices.get(i);
                break;
            }
        }

        if (qData.getAssets().getCredits() < price) {
            JOptionPane.showMessageDialog(null, "Unfortunately, you cannot afford that set yet.\n"
                    + "To unlock " + chooseEd.getName() + ", you need " + price + " credits.\n"
                    + "You have only " + qData.getAssets().getCredits() + " credits.",
                    "Failed to unlock " + chooseEd.getName(),
                    JOptionPane.PLAIN_MESSAGE);
            return null;
        }

        final int unlockConfirm = JOptionPane.showConfirmDialog(null,
                "Unlocking " + chooseEd.getName() + " will cost you " + price + " credits.\n"
                + "You have " + qData.getAssets().getCredits() + " credits.\n\n"
                + "Are you sure you want to unlock " + chooseEd.getName() + "?",
                "Confirm Unlocking " + chooseEd.getName(), JOptionPane.YES_NO_OPTION);
        if (unlockConfirm == JOptionPane.NO_OPTION) {
            return null;
        }

        qData.getAssets().subtractCredits(price);
        JOptionPane.showMessageDialog(null, "You have successfully unlocked " + chooseEd.getName() + "!",
                chooseEd.getName() + " unlocked!",
                JOptionPane.PLAIN_MESSAGE);
        return chooseEd;
    }

    /**
     * Helper function for unlockSet().
     * 
     * @return unmodifiable list, assorted sets that are not currently in the format.
     */
    private static List<CardEdition> unlockableSets(final QuestController qData) {
         if (qData.getFormat() == null || qData.getFormat().getExcludedSetCodes().isEmpty()) {
            return null;
        }

        final int nrChoices = qData.getFormatNumberUnlockable();
        if (nrChoices < 1) { // Should never happen if we made it this far but better safe than sorry...
            throw new RuntimeException("BUG? Could not find unlockable sets even though we should.");
        }
        List<CardEdition> options = new ArrayList<CardEdition>();

         // Sort current sets by index
         TreeMap<Integer, CardEdition> sortedFormat = new TreeMap<Integer, CardEdition>();
         for (String edCode : qData.getFormat().getAllowedSetCodes()) {
             sortedFormat.put(new Integer(Singletons.getModel().getEditions().get(edCode).getIndex()), Singletons.getModel().getEditions().get(edCode));
         }
         List<CardEdition> currentSets = new ArrayList<CardEdition>(sortedFormat.values());

         // Sort unlockable sets by index
         TreeMap<Integer, CardEdition> sortedExcluded = new TreeMap<Integer, CardEdition>();
         for (String edCode : qData.getFormat().getExcludedSetCodes()) {
             sortedExcluded.put(new Integer(Singletons.getModel().getEditions().get(edCode).getIndex()), Singletons.getModel().getEditions().get(edCode));
         }
         List<CardEdition> excludedSets = new ArrayList<CardEdition>(sortedExcluded.values());

         // Collect 'previous' and 'next' editions
         CardEdition first = currentSets.get(0);
         CardEdition last = currentSets.get(currentSets.size() - 1);
         List<CardEdition> fillers = new ArrayList<CardEdition>();

         // Add nearby sets first
         for (CardEdition ce : excludedSets) {
             if (first.getIndex() == ce.getIndex() + 1 || last.getIndex() + 1 == ce.getIndex())
             {
                 options.add(ce);
                 // System.out.println("Added adjacent set: " + ce.getName());
             }
         }

         // Fill in the in-between sets
         int j = 0;
         // Find the first excluded set between current sets first and current sets last
         while (j < excludedSets.size() && excludedSets.get(j).getIndex() < currentSets.get(0).getIndex()) {
             j++;
         }
         // Consider all sets until current sets last
         while (j < excludedSets.size() && excludedSets.get(j).getIndex() < currentSets.get(currentSets.size() - 1).getIndex()) {
             if (!options.contains(excludedSets.get(j)) && !fillers.contains(excludedSets.get(j))) {
                 // System.out.println("Added in-between set " + excludedSets.get(j).getCode());
                 fillers.add(excludedSets.get(j));
             }
             j++;
         }
         // Add more nearby sets
         for (CardEdition ce : excludedSets) {
             if (first.getIndex() == ce.getIndex() + 2 || last.getIndex() + 2 == ce.getIndex())
             {
                 if (!fillers.contains(ce) && !options.contains(ce)) {
                 fillers.add(ce);
                 // System.out.println("Added adjacent filler set: " + ce.getName());
                 }
             }
         }

         // Look for nearby core sets or block starting sets...
         for (BoosterData bd : Singletons.getModel().getTournamentPacks()) {
             if (qData.getFormat().getExcludedSetCodes().contains(bd.getEdition())
                     && !(fillers.contains(Singletons.getModel().getEditions().get(bd.getEdition())))
                     && !(options.contains(Singletons.getModel().getEditions().get(bd.getEdition())))) {
                 // Set is not yet on any of the lists, see if it is 'close' to any of the sets we currently have
                 CardEdition curEd = Singletons.getModel().getEditions().get(bd.getEdition());
                 int edIdx = curEd.getIndex();
                 for (String cmpCode : qData.getFormat().getAllowedSetCodes()) {
                     int cmpIdx = Singletons.getModel().getEditions().get(cmpCode).getIndex();
                     // Note that we need to check for fillers.contains() again inside this 'for' loop!
                     if (!fillers.contains(curEd) && (cmpIdx == edIdx + 1 || edIdx == cmpIdx + 1)) {
                         fillers.add(curEd);
                         // System.out.println("Added nearby starter/core set " + curEd.getName());
                     }
                     else if (!fillers.contains(curEd) && (cmpIdx == edIdx + 2 || edIdx == cmpIdx + 2)) {
                         fillers.add(curEd);
                         //System.out.println("Added nearby2 starter/core set " + curEd.getName());
                     }
                 }
             }
         }

         // Add padding if necessary
         if (fillers.size() + options.size() < nrChoices && excludedSets.size() > fillers.size() + options.size()) {
                 // Pad in order.
                 for (CardEdition ce : excludedSets) {
                     if (!fillers.contains(ce) && !options.contains(ce)) {
                         fillers.add(ce);
                         if (fillers.size() + options.size() >= nrChoices) {
                             break;
                         }
                     }
                 }
         }

         for (int i = 0; (options.size() < nrChoices) && i < fillers.size(); i++) {
             options.add(fillers.get(i));
             // System.out.println("Padded with: " + fillers.get(i).getName());
         }

         return Collections.unmodifiableList(options);
    }

    /**
     * 
     * Unlock a set and get some free cards, if a tournament pack or boosters are available.
     * @param qData the quest controller
     * @param unlockedSet the edition to unlock
     */
    public static void doUnlock(QuestController qData, final CardEdition unlockedSet) {

        qData.getFormat().unlockSet(unlockedSet.getCode());

        List<CardPrinted> displayCards = new ArrayList<CardPrinted>();

        if (Singletons.getModel().getTournamentPacks().contains(unlockedSet.getCode())) {
            final List<CardPrinted> cardsWon = (new UnOpenedProduct(Singletons.getModel().getTournamentPacks().get(unlockedSet.getCode()))).open();

            qData.getCards().addAllCards(cardsWon);
            displayCards.addAll(cardsWon);
        }
        else if (Singletons.getModel().getBoosters().contains(unlockedSet.getCode())) {
            for (int i = 0; i < 3; i++) {
                final List<CardPrinted> cardsWon = (new UnOpenedProduct(Singletons.getModel().getBoosters().get(unlockedSet.getCode()))).open();

                qData.getCards().addAllCards(cardsWon);
                displayCards.addAll(cardsWon);
            }
        }

        final CardListViewer cardView = new CardListViewer(unlockedSet.getName(),
                "You get the following bonus cards:", displayCards);
        cardView.show();

        qData.save();

    }
}
