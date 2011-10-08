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

import forge.AllZone;
import forge.Phase;
import forge.Player;
import forge.gui.skin.FButton;
import forge.gui.skin.FPanel;
import forge.properties.ForgeProps;
import forge.properties.NewConstants.LANG.WinLoseFrame.WINLOSETEXT;
import forge.quest.data.QuestMatchState;

import net.miginfocom.swing.MigLayout;

/** <p>WinLoseFrame.</p>
 * VIEW - Core display for win/lose UI shown after completing a game.
 * Uses handlers to customize central panel for various game modes. 
 *
 */
@SuppressWarnings("serial")
public class WinLoseFrame extends JFrame {

    private QuestMatchState matchState;
    private WinLoseModeHandler modeHandler;
    
    public FButton btnContinue;
    public FButton btnQuit;
    public FButton btnRestart;
    
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

        // Place all content in FPanel
        FPanel contentPanel = new FPanel(new MigLayout("wrap, fill, insets 20 0 10 10"));
        contentPanel.setBGImg(AllZone.getSkin().texture1);
        contentPanel.setBorder(new WinLoseBorder());
        getContentPane().add(contentPanel);        
        
        //Footer should be at least 150 to keep buttons in-pane on Mac OS X
        int HEAD_HEIGHT = 150;
        int FOOT_HEIGHT = 150;
        int FRAME_WIDTH_SMALL = 300;
        int FRAME_WIDTH_BIG = 600;
        
        // Head panel
        JPanel pnlHead = new JPanel(new MigLayout("wrap, fill"));
        pnlHead.setOpaque(false);
        contentPanel.add(pnlHead,"width " + FRAME_WIDTH_SMALL + "!, align center");

        lblTitle = new JLabel("WinLoseFrame > lblTitle is broken.");
        lblTitle.setForeground(Color.white); 
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        lblTitle.setFont(AllZone.getSkin().font1.deriveFont(Font.PLAIN,26));
        
        lblStats = new JLabel("WinLoseFrame > lblStats is broken.");
        lblStats.setForeground(Color.white); 
        lblStats.setHorizontalAlignment(SwingConstants.CENTER);
        lblStats.setFont(AllZone.getSkin().font1.deriveFont(Font.PLAIN,26));
        
        pnlHead.add(lblTitle, "growx");
        pnlHead.add(lblStats, "growx");
        
        // Custom display panel in center; populated later by mode handler.
        JScrollPane scroller = new JScrollPane();
        pnlCustom = new JPanel(new MigLayout("wrap, fillx"));
        pnlCustom.setBackground(new Color(111,87,59));
        pnlCustom.setForeground(Color.white);
        contentPanel.add(scroller,"w 96%!, align center, gapleft 2%");
        scroller.getViewport().add(pnlCustom);
        
        // Foot panel        
        JPanel pnlFoot = new JPanel(new MigLayout("wrap, fill, hidemode 3"));
        pnlFoot.setOpaque(false);
        contentPanel.add(pnlFoot,"width " + FRAME_WIDTH_SMALL + "!, align center");
       
        this.btnContinue    = new FButton("Continue");
        this.btnRestart     = new FButton("Restart");
        this.btnQuit        = new FButton("Quit");
        
        pnlFoot.add(btnContinue,"h 36:36, w 200!, gap 0 0 5 5, align center");
        pnlFoot.add(btnRestart,"h 36:36, w 200!, gap 0 0 5 5, align center");
        pnlFoot.add(btnQuit,"h 36:36, w 200!, gap 0 0 5 5, align center");        
        
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
        boolean hasContents = modeHandler.populateCustomPanel();
        if(!hasContents) { scroller.setVisible(false); }
        
        // Size and show frame
        Dimension screen = this.getToolkit().getScreenSize();
        Rectangle bounds = this.getBounds();
        
        if(hasContents) { 
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
        
        this.setBackground(AllZone.getSkin().bg1a);
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
        // Issue 147 - keep battlefield up following win/loss
        JFrame frame = (JFrame) AllZone.getDisplay();
        frame.dispose();
        frame.setEnabled(true);
        this.dispose();
        return frame;
    }
    
    private class WinLoseBorder extends AbstractBorder {        
        public void paintBorder(Component c, 
            Graphics g, int x, int y, int width, 
            int height) {
                g.setColor(AllZone.getSkin().txt1a);
                g.drawRect(x+1, y+1, width-3, height-3);
                g.setColor(AllZone.getSkin().bg1a);
                g.drawRect(x+3, y+3, width-7, height-7);
         }
    }
}
