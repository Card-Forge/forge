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
import java.util.Collections;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.card.spellability.SpellAbility;
import forge.game.player.Player;
import forge.util.MyRandom;


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


    /**
     * <p>
     * shuffle.
     * </p>
     */
    public final void shuffle() {
        // reseed Random each time we want to Shuffle
        // MyRandom.random = MyRandom.random;
        Collections.shuffle(this, MyRandom.getRandom());
        Collections.shuffle(this, MyRandom.getRandom());
        Collections.shuffle(this, MyRandom.getRandom());
    }

    /**
     * Create a new list of cards by applying a filter to this one.
     * 
     * @param filt
     *            determines which cards are present in the resulting list
     * 
     * @return a subset of this CardList whose items meet the filtering
     *         criteria; may be empty, but never null.
     */
    public final CardList filter(final Predicate<Card> filt) {
        return new CardList(Iterables.filter(this, filt));
    }

    public final boolean containsName(final String name) {
        return Iterables.any(this, CardPredicates.nameEquals(name));
    }

    public final CardList getController(final Player player) {
        return this.filter(CardPredicates.isController(player));
    }


    // cardType is like "Land" or "Goblin", returns a new CardList that is a
    // subset of current CardList
    public final CardList getType(final String cardType) {
        return this.filter(CardPredicates.isType(cardType));
    }

    // cardType is like "Land" or "Goblin", returns a new CardList with cards
    // that do not have this type
    public final CardList getNotType(final String cardType) {
        return this.filter(Predicates.not(CardPredicates.isType(cardType)));
    }

    public final CardList getKeyword(final String keyword) {
        return this.filter(CardPredicates.hasKeyword(keyword));
    }

    public final CardList getNotKeyword(final String keyword) {
        return this.filter(Predicates.not(CardPredicates.hasKeyword(keyword)));
    }

    public final CardList getTargetableCards(final SpellAbility source) {
        return this.filter(CardPredicates.isTargetableBy(source));
    }

    public final CardList getUnprotectedCards(final Card source) {
        return this.filter(Predicates.not(CardPredicates.isProtectedFrom(source)));
    }

    public final CardList getValidCards(final String restrictions, final Player sourceController, final Card source) {
        return this.filter(CardPredicates.restriction(restrictions.split(","), sourceController, source));
    }

    public final CardList getValidCards(final String[] restrictions, final Player sourceController, final Card source) {
        return this.filter(CardPredicates.restriction(restrictions, sourceController, source));
    }



} // end class CardList
