package forge.card.spellability;

import java.util.HashMap;

import forge.Card;
import forge.CardList;
import forge.Player;

/**
 * <p>
 * SpellAbility_StackInstance class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class SpellAbility_StackInstance {
    // At some point I want this functioning more like Target/Target Choices
    // where the SA has an "active"
    // Stack Instance, and instead of having duplicate parameters, it adds
    // changes directly to the "active" one
    // When hitting the Stack, the active SI gets "applied" to the Stack and
    // gets cleared from the base SI
    // Coming off the Stack would work similarly, except it would just add the
    // full active SI instead of each of the parts
    /** The ability. */
    private SpellAbility ability = null;

    /** The sub instace. */
    private SpellAbility_StackInstance subInstace = null;

    // When going to a SubAbility that SA has a Instance Choice object
    /** The tc. */
    private Target_Choices tc = null;

    /** The activating player. */
    private Player activatingPlayer = null;

    /** The stack description. */
    private String stackDescription = null;

    // Adjusted Mana Cost
    // private String adjustedManaCost = "";

    // Paid Mana Cost
    // private ArrayList<Mana> payingMana = new ArrayList<Mana>();
    // private ArrayList<Ability_Mana> paidAbilities = new
    // ArrayList<Ability_Mana>();
    private int xManaPaid = 0;

    // Other Paid things
    private HashMap<String, CardList> paidHash = new HashMap<String, CardList>();

    // Additional info
    // is Kicked, is Buyback

    // Triggers
    private HashMap<String, Object> triggeringObjects = new HashMap<String, Object>();

    private final HashMap<String, String> storedSVars = new HashMap<String, String>();

    /**
     * <p>
     * Constructor for SpellAbility_StackInstance.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public SpellAbility_StackInstance(final SpellAbility sa) {
        // Base SA info
        this.ability = sa;
        this.stackDescription = this.ability.getStackDescription();
        this.activatingPlayer = sa.getActivatingPlayer();

        // Payment info
        this.paidHash = this.ability.getPaidHash();
        this.ability.resetPaidHash();

        // TODO getXManaCostPaid should be on the SA, not the Card
        this.xManaPaid = sa.getSourceCard().getXManaCostPaid();

        // Triggering info
        this.triggeringObjects = sa.getTriggeringObjects();

        final Ability_Sub subAb = this.ability.getSubAbility();
        if (subAb != null) {
            this.subInstace = new SpellAbility_StackInstance(subAb);
        }

        // Targeting info -- 29/06/11 Moved to after taking care of SubAbilities
        // because otherwise AF_DealDamage SubAbilities that use Defined$
        // Targeted breaks (since it's parents target is reset)
        final Target target = sa.getTarget();
        if (target != null) {
            this.tc = target.getTargetChoices();
            this.ability.getTarget().resetTargets();
        }

        final Card source = this.ability.getSourceCard();

        // Store SVars and Clear
        for (final String store : Card.getStorableSVars()) {
            final String value = source.getSVar(store);
            if (value.length() > 0) {
                this.storedSVars.put(store, value);
                source.setSVar(store, "");
            }
        }
    }

    /**
     * <p>
     * getSpellAbility.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getSpellAbility() {
        if (this.ability.getTarget() != null) {
            this.ability.getTarget().resetTargets();
            this.ability.getTarget().setTargetChoices(this.tc);
        }
        this.ability.setActivatingPlayer(this.activatingPlayer);

        // Saved sub-SA needs to be reset on the way out
        if (this.subInstace != null) {
            this.ability.setSubAbility((Ability_Sub) this.subInstace.getSpellAbility());
        }

        // Set Cost specific things here
        this.ability.setPaidHash(this.paidHash);
        this.ability.getSourceCard().setXManaCostPaid(this.xManaPaid);

        // Triggered
        this.ability.setAllTriggeringObjects(this.triggeringObjects);

        // Add SVars back in
        final Card source = this.ability.getSourceCard();
        for (final String store : this.storedSVars.keySet()) {
            final String value = this.storedSVars.get(store);
            if (value.length() > 0) {
                source.setSVar(store, value);
            }
        }

        return this.ability;
    }

    // A bit of SA shared abilities to restrict conflicts
    /**
     * <p>
     * Getter for the field <code>stackDescription</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getStackDescription() {
        return this.stackDescription;
    }

    /**
     * <p>
     * getSourceCard.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public final Card getSourceCard() {
        return this.ability.getSourceCard();
    }

    /**
     * <p>
     * Getter for the field <code>activatingPlayer</code>.
     * </p>
     * 
     * @return a {@link forge.Player} object.
     */
    public final Player getActivatingPlayer() {
        return this.activatingPlayer;
    }

    /**
     * <p>
     * isSpell.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isSpell() {
        return this.ability.isSpell();
    }

    /**
     * <p>
     * isAbility.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isAbility() {
        return this.ability.isAbility();
    }

    /**
     * <p>
     * isTrigger.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isTrigger() {
        return this.ability.isTrigger();
    }

    /**
     * <p>
     * isStateTrigger.
     * </p>
     * 
     * @param id
     *            a int.
     * @return a boolean.
     */
    public final boolean isStateTrigger(final int id) {
        return this.ability.getSourceTrigger() == id;
    }

    /**
     * Checks if is optional trigger.
     * 
     * @return true, if is optional trigger
     */
    public final boolean isOptionalTrigger() {
        return this.ability.isOptionalTrigger();
    }
}
