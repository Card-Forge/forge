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

import java.util.*;
import java.util.function.Predicate;

import org.testng.collections.Maps;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.MultimapBuilder;

import forge.game.Game;
import forge.game.GameType;
import forge.game.card.*;
import forge.game.event.EventValueChangeType;
import forge.game.event.GameEventZone;
import forge.game.player.Player;
import forge.util.MyRandom;

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

    protected final transient ListMultimap<ZoneType, Card> cardsAddedThisTurn = MultimapBuilder.enumKeys(ZoneType.class).arrayListValues().build();
    protected final transient ListMultimap<ZoneType, Card> cardsAddedLastTurn = MultimapBuilder.enumKeys(ZoneType.class).arrayListValues().build();
    protected final transient Map<Card, ZoneType> enteredFromThisTurn = Maps.newHashMap();

    // might support different order via preference later
    private static final Comparator<Card> COMPARATOR = Comparator.comparingInt((Card c) -> c.getCMC())
            .thenComparing(c -> c.getColor().getOrderWeight())
            .thenComparing(Comparator.comparing(Card::getName))
            .thenComparing(Card::hasPerpetual);

    protected void sort() {
        cardList.sort(COMPARATOR);
    }

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
        add(c, index, latestState, false);
    }
    public void add(final Card c, Integer index, final Card latestState, final boolean rollback) {
        if (index != null && cardList.isEmpty() && index > 0) {
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

        if (!rollback) {
            // Immutable cards are usually emblems and effects
            if (!c.isImmutable()) {
                final Zone oldZone = game.getZoneOf(c);
                final ZoneType zt = oldZone == null ? ZoneType.Stack : oldZone.getZoneType();

                // don't go in there if its a control change
                if (zt != zoneType) {
                    c.setTurnInController(getPlayer());
                    c.setTurnInZone(game.getPhaseHandler().getTurn());
                    if (latestState != null) {
                        cardsAddedThisTurn.put(zt, latestState);
                        enteredFromThisTurn.put(latestState, zt);
                    }
                }
            }

            if (zoneType != ZoneType.Battlefield) {
                c.setTapped(false);
            }

            if (zoneType == ZoneType.Graveyard && c.isPermanent() && !c.isToken()) {
                c.getOwner().descend();
            }
        }

        c.setZone(this);

        if ((zoneType == ZoneType.Battlefield || !c.isToken()) || (zoneType == ZoneType.Stack && c.getCopiedPermanent() != null)) {
            if (index == null) {
                cardList.add(c);
            } else {
                cardList.add(index, c);
            }
        }
        onChanged();

        game.fireEvent(new GameEventZone(zoneType, getPlayer(), EventValueChangeType.Added, c));
   }

    public final boolean contains(final Card c) {
        return cardList.contains(c);
    }

    public final boolean contains(final Predicate<Card> condition) {
        return cardList.anyMatch(condition);
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
                remove(c);
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
        return getCardsAdded(cardsAddedThisTurn, origin);
    }

    public final List<Card> getCardsAddedLastTurn(final ZoneType origin) {
        return getCardsAdded(cardsAddedLastTurn, origin);
    }

    public final boolean isCardAddedThisTurn(final Card card, final ZoneType origin) {
        if (cardsAddedThisTurn.containsEntry(origin, card)) {
            return origin.equals(enteredFromThisTurn.get(card));
        }
        return false;
    }

    private static List<Card> getCardsAdded(final ListMultimap<ZoneType, Card> cardsAdded, final ZoneType origin) {
        if (origin != null) {
            return Lists.newArrayList(cardsAdded.get(origin));
        }

        if (cardsAdded.isEmpty()) {
            return ImmutableList.of();
        }

        // all cards if key == null
        return Lists.newArrayList(cardsAdded.values());
    }

    public final void resetCardsAddedThisTurn() {
        cardsAddedLastTurn.clear();
        cardsAddedLastTurn.putAll(cardsAddedThisTurn);
        cardsAddedThisTurn.clear();
        enteredFromThisTurn.clear();
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

        result.setCards(CardCopyService.getLKICopyList(getCards(), cachedMap));

        return result;
    }

    public void saveLKI(Card c, Card old) {
        final Zone oldZone = game.getZoneOf(old);
        final ZoneType zt = oldZone == null ? ZoneType.Stack : oldZone.getZoneType();
        if (zt == zoneType) {
            return;
        }
        cardsAddedThisTurn.put(zt, CardCopyService.getLKICopy(c));
    }
}
