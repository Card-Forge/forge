package forge.card.spellability;

import java.util.HashMap;

import forge.Card;
import forge.CardList;
import forge.Player;

public class SpellAbility_StackInstance {
	// At some point I want this functioning more like Target/Target Choices where the SA has an "active"
	// Stack Instance, and instead of having duplicate parameters, it adds changes directly to the "active" one
	// When hitting the Stack, the active SI gets "applied" to the Stack and gets cleared from the base SI
	// Coming off the Stack would work similarly, except it would just add the full active SI instead of each of the parts 
	SpellAbility ability = null;
	SpellAbility_StackInstance subInstace = null;

	// When going to a SubAbility that SA has a Instance Choice object
	Target_Choices tc = null;
	Player activatingPlayer = null;
	String activatedFrom = null;

	String stackDescription = null;

	// Adjusted Mana Cost
	//private String adjustedManaCost = "";

	// Paid Mana Cost
	//private ArrayList<Mana> payingMana = new ArrayList<Mana>();
	//private ArrayList<Ability_Mana> paidAbilities = new ArrayList<Ability_Mana>();
	private int xManaPaid = 0;

	// Other Paid things
	private HashMap<String, CardList> paidHash = new HashMap<String, CardList>();
	
	// Additional info
    // is Kicked, is Buyback
	
	
	// Triggers
	private HashMap<String, Object> triggeringObjects = new HashMap<String, Object>();
	
	public SpellAbility_StackInstance(SpellAbility sa){
		// Base SA info
		ability = sa;
		stackDescription = ability.getStackDescription();
		activatingPlayer = sa.getActivatingPlayer();
		
		// Payment info
		paidHash = ability.getPaidHash();
		ability.resetPaidHash();
		
		// TODO getXManaCostPaid should be on the SA, not the Card
		xManaPaid = sa.getSourceCard().getXManaCostPaid();
		
		// Targeting info
		Target target = sa.getTarget();
		if (target != null){
			tc = target.getTargetChoices(); 
			ability.getTarget().resetTargets();
		}

		// Triggering info
		triggeringObjects = sa.getTriggeringObjects();
		
		Ability_Sub subAb = ability.getSubAbility();
		if (subAb != null)
			subInstace = new SpellAbility_StackInstance(subAb);
	}
	
	public SpellAbility getSpellAbility(){
		if (ability.getTarget() != null){
			ability.getTarget().resetTargets();
			ability.getTarget().setTargetChoices(tc);
		}
		ability.setActivatingPlayer(activatingPlayer);
		
		// Saved sub-SA needs to be reset on the way out
		if (this.subInstace != null)
			ability.setSubAbility((Ability_Sub)this.subInstace.getSpellAbility());
		
		// Set Cost specific things here
		ability.setPaidHash(paidHash);
		ability.getSourceCard().setXManaCostPaid(xManaPaid);
		
		// Triggered
		ability.setAllTriggeringObjects(triggeringObjects);
		
		return ability;
	}
	
	// A bit of SA shared abilities to restrict conflicts
	public String getStackDescription() {
		return stackDescription;
	}

	public Card getSourceCard(){
		return ability.getSourceCard();
	}
	
	public Player getActivatingPlayer(){
		return activatingPlayer;
	}
	
	public boolean isSpell(){
		return ability.isSpell();
	}
	
	public boolean isAbility(){
		return ability.isAbility();
	}
	
	public boolean isTrigger(){
		return ability.isTrigger();
	}
}
