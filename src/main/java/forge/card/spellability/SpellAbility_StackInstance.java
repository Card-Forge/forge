package forge.card.spellability;

import forge.Card;
import forge.CardList;
import forge.Player;

import java.util.HashMap;

/**
 * <p>SpellAbility_StackInstance class.</p>
 *
 * @author Forge
 * @version $Id$
 */
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
    
    private HashMap<String, String> storedSVars = new HashMap<String, String>();
 
    /**
     * <p>Constructor for SpellAbility_StackInstance.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    public SpellAbility_StackInstance(SpellAbility sa) {
        // Base SA info
        ability = sa;
        stackDescription = ability.getStackDescription();
        activatingPlayer = sa.getActivatingPlayer();

        // Payment info
        paidHash = ability.getPaidHash();
        ability.resetPaidHash();

        // TODO getXManaCostPaid should be on the SA, not the Card
        xManaPaid = sa.getSourceCard().getXManaCostPaid();

        // Triggering info
        triggeringObjects = sa.getTriggeringObjects();

        Ability_Sub subAb = ability.getSubAbility();
        if (subAb != null)
            subInstace = new SpellAbility_StackInstance(subAb);

        // Targeting info  -- 29/06/11 Moved to after taking care of SubAbilities because otherwise AF_DealDamage SubAbilities that use Defined$ Targeted breaks (since it's parents target is reset)
        Target target = sa.getTarget();
        if (target != null) {
            tc = target.getTargetChoices();
            ability.getTarget().resetTargets();
        }
        
        Card source = ability.getSourceCard();
        
        //Store SVars and Clear
        for(String store : Card.getStorableSVars()){
            String value = source.getSVar(store);
            if (value.length() > 0){
                storedSVars.put(store, value);
                source.setSVar(store, "");
            }
        }
    }

    /**
     * <p>getSpellAbility.</p>
     *
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public SpellAbility getSpellAbility() {
        if (ability.getTarget() != null) {
            ability.getTarget().resetTargets();
            ability.getTarget().setTargetChoices(tc);
        }
        ability.setActivatingPlayer(activatingPlayer);

        // Saved sub-SA needs to be reset on the way out
        if (this.subInstace != null)
            ability.setSubAbility((Ability_Sub) this.subInstace.getSpellAbility());

        // Set Cost specific things here
        ability.setPaidHash(paidHash);
        ability.getSourceCard().setXManaCostPaid(xManaPaid);

        // Triggered
        ability.setAllTriggeringObjects(triggeringObjects);

        // Add SVars back in
        Card source = ability.getSourceCard();
        for(String store : storedSVars.keySet()){
            String value = storedSVars.get(store);
            if (value.length() > 0){
                source.setSVar(store, value);
            }
        }
        
        return ability;
    }

    // A bit of SA shared abilities to restrict conflicts
    /**
     * <p>Getter for the field <code>stackDescription</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getStackDescription() {
        return stackDescription;
    }

    /**
     * <p>getSourceCard.</p>
     *
     * @return a {@link forge.Card} object.
     */
    public Card getSourceCard() {
        return ability.getSourceCard();
    }

    /**
     * <p>Getter for the field <code>activatingPlayer</code>.</p>
     *
     * @return a {@link forge.Player} object.
     */
    public Player getActivatingPlayer() {
        return activatingPlayer;
    }

    /**
     * <p>isSpell.</p>
     *
     * @return a boolean.
     */
    public boolean isSpell() {
        return ability.isSpell();
    }

    /**
     * <p>isAbility.</p>
     *
     * @return a boolean.
     */
    public boolean isAbility() {
        return ability.isAbility();
    }

    /**
     * <p>isTrigger.</p>
     *
     * @return a boolean.
     */
    public boolean isTrigger() {
        return ability.isTrigger();
    }

    /**
     * <p>isStateTrigger.</p>
     *
     * @param ID a int.
     * @return a boolean.
     */
    public boolean isStateTrigger(int ID) {
        return ability.getSourceTrigger() == ID;
    }
    
    public boolean isOptionalTrigger() {
    	return ability.isOptionalTrigger();
    }
}
