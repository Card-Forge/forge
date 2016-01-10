package forge.screens.match;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.StringUtils;

import forge.UiCommand;
import forge.assets.FSkinProp;
import forge.game.GameLogEntry;
import forge.game.GameLogEntryType;
import forge.game.GameView;
import forge.gui.SOverlayUtils;
import forge.interfaces.IWinLoseView;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.toolbox.FButton;
import forge.toolbox.FLabel;
import forge.toolbox.FOverlay;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.Colors;
import forge.toolbox.FSkin.SkinColor;
import forge.toolbox.FSkin.SkinIcon;
import forge.toolbox.FSkin.SkinnedLabel;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.toolbox.FTextArea;

public class ViewWinLose implements IWinLoseView<FButton> {
    private final ControlWinLose control;

    private final FScrollPane scrLog;
    private final FButton btnContinue, btnRestart, btnQuit;
    private final SkinnedPanel pnlCustom;

    private final SkinnedLabel lblTitle = new SkinnedLabel("WinLoseFrame > lblTitle needs updating.");
    private final SkinnedLabel lblStats = new SkinnedLabel("WinLoseFrame > lblStats needs updating.");
    private final JPanel pnlOutcomes = new JPanel(new MigLayout("wrap, align center"));

    /** String constraint parameters for title blocks and cardviewer blocks. */
    private static final SkinColor FORE_COLOR = FSkin.getColor(Colors.CLR_TEXT);
    private static final String CONSTRAINTS_TITLE = "w 95%!, gap 0 0 20px 10px";
    private static final String CONSTRAINTS_TEXT = "w 95%!, h 220px!, gap 0 0 0 20px";
    private static final String CONSTRAINTS_CARDS = "w 95%!, h 330px!, gap 0 0 0 20px";
    private static final String CONSTRAINTS_CARDS_LARGE = "w 95%!, h 600px!, gap 0 0 0 20px";

    private final GameView game;

    @SuppressWarnings("serial")
    public ViewWinLose(final GameView game0, final CMatchUI matchUI) {
        this.game = game0;

        final JPanel overlay = FOverlay.SINGLETON_INSTANCE.getPanel();

        final JPanel pnlLeft = new JPanel();
        final JPanel pnlRight = new JPanel();
        final FScrollPane scrCustom = new FScrollPane(false);
        pnlCustom = new SkinnedPanel();

        btnContinue = new FButton();
        btnRestart = new FButton();
        btnQuit = new FButton();

        // Control of the win/lose is handled differently for various game
        // modes.
        ControlWinLose control = null;
        switch (game0.getGameType()) {
        case Quest:
            control = new QuestWinLose(this, game0, matchUI);
            break;
        case QuestDraft:
            control = new QuestDraftWinLose(this, game0, matchUI);
            break;
        case Draft:
            if (!FModel.getGauntletMini().isGauntletDraft()) {
                break;
            }
            //$FALL-THROUGH$
        case Sealed:
            control = new LimitedWinLose(this, game0, matchUI);
            break;
        case Gauntlet:
            control = new GauntletWinLose(this, game0, matchUI);
            break;
        default: // will catch it after switch
            break;
        }
        if (null == control) {
            control = new ControlWinLose(this, game0, matchUI);
        }
        this.control = control;

        pnlLeft.setOpaque(false);
        pnlRight.setOpaque(false);
        pnlCustom.setOpaque(false);
        scrCustom.getViewport().add(pnlCustom);

        lblTitle.setForeground(Color.white);
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        lblTitle.setFont(FSkin.getBoldFont(30));

        lblStats.setForeground(Color.white);
        lblStats.setHorizontalAlignment(SwingConstants.CENTER);
        lblStats.setFont(FSkin.getFont(26));

        btnContinue.setText("Next Game");
        btnContinue.setFont(FSkin.getFont(22));
        btnRestart.setText("Start New Match");
        btnRestart.setFont(FSkin.getFont(22));
        btnQuit.setText("Quit Match");
        btnQuit.setFont(FSkin.getFont(22));
        btnContinue.setEnabled(!game0.isMatchOver());

        // Assemble game log scroller.
        final FTextArea txtLog = new FTextArea();
        txtLog.setText(StringUtils.join(game.getGameLog().getLogEntries(null), "\r\n").replace("[COMPUTER]", "[AI]"));
        txtLog.setFont(FSkin.getFont(14));
        txtLog.setFocusable(true); // allow highlighting and copying of log

        final FLabel btnCopyLog = new FLabel.ButtonBuilder().text("Copy to clipboard").build();
        btnCopyLog.setCommand(new UiCommand() {
            @Override
            public void run() {
                final StringSelection ss = new StringSelection(txtLog.getText());
                try {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
                } catch (final IllegalStateException ex) {
                    // ignore; may be unavailable on some platforms
                }
            }
        });

        // Add all components accordingly.
        overlay.setLayout(new MigLayout("insets 0, w 100%!, h 100%!"));
        pnlLeft.setLayout(new MigLayout("insets 0, wrap, align center"));
        pnlRight.setLayout(new MigLayout("insets 0, wrap"));
        pnlCustom.setLayout(new MigLayout("insets 0, wrap, align center"));

        final boolean customIsPopulated = control.populateCustomPanel();
        if (customIsPopulated) {
            overlay.add(pnlLeft, "w 40%!, h 100%!");
            overlay.add(pnlRight, "w 60%!, h 100%!");
            pnlRight.add(scrCustom, "w 100%!, h 100%!");
        } else {
            overlay.add(pnlLeft, "w 100%!, h 100%!");
        }

        pnlOutcomes.setOpaque(false);
        pnlLeft.add(lblTitle, "h 60px!, center");
        pnlLeft.add(pnlOutcomes, "center");
        pnlLeft.add(lblStats, "h 60px!, center");

        // A container must be made to ensure proper centering.
        final JPanel pnlButtons = new JPanel(new MigLayout("insets 0, wrap, ax center"));
        pnlButtons.setOpaque(false);

        final String constraints = "w 300px!, h 50px!, gap 0 0 20px 0";
        pnlButtons.add(btnContinue, constraints);
        pnlButtons.add(btnRestart, constraints);
        pnlButtons.add(btnQuit, constraints);
        pnlLeft.add(pnlButtons, "w 100%!");

        final JPanel pnlLog = new JPanel(new MigLayout("insets 0, wrap, ax center"));
        scrLog = new FScrollPane(txtLog, false);
        pnlLog.setOpaque(false);

        pnlLog.add(
                new FLabel.Builder().text("Game Log").fontAlign(SwingConstants.CENTER).fontSize(18)
                .fontStyle(Font.BOLD).build(), "w 300px!, h 28px!, gaptop 20px");

        pnlLog.add(scrLog, "w 300px!, h 100px!, gap 0 0 10 10");
        pnlLog.add(btnCopyLog, "center, w pref+16, h pref+8");
        pnlLeft.add(pnlLog, "w 100%!");

        lblTitle.setText(composeTitle(game0));
    }

