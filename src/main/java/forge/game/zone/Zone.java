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
import java.util.Observable;
import java.util.Observer;

import forge.Card;

import forge.Singletons;
import forge.util.MyObservable;

/**
 * <p>
 * DefaultPlayerZone class.
 * </p>
 * 
 * @author Forge
 * @version $Id: PlayerZone.java 17582 2012-10-19 22:39:09Z Max mtg $
 */
public class Zone extends MyObservable implements IZone, Observer, java.io.Serializable, Iterable<Card> {
    /** Constant <code>serialVersionUID=-5687652485777639176L</code>. */
    private static final long serialVersionUID = -5687652485777639176L;

    /** The cards. */
    protected final List<Card> cardList = new ArrayList<Card>();
    protected final Collection<Card> roCardList;    
    protected final ZoneType zoneName;
    protected boolean update = true;

    protected final List<Card> cardsAddedThisTurn = new ArrayList<Card>();
    protected final ArrayList<ZoneType> cardsAddedThisTurnSource = new ArrayList<ZoneType>();

    

    /**
     * <p>
     * Constructor for DefaultPlayerZone.
     * </p>
     * 
     * @param zone
     *            a {@link java.lang.String} object.
     * @param inPlayer
     *            a {@link forge.game.player.Player} object.
     */
    public Zone(final ZoneType zone) {
        this.zoneName = zone;
        this.roCardList = Collections.unmodifiableCollection(cardList);
    }

    // ************ BEGIN - these methods fire updateObservers() *************

    @Override
    public void add(final Object o, boolean update) {
        final Card c = (Card) o;

        // Immutable cards are usually emblems,effects and the mana pool and we
        // don't want to log those.
        if (!c.isImmutable()) {
            this.cardsAddedThisTurn.add(c);
            final Zone zone = Singletons.getModel().getGame().getZoneOf(c);
            if (zone != null) {
                this.cardsAddedThisTurnSource.add(zone.getZoneType());
            } else {
                this.cardsAddedThisTurnSource.add(null);
            }
        }

        c.addObserver(this);
        c.setTurnInZone(Singletons.getModel().getGame().getPhaseHandler().getTurn());
        c.setTapped(false);

        this.cardList.add(c);
        
        if (update) {
            this.update();
        }
    }
    
    
    /**
     * Adds the.
     * 
     * @param o
     *            a {@link java.lang.Object} object.
     */
    @Override
    public void add(final Object o) {
        this.add(o, true);
    }

    /**
     * Update.
     * 
     * @param ob
     *            an Observable
     * @param object
     *            an Object
     */
    @Override
    public final void update(final Observable ob, final Object object) {
        this.update();
    }

    /**
     * Adds the.
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param index
     *            a int.
     */
    @Override
    public final void add(final Card c, final int index) {
        // Immutable cards are usually emblems,effects and the mana pool and we
        // don't want to log those.
        if (!c.isImmutable()) {
            this.cardsAddedThisTurn.add(c);
            final Zone zone = Singletons.getModel().getGame().getZoneOf(c);
            if (zone != null) {
                this.cardsAddedThisTurnSource.add(zone.getZoneType());
            } else {
                this.cardsAddedThisTurnSource.add(null);
            }
        }

        if (!this.is(ZoneType.Battlefield)) {
            c.setTapped(false);
        }

        this.cardList.add(index, c);
        c.setTurnInZone(Singletons.getModel().getGame().getPhaseHandler().getTurn());
        this.update();
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.IPlayerZone#contains(forge.Card)
     */
    /**
     * Contains.
     * 
     * @param c
     *            Card
     * @return boolean
     */
    @Override
    public final boolean contains(final Card c) {
        return this.cardList.contains(c);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.IPlayerZone#getPosition(forge.Card)
     */
    @Override
    public final Integer getPosition(final Card c) {
        int index = this.cardList.indexOf(c);
        if (index == -1) {
            return null;
        }
        return index;
    }

    /**
     * Removes the.
     * 
     * @param c
     *            an Object
     */
    @Override
    public void remove(final Object c) {
        this.cardList.remove(c);
        this.update();
    }

    /**
     * <p>
     * Setter for the field <code>cards</code>.
     * </p>
     * 
     * @param c
     *            an array of {@link forge.Card} objects.
     */
    @Override
    public final void setCards(final Iterable<Card> cards) {
        cardList.clear();
        for(Card c : cards)
            cardList.add(c);
        this.update();
    }

    // ************ END - these methods fire updateObservers() *************

    /**
     * Checks if is.
     * 
     * @param zone
     *            a {@link java.lang.String} object.
     * @return a boolean
     */
    @Override
    public final boolean is(final ZoneType zone) {
        return zone == this.zoneName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.IPlayerZone#is(java.util.List)
     */
    @Override
    public final boolean is(final List<ZoneType> zones) {
        return zones.contains(this.zoneName);
    }

    /**
     * <p>
     * Getter for the field <code>zoneName</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    @Override
    public final ZoneType getZoneType() {
        return this.zoneName;
    }

    /**
     * <p>
     * size.
     * </p>
     * 
     * @return a int.
     */
    @Override
    public final int size() {
        return this.cardList.size();
    }

    /**
     * Gets the.
     * 
     * @param index
     *            a int.
     * @return a int
     */
    @Override
    public final Card get(final int index) {
        return this.cardList.get(index);
    }

    /**
     * <p>
     * Getter for the field <code>cards</code>.
     * </p>
     * 
     * @return an array of {@link forge.Card} objects.
     */
    @Override
    public final List<Card> getCards() {
        return this.getCards(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.IPlayerZone#getCards(boolean)
     */
    @Override
    public List<Card> getCards(final boolean filter) {
        // Non-Battlefield PlayerZones don't care about the filter
        return new ArrayList<Card>(this.cardList);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.IPlayerZone#getCards(int)
     */
    @Override
    public final List<Card> getCards(final int n) {
        return this.cardList.subList(0, Math.min(this.cardList.size(), n));
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
     * update.
     * </p>
     */
    public final void update() {
        if (this.update) {
            this.updateObservers();
        }
    }

    /**
     * Sets the update.
     * 
     * @param b
     *            a boolean.
     */
    @Override
    public final void setUpdate(final boolean b) {
        this.update = b;
    }

    /**
     * <p>
     * Getter for the field <code>update</code>.
     * </p>
     * 
     * @return a boolean.
     */
    @Override
    public final boolean getUpdate() {
        return this.update;
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
        return this.zoneName.toString();
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
        final List<Card> ret = new ArrayList<Card>();
        for (int i = 0; i < this.cardsAddedThisTurn.size(); i++) {
            if ((this.cardsAddedThisTurnSource.get(i) == origin) || (origin == null)) {
                ret.add(this.cardsAddedThisTurn.get(i));
            }
        }
        //System.out.println(ret.size());
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
        this.cardsAddedThisTurnSource.clear();
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<Card> iterator() {
        return roCardList.iterator();
    }

    /**
     * TODO: Write javadoc for this method.
     */
    public void updateLabelObservers() {
    }

}
