
package forge;


public abstract class Input implements java.io.Serializable {
    private static final long serialVersionUID = -6539552513871194081L;
    
    private boolean           isFree           = false;
    
    //showMessage() is always the first method called
    public void showMessage() {
        AllZone.Display.showMessage("Blank Input");
    }
    
    public void selectCard(Card c, PlayerZone zone) {}
    
    public void selectPlayer(final Player player) {
    	if(player.canChannel()) {
    		Card channel = player.getChannelCard();
    		if (GameActionUtil.showYesNoDialog(channel, "Pay 1 life?")) {
    			player.payLife(1, channel);
    			AllZone.ManaPool.addManaToFloating("1", channel);
    		}
    	}
    }
    
    public void selectButtonOK() {}
    
    public void selectButtonCancel() {}
    
    //helper methods, since they are used alot
    //to be used by anything in CardFactory like SetTargetInput
    //NOT TO BE USED by Input_Main or any of the "regular" Inputs objects that are not set using AllZone.InputControl.setInput(Input)
    final public void stop() {
        //clears a "temp" Input like Input_PayManaCost if there is one
        AllZone.InputControl.resetInput();
        
        if(AllZone.Phase.isNeedToNextPhase()) {
            // mulligan needs this to move onto next phase
            AllZone.Phase.setNeedToNextPhase(false);
            AllZone.Phase.nextPhase();
        }
    }
    
    //exits the "current" Input and sets the next Input
    final public void stopSetNext(Input in) {
        stop();
        AllZone.InputControl.setInput(in);
    }
    
    @Override
    public String toString() {
        return "blank";
    }//returns the Input name like "EmptyStack"
    
    public void setFree(boolean isFree) {
        this.isFree = isFree;
    }
    
    public boolean isFree() {
        return isFree;
    }
}
