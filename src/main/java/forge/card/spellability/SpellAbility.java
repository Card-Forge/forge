/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.card.spellability;

import java.util.ArrayList;
import java.util.HashMap;

import forge.Card;
import forge.CardList;
import forge.Command;
import forge.CommandArgs;
import forge.ComputerUtil;
import forge.Player;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.cost.Cost;
import forge.card.mana.Mana;
import forge.gui.input.Input;

//only SpellAbility can go on the stack
//override any methods as needed
/**
 * <p>
 * Abstract SpellAbility class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class SpellAbility {

    // choices for constructor isPermanent argument
    /** Constant <code>Spell=0</code>. */
    private static final int SPELL = 0;

    /** Constant <code>Ability=1</code>. */
    private static final int ABILITY = 1;

    private String description = "";
    private Player targetPlayer = null;
    private String stackDescription = "";
    private String manaCost = "";
    private String additionalManaCost = "";
    private String multiKickerManaCost = "";
    private String replicateManaCost = "";
    private String xManaCost = "";
    private Player activatingPlayer = null;

    private String type = "Intrinsic"; // set to Intrinsic by default

    private Card targetCard;
    private Card sourceCard;

    private CardList targetList;
    // targetList doesn't appear to be used anymore

    private boolean spell;
    private boolean trigger = false;
    private boolean optionalTrigger = false;
    private int sourceTrigger = -1;
    private boolean mandatory = false;
    private boolean temporarilySuppressed = false;

    private boolean tapAbility;
    private boolean untapAbility;
    private boolean buyBackAbility = false; // false by default
    private boolean flashBackAbility = false;
    private boolean multiKicker = false;
    private boolean replicate = false;
    private boolean xCost = false;
    private boolean kickerAbility = false;
    private boolean cycling = false;
    private boolean isCharm = false;
    private boolean isDelve = false;

    private int charmNumber;
    private int minCharmNumber;
    private final ArrayList<SpellAbility> charmChoices = new ArrayList<SpellAbility>();

    private Input beforePayMana;
    private Input afterResolve;
    private Input afterPayMana;

    /** The pay costs. */
    private Cost payCosts = null;

    /** The chosen target. */
    private Target chosenTarget = null;

    private SpellAbilityRestriction restrictions = new SpellAbilityRestriction();
    private SpellAbilityCondition conditions = new SpellAbilityCondition();
    private AbilitySub subAbility = null;

    private AbilityFactory abilityFactory = null;

    private final ArrayList<Mana> payingMana = new ArrayList<Mana>();
    private final ArrayList<AbilityMana> paidAbilities = new ArrayList<AbilityMana>();

    private HashMap<String, CardList> paidLists = new HashMap<String, CardList>();

    private HashMap<String, Object> triggeringObjects = new HashMap<String, Object>();

    private Command beforePayManaAI = Command.BLANK;

    private CommandArgs randomTarget = new CommandArgs() {

        private static final long serialVersionUID = 1795025064923737374L;

        @Override
        public void execute(final Object o) {
        }
    };

    private CardList tappedForConvoke = null;

    /**
     * <p>
     * Constructor for SpellAbility.
     * </p>
     * 
     * @param spellOrAbility
     *            a int.
     * @param iSourceCard
     *            a {@link forge.Card} object.
     */
    public SpellAbility(final int spellOrAbility, final Card iSourceCard) {
        if (spellOrAbility == SpellAbility.getSpell()) {
            this.spell = true;
        } else if (spellOrAbility == SpellAbility.getAbility()) {
            this.spell = false;
        } else {
            throw new RuntimeException("SpellAbility : constructor error, invalid spellOrAbility argument = "
                    + spellOrAbility);
        }

        this.sourceCard = iSourceCard;
    }

    // Spell, and Ability, and other Ability objects override this method
    /**
     * <p>
     * canPlay.
     * </p>
     * 
     * @return a boolean.
     */
    public abstract boolean canPlay();

    /**
     * Can afford.
     * 
     * @return boolean
     */
    public boolean canAfford() {
        Player activator = this.getActivatingPlayer();
        if (activator == null) {
            activator = this.getSourceCard().getController();
        }

        return ComputerUtil.canPayCost(this, activator);
    }

    /**
     * Can play and afford.
     * 
     * @return true, if successful
     */
    public final boolean canPlayAndAfford() {
        return this.canPlay() && this.canAfford();
    }

    // all Spell's and Abilities must override this method
    /**
     * <p>
     * resolve.
     * </p>
     */
    public abstract void resolve();

    /**
     * <p>
     * canPlayAI.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean canPlayAI() {
        return true;
    }

    // This should be overridden by ALL AFs
    /**
     * <p>
     * doTrigger.
     * </p>
     * 
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    public boolean doTrigger(final boolean mandatory) {
        return false;
    }

    /**
     * <p>
     * chooseTargetAI.
     * </p>
     */
    public void chooseTargetAI() {
        this.randomTarget.execute(this);
    }

    /**
     * <p>
     * setChooseTargetAI.
     * </p>
     * 
     * @param c
     *            a {@link forge.CommandArgs} object.
     */
    public void setChooseTargetAI(final CommandArgs c) {
        this.randomTarget = c;
    }

    /**
     * <p>
     * getChooseTargetAI.
     * </p>
     * 
     * @return a {@link forge.CommandArgs} object.
     */
    public CommandArgs getChooseTargetAI() {
        return this.randomTarget;
    }

    /**
     * <p>
     * Getter for the field <code>manaCost</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public String getManaCost() {
        return this.manaCost;
    }

    /**
     * <p>
     * Setter for the field <code>manaCost</code>.
     * </p>
     * 
     * @param cost
     *            a {@link java.lang.String} object.
     */
    public void setManaCost(final String cost) {
        this.manaCost = cost;
    }

    /**
     * <p>
     * Getter for the field <code>additionalManaCost</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public String getAdditionalManaCost() {
        return this.additionalManaCost;
    }

    /**
     * <p>
     * Setter for the field <code>additionalManaCost</code>.
     * </p>
     * 
     * @param cost
     *            a {@link java.lang.String} object.
     */
    public void setAdditionalManaCost(final String cost) {
        this.additionalManaCost = cost;
    }

    /**
     * <p>
     * Getter for the field <code>multiKickerManaCost</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public String getMultiKickerManaCost() {
        return this.multiKickerManaCost;
    }

    /**
     * <p>
     * Setter for the field <code>multiKickerManaCost</code>.
     * </p>
     * 
     * @param cost
     *            a {@link java.lang.String} object.
     */
    public void setMultiKickerManaCost(final String cost) {
        this.multiKickerManaCost = cost;
    }

    /**
     * <p>
     * Getter for the field <code>replicateManaCost</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public String getReplicateManaCost() {
        return this.replicateManaCost;
    }

    /**
     * <p>
     * Setter for the field <code>replicateManaCost</code>.
     * </p>
     * 
     * @param cost
     *            a {@link java.lang.String} object.
     */
    public void setReplicateManaCost(final String cost) {
        this.replicateManaCost = cost;
    }

    /**
     * <p>
     * Getter for the field <code>xManaCost</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public String getXManaCost() {
        return this.xManaCost;
    }

    /**
     * <p>
     * Setter for the field <code>xManaCost</code>.
     * </p>
     * 
     * @param cost
     *            a {@link java.lang.String} object.
     */
    public void setXManaCost(final String cost) {
        this.xManaCost = cost;
    }

    /**
     * <p>
     * Getter for the field <code>activatingPlayer</code>.
     * </p>
     * 
     * @return a {@link forge.Player} object.
     */
    public Player getActivatingPlayer() {
        return this.activatingPlayer;
    }

    /**
     * <p>
     * Setter for the field <code>activatingPlayer</code>.
     * </p>
     * 
     * @param player
     *            a {@link forge.Player} object.
     */
    public void setActivatingPlayer(final Player player) {
        // trickle down activating player
        this.activatingPlayer = player;
        if (this.subAbility != null) {
            this.subAbility.setActivatingPlayer(player);
        }
    }

    /**
     * <p>
     * isSpell.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean isSpell() {
        return this.spell;
    }

    /**
     * <p>
     * isAbility.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean isAbility() {
        return !this.isSpell();
    }

    /**
     * <p>
     * isTapAbility.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean isTapAbility() {
        return this.tapAbility;
    }

    /**
     * <p>
     * isUntapAbility.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean isUntapAbility() {
        return this.untapAbility;
    }

    /**
     * <p>
     * makeUntapAbility.
     * </p>
     */
    public void makeUntapAbility() {
        this.untapAbility = true;
        this.tapAbility = false;
    }

    /**
     * <p>
     * setIsBuyBackAbility.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public void setIsBuyBackAbility(final boolean b) {
        this.buyBackAbility = b;
    }

    /**
     * <p>
     * isBuyBackAbility.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean isBuyBackAbility() {
        return this.buyBackAbility;
    }

    /**
     * <p>
     * setIsMultiKicker.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public void setIsMultiKicker(final boolean b) {
        this.multiKicker = b;
    }

    /**
     * <p>
     * isMultiKicker.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean isMultiKicker() {
        return this.multiKicker;
    }

    /**
     * <p>
     * setIsReplicate.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public void setIsReplicate(final boolean b) {
        this.replicate = b;
    }

    /**
     * <p>
     * isReplicate.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean isReplicate() {
        return this.replicate;
    }

    /**
     * <p>
     * setIsXCost.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public void setIsXCost(final boolean b) {
        this.xCost = b;
    }

    /**
     * <p>
     * isXCost.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean isXCost() {
        return this.xCost;
    }

    /**
     * <p>
     * setIsCycling.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public void setIsCycling(final boolean b) {
        this.cycling = b;
    }

    /**
     * <p>
     * isCycling.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean isCycling() {
        return this.cycling;
    }

    /**
     * <p>
     * Setter for the field <code>sourceCard</code>.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public void setSourceCard(final Card c) {
        this.sourceCard = c;
    }

    /**
     * <p>
     * Getter for the field <code>sourceCard</code>.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public Card getSourceCard() {
        return this.sourceCard;
    }

    /**
     * <p>
     * Getter for the field <code>beforePayManaAI</code>.
     * </p>
     * 
     * @return a {@link forge.Command} object.
     */
    public Command getBeforePayManaAI() {
        return this.beforePayManaAI;
    }

    /**
     * <p>
     * Setter for the field <code>beforePayManaAI</code>.
     * </p>
     * 
     * @param c
     *            a {@link forge.Command} object.
     */
    public void setBeforePayManaAI(final Command c) {
        this.beforePayManaAI = c;
    }

    // begin - Input methods
    /**
     * <p>
     * Getter for the field <code>beforePayMana</code>.
     * </p>
     * 
     * @return a {@link forge.gui.input.Input} object.
     */
    public Input getBeforePayMana() {
        return this.beforePayMana;
    }

    /**
     * <p>
     * Setter for the field <code>beforePayMana</code>.
     * </p>
     * 
     * @param in
     *            a {@link forge.gui.input.Input} object.
     */
    public void setBeforePayMana(final Input in) {
        this.beforePayMana = in;
    }

    /**
     * <p>
     * Getter for the field <code>afterPayMana</code>.
     * </p>
     * 
     * @return a {@link forge.gui.input.Input} object.
     */
    public Input getAfterPayMana() {
        return this.afterPayMana;
    }

    /**
     * <p>
     * Setter for the field <code>afterPayMana</code>.
     * </p>
     * 
     * @param in
     *            a {@link forge.gui.input.Input} object.
     */
    public void setAfterPayMana(final Input in) {
        this.afterPayMana = in;
    }

    /**
     * <p>
     * Getter for the field <code>payCosts</code>.
     * </p>
     * 
     * @return a {@link forge.card.cost.Cost} object.
     */
    public Cost getPayCosts() {
        return this.payCosts;
    }

    /**
     * <p>
     * Setter for the field <code>payCosts</code>.
     * </p>
     * 
     * @param abCost
     *            a {@link forge.card.cost.Cost} object.
     */
    public void setPayCosts(final Cost abCost) {
        this.payCosts = abCost;
    }

    /**
     * <p>
     * getTarget.
     * </p>
     * 
     * @return a {@link forge.card.spellability.Target} object.
     */
    public Target getTarget() {
        return this.getChosenTarget();
    }

    /**
     * <p>
     * setTarget.
     * </p>
     * 
     * @param tgt
     *            a {@link forge.card.spellability.Target} object.
     */
    public void setTarget(final Target tgt) {
        this.setChosenTarget(tgt);
    }

    /**
     * <p>
     * Setter for the field <code>restrictions</code>.
     * </p>
     * 
     * @param restrict
     *            a {@link forge.card.spellability.SpellAbilityRestriction}
     *            object.
     */
    public void setRestrictions(final SpellAbilityRestriction restrict) {
        this.restrictions = restrict;
    }

    /**
     * <p>
     * Getter for the field <code>restrictions</code>.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbilityRestriction} object.
     */
    public SpellAbilityRestriction getRestrictions() {
        return this.restrictions;
    }

    /**
     * <p>
     * Shortcut to see how many activations there were.
     * </p>
     * 
     * @return the activations this turn
     */
    public int getActivationsThisTurn() {
        return this.restrictions.getNumberTurnActivations();
    }

    /**
     * <p>
     * Setter for the field <code>conditions</code>.
     * </p>
     * 
     * @param condition
     *            a {@link forge.card.spellability.SpellAbilityCondition}
     *            object.
     * @since 1.0.15
     */
    public void setConditions(final SpellAbilityCondition condition) {
        this.conditions = condition;
    }

    /**
     * <p>
     * Getter for the field <code>conditions</code>.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbilityCondition} object.
     * @since 1.0.15
     */
    public SpellAbilityCondition getConditions() {
        return this.conditions;
    }

    /**
     * <p>
     * Setter for the field <code>abilityFactory</code>.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     */
    public void setAbilityFactory(final AbilityFactory af) {
        this.abilityFactory = af;
    }

    /**
     * <p>
     * Getter for the field <code>abilityFactory</code>.
     * </p>
     * 
     * @return a {@link forge.card.abilityfactory.AbilityFactory} object.
     */
    public AbilityFactory getAbilityFactory() {
        return this.abilityFactory;
    }

    /**
     * <p>
     * Getter for the field <code>payingMana</code>.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public ArrayList<Mana> getPayingMana() {
        return this.payingMana;
    }

    /**
     * <p>
     * getPayingManaAbilities.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public ArrayList<AbilityMana> getPayingManaAbilities() {
        return this.paidAbilities;
    }

    // Combined PaidLists
    /**
     * <p>
     * setPaidHash.
     * </p>
     * 
     * @param hash
     *            a {@link java.util.HashMap} object.
     */
    public void setPaidHash(final HashMap<String, CardList> hash) {
        this.paidLists = hash;
    }

    /**
     * <p>
     * getPaidHash.
     * </p>
     * 
     * @return a {@link java.util.HashMap} object.
     */
    public HashMap<String, CardList> getPaidHash() {
        return this.paidLists;
    }

    // Paid List are for things ca
    /**
     * <p>
     * setPaidList.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @param str
     *            a {@link java.lang.String} object.
     */
    public void setPaidList(final CardList list, final String str) {
        this.paidLists.put(str, list);
    }

    /**
     * <p>
     * getPaidList.
     * </p>
     * 
     * @param str
     *            a {@link java.lang.String} object.
     * @return a {@link forge.CardList} object.
     */
    public CardList getPaidList(final String str) {
        return this.paidLists.get(str);
    }

    /**
     * <p>
     * addCostToHashList.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param str
     *            a {@link java.lang.String} object.
     */
    public void addCostToHashList(final Card c, final String str) {
        if (!this.paidLists.containsKey(str)) {
            this.paidLists.put(str, new CardList());
        }

        this.paidLists.get(str).add(c);
    }

    /**
     * <p>
     * resetPaidHash.
     * </p>
     */
    public void resetPaidHash() {
        this.paidLists = new HashMap<String, CardList>();
    }

    /**
     * <p>
     * Getter for the field <code>triggeringObjects</code>.
     * </p>
     * 
     * @return a {@link java.util.HashMap} object.
     * @since 1.0.15
     */
    public HashMap<String, Object> getTriggeringObjects() {
        return this.triggeringObjects;
    }

    /**
     * <p>
     * setAllTriggeringObjects.
     * </p>
     * 
     * @param triggeredObjects
     *            a {@link java.util.HashMap} object.
     * @since 1.0.15
     */
    public void setAllTriggeringObjects(final HashMap<String, Object> triggeredObjects) {
        this.triggeringObjects = triggeredObjects;
    }

    /**
     * <p>
     * setTriggeringObject.
     * </p>
     * 
     * @param type
     *            a {@link java.lang.String} object.
     * @param o
     *            a {@link java.lang.Object} object.
     * @since 1.0.15
     */
    public void setTriggeringObject(final String type, final Object o) {
        this.triggeringObjects.put(type, o);
    }

    /**
     * <p>
     * getTriggeringObject.
     * </p>
     * 
     * @param type
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.Object} object.
     * @since 1.0.15
     */
    public Object getTriggeringObject(final String type) {
        return this.triggeringObjects.get(type);
    }

    /**
     * <p>
     * hasTriggeringObject.
     * </p>
     * 
     * @param type
     *            a {@link java.lang.String} object.
     * @return a boolean.
     * @since 1.0.15
     */
    public boolean hasTriggeringObject(final String type) {
        return this.triggeringObjects.containsKey(type);
    }

    /**
     * <p>
     * resetTriggeringObjects.
     * </p>
     * 
     * @since 1.0.15
     */
    public void resetTriggeringObjects() {
        this.triggeringObjects = new HashMap<String, Object>();
    }

    /**
     * <p>
     * resetOnceResolved.
     * </p>
     */
    public void resetOnceResolved() {
        this.resetPaidHash();

        if (this.getChosenTarget() != null) {
            this.getChosenTarget().resetTargets();
        }

        this.resetTriggeringObjects();

        // Clear SVars
        for (final String store : Card.getStorableSVars()) {
            final String value = this.sourceCard.getSVar(store);
            if (value.length() > 0) {
                this.sourceCard.setSVar(store, "");
            }
        }
    }

    /**
     * <p>
     * Getter for the field <code>afterResolve</code>.
     * </p>
     * 
     * @return a {@link forge.gui.input.Input} object.
     */
    public Input getAfterResolve() {
        return this.afterResolve;
    }

    /**
     * <p>
     * Setter for the field <code>afterResolve</code>.
     * </p>
     * 
     * @param in
     *            a {@link forge.gui.input.Input} object.
     */
    public void setAfterResolve(final Input in) {
        this.afterResolve = in;
    }

    /**
     * <p>
     * Setter for the field <code>stackDescription</code>.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     */
    public void setStackDescription(final String s) {
        this.stackDescription = s;
        if ((this.description == "") && this.sourceCard.getText().equals("")) {
            this.description = s;
        }
    }

    /**
     * <p>
     * Getter for the field <code>stackDescription</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public String getStackDescription() {
        if (this.stackDescription.equals(this.getSourceCard().getText().trim())) {
            return this.getSourceCard().getName() + " - " + this.getSourceCard().getText();
        }

        return this.stackDescription.replaceAll("CARDNAME", this.getSourceCard().getName());
    }

    /**
     * <p>
     * isIntrinsic.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean isIntrinsic() {
        return this.type.equals("Intrinsic");
    }

    /**
     * <p>
     * isExtrinsic.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean isExtrinsic() {
        return this.type.equals("Extrinsic");
    }

    /**
     * <p>
     * Setter for the field <code>type</code>.
     * </p>
     * 
     * Extrinsic or Intrinsic:
     * 
     * @param s
     *            a {@link java.lang.String} object.
     */
    public void setType(final String s) {
        this.type = s;
    }

    /**
     * <p>
     * Getter for the field <code>type</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public String getType() {
        // Extrinsic or Intrinsic:
        return this.type;
    }

    // setDescription() includes mana cost and everything like
    // "G, tap: put target creature from your hand onto the battlefield"
    /**
     * <p>
     * Setter for the field <code>description</code>.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     */
    public void setDescription(final String s) {
        this.description = s;
    }

    /**
     * <p>
     * Getter for the field <code>description</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public String getDescription() {
        return this.description;
    }

    /** {@inheritDoc} */
    @Override
    public final String toString() {

        if (this.isSuppressed()) {
            return "";
        }

        return this.toUnsuppressedString();
    }

    /**
     * To unsuppressed string.
     * 
     * @return the string
     */
    public String toUnsuppressedString() {

        final StringBuilder sb = new StringBuilder();
        SpellAbility node = this;

        while (node != null) {
            if (node != this) {
                sb.append(" ");
            }

            sb.append(node.getDescription().replace("CARDNAME", node.getSourceCard().getName()));
            node = node.getSubAbility();

        }
        return sb.toString();
    }

    /**
     * <p>
     * Setter for the field <code>subAbility</code>.
     * </p>
     * 
     * @param subAbility
     *            a {@link forge.card.spellability.AbilitySub} object.
     */
    public void setSubAbility(final AbilitySub subAbility) {
        this.subAbility = subAbility;
        if (subAbility != null) {
            subAbility.setParent(this);
        }
    }

    /**
     * <p>
     * Getter for the field <code>subAbility</code>.
     * </p>
     * 
     * @return a {@link forge.card.spellability.AbilitySub} object.
     */
    public AbilitySub getSubAbility() {
        return this.subAbility;
    }

    /**
     * <p>
     * Getter for the field <code>targetCard</code>.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public Card getTargetCard() {
        if (this.targetCard == null) {
            final Target tgt = this.getTarget();
            if (tgt != null) {
                final ArrayList<Card> list = tgt.getTargetCards();

                if (!list.isEmpty()) {
                    return list.get(0);
                }
            }
            return null;
        }

        return this.targetCard;
    }

    /**
     * <p>
     * Setter for the field <code>targetCard</code>.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     */
    public void setTargetCard(final Card card) {
        if (card == null) {
            System.out.println(this.getSourceCard()
                    + " - SpellAbility.setTargetCard() called with null for target card.");
            return;
        }

        final Target tgt = this.getTarget();
        if (tgt != null) {
            tgt.addTarget(card);
        } else {
            this.targetPlayer = null; // reset setTargetPlayer()
            this.targetCard = card;
        }
        String desc = "";
        if (null != card) {
            if (!card.isFaceDown()) {
                desc = this.getSourceCard().getName() + " - targeting " + card;
            } else {
                desc = this.getSourceCard().getName() + " - targeting Morph(" + card.getUniqueNumber() + ")";
            }
            this.setStackDescription(desc);
        }
    }

    /**
     * <p>
     * Getter for the field <code>targetList</code>.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    public CardList getTargetList() {
        return this.targetList;
    }

    /**
     * <p>
     * Setter for the field <code>targetList</code>.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    public void setTargetList(final CardList list) {
        // The line below started to create a null error at
        // forge.CardFactoryUtil.canBeTargetedBy(CardFactoryUtil.java:3329)
        // after ForgeSVN r2699. I hope that commenting out the line below will
        // not result in other bugs. :)
        // targetPlayer = null;//reset setTargetPlayer()

        this.targetList = list;
        final StringBuilder sb = new StringBuilder();
        sb.append(this.getSourceCard().getName()).append(" - targeting ");
        for (int i = 0; i < this.targetList.size(); i++) {

            if (!this.targetList.get(i).isFaceDown()) {
                sb.append(this.targetList.get(i));
            } else {
                sb.append("Morph(").append(this.targetList.get(i).getUniqueNumber()).append(")");
            }

            if (i < (this.targetList.size() - 1)) {
                sb.append(", ");
            }
        }
        this.setStackDescription(sb.toString());
    }

    /**
     * <p>
     * Setter for the field <code>targetPlayer</code>.
     * </p>
     * 
     * @param p
     *            a {@link forge.Player} object.
     */
    public void setTargetPlayer(final Player p) {
        if ((p == null) || (!(p.isHuman() || p.isComputer()))) {
            throw new RuntimeException("SpellAbility : setTargetPlayer() error, argument is " + p + " source card is "
                    + this.getSourceCard());
        }

        final Target tgt = this.getTarget();
        if (tgt != null) {
            tgt.addTarget(p);
        } else {
            this.targetCard = null; // reset setTargetCard()
            this.targetPlayer = p;
        }
        this.setStackDescription(this.getSourceCard().getName() + " - targeting " + p);
    }

    /**
     * <p>
     * Getter for the field <code>targetPlayer</code>.
     * </p>
     * 
     * @return a {@link forge.Player} object.
     */
    public Player getTargetPlayer() {
        if (this.targetPlayer == null) {
            final Target tgt = this.getTarget();
            if (tgt != null) {
                final ArrayList<Player> list = tgt.getTargetPlayers();

                if (!list.isEmpty()) {
                    return list.get(0);
                }
            }
            return null;
        }
        return this.targetPlayer;
    }

    /**
     * <p>
     * Setter for the field <code>flashBackAbility</code>.
     * </p>
     * 
     * @param flashBackAbility
     *            a boolean.
     */
    public void setFlashBackAbility(final boolean flashBackAbility) {
        this.flashBackAbility = flashBackAbility;
    }

    /**
     * <p>
     * isFlashBackAbility.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean isFlashBackAbility() {
        return this.flashBackAbility;
    }

    /**
     * <p>
     * Setter for the field <code>kickerAbility</code>.
     * </p>
     * 
     * @param kab
     *            a boolean.
     */
    public void setKickerAbility(final boolean kab) {
        this.kickerAbility = kab;
    }

    /**
     * <p>
     * isKickerAbility.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean isKickerAbility() {
        return this.kickerAbility;
    }

    // Only used by Ability_Reflected_Mana, because the user has an option to
    // cancel the input.
    // Most spell abilities and even most mana abilities do not need to use
    // this.
    /**
     * <p>
     * wasCancelled.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean wasCancelled() {
        return false;
    }

    /**
     * <p>
     * copy.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public SpellAbility copy() {
        SpellAbility clone = null;
        try {
            clone = (SpellAbility) this.clone();
        } catch (final CloneNotSupportedException e) {
            System.err.println(e);
        }
        return clone;
    }

    /**
     * <p>
     * Setter for the field <code>trigger</code>.
     * </p>
     * 
     * @param trigger
     *            a boolean.
     */
    public void setTrigger(final boolean trigger) {
        this.trigger = trigger;
    }

    /**
     * <p>
     * isTrigger.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean isTrigger() {
        return this.trigger;
    }

    /**
     * Sets the optional trigger.
     * 
     * @param optrigger
     *            the new optional trigger
     */
    public void setOptionalTrigger(final boolean optrigger) {
        this.optionalTrigger = optrigger;
    }

    /**
     * Checks if is optional trigger.
     * 
     * @return true, if is optional trigger
     */
    public boolean isOptionalTrigger() {
        return this.optionalTrigger;
    }

    /**
     * <p>
     * setSourceTrigger.
     * </p>
     * 
     * @param id
     *            a int.
     */
    public void setSourceTrigger(final int id) {
        this.sourceTrigger = id;
    }

    /**
     * <p>
     * getSourceTrigger.
     * </p>
     * 
     * @return a int.
     */
    public int getSourceTrigger() {
        return this.sourceTrigger;
    }

    /**
     * <p>
     * Setter for the field <code>mandatory</code>.
     * </p>
     * 
     * @param mand
     *            a boolean.
     */
    public final void setMandatory(final boolean mand) {
        this.mandatory = mand;
    }

    /**
     * <p>
     * isMandatory.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isMandatory() {
        return this.mandatory;
    }

    /**
     * <p>
     * getRootSpellAbility.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public final SpellAbility getRootSpellAbility() {
        if (this instanceof AbilitySub) {
            final SpellAbility parent = ((AbilitySub) this).getParent();
            if (parent != null) {
                return parent.getRootSpellAbility();
            }
        }

        return this;
    }

    /**
     * <p>
     * getAllTargetChoices.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     * @since 1.0.15
     */
    public final ArrayList<TargetChoices> getAllTargetChoices() {
        final ArrayList<TargetChoices> res = new ArrayList<TargetChoices>();

        SpellAbility sa = this.getRootSpellAbility();
        if (sa.getTarget() != null) {
            res.add(sa.getTarget().getTargetChoices());
        }
        while (sa.getSubAbility() != null) {
            sa = sa.getSubAbility();

            if (sa.getTarget() != null) {
                res.add(sa.getTarget().getTargetChoices());
            }
        }

        return res;
    }

    // is this a wrapping ability (used by trigger abilities)
    /**
     * <p>
     * isWrapper.
     * </p>
     * 
     * @return a boolean.
     * @since 1.0.15
     */
    public boolean isWrapper() {
        return false;
    }

    /**
     * Sets the temporarily suppressed.
     * 
     * @param supp
     *            the new temporarily suppressed
     */
    public final void setTemporarilySuppressed(final boolean supp) {
        this.temporarilySuppressed = supp;
    }

    /**
     * Checks if is suppressed.
     * 
     * @return true, if is suppressed
     */
    public final boolean isSuppressed() {
        return (this.temporarilySuppressed);
    }

    /**
     * <p>
     * setIsCharm.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setIsCharm(final boolean b) {
        this.isCharm = b;
    }

    /**
     * <p>
     * isCharm.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isCharm() {
        return this.isCharm;
    }

    /**
     * <p>
     * setCharmNumber.
     * </p>
     * 
     * @param i
     *            an int
     */
    public final void setCharmNumber(final int i) {
        this.charmNumber = i;
    }

    /**
     * <p>
     * getCharmNumber.
     * </p>
     * 
     * @return an int
     */
    public final int getCharmNumber() {
        return this.charmNumber;
    }

    /**
     * <p>
     * setMinCharmNumber.
     * </p>
     * 
     * @param i
     *            an int
     * @since 1.1.6
     */
    public final void setMinCharmNumber(final int i) {
        this.minCharmNumber = i;
    }

    /**
     * <p>
     * getMinCharmNumber.
     * </p>
     * 
     * @return an int
     * @since 1.1.6
     */
    public final int getMinCharmNumber() {
        return this.minCharmNumber;
    }

    /**
     * <p>
     * addCharmChoice.
     * </p>
     * 
     * @param sa
     *            a SpellAbility
     * @since 1.1.6
     */
    public final void addCharmChoice(final SpellAbility sa) {
        this.charmChoices.add(sa);
    }

    /**
     * <p>
     * getCharmChoicesMade.
     * </p>
     * 
     * @return an ArrayList<SpellAbility>
     * @since 1.1.6
     */
    public final ArrayList<SpellAbility> getCharmChoices() {
        return this.charmChoices;
    }

    /**
     * Gets the checks if is delve.
     * 
     * @return the isDelve
     */
    public final boolean getIsDelve() {
        return this.isDelve;
    }

    /**
     * Sets the checks if is delve.
     * 
     * @param isDelve0
     *            the isDelve to set
     */
    public final void setIsDelve(final boolean isDelve0) {
        this.isDelve = isDelve0; // TODO Add 0 to parameter's name.
    }

    /**
     * Gets the ability.
     * 
     * @return the ability
     */
    public static int getAbility() {
        return SpellAbility.ABILITY;
    }

    /**
     * Gets the spell.
     * 
     * @return the spell
     */
    public static int getSpell() {
        return SpellAbility.SPELL;
    }

    /**
     * Gets the chosen target.
     * 
     * @return the chosenTarget
     */
    public Target getChosenTarget() {
        return this.chosenTarget;
    }

    /**
     * Sets the chosen target.
     * 
     * @param chosenTarget
     *            the chosenTarget to set
     */
    public void setChosenTarget(final Target chosenTarget) {
        this.chosenTarget = chosenTarget; // TODO: Add 0 to parameter's name.
    }

    /**
     * Adds the tapped for convoke.
     * 
     * @param c
     *            the c
     */
    public void addTappedForConvoke(final Card c) {
        if (this.tappedForConvoke == null) {
            this.tappedForConvoke = new CardList();
        }

        this.tappedForConvoke.add(c);
    }

    /**
     * Gets the tapped for convoke.
     * 
     * @return the tapped for convoke
     */
    public CardList getTappedForConvoke() {
        return this.tappedForConvoke;
    }

    /**
     * Clear tapped for convoke.
     */
    public void clearTappedForConvoke() {
        if (this.tappedForConvoke != null) {
            this.tappedForConvoke.clear();
        }
    }

}
