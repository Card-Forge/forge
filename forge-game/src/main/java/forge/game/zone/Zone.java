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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.GameType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardUtil;
import forge.game.event.EventValueChangeType;
import forge.game.event.GameEventZone;
import forge.game.player.Player;
import forge.util.CollectionSuppliers;
import forge.util.MyRandom;
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
public class Zone implements java.io.Serializable, Iterable<Card> {
    private static final long serialVersionUID = -5687652485777639176L;

    private final CardCollection cardList = new CardCollection();
    protected final ZoneType zoneType;
    protected final Game game;

    protected final transient MapOfLists<ZoneType, Card> cardsAddedThisTurn = new EnumMapOfLists<>(ZoneType.class, CollectionSuppliers.arrayLists());
    protected final transient MapOfLists<ZoneType, Card> cardsAddedLastTurn = new EnumMapOfLists<>(ZoneType.class, CollectionSuppliers.arrayLists());

    public Zone(final ZoneType zone0, Game game0) {
        zoneType = zone0;
        game = game0;
    }

    protected void onChanged() {
    }

    public Player getPlayer() { // generic zones like stack have no player associated
        return null;
    }

    public final void reorder(final Card c, final int index) {
        cardList.remove(c);
        cardList.add(index, c);
    }

    public final void add(final Card c) {
        add(c, null);
    }

    public final void add(final Card c, final Integer index) {
        add(c, index, null);
    }

    public void add(final Card c, Integer index, final Card latestState) {
        if (index != null && cardList.isEmpty() && index.intValue() > 0) {
            // something went wrong, most likely the method fired when the game was in an unexpected state
            // (e.g. conceding during the mana payment prompt)
            System.out.println("Warning: tried to add a card to zone with a specific non-zero index, but the zone was empty! Canceling Zone#add to avoid a crash.");
            return;
        }

        //ensure commander returns to being first card in command zone
        if (index == null && zoneType == ZoneType.Command && c.isCommander()) {
            index = 0;
            if (game.getRules().hasAppliedVariant(GameType.Oathbreaker) && c.getRules().canBeSignatureSpell()
                    && !cardList.isEmpty() && cardList.get(0).isCommander()) {
                index = 1; //signature spell should return to being second card in command zone if oathbreaker is there too
            }
        }

        // Immutable cards are usually emblems and effects
        if (!c.isImmutable()) {
            final Zone oldZone = game.getZoneOf(c);
            final ZoneType zt = oldZone == null ? ZoneType.Stack : oldZone.getZoneType();

            // only if the zoneType differss from this
            if (zt != zoneType) {
                cardsAddedThisTurn.add(zt, latestState != null ? latestState : c);
            }
        }

        c.setTurnInZone(game.getPhaseHandler().getTurn());
        if (zoneType != ZoneType.Battlefield) {
            c.setTapped(false);
        }

        // Do not add Tokens to other zones than the battlefield.
        // But Effects/Emblems count as Tokens too, so allow Command too.
        if (zoneType == ZoneType.Battlefield || !c.isToken()) {
            c.setZone(this);

            if (index == null) {
                cardList.add(c);
            } else {
                cardList.add(index.intValue(), c);
            }
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

    public final void removeAllCards(boolean forcedWithoutEvents) {
        if (forcedWithoutEvents) {
            cardList.clear();
        } else {
            for (Card c : cardList) {
                if (cardList.remove(c)) {
                    onChanged();
                    game.fireEvent(new GameEventZone(zoneType, getPlayer(), EventValueChangeType.Removed, c));
                }
            }
        }
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

    public final CardCollectionView getCards() {
        return getCards(true);
    }

    public CardCollectionView getCards(final boolean filter) {
        return cardList; // Non-Battlefield PlayerZones don't care about the filter
    }

    public final boolean isEmpty() {
        return cardList.isEmpty();
    }

    public final List<Card> getCardsAddedThisTurn(final ZoneType origin) {
        //System.out.print("Request cards put into " + getZoneType() + " from " + origin + ".Amount: ");
        return getCardsAdded(cardsAddedThisTurn, origin);
    }

    public final List<Card> getCardsAddedLastTurn(final ZoneType origin) {
        //System.out.print("Last turn - Request cards put into " + getZoneType() + " from " + origin + ".Amount: ");
        return getCardsAdded(cardsAddedLastTurn, origin);
    }

    public final boolean isCardAddedThisTurn(final Card card, final ZoneType origin) {
        if (!cardsAddedThisTurn.containsKey(origin)) {
            return false;
        }
        return cardsAddedThisTurn.get(origin).contains(card);
    }

    private static List<Card> getCardsAdded(final MapOfLists<ZoneType, Card> cardsAdded, final ZoneType origin) {
        if (origin != null) {
            final Collection<Card> cards = cardsAdded.get(origin);
            return cards == null ? ImmutableList.of() : Lists.newArrayList(cards);
        }

        if (cardsAdded.isEmpty()) {
            return ImmutableList.of();
        }

        // all cards if key == null
        final List<Card> ret = Lists.newArrayList();
        for (final Collection<Card> kv : cardsAdded.values()) {
            ret.addAll(kv);
        }
        return ret;
    }

    public final void resetCardsAddedThisTurn() {
        cardsAddedLastTurn.clear();
        for (final Entry<ZoneType, Collection<Card>> entry : cardsAddedThisTurn.entrySet()) {
            cardsAddedLastTurn.addAll(entry.getKey(), entry.getValue());
        }
        cardsAddedThisTurn.clear();
    }

    @Override
    public Iterator<Card> iterator() {
        return cardList.iterator();
    }

    public void shuffle() {
        Collections.shuffle(cardList, MyRandom.getRandom());
        onChanged();
    }

    @Override
    public String toString() {
        return zoneType.toString();
    }

    public Zone getLKICopy(Map<Integer, Card> cachedMap) {
        Zone result = new Zone(zoneType, game);

        result.setCards(CardUtil.getLKICopyList(getCards(), cachedMap));

        return result;
    }
}
