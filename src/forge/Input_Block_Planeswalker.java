
package forge;


import java.util.ArrayList;


public class Input_Block_Planeswalker extends Input {
    private static final long serialVersionUID = 8504632360578751473L;
    
    private Card              currentAttacker  = null;
    private ArrayList<Card>   allBlocking      = new ArrayList<Card>();
    
    @Override
    public void showMessage() {
        //for Castle Raptors, since it gets a bonus if untapped
        for(String effect:AllZone.StaticEffects.getStateBasedMap().keySet()) {
            Command com = GameActionUtil.commands.get(effect);
            com.execute();
        }
        GameActionUtil.executeCardStateEffects();
        
        //could add "Reset Blockers" button
        ButtonUtil.enableOnlyOK();
        
        if(currentAttacker == null) {
        	//Lure
        	CardList attackers = new CardList(AllZone.Combat.getAttackers());
        	for(Card attacker:attackers) {
        		if(attacker.hasKeyword("All creatures able to block CARDNAME do so.")) {
        			CardList bls = AllZoneUtil.getCreaturesInPlay(AllZone.HumanPlayer);
        			for(Card bl:bls) {
        				if(CombatUtil.canBlock(attacker, bl)) {
        					allBlocking.add(bl);
        					AllZone.Combat.addBlocker(attacker, bl);
        				}
        			}
        		}
        	}
        	
        	AllZone.Display.showMessage("Planeswalker Combat\r\nTo Block, click on your Opponents attacker first , then your blocker(s)");
        }
        else {
        	String attackerName = currentAttacker.isFaceDown() ? "Morph" : currentAttacker.getName();
        	AllZone.Display.showMessage("Select a creature to block " + attackerName + " ("
        			+ currentAttacker.getUniqueNumber() + ") ");
        }
        
        CombatUtil.showCombat();
    }
    
    @Override
    public void selectButtonOK() {
       	if (AllZone.Combat.getAttackers().length > 0)
    		AllZone.Phase.setCombat(true);
        ButtonUtil.reset();
        
        //AllZone.Phase.nextPhase();
        //for debugging: System.out.println("need to nextPhase(Input_Block_Planeswalker.selectButtonOK) = true; Note, this has not been tested, did it work?");
        AllZone.Phase.setNeedToNextPhase(true);
        this.stop();
    }
    
    @Override
    public void selectCard(Card card, PlayerZone zone) {
        //is attacking?
        if(CardUtil.toList(AllZone.pwCombat.getAttackers()).contains(card)) {
            currentAttacker = card;
        } else if(zone.is(Constant.Zone.Battlefield, AllZone.HumanPlayer) && card.isCreature() && card.isUntapped()
                && CombatUtil.canBlock(currentAttacker, card)) {
            if(currentAttacker != null && (!allBlocking.contains(card))) {
                allBlocking.add(card);
                AllZone.pwCombat.addBlocker(currentAttacker, card);
            }
        }
        showMessage();
    }//selectCard()
}
