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
package forge.game.zone;

import java.util.List;

import com.google.common.base.Predicate;

import forge.Card;
import forge.game.player.Player;

/**
 * <p>
 * IPlayerZone interface.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
interface IZone {

    int size();
    boolean isEmpty();

    boolean contains(Card c);
    boolean contains(final Predicate<Card> condition);

    void add(Card o);
    void add(Card c, int index);
    void remove(Card o);
    void setCards(Iterable<Card> c);

    Card get(int index);
    List<Card> getCards(boolean filter);
    List<Card> getCards();

    ZoneType getZoneType();
    Player getPlayer();
    boolean is(ZoneType zone);
    boolean is(ZoneType zone, Player player);
    

    void resetCardsAddedThisTurn();
}
