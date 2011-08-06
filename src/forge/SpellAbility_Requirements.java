package forge;

import java.util.ArrayList;

public class SpellAbility_Requirements {
	private SpellAbility ability = null;
	private Target_Selection select = null;
	private Cost_Payment payment = null;
	
	private PlayerZone fromZone = null;
	private boolean bCasting = false;
	
	public SpellAbility_Requirements(SpellAbility sa, Target_Selection ts, Cost_Payment cp){
		ability = sa;
		select = ts;
		payment = cp;
	}
	
	public void fillRequirements(){
		if (ability instanceof Spell && !bCasting){
			// remove from hand
			bCasting = true;
			if (!ability.getSourceCard().isCopiedSpell()){
				Card c = ability.getSourceCard();
				fromZone = AllZone.getZone(c);
				if (fromZone != null)
					fromZone.remove(c);	
			}
		}
		
		// freeze Stack. No abilities should go onto the stack while I'm filling requirements.
		AllZone.Stack.freezeStack();
		
		// Skip to paying if parent ability doesn't target and has no subAbilities.
		if (!select.doesTarget() && ability.getSubAbility() == null)
			startPaying();
		else{
			select.setRequirements(this);
			select.resetTargets();
			select.chooseTargets();
		}
	}
	
	public void finishedTargeting(){
		if (select.isCanceled()){
			// cancel ability during target choosing
			if (bCasting && !ability.getSourceCard().isCopiedSpell()){	// and not a copy
				// add back to hand
				fromZone.add(ability.getSourceCard());
			}
			
			select.resetTargets();
			AllZone.Stack.clearFrozen();
			return;
		}
		startPaying();
	}
	
	public void startPaying(){
		payment.setRequirements(this);
		payment.payCost();
	}
	
	public void finishPaying(){
		if (payment.isAllPaid())
			addAbilityToStack();
		else if (payment.isCanceled()){
			if (bCasting){	// and not a copy
				// add back to hand
				fromZone.add(ability.getSourceCard());
			}
			if (select != null)
				select.resetTargets();
			
			payment.cancelPayment();
			AllZone.Stack.clearFrozen();
		}
	}
	
	public void addAbilityToStack(){
		// For older abilities that don't setStackDescription set it here
		if (ability.getStackDescription().equals("")){
			StringBuilder sb = new StringBuilder();
			sb.append(ability.getSourceCard().getName());
			if (ability.getTarget() != null){
				ArrayList<Object> targets = ability.getTarget().getTargets();
				if (targets.size() > 0){
					sb.append(" - Targeting ");
					for(Object o : targets)
						sb.append(o.toString()).append(" ");
				}
			}
	
			ability.setStackDescription(sb.toString());
		}
		
		ability.getRestrictions().abilityActivated();
		if(ability.getRestrictions().getActivationNumberSacrifice() != -1 &&
				ability.getRestrictions().getNumberTurnActivations() >= ability.getRestrictions().getActivationNumberSacrifice()) {
			ability.getSourceCard().addExtrinsicKeyword("At the beginning of the end step, sacrifice CARDNAME.");
		}
		AllZone.ManaPool.clearPay(false);
		AllZone.Stack.addAndUnfreeze(ability);
	}
}
