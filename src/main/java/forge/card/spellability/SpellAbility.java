package forge.card.spellability;

import java.util.ArrayList;
import java.util.HashMap;

import forge.Card;
import forge.CardList;
import forge.Command;
import forge.CommandArgs;
import forge.ComputerUtil;
import forge.Player;
import forge.card.abilityFactory.AbilityFactory;
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

    /** The choices_made. */
    public Object[] choices_made; // open ended Casting choice storage
    // choices for constructor isPermanent argument
    /** Constant <code>Spell=0</code>. */
    public static final int Spell = 0;

    /** Constant <code>Ability=1</code>. */
    public static final int Ability = 1;

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
    private ArrayList<SpellAbility> charmChoices = new ArrayList<SpellAbility>();
    // private ArrayList<SpellAbility> charmChoicesMade = new
    // ArrayList<SpellAbility>();

    private Input beforePayMana;
    private Input afterResolve;
    private Input afterPayMana;

    /** The pay costs. */
    protected Cost payCosts = null;

    /** The chosen target. */
    protected Target chosenTarget = null;

    private SpellAbility_Restriction restrictions = new SpellAbility_Restriction();
    private SpellAbility_Condition conditions = new SpellAbility_Condition();
    private Ability_Sub subAbility = null;

    private AbilityFactory abilityFactory = null;

    private ArrayList<Mana> payingMana = new ArrayList<Mana>();
    private ArrayList<Ability_Mana> paidAbilities = new ArrayList<Ability_Mana>();

    private HashMap<String, CardList> paidLists = new HashMap<String, CardList>();

    private HashMap<String, Object> triggeringObjects = new HashMap<String, Object>();

    private Command cancelCommand = Command.BLANK;
    private Command beforePayManaAI = Command.BLANK;

    private CommandArgs randomTarget = new CommandArgs() {

        private static final long serialVersionUID = 1795025064923737374L;

        public void execute(final Object o) {
        }
    };

    /**
     * <p>
     * Constructor for SpellAbility.
     * </p>
     * 
     * @param spellOrAbility
     *            a int.
     * @param i_sourceCard
     *            a {@link forge.Card} object.
     */
    public SpellAbility(final int spellOrAbility, final Card i_sourceCard) {
        if (spellOrAbility == Spell) {
            spell = true;
        } else if (spellOrAbility == Ability) {
            spell = false;
        } else {
            throw new RuntimeException("SpellAbility : constructor error, invalid spellOrAbility argument = "
                    + spellOrAbility);
        }

        sourceCard = i_sourceCard;
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
        return canPlay() && canAfford();
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
        randomTarget.execute(this);
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
        randomTarget = c;
    }

    /**
     * <p>
     * getChooseTargetAI.
     * </p>
     * 
     * @return a {@link forge.CommandArgs} object.
     */
    public CommandArgs getChooseTargetAI() {
        return randomTarget;
    }

    /**
     * <p>
     * Getter for the field <code>manaCost</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public String getManaCost() {
        return manaCost;
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
        manaCost = cost;
    }

    /**
     * <p>
     * Getter for the field <code>additionalManaCost</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public String getAdditionalManaCost() {
        return additionalManaCost;
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
        additionalManaCost = cost;
    }

    /**
     * <p>
     * Getter for the field <code>multiKickerManaCost</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public String getMultiKickerManaCost() {
        return multiKickerManaCost;
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
        multiKickerManaCost = cost;
    }

    /**
     * <p>
     * Getter for the field <code>replicateManaCost</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public String getReplicateManaCost() {
        return replicateManaCost;
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
        replicateManaCost = cost;
    }

    /**
     * <p>
     * Getter for the field <code>xManaCost</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public String getXManaCost() {
        return xManaCost;
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
        xManaCost = cost;
    }

    /**
     * <p>
     * Getter for the field <code>activatingPlayer</code>.
     * </p>
     * 
     * @return a {@link forge.Player} object.
     */
    public Player getActivatingPlayer() {
        return activatingPlayer;
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
        activatingPlayer = player;
        if (subAbility != null) {
            subAbility.setActivatingPlayer(player);
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
        return spell;
    }

    /**
     * <p>
     * isAbility.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean isAbility() {
        return !isSpell();
    }

    /**
     * <p>
     * isTapAbility.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean isTapAbility() {
        return tapAbility;
    }

    /**
     * <p>
     * isUntapAbility.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean isUntapAbility() {
        return untapAbility;
    }

    /**
     * <p>
     * makeUntapAbility.
     * </p>
     */
    public void makeUntapAbility() {
        untapAbility = true;
        tapAbility = false;
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
        buyBackAbility = b;
    }

    /**
     * <p>
     * isBuyBackAbility.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean isBuyBackAbility() {
        return buyBackAbility;
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
        multiKicker = b;
    }

    /**
     * <p>
     * isMultiKicker.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean isMultiKicker() {
        return multiKicker;
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
        replicate = b;
    }

    /**
     * <p>
     * isReplicate.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean isReplicate() {
        return replicate;
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
        xCost = b;
    }

    /**
     * <p>
     * isXCost.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean isXCost() {
        return xCost;
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
        cycling = b;
    }

    /**
     * <p>
     * isCycling.
     * </p>
     * 
     * @return a boolean.
     */
    public  boolean isCycling() {
        return cycling;
    }

    /**
     * <p>
     * Setter for the field <code>sourceCard</code>.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public  void setSourceCard(final Card c) {
        sourceCard = c;
    }

    /**
     * <p>
     * Getter for the field <code>sourceCard</code>.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public Card getSourceCard() {
        return sourceCard;
    }

    /**
     * <p>
     * Getter for the field <code>beforePayManaAI</code>.
     * </p>
     * 
     * @return a {@link forge.Command} object.
     */
    public Command getBeforePayManaAI() {
        return beforePayManaAI;
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
        beforePayManaAI = c;
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
        return beforePayMana;
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
        beforePayMana = in;
    }

    /**
     * <p>
     * Getter for the field <code>afterPayMana</code>.
     * </p>
     * 
     * @return a {@link forge.gui.input.Input} object.
     */
    public Input getAfterPayMana() {
        return afterPayMana;
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
        afterPayMana = in;
    }

    /**
     * <p>
     * Getter for the field <code>payCosts</code>.
     * </p>
     * 
     * @return a {@link forge.card.cost.Cost} object.
     */
    public Cost getPayCosts() {
        return payCosts;
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
        payCosts = abCost;
    }

    /**
     * <p>
     * getTarget.
     * </p>
     * 
     * @return a {@link forge.card.spellability.Target} object.
     */
    public Target getTarget() {
        return chosenTarget;
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
        chosenTarget = tgt;
    }

    /**
     * <p>
     * Setter for the field <code>restrictions</code>.
     * </p>
     * 
     * @param restrict
     *            a {@link forge.card.spellability.SpellAbility_Restriction}
     *            object.
     */
    public void setRestrictions(final SpellAbility_Restriction restrict) {
        restrictions = restrict;
    }

    /**
     * <p>
     * Getter for the field <code>restrictions</code>.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility_Restriction}
     *         object.
     */
    public SpellAbility_Restriction getRestrictions() {
        return restrictions;
    }

    /**
     * <p>
     * Shortcut to see how many activations there were.
     * </p>
     * 
     * @return the activations this turn
     */
    public int getActivationsThisTurn() {
        return restrictions.getNumberTurnActivations();
    }

    /**
     * <p>
     * Setter for the field <code>conditions</code>.
     * </p>
     * 
     * @param condition
     *            a {@link forge.card.spellability.SpellAbility_Condition}
     *            object.
     * @since 1.0.15
     */
    public void setConditions(final SpellAbility_Condition condition) {
        conditions = condition;
    }

    /**
     * <p>
     * Getter for the field <code>conditions</code>.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility_Condition} object.
     * @since 1.0.15
     */
    public SpellAbility_Condition getConditions() {
        return conditions;
    }

    /**
     * <p>
     * Setter for the field <code>abilityFactory</code>.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     */
    public void setAbilityFactory(final AbilityFactory af) {
        abilityFactory = af;
    }

    /**
     * <p>
     * Getter for the field <code>abilityFactory</code>.
     * </p>
     * 
     * @return a {@link forge.card.abilityFactory.AbilityFactory} object.
     */
    public AbilityFactory getAbilityFactory() {
        return abilityFactory;
    }

    /**
     * <p>
     * Getter for the field <code>payingMana</code>.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public ArrayList<Mana> getPayingMana() {
        return payingMana;
    }

    /**
     * <p>
     * getPayingManaAbilities.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public ArrayList<Ability_Mana> getPayingManaAbilities() {
        return paidAbilities;
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
        paidLists = hash;
    }

    /**
     * <p>
     * getPaidHash.
     * </p>
     * 
     * @return a {@link java.util.HashMap} object.
     */
    public HashMap<String, CardList> getPaidHash() {
        return paidLists;
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
        paidLists.put(str, list);
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
        return paidLists.get(str);
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
        if (!paidLists.containsKey(str)) {
            paidLists.put(str, new CardList());
        }

        paidLists.get(str).add(c);
    }

    /**
     * <p>
     * resetPaidHash.
     * </p>
     */
    public void resetPaidHash() {
        paidLists = new HashMap<String, CardList>();
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
        return triggeringObjects;
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
        return triggeringObjects.get(type);
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
        return triggeringObjects.containsKey(type);
    }

    /**
     * <p>
     * resetTriggeringObjects.
     * </p>
     * 
     * @since 1.0.15
     */
    public void resetTriggeringObjects() {
        triggeringObjects = new HashMap<String, Object>();
    }

    /**
     * <p>
     * resetOnceResolved.
     * </p>
     */
    public void resetOnceResolved() {
        resetPaidHash();

        if (chosenTarget != null) {
            chosenTarget.resetTargets();
        }

        resetTriggeringObjects();

        // Clear SVars
        for (String store : Card.getStorableSVars()) {
            String value = sourceCard.getSVar(store);
            if (value.length() > 0) {
                sourceCard.setSVar(store, "");
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
        return afterResolve;
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
        afterResolve = in;
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
        stackDescription = s;
        if (description == "" && sourceCard.getText().equals("")) {
            description = s;
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
        if (stackDescription.equals(getSourceCard().getText().trim())) {
            return getSourceCard().getName() + " - " + getSourceCard().getText();
        }

        return stackDescription.replaceAll("CARDNAME", this.getSourceCard().getName());
    }

    /**
     * <p>
     * isIntrinsic.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean isIntrinsic() {
        return type.equals("Intrinsic");
    }

    /**
     * <p>
     * isExtrinsic.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean isExtrinsic() {
        return type.equals("Extrinsic");
    }

    /**
     * <p>
     * Setter for the field <code>type</code>.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     */
    public void setType(final String s) // Extrinsic or Intrinsic:
    {
        type = s;
    }

    /**
     * <p>
     * Getter for the field <code>type</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public String getType() // Extrinsic or Intrinsic:
    {
        return type;
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
        description = s;
    }

    /**
     * <p>
     * Getter for the field <code>description</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public String getDescription() {
        return description;
    }

    /** {@inheritDoc} */
    @Override
    public final String toString() {

        if (isSuppressed()) {
            return "";
        }

        return toUnsuppressedString();
    }

    /**
     * To unsuppressed string.
     * 
     * @return the string
     */
    public String toUnsuppressedString() {

        StringBuilder sb = new StringBuilder();
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
     *            a {@link forge.card.spellability.Ability_Sub} object.
     */
    public void setSubAbility(final Ability_Sub subAbility) {
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
     * @return a {@link forge.card.spellability.Ability_Sub} object.
     */
    public Ability_Sub getSubAbility() {
        return this.subAbility;
    }

    /**
     * <p>
     * Getter for the field <code>targetCard</code>.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public  Card getTargetCard() {
        if (targetCard == null) {
            Target tgt = this.getTarget();
            if (tgt != null) {
                ArrayList<Card> list = tgt.getTargetCards();

                if (!list.isEmpty()) {
                    return list.get(0);
                }
            }
            return null;
        }

        return targetCard;
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
            System.out.println(getSourceCard() + " - SpellAbility.setTargetCard() called with null for target card.");
            return;
        }

        Target tgt = this.getTarget();
        if (tgt != null) {
            tgt.addTarget(card);
        } else {
            targetPlayer = null; // reset setTargetPlayer()
            targetCard = card;
        }
        String desc = "";
        if (null != card) {
            if (!card.isFaceDown()) {
                desc = getSourceCard().getName() + " - targeting " + card;
            } else {
                desc = getSourceCard().getName() + " - targeting Morph(" + card.getUniqueNumber() + ")";
            }
            setStackDescription(desc);
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
        return targetList;
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
        // forge.CardFactoryUtil.canTarget(CardFactoryUtil.java:3329)
        // after ForgeSVN r2699. I hope that commenting out the line below will
        // not result in other bugs. :)
        // targetPlayer = null;//reset setTargetPlayer()

        targetList = list;
        StringBuilder sb = new StringBuilder();
        sb.append(getSourceCard().getName()).append(" - targeting ");
        for (int i = 0; i < targetList.size(); i++) {

            if (!targetList.get(i).isFaceDown()) {
                sb.append(targetList.get(i));
            } else {
                sb.append("Morph(").append(targetList.get(i).getUniqueNumber()).append(")");
            }

            if (i < targetList.size() - 1) {
                sb.append(", ");
            }
        }
        setStackDescription(sb.toString());
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
        if (p == null || (!(p.isHuman() || p.isComputer()))) {
            throw new RuntimeException("SpellAbility : setTargetPlayer() error, argument is " + p + " source card is "
                    + getSourceCard());
        }

        Target tgt = this.getTarget();
        if (tgt != null) {
            tgt.addTarget(p);
        } else {
            targetCard = null; // reset setTargetCard()
            targetPlayer = p;
        }
        setStackDescription(getSourceCard().getName() + " - targeting " + p);
    }

    /**
     * <p>
     * Getter for the field <code>targetPlayer</code>.
     * </p>
     * 
     * @return a {@link forge.Player} object.
     */
    public Player getTargetPlayer() {
        if (targetPlayer == null) {
            Target tgt = this.getTarget();
            if (tgt != null) {
                ArrayList<Player> list = tgt.getTargetPlayers();

                if (!list.isEmpty()) {
                    return list.get(0);
                }
            }
            return null;
        }
        return targetPlayer;
    }

    /**
     * <p>
     * Getter for the field <code>cancelCommand</code>.
     * </p>
     * 
     * @return a {@link forge.Command} object.
     */
    public Command getCancelCommand() {
        return cancelCommand;
    }

    /**
     * <p>
     * Setter for the field <code>cancelCommand</code>.
     * </p>
     * 
     * @param cancelCommand
     *            a {@link forge.Command} object.
     */
    public void setCancelCommand(final Command cancelCommand) {
        this.cancelCommand = cancelCommand;
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
        return flashBackAbility;
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
        return kickerAbility;
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
        } catch (CloneNotSupportedException e) {
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
        return trigger;
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
     * @param ID
     *            a int.
     */
    public void setSourceTrigger(final int ID) {
        sourceTrigger = ID;
    }

    /**
     * <p>
     * getSourceTrigger.
     * </p>
     * 
     * @return a int.
     */
    public int getSourceTrigger() {
        return sourceTrigger;
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
        return mandatory;
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
        if (this instanceof Ability_Sub) {
            SpellAbility parent = ((Ability_Sub) this).getParent();
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
    public final ArrayList<Target_Choices> getAllTargetChoices() {
        ArrayList<Target_Choices> res = new ArrayList<Target_Choices>();

        SpellAbility sa = getRootSpellAbility();
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
        temporarilySuppressed = supp;
    }

    /**
     * Checks if is suppressed.
     * 
     * @return true, if is suppressed
     */
    public final boolean isSuppressed() {
        return (temporarilySuppressed);
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
        isCharm = b;
    }

    /**
     * <p>
     * isCharm.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isCharm() {
        return isCharm;
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
        charmNumber = i;
    }

    /**
     * <p>
     * getCharmNumber.
     * </p>
     * 
     * @return an int
     */
    public final int getCharmNumber() {
        return charmNumber;
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
        minCharmNumber = i;
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
        return minCharmNumber;
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
        charmChoices.add(sa);
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
        return charmChoices;
    }

    /**
     * Gets the checks if is delve.
     * 
     * @return the isDelve
     */
    public final boolean getIsDelve() {
        return isDelve;
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

}
