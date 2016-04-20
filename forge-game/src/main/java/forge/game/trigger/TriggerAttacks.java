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
package forge.game.trigger;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import java.util.List;
import java.util.Map;

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
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerAttacks(final java.util.Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final Map<String, Object> runParams2) {
        if (this.mapParams.containsKey("ValidCard")) {
            if (!matchesValid(runParams2.get("Attacker"), this.mapParams.get("ValidCard").split(","),
                    this.getHostCard())) {
                return false;
            }
        }

        if (this.mapParams.containsKey("Attacked")) {
            GameEntity attacked = (GameEntity) runParams2.get("Attacked");
            if (!attacked.isValid(this.mapParams.get("Attacked").split(",")
                    , this.getHostCard().getController(), this.getHostCard(), null)) {
                return false;
            }
        }

        if (this.mapParams.containsKey("Alone")) {
            @SuppressWarnings("unchecked")
            final List<Card> otherAttackers = (List<Card>) runParams2.get("OtherAttackers");
            if (otherAttackers == null) {
                return false;
            }
            if (this.mapParams.get("Alone").equals("True")) {
                if (otherAttackers.size() != 0) {
                    return false;
                }
            } else {
                if (otherAttackers.size() == 0) {
                    return false;
                }
            }
        }

        if (this.mapParams.containsKey("FirstAttack")) {
            Card attacker = (Card) runParams2.get("Attacker");
            if (attacker.getDamageHistory().getCreatureAttacksThisTurn() > 1) {
                return false;
            }
        }

        if (this.mapParams.containsKey("DefendingPlayerPoisoned")) {
            Player defendingPlayer = (Player) runParams2.get("DefendingPlayer");
        	if (defendingPlayer.getPoisonCounters() == 0) {
        		return false;
        	}
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa) {
        sa.setTriggeringObject("Attacker", this.getRunParams().get("Attacker"));
        sa.setTriggeringObject("Defender", this.getRunParams().get("Attacked"));
        sa.setTriggeringObject("DefendingPlayer", this.getRunParams().get("DefendingPlayer"));
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();

        sb.append("Attacker: ").append(sa.getTriggeringObject("Attacker"));
        return sb.toString();
    }
}
