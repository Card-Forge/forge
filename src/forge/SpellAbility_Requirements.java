package forge;

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
			// remove from hand, todo(sol) be careful of spell copies if spells start using this
			bCasting = true;
			Card c = ability.getSourceCard();
			fromZone = AllZone.getZone(c);
			fromZone.remove(c);
		}
		
		if (select.getTgt().doesTarget()){
			select.setRequirements(this);
			select.chooseTargets();
		}
		else
			startPaying();
	}
	
	public void finishedTargeting(){
		if (select.isCanceled()){
			// cancel ability during target choosing
			if (bCasting){	// and not a copy
				// add back to hand
				fromZone.add(ability.getSourceCard());
			}
			
			select.getTgt().resetTargets();
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
			if (select.getTgt().doesTarget())
				select.getTgt().resetTargets();
			
			payment.cancelPayment();
		}
	}
	
	public void addAbilityToStack(){
		AllZone.ManaPool.clearPay(false);
		AllZone.Stack.add(ability);
		if (select != null)
			select.getTgt().resetTargets();
	}
}
