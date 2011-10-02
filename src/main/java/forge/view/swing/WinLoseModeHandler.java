package forge.view.swing;

import forge.AllZone;
import forge.Constant;

/** 
 * <p>WinLoseModeHandler.</p>
 * 
 * Superclass of custom mode handling for win/lose UI.  Can add swing components
 * to custom center panel. Custom mode handling for quest, puzzle, etc. 
 * should extend this class.
 *
 */
public class WinLoseModeHandler {
    
    protected WinLoseFrame view;
    
    /**
     * <p>actionOnQuit.</p>
     * Action performed when "continue" button is pressed in default win/lose UI.
     * 
     */
    public void actionOnContinue() {
        
    }
    
    /**
     * <p>actionOnQuit.</p>
     * Action performed when "quit" button is pressed in default win/lose UI.
     * 
     */
    public void actionOnQuit() { 
        if (System.getenv("NG2") != null) {
            if (System.getenv("NG2").equalsIgnoreCase("true")) {
                String argz[] = {};
                Gui_HomeScreen.main(argz);
            } else {
                new OldGuiNewGame();
            }
        } else {
            new OldGuiNewGame();
        }
    }
    
    /**
     * <p>actionOnRestart.</p>
     * Action performed when "restart" button is pressed in default win/lose UI.
     * 
     */
    public void actionOnRestart() {
        
    }
    
    /**
     * <p>startNextRound.</p>
     * Either continues or restarts a current game. May be overridden
     * for use with other game modes.
     *
     * @param e a {@link java.awt.event.ActionEvent} object.
     */
    public void startNextRound() {
        AllZone.getGameAction().newGame(Constant.Runtime.HumanDeck[0], Constant.Runtime.ComputerDeck[0]);
    }
    
    /**
     * <p>populateCustomPanel.</p>
     * May be overridden as required by various mode handlers to show
     * custom information in center panel. Default configuration is empty.
     * 
     * @param boolean indicating if custom panel has contents.
     */
    public boolean populateCustomPanel() {
        return false;
    }
    
    /**
     * <p>setView.</p>
     * Links win/lose swing frame to mode handler, mostly to allow
     * direct manipulation of custom center panel.
     * 
     */
    public void setView(WinLoseFrame wlh) {
        view = wlh;  
    }
}
