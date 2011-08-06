package forge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

//AB:GainControl|ValidTgts$Creature|TgtPrompt$Select target legendary creature|LoseControl$[Untap],[PowerGT],[LoseControl]|UntilEOT$True|SpellDescription$Gain control of target xxxxxxx

//GainControl specific params:
//	LoseControl - the lose control conditions (as a comma separated list)
//			-Untap
//			-LoseControl
//			-PowerGT - (not implemented yet for Old Man of the Sea)
//	UntilEOT - set to True if this only lasts to end of turn (not implemented yet)
//	AddKWs	- Keywords to add to the controlled card (like Haste, Sacrifice CARDNAME at EOT, any standard keyword) - not implemented yet
//  OppChoice - set to True if opponent chooses creature (for Preacher) - not implemented yet

public class AbilityFactory_GainControl {
	
	private final Card movedCards[] = new Card[1];

	private AbilityFactory AF = null;
	private HashMap<String,String> params = null;
	private Card hostCard = null;
	private ArrayList<String> lose = null;
	
	public AbilityFactory_GainControl(AbilityFactory newAF) {
		AF = newAF;
		params = AF.getMapParams();
		hostCard = AF.getHostCard();
		lose = new ArrayList<String>(Arrays.asList(params.get("LoseControl").split(",")));
	}
	
	public SpellAbility getSpell() {
        SpellAbility spControl = new Spell(hostCard, AF.getAbCost(), AF.getAbTgt()) {
			private static final long serialVersionUID = 3125489644424832311L;
			
			@Override
			public boolean canPlayAI() {
            	return doTgtAI(this);
            }
            
			@Override
            public void resolve() {
            	doResolve(this);
            }//resolve
            
            /*@Override
			public String getStackDescription(){
				 StringBuilder sb = new StringBuilder();
				 String name = AF.getHostCard().getName();
				 sb.append(name).append(" - targeting ");
				 Card tgt = getTargetCard();
				 if (tgt != null) sb.append(tgt.getName());
				 return sb.toString();
			}*/
        };//SpellAbility
        
        return spControl;
	}

	public SpellAbility getAbility() {
        
        	final SpellAbility abControl = new Ability_Activated(hostCard, AF.getAbCost(), AF.getAbTgt()) {
				private static final long serialVersionUID = -4384705198674678831L;

				@Override
    			public boolean canPlay(){
    				return super.canPlay();
    			}

    			@Override
    			public boolean canPlayAI() {
    				return doTgtAI(this);
    			}

    			@Override
    			public void resolve() {
    				doResolve(this);
    				hostCard.setAbilityUsed(hostCard.getAbilityUsed() + 1);
    			}
    			
    			@Override
    			public String getStackDescription(){
    				 StringBuilder sb = new StringBuilder();
    				 String name = AF.getHostCard().getName();
    				 sb.append(name).append(" - targeting ");
    				 Card tgt = getTargetCard();
    				 if (tgt != null) sb.append(tgt.getName());
    				 return sb.toString();
    			}
    		};//Ability_Activated

    		return abControl;
    	}


    private boolean doTgtAI(SpellAbility sa) {
        
		Target tgt = AF.getAbTgt();
		CardList list = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer);
		list = list.getValidCards(tgt.getValidTgts(), hostCard.getController(), hostCard);
		
        if (list.isEmpty())
        	return false;
        
		while(tgt.getNumTargeted() < tgt.getMaxTargets()){ 
			Card t = null;
			
			if (list.isEmpty()){
				if (tgt.getNumTargeted() < tgt.getMinTargets() || tgt.getNumTargeted() == 0){
					tgt.resetTargets();
					return false;
				}
				else{
					// todo is this good enough? for up to amounts?
					break;
				}
			}
			
			t = CardFactoryUtil.AI_getBestCreature(list);
			tgt.addTarget(t);
			list.remove(t);
		}
        
        return true;

    }   
    
    private void doResolve(SpellAbility sa) {
		ArrayList<Card> tgtCards;
		Target tgt = AF.getAbTgt();
		if (tgt != null)
			tgtCards = tgt.getTargetCards();
		else{
			tgtCards = new ArrayList<Card>();
			tgtCards.add(hostCard);
		}

		int size = tgtCards.size();
		for(int j = 0; j < size; j++){
			final Card tgtC = tgtCards.get(j);
			
			// copied from CardFactory_Creatures for Rubinia Soulsinger
			
			//Card c = getTargetCard();
            movedCards[j] = tgtC;
            
            if(AllZone.GameAction.isCardInPlay(tgtC) && CardFactoryUtil.canTarget(hostCard, tgtC)) {
                //set summoning sickness
                if(tgtC.getKeyword().contains("Haste")) {
                    tgtC.setSickness(false);
                } else {
                    tgtC.setSickness(true);
                }
                
                ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(false);
                ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(false);
                
                tgtC.setSickness(true);
                tgtC.setController(hostCard.getController());
                
                PlayerZone from = AllZone.getZone(tgtC);
                from.remove(tgtC);
                
                PlayerZone to = AllZone.getZone(Constant.Zone.Play, tgtC.getController());
                to.add(tgtC);
                
                ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(true);
                ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(true);
            }
			
			
			//end copied
            
            
            if(lose.contains("LeavesPlay")) {
            	hostCard.addLeavesPlayCommand(getLoseControlCommand(j));
            }
            if(lose.contains("Untap")) {
            	hostCard.addUntapCommand(getLoseControlCommand(j));
            }
            if(lose.contains("ChangeController")) {
            	hostCard.addChangeControllerCommand(getLoseControlCommand(j));
            }
			
        
		}//end foreach target
		
		//drawbacks are not implemented
		/*
		Card first = tgtCards.get(0);
		
        if(AF.hasSubAbility()) 
        	CardFactoryUtil.doDrawBack(params.get("SubAbility"), 0,
                hostCard.getController(), hostCard.getController().getOpponent(),
                first.getController(), hostCard, first, sa);
                */
        
    }
    
    private Command getLoseControlCommand(final int i) {
    	final Command loseControl = new Command() {
    		private static final long serialVersionUID = 878543373519872418L;

    		public void execute() {
    			Card c = movedCards[i];

    			if(AllZone.GameAction.isCardInPlay(c)) {
    				((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(false);
    				((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(false);

    				c.setSickness(true);
    				c.setController(c.getController().getOpponent());

    				PlayerZone from = AllZone.getZone(c);
    				from.remove(c);

    				//make sure the creature is removed from combat:
    				CardList list = new CardList(AllZone.Combat.getAttackers());
    				if(list.contains(c)) AllZone.Combat.removeFromCombat(c);

    				CardList pwlist = new CardList(AllZone.pwCombat.getAttackers());
    				if(pwlist.contains(c)) AllZone.pwCombat.removeFromCombat(c);

    				PlayerZone to = AllZone.getZone(Constant.Zone.Play, c.getOwner());
    				to.add(c);

    				((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(true);
    				((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(true);
    			}//if
    		}//execute()
    	};
    	
    	return loseControl;
    }
}
