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

import forge.game.GameEntity;
import forge.game.GameObjectPredicates;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Aggregates;
import forge.util.Expressions;
import forge.util.IterableUtil;
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
        // a team's attack runs once per attacking player for triggers keyed to one, and once whole for the rest (CR 805.10c)
        if (runParams.containsKey(AbilityKey.OneOfAttackingTeam)) {
            final boolean perAttackingPlayer = hasParam("AttackingPlayer") || refersToTriggering(AbilityKey.AttackingPlayer);
            if ((boolean) runParams.get(AbilityKey.OneOfAttackingTeam) != perAttackingPlayer) {
                return false;
            }
        }
        if (!matchesValidParam("AttackingPlayer", runParams.get(AbilityKey.AttackingPlayer))) {
            return false;
        }
        if (!matchesValidParam("AttackedTarget", runParams.get(AbilityKey.AttackedTarget))) {
            return false;
        }
        if (!matchesAttackers((CardCollection) runParams.get(AbilityKey.Attackers))) {
            return false;
        }

        if (hasParam("FirstTime")) {
            // each attacking player of a team is only compared to their own creatures
            final boolean ownOnly = (boolean) runParams.getOrDefault(AbilityKey.OneOfAttackingTeam, false);
            final Player attackingPlayer = (Player) runParams.get(AbilityKey.AttackingPlayer);
            for (final CardCollection before : (List<CardCollection>) runParams.getOrDefault(AbilityKey.AttackersBefore, List.of())) {
                if (matchesAttackers(ownOnly ? CardLists.filterControlledBy(before, attackingPlayer) : before)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean matchesAttackers(final Iterable<Card> declared) {
        if (hasParam("ValidAttackers")) {
            String param = getParamOrDefault("ValidAttackersAmount", "GE1");
            int attackers = CardLists.getValidCardCount(declared, getParam("ValidAttackers"), getHostCard().getController(), getHostCard(), this);
            int amount = AbilityUtils.calculateAmount(getHostCard(), param.substring(2), this);
            if (!Expressions.compare(attackers, param, amount)) {
                return false;
            }
        }
        if (hasParam("AttackersTotalPower")) {
            String param = getParam("AttackersTotalPower");
            int amount = AbilityUtils.calculateAmount(getHostCard(), param.substring(2), this);
            if (!Expressions.compare(Aggregates.sum(declared, Card::getNetPower), param, amount)) {
                return false;
            }
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final Map<AbilityKey, Object> objects, Map<AbilityKey, Object> runParams) {
        Iterable<GameEntity> attackedTarget = (Iterable<GameEntity>) runParams.get(AbilityKey.AttackedTarget);

        CardCollection attackers = (CardCollection) runParams.get(AbilityKey.Attackers);
        if (hasParam("ValidAttackers")) {
            attackers = CardLists.getValidCards(attackers, getParam("ValidAttackers"), getHostCard().getController(), getHostCard(), this);
            FCollection<GameEntity> defenders = new FCollection<>();
            for (Card attacker : attackers) {
                defenders.add(attacker.getGame().getCombat().getDefenderByAttacker(attacker));
            }
            attackedTarget = defenders;
        }
        objects.put(AbilityKey.Attackers, attackers);

        if (hasParam("AttackedTarget")) {
            attackedTarget = IterableUtil.filter(attackedTarget, GameObjectPredicates.restriction(getParam("AttackedTarget").split(","), getHostCard().getController(), getHostCard(), this));
        }
        objects.put(AbilityKey.AttackedTarget, attackedTarget);

        AbilityKey.copyKeys(runParams, objects, AbilityKey.AttackingPlayer);
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append(Localizer.getInstance().getMessage("lblNumberAttackers")).append(": ").append(sa.getTriggeringObject(AbilityKey.Attackers));
        return sb.toString();
    }
}
