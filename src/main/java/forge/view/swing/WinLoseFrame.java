package forge.view.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.AbstractBorder;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.Phase;
import forge.Player;
import forge.gui.skin.FButton;
import forge.gui.skin.FPanel;
import forge.properties.ForgeProps;
import forge.properties.NewConstants.Lang.WinLoseFrame.WinLoseText;
import forge.quest.data.QuestMatchState;

/**
 * <p>
 * WinLoseFrame.
 * </p>
 * VIEW - Core display for win/lose UI shown after completing a game. Uses
 * handlers to customize central panel for various game modes.
 * 
 */
@SuppressWarnings("serial")
public class WinLoseFrame extends JFrame {

    private final QuestMatchState matchState;
    private final WinLoseModeHandler modeHandler;

    /** The btn continue. */
    private FButton btnContinue;

    /** The btn quit. */
    private FButton btnQuit;

    /** The btn restart. */
    private FButton btnRestart;

    /** The lbl title. */
    private JLabel lblTitle;

    /** The lbl stats. */
    private JLabel lblStats;

    /** The pnl custom. */
    private JPanel pnlCustom;

    /**
     * <p>
     * WinLoseFrame.
     * </p>
     * Starts win/lose UI with default display.
     * 
     */
    public WinLoseFrame() {
        this(new WinLoseModeHandler());
    }

