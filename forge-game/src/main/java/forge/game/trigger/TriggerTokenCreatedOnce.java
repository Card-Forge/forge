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

import java.util.Collections;
import java.util.Map;

import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardPredicates;
import forge.game.player.PlayerCollection;
import forge.game.spellability.SpellAbility;
import forge.util.IterableUtil;

public class TriggerTokenCreatedOnce extends Trigger {

    public TriggerTokenCreatedOnce(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        return "";
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa, Map<AbilityKey, Object> runParams) {
        @SuppressWarnings("unchecked")
        Iterable<Card> tokens = (Iterable<Card>) runParams.get(AbilityKey.Cards);
        if (hasParam("ValidToken")) {
            tokens = IterableUtil.filter(tokens, CardPredicates.restriction(getParam("ValidToken").split(","), getHostCard().getController(), getHostCard(), this));
        }

        sa.setTriggeringObject(AbilityKey.Cards, tokens);
    }

    /** {@inheritDoc}
     * @param runParams*/
    @Override
    public final boolean performTest(final Map<AbilityKey, Object> runParams) {
        if (!matchesValidParam("ValidToken", runParams.get(AbilityKey.Cards))) {
            return false;
        }

        if (hasParam("OnlyFirst")) {
            if (Collections.disjoint(((PlayerCollection) runParams.get(AbilityKey.FirstTime)), AbilityUtils.getDefinedPlayers(getHostCard(), getParam("OnlyFirst"), this))) {
                return false;
            }
        }
        return true;
    }

}
