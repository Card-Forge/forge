package forge.gamemodes.rogue;

import forge.LobbyPlayer;
import forge.deck.Deck;
import forge.game.GameView;
import forge.game.player.PlayerView;
import forge.gui.interfaces.IButton;
import forge.gui.interfaces.IWinLoseView;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.player.GamePlayerUtil;
import forge.util.Localizer;

import java.util.List;

/**
 * Controller for Rogue Commander win/lose screen.
 * Handles reward logic after matches.
 */
public class RogueWinLoseController {
    private final GameView lastGame;
    private final IWinLoseView<? extends IButton> view;
    private final boolean wonMatch;
    private final RogueRunData currentRun;

    public RogueWinLoseController(final GameView game0, final IWinLoseView<? extends IButton> view0, final RogueRunData currentRun0) {
        this.lastGame = game0;
        this.view = view0;
        this.currentRun = currentRun0;

        // Determine if player won
        final LobbyPlayer humanLobbyPlayer = GamePlayerUtil.getGuiPlayer();
        this.wonMatch = lastGame.isMatchWonBy(humanLobbyPlayer);
    }

    public void showRewards() {
        view.getBtnRestart().setVisible(false);

        final boolean matchIsNotOver = !lastGame.isMatchOver();
        if (matchIsNotOver) {
            view.getBtnQuit().setText(Localizer.getInstance().getMessage("lblQuit"));
        } else {
            view.getBtnContinue().setVisible(false);
            if (wonMatch) {
                view.getBtnQuit().setText(Localizer.getInstance().getMessage("lblGreat") + "!");
            } else {
                view.getBtnQuit().setText(Localizer.getInstance().getMessage("lblOK"));
            }
        }

        // Show rewards on a separate thread
        view.showRewards(() -> {
            if (matchIsNotOver) {
                return; // Skip logic if match isn't over yet
            }

            if (wonMatch) {
                handleVictory();
            } else {
                handleDefeat();
            }
        });
    }

    private void handleVictory() {
        if (currentRun == null) {
            System.err.println("Warning: No current run found in RogueWinLoseController");
            return;
        }

        // Persist life total from match
        persistLifeTotal();

        // Mark current node as completed and award gold/echo rewards
        NodeData currentNode = currentRun.getCurrentNode();
        if (currentNode != null) {
            currentNode.setCompleted(true);

            // Award gold and echo rewards based on node type
            int goldReward = currentNode.getType().getGoldReward();
            int echoReward = currentNode.getType().getEchoReward();
            currentRun.setCurrentGold(currentRun.getCurrentGold() + goldReward);
            currentRun.setCurrentEchoes(currentRun.getCurrentEchoes() + echoReward);
        }

        // Award card rewards
        awardCardRewards(currentNode);

        // Move to next node
        currentRun.nextNode();

        // Save run
        RogueIO.saveRun(currentRun);
    }

    private void persistLifeTotal() {
        // Get player's life total at end of match
        final LobbyPlayer humanLobbyPlayer = GamePlayerUtil.getGuiPlayer();
        PlayerView humanPlayer = null;
        for (final PlayerView p : lastGame.getPlayers()) {
            if (p.isLobbyPlayer(humanLobbyPlayer)) {
                humanPlayer = p;
                break;
            }
        }

        if (humanPlayer != null) {
            int endingLife = humanPlayer.getLife();
            currentRun.setCurrentLife(endingLife);
        }
    }

    private void awardCardRewards(NodeData currentNode) {
        // Get the rogue deck data to draw rewards from
        RogueDeckData rogueDeck = currentRun.getSelectedRogueDeck();

        if (rogueDeck == null) {
            System.err.println("Warning: Could not find rogue deck for current run.");
            return;
        }

        // Draw 7 random cards from reward pool
        List<PaperCard> rewardOptions = rogueDeck.drawRewardOptions(7);

        if (rewardOptions.isEmpty()) {
            view.showMessage("No more cards available in reward pool.", "No Rewards", FSkinProp.ADV_CLR_ACTIVE);
            return;
        }

        // Get rewards earned from current node
        int goldReward = 0;
        int echoReward = 0;
        if (currentNode != null) {
            goldReward = currentNode.getType().getGoldReward();
            echoReward = currentNode.getType().getEchoReward();
        }

        // Show visual card selection dialog
        List<PaperCard> chosenCards = view.showCardRewardDialog(
            "Choose Your Rewards",
            rewardOptions,
            0,
            3,
            goldReward,
            echoReward
        );

        if (chosenCards != null && !chosenCards.isEmpty()) {
            // Add chosen cards to the run's current deck
            Deck currentDeck = currentRun.getCurrentDeck();
            for (PaperCard card : chosenCards) {
                currentDeck.getMain().add(card);
            }

            // Show confirmation
            view.showCards("Cards Added to Your Deck", chosenCards);

            // Remove reward options (both chosen and unchosen) from the reward pool
            rogueDeck.removeFromRewardPool(rewardOptions);
        }
    }

    private void handleDefeat() {
        if (currentRun == null) {
            return;
        }

        // TODO: Mark run as failed (add setRunActive method to RogueRunData later)
        // For now, the run remains in progress and player can retry

        // Save run state
        RogueIO.saveRun(currentRun);

        view.showMessage("You were defeated! Return to the map to try again.", "Defeat", FSkinProp.ADV_CLR_ACTIVE);
    }

    public void actionOnQuit() {
        // Any cleanup needed before quitting
        // Currently handled by RogueWinLose.actionOnQuit()
    }
}
