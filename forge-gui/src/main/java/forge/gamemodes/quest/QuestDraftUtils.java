package forge.gamemodes.quest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;

import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.DeckSection;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.limited.LimitedDeckEvaluator;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.quest.data.QuestPreferences;
import forge.gamemodes.tournament.system.TournamentBracket;
import forge.gamemodes.tournament.system.TournamentPairing;
import forge.gamemodes.tournament.system.TournamentPlayer;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.util.MyRandom;
import forge.util.storage.IStorage;

public class QuestDraftUtils {
    public static boolean TOURNAMENT_TOGGLE = false;
    public static boolean AI_BACKGROUND = false;

    public static boolean matchInProgress = false;
    private static boolean waitForUserInput = false;
    private static final List<DraftMatchup> matchups = new ArrayList<>();

    public enum Mode {
        EMPTY,
        SELECT_TOURNAMENT,
        PREPARE_DECK,
        TOURNAMENT_ACTIVE
    }

    public static void completeDraft(final DeckGroup finishedDraft) {
        final List<Deck> aiDecks = new ArrayList<>(finishedDraft.getAiDecks());
        finishedDraft.getAiDecks().clear();

        for (int i = 0; i < aiDecks.size(); i++) {
            final Deck oldDeck = aiDecks.get(i);
            final Deck namedDeck = new Deck("AI Deck " + i);
            namedDeck.putSection(DeckSection.Main, oldDeck.get(DeckSection.Main));
            namedDeck.putSection(DeckSection.Sideboard, oldDeck.get(DeckSection.Sideboard));
            finishedDraft.getAiDecks().add(namedDeck);
        }

        final IStorage<DeckGroup> draft = FModel.getQuest().getDraftDecks();
        draft.add(finishedDraft);

        FModel.getQuest().save();
    }

    public static String getDeckLegality() {
        if (!FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY)) {
            return null;
        }

