package forge.screens.match.winlose;

import forge.LobbyPlayer;
import forge.game.GameView;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.RogueWinLoseController;
import forge.gamemodes.rogue.effect.RogueEffectComposite;
import forge.gamemodes.rogue.flow.RogueFlow;
import forge.gui.FThreads;
import forge.gui.interfaces.IWinLoseView;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.player.GamePlayerUtil;
import forge.screens.match.MatchController;
import forge.screens.rogue.RogueController;
import forge.screens.rogue.RogueQuestion;
import forge.screens.rogue.RogueSpoils;
import forge.screens.rogue.RogueSpoilsScreen;
import forge.toolbox.FButton;
import forge.util.Localizer;
import java.util.List;

/**
 * Rogue Commander's result: only whether the battle was won, and one way on. The match closes before the rewards are
 * worked out, so the reward screens show over the trail, and they list each spoil in a ledger rather than in a message.
 * Next Game and New Match are not offered.
 */
public class RogueWinLose extends ControlWinLose {
    private boolean left;

    public RogueWinLose(final ViewWinLose view, final GameView lastGame) {
        super(view, lastGame);
        view.getBtnContinue().setVisible(false);
        view.getBtnRestart().setVisible(false);
    }

    private static RogueFlow flow() {
        return RogueController.get().flow();
    }

    @Override
    public void showRewards() {
        ViewWinLose view = getView();
        Localizer localizer = Localizer.getInstance();
        LobbyPlayer human = GamePlayerUtil.getGuiPlayer();
        boolean won = lastGame.getOutcome() != null ? lastGame.getOutcome().isWinner(human) : lastGame.isMatchWonBy(human);
        view.setTitle(localizer.getMessage(won ? "lblRogueBattleWon" : "lblRogueBattleLost"));
        // A loss ends the run unless an effect such as the Continuum Engine revives the player
        RogueRun run = flow().run();
        boolean ends = !won && run != null && !RogueEffectComposite.INSTANCE.wouldRevive(run);
        view.getBtnQuit().setText(localizer.getMessage(ends ? "lblRogueEndRun" : "lblRogueContinueRun"));
        view.getBtnQuit().setEnabled(true);
        view.revalidate();
    }

    @Override
    public void actionOnContinue() {
    }

    @Override
    public void actionOnQuit() {
        if (left) {
            return;
        }
        left = true;
        ViewWinLose view = getView();
        saveOptions();
        MatchController.getHostedMatch().endCurrentGame();
        view.hide();
        RogueController.get().queue(() -> {
            RogueSpoils.start();
            RogueWinLoseController result = flow().matchOver(lastGame, new InlineView(view));
            boolean ending = flow().offersEnding(result);
            boolean endless = false;
            if (ending || RogueSpoils.unseen()) {
                String title = flow().resultTitle(result);
                endless = RogueQuestion.ask(q -> new RogueSpoilsScreen(flow().run(), title, ending, q));
            }
            flow().resultLeft(result, endless);
            RogueSpoils.start();
        });
    }

    /** Runs the reward script inline on the flow thread, gathering each message as a spoil for the reward screens. */
    private static final class InlineView implements IWinLoseView<FButton> {
        private final ViewWinLose view;

        InlineView(ViewWinLose view) {
            this.view = view;
        }

        @Override
        public FButton getBtnContinue() {
            return view.getBtnContinue();
        }

        @Override
        public FButton getBtnRestart() {
            return view.getBtnRestart();
        }

        @Override
        public FButton getBtnQuit() {
            return view.getBtnQuit();
        }

        @Override
        public void hide() {
            FThreads.invokeInEdtLater(view::hide);
        }

        @Override
        public void showRewards(Runnable runnable) {
            runnable.run();
        }

        /** The cards just picked, which the player has already seen. */
        @Override
        public void showCards(String title, List<PaperCard> cards) {
        }

        @Override
        public void showMessage(String message, String title, FSkinProp icon) {
            RogueSpoils.add(message, title, icon);
        }
    }
}
