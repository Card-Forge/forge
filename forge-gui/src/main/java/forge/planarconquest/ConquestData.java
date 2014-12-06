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
package forge.planarconquest;

import forge.FThreads;
import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.planarconquest.ConquestPlane.Region;
import forge.planarconquest.ConquestPlaneData.RegionData;
import forge.planarconquest.ConquestPreferences.CQPref;
import forge.properties.ForgeConstants;
import forge.util.Aggregates;
import forge.util.Lang;
import forge.util.gui.SGuiChoose;
import forge.util.gui.SOptionPane;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;

import com.google.common.base.Predicate;

public final class ConquestData {
    /** Holds the latest version of the Conquest Data. */
    public static final int CURRENT_VERSION_NUMBER = 0;

    // This field places the version number into QD instance,
    // but only when the object is created through the constructor
    // DO NOT RENAME THIS FIELD
    private int versionNumber = ConquestData.CURRENT_VERSION_NUMBER;

    private String name;
    private int wins, losses;
    private int winStreakBest = 0;
    private int winStreakCurrent = 0;
    private int day = 1;
    private int difficulty;
    private ConquestPlane startingPlane, currentPlane;
    private int currentRegionIndex;
    private EnumMap<ConquestPlane, ConquestPlaneData> planeDataMap = new EnumMap<ConquestPlane, ConquestPlaneData>(ConquestPlane.class);

    private final CardPool collection = new CardPool();
    private final HashMap<String, Deck> decks = new HashMap<String, Deck>();

    public ConquestData() { //needed for XML serialization
    }

    public ConquestData(String name0, int difficulty0, ConquestPlane startingPlane0, PaperCard startingCommander0) {
        name = name0;
        difficulty = difficulty0;
        startingPlane = startingPlane0;
        currentPlane = startingPlane0;
        addCommander(startingCommander0);
    }

    private void addCommander(PaperCard card) {
        ConquestCommander commander = new ConquestCommander(card, currentPlane.getCardPool(), false);
        getCurrentPlaneData().getCommanders().add(commander);
        decks.put(commander.getDeck().getName(), commander.getDeck());
        collection.addAll(commander.getDeck().getMain());
        collection.add(card);
    }

