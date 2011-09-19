package forge.card.spellability;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.Player;
import forge.card.cost.Cost;
import forge.card.mana.ManaPool;

import java.util.HashMap;

/**
 * <p>Abstract Ability_Mana class.</p>
 *
 * @author Forge
 * @version $Id$
 */
abstract public class Ability_Mana extends Ability_Activated implements java.io.Serializable {
    /** Constant <code>serialVersionUID=-6816356991224950520L</code> */
    private static final long serialVersionUID = -6816356991224950520L;

    private String origProduced;
    private int amount = 1;
    protected boolean reflected = false;
    protected boolean undoable = true;
    protected boolean canceled = false;

    /**
     * <p>Constructor for Ability_Mana.</p>
     *
     * @param sourceCard a {@link forge.Card} object.
     * @param parse a {@link java.lang.String} object.
     * @param produced a {@link java.lang.String} object.
     */
    public Ability_Mana(Card sourceCard, String parse, String produced) {
        this(sourceCard, parse, produced, 1);
    }

    /**
     * <p>Constructor for Ability_Mana.</p>
     *
     * @param sourceCard a {@link forge.Card} object.
     * @param parse a {@link java.lang.String} object.
     * @param produced a {@link java.lang.String} object.
     * @param num a int.
     */
    public Ability_Mana(Card sourceCard, String parse, String produced, int num) {
        this(sourceCard, new Cost(parse, sourceCard.getName(), true), produced, num);
    }

    /**
     * <p>Constructor for Ability_Mana.</p>
     *
     * @param sourceCard a {@link forge.Card} object.
     * @param cost a {@link forge.card.cost.Cost} object.
     * @param produced a {@link java.lang.String} object.
     */
    public Ability_Mana(Card sourceCard, Cost cost, String produced) {
        this(sourceCard, cost, produced, 1);
    }

    /**
     * <p>Constructor for Ability_Mana.</p>
     *
     * @param sourceCard a {@link forge.Card} object.
     * @param cost a {@link forge.card.cost.Cost} object.
     * @param produced a {@link java.lang.String} object.
     * @param num a int.
     */
    public Ability_Mana(Card sourceCard, Cost cost, String produced, int num) {
        super(sourceCard, cost, null);

        origProduced = produced;
        amount = num;
    }

    /** {@inheritDoc} */
    @Override
    public boolean canPlayAI() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void resolve() {
        produceMana();
    }

    /**
     * <p>produceMana.</p>
     */
    public void produceMana() {
        StringBuilder sb = new StringBuilder();
        if (amount == 0)
            sb.append("0");
        else {
            try {
                // if baseMana is an integer(colorless), just multiply amount and baseMana
                int base = Integer.parseInt(origProduced);
                sb.append(base * amount);
            } catch (NumberFormatException e) {
                for (int i = 0; i < amount; i++) {
                    if (i != 0)
                        sb.append(" ");
                    sb.append(origProduced);
                }
            }
        }
        produceMana(sb.toString(), this.getSourceCard().getController());
    }

    /**
     * <p>produceMana.</p>
     *
     * @param produced a {@link java.lang.String} object.
     * @param player a {@link forge.Player} object.
     */
    public void produceMana(String produced, Player player) {
        final Card source = this.getSourceCard();
        ManaPool manaPool = player.getManaPool();
        // change this, once ManaPool moves to the Player
        // this.getActivatingPlayer().ManaPool.addManaToFloating(origProduced, getSourceCard());
        manaPool.addManaToFloating(produced, source);

        // TODO: all of the following would be better as trigger events "tapped for mana"
        if (source.getName().equals("Rainbow Vale")) {
            this.undoable = false;
            source.addExtrinsicKeyword("An opponent gains control of CARDNAME at the beginning of the next end step.");
        }

        if (source.getName().equals("Undiscovered Paradise")) {
            this.undoable = false;
            // Probably best to conver this to an Extrinsic Ability
            source.setBounceAtUntap(true);
        }

        //Run triggers        
        HashMap<String, Object> runParams = new HashMap<String, Object>();

        runParams.put("Card", source);
        runParams.put("Player", player);
        runParams.put("Ability_Mana", this);
        runParams.put("Produced", produced);
        AllZone.getTriggerHandler().runTrigger("TapsForMana", runParams);

    }//end produceMana(String)

    /**
     * <p>mana.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String mana() {
        return origProduced;
    }

    /**
     * <p>setMana.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public void setMana(String s) {
        origProduced = s;
    }

    /**
     * <p>setReflectedMana.</p>
     *
     * @param bReflect a boolean.
     */
    public void setReflectedMana(boolean bReflect) {
        reflected = bReflect;
    }

    /**
     * <p>isSnow.</p>
     *
     * @return a boolean.
     */
    public boolean isSnow() {
        return this.getSourceCard().isSnow();
    }

    /**
     * <p>isSacrifice.</p>
     *
     * @return a boolean.
     */
    public boolean isSacrifice() {
        return payCosts.getSacCost();
    }

    /**
     * <p>isReflectedMana.</p>
     *
     * @return a boolean.
     */
    public boolean isReflectedMana() {
        return reflected;
    }

    /**
     * <p>canProduce.</p>
     *
     * @param s a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean canProduce(String s) {
        return origProduced.contains(s);
    }

    /**
     * <p>isBasic.</p>
     *
     * @return a boolean.
     */
    public boolean isBasic() {
        if (origProduced.length() != 1)
            return false;

        if (amount > 1)
            return false;

        return true;
    }

    /**
     * <p>isUndoable.</p>
     *
     * @return a boolean.
     */
    public boolean isUndoable() {
        return undoable && getPayCosts().isUndoable() && AllZoneUtil.isCardInPlay(getSourceCard());
    }

    /**
     * <p>Setter for the field <code>undoable</code>.</p>
     *
     * @param bUndo a boolean.
     */
    public void setUndoable(boolean bUndo) {
        undoable = bUndo;
    }

    /**
     * <p>Setter for the field <code>canceled</code>.</p>
     *
     * @param bCancel a boolean.
     */
    public void setCanceled(boolean bCancel) {
        canceled = bCancel;
    }

    /**
     * <p>Getter for the field <code>canceled</code>.</p>
     *
     * @return a boolean.
     */
    public boolean getCanceled() {
        return canceled;
    }

    /**
     * <p>undo.</p>
     */
    public void undo() {
        if (isUndoable()) {
            getPayCosts().refundPaidCost(getSourceCard());
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        //Mana abilities with same Descriptions are "equal"
        if (o == null || !(o instanceof Ability_Mana))
            return false;
        
        Ability_Mana abm = (Ability_Mana) o;
        
        return abm.toUnsuppressedString().equals(this.toUnsuppressedString());
    }

}//end class Ability_Mana

