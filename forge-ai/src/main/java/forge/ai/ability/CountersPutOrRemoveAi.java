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
package forge.ai.ability;

import com.google.common.base.Predicate;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

import java.util.List;

/**
 * <p>
 * AbilityFactory_PutOrRemoveCountersAi class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CountersPutOrRemoveAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        return doTriggerAINoCost(ai, sa, false);
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        // if Defined, don't worry about targeting

        List<ZoneType> zones = ZoneType.listValueOf(sa.getParamOrDefault("TgtZones", "Battlefield"));
        List<Card> validCards = CardLists.getValidCards(ai.getGame().getCardsIn(zones),
                tgt.getValidTgts(), sa.getActivatingPlayer(), sa.getHostCard(), sa);

        if (validCards.isEmpty()) {
            return false;
        }

        List<Card> cWithCounters = CardLists.filter(validCards, new Predicate<Card>() {
            @Override
            public boolean apply(final Card crd) {
                return crd.hasCounters();
            }
        });

        if (cWithCounters.isEmpty()) {
            if (mandatory) {
                cWithCounters = validCards;
            } else {
                return false;
            }
        }

        while (sa.getTargets().getNumTargeted() < tgt.getMaxTargets(sa.getHostCard(), sa)) {
            Card targetCard = null;
            if (cWithCounters.isEmpty() && ((sa.getTargets().getNumTargeted() < tgt.getMinTargets(sa.getHostCard(), sa))
                    || (sa.getTargets().getNumTargeted() == 0))) {
                sa.resetTargets();
                return false;
            }
            
            int random = MyRandom.getRandom().nextInt(cWithCounters.size());
            targetCard = cWithCounters.get(random);

            sa.getTargets().add(targetCard);
            cWithCounters.remove(targetCard);
        }
        return true;
    }

}
