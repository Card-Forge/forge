package forge.quest;

import java.util.ArrayList;
import java.util.List;

import forge.GuiBase;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.DeckSection;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.interfaces.IGuiGame;
import forge.match.HostedMatch;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.properties.ForgePreferences.FPref;
import forge.util.storage.IStorage;

public class QuestDraftUtils {
    private static final List<DraftMatchup> matchups = new ArrayList<DraftMatchup>();

    public static boolean matchInProgress = false;
    private static boolean waitForUserInput = false;

    public static void completeDraft(final DeckGroup finishedDraft) {

        final List<Deck> aiDecks = new ArrayList<Deck>(finishedDraft.getAiDecks());
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
        final String message = GameType.QuestDraft.getDeckFormat().getDeckConformanceProblem(FModel.getQuest().getAssets().getDraftDeckStorage().get(QuestEventDraft.DECK_NAME).getHumanDeck());
        if (message != null && FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY)) {
            return message;
        }
        return null;
    }

    public static void startNextMatch(final IGuiGame gui) {
        if (matchups.size() > 0) {
            return;
        }

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

        switch (currentSet) {

        case 7:
            addMatchup(0, 1, draft);
            addMatchup(2, 3, draft);
            addMatchup(4, 5, draft);
            addMatchup(6, 7, draft);
            break;

        case 8:
            addMatchup(2, 3, draft);
            addMatchup(4, 5, draft);
            addMatchup(6, 7, draft);
            break;

        case 9:
            addMatchup(4, 5, draft);
            addMatchup(6, 7, draft);
            break;

        case 10:
            addMatchup(6, 7, draft);
            break;

        case 11:
            addMatchup(8, 9, draft);
            addMatchup(10, 11, draft);
            break;

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
        } else if (draft.getStandings()[player2].equals(QuestEventDraft.HUMAN)) {
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

    public static void update(final IGuiGame gui) {
        if (matchups.isEmpty()) {
            return;
        }

        if (waitForUserInput || matchInProgress) {
            return;
        }

        gui.enableOverlay();

        final DraftMatchup nextMatch = matchups.remove(0);
        matchInProgress = true;

        if (nextMatch.hasHumanPlayer()) {
            waitForUserInput = true;
        } else {
            gui.disableOverlay();
            waitForUserInput = false;
        }

        final GameRules rules = new GameRules(GameType.QuestDraft);
        rules.setPlayForAnte(false);
        rules.setMatchAnteRarity(false);
        rules.setGamesPerMatch(3);
        rules.setManaBurn(FModel.getPreferences().getPrefBoolean(FPref.UI_MANABURN));
        rules.setCanCloneUseTargetsImage(FModel.getPreferences().getPrefBoolean(FPref.UI_CLONE_MODE_SOURCE));

        final HostedMatch newMatch = GuiBase.getInterface().hostMatch();
        newMatch.startMatch(rules, null, nextMatch.matchStarter, nextMatch.humanPlayer, GuiBase.getInterface().getNewGuiGame());
    }

    public static void continueMatches(final IGuiGame gui) {
        waitForUserInput = false;
        update(gui);
    }

    private static final class DraftMatchup {
        private final List<RegisteredPlayer> matchStarter = new ArrayList<RegisteredPlayer>();
        private RegisteredPlayer humanPlayer = null;
        private void setHumanPlayer(final RegisteredPlayer humanPlayer) {
            this.matchStarter.add(humanPlayer);
            this.humanPlayer = humanPlayer;
        }
        private boolean hasHumanPlayer() {
            return humanPlayer != null;
        }
    }
}
