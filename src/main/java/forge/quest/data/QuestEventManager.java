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
import forge.util.FileUtil;
import forge.util.SectionUtil;

/**
 * <p>
 * QuestEventManager.
 * </p>
 * MODEL - Manages collections of quest events (duelsquests, etc.)
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestEventManager {

    /** The easy a iduels. */
    private final List<QuestDuel> easyAIduels = new ArrayList<QuestDuel>();

    /** The medium a iduels. */
    private final List<QuestDuel> mediumAIduels = new ArrayList<QuestDuel>();

    /** The hard a iduels. */
    private final List<QuestDuel> hardAIduels = new ArrayList<QuestDuel>();

    /** The very hard a iduels. */
    private final List<QuestDuel> veryHardAIduels = new ArrayList<QuestDuel>();

    /** The all duels. */
    private List<QuestDuel> allDuels = null;

    /** The all challenges. */
    private List<QuestChallenge> allChallenges = null;

    /**
     * <p>
     * assembleAllEvents.
     * </p>
     * * Reads all duel and challenge files and instantiates all events, and
     * difficulty lists accordingly. Should be used sparingly.
     */
    public final void assembleAllEvents() {
        this.allDuels = new ArrayList<QuestDuel>();
        this.allChallenges = new ArrayList<QuestChallenge>();

        QuestEvent tempEvent;

        final File[] allFiles = ForgeProps.getFile(NewConstants.Quest.DECKS).listFiles(DeckSerializer.DCK_FILE_FILTER);

        for (final File f : allFiles) {
            Map<String, List<String>> contents = SectionUtil.parseSections(FileUtil.readFile(f));

            if (contents.containsKey("quest")) {
                tempEvent = readChallenge(contents.get("quest"));
                this.allChallenges.add((QuestChallenge) tempEvent);
            } // End if([quest])
            else {
                tempEvent = readDuel(contents.get("metadata"));
                this.allDuels.add((QuestDuel) tempEvent);
            }

            // Assemble metadata (may not be necessary later) and deck object.
            this.readMetadata(contents.get("metadata"), tempEvent);
            tempEvent.setEventDeck(Deck.fromSections(contents));
        } // End for(allFiles)

        this.assembleDuelDifficultyLists();

    } // End assembleAllEvents()

    /**
     * Retrieve single event, using its name.
     * 
     * @param s0 &emsp; {@link java.lang.String}
     * @return {@link forge.data.QuestEvent}
     */
    public QuestEvent getEvent(final String s0) {
        for (QuestEvent q : allDuels) {
            if (q.getName().equals(s0)) { return q; } }

        for (QuestChallenge q : allChallenges) {
            if (q.getName().equals(s0)) { return q; } }

        return null;
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
    private QuestDuel readDuel(final List<String> contents) {
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
    private QuestChallenge readChallenge(final List<String> contents) {
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
     * getAllDuels.
     * </p>
     * Returns complete list of all duel objects.
     * 
     * @return a {@link java.util.List} object.
     */
    public final List<QuestDuel> getAllDuels() {
        return this.allDuels;
    }

    /**
     * <p>
     * getAllChallenges.
     * </p>
     * Returns complete list of all challenge objects.
     * 
     * @return a {@link java.util.List} object.
     */
    public final List<QuestChallenge> getAllChallenges() {
        return this.allChallenges;
    }

    /**
     * <p>
     * assembleDuelDifficultyLists.
     * </p>
     * Assemble duel deck difficulty lists
     */
    private void assembleDuelDifficultyLists() {

        easyAIduels.clear();
        mediumAIduels.clear();
        hardAIduels.clear();
        veryHardAIduels.clear();
        String s;

        for (final QuestDuel qd : this.allDuels) {
            s = qd.getDifficulty();
            if (s.equalsIgnoreCase("easy")) {
                this.easyAIduels.add(qd);
            } else if (s.equalsIgnoreCase("medium")) {
                this.mediumAIduels.add(qd);
            } else if (s.equalsIgnoreCase("hard")) {
                this.hardAIduels.add(qd);
            } else if (s.equalsIgnoreCase("very hard")) {
                this.veryHardAIduels.add(qd);
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
    private QuestChallenge getChallengeEventByNumber(final int n) {
        for (final QuestChallenge qc : this.allChallenges) {
            if (qc.getId() == n) {
                return qc;
            }
        }
        return null;
    }

    /**
     * <p>
     * generateDuels.
     * </p>
     * Generates an array of new duel opponents based on current win conditions.
     * 
     * @return an array of {@link java.lang.String} objects.
     */
    public final List<QuestDuel> generateDuels() {
        final QuestPreferences qpref = Singletons.getModel().getQuestPreferences();
        if (AllZone.getQuestData() == null) { return null; }

        final int index = AllZone.getQuestData().getDifficultyIndex();
        final List<QuestDuel> duelOpponents = new ArrayList<QuestDuel>();

        if (AllZone.getQuestData().getWin() < qpref.getPreferenceInt(QPref.WINS_MEDIUMAI, index)) {
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(this.easyAIduels, 0));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(this.easyAIduels, 1));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(this.easyAIduels, 2));
        } else if (AllZone.getQuestData().getWin() == qpref.getPreferenceInt(QPref.WINS_MEDIUMAI, index)) {
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(this.easyAIduels, 0));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(this.mediumAIduels, 0));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(this.mediumAIduels, 1));
        } else if (AllZone.getQuestData().getWin() < qpref.getPreferenceInt(QPref.WINS_HARDAI, index)) {
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(this.mediumAIduels, 0));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(this.mediumAIduels, 1));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(this.mediumAIduels, 2));
        }

        else if (AllZone.getQuestData().getWin() == qpref.getPreferenceInt(QPref.WINS_HARDAI, index)) {
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(this.mediumAIduels, 0));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(this.hardAIduels, 0));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(this.hardAIduels, 1));
        }

        else if (AllZone.getQuestData().getWin() < qpref.getPreferenceInt(QPref.WINS_EXPERTAI, index)) {
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(this.hardAIduels, 0));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(this.hardAIduels, 1));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(this.hardAIduels, 2));
        } else {
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(this.hardAIduels, 0));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(this.hardAIduels, 1));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(this.veryHardAIduels, 2));
        }

        return duelOpponents;
    }

    /**
     * <p>
     * generateChallenges.
     * </p>
     * Generates an array of new challenge opponents based on current win
     * conditions.
     * 
     * @return a {@link java.util.List} object.
     */
    public final List<QuestChallenge> generateChallenges(QuestData questData) {
        final List<QuestChallenge> challengeOpponents = new ArrayList<QuestChallenge>();

        int maxChallenges = questData.getWin() / 10;
        if (maxChallenges > 5) {
            maxChallenges = 5;
        }

        // Generate IDs as needed.
        if ((questData.getAvailableChallenges() == null) || (questData.getAvailableChallenges().size() < maxChallenges)) {

            final List<Integer> unlockedChallengeIds = new ArrayList<Integer>();
            final List<Integer> availableChallengeIds = new ArrayList<Integer>();

            for (final QuestChallenge qc : this.allChallenges) {
                if ((qc.getWinsReqd() <= questData.getWin())
                        && !questData.getCompletedChallenges().contains(qc.getId())) {
                    unlockedChallengeIds.add(qc.getId());
                }
            }

            Collections.shuffle(unlockedChallengeIds);

            maxChallenges = Math.min(maxChallenges, unlockedChallengeIds.size());

            for (int i = 0; i < maxChallenges; i++) {
                availableChallengeIds.add(unlockedChallengeIds.get(i));
            }

            questData.setAvailableChallenges(availableChallengeIds);
            questData.saveData();
        }

        // Finally, pull challenge events from available IDs and return.
        for (final int i : questData.getAvailableChallenges()) {
            challengeOpponents.add(this.getChallengeEventByNumber(i));
        }

        return challengeOpponents;
    }

}
