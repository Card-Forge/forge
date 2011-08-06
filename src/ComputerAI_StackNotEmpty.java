public class ComputerAI_StackNotEmpty extends Input
{
 	private static final long serialVersionUID = -3995969852275185353L;

	public void showMessage() 
    {
	AllZone.Display.showMessage("Phase: " +AllZone.Phase.getPhase() +"\nComputer is thinking");    
	AllZone.Computer.stackNotEmpty();
    }
}
