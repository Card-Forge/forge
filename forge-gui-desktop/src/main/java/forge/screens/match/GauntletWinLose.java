package forge.screens.match;

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
import forge.assets.FSkinProp;
import forge.game.GameView;
import forge.gauntlet.GauntletWinLoseController;
import forge.toolbox.FLabel;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedPanel;

/**
 * The Win/Lose handler for 'gauntlet' type tournament
 * games.
 */
public class GauntletWinLose extends ControlWinLose {
    private final GauntletWinLoseController controller;

    /**
     * Instantiates a new gauntlet win/lose handler.
     * 
     * @param view0 ViewWinLose object
     * @param match
     */
    public GauntletWinLose(final ViewWinLose view0, final GameView game0, final CMatchUI matchUI) {
        super(view0, game0, matchUI);
        controller = new GauntletWinLoseController(view0, game0) {
            @Override
            protected void showOutcome(boolean isMatchOver, String message1, String message2, FSkinProp icon, List<String> lstEventNames, List<String> lstEventRecords, int len, int num) {
                final JLabel lblTitle = new FLabel.Builder().text("Gauntlet Progress")
                        .fontAlign(SwingConstants.CENTER).fontSize(18).build();

                final JPanel pnlResults = new JPanel();
                pnlResults.setOpaque(false);
                pnlResults.setLayout(new MigLayout("insets 0, gap 0, wrap "
                        + (int) Math.ceil(len / 2d) + ", flowy"));

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

                final SkinnedPanel pnl = view0.getPnlCustom();
                pnl.setLayout(new MigLayout("insets 0, gap 0, wrap, ax center"));
                pnl.setOpaque(true);
                pnl.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
                pnl.add(lblTitle, "gap 0 0 20px 10px, ax center");
                pnl.add(pnlResults, "w 96%!, growy, pushy, gap 2% 0 0 0");

                if (message1 != null) {
                    pnl.add(new FLabel.Builder().icon(FSkin.getIcon(icon)).build(), "w 120px!, h 120px!, ax center");
                    pnl.add(new FLabel.Builder().fontSize(24).text(message1).build(), "w 96%!, h 40px!, gap 2% 0 0 0");
                    pnl.add(new FLabel.Builder().fontSize(18).text(message2).build(), "w 96%!, h 40px!, gap 2% 0 0 50px");
                }
            }

            @Override
            protected void saveOptions() {
                GauntletWinLose.this.saveOptions();
            }
        };
    }

    /**
     * <p>
     * populateCustomPanel.
     * </p>
     * @return true
     */
    @Override
    public final boolean populateCustomPanel() {
        controller.showOutcome();
        return true;
    }

    @Override
    public void actionOnContinue() {
        if (!controller.actionOnContinue()) {
            super.actionOnContinue();
        }
    }
}
