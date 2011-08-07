package forge.card.spellability;

import java.util.ArrayList;
import java.util.HashMap;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.ButtonUtil;
import forge.Card;
import forge.CardList;
import forge.Constant;
import forge.Player;
import forge.PlayerZone;
import forge.card.cardFactory.CardFactoryUtil;
import forge.gui.GuiUtils;
import forge.gui.input.Input;

public class Target_Selection {
	private Target target = null;
	private SpellAbility ability = null;
	private Card card = null;

	public Target getTgt() { return target; }
	public SpellAbility getAbility() { return ability; }
	public Card getCard() { return card; }
	
	private SpellAbility_Requirements req = null;
	public void setRequirements(SpellAbility_Requirements reqs) { req = reqs; } 
	
	private boolean bCancel = false;
	public void setCancel(boolean done) { bCancel = done; }
	public boolean isCanceled() { return bCancel; }
	private boolean bDoneTarget = false;
	public void setDoneTarget(boolean done) { bDoneTarget = done; } 
	
	public Target_Selection(Target tgt, SpellAbility sa){
		target = tgt;
		ability = sa;
		card = sa.getSourceCard();
	}
	
	public boolean doesTarget(){
		if (target == null)
			return false;
		return target.doesTarget();
	}
	
	public void resetTargets(){
		if (target != null)
			target.resetTargets();
	}
	
	public boolean chooseTargets(){
		// if not enough targets chosen, reset and cancel Ability
		if (bCancel || (bDoneTarget && !target.isMinTargetsChosen(card, ability))){
			bCancel = true;
			req.finishedTargeting();
			return false;
		}
		else if (!doesTarget() || bDoneTarget && target.isMinTargetsChosen(card, ability) || target.isMaxTargetsChosen(card, ability)){
			Ability_Sub abSub = ability.getSubAbility();
			
			if (abSub == null){
				// if no more SubAbilities finish targeting
				req.finishedTargeting();
				return true;
			}
			else{
				// Has Sub Ability
				Target_Selection ts = new Target_Selection(abSub.getTarget(), abSub);
				ts.setRequirements(req);
				ts.resetTargets();
				return ts.chooseTargets();
			}
		}

		chooseValidInput();

        return false;
	}

    // these have been copied over from CardFactoryUtil as they need two extra parameters for target selection.
	// however, due to the changes necessary for SA_Requirements this is much different than the original

	public void chooseValidInput(){
		Target tgt = this.getTgt();
		String zone = tgt.getZone();
		final boolean mandatory = target.getMandatory() ? target.hasCandidates() : false;
		
		if (zone.equals(Constant.Zone.Stack)){
			// If Zone is Stack, the choices are handled slightly differently
			chooseCardFromStack(mandatory);
			return;
		}
		
		CardList choices = AllZoneUtil.getCardsInZone(zone).getValidCards(target.getValidTgts(), ability.getActivatingPlayer(), ability.getSourceCard());
		
		// Remove cards already targeted
		ArrayList<Card> targeted = tgt.getTargetCards();
		for(Card c : targeted){
			if (choices.contains(c))
				choices.remove(c);
		}
		
		if (zone.equals(Constant.Zone.Battlefield)){
			AllZone.InputControl.setInput(input_targetSpecific(choices, true, mandatory));
		}
		else
			chooseCardFromList(choices, true, mandatory);
    }//input_targetValid

    //CardList choices are the only cards the user can successful select
    public Input input_targetSpecific(final CardList choices, final boolean targeted, final boolean mandatory) {
    	final SpellAbility sa = this.ability;
    	final Target_Selection select = this;	
		final Target tgt = this.target;
		final SpellAbility_Requirements req = this.req;

    	Input target = new Input() {
			private static final long serialVersionUID = -1091595663541356356L;

			@Override
            public void showMessage() {	
				StringBuilder sb = new StringBuilder();
				sb.append("Targeted: ");
				sb.append(tgt.getTargetedString());
				sb.append("\n");
				sb.append(tgt.getVTSelection());
				
                AllZone.Display.showMessage(sb.toString());     
               
                // If reached Minimum targets, enable OK button
                if (!tgt.isMinTargetsChosen(sa.getSourceCard(), sa))
                	ButtonUtil.enableOnlyCancel();
                else
                	ButtonUtil.enableAll();
                
                if(mandatory)
                	ButtonUtil.disableCancel();
            }
            
            @Override
            public void selectButtonCancel() {
            	select.setCancel(true);
                stop();
                req.finishedTargeting();
            }
            
            @Override
            public void selectButtonOK() {
            	select.setDoneTarget(true);
            	done();
            }
            
            @Override
            public void selectCard(Card card, PlayerZone zone) {
            	// leave this in temporarily, there some seriously wrong things going on here
                if(targeted && !CardFactoryUtil.canTarget(sa, card)) {
                    AllZone.Display.showMessage("Cannot target this card (Shroud? Protection? Restrictions?).");
                } 
                else if(choices.contains(card)) {
                	tgt.addTarget(card);
                    done();
                }
            }//selectCard()
            
            @Override
            public void selectPlayer(Player player) {
            	if ((tgt.canTgtPlayer() || (tgt.canOnlyTgtOpponent() && player.equals(sa.getActivatingPlayer().getOpponent()))) && 
	               player.canTarget(sa.getSourceCard())){
            		tgt.addTarget(player);
            		done();
	            }
            }
            
            void done() {
                stop();

                select.chooseTargets();
            }
        };

        return target;
    }//input_targetSpecific()
    
    
    public void chooseCardFromList(final CardList choices, boolean targeted, final boolean mandatory){
    	// Send in a list of valid cards, and popup a choice box to target 
		final Card dummy = new Card();
		dummy.setName("[FINISH TARGETING]");
    	final SpellAbility sa = this.ability;
    	final String message = this.target.getVTSelection();
		
    	Target tgt = this.getTgt();
    	
    	CardList choicesWithDone = choices;
    	if (tgt.isMinTargetsChosen(sa.getSourceCard(), sa)){
    		// is there a more elegant way of doing this?
    		choicesWithDone.add(dummy);
    	}
        Object check = GuiUtils.getChoiceOptional(message, choicesWithDone.toArray());
        if(check != null) {
        	Card c = (Card) check;
        	if (c.equals(dummy))
        		this.setDoneTarget(true);
        	else
        		tgt.addTarget(c);
        }
        else
        	this.setCancel(true);
        
        this.chooseTargets();
    }
    
