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

import forge.Card;

/**
 * <p>
 * Abstract Ability_Sub class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class AbilitySub extends SpellAbility implements java.io.Serializable {
    /** Constant <code>serialVersionUID=4650634415821733134L</code>. */
    private static final long serialVersionUID = 4650634415821733134L;

    private SpellAbility parent = null;

    /**
     * <p>
     * Constructor for Ability_Sub.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param tgt
     *            a {@link forge.card.spellability.Target} object.
     */
    public AbilitySub(final Card sourceCard, final Target tgt) {
        super(SpellAbility.getAbility(), sourceCard);
        this.setTarget(tgt);
    }

    /** {@inheritDoc} */
    @Override
    public boolean canPlay() {
        // this should never be on the Stack by itself
        return false;
    }

    /**
     * <p>
     * chkAI_Drawback.
     * </p>
     * 
     * @return a boolean.
     */
    public abstract boolean chkAIDrawback();

    public abstract AbilitySub getCopy();

    /** {@inheritDoc} */
    @Override
    public abstract boolean doTrigger(boolean mandatory);

    /**
     * <p>
     * Setter for the field <code>parent</code>.
     * </p>
     * 
     * @param parent
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public final void setParent(final SpellAbility parent) {
        this.parent = parent;
    }

    /**
     * <p>
     * Getter for the field <code>parent</code>.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getParent() {
        return this.parent;
    }
}
