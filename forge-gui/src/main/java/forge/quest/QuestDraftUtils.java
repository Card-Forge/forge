package forge.quest;

import forge.FThreads;
import forge.GuiBase;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.DeckSection;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.player.RegisteredPlayer;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.util.storage.IStorage;

import java.util.ArrayList;
import java.util.List;

public class QuestDraftUtils {
    private static List<DraftMatchup> matchups = new ArrayList<DraftMatchup>();
    
    public static boolean matchInProgress = false;
    public static boolean aiMatchInProgress = false;
    private static boolean waitForUserInput = false;

    public static void continueMatch(Game lastGame) {
        if (lastGame.getMatch().isMatchOver()) {
            matchInProgress = false;
        }
        GuiBase.getInterface().continueMatch(matchInProgress ? lastGame.getMatch() : null);
    }
	
	public static void completeDraft(DeckGroup finishedDraft) {

		List<Deck> aiDecks = new ArrayList<Deck>(finishedDraft.getAiDecks());
		finishedDraft.getAiDecks().clear();

		for (int i = 0; i < aiDecks.size(); i++) {
			Deck oldDeck = aiDecks.get(i);
			Deck namedDeck = new Deck("AI Deck " + i);
			namedDeck.putSection(DeckSection.Main, oldDeck.get(DeckSection.Main));
			namedDeck.putSection(DeckSection.Sideboard, oldDeck.get(DeckSection.Sideboard));
			finishedDraft.getAiDecks().add(namedDeck);
		}

		IStorage<DeckGroup> draft = FModel.getQuest().getDraftDecks();
		draft.add(finishedDraft);

		FModel.getQuest().save();
		
	}
	
	public static String getDeckLegality() {
		String message = GameType.QuestDraft.getDecksFormat().getDeckConformanceProblem(FModel.getQuest().getAssets().getDraftDeckStorage().get(QuestEventDraft.DECK_NAME).getHumanDeck());
		if (message != null && FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY)) {
			return message;
		}
		return null;
	}

    public static void startNextMatch() {
        
        if (matchups.size() > 0) {
            return;
        }
        
        matchups.clear();
        
        QuestEventDraft draft = FModel.getQuest().getAchievements().getCurrentDraft();
        String[] currentStandings = draft.getStandings();
        
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
        
        update();
        
    }
    
    private static void addMatchup(final int player1, final int player2, final QuestEventDraft draft) {
        
        DraftMatchup matchup = new DraftMatchup();
        DeckGroup decks = FModel.getQuest().getAssets().getDraftDeckStorage().get(QuestEventDraft.DECK_NAME);
        
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
            
            matchup.hasHumanPlayer = true;
            matchup.matchStarter.add(new RegisteredPlayer(decks.getHumanDeck()).setPlayer(GuiBase.getInterface().getGuiPlayer()));

            int aiName = Integer.parseInt(draft.getStandings()[aiIndex]) - 1;
            
            int aiDeckIndex = Integer.parseInt(draft.getStandings()[aiIndex]) - 1;
            matchup.matchStarter.add(new RegisteredPlayer(decks.getAiDecks().get(aiDeckIndex)).setPlayer(GuiBase.getInterface().createAiPlayer(draft.getAINames()[aiName], draft.getAIIcons()[aiName])));
            
        } else {

            int aiName1 = Integer.parseInt(draft.getStandings()[player1]) - 1;
            int aiName2 = Integer.parseInt(draft.getStandings()[player2]) - 1;
            
            int aiDeckIndex = Integer.parseInt(draft.getStandings()[player1]) - 1;
            matchup.matchStarter.add(new RegisteredPlayer(decks.getAiDecks().get(aiDeckIndex)).setPlayer(GuiBase.getInterface().createAiPlayer(draft.getAINames()[aiName1], draft.getAIIcons()[aiName1])));

            aiDeckIndex = Integer.parseInt(draft.getStandings()[player2]) - 1;
            matchup.matchStarter.add(new RegisteredPlayer(decks.getAiDecks().get(aiDeckIndex)).setPlayer(GuiBase.getInterface().createAiPlayer(draft.getAINames()[aiName2], draft.getAIIcons()[aiName2])));
            
        }
        
        matchups.add(matchup);
    }

    public static void update() {
        if (matchups.isEmpty()) {
            if (!matchInProgress) {
                aiMatchInProgress = false;
            }
            return;
        }
        
        if (waitForUserInput) {
            return;
        }
        
        if (matchInProgress) {
            return;
        }
        
        GuiBase.getInterface().enableOverlay();
        
        DraftMatchup nextMatch = matchups.remove(0);
        
        matchInProgress = true;
        
        if (!nextMatch.hasHumanPlayer) {
            GuiBase.getInterface().disableOverlay();
            waitForUserInput = false;
            aiMatchInProgress = true;
        }
        else {
            waitForUserInput = true;
            aiMatchInProgress = false;
        }
        
        GameRules rules = new GameRules(GameType.QuestDraft);
        rules.setPlayForAnte(false);
        rules.setMatchAnteRarity(false);
        rules.setGamesPerMatch(3);
        rules.setManaBurn(FModel.getPreferences().getPrefBoolean(FPref.UI_MANABURN));
        rules.canCloneUseTargetsImage = FModel.getPreferences().getPrefBoolean(FPref.UI_CLONE_MODE_SOURCE);
        
        final Match match = new Match(rules, nextMatch.matchStarter);
        FThreads.invokeInEdtLater(new Runnable(){
            @Override
            public void run() {
                GuiBase.getInterface().startGame(match);
            }
        });
    }
    
    public static void continueMatches() {
        waitForUserInput = false;
        update();
    }
    
    private static class DraftMatchup {
        
        private List<RegisteredPlayer> matchStarter = new ArrayList<RegisteredPlayer>();
        private boolean hasHumanPlayer = false;
        
    }
}
