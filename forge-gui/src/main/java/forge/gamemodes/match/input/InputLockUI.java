package forge.gamemodes.match.input;

import forge.game.Game;
import forge.game.GameView;
import forge.game.card.Card;
import forge.game.phase.PhaseHandler;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbility;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.player.PlayerControllerHuman;
import forge.util.ITriggerEvent;
import forge.util.Localizer;
import forge.util.ThreadUtil;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class InputLockUI implements Input {
    private final AtomicInteger iCall = new AtomicInteger();

    private final InputQueue inputQueue;
    private final PlayerControllerHuman controller;
    public InputLockUI(final InputQueue inputQueue0, final PlayerControllerHuman controller) {
        inputQueue = inputQueue0;
        this.controller = controller;
    }

    @Override
    public PlayerView getOwner() {
        return null;
    }

    @Override
    public void showMessageInitial() {
        final int ixCall = 1 + iCall.getAndIncrement();
        ThreadUtil.delay(500, new InputUpdater(ixCall));
    }

    @Override
    public String toString() {
        return "lockUI";
    }

    private class InputUpdater implements Runnable {
        final int ixCall;

        public InputUpdater(final int idxCall) {
            ixCall = idxCall;
        }

        @Override
        public void run() {
            if ( ixCall != iCall.get() || !isActive()) {
                return;
            }
            FThreads.invokeInEdtLater(showMessageFromEdt);
        }
    }

    private final Runnable showMessageFromEdt = new Runnable() {
        @Override
        public void run() {
            controller.getGui().updateButtons(InputLockUI.this.getOwner(), "", "", false, false, false);
            showMessage(getWaitingMessage());
        }
    };

    /**
     * Get a descriptive waiting message.
     * In network games, shows which player we're waiting for.
     * In local games, shows the generic "Waiting for Actions" message.
     */
    private String getWaitingMessage() {
        Localizer localizer = Localizer.getInstance();

        // In network games, show who we're waiting for
        if (GuiBase.isNetworkplay()) {
            // First try: Get priority player from the local Game object (works on host)
            Player player = controller.getPlayer();
            if (player != null) {
                Game game = player.getGame();
                if (game != null && !game.isGameOver()) {
                    PhaseHandler ph = game.getPhaseHandler();
                    if (ph != null) {
                        Player priorityPlayer = ph.getPriorityPlayer();
                        if (priorityPlayer != null && priorityPlayer != player) {
                            // Show "Waiting for [Player Name]..."
                            return localizer.getMessage("lblWaitingForPlayer", priorityPlayer.getName());
                        }
                    }
                }
            }

            // Fallback: Get priority player from the GameView (works on client)
            // On the network client, the Game object is on the server, but GameView is synced
            IGuiGame gui = controller.getGui();
            if (gui != null) {
                GameView gameView = gui.getGameView();
                if (gameView != null && !gameView.isGameOver()) {
                    PlayerView priorityPlayer = findPriorityPlayer(gameView);
                    // Show the waiting message if priority player exists and is different from our player
                    PlayerView localPlayer = controller.getLocalPlayerView();
                    if (priorityPlayer != null && (localPlayer == null || priorityPlayer.getId() != localPlayer.getId())) {
                        return localizer.getMessage("lblWaitingForPlayer", priorityPlayer.getName());
                    }
                }
            }
        }

        // Default message for local games or when player info not available
        return localizer.getMessage("lblWaitingforActions");
    }

    /**
     * Find the player with priority from the GameView.
     * Checks PlayerView.getHasPriority() for each player.
     * Falls back to getPlayerTurn() during game setup when no priority is set.
     */
    private PlayerView findPriorityPlayer(GameView gameView) {
        if (gameView.getPlayers() != null) {
            for (PlayerView pv : gameView.getPlayers()) {
                if (pv.getHasPriority()) {
                    return pv;
                }
            }
        }
        // Fallback to player turn during game setup (mulligan phase)
        return gameView.getPlayerTurn();
    }

    protected final boolean isActive() {
        return inputQueue.getInput() == this;
    }

    protected void showMessage(final String message) {
        controller.getGui().showPromptMessage(getOwner(), message);
    }

    @Override
    public boolean selectCard(final Card c, final List<Card> otherCardsToSelect, final ITriggerEvent triggerEvent) {
        return false;
    }
    @Override
    public boolean selectAbility(final SpellAbility ab) {
        return false;
    }
    @Override
    public void selectPlayer(final Player player, final ITriggerEvent triggerEvent) {
    }
    @Override
    public void selectButtonOK() {
    }
    @Override
    public void selectButtonCancel() {
        //cancel auto pass for all players
        for (final Player player : controller.getGame().getPlayers()) {
            player.getController().autoPassCancel();
        }
    }

    @Override
    public String getActivateAction(final Card card) {
        return null;
    }
}
