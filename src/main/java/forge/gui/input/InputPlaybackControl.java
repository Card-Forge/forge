package forge.gui.input;

import forge.control.FControl;
import forge.control.FControlGamePlayback;
import forge.view.ButtonUtil;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class InputPlaybackControl extends InputSyncronizedBase implements InputSynchronized {
    private static final long serialVersionUID = 7979208993306642072L;

    FControlGamePlayback control;
    
    private boolean isPaused = false;
    private boolean isFast = false;
    
    /**
     * TODO: Write javadoc for Constructor.
     * @param fControlGamePlayback
     */
    public InputPlaybackControl(FControlGamePlayback fControlGamePlayback) {
        control = fControlGamePlayback;
    }

    /* (non-Javadoc)
     * @see forge.gui.input.InputBase#showMessage()
     */
    @Override
    protected void showMessage() {
        setPause(false);
        ButtonUtil.enableAllFocusOk();
    }
    
    private void setPause(boolean pause) {
        isPaused = pause; 
        if ( isPaused ) 
            ButtonUtil.setButtonText("Resume", "Step");
        else  {
            ButtonUtil.setButtonText("Pause", isFast ? "1x Speed" : "10x Faster");
            showMessage("Press pause to pause game.");
        }
    }
    
    public void onGamePaused() {
        showMessage(getTurnPhasePriorityMessage(FControl.instance.getObservedGame()));
    }
    
    @Override
    protected void onOk() {
        if ( isPaused ) {
            control.resume();
            setPause(false);
        } else {
            control.pause();
            setPause(true);
        }
    }
    

    @Override
    protected void onCancel() {
        if ( isPaused ) {
            control.singleStep();
        } else {  
            isFast = !isFast;
            control.setSpeed(isFast);
            setPause(isPaused); // update message
        }

    }

}
