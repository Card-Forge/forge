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
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;

import java.util.List;
import java.util.Map;

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
	@SuppressWarnings("unchecked")
    @Override
    public final boolean performTest(final Map<AbilityKey, Object> runParams) {
        if (hasParam("AttackingPlayer")) {
            if (!matchesValid(runParams.get(AbilityKey.AttackingPlayer),
                    getParam("AttackingPlayer").split(","), this.getHostCard())) {
                return false;
            }
        }
        if (hasParam("AttackedTarget")) {
            boolean valid = false;
            List<GameEntity> list = (List<GameEntity>) runParams.get(AbilityKey.AttackedTarget);
            for (GameEntity b : list) {
                if (matchesValid(b, getParam("AttackedTarget").split(","), this.getHostCard())) {
                    valid = true;
                    break;
                }
            }
            if (!valid) {
                return false;
            }
        }
        if (hasParam("ValidAttackers")) {
            boolean valid = false;

            final Iterable<Card> srcs = (Iterable<Card>) runParams.get(AbilityKey.Attackers);
            for (Card c : srcs) {
                if (c.isValid(getParam("ValidAttackers").split(","), this.getHostCard().getController(), this.getHostCard(), null)) {
                    valid = true;
                }
            }
            if (!valid) {
                return false;
            }
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa, Map<AbilityKey, Object> runParams) {
        sa.setTriggeringObjectsFrom(runParams, AbilityKey.Attackers, AbilityKey.AttackingPlayer, AbilityKey.AttackedTarget);
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append(Localizer.getInstance().getMessage("lblNumberAttackers")).append(": ").append(sa.getTriggeringObject(AbilityKey.Attackers));
        return sb.toString();
    }
}
