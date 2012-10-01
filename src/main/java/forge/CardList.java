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
package forge;

import java.util.ArrayList;


/**
 * <p>
 * CardList class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardList extends ArrayList<Card> {

    private static final long serialVersionUID = 7912620750458976012L;

    public CardList() {}
    public CardList(final Card c) { this.add(c); }
    public CardList(final Iterable<Card> al) { for(Card c : al) this.add(c); }

} // end class CardList
