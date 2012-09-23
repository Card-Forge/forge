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
import java.util.Comparator;
import java.util.Iterator;
import com.google.code.jyield.Generator;
import com.google.code.jyield.Yieldable;

import forge.card.spellability.SpellAbility;
import forge.game.phase.CombatUtil;
import forge.game.player.Player;
import forge.util.MyRandom;
import forge.util.closures.Predicate;

/**
 * <p>
 * CardList class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardList implements Iterable<Card> {

    /**
     * <p>
     * iterator.
     * </p>
     * 
     * @return a {@link java.util.Iterator} object.
     */
    @Override
    public final Iterator<Card> iterator() {
        return this.list.iterator();
    }

    private ArrayList<Card> list = new ArrayList<Card>();

    /**
     * <p>
     * Constructor for CardList.
     * </p>
     */
    public CardList() {
    }

    /**
     * <p>
     * Constructor for CardList.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public CardList(final Card c) {
        this.add(c);
    }

    /**
     * <p>
     * Constructor for CardList.
     * </p>
     * 
     * @param al
     *            a {@link java.util.ArrayList} object.
     */
    public CardList(final Iterable<Card> al) {
        this.addAll(al);
    }

    /**
     * Create a CardList from a finite generator of Card instances.
     * 
     * We ignore null values produced by the generator.
     * 
     * @param generator
     *            a non-infinite generator of Card instances.
     */
    public CardList(final Generator<Card> generator) {
        // Generators yield their contents to a Yieldable. Here,
        // we create a quick Yieldable that adds the information it
        // receives to this CardList's list field.

        final Yieldable<Card> valueReceiver = new Yieldable<Card>() {
            @Override
            public void yield(final Card card) {
                if (card != null) {
                    CardList.this.list.add(card);
                }
            }
        };

        generator.generate(valueReceiver);
    }

    /**
     * Create a cardlist with an initial estimate of its maximum size.
     * 
     * @param size
     *            an initialize estimate of its maximum size
     */
    public CardList(final int size) {
        this.list = new ArrayList<Card>(size);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean equals(final Object a) {
        if (a instanceof CardList) {
            final CardList b = (CardList) a;
            if (this.list.size() != b.size()) {
                return false;
            }
    
            for (int i = 0; i < this.list.size(); i++) {
                if (!this.list.get(i).equals(b.get(i))) {
                    return false;
                }
            }
    
            return true;
        } else {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return (41 * (41 + this.list.size() + this.list.hashCode()));
    }

    /**
     * <p>
     * add.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void add(final Card c) {
        this.list.add(c);
    }

    /**
     * <p>
     * add.
     * </p>
     * 
     * @param n
     *            a int.
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void add(final int n, final Card c) {
        this.list.add(n, c);
    }

    /**
     * addAll(CardList) - lets you add one CardList to another directly.
     * 
     * @param in
     *            - CardList to add to the current CardList
     */
    public final void addAll(final Iterable<Card> in) {
        for (final Card element : in) {
            this.list.add(element);
        }
    }

    /**
     * <p>
     * addAll.
     * </p>
     * 
     * @param c
     *            an array of {@link java.lang.Object} objects.
     */
    public final void addAll(final Card[] c) {
        for (final Object element : c) {
            this.list.add((Card) element);
        }
    }

    /**
     * <p>
     * remove.
     * </p>
     * 
     * @param i
     *            a int.
     * @return a {@link forge.Card} object.
     */
    public final Card remove(final int i) {
        return this.list.remove(i);
    }

    /**
     * <p>
     * remove.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void remove(final Card c) {
        this.list.remove(c);
    }

    /**
     * <p>
     * removeAll.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void removeAll(final Card c) {
        final ArrayList<Card> cList = new ArrayList<Card>();
        cList.add(c);
        this.list.removeAll(cList);
    }

    /**
     * <p>
     * removeAll.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void removeAll(final CardList list) {
        for (Card c : list) {
            this.list.remove(c);
        }
    }

    /**
     * <p>
     * isEmpty.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isEmpty() {
        return this.list.isEmpty();
    }

    /**
     * <p>
     * size.
     * </p>
     * 
     * @return a int.
     */
    public final int size() {
        return this.list.size();
    }
    
    public final int indexOf(Card obj) {
        return list.indexOf(obj);
    }

    /**
     * <p>
     * get.
     * </p>
     * 
     * @param i
     *            a int.
     * @return a {@link forge.Card} object.
     */
    public final Card get(final int i) {
        return this.list.get(i);
    }

    /**
     * <p>
     * clear.
     * </p>
     */
    public final void clear() {
        this.list.clear();
    }

    /**
     * <p>
     * shuffle.
     * </p>
     */
    public final void shuffle() {
        // reseed Random each time we want to Shuffle
        // MyRandom.random = MyRandom.random;
        Collections.shuffle(this.list, MyRandom.getRandom());
        Collections.shuffle(this.list, MyRandom.getRandom());
        Collections.shuffle(this.list, MyRandom.getRandom());
    }

    /**
     * <p>
     * reverse.
     * </p>
     */
    public final void reverse() {
        Collections.reverse(this.list);
    }
    
    /**
     * <p>
     * toArray.
     * </p>
     * 
     * @return an array of {@link forge.Card} objects.
     */
    public final Card[] toArray() {
        final Card[] c = new Card[this.list.size()];
        this.list.toArray(c);
        return c;
    }

    /**
     * <p>
     * sort.
     * </p>
     * 
     * @param c
     *            a {@link java.util.Comparator} object.
     */
    public final void sort(final Comparator<Card> c) {
        Collections.sort(this.list, c);
    }

    /** {@inheritDoc} */
    @Override
    public final String toString() {
        return this.list.toString();
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
        return new CardList(filt.select(this));
    }

    /**
     * <p>
     * getColor.
     * </p>
     * 
     * @param cardColor
     *            a {@link java.lang.String} object.
     * @return a {@link forge.CardList} object.
     */
    public final CardList getColor(final String cardColor) {
        final CardList list = new CardList();
        for (final Card c : this) {
            if (cardColor.equals("Multicolor") && (c.getColor().size() > 1)) {
                list.add(c);
            } else if (c.isColor(cardColor) && (c.getColor().size() == 1)) {
                list.add(c);
            }
        }
        return list;
    } // getColor()




    public final boolean contains(final Card c) {
        return this.list.contains(c);
    }

    public final boolean containsName(final String name) {
        return CardPredicates.nameEquals(name).any(list);
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
        return this.filter(Predicate.not(CardPredicates.isType(cardType)));
    }

    public final CardList getKeyword(final String keyword) {
        return this.filter(CardPredicates.hasKeyword(keyword));
    }

    public final CardList getNotKeyword(final String keyword) {
        return this.filter(Predicate.not(CardPredicates.hasKeyword(keyword)));
    }

    public final CardList getTargetableCards(final SpellAbility source) {
        return this.filter(CardPredicates.isTargetableBy(source));
    }

    /**
     * <p>
     * getUnprotectedCards.
     * </p>
     * 
     * @param source
     *            a {@link forge.Card} object.
     * @return a {@link forge.CardList} object.
     */
    public final CardList getUnprotectedCards(final Card source) {
        return this.filter(new Predicate<Card>() {
            @Override
            public boolean isTrue(final Card c) {
                return !c.hasProtectionFrom(source);
            }
        });
    }

    /**
     * <p>
     * getValidCards.
     * </p>
     * 
     * @param restrictions
     *            a {@link java.lang.String} object.
     * @param sourceController
     *            a {@link forge.game.player.Player} object.
     * @param source
     *            a {@link forge.Card} object.
     * @return a {@link forge.CardList} object.
     */
    public final CardList getValidCards(final String restrictions, final Player sourceController, final Card source) {
        return this.getValidCards(restrictions.split(","), sourceController, source);
    }

    /**
     * <p>
     * getValidCards.
     * </p>
     * 
     * @param restrictions
     *            a {@link java.lang.String} object.
     * @param sourceController
     *            a {@link forge.game.player.Player} object.
     * @param source
     *            a {@link forge.Card} object.
     * @return a {@link forge.CardList} object.
     */
    public final CardList getValidCards(final String[] restrictions, final Player sourceController, final Card source) {
        return this.filter(new Predicate<Card>() {
            @Override
            public boolean isTrue(final Card c) {
                return (c != null) && c.isValid(restrictions, sourceController, source);
            }
        });
    }

    /**
     * <p>
     * getPossibleBlockers.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    public final CardList getPossibleBlockers(final Card attacker) {
        return this.filter(new Predicate<Card>() {
            @Override
            public boolean isTrue(final Card c) {
                return (c.isCreature() && CombatUtil.canBlock(attacker, c));
            }
        });
    }

    /**
     * <p>
     * getPossibleAttackers.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    public final CardList getPossibleAttackers() {
        return this.filter(new Predicate<Card>() {
            @Override
            public boolean isTrue(final Card c) {
                return (c.isCreature() && CombatUtil.canAttack(c));
            }
        });
    }

    /**
     * <p>
     * getTotalConvertedManaCost.
     * </p>
     * 
     * @return a int.
     */
    public final int getTotalConvertedManaCost() {
        int total = 0;
        for (int i = 0; i < this.size(); i++) {
            total += this.get(i).getCMC();
        }
        return total;
    }

    /**
     * 
     * <p>
     * getTotalCreaturePower.
     * </p>
     * 
     * @return a int.
     */

    public final int getTotalCreaturePower() {
        int total = 0;
        for (int i = 0; i < this.size(); i++) {
            total += this.get(i).getCurrentPower();
        }
        return total;
    }

    /**
     * <p>
     * getHighestConvertedManaCost.
     * </p>
     * 
     * @return a int.
     * @since 1.0.15
     */
    public final int getHighestConvertedManaCost() {
        int total = 0;
        for (int i = 0; i < this.size(); i++) {
            total = Math.max(total, this.get(i).getCMC());
        }
        return total;
    }

    /**
     * <p>
     * getColored.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    public final CardList getColored() {
        return this.filter(new Predicate<Card>() {
            @Override
            public boolean isTrue(final Card c) {
                return (!c.isColorless());
            }
        });
    }

    /**
     * <p>
     * getMonoColored.
     * </p>
     * @param includeColorless should colorless cards be included?
     * 
     * @return a {@link forge.CardList} object.
     */
    public final CardList getMonoColored(final boolean includeColorless) {
        return this.filter(new Predicate<Card>() {
            @Override
            public boolean isTrue(final Card c) {
                return (CardUtil.getColors(c).size() == 1 && (includeColorless || !c.isColorless()));
            }
        });
    }


} // end class CardList