    /**
     * <p>
     * WinLoseFrame.
     * </p>
     * Starts the standard win/lose UI with custom display for different game
     * modes (quest, puzzle, etc.)
     * 
     * @param mh
     *            the mh {@link forge.view.swing.WinLoseModeHandler}
     */
    public WinLoseFrame(final WinLoseModeHandler mh) {
        super();

        // modeHandler handles the unique display for different game modes.
        this.modeHandler = mh;
        this.modeHandler.setView(this);
        this.matchState = AllZone.getMatchState();

        // Place all content in FPanel
        final FPanel contentPanel = new FPanel(new MigLayout("wrap, fill, insets 20 0 10 10"));
        contentPanel.setBGImg(AllZone.getSkin().getTexture1());
        contentPanel.setBorder(new WinLoseBorder());
        this.getContentPane().add(contentPanel);

        // Footer should be at least 150 to keep buttons in-pane on Mac OS X
        // it needs to be > 175 now that skinning is used
        final int headHeight = 150;
        final int footHeight = 182;
        final int frameWidthSmall = 300;
        final int frameWidthBig = 600;

        // Head panel
        final JPanel pnlHead = new JPanel(new MigLayout("wrap, fill"));
        pnlHead.setOpaque(false);
        contentPanel.add(pnlHead, "width " + frameWidthSmall + "!, align center");

        this.lblTitle = new JLabel("WinLoseFrame > lblTitle is broken.");
        this.lblTitle.setForeground(Color.white);
        this.lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        this.lblTitle.setFont(AllZone.getSkin().getFont1().deriveFont(Font.PLAIN, 26));

        this.lblStats = new JLabel("WinLoseFrame > lblStats is broken.");
        this.lblStats.setForeground(Color.white);
        this.lblStats.setHorizontalAlignment(SwingConstants.CENTER);
        this.lblStats.setFont(AllZone.getSkin().getFont1().deriveFont(Font.PLAIN, 26));

        pnlHead.add(this.lblTitle, "growx");
        pnlHead.add(this.lblStats, "growx");

        // Custom display panel in center; populated later by mode handler.
        final JScrollPane scroller = new JScrollPane();
        scroller.getVerticalScrollBar().setUnitIncrement(16);
        this.setPnlCustom(new JPanel(new MigLayout("wrap, fillx")));
        this.getPnlCustom().setBackground(new Color(111, 87, 59));
        this.getPnlCustom().setForeground(Color.white);
        contentPanel.add(scroller, "w 96%!, align center, gapleft 2%");
        scroller.getViewport().add(this.getPnlCustom());

        // Foot panel
        final JPanel pnlFoot = new JPanel(new MigLayout("wrap, fill, hidemode 3"));
        pnlFoot.setOpaque(false);
        contentPanel.add(pnlFoot, "width " + frameWidthSmall + "!, align center");

        this.setBtnContinue(new FButton("Continue"));
        this.setBtnRestart(new FButton("Restart"));
        this.setBtnQuit(new FButton("Quit"));

        pnlFoot.add(this.getBtnContinue(), "h 36:36, w 200!, gap 0 0 5 5, align center");
        pnlFoot.add(this.getBtnRestart(), "h 36:36, w 200!, gap 0 0 5 5, align center");
        pnlFoot.add(this.getBtnQuit(), "h 36:36, w 200!, gap 0 0 5 5, align center");

        // Button actions
        this.getBtnQuit().addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                WinLoseFrame.this.btnQuitActionPerformed(e);
            }
        });

        this.getBtnContinue().addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                WinLoseFrame.this.btnContinueActionPerformed(e);
            }
        });

        this.getBtnRestart().addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                WinLoseFrame.this.btnRestartActionPerformed(e);
            }
        });

        // End game and set state of "continue" button
        Phase.setGameBegins(0);

        if (this.matchState.isMatchOver()) {
            this.getBtnContinue().setEnabled(false);
            this.getBtnQuit().grabFocus();
        }

        // Show Wins and Loses
        final Player human = AllZone.getHumanPlayer();
        final int humanWins = this.matchState.countGamesWonBy(human.getName());
        final int humanLosses = this.matchState.getGamesPlayedCount() - humanWins;

        this.lblStats.setText(ForgeProps.getLocalized(WinLoseText.WON) + humanWins
                + ForgeProps.getLocalized(WinLoseText.LOST) + humanLosses);

        // Show "You Won" or "You Lost"
        if (this.matchState.hasWonLastGame(human.getName())) {
            this.lblTitle.setText(ForgeProps.getLocalized(WinLoseText.WIN));
        } else {
            this.lblTitle.setText(ForgeProps.getLocalized(WinLoseText.LOSE));
        }

        // Populate custom panel, if necessary.
        final boolean hasContents = this.modeHandler.populateCustomPanel();
        if (!hasContents) {
            scroller.setVisible(false);
        }

        // Size and show frame
        final Dimension screen = this.getToolkit().getScreenSize();
        final Rectangle bounds = this.getBounds();

        if (hasContents) {
            bounds.height = screen.height - 150;
            scroller.setPreferredSize(new Dimension(frameWidthBig, screen.height - headHeight - footHeight));
            bounds.width = frameWidthBig;
            bounds.x = (screen.width - frameWidthBig) / 2;
            bounds.y = (screen.height - bounds.height) / 2;
        } else {
            bounds.height = headHeight + footHeight;
            bounds.width = frameWidthSmall;
            bounds.x = (screen.width - frameWidthSmall) / 2;
            bounds.y = (screen.height - bounds.height) / 2;
        }

        this.setBackground(AllZone.getSkin().getBg1a());
        this.setBounds(bounds);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setUndecorated(true);
        this.setVisible(true);
    }

    /**
     * <p>
     * continueButton_actionPerformed.
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.ActionEvent} object.
     */
    final void btnContinueActionPerformed(final ActionEvent e) {
        this.closeWinLoseFrame();
        AllZone.getDisplay().setVisible(true);
        this.modeHandler.startNextRound();
    }

    /**
     * <p>
     * restartButton_actionPerformed.
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.ActionEvent} object.
     */
    final void btnRestartActionPerformed(final ActionEvent e) {
        this.closeWinLoseFrame();
        AllZone.getDisplay().setVisible(true);
        this.matchState.reset();
        this.modeHandler.startNextRound();
    }

    /**
     * <p>
     * btnQuit_actionPerformed.
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.ActionEvent} object.
     */
    final void btnQuitActionPerformed(final ActionEvent e) {
        this.closeWinLoseFrame();
        this.matchState.reset();
        this.modeHandler.actionOnQuit();

        // clear Image caches, so the program doesn't get slower and slower
        // not needed with soft values - will shrink as needed
        // ImageUtil.rotatedCache.clear();
        // ImageCache.cache.clear();
    }

    /**
     * <p>
     * closeWinLoseFrame.
     * </p>
     * Disposes WinLoseFrame UI.
     * 
     * @return {@link javax.swing.JFrame} display frame
     */
    final JFrame closeWinLoseFrame() {
        // Issue 147 - keep battlefield up following win/loss
        final JFrame frame = (JFrame) AllZone.getDisplay();
        frame.dispose();
        frame.setEnabled(true);
        this.dispose();
        return frame;
    }

    /**
     * @return the btnContinue
     */
    public FButton getBtnContinue() {
        return btnContinue;
    }

    /**
     * @param btnContinue the btnContinue to set
     */
    public void setBtnContinue(FButton btnContinue) {
        this.btnContinue = btnContinue; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the btnRestart
     */
    public FButton getBtnRestart() {
        return btnRestart;
    }

    /**
     * @param btnRestart the btnRestart to set
     */
    public void setBtnRestart(FButton btnRestart) {
        this.btnRestart = btnRestart; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the btnQuit
     */
    public FButton getBtnQuit() {
        return btnQuit;
    }

    /**
     * @param btnQuit the btnQuit to set
     */
    public void setBtnQuit(FButton btnQuit) {
        this.btnQuit = btnQuit; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the pnlCustom
     */
    public JPanel getPnlCustom() {
        return pnlCustom;
    }

    /**
     * @param pnlCustom the pnlCustom to set
     */
    public void setPnlCustom(JPanel pnlCustom) {
        this.pnlCustom = pnlCustom; // TODO: Add 0 to parameter's name.
    }

    private class WinLoseBorder extends AbstractBorder {
        @Override
        public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int width,
                final int height) {
            g.setColor(AllZone.getSkin().getTxt1a());
            g.drawRect(x + 1, y + 1, width - 3, height - 3);
            g.setColor(AllZone.getSkin().getBg1a());
            g.drawRect(x + 3, y + 3, width - 7, height - 7);
        }
    }
}
