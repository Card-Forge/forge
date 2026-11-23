package forge.screens.match.winlose;

import forge.game.GameView;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.RogueWinLoseController;
import forge.gamemodes.rogue.flow.RogueFlow;
import forge.gui.FThreads;
import forge.gui.interfaces.IWinLoseView;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.screens.match.MatchController;
import forge.screens.rogue.RogueController;
import forge.toolbox.FButton;
import forge.util.Localizer;
import java.util.List;

/**
 * Rogue Commander's result: the flow decides rewards on its own thread, then the title and the way on are set.
 * Next Game and New Match are not offered. Until the flow has finished, Quit returns to the reward question waiting.
 */
public class RogueWinLose extends ControlWinLose {
    private volatile RogueWinLoseController controller;
    private volatile boolean failed;

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
        RogueController.get().queue(() -> {
            try {
                controller = flow().matchOver(lastGame, new InlineView(view));
            } catch (RuntimeException e) {
                failed = true;
                throw e;
            } finally {
                FThreads.invokeInEdtLater(this::showWayOn);
            }
        });
    }

    private void showWayOn() {
        ViewWinLose view = getView();
        if (controller == null) {
            view.getBtnQuit().setEnabled(true);
            return;
        }
        Localizer localizer = Localizer.getInstance();
        view.setTitle(flow().resultTitle(controller));
        if (flow().offersEnding(controller)) {
            boolean unlocked = RogueMetaProgress.getInstance().isEndlessModeUnlocked();
            view.getBtnContinue().setText(localizer.getMessage("lblRogueCompleteRun"));
            view.getBtnContinue().setVisible(true);
            view.getBtnContinue().setEnabled(true);
            view.getBtnQuit().setText(localizer.getMessage("lblRogueGoEndless"));
            view.getBtnQuit().setEnabled(unlocked);
            if (!unlocked) {
                view.setHint(localizer.getMessage("lblRogueUnlockEndlessHint").replace("\n", " "));
            }
        } else {
            view.getBtnQuit().setEnabled(true);
        }
        view.revalidate();
    }

    @Override
    public void actionOnContinue() {
        if (controller != null && flow().offersEnding(controller)) {
            leave(false);
        }
    }

    @Override
    public void actionOnQuit() {
        if (failed) {
            failed = false;
            MatchController.getHostedMatch().endCurrentGame();
            getView().hide();
            RogueController.backToRuns();
            return;
        }
        if (controller == null) {
            RogueController.get().reshowOpenQuestion();
            return;
        }
        leave(flow().offersEnding(controller));
    }

    /** The match closes before the flow goes on, so whatever it shows next is not hidden under the match. */
    private void leave(boolean endless) {
        RogueWinLoseController result = controller;
        controller = null;
        saveOptions();
        MatchController.getHostedMatch().endCurrentGame();
        getView().hide();
        RogueController.get().queue(() -> flow().resultLeft(result, endless));
    }

    /** Runs the reward script inline on the flow thread, where its questions can wait for their answers. */
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

        @Override
        public void showCards(String title, List<PaperCard> cards) {
            view.showCards(title, cards);
        }

        @Override
        public void showMessage(String message, String title, FSkinProp icon) {
            view.showMessage(message, title, icon);
        }
    }
}
