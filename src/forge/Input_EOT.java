package forge;
public class Input_EOT extends Input
{
  private static final long serialVersionUID = 5765929456837761648L;

  public void showMessage()
  {
    updateGUI();

    ButtonUtil.enableOnlyOK();
    //String phase = AllZone.Phase.getPhase(); // unused
    //String player = AllZone.Phase.getActivePlayer(); // unused
    AllZone.Display.showMessage("Computer's End of Turn - Play Instants and Abilities");
  }
  public void selectButtonOK()
  {
    updateGUI();
    
    //AllZone.Phase.nextPhase();
    //for debugging: System.out.println("need to nextPhase(Input_EOT.selectButtonOK) = true; note, this has not been tested, did it work?");
    AllZone.Phase.setNeedToNextPhase(true);
  }
  public void selectCard(Card card, PlayerZone zone)
  {
    InputUtil.playInstantAbility(card, zone);
  }//selectCard()
  private void updateGUI()
  {
    AllZone.Computer_Play.updateObservers();
    AllZone.Human_Play.updateObservers();
    AllZone.Human_Hand.updateObservers();
  }
}