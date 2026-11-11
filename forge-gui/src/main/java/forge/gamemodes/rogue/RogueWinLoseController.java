package forge.gamemodes.rogue;

import com.google.common.collect.ImmutableList;
import forge.LobbyPlayer;
import forge.game.Game;
import forge.game.GameView;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gamemodes.rogue.RogueRun.CarryCard;
import forge.gamemodes.rogue.RogueRun.CarryCardType;
import forge.gamemodes.rogue.effect.DefeatContext;
import forge.gamemodes.rogue.effect.MatchRewardContext;
import forge.gamemodes.rogue.effect.RogueEffectComposite;
import forge.gamemodes.rogue.path.NodeEvent;
import forge.gamemodes.rogue.path.NodePlanebound;
import forge.gamemodes.rogue.path.RoguePathNode;
import forge.gui.interfaces.IButton;
import forge.gui.interfaces.IWinLoseView;
import forge.item.PaperCard;
import forge.util.Localizer;
import forge.localinstance.achievements.RogueCommanderAchievements;
import forge.localinstance.skin.FSkinProp;
import forge.player.GamePlayerUtil;
import java.util.ArrayList;
import java.util.List;
import org.tinylog.Logger;

/**
 * Controller for Rogue Commander win/lose screen.
 * Handles reward logic after matches.
 */
public class RogueWinLoseController {
    private final Localizer localizer = Localizer.getInstance();

    private final GameView lastGame;
    private final IWinLoseView<? extends IButton> view;
    private final CardRewardHelper.RewardDialog rewardDialog;
    private final boolean wonMatch;
    private final RogueRun currentRun;

    // Carry cards lost during the match (set by handleMatchData, shown by both victory/defeat)
    private List<CarryCard> lostCarryCards = ImmutableList.of();

    public RogueWinLoseController(final GameView game0, final IWinLoseView<? extends IButton> view0,
                                 final RogueRun currentRun0, final CardRewardHelper.RewardDialog rewardDialog0) {
        this.lastGame = game0;
        this.view = view0;
        this.rewardDialog = rewardDialog0;
        this.currentRun = currentRun0;

        // Determine if player won using GameOutcome (more reliable than isMatchWonBy)
        final LobbyPlayer humanLobbyPlayer = GamePlayerUtil.getGuiPlayer();
        if (lastGame.getOutcome() != null) {
            this.wonMatch = lastGame.getOutcome().isWinner(humanLobbyPlayer);
        } else {
            // Fallback if GameOutcome is null (shouldn't happen)
            this.wonMatch = lastGame.isMatchWonBy(humanLobbyPlayer);
        }
    }

    public void showRewards() {
        view.getBtnRestart().setVisible(false);

        // Setup buttons - we're in the win/lose screen, so match is effectively over
        view.getBtnContinue().setVisible(false);
        if (wonMatch) {
            view.getBtnQuit().setText(localizer.getMessage("lblRogueContinueRun"));
        } else {
            view.getBtnQuit().setText(localizer.getMessage("lblRogueEndRun"));
        }

        // Show rewards on a separate thread
        view.showRewards(() -> {
            if (wonMatch) {
                handleMatchVictory();
            } else {
                handleMatchDefeat();
            }
        });
    }

    public boolean hasWonMatch() {
        return wonMatch;
    }

    public boolean hasWonRun() {
        return wonMatch && currentRun != null && currentRun.isRunComplete();
    }

    public boolean hasFailedRun() {
        return currentRun != null && currentRun.isRunFailed();
    }

