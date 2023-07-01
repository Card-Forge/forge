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

import com.esotericsoftware.minlog.Log;

import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.cost.Cost;

/**
 * <p>
 * Abstract Ability class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class Ability extends SpellAbility {

    protected Ability(final Card sourceCard, final ManaCost manaCost) {
        this(sourceCard, new Cost(manaCost, true), null);
    }
    protected Ability(final Card sourceCard, final ManaCost manaCost, SpellAbilityView view0) {
        this(sourceCard, new Cost(manaCost, true), view0);
    }
    protected Ability(final Card sourceCard, final Cost cost) {
        this(sourceCard, cost, null);
    }
    protected Ability(final Card sourceCard, final Cost cost, SpellAbilityView view0) {
        super(sourceCard, cost, view0);
    }

    /**
     * <p>
     * Constructor for Ability.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.game.card.Card} object.
     * @param manaCost
     *            a {@link java.lang.String} object.
     * @param stackDescription
     *            a {@link java.lang.String} object.
     */
    public Ability(final Card sourceCard, final ManaCost manaCost, final String stackDescription) {
        this(sourceCard, manaCost);
        this.setStackDescription(stackDescription);
        Log.debug("an ability is being played from" + sourceCard.getName());
    }

    /** {@inheritDoc} */
    @Override
    public boolean canPlay() {
        final Game game = getActivatingPlayer().getGame();
        if (game.getStack().isSplitSecondOnStack() && !this.isManaAbility()) {
            return false;
        }

        return this.getHostCard().isInPlay() && !this.getHostCard().isFaceDown();
    }

}
