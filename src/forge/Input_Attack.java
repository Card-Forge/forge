
package forge;


public class Input_Attack extends Input {
    private static final long serialVersionUID = 7849903731842214245L;
    
    @Override
    public void showMessage() {
    	// TODO: still seems to have some issues with multiple planeswalkers
    	
        ButtonUtil.enableOnlyOK();

        Object o = AllZone.Combat.nextDefender();        
        if (o == null){
        	return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Declare Attackers: Select Creatures to Attack ");
        sb.append(o.toString());
        
        AllZone.Display.showMessage(sb.toString());
        
        if(AllZone.Combat.getRemainingDefenders() == 0) {
        	// Nothing left to attack, has to attack this defender
            CardList possibleAttackers = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer);
            possibleAttackers = possibleAttackers.getType("Creature");
            for(int i = 0; i < possibleAttackers.size(); i++) {
                Card c = possibleAttackers.get(i);
                if(c.getKeyword().contains("CARDNAME attacks each turn if able.") && CombatUtil.canAttack(c) && !c.isAttacking()) {
                    AllZone.Combat.addAttacker(c);
                    //if(!c.getKeyword().contains("Vigilance")) 
                    //	c.tap();
                }
            }
        }
    }
    
    @Override
    public void selectButtonOK() {
    	if (AllZone.Combat.getAttackers().length > 0)
    		AllZone.Phase.setCombat(true);
    
    	if (AllZone.Combat.getRemainingDefenders() != 0)
    		AllZone.Phase.repeatPhase();
    	
    	AllZone.Phase.setNeedToNextPhase(true);
    	AllZone.InputControl.resetInput();
    }
    
    @Override
    public void selectCard(Card card, PlayerZone zone) {
    	if (card.isAttacking() || card.getController().isComputer())
    		return;
    	
        if(zone.is(Constant.Zone.Battlefield, AllZone.HumanPlayer) && CombatUtil.canAttack(card)) {
            
        	// todo add the propaganda code here and remove it in Phase.nextPhase()
        	// if (!CombatUtil.checkPropagandaEffects(card))
        	// 		return;
        	         
            AllZone.Combat.addAttacker(card);
            AllZone.Human_Battlefield.updateObservers();	// just to make sure the attack symbol is marked
            
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