    private void handleMatchVictory() {
        if (currentRun == null) {
            Logger.error("Cannot record Rogue match victory without an active run");
            return;
        }

        // Record the victory (this also marks the node as completed)
        currentRun.recordMatchResult(true);

        // Persist life total and check carry card survival
        handleMatchData();

        // Check if this was the last node (run completed)
        boolean isLastNode = currentRun.getCurrentNodeIndex() >= currentRun.getPath().getNodeCount() - 1;

        // Apply match win effects (healing, etc.) — not for Boss/last node
        if (!isLastNode) {
            int lifeBefore = currentRun.getCurrentLife();
            RogueEffectComposite.INSTANCE.onMatchWin(currentRun);
            int gainedLife = currentRun.getCurrentLife() - lifeBefore;
            if (gainedLife > 0) {
                view.showMessage(localizer.getMessage("lblRogueGainedLife", gainedLife), localizer.getMessage("lblRogueEffect"), FSkinProp.ICO_QUEST_CHARM);
            }

            // Won the match but life is still <= 0 (e.g. "can't lose the game" effect) — run is lost
            if (currentRun.getCurrentLife() <= 0) {
                recordMatchStats(RogueMetaProgress.getInstance(), true);
                handleRunDefeat("being unable to continue the run at 0 or less life");
                return;
            }
        }

        // Get current node for rewards (applies to ALL nodes including Boss)
        RoguePathNode currentNode = currentRun.getCurrentNode();

        // Track gold and echo rewards for showing messages later
        int goldReward = 0;
        int echoReward = 0;

        var progress = RogueMetaProgress.getInstance();

        // Check if rewards should be skipped (e.g. Distortion effect)
        MatchRewardContext rewardCtx = new MatchRewardContext();
        RogueEffectComposite.INSTANCE.onBeforeRewards(rewardCtx, currentRun);

        NodePlanebound planeboundNode = resolvePlanebound(currentNode);

        // Award gold and echo rewards for ALL planebound nodes (including Boss)
        if (planeboundNode != null && !rewardCtx.skipRewards) {
            goldReward = Math.max(0, planeboundNode.getGoldReward() + rewardCtx.goldRewardAdjustment);
            echoReward = planeboundNode.getEchoReward();

            // Gold is run-specific (spent at Bazaar during the run)
            currentRun.addGold(goldReward);

            // Echoes are meta-progression currency - add directly to meta progress
            if (echoReward > 0) {
                progress.addEchoes(echoReward);
            }
        }

        if (currentRun.isRunComplete()) {
            handleTrailVictory(echoReward);
            return;
        }

        // Award card rewards (only for non-final nodes, skip if distortion)
        if (planeboundNode != null && !rewardCtx.skipRewards) {
            boolean isElite = planeboundNode.getPlaneboundType() == RoguePlaneboundType.ELITE;
            awardCardRewards(isElite, goldReward, echoReward, rewardCtx);
        }

        // Show lost carry card message
        showLostCarryCards();

        // Track meta progress for match
        recordMatchStats(progress, true);

        // Evaluate run-level achievements after rewards
        RogueCommanderAchievements.instance.evaluateRunAchievements(currentRun);

        // Move to next node
        currentRun.nextNode();

        // Save run
        RogueIO.saveRun(currentRun);
    }

    private void handleTrailVictory(int echoReward) {
        if (echoReward > 0) {
            view.showMessage(localizer.getMessage("lblRogueEchoesWon", echoReward), localizer.getMessage("lblRogueEchoReward"), FSkinProp.ICO_QUEST_GOLD);
        }

        RogueMetaProgress progress = RogueMetaProgress.getInstance();
        recordMatchStats(progress, true);
        if (currentRun.getCurrentLife() <= 0) {
            handleRunDefeat("being unable to continue the run at 0 or less life");
            return;
        }
        progress.recordEndlessLevelCompleted(currentRun);
        RogueCommanderAchievements.instance.recordEndlessLevelWon(currentRun.getEndlessLevel());
        if (currentRun.isCustomRun() && currentRun.getEndlessLevel() == 0) {
            progress.unlockEndlessMode();
        }
        view.getBtnContinue().setText(localizer.getMessage("lblRogueCompleteRun"));
    }

    public static boolean finalizeRunVictory(RogueRun run, String defeatedBy) {
        if (run == null || run.isRunWon()) {
            return false;
        }
        run.setRunWon(true);
        run.getRunTimer().stop();

        // Record run history - find boss name from last NodePlanebound (BOSS type)
        String bossName = "";
        for (RoguePathNode node : run.getPath().getNodes()) {
            if (node instanceof NodePlanebound pb && pb.getPlaneboundType() == RoguePlaneboundType.BOSS) {
                bossName = pb.getRoguePlanebound().planeboundName();
            }
        }

        var progress = RogueMetaProgress.getInstance();
        progress.addRunHistoryEntry(RogueRunHistoryEntry.fromRun(run,
            defeatedBy == null ? "VICTORY" : "DEFEAT", defeatedBy == null ? bossName : defeatedBy));

        RogueStats.fireOnRunCompleted(run, progress, true);

        RogueCommanderAchievements.instance.recordRunWon(
            run.getCurrentCommanderName());

        boolean sparkAwarded = progress.recordDescensionWin(run);
        RogueCommanderAchievements.instance.evaluateRunAchievements(run);
        progress.notifyDescensionL1IfFirstWin(run);

        RogueIO.saveRun(run);
        return sparkAwarded;
    }

