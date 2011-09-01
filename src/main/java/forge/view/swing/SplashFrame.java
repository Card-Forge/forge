package forge.view.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import net.slightlymagic.braids.util.progress_monitor.BraidsProgressMonitor;

/**
 * Shows the splash frame as the application starts.
 */
@SuppressWarnings("serial")
public class SplashFrame extends JFrame {

    // Inits: Visual changes can be made here.
    private static final String BG_ADDRESS = "res/images/ui/forgeSplash.jpg";
    
    private static final int    BAR_PADDING_X           = 20;
    private static final int    BAR_PADDING_Y           = 20;
    private static final int    BAR_HEIGHT              = 57;
    
    private static final int    DISCLAIMER_HEIGHT       = 20;
    private static final int    DISCLAIMER_TOP          = 300;
    private static final int    DISCLAIMER_FONT_SIZE    = 9;
    private static final Color  DISCLAIMER_COLOR        = Color.white;
    
    //private static final int    CLOSEBTN_PADDING_X      = 15;
    private static final int    CLOSEBTN_PADDING_Y      = 15;
    private static final int    CLOSEBTN_SIDELENGTH     = 15;
    private static final Color  CLOSEBTN_COLOR          = new Color(215,208,188);
    
    private SplashProgressModel monitorModel = null;
    private SplashProgressComponent monitorView = null;
    
    private boolean SplashHasBeenClosed = false;

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

        lblDisclaimer.setBounds(0, DISCLAIMER_TOP,
                splashWidthPx, DISCLAIMER_HEIGHT);

        lblDisclaimer.setFont(new Font("Tahoma", Font.PLAIN, DISCLAIMER_FONT_SIZE));
        lblDisclaimer.setHorizontalAlignment(SwingConstants.CENTER);
        lblDisclaimer.setForeground(DISCLAIMER_COLOR);
        contentPane.add(lblDisclaimer);
       
        // Add close button
        final JButton btnClose = new JButton("X");
        btnClose.setBounds(splashWidthPx- (2 * CLOSEBTN_PADDING_Y),
                CLOSEBTN_PADDING_Y,CLOSEBTN_SIDELENGTH,CLOSEBTN_SIDELENGTH);
        btnClose.setForeground(CLOSEBTN_COLOR);
        btnClose.setBorder(BorderFactory.createLineBorder(CLOSEBTN_COLOR));
        btnClose.setOpaque(false);
        btnClose.setBackground(new Color(0,0,0));
        btnClose.setFocusPainted(false);
        contentPane.add(btnClose);
        
        // Action handler: button hover effect
        btnClose.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnClose.setBorder(BorderFactory.createLineBorder(Color.white));
                btnClose.setForeground(Color.white);
            }
            
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnClose.setBorder(BorderFactory.createLineBorder(CLOSEBTN_COLOR));
                btnClose.setForeground(CLOSEBTN_COLOR);
            }
        });
        
        // Action handler: button close
        btnClose.addActionListener(new closeAction());
        
        // Action handler: esc key close
        contentPane.getInputMap().put(
                KeyStroke.getKeyStroke( "ESCAPE" ), "escAction" );

        contentPane.getActionMap().put("escAction", new closeAction());
        
        
        // Instantiate model and view and tie together.
        monitorModel = new SplashProgressModel();
        monitorView = new SplashProgressComponent();
    
        monitorModel.setCurrentView(monitorView);
        monitorView.setCurrentModel(monitorModel);
        
        // Add prog bar + message, bg image
        monitorView.displayUpdate("Assembling file list...");
        monitorView.setBounds(BAR_PADDING_X, splashHeightPx - BAR_PADDING_Y - BAR_HEIGHT,
                splashWidthPx - (2 * BAR_PADDING_X), BAR_HEIGHT);
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
    public final SplashProgressComponent getMonitorView() {
        return monitorView;
    }
    
    /**
     * Getter for progress monitor model.
     * @return the BraidsProgressMonitor model used in the splash frame.
     */
    public final BraidsProgressMonitor getMonitorModel() {
        return monitorModel;
    }
    
    /**
     * Returns state of splash frame, to determine if GUI should continue loading.
     * @return SplashHasBeenClosed boolean.
     */
    public final boolean getSplashHasBeenClosed() {
        return SplashHasBeenClosed;
    }
    
    /**
     * Sets state of splash frame, to determine if GUI should continue loading.
     * @param SplashHasBeenClosed boolean.
     */
    public final void setSplashHasBeenClosed(boolean neoState) {
        SplashHasBeenClosed = neoState;
    }
    
    /**
     * <p>closeAction</p>
     * Closes the splash frame by toggling "has been closed" switch
     * (to cancel preloading) and dispose() (to discard JFrame).
     * @param SplashHasBeenClosed boolean.
     */ 
    private class closeAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            setSplashHasBeenClosed(true);
            dispose();
        }
    }

}
