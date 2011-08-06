package forge;
public class ComputerAI_Testing implements Computer 
{
    
	private int numberPlayLand = CardFactoryUtil.getCanPlayNumberOfLands(Constant.Player.Computer);
	
    //must shuffle this
    public Card[] getLibrary() {return new Card[] {};}

    public void stack_not_empty() 
    {
	System.out.println("Computer: not empty");
	//same as Input.stop() method
	//ends the method
	//different than methods because this isn't a phase like Main1 or Declare Attackers
	AllZone.InputControl.resetInput();
	AllZone.InputControl.updateObservers();    
    }
    
    public void main1() {AllZone.Phase.nextPhase();}
    
    public void declare_attackers_before()
    {
    	 AllZone.Phase.setNeedToNextPhase(true);
    }
    
    public void declare_attackers(){
    	
    	//AllZone.Phase.nextPhase();
    	//for debugging: System.out.println("need to nextPhase(ComputerAI_Testing.declare_attackers) = true");
        AllZone.Phase.setNeedToNextPhase(true);
    }

    public void declare_blockers(){
    	
    	//AllZone.Phase.nextPhase();
    	//for debugging: System.out.println("need to nextPhase(ComputerAI_Testing.declare_blockers) = true");
        AllZone.Phase.setNeedToNextPhase(true);
     }

    //can play Instants and Abilities
    public void declare_blockers_after(){
    	
    	//AllZone.Phase.nextPhase();
    	//for debugging: System.out.println("need to nextPhase(ComputerAI_Testing.declare_blockers_after) = true");
        AllZone.Phase.setNeedToNextPhase(true);
    }

    public void main2(){
    	
    	//AllZone.Phase.nextPhase();
    	//for debugging: System.out.println("need to nextPhase(ComputerAI_Testing.main2) = true");
        AllZone.Phase.setNeedToNextPhase(true);
    }

    //end of Human's turn
    public void end_of_turn(){
    	//AllZone.Phase.nextPhase();
    	//for debugging: System.out.println("need to nextPhase(ComputerAI_Testing.end_of_turn) = true");
        AllZone.Phase.setNeedToNextPhase(true);
        }
    
    public void addNumberPlayLands(int n)
    {
    	numberPlayLand += n;
    }
    
    public void setNumberPlayLands(int n)
    {
    	numberPlayLand = n;
    }
}