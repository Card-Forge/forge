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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;

import forge.AllZone;
import forge.Singletons;
import forge.deck.Deck;
import forge.deck.io.DeckSerializer;
import forge.quest.data.QuestAchievements;
import forge.quest.data.QuestPreferences;
import forge.quest.data.QuestPreferences.QPref;
import forge.util.FileSection;
import forge.util.FileUtil;

/**
 * QuestEventManager.
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestEventManager {

    private final Map<QuestEventDifficulty, List<QuestEventDuel>> sortedDuels = new EnumMap<QuestEventDifficulty, List<QuestEventDuel>>(QuestEventDifficulty.class);
    private final List<QuestEventDuel> allDuels = new ArrayList<QuestEventDuel>();
    private final List<QuestEventChallenge> allChallenges = new ArrayList<QuestEventChallenge>();

    /** Instantiate all events and difficulty lists.
     * @param dir &emsp; File object */
    public QuestEventManager(final File dir) {
        QuestEvent tempEvent;

        final File[] allFiles = dir.listFiles(DeckSerializer.DCK_FILE_FILTER);

        for (final File f : allFiles) {
            final Map<String, List<String>> contents = FileSection.parseSections(FileUtil.readFile(f));

            if (contents.containsKey("quest")) {
                tempEvent = readChallenge(contents.get("quest"));
                allChallenges.add((QuestEventChallenge) tempEvent);
            }
            else {
                tempEvent = readDuel(contents.get("metadata"));
                allDuels.add((QuestEventDuel) tempEvent);
            }

            // Assemble metadata (may not be necessary later) and deck object.
            readMetadata(contents.get("metadata"), tempEvent);
            tempEvent.setEventDeck(Deck.fromSections(contents));
        } // End for(allFiles)

        assembleDuelDifficultyLists();
    } // End assembleAllEvents()

    /**
     * Retrieve single event, using its name.
     * 
     * @param s0
     *            &emsp; {@link java.lang.String}
     * @return {@link forge.quest.data.QuestEvent}
     */
    public QuestEvent getEvent(final String s0) {
        for (final QuestEvent q : allDuels) {
            if (q.getName().equals(s0)) {
                return q;
            }
        }

        for (final QuestEventChallenge q : allChallenges) {
            if (q.getName().equals(s0)) {
                return q;
            }
        }

        return null;
    }

    /** @return List<QuestEventDuel> */
    public List<QuestEventDuel> getAllDuels() {
        return allDuels;
    }

    /** @return List<QuestEventChallenge> */
    public List<QuestEventChallenge> getAllChallenges() {
        return allChallenges;
    }

    /** Generates an array of new duel opponents based on current win conditions.
     * 
     * @return an array of {@link java.lang.String} objects.
     */
    public final List<QuestEventDuel> generateDuels() {
        final QuestPreferences qpref = Singletons.getModel().getQuestPreferences();
        if (AllZone.getQuest().getAchievements() == null) {
            return null;
        }

        final QuestController qCtrl = AllZone.getQuest();
        final int cntWins = qCtrl.getAchievements().getWin();

        final int index = qCtrl.getAchievements().getDifficulty();
        final List<QuestEventDuel> duelOpponents = new ArrayList<QuestEventDuel>();

        if (cntWins < qpref.getPreferenceInt(QPref.WINS_MEDIUMAI, index)) {
            duelOpponents.add(sortedDuels.get(QuestEventDifficulty.EASY).get(0));
            duelOpponents.add(sortedDuels.get(QuestEventDifficulty.EASY).get(1));
            duelOpponents.add(sortedDuels.get(QuestEventDifficulty.EASY).get(2));
        } else if (cntWins == qpref.getPreferenceInt(QPref.WINS_MEDIUMAI, index)) {
            duelOpponents.add(sortedDuels.get(QuestEventDifficulty.EASY).get(0));
            duelOpponents.add(sortedDuels.get(QuestEventDifficulty.MEDIUM).get(0));
            duelOpponents.add(sortedDuels.get(QuestEventDifficulty.MEDIUM).get(1));
        } else if (cntWins < qpref.getPreferenceInt(QPref.WINS_HARDAI, index)) {
            duelOpponents.add(sortedDuels.get(QuestEventDifficulty.MEDIUM).get(0));
            duelOpponents.add(sortedDuels.get(QuestEventDifficulty.MEDIUM).get(1));
            duelOpponents.add(sortedDuels.get(QuestEventDifficulty.MEDIUM).get(2));
        }

        else if (cntWins == qpref.getPreferenceInt(QPref.WINS_HARDAI, index)) {
            duelOpponents.add(sortedDuels.get(QuestEventDifficulty.MEDIUM).get(0));
            duelOpponents.add(sortedDuels.get(QuestEventDifficulty.HARD).get(0));
            duelOpponents.add(sortedDuels.get(QuestEventDifficulty.HARD).get(1));
        }

        else if (cntWins < qpref.getPreferenceInt(QPref.WINS_EXPERTAI, index)) {
            duelOpponents.add(sortedDuels.get(QuestEventDifficulty.HARD).get(0));
            duelOpponents.add(sortedDuels.get(QuestEventDifficulty.HARD).get(1));
            duelOpponents.add(sortedDuels.get(QuestEventDifficulty.HARD).get(2));
        } else {
            duelOpponents.add(sortedDuels.get(QuestEventDifficulty.HARD).get(0));
            duelOpponents.add(sortedDuels.get(QuestEventDifficulty.HARD).get(1));
            duelOpponents.add(sortedDuels.get(QuestEventDifficulty.EXPERT).get(0));
        }

        return duelOpponents;
    }

    /** Generates an array of new challenge opponents based on current win conditions.
     * 
     * @param qCtrl &emsp; QuestController
     * @return a {@link java.util.List} object.
     */
    public final List<QuestEventChallenge> generateChallenges(final QuestController qCtrl) {
        final QuestAchievements achievements = qCtrl.getAchievements();
        final List<QuestEventChallenge> challengeOpponents = new ArrayList<QuestEventChallenge>();
        final List<Integer> unlockedChallengeIds = new ArrayList<Integer>();
        final List<Integer> availableChallengeIds = achievements.getCurrentChallenges();

        int maxChallenges = achievements.getWin() / 10;
        if (maxChallenges > 5) {
            maxChallenges = 5;
        }

        // Generate IDs as needed.
        if (achievements.getCurrentChallenges().size() < maxChallenges) {
            for (final QuestEventChallenge qc : allChallenges) {
                if ((qc.getWinsReqd() <= achievements.getWin())
                        && !achievements.getLockedChallenges().contains(qc.getId())
                        && !availableChallengeIds.contains(qc.getId())) {
                    unlockedChallengeIds.add(qc.getId());
                }
            }

            Collections.shuffle(unlockedChallengeIds);

            maxChallenges = Math.min(maxChallenges, unlockedChallengeIds.size());

            for (int i = availableChallengeIds.size(); i < maxChallenges; i++) {
                availableChallengeIds.add(unlockedChallengeIds.get(i));
            }

            achievements.setCurrentChallenges(availableChallengeIds);
            qCtrl.save();
        }

        // Finally, pull challenge events from available IDs and return.
        for (final int i : achievements.getCurrentChallenges()) {
            challengeOpponents.add(getChallengeEventByNumber(i));
        }

        return challengeOpponents;
    }

    /**
     * <p>
     * assembleDuelUniqueData.
     * </p>
     * Handler for any unique data contained in duel files.
     * 
     * @param contents
     * @param qd
     */
    private QuestEventDuel readDuel(final List<String> contents) {
        final QuestEventDuel qd = new QuestEventDuel();
        int eqpos;
        String key, value;

        for (final String s : contents) {
            if (s.equals("")) {
                continue;
            }

            eqpos = s.indexOf('=');
            if (eqpos < 0) {
                continue;
            }
            key = s.substring(0, eqpos);
            value = s.substring(eqpos + 1);

            if (key.equalsIgnoreCase("Name")) {
                qd.setName(value);
            }
        }
        return qd;
    }

    /**
     * <p>
     * assembleChallengeUniquedata.
     * </p>
     * Handler for any unique data contained in a challenge file.
     * 
     * @param contents
     * @param qc
     */
    private QuestEventChallenge readChallenge(final List<String> contents) {
        int eqpos;
        String key, value;

        final QuestEventChallenge qc = new QuestEventChallenge();
        // Unique properties
        for (final String s : contents) {
            if (StringUtils.isBlank(s)) {
                continue;
            }

            eqpos = s.indexOf('=');
            key = s.substring(0, eqpos);
            value = s.substring(eqpos + 1).trim();

            if (key.equalsIgnoreCase("ID")) {
                qc.setId(Integer.parseInt(value));
            } else if (key.equalsIgnoreCase("Repeat")) {
                qc.setRepeatable(Boolean.parseBoolean(value));
            } else if (key.equalsIgnoreCase("AILife")) {
                qc.setAiLife(Integer.parseInt(value));
            } else if (key.equalsIgnoreCase("Wins")) {
                qc.setWinsReqd(Integer.parseInt(value));
            } else if (key.equalsIgnoreCase("Credit Reward")) {
                qc.setCreditsReward(Integer.parseInt(value));
            } else if (key.equalsIgnoreCase("Card Reward")) {
                qc.setCardReward(value);
            }
            // Human extra card list assembled here.
            else if (key.equalsIgnoreCase("HumanExtras") && !value.equals("")) {
                final String[] names = value.split("\\|");
                final List<String> templist = new ArrayList<String>();

                for (final String n : names) {
                    templist.add(n);
                }

                qc.setHumanExtraCards(templist);
            }
            // AI extra card list assembled here.
            else if (key.equalsIgnoreCase("AIExtras") && !value.equals("")) {
                final String[] names = value.split("\\|");
                final List<String> templist = new ArrayList<String>();

                for (final String n : names) {
                    templist.add(n);
                }

                qc.setAiExtraCards(templist);
            }
            // Card reward list assembled here.
            else if (key.equalsIgnoreCase("Card Reward")) {
                qc.setCardReward(value);
            }
        }
        return qc;
    }

    /**
     * <p>
     * assembleEventMetadata.
     * </p>
     * Handler for metadata contained in event files.
     * 
     * @param contents
     * @param qe
     */
    private void readMetadata(final List<String> contents, final QuestEvent qe) {
        int eqpos;
        String key, value;

        for (String s : contents) {
            s = s.trim();
            eqpos = s.indexOf('=');

            if (eqpos == -1) {
                continue;
            }

            key = s.substring(0, eqpos);
            value = s.substring(eqpos + 1);

            if (key.equalsIgnoreCase("Name")) {
                qe.setName(value);
            } else if (key.equalsIgnoreCase("Title")) {
                qe.setTitle(value);
            } else if (key.equalsIgnoreCase("Difficulty")) {
                qe.setDifficulty(value);
            } else if (key.equalsIgnoreCase("Description")) {
                qe.setDescription(value);
            } else if (key.equalsIgnoreCase("Icon")) {
                qe.setIconFilename(value);
            }
        }
    }

    /**
     * <p>
     * assembleDuelDifficultyLists.
     * </p>
     * Assemble duel deck difficulty lists
     */
    private void assembleDuelDifficultyLists() {
        sortedDuels.clear();
        sortedDuels.put(QuestEventDifficulty.EASY, new ArrayList<QuestEventDuel>());
        sortedDuels.put(QuestEventDifficulty.MEDIUM, new ArrayList<QuestEventDuel>());
        sortedDuels.put(QuestEventDifficulty.HARD, new ArrayList<QuestEventDuel>());
        sortedDuels.put(QuestEventDifficulty.EXPERT, new ArrayList<QuestEventDuel>());

        String s;

        for (final QuestEventDuel qd : allDuels) {
            s = qd.getDifficulty();
            if (s.equalsIgnoreCase("easy")) {
                sortedDuels.get(QuestEventDifficulty.EASY).add(qd);
            } else if (s.equalsIgnoreCase("medium")) {
                sortedDuels.get(QuestEventDifficulty.MEDIUM).add(qd);
            } else if (s.equalsIgnoreCase("hard")) {
                sortedDuels.get(QuestEventDifficulty.HARD).add(qd);
            } else if (s.equalsIgnoreCase("very hard")) {
                sortedDuels.get(QuestEventDifficulty.EXPERT).add(qd);
            }
        }
    }

    /** */
    public void randomizeOpponents() {
        final long seed = new Random().nextLong();
        final Random r = new Random(seed);
        Collections.shuffle(sortedDuels.get(QuestEventDifficulty.EASY), r);
        Collections.shuffle(sortedDuels.get(QuestEventDifficulty.MEDIUM), r);
        Collections.shuffle(sortedDuels.get(QuestEventDifficulty.HARD), r);
        Collections.shuffle(sortedDuels.get(QuestEventDifficulty.EXPERT), r);
    }

    /**
     * <p>
     * getChallengeOpponentByNumber.
     * </p>
     * Returns specific challenge event using its ID. This is to make sure that
     * the opponents do not change when the deck editor is launched.
     * 
     * @param n
     * @return
     */
    private QuestEventChallenge getChallengeEventByNumber(final int n) {
        for (final QuestEventChallenge qc : allChallenges) {
            if (qc.getId() == n) {
                return qc;
            }
        }
        return null;
    }
}
