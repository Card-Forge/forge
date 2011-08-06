package forge;

import java.util.LinkedList;
import java.util.Stack;

public class InputControl extends MyObservable implements java.io.Serializable {
    private static final long serialVersionUID = 3955194449319994301L;
    
    private Input             input;
    static int                n                = 0;
    private Stack<Input>      inputStack       = new Stack<Input>();
    private LinkedList<Input> resolvingQueue 	= new LinkedList<Input>();

    // todo: needs a bit more work to allow mana abilities to put Inputs "on hold" while they are paid and then reinstuted

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
    	resolvingQueue.add(in);	
        updateObservers();
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

        // todo: this resolving portion needs more work, but fixes Death Cloud issues 
		if (resolvingQueue.size() > 0) {
			if (input != null) {
				return input;
			}

			// if an SA is resolving, only change input for something that is part of the resolving SA
			changeInput(resolvingQueue.poll());
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
        
        if (Phase.GameBegins != 0 && AllZone.Phase.doPhaseEffects()){
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
        		// test this. probably should just call Input_Block and let block pass along?
                if(AllZone.Combat.getAttackers().length == 0){
                	if (AllZone.pwCombat.getAttackers().length != 0)
                		return new Input_Block_Planeswalker();
                	
                	// no active attackers, skip the Blocking phase
                	AllZone.Phase.setNeedToNextPhase(true);
                	return null;
                }
                else 
                	return new Input_Block();
        	}
        }
        
        else if(phase.equals(Constant.Phase.End_Of_Turn)){
            if (priority.isComputer()){
        		// AI passes priority in his end of turn phase to player automatically
        		AllZone.Phase.passPriority();
        		return null;
        	}
            // Players EOT can just fall through to general passpriority
        }
        
        else if(phase.equals(Constant.Phase.Cleanup))	// Player needs to discard
        	if (AllZone.Stack.size() == 0)	// fall through to resolve things like Madness
        		return new Input_Cleanup();

        // *********************
        // Special phases handled above, everything else is handled simply by priority
        
        if (priority.isHuman()){
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
