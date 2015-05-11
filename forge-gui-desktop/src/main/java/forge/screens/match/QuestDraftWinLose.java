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
package forge.screens.match;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.google.common.collect.ImmutableList;

import forge.assets.FSkinProp;
import forge.game.GameView;
import forge.match.NextGameDecision;
import forge.model.FModel;
import forge.quest.QuestController;
import forge.quest.QuestDraftUtils;
import forge.screens.home.quest.CSubmenuQuestDraft;
import forge.screens.home.quest.VSubmenuQuestDraft;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin;

/**
 * <p>
 * QuestWinLose.
 * </p>
 * Processes win/lose presentation for Quest events. This presentation is
 * displayed by WinLoseFrame. Components to be added to pnlCustom in
 * WinLoseFrame should use MigLayout.
 *
 */
public class QuestDraftWinLose extends ControlWinLose {
    private final transient ViewWinLose view;

    /**
     * Instantiates a new quest win lose handler.
     *
     * @param view0 ViewWinLose object
     * @param match2
     */
    public QuestDraftWinLose(final ViewWinLose view0, final GameView game0, final CMatchUI matchUI) {
        super(view0, game0, matchUI);
        this.view = view0;
    }

    /**
     * <p>
     * populateCustomPanel.
     * </p>
     * Checks conditions of win and fires various reward display methods
     * accordingly.
     *
     * @return true, if successful
     */
    @Override
    public final boolean populateCustomPanel() {
        final QuestController quest = FModel.getQuest();
        final boolean gameHadHumanPlayer = matchUI.hasLocalPlayers();

        if (lastGame.isMatchOver()) {
            final String winner = lastGame.getWinningPlayerName();

            quest.getAchievements().getCurrentDraft().setWinner(winner);
            quest.save();
        }

        if (!gameHadHumanPlayer) {
            QuestDraftUtils.matchInProgress = false;
            QuestDraftUtils.update(matchUI);
            return false;
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
        for (final ActionListener listener : view.getBtnQuit().getActionListeners()) {
            view.getBtnQuit().removeActionListener(listener);
        }
        view.getBtnQuit().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (warningString == null ||
                        FOptionPane.showOptionDialog(warningString, warningCaption, FSkin.getImage(FSkinProp.ICO_WARNING).scale(2), ImmutableList.of("Yes", "No"), 1) == 0) {
                    matchUI.getGameController().nextGameDecision(NextGameDecision.QUIT);
                    QuestDraftUtils.matchInProgress = false;
                    QuestDraftUtils.continueMatches(matchUI);
                }
            }
        });

        CSubmenuQuestDraft.SINGLETON_INSTANCE.update();
        VSubmenuQuestDraft.SINGLETON_INSTANCE.populate();

        return false; //We're not awarding anything, so never display the custom panel.
    }

}
