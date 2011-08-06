package forge.gui.input;

import java.util.LinkedList;
import java.util.Stack;

import forge.AllZone;
import forge.Constant;
import forge.MyObservable;
import forge.Phase;
import forge.Player;

public class InputControl extends MyObservable implements java.io.Serializable {
    private static final long serialVersionUID = 3955194449319994301L;
    
    private Input             input;
    static int                n                = 0;
    private Stack<Input>      inputStack       = new Stack<Input>();
    private Stack<Input>	  resolvingStack 	= new Stack<Input>();
    private LinkedList<Input> resolvingQueue 	= new LinkedList<Input>();

    public void setInput(final Input in) {
    	if(AllZone.Stack.getResolving() || !(input == null || input instanceof Input_PassPriority)) 
        	inputStack.add(in);
        else 
        	input = in;
        updateObservers();
    }
    
    public void setInput(final Input in, boolean bAddToResolving) {
    	// Make this
    	if (!bAddToResolving){
    		setInput(in);
    		return;
    	}

    	Input old = input;
    	resolvingStack.add(old);
    	changeInput(in);
    }
    
    private void changeInput(final Input in){
    	input = in;
        updateObservers();
    }
    
    public Input getInput(){
    	return input;
    }
    
    public void clearInput() {
        input = null;
        resolvingQueue.clear();
        inputStack.clear();
    }
    
    public void resetInput() {
        input = null;
        updateObservers();
    }
    
    public void resetInput(boolean update) {
        input = null;
        if (update)
        	updateObservers();
    }

    public Input updateInput() {
        final String phase = AllZone.Phase.getPhase();
        final Player playerTurn = AllZone.Phase.getPlayerTurn();
        final Player priority = AllZone.Phase.getPriorityPlayer();

        // TODO: this resolving portion needs more work, but fixes Death Cloud issues 
		if (resolvingStack.size() > 0) {
			if (input != null) {				
				return input;
			}

			// if an SA is resolving, only change input for something that is part of the resolving SA
			changeInput(resolvingStack.pop());
			return input;
		}

	    if (AllZone.Stack.getResolving())
        	return null;

        
        if (input != null)
        	return input;

        else if(inputStack.size() > 0) {		// incoming input to Control
        	changeInput(inputStack.pop());
            return input;
        }
        
        if (Phase.getGameBegins() != 0 && AllZone.Phase.doPhaseEffects()){
        	// Handle begin phase stuff, then start back from the top
        	AllZone.Phase.handleBeginPhase();
        	return updateInput();
        }
        
    	// If the Phase we're in doesn't allow for Priority, return null to move to next phase
        if (AllZone.Phase.isNeedToNextPhase())	
        	return null;
        
        // Special Inputs needed for the following phases:        
        if(phase.equals(Constant.Phase.Combat_Declare_Attackers)) {
        	AllZone.Stack.freezeStack();
        	
        	if (playerTurn.isHuman())
        		return new Input_Attack();
        }
        
        else if(phase.equals(Constant.Phase.Combat_Declare_Blockers)) {
        	AllZone.Stack.freezeStack();
            if (playerTurn.isHuman()){
            	AllZone.Computer.getComputer().declare_blockers();
            	return null;
        	}
        	else{
                if(AllZone.Combat.getAttackers().length == 0){
                	// no active attackers, skip the Blocking phase
                	AllZone.Phase.setNeedToNextPhase(true);
                	return null;
                }
                else 
                	return new Input_Block();
        	}
        }
        
        else if(phase.equals(Constant.Phase.Cleanup))	// Player needs to discard
        	if (AllZone.Stack.size() == 0)	// fall through to resolve things like Madness
        		return new Input_Cleanup();

        // *********************
        // Special phases handled above, everything else is handled simply by priority
        
        if (priority.isHuman()){
        	boolean skip = AllZone.Phase.doSkipPhase();
        	AllZone.Phase.setSkipPhase(false);
	    	if(AllZone.Stack.size() == 0 && !AllZone.Display.stopAtPhase(playerTurn, phase) && skip) {
            	AllZone.Phase.passPriority();
            	return null;
            }
	    	else
	    		return new Input_PassPriority();
    	}
        
        else if (playerTurn.isComputer())
    		return AllZone.Computer;
    	else{
        	AllZone.Computer.getComputer().stack_not_empty();
        	return null;
        }
    }//getInput()
}//InputControl