    public String getName() {
        return name;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public int getDay() {
        return day;
    }

    public void endDay(final IVConquestBase base) {
        FThreads.invokeInBackgroundThread(new Runnable() {
            @Override
            public void run() {
                //prompt user if any commander hasn't taken an action
                final List<ConquestCommander> commanders = getCurrentPlaneData().getCommanders();
                for (ConquestCommander commander : commanders) {
                    if (commander.getCurrentDayAction() == null) {
                        if (!SOptionPane.showConfirmDialog(commander.getName() + " has not taken an action today. End day anyway?", "Action Not Taken", "End Day", "Cancel")) {
                            return;
                        }
                    }
                }
                //perform all commander actions
                for (ConquestCommander commander : commanders) {
                    switch (commander.getCurrentDayAction()) {
                    case Attack1:
                        playGame(commander, 0, false);
                        break;
                    case Attack2:
                        playGame(commander, 1, false);
                        break;
                    case Attack3:
                        playGame(commander, 2, false);
                        break;
                    case Defend:
                        playGame(commander, Aggregates.randomInt(0, 2), true); //defend against random opponent
                        break;
                    case Recruit:
                        if (!recruit(commander)) { return; }
                        break;
                    case Study:
                        if (!study(commander)) { return; }
                        break;
                    case Undeploy:
                        getCurrentPlaneData().getRegionData(commander.getDeployedRegion()).setDeployedCommander(null);
                        break;
                    default: //remaining actions don't need to do anything more
                        break;
                    }
                }
                //increment day and reset actions, then update UI for new day
                FThreads.invokeInEdtLater(new Runnable() {
                    @Override
                    public void run() {
                        day++;
                        for (ConquestCommander commander : commanders) {
                            commander.setCurrentDayAction(null);
                        }
                        base.updateCurrentDay();
                    }
                });
            }
        });
    }

    private void playGame(ConquestCommander commander, int opponentIndex, boolean isHumanDefending) {
        RegionData regionData = getCurrentPlaneData().getRegionData(commander.getDeployedRegion());
        ConquestCommander opponent = regionData.getOpponent(opponentIndex);
        //TODO
    }

    private boolean recruit(ConquestCommander commander) {
        boolean bonusCard = Aggregates.randomInt(1, 100) <= FModel.getConquestPreferences().getPrefInt(CQPref.RECRUIT_BONUS_CARD_ODDS);
        return rewardNewCards(commander.getDeployedRegion().getCardPool().getAllCards(),
                commander.getName() + " recruited", "new creature",
                CardRulesPredicates.Presets.IS_CREATURE, bonusCard ? 2 : 1);
    }

    private boolean study(ConquestCommander commander) {
        boolean bonusCard = Aggregates.randomInt(1, 100) <= FModel.getConquestPreferences().getPrefInt(CQPref.STUDY_BONUS_CARD_ODDS);
        return rewardNewCards(commander.getDeployedRegion().getCardPool().getAllCards(),
                commander.getName() + " unlocked", "new spell",
                CardRulesPredicates.Presets.IS_NON_CREATURE_SPELL, bonusCard ? 2 : 1);
    }

    private boolean rewardNewCards(Iterable<PaperCard> cardPool, String messagePrefix, String messageSuffix, Predicate<CardRules> pred, int count) {
        List<PaperCard> commons = new ArrayList<PaperCard>();
        List<PaperCard> uncommons = new ArrayList<PaperCard>();
        List<PaperCard> rares = new ArrayList<PaperCard>();
        List<PaperCard> mythics = new ArrayList<PaperCard>();
        int newCardCount = 0;
        for (PaperCard c : cardPool) {
            if (pred.apply(c.getRules()) && !collection.contains(c)) {
                switch (c.getRarity()) {
                case Common:
                    commons.add(c);
                    break;
                case Uncommon:
                    uncommons.add(c);
                    break;
                case Rare:
                    rares.add(c);
                    break;
                case MythicRare:
                    mythics.add(c);
                    break;
                default:
                    break;
                }
            }
        }

        newCardCount = commons.size() + uncommons.size() + rares.size() + mythics.size();
        if (newCardCount == 0) {
            return false;
        }

        ConquestPreferences prefs = FModel.getConquestPreferences();
        int rareThreshold = prefs.getPrefInt(CQPref.BOOSTER_RARES);
        int uncommonThreshold = rareThreshold + prefs.getPrefInt(CQPref.BOOSTER_UNCOMMONS);
        int cardsPerPack = uncommonThreshold + prefs.getPrefInt(CQPref.BOOSTER_COMMONS);

        List<PaperCard> rewardPool;
        List<PaperCard> rewards = new ArrayList<PaperCard>();
        for (int i = 0; i < count; i++) {
            //determine which rarity card to get based on pack ratios
            int value = Aggregates.randomInt(1, cardsPerPack);
            if (value <= rareThreshold) {
                if (mythics.size() > 0 && Aggregates.randomInt(1, 8) == 1) {
                    rewardPool = mythics;
                }
                else {
                    rewardPool = rares;
                }
            }
            else if (value <= uncommonThreshold) {
                rewardPool = uncommons;
            }
            else {
                rewardPool = commons;
            }
            if (rewardPool.isEmpty()) { continue; } //if no cards in selected pool, determine random pool again

            int index = Aggregates.randomInt(0, rewardPool.size() - 1);
            rewards.add(rewardPool.remove(index));

            if (--newCardCount == 0) {
                break; //break out if no new cards remain
            }
        }

        collection.add(rewards);

        String message = messagePrefix + " " + Lang.nounWithAmount(rewards.size(), messageSuffix);
        SGuiChoose.reveal(message, rewards);
        return true;
    }

    public ConquestPlane getStartingPlane() {
        return startingPlane;
    }

    public ConquestPlane getCurrentPlane() {
        return currentPlane;
    }

    public ConquestPlaneData getCurrentPlaneData() {
        ConquestPlaneData planeData = planeDataMap.get(currentPlane);
        if (planeData == null) {
            planeData = new ConquestPlaneData();
            planeDataMap.put(currentPlane, planeData);
        }
        return planeData;
    }

    public Region getCurrentRegion() {
        return currentPlane.getRegions().get(currentRegionIndex);
    }

    public void incrementRegion(int dir) {
        if (dir > 0) {
            currentRegionIndex++;
            if (currentRegionIndex >= currentPlane.getRegions().size()) {
                currentRegionIndex = 0;
            }
        }
        else {
            currentRegionIndex--;
            if (currentRegionIndex < 0) {
                currentRegionIndex = currentPlane.getRegions().size() - 1;
            }
        }
    }

    public CardPool getCollection() {
        return collection;
    }

    public ConquestDeckMap getDeckStorage() {
        return new ConquestDeckMap(decks);
    }

    public void addWin() {
        wins++;
        winStreakCurrent++;
        getCurrentPlaneData().addWin();

        if (winStreakCurrent > winStreakBest) {
            winStreakBest = winStreakCurrent;
        }
    }

    public void addLoss() {
        losses++;
        winStreakCurrent = 0;
        getCurrentPlaneData().addLoss();
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public int getWinStreakBest() {
        return winStreakBest;
    }

    public int getWinStreakCurrent() {
        return winStreakCurrent;
    }

    // SERIALIZATION - related things
    // This must be called by XML-serializer via reflection
    public Object readResolve() {
        return this;
    }

    public void saveData() {
        ConquestDataIO.saveData(this);
    }

    public int getVersionNumber() {
        return versionNumber;
    }
    public void setVersionNumber(final int versionNumber0) {
        versionNumber = versionNumber0;
    }

    public void rename(final String newName) {
        File newpath = new File(ForgeConstants.CONQUEST_SAVE_DIR, newName + ".dat");
        File oldpath = new File(ForgeConstants.CONQUEST_SAVE_DIR, name + ".dat");
        oldpath.renameTo(newpath);

        name = newName;
        saveData();
    }
}