    private void awardCardRewards(boolean isElite, int goldReward, int echoReward, MatchRewardContext rewardCtx) {
        List<PaperCard> chosenCards = CardRewardHelper.runReward(currentRun,
                rewardDialog, false, rewardCtx, null);

        // If Elite opponent, show second reward screen with mythic cards
        if (isElite) {
            List<PaperCard> chosenMythics = CardRewardHelper.runReward(currentRun,
                    rewardDialog, true, rewardCtx, null);
            chosenCards.addAll(chosenMythics);
        }

        if (!chosenCards.isEmpty()) {
            view.showCards(localizer.getMessage("lblRogueCardsAdded"), chosenCards);
        }

        if (goldReward > 0) {
            view.showMessage(localizer.getMessage("lblRogueGoldWon", goldReward), localizer.getMessage("lblRogueGoldReward"), FSkinProp.ICO_QUEST_COIN);
        }

        if (echoReward > 0) {
            view.showMessage(localizer.getMessage("lblRogueEchoesWon", echoReward), localizer.getMessage("lblRogueEchoReward"), FSkinProp.ICO_QUEST_GOLD);
        }
    }

    private void handleMatchDefeat() {
        if (currentRun == null) {
            return;
        }

        // Record the match loss (node stays incomplete for retry)
        currentRun.recordMatchResult(false);

        // Persist life total and check carry card survival
        handleMatchData();

        // Check revive effects (e.g. Continuum Engine) BEFORE marking run as failed
        DefeatContext defeatCtx = new DefeatContext();
        RogueEffectComposite.INSTANCE.onDefeat(defeatCtx, currentRun);
        if (defeatCtx.revived) {
            currentRun.setCurrentLife(defeatCtx.reviveLife);
            recordMatchStats(RogueMetaProgress.getInstance(), false);
            RogueIO.saveRun(currentRun);
            view.getBtnQuit().setText(localizer.getMessage("lblRogueContinueRun"));
            view.showMessage(localizer.getMessage("lblRogueContinuumRevive", defeatCtx.reviveLife),
                localizer.getMessage("lblRogueContinuumEngine"), FSkinProp.ICO_QUEST_ELIXIR);
            return;
        }

        recordMatchStats(RogueMetaProgress.getInstance(), false);
        handleRunDefeat(getDefeatedByCurrentNode(currentRun));
    }

    private void handleRunDefeat(String defeatedBy) {
        if (finalizeRunDefeat(currentRun, defeatedBy)) {
            view.showMessage(localizer.getMessage("lblRogueSparkWon"), localizer.getMessage("lblRogueSparkReward"), FSkinProp.ICO_QUEST_ELIXIR);
        }
        view.getBtnQuit().setText(localizer.getMessage("lblRogueEndRun"));
    }

    public static boolean finalizeRunDefeat(RogueRun run, String defeatedBy) {
        if (run == null) {
            return false;
        }
        if (run.getEndlessLevel() > 0) {
            return finalizeRunVictory(run, defeatedBy != null ? defeatedBy : "");
        }
        run.setRunFailed(true);
        run.getRunTimer().stop();

        var progress = RogueMetaProgress.getInstance();
        progress.addRunHistoryEntry(RogueRunHistoryEntry.fromRun(run, "DEFEAT",
                defeatedBy != null ? defeatedBy : ""));

        RogueStats.fireOnRunCompleted(run, progress, false);
        RogueCommanderAchievements.instance.evaluateRunAchievements(run);

        RogueIO.saveRun(run);
        return false;
    }

