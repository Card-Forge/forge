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
package forge.quest.data;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;

import forge.AllZone;
import forge.Singletons;
import forge.deck.Deck;
import forge.deck.io.DeckSerializer;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.quest.BoosterUtils;
import forge.quest.data.QuestPreferences.QPref;
import forge.util.FileSection;
import forge.util.FileUtil;

/**
 * QuestEventManager.
 * 
 * @author Forge
 * @version $Id$
 */
public enum QuestEventManager {
    /** */
    SINGLETON_INSTANCE;

    /** */
    private static final List<QuestDuel> EASY_DUELS = new ArrayList<QuestDuel>();
    /** */
    private static final List<QuestDuel> MEDIUM_DUELS = new ArrayList<QuestDuel>();
    /** */
    private static final List<QuestDuel> HARD_DUELS = new ArrayList<QuestDuel>();
    /** */
    private static final List<QuestDuel> EXPERT_DUELS = new ArrayList<QuestDuel>();
    /** */
    public static final List<QuestDuel> ALL_DUELS = new ArrayList<QuestDuel>();
    /** */
    public static final List<QuestChallenge> ALL_CHALLENGES = new ArrayList<QuestChallenge>();

    /** Instantiate all events and difficulty lists. */
    static {
        QuestEvent tempEvent;

        final File[] allFiles = ForgeProps.getFile(NewConstants.Quest.DECKS).listFiles(DeckSerializer.DCK_FILE_FILTER);

        for (final File f : allFiles) {
            final Map<String, List<String>> contents = FileSection.parseSections(FileUtil.readFile(f));

            if (contents.containsKey("quest")) {
                tempEvent = QuestEventManager.readChallenge(contents.get("quest"));
                ALL_CHALLENGES.add((QuestChallenge) tempEvent);
            }
            else {
                tempEvent = QuestEventManager.readDuel(contents.get("metadata"));
                ALL_DUELS.add((QuestDuel) tempEvent);
            }

            // Assemble metadata (may not be necessary later) and deck object.
            QuestEventManager.readMetadata(contents.get("metadata"), tempEvent);
            tempEvent.setEventDeck(Deck.fromSections(contents));
        } // End for(allFiles)

        QuestEventManager.assembleDuelDifficultyLists();
    } // End assembleAllEvents()

    /**
     * Retrieve single event, using its name.
     * 
     * @param s0
     *            &emsp; {@link java.lang.String}
     * @return {@link forge.data.QuestEvent}
     */
    public static QuestEvent getEvent(final String s0) {
        for (final QuestEvent q : ALL_DUELS) {
            if (q.getName().equals(s0)) {
                return q;
            }
        }

        for (final QuestChallenge q : ALL_CHALLENGES) {
            if (q.getName().equals(s0)) {
                return q;
            }
        }

        return null;
    }

