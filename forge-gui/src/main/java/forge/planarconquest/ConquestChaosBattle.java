package forge.planarconquest;

import java.io.File;
import java.util.EnumSet;
import java.util.Set;

import forge.LobbyPlayer;
import forge.deck.Deck;
import forge.game.GameType;
import forge.game.GameView;
import forge.interfaces.IButton;
import forge.interfaces.IGuiGame;
import forge.interfaces.IWinLoseView;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.planarconquest.ConquestPreferences.CQPref;
import forge.properties.ForgeConstants;
import forge.quest.QuestEventDifficulty;
import forge.quest.QuestEventDuel;
import forge.quest.QuestEventDuelManager;
import forge.quest.QuestWorld;
import forge.util.Aggregates;

public class ConquestChaosBattle extends ConquestBattle {
    private final QuestWorld world;
    private final QuestEventDuel duel;
    private ConquestAwardPool awardPool;

    public ConquestChaosBattle() {
        super(null, 0);

        //use a random quest event duel for Chaos battle
        //using Chaos battle record to determine difficulty
        QuestEventDifficulty prefferedDifficulty;
        ConquestPreferences prefs = FModel.getConquestPreferences();
        int chaosBattleWins = FModel.getConquest().getModel().getChaosBattleRecord().getWins();
        if (chaosBattleWins < prefs.getPrefInt(CQPref.CHAOS_BATTLE_WINS_MEDIUMAI)) {
            prefferedDifficulty = QuestEventDifficulty.EASY;
        }
        else if (chaosBattleWins < prefs.getPrefInt(CQPref.CHAOS_BATTLE_WINS_HARDAI)) {
            prefferedDifficulty = QuestEventDifficulty.MEDIUM;
        }
        else if (chaosBattleWins < prefs.getPrefInt(CQPref.CHAOS_BATTLE_WINS_EXPERTAI)) {
            prefferedDifficulty = QuestEventDifficulty.HARD;
        }
        else {
            prefferedDifficulty = QuestEventDifficulty.EXPERT;
        }

        QuestWorld world0 = null;
        QuestEventDuel duel0 = null;

        //loop until we find a duel
        do {
            world0 = Aggregates.random(FModel.getWorlds());
            String path = world0 == null || world0.getDuelsDir() == null ? ForgeConstants.DEFAULT_DUELS_DIR : ForgeConstants.QUEST_WORLD_DIR + world0.getDuelsDir();
            QuestEventDuelManager duelManager = new QuestEventDuelManager(new File(path));
            QuestEventDifficulty difficulty = prefferedDifficulty;
            duel0 = Aggregates.random(duelManager.getDuels(difficulty));

            //if can't find duel at preferred difficulty, try lower difficulty
            while (duel0 == null && difficulty != QuestEventDifficulty.EASY) {
                switch (difficulty) {
                case EXPERT:
                    difficulty = QuestEventDifficulty.HARD;
                    break;
                case HARD:
                    difficulty = QuestEventDifficulty.MEDIUM;
                    break;
                case MEDIUM:
                    difficulty = QuestEventDifficulty.EASY;
                    break;
                default:
                    continue;
                }
                duel0 = Aggregates.random(duelManager.getDuels(difficulty));
            }
        } while (duel0 == null);

        world = world0;
        duel = duel0;
    }

    @Override
    protected Deck buildOpponentDeck() {
        return duel.getEventDeck();
    }

    @Override
    public String getEventName() {
        return duel.getTitle();
    }

    @Override
    public String getOpponentName() {
        return duel.getName();
    }

    @Override
    public PaperCard getPlaneswalker() {
        return null;
    }

    @Override
    public void setOpponentAvatar(LobbyPlayer aiPlayer, IGuiGame gui) {
        gui.setPlayerAvatar(aiPlayer, duel);
    }

    @Override
    public Set<GameType> getVariants() {
        return EnumSet.noneOf(GameType.class);
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
                setConquered(true);
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
        super.onFinished(model, view);
    }

    public ConquestAwardPool getAwardPool() {
        if (awardPool == null) { //delay initializing until needed
            awardPool = new ConquestAwardPool(world.getAllCards());
        }
        return awardPool;
    }
}