    public final void show() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                scrLog.getViewport().setViewPosition(new Point(0, 0));
                // populateCustomPanel may have changed which buttons are
                // enabled; focus on the 'best' one
                if (btnContinue.isEnabled()) {
                    btnContinue.requestFocusInWindow();
                } else {
                    btnQuit.requestFocusInWindow();
                }
            }
        });

        showGameOutcomeSummary();
        showPlayerScores();
    }

    public final ControlWinLose getControl() {
        return control;
    }

    private static String composeTitle(final GameView game) {
        final String winner = game.getWinningPlayerName();
        final int winningTeam = game.getWinningTeam();
        if (winner == null || winner.isEmpty()) {
            return "It's a draw!";
        } else if (winningTeam != -1) {
            return String.format("Team %d won!", Integer.valueOf(winningTeam));
        } else {
            return String.format("%s won!", winner);
        }
    }

    /** @return {@link forge.toolbox.FButton} */
    @Override
    public FButton getBtnContinue() {
        return this.btnContinue;
    }

    /** @return {@link forge.toolbox.FButton} */
    @Override
    public FButton getBtnRestart() {
        return this.btnRestart;
    }

    /** @return {@link forge.toolbox.FButton} */
    @Override
    public FButton getBtnQuit() {
        return this.btnQuit;
    }

    /** @return {@link javax.swing.JPanel} */
    public SkinnedPanel getPnlCustom() {
        return this.pnlCustom;
    }

    private void showGameOutcomeSummary() {
        for (final GameLogEntry o : game.getGameLog().getLogEntriesExact(GameLogEntryType.GAME_OUTCOME)) {
            pnlOutcomes.add(new FLabel.Builder().text(o.message).fontSize(14).build(), "h 20!");
        }
    }

    private void showPlayerScores() {
        for (final GameLogEntry o : game.getGameLog().getLogEntriesExact(GameLogEntryType.MATCH_RESULTS)) {
            lblStats.setText(removePlayerTypeFromLogMessage(o.message));
        }
    }

    private static String removePlayerTypeFromLogMessage(final String message) {
        return message.replaceAll("\\[[^\\]]*\\]", "");
    }

    @Override
    public void hide() {
        SOverlayUtils.hideOverlay();
    }

    @Override
    public void showRewards(final Runnable runnable) {
        runnable.run(); //just run on GUI thread
    }

    @Override
    public void showCards(final String title, final List<PaperCard> cards) {
        final QuestWinLoseCardViewer cv = new QuestWinLoseCardViewer(cards);
        getPnlCustom().add(new TitleLabel(title), CONSTRAINTS_TITLE);
        if (FModel.getPreferences().getPrefBoolean(FPref.UI_LARGE_CARD_VIEWERS)) {
            getPnlCustom().add(cv, CONSTRAINTS_CARDS_LARGE);
        }
        else {
            getPnlCustom().add(cv, CONSTRAINTS_CARDS);
        }
    }

    @Override
    public void showMessage(String message, final String title, final FSkinProp icon) {
        final SkinIcon icoTemp = FSkin.getIcon(icon).scale(0.5);

        if (message.contains("\n")) { //ensure new line characters are encoded
            message = "<html>" + message.replace("\n", "<br>") + "</html>";
        }
        final SkinnedLabel lblMessage = new SkinnedLabel(message);
        lblMessage.setFont(FSkin.getFont(14));
        lblMessage.setForeground(FORE_COLOR);
        lblMessage.setHorizontalAlignment(SwingConstants.CENTER);
        lblMessage.setIconTextGap(50);
        lblMessage.setIcon(icoTemp);

        getPnlCustom().add(new TitleLabel(title), CONSTRAINTS_TITLE);
        getPnlCustom().add(lblMessage, CONSTRAINTS_TEXT);
    }

    /**
     * JLabel header between reward sections.
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
