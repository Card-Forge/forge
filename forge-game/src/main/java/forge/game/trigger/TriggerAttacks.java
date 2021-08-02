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

import java.util.List;
import java.util.Map;

import forge.game.GameEntity;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;

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
    public TriggerAttacks(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc}
     * @param runParams*/
    @Override
    public final boolean performTest(final Map<AbilityKey, Object> runParams) {
        if (!matchesValidParam("ValidCard", runParams.get(AbilityKey.Attacker))) {
            return false;
        }

        if (!matchesValidParam("Attacked", runParams.get(AbilityKey.Attacked))) {
            return false;
        }

        if (hasParam("Alone")) {
            @SuppressWarnings("unchecked")
            final List<Card> otherAttackers = (List<Card>) runParams.get(AbilityKey.OtherAttackers);
            if (otherAttackers == null) {
                return false;
            }
            if (getParam("Alone").equals("True")) {
                if (otherAttackers.size() != 0) {
                    return false;
                }
            } else {
                if (otherAttackers.size() == 0) {
                    return false;
                }
            }
        }

        if (hasParam("FirstAttack")) {
            Card attacker = (Card) runParams.get(AbilityKey.Attacker);
            if (attacker.getDamageHistory().getCreatureAttacksThisTurn() > 1) {
                return false;
            }
        }

        if (hasParam("DefendingPlayerPoisoned")) {
            Player defendingPlayer = (Player) runParams.get(AbilityKey.DefendingPlayer);
        	if (defendingPlayer.getPoisonCounters() == 0) {
        		return false;
        	}
        }

        if (hasParam("AttackDifferentPlayers")) {
            GameEntity attacked = (GameEntity) runParams.get(AbilityKey.Attacked);
            boolean found = false;
            if (attacked instanceof Player) {
                @SuppressWarnings("unchecked")
                List<GameEntity> list = (List<GameEntity>) runParams.get(AbilityKey.Defenders);
                for (GameEntity e : list) {
                    if ((e instanceof Player) && !e.equals(attacked)) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa, Map<AbilityKey, Object> runParams) {
        sa.setTriggeringObject(AbilityKey.Defender, runParams.get(AbilityKey.Attacked));
        sa.setTriggeringObjectsFrom(
            runParams,
            AbilityKey.Attacker,
            AbilityKey.Defenders,
            AbilityKey.DefendingPlayer
        );
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();

        sb.append(Localizer.getInstance().getMessage("lblAttacker")).append(": ").append(sa.getTriggeringObject(AbilityKey.Attacker));
        return sb.toString();
    }
}
