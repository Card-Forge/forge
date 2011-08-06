
package forge;

import com.esotericsoftware.minlog.Log;


public class ComputerAI_Input extends Input {
    private static final long serialVersionUID = -3091338639571662216L;
    
    private final Computer    computer;
    
    public ComputerAI_Input(Computer i_computer) {
        computer = i_computer;
    }
    
    //wrapper method that ComputerAI_StackNotEmpty class calls
    //ad-hoc way for ComptuerAI_StackNotEmpty to get to the Computer class
    public void stackNotEmpty() {
        computer.stack_not_empty();
    }
    
    @Override
    public void showMessage() {
    	/*
    	 * //put this back in
        ButtonUtil.disableAll();
        AllZone.Display.showMessage("Phase: "
                + AllZone.Phase.getPhase()
                + "\nAn error may have occurred. Please send the \"Stack Report\" and the \"Detailed Error Trace\" to the Forge forum.");
        */
        think();
    }//getMessage();
    
    public Computer getComputer() {
        return computer;
    }
    
    private void think() {
    	//todo: instead of setNextPhase, pass priority
        final String phase = AllZone.Phase.getPhase();
        
        if (AllZone.Stack.size() > 0)
        	computer.stack_not_empty();
        else if(phase.equals(Constant.Phase.Main1)) {
        	Log.debug("Computer main1");
        	computer.main1();
        }
        else if (phase.equals(Constant.Phase.Combat_Begin)){
        	computer.begin_combat();
        }
        else if(phase.equals(Constant.Phase.Combat_Declare_Attackers)) {
            computer.declare_attackers();
        } 
        else if(phase.equals(Constant.Phase.Combat_Declare_Attackers_InstantAbility)) {
            computer.declare_attackers_after();
        }        
        else if(phase.equals(Constant.Phase.Combat_Declare_Blockers_InstantAbility)) {
            computer.declare_blockers_after();
        }
        else if(phase.equals(Constant.Phase.Combat_End)) {
        	computer.end_of_combat();
        }
        else if(phase.equals(Constant.Phase.Main2)) {
        	Log.debug("Computer main2");
            computer.main2();
        }
        else
        	computer.stack_not_empty();

    }//think
}
