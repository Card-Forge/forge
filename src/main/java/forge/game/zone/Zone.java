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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.Card;
import forge.game.Game;
import forge.game.event.EventValueChangeType;
import forge.game.event.GameEventZone;
import forge.game.player.Player;
import forge.util.maps.CollectionSuppliers;
import forge.util.maps.EnumMapOfLists;
import forge.util.maps.MapOfLists;

/**
 * <p>
 * DefaultPlayerZone class.
 * </p>
 * 
 * @author Forge
 * @version $Id: PlayerZone.java 17582 2012-10-19 22:39:09Z Max mtg $
 */
public class Zone implements IZone, java.io.Serializable, Iterable<Card> {
    /** Constant <code>serialVersionUID=-5687652485777639176L</code>. */
    private static final long serialVersionUID = -5687652485777639176L;

    /** The cards. */
    private final transient List<Card> cardList = new CopyOnWriteArrayList<Card>();
    protected final transient List<Card> roCardList;
    protected final ZoneType zoneType;
    protected final Game game;

    protected final transient MapOfLists<ZoneType, Card> cardsAddedThisTurn = new EnumMapOfLists<>(ZoneType.class, CollectionSuppliers.<Card>arrayLists());


    public Zone(final ZoneType zone, Game game) {
        this.zoneType = zone;
        this.game = game;
        this.roCardList = Collections.unmodifiableList(cardList);
        
        //System.out.println(zoneName + " (ct) " + Integer.toHexString(System.identityHashCode(roCardList)));
    }

    @Override
    public Player getPlayer() { // generic zones like stack have no player associated
        return null;
    }
    
    @Override
    public void add(final Card c) {
        logCardAdded(c);
        updateCardState(c);
        this.cardList.add(c);
        c.setZone(this);
        game.fireEvent(new GameEventZone(zoneType, getPlayer(), EventValueChangeType.Added, c));
    }


    @Override
    public final void add(final Card c, final int index) {
        logCardAdded(c);
        updateCardState(c);
        this.cardList.add(index, c);
        c.setZone(this);
        game.fireEvent(new GameEventZone(zoneType, getPlayer(), EventValueChangeType.Added, c));
    }

    // Sets turn in zone... why not to add current zone reference into the card itself?
    private void updateCardState(final Card c) {
        c.setTurnInZone(game.getPhaseHandler().getTurn());
        if (zoneType != ZoneType.Battlefield) {
            c.setTapped(false);
        }
    }

    private void logCardAdded(final Card c) {
        // Immutable cards are usually emblems,effects and the mana pool and we
        // don't want to log those.
        if (c.isImmutable()) return;
    
        final Zone oldZone = game.getZoneOf(c);
        // if any tokens come to battlefield, consider they are from stack. Plain "null" cannot be a key of EnumMap
        final ZoneType zt = oldZone == null ? ZoneType.Stack : oldZone.getZoneType(); 
        cardsAddedThisTurn.add(zt, c);
    }

    @Override
    public final boolean contains(final Card c) {
        return this.cardList.contains(c);
    }
    
    public final boolean contains(final Predicate<Card> condition) {
        return Iterables.any(this.cardList, condition);
    }

    @Override
    public void remove(final Card c) {
        this.cardList.remove(c);
        game.fireEvent(new GameEventZone(zoneType, getPlayer(), EventValueChangeType.Removed, c));
    }

    @Override
    public final void setCards(final Iterable<Card> cards) {
        cardList.clear();
        for (Card c : cards) {
            c.setZone(this);
            cardList.add(c);
        }
        game.fireEvent(new GameEventZone(zoneType, getPlayer(), EventValueChangeType.ComplexUpdate, null));
        
    }

    @Override
    public final boolean is(final ZoneType zone) {
        return zone == this.zoneType;
    }

    // PlayerZone should override it with a correct implementation
    @Override
    public final boolean is(final ZoneType zone, final Player player) {
        return zoneType == zone && player == getPlayer();
    }

    @Override
    public final ZoneType getZoneType() {
        return this.zoneType;
    }

    @Override
    public final int size() {
        return this.cardList.size();
    }

    @Override
    public final Card get(final int index) {
        return this.cardList.get(index);
    }

    @Override
    public final List<Card> getCards() {
        //System.out.println(zoneName + ": " + Integer.toHexString(System.identityHashCode(roCardList)));
        return this.getCards(true);
    }

    @Override
    public List<Card> getCards(final boolean filter) {
        // Non-Battlefield PlayerZones don't care about the filter
        return this.roCardList;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.IPlayerZone#isEmpty()
     */
    @Override
    public final boolean isEmpty() {
        return this.cardList.isEmpty();
    }

    /**
     * <p>
     * Getter for the field <code>cardsAddedThisTurn</code>.
     * </p>
     * 
     * @param origin
     *            a {@link java.lang.String} object.
     * @return a {@link forge.CardList} object.
     */
    public final List<Card> getCardsAddedThisTurn(final ZoneType origin) {
        //System.out.print("Request cards put into " + this.getZoneType() + " from " + origin + ".Amount: ");
        if (origin != null) {
            Collection<Card> cards = cardsAddedThisTurn.get(origin);
            return cards == null ? Lists.<Card>newArrayList() : Lists.newArrayList(cards);
        }
        
        // all cards if key == null
        final List<Card> ret = new ArrayList<Card>();
        for(Collection<Card> kv : cardsAddedThisTurn.values()) 
            ret.addAll(kv);
        return ret;
    }

    /**
     * <p>
     * resetCardsAddedThisTurn.
     * </p>
     */
    @Override
    public final void resetCardsAddedThisTurn() {
        this.cardsAddedThisTurn.clear();
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<Card> iterator() {
        return roCardList.iterator();
    }

    public void shuffle()
    {
        Collections.shuffle(cardList);
    }

    /**
     * <p>
     * toString.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    @Override
    public String toString() {
        return this.zoneType.toString();
    }
    
    
}
