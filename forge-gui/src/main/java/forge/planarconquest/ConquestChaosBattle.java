package forge.planarconquest;

import java.io.File;
import java.util.Set;

import forge.LobbyPlayer;
import forge.deck.Deck;
import forge.game.GameType;
import forge.game.GameView;
import forge.interfaces.IButton;
import forge.interfaces.IWinLoseView;
import forge.model.FModel;
import forge.planarconquest.ConquestPreferences.CQPref;
import forge.properties.ForgeConstants;
import forge.quest.QuestEventDifficulty;
import forge.quest.QuestEventDuel;
import forge.quest.QuestEventDuelManager;
import forge.quest.QuestWorld;
import forge.util.Aggregates;

public class ConquestChaosBattle extends ConquestEvent {
    private final QuestWorld world;
    private final QuestEventDuel duel;

    public ConquestChaosBattle() {
        super(null, 0);

        //use a random quest event duel for Chaos battle
        //using Chaos battle record to determine difficulty
        QuestEventDifficulty difficulty;
        ConquestPreferences prefs = FModel.getConquestPreferences();
        int chaosBattleWins = FModel.getConquest().getModel().getChaosBattleRecord().getWins();
        if (chaosBattleWins < prefs.getPrefInt(CQPref.CHAOS_BATTLE_WINS_MEDIUMAI)) {
            difficulty = QuestEventDifficulty.EASY;
        }
        else if (chaosBattleWins < prefs.getPrefInt(CQPref.CHAOS_BATTLE_WINS_HARDAI)) {
            difficulty = QuestEventDifficulty.MEDIUM;
        }
        else if (chaosBattleWins < prefs.getPrefInt(CQPref.CHAOS_BATTLE_WINS_EXPERTAI)) {
            difficulty = QuestEventDifficulty.HARD;
        }
        else {
            difficulty = QuestEventDifficulty.EXPERT;
        }

        world = Aggregates.random(FModel.getWorlds());
        String path = world == null || world.getDuelsDir() == null ? ForgeConstants.DEFAULT_DUELS_DIR : ForgeConstants.QUEST_WORLD_DIR + world.getDuelsDir();
        QuestEventDuelManager duelManager = new QuestEventDuelManager(new File(path));
        duel = Aggregates.random(duelManager.getDuels(difficulty));
    }

    @Override
    protected Deck buildOpponentDeck() {
        return duel.getEventDeck();
    }

    @Override
    public void addVariants(Set<GameType> variants) {
    }

    @Override
    public String getEventName() {
        return duel.getTitle();
    }

    @Override
    public String getOpponentName() {
        return duel.getTitle();
    }

    @Override
    public String getAvatarImageKey() {
        return duel.getIconImageKey();
    }

    @Override
    public int gamesPerMatch() {
        return 3; //play best 2 out of 3 for Chaos battles
    }

    @Override
    public void showGameOutcome(final ConquestData model, final GameView game, final LobbyPlayer humanPlayer, final IWinLoseView<? extends IButton> view) {
        if (game.isMatchOver()) {
            view.getBtnContinue().setVisible(false);
            if (game.isMatchWonBy(humanPlayer)) {
                view.getBtnQuit().setText("Great!");
                model.getChaosBattleRecord().addWin();
            }
            else {
                view.getBtnQuit().setText("OK");
                model.getChaosBattleRecord().addLoss();
            }
            model.saveData();
        }
        else {
            view.getBtnContinue().setVisible(true);
            view.getBtnContinue().setText("Continue");
            view.getBtnQuit().setText("Quit");
        }
    }

    @Override
    public void onFinished(final ConquestData model, IWinLoseView<? extends IButton> view) {
        if (view.getBtnContinue().isVisible()) {
            //ensure loss saved if you quit the battle before the match is over
            model.getChaosBattleRecord().addLoss();
            model.saveData();
        }
    }
}