        return GameType.QuestDraft.getDeckFormat().getDeckConformanceProblem(FModel.getQuest().getAssets().getDraftDeckStorage().get(QuestEventDraft.DECK_NAME).getHumanDeck());
    }

    private static int getPreviousMatchup(final int position) {
        switch (position) {
            case 0:
            case 1:
                return 0;
            case 2:
            case 3:
                return 2;
            case 4:
            case 5:
                return 4;
            case 6:
            case 7:
                return 6;
            case 8:
                return 0;
            case 9:
                return 2;
            case 10:
                return 4;
            case 11:
                return 6;
            case 12:
                return 8;
            case 13:
                return 10;
            case 14:
                return 12;
        }
        return -1;
    }

    public static void startNextMatchInTournament(final IGuiGame gui) {
        final QuestEventDraft draft = FModel.getQuest().getAchievements().getCurrentDraft();
        TournamentBracket bracket = draft.getBracket();

        TournamentPairing pairing = bracket.getNextPairing();
        if (pairing == null) {
            if (bracket.isTournamentOver()) {
                // Somehow tournament didn't end on its own?
            } else {
                 // Generate next round and regrab the next pairing
                bracket.generateActivePairings();
                pairing = bracket.getNextPairing();
            }
        }

        updateFromTournament(gui);
    }

    public static void startNextMatch(final IGuiGame gui) {
        if (TOURNAMENT_TOGGLE) {
            startNextMatchInTournament(gui);
            return;
        }

        if (!matchups.isEmpty()) {
            return;
        }

        // If matchups isn't empty I don't get here. Right?
        matchups.clear();

        final QuestEventDraft draft = FModel.getQuest().getAchievements().getCurrentDraft();
        final String[] currentStandings = draft.getStandings();

        int currentSet = -1;

        for (int i = currentStandings.length - 1; i >= 0; i--) {
            if (!currentStandings[i].equals(QuestEventDraft.UNDETERMINED)) {
                currentSet = i;
                break;
            }
        }

        int latestSet = currentSet;
        //Choose the start of each matchup; it's always even (0v1 2v3 4v5)
        if (latestSet % 2 == 1) {
            latestSet--;
        }

        if (latestSet == currentStandings.length - 1) {
            // No more matches left
            return;
        }

        //Fill in any missing spots in previous brackets
        boolean foundMatchups = false;
        for (int i = 0; i <= latestSet && i <= 14; i += 2) {
            if (currentStandings[i].equals(QuestEventDraft.UNDETERMINED) && !currentStandings[i + 1].equals(QuestEventDraft.UNDETERMINED)) {
                int previousMatchup = getPreviousMatchup(i);
                addMatchup(previousMatchup, previousMatchup + 1, draft);
                foundMatchups = true;
            } else if (!currentStandings[i].equals(QuestEventDraft.UNDETERMINED) && currentStandings[i + 1].equals(QuestEventDraft.UNDETERMINED)) {
                int previousMatchup = getPreviousMatchup(i + 1);
                addMatchup(previousMatchup, previousMatchup + 1, draft);
                foundMatchups = true;
            } else if (currentStandings[i].equals(QuestEventDraft.UNDETERMINED) && currentStandings[i + 1].equals(QuestEventDraft.UNDETERMINED)) {
                int previousMatchup = getPreviousMatchup(i);
                addMatchup(previousMatchup, previousMatchup + 1, draft);
                if (i >= 8) {
                    previousMatchup = getPreviousMatchup(i + 1);
                    addMatchup(previousMatchup, previousMatchup + 1, draft);
                }
                foundMatchups = true;
            }
        }

        //If no previous matches need doing, start the next round as normal
        if (!foundMatchups) {
            // Fall through to avoid repetition
            switch (currentSet) {
                case 7:
                    addMatchup(0, 1, draft);
                case 8:
                    addMatchup(2, 3, draft);
                case 9:
                    addMatchup(4, 5, draft);
                case 10:
                    addMatchup(6, 7, draft);
                    break;
                case 11:
                    addMatchup(8, 9, draft);
                case 12:
                    addMatchup(10, 11, draft);
                    break;
                case 13:
                    addMatchup(12, 13, draft);
                    break;
                case 14:
                default:
                    return;
            }
        }

        update(gui);
    }

    private static void addMatchup(final int player1, final int player2, final QuestEventDraft draft) {
        final DraftMatchup matchup = new DraftMatchup();
        final DeckGroup decks = FModel.getQuest().getAssets().getDraftDeckStorage().get(QuestEventDraft.DECK_NAME);

        int humanIndex = -1;
        int aiIndex = -1;

        if (draft.getStandings()[player1].equals(QuestEventDraft.HUMAN)) {
            humanIndex = player1;
            aiIndex = player2;
        }
        else if (draft.getStandings()[player2].equals(QuestEventDraft.HUMAN)) {
            humanIndex = player2;
            aiIndex = player1;
        }

        if (humanIndex > -1) {
            matchup.setHumanPlayer(new RegisteredPlayer(decks.getHumanDeck()).setPlayer(GamePlayerUtil.getGuiPlayer()));

            final int aiName = Integer.parseInt(draft.getStandings()[aiIndex]) - 1;

            final int aiDeckIndex = Integer.parseInt(draft.getStandings()[aiIndex]) - 1;
            matchup.matchStarter.add(new RegisteredPlayer(decks.getAiDecks().get(aiDeckIndex)).setPlayer(GamePlayerUtil.createAiPlayer(draft.getAINames()[aiName], draft.getAIIcons()[aiName])));
        } else {
            final int aiName1 = Integer.parseInt(draft.getStandings()[player1]) - 1;
            final int aiName2 = Integer.parseInt(draft.getStandings()[player2]) - 1;

            int aiDeckIndex = Integer.parseInt(draft.getStandings()[player1]) - 1;
            matchup.matchStarter.add(new RegisteredPlayer(decks.getAiDecks().get(aiDeckIndex)).setPlayer(GamePlayerUtil.createAiPlayer(draft.getAINames()[aiName1], draft.getAIIcons()[aiName1])));

            aiDeckIndex = Integer.parseInt(draft.getStandings()[player2]) - 1;
            matchup.matchStarter.add(new RegisteredPlayer(decks.getAiDecks().get(aiDeckIndex)).setPlayer(GamePlayerUtil.createAiPlayer(draft.getAINames()[aiName2], draft.getAIIcons()[aiName2])));
        }

        matchups.add(matchup);
    }

    private static GameRules createQuestDraftRuleset() {
        final GameRules rules = new GameRules(GameType.QuestDraft);
        rules.setPlayForAnte(false);
        rules.setMatchAnteRarity(false);
        rules.setGamesPerMatch(3);
        rules.setManaBurn(FModel.getPreferences().getPrefBoolean(FPref.UI_MANABURN));
        rules.setCanCloneUseTargetsImage(FModel.getPreferences().getPrefBoolean(FPref.UI_CLONE_MODE_SOURCE));
        rules.setUseGrayText(FModel.getPreferences().getPrefBoolean(FPref.UI_GRAY_INACTIVE_TEXT));
        return rules;
    }

    public static void update(final IGuiGame gui) {
        if (TOURNAMENT_TOGGLE) {
            updateFromTournament(gui);
            return;
        }

        if (matchups.isEmpty()) {
            return;
        }

        if (waitForUserInput || matchInProgress) {
            return;
        }

        gui.enableOverlay();

        final DraftMatchup nextMatch = matchups.remove(0);

        if (nextMatch.hasHumanPlayer()) {
            waitForUserInput = true;
        } else {
            if (FModel.getQuestPreferences().getPrefInt(QuestPreferences.QPref.SIMULATE_AI_VS_AI_RESULTS) == 1 || GuiBase.getInterface().isLibgdxPort()) {
                return;
            }
            gui.disableOverlay();
            waitForUserInput = false;
        }

        matchInProgress = true;

        GameRules rules = createQuestDraftRuleset();

        final HostedMatch newMatch = GuiBase.getInterface().hostMatch();
        newMatch.startMatch(rules, null, nextMatch.matchStarter, nextMatch.humanPlayer, GuiBase.getInterface().getNewGuiGame());
    }

    @SuppressWarnings("unused")
    private static List<RegisteredPlayer> registerTournamentPlayers(TournamentPairing pairing, QuestEventDraft draft, DeckGroup decks) {
        List<RegisteredPlayer> registered = Lists.newArrayList();
        for (TournamentPlayer pl : pairing.getPairedPlayers()) {
            if (pl.getIndex() == -1) {
                registered.add(new RegisteredPlayer(decks.getHumanDeck()).setPlayer(pl.getPlayer()));
            } else {
                registered.add(new RegisteredPlayer(decks.getAiDecks().get(pl.getIndex())).setPlayer(pl.getPlayer()));
            }
        }
        return registered;
    }

    @SuppressWarnings("unused")
    private static void updateFromTournament(final IGuiGame gui) {
        // If Human involved launch into a UI, if not show a "Simulating" screen. And simulate the game off-thread
        if (waitForUserInput || matchInProgress || gui == null) {
            return;
        }

        gui.enableOverlay();
        final QuestEventDraft draft = FModel.getQuest().getAchievements().getCurrentDraft();
        if (draft == null) {
            return;
        }

        TournamentBracket bracket = draft.getBracket();
        TournamentPairing pairing = bracket.getNextPairing();

        if (pairing == null) {
            // bracket.isTournamentOver()

            gui.disableOverlay();
            return;
        }

        matchInProgress = true;
        final DeckGroup decks = FModel.getQuest().getAssets().getDraftDeckStorage().get(QuestEventDraft.DECK_NAME);

        boolean waitForUserInput = pairing.hasPlayer(GamePlayerUtil.getGuiPlayer());
        final GameRules rules = createQuestDraftRuleset();

        final List<RegisteredPlayer> registered = TournamentBracket.registerTournamentPlayers(pairing, decks);
        RegisteredPlayer registeredHuman = null;

        if (waitForUserInput) {
            final HostedMatch newMatch = GuiBase.getInterface().hostMatch();
            for(RegisteredPlayer rp : registered) {
                if (rp.getPlayer().equals(GamePlayerUtil.getGuiPlayer())) {
                    registeredHuman = rp;
                }
            }
            newMatch.startMatch(rules, null, registered, registeredHuman, GuiBase.getInterface().getNewGuiGame());
        } else {
            // TODO Show a "Simulating Dialog" and simulate off-thread. Temporary replication of code for now
            gui.disableOverlay();
            if (AI_BACKGROUND) {
                System.out.println("Spawning a thread to simulate the match");
                // Show Dialog popup

                /*
                FThreads.invokeInBackgroundThread(new Runnable() {
                    @Override
                    public void run() {
                        Match mc = new Match(rules, registered, "Simulated Match");
                        Game gm = mc.createGame();
                        mc.startGame(gm);
                    }
                });*/

                Match mc = new Match(rules, registered, "Simulated Match");
                String winner = null;
                while (!mc.isMatchOver()) {
                    Game gm = mc.createGame();
                    mc.startGame(gm);
                    // Update dialog with winner
                }

                RegisteredPlayer regPlayer = mc.getWinner();
                //draft.setWinner(regPlayer.getPlayer().getName());
                //FModel.getQuest().save();
                gui.finishGame();
            } else {
                final HostedMatch newMatch = GuiBase.getInterface().hostMatch();
                newMatch.startMatch(rules, null, registered, registeredHuman, GuiBase.getInterface().getNewGuiGame());
            }

        }
    }

    public static void continueMatches(final IGuiGame gui) {
        waitForUserInput = false;
        update(gui);
    }

    public static void cancelFurtherMatches() {
        matchInProgress = false;
        waitForUserInput = false;
        matchups.clear();
        if (TOURNAMENT_TOGGLE) {
            final QuestEventDraft draft = FModel.getQuest().getAchievements().getCurrentDraft();
            TournamentBracket bracket = draft.getBracket();
            bracket.endTournament();
        }
    }

    private static final class DraftMatchup {
        private final List<RegisteredPlayer> matchStarter = new ArrayList<>();
        private RegisteredPlayer humanPlayer = null;
        private void setHumanPlayer(final RegisteredPlayer humanPlayer) {
            this.matchStarter.add(humanPlayer);
            this.humanPlayer = humanPlayer;
        }
        private boolean hasHumanPlayer() {
            return humanPlayer != null;
        }
    }

    private static double rankDraftDeckValue(String sid) {
        DeckGroup draftDecks = FModel.getQuest().getDraftDecks().get(QuestEventDraft.DECK_NAME);
        Deck d = sid.equals(QuestEventDraft.HUMAN) ? draftDecks.getHumanDeck() : draftDecks.getAiDecks().get(Integer.parseInt(sid) - 1);

        return LimitedDeckEvaluator.getDeckDraftValue(d);
    }

    public static boolean isNextMatchAIvsAI() {
        QuestEventDraft qd = FModel.getQuest().getAchievements().getCurrentDraft();
        int pos = Arrays.asList(qd.getStandings()).indexOf(QuestEventDraft.UNDETERMINED);
        if (pos == -1) {
            return false;
        }

        int offset = (pos - 8) * 2;

        String sid1 = qd.getStandings()[offset];
        String sid2 = qd.getStandings()[offset + 1];

        return !sid1.equals(QuestEventDraft.HUMAN) && !sid2.equals(QuestEventDraft.HUMAN);
    }

    public static boolean injectRandomMatchOutcome(boolean simHumanMatches) {
        matchInProgress = true; // prevent the player from trying to start another match before we finish simulating results
        
        QuestEventDraft qd = FModel.getQuest().getAchievements().getCurrentDraft();

        int pos = Arrays.asList(qd.getStandings()).indexOf(QuestEventDraft.UNDETERMINED);
        if (pos == -1) {
            // no more matches left to play
            matchInProgress = false;
            return true;
        }

        int offset = (pos - 8) * 2;

        String sid1 = qd.getStandings()[offset];
        String sid2 = qd.getStandings()[offset + 1];

        if (sid1.equals(QuestEventDraft.HUMAN) || sid2.equals(QuestEventDraft.HUMAN)) {
            if (!simHumanMatches) {
                matchInProgress = false;
                return false;
            }
        }

        // evaluate decks
        double deck1Value = rankDraftDeckValue(sid1);
        double deck2Value = rankDraftDeckValue(sid2);

        // Bias victory towards a deck with higher total draft rank value
        // Decks with higher rank value have 75% win rate vs. decks with lower rank.
        boolean strongerDeckWon = MyRandom.getRandom().nextInt(100) < 75;
        qd.getStandings()[pos] = strongerDeckWon ? (deck1Value > deck2Value ? sid1 : sid2) : (deck1Value > deck2Value ? sid2 : sid1);

        FModel.getQuest().save();

        matchInProgress = false;

        return true;
    }
}
