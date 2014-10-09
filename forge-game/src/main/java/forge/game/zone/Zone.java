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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.event.EventValueChangeType;
import forge.game.event.GameEventZone;
import forge.game.player.Player;
import forge.util.CollectionSuppliers;
import forge.util.maps.EnumMapOfLists;
import forge.util.maps.MapOfLists;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * <p>
 * DefaultPlayerZone class.
 * </p>
 * 
 * @author Forge
 * @version $Id: PlayerZone.java 17582 2012-10-19 22:39:09Z Max mtg $
 */
public class Zone implements java.io.Serializable, Iterable<Card> {
    private static final long serialVersionUID = -5687652485777639176L;

    private final transient List<Card> cardList = new CopyOnWriteArrayList<Card>();
    protected final transient List<Card> roCardList;
    protected final ZoneType zoneType;
    protected final Game game;

    protected final transient MapOfLists<ZoneType, Card> cardsAddedThisTurn = new EnumMapOfLists<>(ZoneType.class, CollectionSuppliers.<Card>arrayLists());
    protected final transient MapOfLists<ZoneType, Card> cardsAddedLastTurn = new EnumMapOfLists<>(ZoneType.class, CollectionSuppliers.<Card>arrayLists());

    public Zone(final ZoneType zone0, Game game0) {
        zoneType = zone0;
        game = game0;
        roCardList = Collections.unmodifiableList(cardList);
    }

    protected void onChanged() {
    }

    public Player getPlayer() { // generic zones like stack have no player associated
        return null;
    }

    public final void add(final Card c) {
        add(c, null);
    }

    public void add(final Card c, final Integer index) {
        // Immutable cards are usually emblems and effects
        if (!c.isImmutable()) {
            final Zone oldZone = game.getZoneOf(c);
            final ZoneType zt = oldZone == null ? ZoneType.Stack : oldZone.getZoneType();
            cardsAddedThisTurn.add(zt, c);
        }

        c.setTurnInZone(game.getPhaseHandler().getTurn());
        if (zoneType != ZoneType.Battlefield) {
            c.setTapped(false);
        }
        c.setZone(this);

        if (index == null) {
            cardList.add(c);
        }
        else {
            cardList.add(index.intValue(), c);
        }
        onChanged();
        game.fireEvent(new GameEventZone(zoneType, getPlayer(), EventValueChangeType.Added, c));
    }

    public final boolean contains(final Card c) {
        return cardList.contains(c);
    }

    public final boolean contains(final Predicate<Card> condition) {
        return Iterables.any(cardList, condition);
    }

    public void remove(final Card c) {
        if (cardList.remove(c)) {
            onChanged();
            game.fireEvent(new GameEventZone(zoneType, getPlayer(), EventValueChangeType.Removed, c));
        }
    }

    public final void setCards(final Iterable<Card> cards) {
        cardList.clear();
        for (Card c : cards) {
            c.setZone(this);
            cardList.add(c);
        }
        onChanged();
        game.fireEvent(new GameEventZone(zoneType, getPlayer(), EventValueChangeType.ComplexUpdate, null));
    }

    public final boolean is(final ZoneType zone) {
        return zone == zoneType;
    }

    public final boolean is(final ZoneType zone, final Player player) {
        return zoneType == zone && player == getPlayer();
    }

    public final ZoneType getZoneType() {
        return zoneType;
    }

    public final int size() {
        return cardList.size();
    }

    public final Card get(final int index) {
        return cardList.get(index);
    }

    public final List<Card> getCards() {
        return getCards(true);
    }

    public List<Card> getCards(final boolean filter) {
        return roCardList; // Non-Battlefield PlayerZones don't care about the filter
    }

    public final boolean isEmpty() {
        return cardList.isEmpty();
    }

    public final MapOfLists<ZoneType, Card> getCardsAddedThisTurn() {
        return cardsAddedThisTurn;
    }

    public final MapOfLists<ZoneType, Card> getCardsAddedLastTurn() {
        return cardsAddedLastTurn;
    }

    public final List<Card> getCardsAddedThisTurn(final ZoneType origin) {
        //System.out.print("Request cards put into " + getZoneType() + " from " + origin + ".Amount: ");
        return getCardsAdded(cardsAddedThisTurn, origin);
    }

    public final List<Card> getCardsAddedLastTurn(final ZoneType origin) {
        //System.out.print("Last turn - Request cards put into " + getZoneType() + " from " + origin + ".Amount: ");
        return getCardsAdded(cardsAddedLastTurn, origin);
    }

    private final List<Card> getCardsAdded(final MapOfLists<ZoneType, Card> cardsAdded, final ZoneType origin) {
        if (origin != null) {
            Collection<Card> cards = cardsAdded.get(origin);
            return cards == null ? Lists.<Card>newArrayList() : Lists.newArrayList(cards);
        }

        // all cards if key == null
        final List<Card> ret = new ArrayList<Card>();
        for (Collection<Card> kv : cardsAdded.values()) {
            ret.addAll(kv);
        }
        return ret;
    }

    public final void resetCardsAddedThisTurn() {
        cardsAddedLastTurn.clear();
        for (Entry<ZoneType, Collection<Card>> entry : cardsAddedThisTurn.entrySet()) {
            cardsAddedLastTurn.addAll(entry.getKey(), entry.getValue());
        }
        cardsAddedThisTurn.clear();
    }

    @Override
    public Iterator<Card> iterator() {
        return roCardList.iterator();
    }

    public void shuffle() {
        Collections.shuffle(cardList);
        onChanged();
    }

    @Override
    public String toString() {
        return zoneType.toString();
    }
}
