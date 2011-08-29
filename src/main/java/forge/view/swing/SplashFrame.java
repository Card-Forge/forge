package forge.view.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import net.slightlymagic.braids.util.progress_monitor.BaseProgressMonitor;

/**
 * Shows the splash frame as the application starts.
 */
@SuppressWarnings("serial")
public class SplashFrame extends JFrame {

    // Inits: Visual changes can be made here.
    private static final String BG_ADDRESS = "res/images/ui/forgeSplash.jpg";
    
    private static final int    PADDING_X               = 20;
    private static final int    PADDING_Y               = 20;
    
    private static final int    BAR_HEIGHT_PX           = 57;
    private static final int    DISCLAIMER_HEIGHT_PX    = 20;
    private static final int    DISCLAIMER_TOP_PX       = 300;
    
    private static final int    DISCLAIMER_FONT_SIZE    = 9;
    private static final Color  DISCLAIMER_COLOR        = Color.white;
    
    private SplashModelProgressMonitor monitorModel = null;
    private SplashViewProgressMonitor monitorView = null;

    /**
     * <p>Create the frame; this <strong>must</strong> be called from an event
     * dispatch thread.</p>
     *
     * <!-- CheckStyle won't let me use at-throws. -->
     * Throws {@link IllegalStateException} if not called from an event
     * dispatch thread.
     */
    public SplashFrame() {
        super();

        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("SplashFrame() must be called from an event dispatch thread.");
        }

        setUndecorated(true);

        // Load icon and set preferred JFrame properties.
        final ImageIcon bgIcon = new ImageIcon(BG_ADDRESS);
        final int splashWidthPx = bgIcon.getIconWidth();
        final int splashHeightPx = bgIcon.getIconHeight();

        setMinimumSize(new Dimension(splashWidthPx, splashHeightPx));
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Insert JPanel to hold content above background
        final JPanel contentPane = new JPanel();
        setContentPane(contentPane);
        contentPane.setLayout(null);    

        // Add disclaimer
        final JLabel lblDisclaimer = new JLabel("<html><center>Forge is not affiliated in any way with Wizards of the Coast.<br>Forge is open source software, released under the GNU Public License.</center></html>");

        lblDisclaimer.setBounds(PADDING_X, DISCLAIMER_TOP_PX,
                splashWidthPx - (2 * PADDING_X), DISCLAIMER_HEIGHT_PX);

        lblDisclaimer.setFont(new Font("Tahoma", Font.PLAIN, DISCLAIMER_FONT_SIZE));
        lblDisclaimer.setHorizontalAlignment(SwingConstants.CENTER);
        lblDisclaimer.setForeground(DISCLAIMER_COLOR);
        contentPane.add(lblDisclaimer);
       
        // Instantiate model and view and tie together.
        monitorModel = new SplashModelProgressMonitor(1);
        monitorView = new SplashViewProgressMonitor();
    
        monitorModel.setCurrentView(monitorView);
        monitorView.setCurrentModel(monitorModel);
        
        // Add prog bar + message, bg image
        monitorView.displayUpdate("Assembling file list...");
        monitorView.setBounds(PADDING_X, splashHeightPx - PADDING_Y - BAR_HEIGHT_PX,
                splashWidthPx - (2 * PADDING_X), BAR_HEIGHT_PX);
        contentPane.add(monitorView);
        
        contentPane.setOpaque(false);
        final JLabel bgLabel = new JLabel(bgIcon);
        
        // Do not pass Integer.MIN_VALUE directly here; it must be packaged in an Integer
        // instance.  Otherwise, GUI components will not draw unless moused over.
        getLayeredPane().add(bgLabel, Integer.valueOf(Integer.MIN_VALUE));

        bgLabel.setBounds(0, 0, splashWidthPx, splashHeightPx);

        pack(); 
    }

    /**
     * Getter for progress bar view.
     * @return the SplashViewProgressMonitor progress bar used in the splash frame.
     */
    public final SplashViewProgressMonitor getMonitorView() {
        return monitorView;
    }
    
    /**
     * Getter for progress monitor model.
     * @return the BaseProgressMonitor model used in the splash frame.
     */
    public final BaseProgressMonitor getMonitorModel() {
        return monitorModel;
    }

}
