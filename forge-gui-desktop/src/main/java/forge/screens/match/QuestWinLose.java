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

import java.awt.Dimension;
import java.util.List;

import javax.swing.SwingConstants;

import forge.GuiBase;
import forge.assets.FSkinProp;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.quest.QuestWinLoseController;
import forge.screens.home.quest.CSubmenuChallenges;
import forge.screens.home.quest.CSubmenuDuels;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.Colors;
import forge.toolbox.FSkin.SkinColor;
import forge.toolbox.FSkin.SkinIcon;
import forge.toolbox.FSkin.SkinnedLabel;
import forge.view.IGameView;

/**
 * <p>
 * QuestWinLose.
 * </p>
 * Processes win/lose presentation for Quest events. This presentation is
 * displayed by WinLoseFrame. Components to be added to pnlCustom in
 * WinLoseFrame should use MigLayout.
 * 
 */
public class QuestWinLose extends ControlWinLose {
    private final transient ViewWinLose view;
    private final QuestWinLoseController controller;

    /** String constraint parameters for title blocks and cardviewer blocks. */
    private static final SkinColor FORE_COLOR = FSkin.getColor(Colors.CLR_TEXT);
    private static final String CONSTRAINTS_TITLE = "w 95%!, gap 0 0 20px 10px";
    private static final String CONSTRAINTS_TEXT = "w 95%!, h 220px!, gap 0 0 0 20px";
    private static final String CONSTRAINTS_CARDS = "w 95%!, h 330px!, gap 0 0 0 20px";
    private static final String CONSTRAINTS_CARDS_LARGE = "w 95%!, h 600px!, gap 0 0 0 20px";

    /**
     * Instantiates a new quest win lose handler.
     * 
     * @param view0 ViewWinLose object
     * @param match2
     */
    public QuestWinLose(final ViewWinLose view0, final IGameView game0) {
        super(view0, game0);
        view = view0;
        controller = new QuestWinLoseController(game0, GuiBase.getInterface()) {
            @Override
            protected void showRewards(Runnable runnable) {
                runnable.run(); //just run on GUI thread
            }

            @Override
            protected void showCards(String title, List<PaperCard> cards) {
                final QuestWinLoseCardViewer cv = new QuestWinLoseCardViewer(cards);
                view.getPnlCustom().add(new TitleLabel(title), QuestWinLose.CONSTRAINTS_TITLE);
                if (FModel.getPreferences().getPrefBoolean(FPref.UI_LARGE_CARD_VIEWERS)) {
                    view.getPnlCustom().add(cv, QuestWinLose.CONSTRAINTS_CARDS_LARGE);
                }
                else {
                    view.getPnlCustom().add(cv, QuestWinLose.CONSTRAINTS_CARDS);
                }
            }

            @Override
            protected void showMessage(String message, String title, FSkinProp icon) {
                SkinIcon icoTemp = FSkin.getIcon(icon).scale(0.5);

                if (message.contains("\n")) { //ensure new line characters are encoded
                    message = "<html>" + message.replace("\n", "<br>") + "</html>";
                }
                SkinnedLabel lblMessage = new SkinnedLabel(message);
                lblMessage.setFont(FSkin.getFont(14));
                lblMessage.setForeground(FORE_COLOR);
                lblMessage.setHorizontalAlignment(SwingConstants.CENTER);
                lblMessage.setIconTextGap(50);
                lblMessage.setIcon(icoTemp);

                view.getPnlCustom().add(new TitleLabel(title), QuestWinLose.CONSTRAINTS_TITLE);
                view.getPnlCustom().add(lblMessage, QuestWinLose.CONSTRAINTS_TEXT);
            }
        };
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
        controller.showRewards(view);
        return true;
    }

    /**
     * <p>
     * actionOnQuit.
     * </p>
     * When "quit" button is pressed, this method adjusts quest data as
     * appropriate and saves.
     * 
     */
    @Override
    public final void actionOnQuit() {
        controller.actionOnQuit();
        CSubmenuDuels.SINGLETON_INSTANCE.update();
        CSubmenuChallenges.SINGLETON_INSTANCE.update();
        super.actionOnQuit();
    }

    /**
     * JLabel header between reward sections.
     * 
     */
    @SuppressWarnings("serial")
    private class TitleLabel extends SkinnedLabel {
        TitleLabel(final String msg) {
            super(msg);
            setFont(FSkin.getFont(16));
            setPreferredSize(new Dimension(200, 40));
            setHorizontalAlignment(SwingConstants.CENTER);
            setForeground(FORE_COLOR);
            setBorder(new FSkin.MatteSkinBorder(1, 0, 1, 0, FORE_COLOR));
        }
    }
}
