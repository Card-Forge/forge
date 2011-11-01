package forge.quest.gui.main;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import forge.AllZone;
import forge.FileUtil;
import forge.deck.DeckManager;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.quest.data.QuestPreferences;
import forge.quest.data.QuestUtil;

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
    public List<QuestDuel> easyAIduels = null;

    /** The medium a iduels. */
    public List<QuestDuel> mediumAIduels = null;

    /** The hard a iduels. */
    public List<QuestDuel> hardAIduels = null;

    /** The very hard a iduels. */
    public List<QuestDuel> veryHardAIduels = null;

    /** The all duels. */
    public List<QuestDuel> allDuels = null;

    /** The all challenges. */
    public List<QuestChallenge> allChallenges = null;

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

        List<String> contents;
        QuestEvent tempEvent;

        final File file = ForgeProps.getFile(NewConstants.Quest.DECKS);

        final DeckManager manager = new DeckManager(file);

        final File[] allFiles = ForgeProps.getFile(NewConstants.Quest.DECKS).listFiles(DeckManager.DCK_FILE_FILTER);

        for (final File f : allFiles) {
            contents = FileUtil.readFile(f);

            if (contents.get(0).trim().equals("[quest]")) {
                tempEvent = new QuestChallenge();
                this.assembleChallengeUniquedata(contents, (QuestChallenge) tempEvent);
                this.allChallenges.add((QuestChallenge) tempEvent);
            } // End if([quest])
            else {
                tempEvent = new QuestDuel();
                this.assembleDuelUniquedata(contents, (QuestDuel) tempEvent);
                this.allDuels.add((QuestDuel) tempEvent);
            }

            // Assemble metadata (may not be necessary later) and deck object.
            this.assembleEventMetadata(contents, tempEvent);
            tempEvent.eventDeck = manager.getDeck(tempEvent.getName());
        } // End for(allFiles)

        this.assembleDuelDifficultyLists();

    } // End assembleAllEvents()

    /**
     * <p>
     * assembleDuelUniqueData.
     * </p>
     * Handler for any unique data contained in duel files.
     * 
     * @param contents
     * @param qd
     */
    private void assembleDuelUniquedata(final List<String> contents, final QuestDuel qd) {
        int eqpos;
        String key, value;

        for (final String s : contents) {
            if (s.equals("[metadata]")) {
                break;
            }
            if (s.equals("[duel]")) {
                continue;
            }
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
                qd.name = value;
            }
        }
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
    private void assembleChallengeUniquedata(final List<String> contents, final QuestChallenge qc) {
        int eqpos;
        String key, value;

        // Unique properties
        for (final String s : contents) {
            if (s.equals("[metadata]")) {
                break;
            }
            if (s.equals("[quest]")) {
                continue;
            }
            if (s.equals("")) {
                continue;
            }

            eqpos = s.indexOf('=');
            key = s.substring(0, eqpos);
            value = s.substring(eqpos + 1).trim();

            if (key.equalsIgnoreCase("ID")) {
                qc.id = Integer.parseInt(value);
            } else if (key.equalsIgnoreCase("Repeat")) {
                qc.repeatable = Boolean.parseBoolean(value);
            } else if (key.equalsIgnoreCase("AILife")) {
                qc.aiLife = Integer.parseInt(value);
            } else if (key.equalsIgnoreCase("Wins")) {
                qc.winsReqd = Integer.parseInt(value);
            } else if (key.equalsIgnoreCase("Credit Reward")) {
                qc.creditsReward = Integer.parseInt(value);
            } else if (key.equalsIgnoreCase("Card Reward")) {
                qc.cardReward = value;
                qc.cardRewardList = QuestUtil.generateCardRewardList(value);
            }
            // Human extra card list assembled here.
            else if (key.equalsIgnoreCase("HumanExtras") && !value.equals("")) {
                final String[] names = value.split("\\|");
                final List<String> templist = new ArrayList<String>();

                for (final String n : names) {
                    templist.add(n);
                }

                qc.humanExtraCards = templist;
            }
            // AI extra card list assembled here.
            else if (key.equalsIgnoreCase("AIExtras") && !value.equals("")) {
                final String[] names = value.split("\\|");
                final List<String> templist = new ArrayList<String>();

                for (final String n : names) {
                    templist.add(n);
                }

                qc.aiExtraCards = templist;
            }
            // Card reward list assembled here.
            else if (key.equalsIgnoreCase("Card Reward")) {
                qc.cardReward = value;
                qc.cardRewardList = QuestUtil.generateCardRewardList(value);
            }
        }
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
    private void assembleEventMetadata(final List<String> contents, final QuestEvent qe) {
        int eqpos;
        String key, value;

        for (String s : contents) {
            s = s.trim();
            eqpos = s.indexOf('=');

            if (s.equals("[main]")) {
                break;
            }
            if (s.equals("[metadata]")) {
                continue;
            }
            if (s.equals("")) {
                continue;
            }
            if (eqpos == -1) {
                continue;
            }

            key = s.substring(0, eqpos);
            value = s.substring(eqpos + 1);

            if (key.equalsIgnoreCase("Name")) {
                qe.name = value;
            } else if (key.equalsIgnoreCase("Title")) {
                qe.title = value;
            } else if (key.equalsIgnoreCase("Difficulty")) {
                qe.difficulty = value;
            } else if (key.equalsIgnoreCase("Description")) {
                qe.description = value;
            } else if (key.equalsIgnoreCase("Icon")) {
                qe.icon = value;
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
        this.easyAIduels = new ArrayList<QuestDuel>();
        this.mediumAIduels = new ArrayList<QuestDuel>();
        this.hardAIduels = new ArrayList<QuestDuel>();
        this.veryHardAIduels = new ArrayList<QuestDuel>();
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

        final int index = AllZone.getQuestData().getDifficultyIndex();
        final List<QuestDuel> duelOpponents = new ArrayList<QuestDuel>();

        if (AllZone.getQuestData().getWin() < QuestPreferences.getWinsForMediumAI(index)) {
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(this.easyAIduels, 0));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(this.easyAIduels, 1));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(this.easyAIduels, 2));
        } else if (AllZone.getQuestData().getWin() == QuestPreferences.getWinsForMediumAI(index)) {
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(this.easyAIduels, 0));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(this.mediumAIduels, 0));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(this.mediumAIduels, 1));
        } else if (AllZone.getQuestData().getWin() < QuestPreferences.getWinsForHardAI(index)) {
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(this.mediumAIduels, 0));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(this.mediumAIduels, 1));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(this.mediumAIduels, 2));
        }

        else if (AllZone.getQuestData().getWin() == QuestPreferences.getWinsForHardAI(index)) {
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(this.mediumAIduels, 0));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(this.hardAIduels, 0));
            duelOpponents.add(QuestEventManager.getDuelOpponentByNumber(this.hardAIduels, 1));
        }

        else if (AllZone.getQuestData().getWin() < QuestPreferences.getWinsForVeryHardAI(index)) {
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
    public final List<QuestChallenge> generateChallenges() {
        final forge.quest.data.QuestData questData = AllZone.getQuestData();

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
