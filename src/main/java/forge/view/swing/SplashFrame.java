package forge.view.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;

import net.slightlymagic.braids.util.progress_monitor.BraidsProgressMonitor;
import forge.gui.skin.FSkin;

/**
 * Shows the splash frame as the application starts.
 */
@SuppressWarnings("serial")
public class SplashFrame extends JFrame {

    // Inits: Visual changes can be made here.
    private static final int BAR_PADDING_X = 20;
    private static final int BAR_PADDING_Y = 20;
    private static final int BAR_HEIGHT = 57;

    private static final int DISCLAIMER_HEIGHT = 20;
    private static final int DISCLAIMER_TOP = 300;
    private static final int DISCLAIMER_FONT_SIZE = 9;
    private static final Color DISCLAIMER_COLOR = Color.white;

    // private static final int CLOSEBTN_PADDING_X = 15;
    private static final int CLOSEBTN_PADDING_Y = 15;
    private static final int CLOSEBTN_SIDELENGTH = 15;
    private static final Color CLOSEBTN_COLOR = new Color(215, 208, 188);

    private SplashProgressModel monitorModel = null;
    private SplashProgressComponent monitorView = null;

    private boolean splashHasBeenClosed = false;

    /**
     * <p>
     * Create the frame; this <strong>must</strong> be called from an event
     * dispatch thread.
     * </p>
     * 
     * <!-- CheckStyle won't let me use at-throws. --> Throws
     * 
     * @param skin
     *            the skin {@link IllegalStateException} if not called from an
     *            event dispatch thread.
     */
    public SplashFrame(final FSkin skin) {
        super();

        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("SplashFrame() must be called from an event dispatch thread.");
        }

        this.setUndecorated(true);

        // Set preferred JFrame properties.
        final ImageIcon bgIcon = skin.getSplashBG();
        final int splashWidthPx = bgIcon.getIconWidth();
        final int splashHeightPx = bgIcon.getIconHeight();

        this.setMinimumSize(new Dimension(splashWidthPx, splashHeightPx));
        this.setLocationRelativeTo(null);
        this.setResizable(false);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Insert JPanel to hold content above background
        final JPanel contentPane = new JPanel();
        this.setContentPane(contentPane);
        contentPane.setLayout(null);

        // Add disclaimer
        final JLabel lblDisclaimer = new JLabel(
                "<html><center>Forge is not affiliated in any way with Wizards of the Coast."
        + "<br>Forge is open source software, released under the GNU Public License.</center></html>");

        lblDisclaimer.setBounds(0, SplashFrame.DISCLAIMER_TOP, splashWidthPx, SplashFrame.DISCLAIMER_HEIGHT);

        lblDisclaimer.setFont(new Font("Tahoma", Font.PLAIN, SplashFrame.DISCLAIMER_FONT_SIZE));
        lblDisclaimer.setHorizontalAlignment(SwingConstants.CENTER);
        lblDisclaimer.setForeground(SplashFrame.DISCLAIMER_COLOR);
        contentPane.add(lblDisclaimer);

        // Add close button
        final JButton btnClose = new JButton("X");
        btnClose.setBounds(splashWidthPx - (2 * SplashFrame.CLOSEBTN_PADDING_Y), SplashFrame.CLOSEBTN_PADDING_Y,
                SplashFrame.CLOSEBTN_SIDELENGTH, SplashFrame.CLOSEBTN_SIDELENGTH);
        btnClose.setForeground(SplashFrame.CLOSEBTN_COLOR);
        btnClose.setBorder(BorderFactory.createLineBorder(SplashFrame.CLOSEBTN_COLOR));
        btnClose.setOpaque(false);
        btnClose.setBackground(new Color(0, 0, 0));
        btnClose.setFocusPainted(false);
        contentPane.add(btnClose);

        // Action handler: button hover effect
        btnClose.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(final java.awt.event.MouseEvent evt) {
                btnClose.setBorder(BorderFactory.createLineBorder(Color.white));
                btnClose.setForeground(Color.white);
            }

            @Override
            public void mouseExited(final java.awt.event.MouseEvent evt) {
                btnClose.setBorder(BorderFactory.createLineBorder(SplashFrame.CLOSEBTN_COLOR));
                btnClose.setForeground(SplashFrame.CLOSEBTN_COLOR);
            }
        });

        // Action handler: button close
        btnClose.addActionListener(new CloseAction());

        // Action handler: esc key close
        contentPane.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "escAction");

        contentPane.getActionMap().put("escAction", new CloseAction());

        // Set UI to color splash bar fill with skin colors
        UIManager.put("ProgressBar.background", skin.getClrProgress1());           // Unfilled state
        UIManager.put("ProgressBar.selectionBackground", skin.getClrProgress2());  // Unfilled state
        UIManager.put("ProgressBar.foreground", skin.getClrProgress3());           // Filled state
        UIManager.put("ProgressBar.selectionForeground", skin.getClrProgress4());  // Filled state
        UIManager.put("ProgressBar.border", new LineBorder(skin.getClrTheme(), 0));

        // Instantiate model and view and tie together.
        this.monitorModel = new SplashProgressModel();
        this.monitorView = new SplashProgressComponent();

        this.monitorModel.setCurrentView(this.monitorView);
        this.monitorView.setCurrentModel(this.monitorModel);

        // Add prog bar + message, bg image
        this.monitorView.displayUpdate("Assembling file list...");
        this.monitorView.setBounds(SplashFrame.BAR_PADDING_X, splashHeightPx - SplashFrame.BAR_PADDING_Y
                - SplashFrame.BAR_HEIGHT, splashWidthPx - (2 * SplashFrame.BAR_PADDING_X), SplashFrame.BAR_HEIGHT);
        contentPane.add(this.monitorView);

        contentPane.setOpaque(false);
        final JLabel bgLabel = new JLabel(bgIcon);

        // Do not pass Integer.MIN_VALUE directly here; it must be packaged in
        // an Integer instance. Otherwise, GUI components will not draw unless moused over.
        this.getLayeredPane().add(bgLabel, Integer.valueOf(Integer.MIN_VALUE));

        bgLabel.setBounds(0, 0, splashWidthPx, splashHeightPx);

        this.pack();
    }

    /**
     * Getter for progress bar view.
     * 
     * @return the SplashViewProgressMonitor progress bar used in the splash
     *         frame.
     */
    public final SplashProgressComponent getMonitorView() {
        return this.monitorView;
    }

    /**
     * Getter for progress monitor model.
     * 
     * @return the BraidsProgressMonitor model used in the splash frame.
     */
    public final BraidsProgressMonitor getMonitorModel() {
        return this.monitorModel;
    }

    /**
     * Returns state of splash frame, to determine if GUI should continue
     * loading.
     * 
     * @return SplashHasBeenClosed boolean.
     */
    public final boolean getSplashHasBeenClosed() {
        return this.splashHasBeenClosed;
    }

    /**
     * Sets state of splash frame, to determine if GUI should continue loading.
     * 
     * @param neoState
     *            the new splash has been closed
     */
    public final void setSplashHasBeenClosed(final boolean neoState) {
        this.splashHasBeenClosed = neoState;
    }

    /**
     * <p>
     * closeAction
     * </p>
     * Closes the splash frame by toggling "has been closed" switch (to cancel
     * preloading) and dispose() (to discard JFrame).
     * 
     * @param splashHasBeenClosed
     *            boolean.
     */
    private class CloseAction extends AbstractAction {
        @Override
        public void actionPerformed(final ActionEvent e) {
            SplashFrame.this.setSplashHasBeenClosed(true);
            SplashFrame.this.dispose();
        }
    }

}
