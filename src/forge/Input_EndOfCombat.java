
package forge;


//currently not used
public class Input_EndOfCombat extends Input {
    private static final long serialVersionUID = 1144173890819650789L;
    
    @Override
    public void showMessage() {
        updateGUI();
        
        ButtonUtil.enableOnlyOK();
        //String phase = AllZone.Phase.getPhase(); // unused
        //String player = AllZone.Phase.getActivePlayer(); // unused
        AllZone.Display.showMessage("End of Combat - Play Instants and Abilities");
    }
    
    @Override
    public void selectButtonOK() {
        updateGUI();
        
        //AllZone.Phase.nextPhase();
        //for debugging: System.out.println("need to nextPhase(Input_EOT.selectButtonOK) = true; note, this has not been tested, did it work?");
        AllZone.Phase.setNeedToNextPhase(true);
    }
    
    @Override
    public void selectCard(Card card, PlayerZone zone) {
        InputUtil.playInstantAbility(card, zone);
    }//selectCard()
    
    private void updateGUI() {
        AllZone.Computer_Play.updateObservers();
        AllZone.Human_Play.updateObservers();
        AllZone.Human_Hand.updateObservers();
    }
}
