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

import java.util.HashMap;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.Player;
import forge.card.cost.Cost;
import forge.card.mana.ManaPool;

/**
 * <p>
 * Abstract AbilityMana class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class AbilityMana extends AbilityActivated implements java.io.Serializable {
    /** Constant <code>serialVersionUID=-6816356991224950520L</code>. */
    private static final long serialVersionUID = -6816356991224950520L;

    private String origProduced;
    private int amount = 1;

    /** The reflected. */
    private boolean reflected = false;

    /** The undoable. */
    private boolean undoable = true;

    /** The canceled. */
    private boolean canceled = false;

    /**
     * <p>
     * Constructor for AbilityMana.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param parse
     *            a {@link java.lang.String} object.
     * @param produced
     *            a {@link java.lang.String} object.
     */
    public AbilityMana(final Card sourceCard, final String parse, final String produced) {
        this(sourceCard, parse, produced, 1);
    }

    /**
     * <p>
     * Constructor for AbilityMana.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param parse
     *            a {@link java.lang.String} object.
     * @param produced
     *            a {@link java.lang.String} object.
     * @param num
     *            a int.
     */
    public AbilityMana(final Card sourceCard, final String parse, final String produced, final int num) {
        this(sourceCard, new Cost(parse, sourceCard.getName(), true), produced, num);
    }

    /**
     * <p>
     * Constructor for AbilityMana.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param cost
     *            a {@link forge.card.cost.Cost} object.
     * @param produced
     *            a {@link java.lang.String} object.
     */
    public AbilityMana(final Card sourceCard, final Cost cost, final String produced) {
        this(sourceCard, cost, produced, 1);
    }

    /**
     * <p>
     * Constructor for AbilityMana.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param cost
     *            a {@link forge.card.cost.Cost} object.
     * @param produced
     *            a {@link java.lang.String} object.
     * @param num
     *            a int.
     */
    public AbilityMana(final Card sourceCard, final Cost cost, final String produced, final int num) {
        super(sourceCard, cost, null);

        this.origProduced = produced;
        this.amount = num;
    }

    /** {@inheritDoc} */
    @Override
    public boolean canPlayAI() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void resolve() {
        this.produceMana();
    }

    /**
     * <p>
     * produceMana.
     * </p>
     */
    public final void produceMana() {
        final StringBuilder sb = new StringBuilder();
        if (this.amount == 0) {
            sb.append("0");
        } else {
            try {
                // if baseMana is an integer(colorless), just multiply amount
                // and baseMana
                final int base = Integer.parseInt(this.origProduced);
                sb.append(base * this.amount);
            } catch (final NumberFormatException e) {
                for (int i = 0; i < this.amount; i++) {
                    if (i != 0) {
                        sb.append(" ");
                    }
                    sb.append(this.origProduced);
                }
            }
        }
        this.produceMana(sb.toString(), this.getSourceCard().getController());
    }

    /**
     * <p>
     * produceMana.
     * </p>
     * 
     * @param produced
     *            a {@link java.lang.String} object.
     * @param player
     *            a {@link forge.Player} object.
     */
    public final void produceMana(final String produced, final Player player) {
        final Card source = this.getSourceCard();
        final ManaPool manaPool = player.getManaPool();
        // change this, once ManaPool moves to the Player
        // this.getActivatingPlayer().ManaPool.addManaToFloating(origProduced,
        // getSourceCard());
        manaPool.addManaToFloating(produced, source);

        // TODO all of the following would be better as trigger events
        // "tapped for mana"
        if (source.getName().equals("Rainbow Vale")) {
            this.undoable = false;
            source.addExtrinsicKeyword("An opponent gains control of CARDNAME at the beginning of the next end step.");
        }

        if (source.getName().equals("Undiscovered Paradise")) {
            this.undoable = false;
            // Probably best to conver this to an Extrinsic Ability
            source.setBounceAtUntap(true);
        }

        // Run triggers
        final HashMap<String, Object> runParams = new HashMap<String, Object>();

        runParams.put("Card", source);
        runParams.put("Player", player);
        runParams.put("AbilityMana", this);
        runParams.put("Produced", produced);
        AllZone.getTriggerHandler().runTrigger("TapsForMana", runParams);

    } // end produceMana(String)

    /**
     * <p>
     * mana.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String mana() {
        return this.origProduced;
    }

    /**
     * <p>
     * setMana.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     */
    public final void setMana(final String s) {
        this.origProduced = s;
    }

    /**
     * <p>
     * setReflectedMana.
     * </p>
     * 
     * @param bReflect
     *            a boolean.
     */
    public final void setReflectedMana(final boolean bReflect) {
        this.reflected = bReflect;
    }

    /**
     * <p>
     * isSnow.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isSnow() {
        return this.getSourceCard().isSnow();
    }

    /**
     * <p>
     * isSacrifice.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isSacrifice() {
        return this.getPayCosts().getSacCost();
    }

    /**
     * <p>
     * isReflectedMana.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isReflectedMana() {
        return this.reflected;
    }

    /**
     * <p>
     * canProduce.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public final boolean canProduce(final String s) {
        return this.origProduced.contains(s);
    }

    /**
     * <p>
     * isBasic.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isBasic() {
        if (this.origProduced.length() != 1) {
            return false;
        }

        if (this.amount > 1) {
            return false;
        }

        return true;
    }

    /**
     * <p>
     * isUndoable.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isUndoable() {
        return this.undoable && this.getPayCosts().isUndoable() && AllZoneUtil.isCardInPlay(this.getSourceCard());
    }

    /**
     * <p>
     * Setter for the field <code>undoable</code>.
     * </p>
     * 
     * @param bUndo
     *            a boolean.
     */
    public final void setUndoable(final boolean bUndo) {
        this.undoable = bUndo;
    }

    /**
     * <p>
     * Setter for the field <code>canceled</code>.
     * </p>
     * 
     * @param bCancel
     *            a boolean.
     */
    public final void setCanceled(final boolean bCancel) {
        this.canceled = bCancel;
    }

    /**
     * <p>
     * Getter for the field <code>canceled</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getCanceled() {
        return this.canceled;
    }

    /**
     * <p>
     * undo.
     * </p>
     */
    public final void undo() {
        if (this.isUndoable()) {
            this.getPayCosts().refundPaidCost(this.getSourceCard());
        }
    }

    /** {@inheritDoc} */
    @Override
    public final boolean equals(final Object o) {
        // Mana abilities with same Descriptions are "equal"
        if ((o == null) || !(o instanceof AbilityMana)) {
            return false;
        }

        final AbilityMana abm = (AbilityMana) o;

        if (abm.getType() != this.getType()) {
            return false;
        }

        return abm.toUnsuppressedString().equals(this.toUnsuppressedString());
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return (41 * (41 + this.getType().hashCode()));
    }

} // end class AbilityMana