    private static String getDefeatedByCurrentNode(RogueRun run) {
        String defeatedBy = "";
        RoguePathNode curNode = run.getCurrentNode();
        if (curNode instanceof NodePlanebound pb && pb.getRoguePlanebound() != null) {
            defeatedBy = pb.getRoguePlanebound().planeboundName();
        } else if (curNode instanceof NodeEvent ev && ev.getEventPlanebound() != null) {
            defeatedBy = ev.getEventPlanebound().planeboundName();
        }
        return defeatedBy;
    }

    private NodePlanebound resolvePlanebound(RoguePathNode node) {
        if (node instanceof NodePlanebound pb) {
            return pb;
        } else if (node instanceof NodeEvent ev && ev.getEventPlanebound() != null) {
            NodePlanebound pb = new NodePlanebound(ev.getEventPlanebound());
            pb.setRowIndex(node.getRowIndex());
            return pb;
        }
        return null;
    }

    private void recordMatchStats(RogueMetaProgress progress, boolean won) {
        NodePlanebound planeboundNode = resolvePlanebound(currentRun.getCurrentNode());
        if (planeboundNode != null && planeboundNode.getRoguePlanebound() != null) {
            progress.recordPlaneboundResult(planeboundNode.getRoguePlanebound(), won);
        }
        RogueStats.fireOnMatchCompleted(currentRun, progress, won);
    }

    private void showLostCarryCards() {
        if (lostCarryCards.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lostCarryCards.size(); i++) {
            if (i > 0) sb.append(", ");
            RogueRun.CarryCard card = lostCarryCards.get(i);
            sb.append(card.cardName())
              .append(" (")
              .append(getCarryCardTypeLabel(card.type()))
              .append(")");
        }
        view.showMessage(localizer.getMessage("lblRogueYouLost", sb.toString()), localizer.getMessage("lblRogueLostCarryCards"), FSkinProp.ICO_QUEST_MINUS);
    }

    private String getCarryCardTypeLabel(CarryCardType type) {
        return switch (type) {
            case ITEM -> localizer.getMessage("lblRogueItem");
            case FELLOW -> localizer.getMessage("lblRogueFellow");
            case SCROLL -> localizer.getMessage("lblRogueScroll");
        };
    }

    private void handleMatchData() {
        Game game = lastGame.getGame();
        if (game == null || currentRun == null) return;
        final LobbyPlayer humanLobbyPlayer = GamePlayerUtil.getGuiPlayer();
        NodePlanebound planeboundNode = resolvePlanebound(currentRun.getCurrentNode());
        Player humanPlayer = null;
        List<Player> planeboundPlayers = new ArrayList<>();
        for (Player p : game.getRegisteredPlayers()) {
            if (p.getLobbyPlayer() == humanLobbyPlayer) {
                humanPlayer = p;
                currentRun.persistMatchLife(p.getLife());
                checkCarryCardSurvival(p);
            } else {
                planeboundPlayers.add(p);
            }
        }
        if (planeboundNode != null && humanPlayer != null) {
            CodexHelper.recordPlaneboundPublicCardsOwnedBy(planeboundNode.getRoguePlanebound(),
                game.getRegisteredPlayers(), planeboundPlayers);
        }
    }

    private void checkCarryCardSurvival(Player human) {
        List<CarryCard> lost = new ArrayList<>();
        for (CarryCard card : currentRun.getCarryCards()) {
            boolean survived = hasCarryCardSurvived(human, card);
            if (!survived) lost.add(card);
        }
        for (CarryCard card : lost) {
            currentRun.removeCarryCard(card.cardName());
        }
        this.lostCarryCards = lost;
    }

    private static boolean hasCarryCardSurvived(Player human, CarryCard card) {
        boolean inCommandZone = human.getZone(ZoneType.Command).getCards().stream()
            .anyMatch(c -> c.getName().equals(card.cardName()));
        return inCommandZone
            || human.getZone(ZoneType.Battlefield).getCards().stream()
                .anyMatch(c -> c.getName().equals(card.cardName()))
            || human.getCardsIn(ZoneType.Stack).stream()
                .anyMatch(c -> c.getName().equals(card.cardName()));
    }

    public void actionOnQuit() {
        // Any cleanup needed before quitting
        // Currently handled by RogueWinLose.actionOnQuit()
    }
}
