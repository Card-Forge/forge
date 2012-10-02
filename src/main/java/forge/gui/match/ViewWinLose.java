package forge.gui.match;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.Singletons;
import forge.game.GameType;
import forge.game.phase.PhaseHandler;
import forge.game.player.Player;
import forge.gui.SOverlayUtils;
import forge.gui.toolbox.FButton;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FOverlay;
import forge.gui.toolbox.FScrollPane;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FTextArea;
import forge.model.FMatchState;
import forge.properties.ForgeProps;
import forge.properties.NewConstants.Lang.GuiWinLose.WinLoseText;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ViewWinLose {
    private final FButton btnContinue, btnRestart, btnQuit;
    private final JPanel pnlCustom;
    private final FTextArea txtLog;

    /** */
    public ViewWinLose() {
        final JPanel overlay = FOverlay.SINGLETON_INSTANCE.getPanel();
        final FMatchState matchState = Singletons.getModel().getMatchState();

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
        if (Singletons.getModel().getMatchState().getGameType() == GameType.Quest) {
            control = new QuestWinLoseHandler(this);
        }
        else if (Singletons.getModel().getMatchState().getGameType() == GameType.Sealed
                || (Singletons.getModel().getMatchState().getGameType() == GameType.Draft && AllZone.getGauntlet().getGauntletDraft())) {

            control = new GauntletWinLose(this);
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
        lblTitle.setFont(FSkin.getFont().deriveFont(Font.BOLD, 30));

        lblStats.setForeground(Color.white);
        lblStats.setHorizontalAlignment(SwingConstants.CENTER);
        lblStats.setFont(FSkin.getFont().deriveFont(Font.PLAIN, 26));

        btnContinue.setText(ForgeProps.getLocalized(WinLoseText.CONTINUE));
        btnContinue.setFont(FSkin.getFont(22));
        btnRestart.setText(ForgeProps.getLocalized(WinLoseText.RESTART));
        btnRestart.setFont(FSkin.getFont(22));
        btnQuit.setText(ForgeProps.getLocalized(WinLoseText.QUIT));
        btnQuit.setFont(FSkin.getFont(22));

        // End game and set state of "continue" button
        PhaseHandler.setGameBegins(0);

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

        // Assemble game log scroller.
        txtLog = new FTextArea();
        txtLog.setText(AllZone.getGameLog().getLogText());
        txtLog.setFont(FSkin.getFont(14));

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

        pnlLeft.add(lblTitle, "w 90%!, h 50px!, gap 5% 0 20px 0");
        pnlLeft.add(lblStats, "w 90%!, h 50px!, gap 5% 0 20px 0");

        // A container must be made to ensure proper centering.
        final JPanel pnlButtons = new JPanel(new MigLayout("insets 0, wrap, ax center"));
        pnlButtons.setOpaque(false);

        final String constraints = "w 300px!, h 50px!, gap 0 0 20px 0";
        pnlButtons.add(btnContinue, constraints);
        pnlButtons.add(btnRestart, constraints);
        pnlButtons.add(btnQuit, constraints);
        pnlLeft.add(pnlButtons, "w 100%!");

        final JPanel pnlLog = new JPanel(new MigLayout("insets 0, wrap, ax center"));
        final FScrollPane scrLog = new FScrollPane(txtLog);
        scrLog.getViewport().setViewPosition(new Point(0, 0));
        scrLog.setBorder(null);
        pnlLog.setOpaque(false);

        pnlLog.add(new FLabel.Builder().text("Game Log").fontAlign(SwingConstants.CENTER)
                .fontSize(18).fontStyle(Font.BOLD).build(),
                "w 300px!, h 28px!, gap 0 0 20px 0");

        pnlLog.add(scrLog, "w 300px!, h 100px!, gap 0 0 10px 0");
        pnlLeft.add(pnlLog, "w 100%!");

        SOverlayUtils.showOverlay();
    }

    /** @return {@link forge.gui.toolbox.FButton} */
    public FButton getBtnContinue() {
        return this.btnContinue;
    }

    /** @return {@link forge.gui.toolbox.FButton} */
    public FButton getBtnRestart() {
        return this.btnRestart;
    }

    /** @return {@link forge.gui.toolbox.FButton} */
    public FButton getBtnQuit() {
        return this.btnQuit;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getPnlCustom() {
        return this.pnlCustom;
    }
}
