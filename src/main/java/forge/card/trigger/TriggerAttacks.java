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
package forge.card.trigger;

import java.util.HashMap;
import java.util.Map;

import forge.Card;
import forge.CardList;
import forge.GameEntity;
import forge.card.spellability.SpellAbility;

/**
 * <p>
 * Trigger_Attacks class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class TriggerAttacks extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_Attacks.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerAttacks(final HashMap<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final Map<String, Object> runParams2) {
        if (this.getMapParams().containsKey("ValidCard")) {
            if (!matchesValid(runParams2.get("Attacker"), this.getMapParams().get("ValidCard").split(","),
                    this.getHostCard())) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("Attacked")) {
            if (this.getMapParams().get("Attacked").equals("Player") 
                    && ((GameEntity) runParams2.get("Attacked") instanceof Card)) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("Alone")) {
            final CardList otherAttackers = (CardList) runParams2.get("OtherAttackers");
            if (otherAttackers == null) {
                return false;
            }
            if (this.getMapParams().get("Alone").equals("True")) {
                if (otherAttackers.size() != 0) {
                    return false;
                }
            } else {
                if (otherAttackers.size() == 0) {
                    return false;
                }
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final Trigger getCopy() {
        final Trigger copy = new TriggerAttacks(this.getMapParams(), this.getHostCard(), this.isIntrinsic());
        if (this.getOverridingAbility() != null) {
            copy.setOverridingAbility(this.getOverridingAbility());
        }

        copyFieldsTo(copy);
        return copy;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa) {
        sa.setTriggeringObject("Attacker", this.getRunParams().get("Attacker"));
    }
}
