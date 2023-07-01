/** Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.screens.match.winlose;

import forge.game.GameView;
import forge.game.player.PlayerView;
import forge.gamemodes.match.NextGameDecision;
import forge.gamemodes.quest.QuestController;
import forge.gamemodes.quest.QuestDraftUtils;
import forge.model.FModel;
import forge.screens.match.MatchController;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;

/**
 * <p>
 * QuestDraftWinLose.
 * </p>
 * Processes win/lose presentation for Quest Draft events. This presentation is
 * displayed by WinLoseFrame. Components to be added to pnlCustom in
 * WinLoseFrame should use MigLayout.
 *
 */
public class QuestDraftWinLose extends ControlWinLose {
    private final transient ViewWinLose view;
    private final ControlWinLose controller;

    /**
     * Instantiates a new quest win lose handler.
     */
    public QuestDraftWinLose(final ViewWinLose view0, final GameView game0) {
        super(view0, game0);
        this.view = view0;
        controller = new ControlWinLose(view0, game0);
    }

    @Override
    public final void actionOnQuit() {
        controller.actionOnQuit();
        super.actionOnQuit();
    }

    @Override
    public final void showRewards(){
        final QuestController quest = FModel.getQuest();
        final boolean gameHadHumanPlayer = MatchController.instance.hasLocalPlayers();

        if (lastGame.isMatchOver()) {
            final String winner = lastGame.getWinningPlayerName();
            quest.getAchievements().getCurrentDraft().setWinner(winner);
            quest.save();
            // TODO: update needed here?
        }

        QuestDraftUtils.update(MatchController.instance);
        QuestDraftUtils.matchInProgress = false;
        if (!gameHadHumanPlayer) {
            return;
        }

        view.getBtnRestart().setEnabled(false);
        view.getBtnRestart().setVisible(false);

        final boolean isMatchOver = lastGame.isMatchOver();
        final String quitString, warningString, warningCaption;
        if (isMatchOver) {
            quitString = "Continue Tournament";
            warningString = null;
            warningCaption = null;
        } else {
            quitString = "Forfeit Tournament";
            warningString = "Quitting the match now will forfeit the tournament!\n\nReally quit?";
            warningCaption = "Really Quit Tournament?";
        }
        view.getBtnQuit().setEnabled(true);
        view.getBtnContinue().setEnabled(!isMatchOver);
        view.getBtnQuit().setText(quitString);

        view.getBtnQuit().setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                if (warningString == null /*||
                        FOptionPane.showOptionDialog(warningString, warningCaption, FSkin.getImages().get(FSkinProp.ICO_WARNING), ImmutableList.of("Yes", "No"), 1) == 0*/) {
                    if (warningString != null) {
                        PlayerView humanPlayer = null;
                        for (PlayerView playerView : MatchController.instance.getLocalPlayers()) {
                            humanPlayer = playerView;
                        }
                        for (PlayerView playerView : lastGame.getPlayers()) {
                            if (humanPlayer == null) {
                                throw new IllegalStateException("Forfeit tournament button was pressed in a match without human players.");
                            }
                            if (playerView != humanPlayer) {
                                quest.getAchievements().getCurrentDraft().setWinner(playerView.getName());
                                quest.save();
                                // TODO: update needed here?
                            }
                        }
                        //The player is probably not interested in watching more AI matches.
                        QuestDraftUtils.cancelFurtherMatches();
                    } else {
                        MatchController.instance.getGameController().nextGameDecision(NextGameDecision.QUIT);
                        controller.actionOnQuit();
                        QuestDraftUtils.matchInProgress = false;
                        QuestDraftUtils.continueMatches(MatchController.instance);
                    }
                }
            }
        });
    }
}

