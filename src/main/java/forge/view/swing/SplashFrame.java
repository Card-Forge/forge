package forge.view.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import forge.gui.MultiPhaseProgressMonitorWithETA;

/**
 * Shows the splash frame as the application starts.
 */
@SuppressWarnings("serial")
public class SplashFrame extends JFrame {

    private static final Color WHITE_COLOR = new Color(255, 255, 255);
    private static final int DISCLAIMER_EAST_WEST_PADDING_PX = 40; // NOPMD by Braids on 8/17/11 9:06 PM
    private static final int DISCLAIMER_FONT_SIZE = 9; // NOPMD by Braids on 8/17/11 9:06 PM
    private static final int DISCLAIMER_NORTH_PADDING_PX = 300; // NOPMD by Braids on 8/17/11 9:06 PM
    private static final int DISCLAIMER_HEIGHT_PX = 20; // NOPMD by Braids on 8/17/11 9:06 PM

    private MultiPhaseProgressMonitorWithETA monitor;


    /**
     * <p>Create the frame; this <strong>must</strong> be called from an event
     * dispatch thread.</p>
     *
     * <!-- CheckStyle won't let me use at-throws. -->
     * throws {@link IllegalStateException} if not called from an event
     * dispatch thread.
     */
    public SplashFrame() {
        super();

        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("must be called from an event dispatch thread");
        }

        setUndecorated(true);

        final ImageIcon bgIcon = new ImageIcon("res/images/ui/forgeSplash by moomarc.jpg");

        final int splashWidthPx = bgIcon.getIconWidth();
        final int splashHeightPx = bgIcon.getIconHeight();

        monitor = new MultiPhaseProgressMonitorWithETA("Loading card database", 1,
                1, 1.0f);

        final JDialog progressBarDialog = monitor.getDialog();

        final Rectangle progressRect = progressBarDialog.getBounds();

        setMinimumSize(new Dimension(splashWidthPx, splashHeightPx));
        setLocation(progressRect.x, progressRect.y + progressRect.height);

        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        final JPanel contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        setContentPane(contentPane);
        contentPane.setLayout(null);

        final JLabel lblDisclaimer = new JLabel("Forge is not affiliated in any way with Wizards of the Coast.");

        // we can't do multiline labels.
        //+ "\nIt is open source software, released under the GNU Public License."
        //+ "\n And while we have your attention, go buy some Magic: the Gathering cards!"

        lblDisclaimer.setBounds(DISCLAIMER_EAST_WEST_PADDING_PX, DISCLAIMER_NORTH_PADDING_PX,
                splashWidthPx - (2 * DISCLAIMER_EAST_WEST_PADDING_PX),
                DISCLAIMER_HEIGHT_PX);

        lblDisclaimer.setFont(new Font("Tahoma", Font.PLAIN, DISCLAIMER_FONT_SIZE));
        lblDisclaimer.setHorizontalAlignment(SwingConstants.CENTER);
        lblDisclaimer.setForeground(WHITE_COLOR);
        contentPane.add(lblDisclaimer);


        // Add background image.
        contentPane.setOpaque(false);
        final JLabel bgLabel = new JLabel(bgIcon);

        // Do not pass Integer.MIN_VALUE directly here; it must be packaged in an Integer
        // instance.  Otherwise, GUI components will not draw unless moused over.
        getLayeredPane().add(bgLabel, Integer.valueOf(Integer.MIN_VALUE));

        bgLabel.setBounds(0, 0, bgIcon.getIconWidth(), bgIcon.getIconHeight());

        pack();
    }

    /**
     * Getter for monitor.
     * @return the MultiPhaseProgressMonitorWithETA in the lower section of this JFrame
     */
    public final MultiPhaseProgressMonitorWithETA getMonitor() {
        return monitor;
    }

    /**
     * Setter for monitor.
     * @param neoMonitor  the MultiPhaseProgressMonitorWithETA in the lower section of this JFrame
     */
    protected final void setMonitor(final MultiPhaseProgressMonitorWithETA neoMonitor) {
        this.monitor = neoMonitor;
    }
}
