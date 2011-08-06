package forge;
public class Input_Before_Attack_Instant extends Input
{ 

	private static final long serialVersionUID = -6625873677267183150L;
	public void showMessage()
   {
	//GameActionUtil.executeExaltedEffects();
    
	AllZone.Combat.verifyCreaturesInPlay();
    CombatUtil.showCombat();

    ButtonUtil.enableOnlyOK();
    AllZone.Display.showMessage("Before Attack Phase: Play Instants and Abilities");
   }
   public void selectButtonOK()
   {   
    //AllZone.Phase.nextPhase();
    //for debugging: System.out.println("need to nextPhase(Input_Block_Instant.selectButtonOK) = true");
    AllZone.Phase.setNeedToNextPhase(true);
   }
   public void selectCard(Card card, PlayerZone zone)
   {
     InputUtil.playInstantAbility(card, zone);
   }//selectCard()
}