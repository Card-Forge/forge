package forge.gui.match;

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

import java.awt.Color;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.deck.Deck;
import forge.gauntlet.GauntletData;
import forge.gauntlet.GauntletIO;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FSkin;
import forge.model.FMatchState;
import forge.model.FModel;

/**
 * The Win/Lose handler for 'gauntlet' type tournament
 * games.
 */
public class OtherGauntletWinLose extends ControlWinLose {
    /**
     * Instantiates a new gauntlet win/lose handler.
     * 
     * @param view0 ViewWinLose object
     */
    public OtherGauntletWinLose(final ViewWinLose view0) {
        super(view0);
    }

    /**
     * <p>
     * populateCustomPanel.
     * </p>
     * @return true
     */
    @Override
    public final boolean populateCustomPanel() {
        final GauntletData gd = FModel.SINGLETON_INSTANCE.getGauntletData();
        final FMatchState matchState = FModel.SINGLETON_INSTANCE.getMatchState();
        final List<String> lstEventNames = gd.getEventNames();
        final List<Deck> lstDecks = gd.getDecks();
        final List<String> lstEventRecords = gd.getEventRecords();
        final int len = lstDecks.size();
        final int num = gd.getCompleted();
        JLabel lblGraphic = null;
        JLabel lblMessage1 = null;
        JLabel lblMessage2 = null;

        // No restarts.
        this.getView().getBtnRestart().setVisible(false);

        // Generic event record.
        lstEventRecords.set(gd.getCompleted(), "Ongoing");

        // Match won't be saved until it is over. This opens up a cheat
        // or failsafe mechanism (depending on your perspective) in which
        // the player can restart Forge to replay a match.
        // Pretty sure this can't be fixed until in-game states can be
        // saved. Doublestrike 07-10-12
        if (matchState.isMatchOver()) {
            // In all cases, update stats.
            lstEventRecords.set(gd.getCompleted(),
                    matchState.countGamesWonBy(AllZone.getHumanPlayer())
                    + " - " + matchState.countGamesWonBy(AllZone.getComputerPlayer()));
            gd.setCompleted(gd.getCompleted() + 1);

            // Win match case
            if (matchState.isMatchWonBy(AllZone.getHumanPlayer())) {
                // Gauntlet complete: Remove save file
                if (gd.getCompleted() == lstDecks.size()) {
                    lblGraphic = new FLabel.Builder()
                        .icon(FSkin.getIcon(FSkin.QuestIcons.ICO_COIN)).build();
                    lblMessage1 = new FLabel.Builder().fontSize(24)
                        .text("CONGRATULATIONS!").build();
                    lblMessage2 = new FLabel.Builder().fontSize(18)
                        .text("You made it through the gauntlet!").build();

                    this.getView().getBtnContinue().setVisible(false);
                    this.getView().getBtnContinue().repaintSelf();
                    this.getView().getBtnQuit().setText("OK");

                    // Remove save file if it's a quickie, or just reset it.
                    if (gd.getActiveFile().getName().matches(GauntletIO.REGEX_QUICK)) {
                        gd.getActiveFile().delete();
                    }
                    else {
                        gd.reset();
                    }
                }
                // Or, save and move to next game
                else {
                    gd.stamp();
                    GauntletIO.saveGauntlet(gd);
                    matchState.reset();
                    AllZone.getComputerPlayer().setDeck(lstDecks.get(gd.getCompleted()));

                    this.getView().getBtnContinue().setVisible(true);
                    this.getView().getBtnContinue().setEnabled(true);
                    this.getView().getBtnQuit().setText("Save and Quit");
                }
            }
            // Lose match case; stop gauntlet.
            else {
                lblGraphic = new FLabel.Builder()
                    .icon(FSkin.getIcon(FSkin.QuestIcons.ICO_HEART)).build();
                lblMessage1 = new FLabel.Builder().fontSize(24)
                        .text("DEFEATED!").build();
                lblMessage2 = new FLabel.Builder().fontSize(18)
                        .text("You have failed to pass the gauntlet.").build();

                this.getView().getBtnContinue().setVisible(false);

                // Remove save file if it's a quickie, or just reset it.
                if (gd.getActiveFile().getName().matches(GauntletIO.REGEX_QUICK)) {
                    gd.getActiveFile().delete();
                }
                else {
                    gd.reset();
                }
            }
        }

        gd.setEventRecords(lstEventRecords);

        // Custom panel display
        final JLabel lblTitle = new FLabel.Builder().text("Gauntlet Progress")
                .fontAlign(SwingConstants.CENTER).fontSize(18).build();

        final JPanel pnlResults = new JPanel();
        pnlResults.setOpaque(false);
        pnlResults.setLayout(new MigLayout("insets 0, gap 0, wrap "
                + (int) Math.ceil(gd.getDecks().size() / 2d) + ", flowy"));

        JLabel lblTemp;
        for (int i = 0; i < len; i++) {
            lblTemp = new FLabel.Builder().fontSize(14).build();

            if (i <= num) {
                lblTemp.setForeground(Color.green.darker());
                lblTemp.setText((i + 1) + ". " + lstEventNames.get(i)
                        + " (" + lstEventRecords.get(i) + ")");
            }
            else {
                lblTemp.setForeground(Color.red);
                lblTemp.setText((i + 1) + ". ??????");
            }

            pnlResults.add(lblTemp, "w 50%!, h 25px!, gap 0 0 5px 0");
        }

        final JPanel pnl = this.getView().getPnlCustom();
        pnl.setLayout(new MigLayout("insets 0, gap 0, wrap, ax center"));
        pnl.setOpaque(true);
        pnl.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        pnl.add(lblTitle, "gap 0 0 20px 10px, ax center");
        pnl.add(pnlResults, "w 96%!, growy, pushy, gap 2% 0 0 0");

        if (lblGraphic != null) {
            pnl.add(lblGraphic, "w 120px!, h 120px!, ax center");
            pnl.add(lblMessage1, "w 96%!, h 40px!, gap 2% 0 0 0");
            pnl.add(lblMessage2, "w 96%!, h 40px!, gap 2% 0 0 50px");
        }

        return true;
    }
}
