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

import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;


/**
 * <p>
 * AbilityFactory_GainControlVariant class.
 * </p>
 * 
 * @author Forge
 * @version $Id: AbilityFactoryGainControl.java 17764 2012-10-29 11:04:18Z Sloth $
 */
public class ControlGainVariantAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(final Player ai, final SpellAbility sa) {

        String logic = sa.getParam("AILogic");

        if ("GainControlOwns".equals(logic)) {
            List<Card> list = CardLists.filter(ai.getGame().getCardsIn(ZoneType.Battlefield), new Predicate<Card>() {
                @Override
                public boolean apply(final Card crd) {
                    return crd.isCreature() && !crd.getController().equals(crd.getOwner());
                }
            });
            if (list.isEmpty()) {
                return false;
            }
            for (final Card c : list) {
                if (ai.equals(c.getController())) {
                    return false;
                }
            }
        }

        return true;

    }

    @Override
    public Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        Iterable<Card> otherCtrl = CardLists.filter(options, Predicates.not(CardPredicates.isController(ai)));
        if (Iterables.isEmpty(otherCtrl)) {
            return ComputerUtilCard.getWorstAI(options);
        } else {
            return ComputerUtilCard.getBestAI(otherCtrl);
        }
    }

}