    /** Generates an array of new duel opponents based on current win conditions.
     * 
     * @return an array of {@link java.lang.String} objects.
     */
    public static final List<QuestDuel> generateDuels() {
        final QuestPreferences qpref = Singletons.getModel().getQuestPreferences();
        if (AllZone.getQuestData() == null) {
            return null;
        }

        final int index = AllZone.getQuestData().getDifficultyIndex();
        final List<QuestDuel> duelOpponents = new ArrayList<QuestDuel>();

        if (AllZone.getQuestData().getWin() < qpref.getPreferenceInt(QPref.WINS_MEDIUMAI, index)) {
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(QuestEventManager.EASY_DUELS, 0));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(QuestEventManager.EASY_DUELS, 1));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(QuestEventManager.EASY_DUELS, 2));
        } else if (AllZone.getQuestData().getWin() == qpref.getPreferenceInt(QPref.WINS_MEDIUMAI, index)) {
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(QuestEventManager.EASY_DUELS, 0));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(QuestEventManager.MEDIUM_DUELS, 0));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(QuestEventManager.MEDIUM_DUELS, 1));
        } else if (AllZone.getQuestData().getWin() < qpref.getPreferenceInt(QPref.WINS_HARDAI, index)) {
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(QuestEventManager.MEDIUM_DUELS, 0));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(QuestEventManager.MEDIUM_DUELS, 1));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(QuestEventManager.MEDIUM_DUELS, 2));
        }

        else if (AllZone.getQuestData().getWin() == qpref.getPreferenceInt(QPref.WINS_HARDAI, index)) {
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(QuestEventManager.MEDIUM_DUELS, 0));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(QuestEventManager.HARD_DUELS, 0));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(QuestEventManager.HARD_DUELS, 1));
        }

        else if (AllZone.getQuestData().getWin() < qpref.getPreferenceInt(QPref.WINS_EXPERTAI, index)) {
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(QuestEventManager.HARD_DUELS, 0));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(QuestEventManager.HARD_DUELS, 1));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(QuestEventManager.HARD_DUELS, 2));
        } else {
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(QuestEventManager.HARD_DUELS, 0));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(QuestEventManager.HARD_DUELS, 1));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(QuestEventManager.EXPERT_DUELS, 2));
        }

        return duelOpponents;
    }

    /** Generates an array of new challenge opponents based on current win conditions.
     *
     * @return a {@link java.util.List} object.
     */
    public static final List<QuestChallenge> generateChallenges() {
        final List<QuestChallenge> challengeOpponents = new ArrayList<QuestChallenge>();
        final QuestData qData = AllZone.getQuestData();

        int maxChallenges = qData.getWin() / 10;
        if (maxChallenges > 5) {
            maxChallenges = 5;
        }

        // Generate IDs as needed.
        if ((qData.getAvailableChallenges() == null) || (qData.getAvailableChallenges().size() < maxChallenges)) {

            final List<Integer> unlockedChallengeIds = new ArrayList<Integer>();
            final List<Integer> availableChallengeIds = new ArrayList<Integer>();

            for (final QuestChallenge qc : QuestEventManager.ALL_CHALLENGES) {
                if ((qc.getWinsReqd() <= qData.getWin())
                        && !qData.getCompletedChallenges().contains(qc.getId())) {
                    unlockedChallengeIds.add(qc.getId());
                }
            }

            Collections.shuffle(unlockedChallengeIds);

            maxChallenges = Math.min(maxChallenges, unlockedChallengeIds.size());

            for (int i = 0; i < maxChallenges; i++) {
                availableChallengeIds.add(unlockedChallengeIds.get(i));
            }

            qData.setAvailableChallenges(availableChallengeIds);
            qData.saveData();
        }

        // Finally, pull challenge events from available IDs and return.
        for (final int i : qData.getAvailableChallenges()) {
            challengeOpponents.add(QuestEventManager.getChallengeEventByNumber(i));
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
    private static QuestDuel readDuel(final List<String> contents) {
        final QuestDuel qd = new QuestDuel();
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
    private static QuestChallenge readChallenge(final List<String> contents) {
        int eqpos;
        String key, value;

        final QuestChallenge qc = new QuestChallenge();
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
                qc.setCardRewardList(BoosterUtils.generateCardRewardList(value));
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
                qc.setCardRewardList(BoosterUtils.generateCardRewardList(value));
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
    private static void readMetadata(final List<String> contents, final QuestEvent qe) {
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
    private static void assembleDuelDifficultyLists() {
        EASY_DUELS.clear();
        MEDIUM_DUELS.clear();
        HARD_DUELS.clear();
        EXPERT_DUELS.clear();
        String s;

        for (final QuestDuel qd : ALL_DUELS) {
            s = qd.getDifficulty();
            if (s.equalsIgnoreCase("easy")) {
                EASY_DUELS.add(qd);
            } else if (s.equalsIgnoreCase("medium")) {
                MEDIUM_DUELS.add(qd);
            } else if (s.equalsIgnoreCase("hard")) {
                HARD_DUELS.add(qd);
            } else if (s.equalsIgnoreCase("very hard")) {
                EXPERT_DUELS.add(qd);
            }
        }
    }

    /**
     * <p>
     * getDuelOpponent.
     * </p>
     * Returns specific duel opponent from current shuffle of available duels.
     * This is to make sure that the opponents do not change when the deck
     * editor is launched.
     * 
     * @param aiDeck
     *            a {@link java.util.List} object.
     * @param number
     *            a int.
     * @return a {@link java.lang.String} object.
     */
    private static QuestDuel getDuelOpponentByNumber(final List<QuestDuel> aiDeck, final int n) {
        final List<QuestDuel> deckListCopy = new ArrayList<QuestDuel>(aiDeck);
        Collections.shuffle(deckListCopy, new Random(AllZone.getQuestData().getRandomSeed()));

        return deckListCopy.get(n);
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
    private static QuestChallenge getChallengeEventByNumber(final int n) {
        for (final QuestChallenge qc : ALL_CHALLENGES) {
            if (qc.getId() == n) {
                return qc;
            }
        }
        return null;
    }
}
