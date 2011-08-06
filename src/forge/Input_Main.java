
package forge;


public class Input_Main extends Input {
    private static final long serialVersionUID = -2162856359060870957L;
    
    //Input_Draw changes this
    //public static boolean canPlayLand;
    //public static boolean firstLandHasBeenPlayed;
    //public static int canPlayNumberOfLands;
    
    @Override
    public void showMessage() {
        ButtonUtil.enableOnlyOK();
        
        if(AllZone.Phase.getPhase().equals(Constant.Phase.Main1)) AllZone.Display.showMessage("Main 1 Phase: Play any card");
        else AllZone.Display.showMessage("Main 2 Phase: Play any card");
    }
    
    @Override
    public void selectButtonOK() {
        //AllZone.Phase.nextPhase();
        //for debugging: System.out.println("need to nextPhase(Input_Main.selectButtonOK) = true");
        AllZone.Phase.setNeedToNextPhase(true);
    }
    
    @Override
    public void selectCard(Card card, PlayerZone zone) {
        //these if statements cannot be combined
        if(card.isLand() && zone.is(Constant.Zone.Hand, AllZone.HumanPlayer)) {
            if(CardFactoryUtil.canHumanPlayLand()) {
                InputUtil.playAnyCard(card, zone);
                AllZone.GameAction.checkStateEffects();
            }

            //card might have cycling/transmute/etc.
            else {
                SpellAbility[] sa = card.getSpellAbility();
                if(sa.length > 0) {
                    int count = 0;
                    for(SpellAbility s:sa) {
                    	s.setActivatingPlayer(AllZone.HumanPlayer);
                        if(s.canPlay()) count++;
                    }
                    if(count > 0) InputUtil.playAnyCard(card, zone);
                }
            }
        } else {
            InputUtil.playAnyCard(card, zone);
        }
        AllZone.GameAction.checkStateEffects();
    }//selectCard()
}
