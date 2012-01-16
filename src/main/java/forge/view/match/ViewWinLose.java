package forge.view.match;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.Constant;
import forge.Phase;
import forge.Player;
import forge.control.match.ControlWinLose;
import forge.game.GameType;
import forge.properties.ForgeProps;
import forge.properties.NewConstants.Lang.GuiWinLose.WinLoseText;
import forge.quest.data.QuestMatchState;
import forge.quest.gui.QuestWinLoseHandler;
import forge.view.toolbox.FButton;
import forge.view.toolbox.FOverlay;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ViewWinLose {
    private FButton btnContinue, btnRestart, btnQuit;
    private JPanel pnlCustom;

    /** */
    public ViewWinLose() {
        final FOverlay overlay = AllZone.getOverlay();
        final QuestMatchState matchState = AllZone.getMatchState();

        final JPanel pnlLeft = new JPanel();
        final JPanel pnlRight = new JPanel();

        final JLabel lblTitle = new JLabel("WinLoseFrame > lblTitle needs updating.");
        final JLabel lblStats = new JLabel("WinLoseFrame > lblStats needs updating.");
        final JScrollPane scrCustom = new JScrollPane();
        pnlCustom = new JPanel();

        btnContinue = new FButton();
        btnRestart = new FButton();
        btnQuit = new FButton();

        // Control of the win/lose is handled differently for various game modes.
        ControlWinLose control;
        if (Constant.Runtime.getGameType() == GameType.Quest) {
            control = new QuestWinLoseHandler(this);
        }
        else {
            control = new ControlWinLose(this);
        }

        pnlLeft.setOpaque(false);
        pnlRight.setOpaque(false);
        pnlCustom.setOpaque(false);
        scrCustom.setOpaque(false);
        scrCustom.setBorder(null);
        scrCustom.getVerticalScrollBar().setUnitIncrement(16);
        scrCustom.getViewport().setOpaque(false);
        scrCustom.getViewport().add(pnlCustom);

        lblTitle.setForeground(Color.white);
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        lblTitle.setFont(AllZone.getSkin().getFont().deriveFont(Font.BOLD, 30));

        lblStats.setForeground(Color.white);
        lblStats.setHorizontalAlignment(SwingConstants.CENTER);
        lblStats.setFont(AllZone.getSkin().getFont().deriveFont(Font.PLAIN, 26));

        btnContinue.setText("Continue");
        btnContinue.setFont(AllZone.getSkin().getFont(22));
        btnRestart.setText("Restart");
        btnRestart.setFont(AllZone.getSkin().getFont(22));
        btnQuit.setText("Quit");
        btnQuit.setFont(AllZone.getSkin().getFont(22));

        // End game and set state of "continue" button
        Phase.setGameBegins(0);

        if (matchState.isMatchOver()) {
            this.getBtnContinue().setEnabled(false);
            this.getBtnQuit().grabFocus();
        }

        // Show Wins and Loses
        final Player human = AllZone.getHumanPlayer();
        final int humanWins = matchState.countGamesWonBy(human.getName());
        final int humanLosses = matchState.getGamesPlayedCount() - humanWins;

        lblStats.setText(ForgeProps.getLocalized(WinLoseText.WON) + humanWins
                + ForgeProps.getLocalized(WinLoseText.LOST) + humanLosses);

        // Show "You Won" or "You Lost"
        if (matchState.hasWonLastGame(human.getName())) {
            lblTitle.setText(ForgeProps.getLocalized(WinLoseText.WIN));
        } else {
            lblTitle.setText(ForgeProps.getLocalized(WinLoseText.LOSE));
        }

        // Add all components accordingly.
        overlay.removeAll();
        overlay.setLayout(new MigLayout("insets 0, w 100%!, h 100%!"));
        pnlLeft.setLayout(new MigLayout("insets 0, wrap, align center"));
        pnlRight.setLayout(new MigLayout("insets 0, wrap"));
        pnlCustom.setLayout(new MigLayout("insets 0, wrap, align center"));

        final boolean customIsPopulated = control.populateCustomPanel();
        if (customIsPopulated) {
            overlay.add(pnlLeft, "w 40%!, h 100%!");
            overlay.add(pnlRight, "w 60%!, h 100%!");
            pnlRight.add(scrCustom, "w 100%!, h 100%!");
        }
        else {
            overlay.add(pnlLeft, "w 100%!, h 100%!");
        }

        pnlLeft.add(lblTitle, "w 90%!, h 50px!, gap 5% 0 2% 0");
        pnlLeft.add(lblStats, "w 90%!, h 50px!, gap 5% 0 2% 0");

        // A container must be made to ensure proper centering.
        final JPanel pnlButtons = new JPanel(new MigLayout("insets 0, wrap, ax center"));
        pnlButtons.setOpaque(false);

        final String constraints = "w 300px!, h 50px!, gaptop 20px";
        pnlButtons.add(btnContinue, constraints);
        pnlButtons.add(btnRestart, constraints);
        pnlButtons.add(btnQuit, constraints);
        pnlLeft.add(pnlButtons, "w 100%!");

        overlay.showOverlay();
    }

    /** @return {@link forge.view.toolbox.FButton} */
    public FButton getBtnContinue() {
        return this.btnContinue;
    }

    /** @return {@link forge.view.toolbox.FButton} */
    public FButton getBtnRestart() {
        return this.btnRestart;
    }

    /** @return {@link forge.view.toolbox.FButton} */
    public FButton getBtnQuit() {
        return this.btnQuit;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getPnlCustom() {
        return this.pnlCustom;
    }
}
