package forge;
public class Computer_Draw extends Input
{
	private static final long serialVersionUID = -815953053902719496L;

	public void showMessage() 
    {
	  AllZone.GameAction.drawCard(Constant.Player.Computer);	
	  
	  //AllZone.Phase.nextPhase();
	  //for debugging: System.out.println("need to nextPhase(Computer_Draw.showMessage) = true");
	  AllZone.Phase.setNeedToNextPhase(true);
    }    
}
