package forge;

import java.util.Stack;

public class InputControl extends MyObservable implements java.io.Serializable {
    private static final long serialVersionUID = 3955194449319994301L;
    
    private Input             input;
    static int                n                = 0;
    private Stack<Input>      inputStack       = new Stack<Input>();
    
    private boolean bResolving = false;
    public void setResolving(boolean b) { bResolving = b; }
    public boolean getResolving() { return bResolving; }
    
    // todo: needs a bit more work to allow mana abilities to put Inputs "on hold" while they are paid and then reinstuted
    
    public void setInput(final Input in) {
        if(bResolving || !(input == null || input instanceof Input_PassPriority)) 
        	inputStack.add(in);
        else 
        	input = in;
        updateObservers();
    }

    public Input getInput(){
    	return input;
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

        if (bResolving){
        	return null;
        }
        
        if (input != null)
        	return input;

        else if(inputStack.size() > 0) {		// incoming input to Control
            setInput(inputStack.pop());
            return input;
        }
        
        if (AllZone.Phase.doPhaseEffects())
        	AllZone.Phase.handleBeginPhase();
        
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
                if(AllZone.Combat.getAttackers().length == 0) 
                	return new Input_Block_Planeswalker();
                else 
                	return new Input_Block();
        	}
        }
        
        else if(phase.equals(Constant.Phase.End_Of_Turn)){
            if (playerTurn.isHuman()){
            	AllZone.Computer.getComputer().end_of_turn();
            	return null;
        	}
        	else if (priority.isComputer()){
        		// AI passes priority in his end of turn phase to player automatically
        		AllZone.Phase.passPriority();
        		return null;
        	}
        }
        
        else if(phase.equals(Constant.Phase.Cleanup))	// Player needs to discard
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
