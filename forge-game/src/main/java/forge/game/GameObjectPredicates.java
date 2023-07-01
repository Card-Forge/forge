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
package forge.game;

import com.google.common.base.Predicate;

import forge.game.card.Card;
import forge.game.player.Player;


/**
 * <p>
 * Predicate<GameObject> interface.
 * </p>
 *
 * @author Forge
 */
public final class GameObjectPredicates {

    public static final Predicate<GameObject> restriction(final String[] restrictions, final Player sourceController, final Card source, final CardTraitBase spellAbility) {
        return new Predicate<GameObject>() {
            @Override
            public boolean apply(final GameObject c) {
                return c != null && c.isValid(restrictions, sourceController, source, spellAbility);
            }
        };
    }
}
