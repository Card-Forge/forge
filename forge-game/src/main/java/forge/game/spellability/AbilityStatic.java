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
package forge.game.spellability;

import forge.card.mana.ManaCost;
import forge.game.card.Card;
import forge.game.cost.Cost;

/**
 * <p>
 * Abstract Ability_Static class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class AbilityStatic extends Ability implements Cloneable {
    /**
     * <p>
     * Constructor for Ability_Static.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.game.card.Card} object.
     * @param manaCost
     *            a {@link java.lang.String} object.
     */
    public AbilityStatic(final Card sourceCard, final ManaCost manaCost) {
        super(sourceCard, manaCost);
    }

    public AbilityStatic(final Card sourceCard, final Cost abCost, final TargetRestrictions tgt) {
        super(sourceCard, abCost);
        this.setTargetRestrictions(tgt);
    }
    @Override
    public boolean canPlay() {
        final Card c = this.getHostCard();

        // Check if ability can't be attempted because of replacement effect
        // Initial usage is Karlov Watchdog preventing disguise/morph/cloak/manifest turning face up
        if (this.isTurnFaceUp() && !c.canBeTurnedFaceUp()) {
            return false;
        }

        return this.getRestrictions().canPlay(c, this);
    }

    /** {@inheritDoc} */
    @Override
    public final Object clone() {
        try {
            return super.clone();
        } catch (final Exception ex) {
            throw new RuntimeException("AbilityStatic : clone() error, " + ex);
        }
    }
}
