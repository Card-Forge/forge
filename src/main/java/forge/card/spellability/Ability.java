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

import com.esotericsoftware.minlog.Log;

import forge.Card;
import forge.Singletons;

/**
 * <p>
 * Abstract Ability class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class Ability extends SpellAbility {

    /**
     * <p>
     * Constructor for Ability.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param manaCost
     *            a {@link java.lang.String} object.
     */
    public Ability(final Card sourceCard, final String manaCost) {
        super(SpellAbility.getAbility(), sourceCard);
        this.setManaCost(manaCost);
    }

    /**
     * <p>
     * Constructor for Ability.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param manaCost
     *            a {@link java.lang.String} object.
     * @param stackDescription
     *            a {@link java.lang.String} object.
     */
    public Ability(final Card sourceCard, final String manaCost, final String stackDescription) {
        this(sourceCard, manaCost);
        this.setStackDescription(stackDescription);
        Log.debug("an ability is being played from" + sourceCard.getName());
    }

    /** {@inheritDoc} */
    @Override
    public boolean canPlay() {
        if (Singletons.getModel().getGame().getStack().isSplitSecondOnStack() && null == this.getManaPart()) {
            return false;
        }

        return this.getSourceCard().isInPlay() && !this.getSourceCard().isFaceDown();
    }
}
