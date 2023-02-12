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

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Maps;

import forge.game.ability.ApiType;
import forge.game.ability.SpellApiBased;
import forge.game.card.Card;
import forge.game.card.CardState;
import forge.game.cost.Cost;

/**
 * <p>
 * Spell_Permanent class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class SpellPermanent extends SpellApiBased {
    /** Constant <code>serialVersionUID=2413495058630644447L</code>. */
    private static final long serialVersionUID = 2413495058630644447L;

    /**
     * <p>
     * Constructor for Spell_Permanent.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.game.card.Card} object.
     */
    public SpellPermanent(final Card sourceCard) {
        this(sourceCard, sourceCard.getCurrentState(), new Cost(sourceCard.getManaCost(), false));
    }
    public SpellPermanent(final Card sourceCard, final CardState cardstate) {
        this(sourceCard, cardstate, new Cost(cardstate.getManaCost(), false));
    }
    public SpellPermanent(final Card sourceCard, final CardState cardstate, final Cost cost) {
        super(cardstate.getType().isCreature() ? ApiType.PermanentCreature : ApiType.PermanentNoncreature, sourceCard,
                cost, null, Maps.newHashMap());

        // reset StackDescription for something with Text
        this.setStackDescription("");
        this.setDescription(this.getStackDescription());

        if (costHasManaX() && StringUtils.isNotBlank(getHostCard().getSVar("X"))) {
            this.setSVar("X", this.getHostCard().getSVar("X"));
        }
    } // Spell_Permanent()

}
