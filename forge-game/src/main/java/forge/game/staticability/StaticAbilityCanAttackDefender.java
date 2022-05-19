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
package forge.game.staticability;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.card.Card;

/**
 * The Class StaticAbility_CanAttackDefender.
 *  - used to allow cards with Defender keyword to attack normally
 */
public class StaticAbilityCanAttackDefender {

    public static boolean applyCanAttackAbility(final StaticAbility stAb, final Card card, final GameEntity target) {
        final Card hostCard = stAb.getHostCard();
        final Game game = hostCard.getGame();

        if (!stAb.matchesValidParam("ValidCard", card)) {
            return false;
        }

        if (!stAb.matchesValidParam("ValidAttacked", target)) {
            return false;
        }

        return true;
    }
}
