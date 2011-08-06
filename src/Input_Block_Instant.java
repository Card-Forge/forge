public class Input_Block_Instant extends Input
{
  private static final long serialVersionUID = 6024555691502280746L;

  public void showMessage()
  {
	//GameActionUtil.executeExaltedEffects();
    
	AllZone.Combat.verifyCreaturesInPlay();
    CombatUtil.showCombat();

    ButtonUtil.enableOnlyOK();
    AllZone.Display.showMessage("Declare Blockers: Play Instants and Abilities");
  }
  public void selectButtonOK()
  {
     AllZone.Combat.setAssignedFirstStrikeDamage();
     AllZone.pwCombat.setAssignedFirstStrikeDamage();

   
    //AllZone.Phase.nextPhase();
    //for debugging: System.out.println("need to nextPhase(Input_Block_Instant.selectButtonOK) = true");
    AllZone.Phase.setNeedToNextPhase(true);
  }
  public void selectCard(Card card, PlayerZone zone)
  {
    InputUtil.playInstantAbility(card, zone);
  }//selectCard()
}