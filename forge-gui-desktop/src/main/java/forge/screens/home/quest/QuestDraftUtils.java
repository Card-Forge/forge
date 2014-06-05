package forge.screens.home.quest;

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import forge.GuiBase;
import forge.Singletons;
import forge.deck.DeckGroup;
import forge.game.Game;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gui.SOverlayUtils;
import forge.gui.framework.FScreen;
import forge.model.FModel;
import forge.quest.QuestEventDraft;

public class QuestDraftUtils {
    
    private static List<DraftMatchup> matchups = new ArrayList<DraftMatchup>();
    
    public static boolean matchInProgress = false;
    public static boolean aiMatchInProgress = false;
    private static boolean waitForUserInput = false;
    
    public static void continueMatch(Game lastGame) {
        
        if (lastGame.getMatch().isMatchOver()) {
            matchInProgress = false;
        }
        
        if (!matchInProgress) {
            Singletons.getControl().endCurrentGame();
            Singletons.getControl().setCurrentScreen(FScreen.HOME_SCREEN);
        } else {
            Singletons.getControl().endCurrentGame();
            Singletons.getControl().startGameWithUi(lastGame.getMatch());
        }
        
    }

    public static void startNextMatch() {
        
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
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SOverlayUtils.startGameOverlay();
                SOverlayUtils.showOverlay();
            }
        });
        
        if (!nextMatch.hasHumanPlayer) {
            GuiBase.getInterface().disableOverlay();
            waitForUserInput = false;
            aiMatchInProgress = true;
        } else {
            waitForUserInput = true;
            aiMatchInProgress = false;
        }
        
        GuiBase.getInterface().startMatch(GameType.QuestDraft, nextMatch.matchStarter);
        
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