    public void chooseCardFromStack(final boolean mandatory){
    	Target tgt = this.target;
    	String message = tgt.getVTSelection();
		Target_Selection select = this;
    		
		// Find what's targetable, then allow human to choose 
		ArrayList<SpellAbility> choosables = getTargetableOnStack(this.ability, select.getTgt());

		HashMap<String,SpellAbility> map = new HashMap<String,SpellAbility>();

		for(SpellAbility sa : choosables) {
			map.put(sa.getStackDescription(),sa);
		}

		String[] choices = new String[map.keySet().size()];
		choices = map.keySet().toArray(choices);

		if (choices.length == 0){
			select.setCancel(true);
		}
		else{
			String madeChoice = GuiUtils.getChoiceOptional(message, choices);

			if (madeChoice != null){
				tgt.addTarget(map.get(madeChoice));
			}
			else
				select.setCancel(true);
		}

		select.chooseTargets();
    }
    
    // TODO: The following three functions are Utility functions for TargetOnStack, probably should be moved
    // The following should be select.getTargetableOnStack()
    public static ArrayList<SpellAbility> getTargetableOnStack(SpellAbility sa, Target tgt){
		ArrayList<SpellAbility> choosables = new ArrayList<SpellAbility>();

		for(int i = 0; i < AllZone.Stack.size(); i++) {
			choosables.add(AllZone.Stack.peekAbility(i));
		}
		
		for(int i = 0; i < choosables.size(); i++) {
			if (!matchSpellAbility(sa, choosables.get(i), tgt)){
				choosables.remove(i);
			}
		}
		return choosables;
    }
    
    public static boolean matchSpellAbility(SpellAbility sa, SpellAbility topSA, Target tgt){
    	String saType = tgt.getTargetSpellAbilityType();
    	
    	if(null == saType) {
    		//just take this to mean no restrictions - carry on.
    	}
    	else if (topSA.isSpell()){
    		if (!saType.contains("Spell"))
    			return false;
    	}
    	else if (topSA.isTrigger()){
    		if (!saType.contains("Triggered"))
    			return false;
    	}
    	else if (topSA.isAbility()){
    		if (!saType.contains("Activated"))
    			return false;
    	}
    	
    	String splitTargetRestrictions = tgt.getSAValidTargeting();
		if(splitTargetRestrictions != null){
			// TODO: What about spells with SubAbilities with Targets?
			
			Target matchTgt = topSA.getTarget();
			
			if (matchTgt == null)
				return false;
			
			boolean result = false;
			
			for(Object o : matchTgt.getTargets()){
				if(matchesValid(o, splitTargetRestrictions.split(","), sa)){
					result = true;
					break;
				}
			}

			if (!result)
				return false;
		}
		
		if(!matchesValid(topSA, tgt.getValidTgts(), sa)){
			return false;
		}
	
    	return true;
    }
    
	private static boolean matchesValid(Object o, String[] valids, SpellAbility sa)
	{
		Card srcCard = sa.getSourceCard();
		Player activatingPlayer = sa.getActivatingPlayer();
		if(o instanceof Card){
			Card c = (Card)o;
			return c.isValidCard(valids, activatingPlayer, srcCard);
		}
		
		if(o instanceof Player){
			for(String v : valids){
				if(v.equalsIgnoreCase("Player"))
					return true;

				if(v.equalsIgnoreCase("Opponent")){
					if(o.equals(activatingPlayer.getOpponent())){
						return true;
					}
				}
				if(v.equalsIgnoreCase("You"))
					return o.equals(activatingPlayer);
			}
		}
		
		if (o instanceof SpellAbility){
			Card c = ((SpellAbility)o).getSourceCard();
			return c.isValidCard(valids, activatingPlayer, srcCard);
		}
		
		return false;
	}
}
