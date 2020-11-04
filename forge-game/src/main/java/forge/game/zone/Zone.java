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
import forge.game.Game;
import forge.game.GameType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardUtil;
import forge.game.event.EventValueChangeType;
import forge.game.event.GameEventZone;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerController;
import forge.util.CollectionSuppliers;
import forge.util.MyRandom;
import forge.util.maps.EnumMapOfLists;
import forge.util.maps.MapOfLists;

import java.util.*;
import java.util.Map.Entry;

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
    protected final transient MapOfLists<ZoneType, Card> latestStateCardsAddedThisTurn = new EnumMapOfLists<>(ZoneType.class, CollectionSuppliers.arrayLists());
    protected final transient MapOfLists<ZoneType, Card> latestStateCardsAddedLastTurn = new EnumMapOfLists<>(ZoneType.class, CollectionSuppliers.arrayLists());

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
                cardsAddedThisTurn.add(zt, c);
                latestStateCardsAddedThisTurn.add(zt, latestState != null ? latestState : c);
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

        if(zoneType == ZoneType.Battlefield && c.isLand()) {
            PlayerCollection playerCollection = game.getPlayers();
            int numPlayers = playerCollection.size();
            for (int i = 0; i < numPlayers; i++) {
                Player player = playerCollection.get(i);
                if(!player.isAI()) {
                    PlayerController playerControllerHuman = player.getController();
                    playerControllerHuman.handleLandPlayed(c,this);
                }
            }                    
        }
        
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

    public final MapOfLists<ZoneType, Card> getCardsAddedThisTurn() {
        return cardsAddedThisTurn;
    }

    public final MapOfLists<ZoneType, Card> getCardsAddedThisTurn(boolean latestState) {
        if (latestState) {
            return latestStateCardsAddedThisTurn;
        } else {
            return cardsAddedThisTurn;
        }
    }
    
    public final MapOfLists<ZoneType, Card> getCardsAddedLastTurn() {
        return cardsAddedLastTurn;
    }

    public final MapOfLists<ZoneType, Card> getCardsAddedLastTurn(boolean latestState) {
        if (latestState) {
            return latestStateCardsAddedLastTurn;
        } else {
            return cardsAddedLastTurn;
        }
    }

    public final CardCollectionView getCardsAddedThisTurn(final ZoneType origin) {
        return getCardsAddedThisTurn(origin, true);
    }

    public final CardCollectionView getCardsAddedThisTurn(final ZoneType origin, boolean latestState) {
        //System.out.print("Request cards put into " + getZoneType() + " from " + origin + ".Amount: ");
        return getCardsAdded(latestState ? latestStateCardsAddedThisTurn : cardsAddedThisTurn, origin);
    }

    public final CardCollectionView getCardsAddedLastTurn(final ZoneType origin) {
        return getCardsAddedLastTurn(origin, true);
    }

    public final CardCollectionView getCardsAddedLastTurn(final ZoneType origin, boolean latestState) {
        //System.out.print("Last turn - Request cards put into " + getZoneType() + " from " + origin + ".Amount: ");
        return getCardsAdded(latestState ? latestStateCardsAddedLastTurn : cardsAddedLastTurn, origin);
    }

    private static CardCollectionView getCardsAdded(final MapOfLists<ZoneType, Card> cardsAdded, final ZoneType origin) {
        if (origin != null) {
            final Collection<Card> cards = cardsAdded.get(origin);
            return cards == null ? CardCollection.EMPTY : new CardCollection(cards);
        }

        // all cards if key == null
        final CardCollection ret = new CardCollection();
        for (final Collection<Card> kv : cardsAdded.values()) {
            ret.addAll(kv);
        }
        return ret;
    }

    public final void resetCardsAddedThisTurn() {
        cardsAddedLastTurn.clear();
        latestStateCardsAddedLastTurn.clear();
        for (final Entry<ZoneType, Collection<Card>> entry : cardsAddedThisTurn.entrySet()) {
            cardsAddedLastTurn.addAll(entry.getKey(), entry.getValue());
        }
        for (final Entry<ZoneType, Collection<Card>> entry : latestStateCardsAddedThisTurn.entrySet()) {
            latestStateCardsAddedLastTurn.addAll(entry.getKey(), entry.getValue());
        }
        cardsAddedThisTurn.clear();
        latestStateCardsAddedThisTurn.clear();
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
    
    public Zone getLKICopy() {
        Zone result = new Zone(zoneType, game);

        final CardCollection list = new CardCollection();
        for (final Card c : getCards()) {
            list.add(CardUtil.getLKICopy(c));
        }
        result.setCards(list);

        return result;
    }
}
