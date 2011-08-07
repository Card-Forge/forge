package forge.card.spellability;

import java.util.ArrayList;

import forge.AllZone;
import forge.Card;
import forge.PlayerZone;

public class SpellAbility_Requirements {
	private SpellAbility ability = null;
	private Target_Selection select = null;
	private Cost_Payment payment = null;
	private boolean isFree = false;
	private boolean skipStack = false;
	public void setSkipStack(boolean bSkip) { skipStack = bSkip; }
	public void setFree(boolean bFree) { isFree = bFree; }
	
	private PlayerZone fromZone = null;
	private boolean bCasting = false;
	
	public SpellAbility_Requirements(SpellAbility sa, Target_Selection ts, Cost_Payment cp){
		ability = sa;
		select = ts;
		payment = cp;
	}
	
	public void fillRequirements(){
		fillRequirements(false);
	}
	
	public void fillRequirements(boolean skipTargeting){
		if (ability instanceof Spell && !bCasting){
			// remove from hand
			bCasting = true;
			if (!ability.getSourceCard().isCopiedSpell()){
				Card c = ability.getSourceCard();

				fromZone = AllZone.getZone(c);
				AllZone.GameAction.moveToStack(c);
			}
		}
		
		// freeze Stack. No abilities should go onto the stack while I'm filling requirements.
		AllZone.Stack.freezeStack();
		
		// Skip to paying if parent ability doesn't target and has no subAbilities. (or trigger case where its already targeted)
		if (!skipTargeting && (select.doesTarget() || ability.getSubAbility() != null)){
			select.setRequirements(this);
			select.resetTargets();
			select.chooseTargets();
		}
		else 
			needPayment();
	}
	
	public void finishedTargeting(){
		if (select.isCanceled()){
			// cancel ability during target choosing
			Card c = ability.getSourceCard();
			if (bCasting && !c.isCopiedSpell()){	// and not a copy
				// add back to where it came from
				AllZone.GameAction.moveTo(fromZone, c);
			}
			
			select.resetTargets();
			AllZone.Stack.clearFrozen();
			return;
		}
		else
			needPayment();
	}
	
	public void needPayment(){
		if (!isFree)
			startPaying();
		else
			finishPaying();
	}
	
	public void startPaying(){
		payment.setRequirements(this);
		payment.payCost();
	}
	
	public void finishPaying(){
		if (isFree || payment.isAllPaid())
		{
			if(skipStack)
			{
				ability.resolve();
			}
			else
			{
				addAbilityToStack();
			}
            AllZone.GameAction.checkStateEffects();
		}
		else if (payment.isCanceled()){
			Card c = ability.getSourceCard();
			if (bCasting && !c.isCopiedSpell()){	// and not a copy
				// add back to Previous Zone
				AllZone.GameAction.moveTo(fromZone, c);
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
		
		AllZone.ManaPool.clearPay(ability, false);
		AllZone.Stack.addAndUnfreeze(ability);
	}
}
