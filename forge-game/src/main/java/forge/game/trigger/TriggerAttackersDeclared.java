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

import java.util.*;

import com.google.common.collect.Iterables;
import forge.game.GameEntity;
import forge.game.GameObjectPredicates;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;
import forge.util.collect.FCollection;

/**
 * TODO Write javadoc for this type.
 * 
 */
public class TriggerAttackersDeclared extends Trigger {

    /**
     * Instantiates a new trigger_ attackers declared.
     * 
     * @param params
     *            the params
     * @param host
     *            the host
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerAttackersDeclared(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc}
     * @param runParams*/
    @Override
    public final boolean performTest(final Map<AbilityKey, Object> runParams) {
        if (!matchesValidParam("AttackingPlayer", runParams.get(AbilityKey.AttackingPlayer))) {
            return false;
        }
        if (!matchesValidParam("AttackedTarget", runParams.get(AbilityKey.AttackedTarget))) {
            return false;
        }
        if (!matchesValidParam("ValidAttackers", runParams.get(AbilityKey.Attackers))) {
            return false;
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa, Map<AbilityKey, Object> runParams) {
        Iterable<GameEntity> attackedTarget = (Iterable<GameEntity>) runParams.get(AbilityKey.AttackedTarget);

        CardCollection attackers = (CardCollection)(runParams.get(AbilityKey.Attackers));
        if (hasParam("ValidAttackers")) {
            attackers = CardLists.getValidCards(attackers, getParam("ValidAttackers").split(","), getHostCard().getController(), getHostCard(), this);
            FCollection<GameEntity> defenders = new FCollection<>();
            for (Card attacker : attackers) {
                defenders.add(attacker.getGame().getCombat().getDefenderByAttacker(attacker));
            }
            attackedTarget = defenders;
        }
        sa.setTriggeringObject(AbilityKey.Attackers, attackers);

        if (hasParam("AttackedTarget")) {
            attackedTarget = Iterables.filter(attackedTarget, GameObjectPredicates.restriction(getParam("AttackedTarget").split(","), getHostCard().getController(), getHostCard(), this));
        }
        sa.setTriggeringObject(AbilityKey.AttackedTarget, attackedTarget);

        sa.setTriggeringObjectsFrom(runParams, AbilityKey.AttackingPlayer);
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append(Localizer.getInstance().getMessage("lblNumberAttackers")).append(": ").append(sa.getTriggeringObject(AbilityKey.Attackers));
        return sb.toString();
    }
}
