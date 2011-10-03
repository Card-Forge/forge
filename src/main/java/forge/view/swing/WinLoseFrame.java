package forge.view.swing;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import forge.AllZone;
import forge.Phase;
import forge.Player;
import forge.properties.ForgeProps;
import forge.properties.NewConstants.LANG.WinLoseFrame.WINLOSETEXT;
import forge.quest.data.QuestMatchState;

import net.miginfocom.swing.MigLayout;

/** <p>WinLoseFrame.</p>
 * Core display for win/lose UI shown after completing a game.
 * Uses handlers to customize central panel for various game modes. 
 *
 */
@SuppressWarnings("serial")
public class WinLoseFrame extends JFrame {

    private QuestMatchState matchState;
    private WinLoseModeHandler modeHandler;
    
    public JButton btnContinue;
    public JButton btnQuit;
    public JButton btnRestart;
    
    public JLabel lblTitle;
    public JLabel lblStats;
    
    public JPanel pnlCustom;
    
    /**
     * <p>WinLoseFrame.</p>
     * Starts win/lose UI with default display.
     * 
     */
    public WinLoseFrame() {
        this(new WinLoseModeHandler());
    }
    
    /**
     * <p>WinLoseFrame.</p>
     * Starts the standard win/lose UI with custom display 
     * for different game modes (quest, puzzle, etc.)
     * 
     * @param mh {@link forge.view.swing.WinLoseModeHandler}
     */
    public WinLoseFrame(WinLoseModeHandler mh) {
        super();
        
        // modeHandler handles the unique display for different game modes.
        modeHandler = mh;
        modeHandler.setView(this);
        matchState = AllZone.getMatchState();
        Container contentPane = this.getContentPane();
        contentPane.setLayout(new MigLayout("wrap, fill"));
        contentPane.setBackground(new Color(16,28,50));
        
        //This needs to be at least 150, or the quit button is off the pane on Mac OS X
        int HEAD_HEIGHT = 150;
        int FOOT_HEIGHT = 150;
        int FRAME_WIDTH_SMALL = 300;
        int FRAME_WIDTH_BIG = 600;
        
        // Head panel
        JPanel pnlHead = new JPanel(new MigLayout("wrap, fill"));
        pnlHead.setOpaque(false);
        this.add(pnlHead,"width " + FRAME_WIDTH_SMALL + "!, align center");

        lblTitle = new JLabel("WinLoseFrame > lblTitle is broken.");
        lblTitle.setForeground(Color.white); 
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        lblTitle.setFont(new Font("Tahoma", Font.PLAIN, 26));
        
        lblStats = new JLabel("WinLoseFrame > lblStats is broken.");
        lblStats.setForeground(Color.white); 
        lblStats.setHorizontalAlignment(SwingConstants.CENTER);
        lblStats.setFont(new Font("Tahoma", Font.ITALIC, 21));
        
        pnlHead.add(lblTitle, "growx");
        pnlHead.add(lblStats, "growx");
        
        // Custom display panel in center; populated later by mode handler.
        JScrollPane scroller = new JScrollPane();
        pnlCustom = new JPanel(new MigLayout("wrap, fillx"));
        pnlCustom.setBackground(new Color(111,87,59));
        pnlCustom.setForeground(Color.white);
        this.add(scroller);
        scroller.getViewport().add(pnlCustom);
        
        // Foot panel        
        JPanel pnlFoot = new JPanel(new MigLayout("wrap, fill, hidemode 3"));
        pnlFoot.setOpaque(false);
        this.add(pnlFoot,"width " + FRAME_WIDTH_SMALL + "!, align center");
       
        this.btnContinue    = new WinLoseButton("Continue");
        this.btnRestart     = new WinLoseButton("Restart");
        this.btnQuit        = new WinLoseButton("Quit");
        
        pnlFoot.add(btnContinue,"height 30!, gap 0 0 5 5, align center");
        pnlFoot.add(btnRestart,"height 30!, gap 0 0 5 5, align center");
        pnlFoot.add(btnQuit,"height 30!, gap 0 0 5 5, align center");        
        
        // Button actions
        btnQuit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(final ActionEvent e) { btnQuit_actionPerformed(e); }
        });
        
        btnContinue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(final ActionEvent e) { btnContinue_actionPerformed(e); }
        });
        
        btnRestart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(final ActionEvent e) { btnRestart_actionPerformed(e); }
        });

        // End game and set state of "continue" button
        Phase.setGameBegins(0);
        
        if (matchState.isMatchOver()) {
            btnContinue.setEnabled(false);
            btnQuit.grabFocus();
        }

        // Show Wins and Loses
        Player human = AllZone.getHumanPlayer(); 
        int humanWins = matchState.countGamesWonBy(human.getName());
        int humanLosses = matchState.getGamesPlayedCount() - humanWins;  
        
        lblStats.setText(ForgeProps.getLocalized(WINLOSETEXT.WON) + humanWins
                + ForgeProps.getLocalized(WINLOSETEXT.LOST) + humanLosses);

        // Show "You Won" or "You Lost"
        if (matchState.hasWonLastGame(human.getName())) {
            lblTitle.setText(ForgeProps.getLocalized(WINLOSETEXT.WIN));
        } else {
            lblTitle.setText(ForgeProps.getLocalized(WINLOSETEXT.LOSE));
        }
        
        // Populate custom panel, if necessary.
        boolean hasStuff = modeHandler.populateCustomPanel();
        if(!hasStuff) { scroller.setVisible(false); }
        
        // Size and show frame
        Dimension screen = this.getToolkit().getScreenSize();
        Rectangle bounds = this.getBounds();
        
        if(hasStuff) { 
            bounds.height = screen.height - 150;
            scroller.setPreferredSize(new Dimension(FRAME_WIDTH_BIG,
                    screen.height - HEAD_HEIGHT - FOOT_HEIGHT));
            bounds.width = FRAME_WIDTH_BIG;
            bounds.x = (screen.width - FRAME_WIDTH_BIG)/2;
            bounds.y = (screen.height - bounds.height)/2;
        }
        else {
            bounds.height = HEAD_HEIGHT + FOOT_HEIGHT;
            bounds.width = FRAME_WIDTH_SMALL;
            bounds.x = (screen.width - FRAME_WIDTH_SMALL)/2;
            bounds.y = (screen.height - bounds.height)/2;
        }
        
        this.setBounds(bounds);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setUndecorated(true);
        this.setVisible(true);
    }
    
    /**
     * <p>continueButton_actionPerformed.</p>
     *
     * @param e a {@link java.awt.event.ActionEvent} object.
     */
    final void btnContinue_actionPerformed(ActionEvent e) {
        closeWinLoseFrame();
        AllZone.getDisplay().setVisible(true);
        modeHandler.startNextRound();
    }
    
    /**
     * <p>restartButton_actionPerformed.</p>
     *
     * @param e a {@link java.awt.event.ActionEvent} object.
     */
    final void btnRestart_actionPerformed(ActionEvent e) {
        closeWinLoseFrame();        
        AllZone.getDisplay().setVisible(true);
        matchState.reset();
        modeHandler.startNextRound();
    }
    
    /**
     * <p>btnQuit_actionPerformed.</p>
     *
     * @param e a {@link java.awt.event.ActionEvent} object.
     */
    final void btnQuit_actionPerformed(ActionEvent e) {
        closeWinLoseFrame();        
        matchState.reset();
        modeHandler.actionOnQuit();            

        // clear Image caches, so the program doesn't get slower and slower
        // not needed with soft values - will shrink as needed
        // ImageUtil.rotatedCache.clear();
        // ImageCache.cache.clear();
    }
    
    /**
     * <p>closeWinLoseFrame.</p>
     * Disposes WinLoseFrame UI.
     * 
     * @return {@link javax.swing.JFrame} display frame
     */
    final JFrame closeWinLoseFrame() {
        // issue 147 - keep battlefield up following win/loss
        JFrame frame = (JFrame) AllZone.getDisplay();
        frame.dispose();
        frame.setEnabled(true);
        this.dispose();
        return frame;
    }
    
    /**
     * <p>WinLoseButton.</p>
     * Private button class to standardize buttons.
     * 
     */
    private class WinLoseButton extends JButton {
        WinLoseButton(String msg) {
            super(msg);
            this.setFont(new Font("Tahoma", Font.PLAIN, 14));
            this.setOpaque(false);
            this.setPreferredSize(new Dimension(125,30));
        }
    }
}
