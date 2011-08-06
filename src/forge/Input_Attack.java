
package forge;


public class Input_Attack extends Input {
    private static final long serialVersionUID = 7849903731842214245L;
    
    @Override
    public void showMessage() {
        ButtonUtil.enableOnlyOK();
        AllZone.Display.showMessage("Declare Attackers: Select creatures that you want to attack with");
        
        PlayerZone play = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
        CardList creats = new CardList(play.getCards());
        creats = creats.getType("Creature");
        
        if(getPlaneswalker() == null) {
            for(int i = 0; i < creats.size(); i++) {
                Card c = creats.get(i);
                if(CombatUtil.canAttack(c) && c.getKeyword().contains("CARDNAME attacks each turn if able.")) {
                    
                    AllZone.Combat.addAttacker(c);
                    if(!c.getKeyword().contains("Vigilance")) c.tap();
                }
            }
        }
    }
    
    @Override
    public void selectButtonOK() {
    	if (AllZone.Combat.getAttackers().length > 0)
    		AllZone.Phase.setCombat(true);
    	
        Card check = getPlaneswalker();
        if(check == null) {
            
            //AllZone.Phase.nextPhase();
            //for debugging: System.out.println("need to nextPhase(Input_Attack.selectButtonOK,check==null) = true");
            AllZone.Phase.setNeedToNextPhase(true);
        } else {
            AllZone.pwCombat.setPlaneswalker(check);
            AllZone.InputControl.setInput(new Input_Attack_Planeswalker());
        }
    }
    
    //return Computer's planeswalker if there is one
    //just returns 1, does not return multiple planeswalkers
    private Card getPlaneswalker() {
        CardList c = new CardList(AllZone.Computer_Play.getCards());
        c = c.getType("Planeswalker");
        
        if(c.isEmpty()) return null;
        
        return c.get(0);
    }
    
    @Override
    public void selectCard(Card card, PlayerZone zone) {
        if(zone.is(Constant.Zone.Play, AllZone.HumanPlayer) && card.isCreature() && card.isUntapped()
                && CombatUtil.canAttack(card)) {
            
            if(!card.getKeyword().contains("Vigilance")) {
                card.tap();
                //otherwise cards stay untapped, not sure why this is needed but it works
                AllZone.Human_Play.updateObservers();
            }
            AllZone.Combat.addAttacker(card);
            
            //for Castle Raptors, since it gets a bonus if untapped
            for(String effect:AllZone.StaticEffects.getStateBasedMap().keySet()) {
                Command com = GameActionUtil.commands.get(effect);
                com.execute();
            }
            GameActionUtil.executeCardStateEffects();
            
            CombatUtil.showCombat();
        }
    }//selectCard()
    
    public void unselectCard(Card card, PlayerZone zone) {

    }
}
