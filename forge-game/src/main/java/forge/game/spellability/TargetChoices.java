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
package forge.game.spellability;

import com.google.common.base.Predicates;
import com.google.common.collect.ForwardingList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.util.collect.FCollection;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Target_Choices class.
 * </p>
 *
 * @author Forge
 * @version $Id$
 */
public class TargetChoices extends ForwardingList<GameObject> implements Cloneable {

    private final FCollection<GameObject> targets = new FCollection<GameObject>();

    private final Map<GameObject, Integer> dividedMap = Maps.newHashMap();

    public final int getTotalTargetedCMC() {
        int totalCMC = 0;
        for (Card c : Iterables.filter(targets, Card.class)) {
            totalCMC += c.getCMC();
        }
        return totalCMC;
    }

    @Override
    public final boolean add(final GameObject o) {
        if (o instanceof Player || o instanceof Card || o instanceof SpellAbility) {
            return super.add(o);
        }
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        boolean result = super.removeAll(collection);
        for (Object e : collection) {
            this.dividedMap.remove(e);
        }
        return result;
    }

    @Override
    public boolean remove(Object object) {
        boolean result = super.remove(object);
        dividedMap.remove(object);
        return result;
    }

    public final CardCollectionView getTargetCards() {
        return new CardCollection(Iterables.filter(targets, Card.class));
    }

    public final Iterable<Player> getTargetPlayers() {
        return Iterables.filter(targets, Player.class);
    }

    public final Iterable<SpellAbility> getTargetSpells() {
        return Iterables.filter(targets, SpellAbility.class);
    }

    public final List<GameEntity> getTargetEntities() {
        return Lists.newArrayList(Iterables.filter(targets, GameEntity.class));
    }

    public final boolean isTargetingAnyCard() {
        return Iterables.any(targets, Predicates.instanceOf(Card.class));
    }

    public final boolean isTargetingAnyPlayer() {
        return Iterables.any(targets, Predicates.instanceOf(Player.class));
    }

    public final boolean isTargetingAnySpell() {
        return Iterables.any(targets, Predicates.instanceOf(SpellAbility.class));
    }

    public final Card getFirstTargetedCard() {
        return Iterables.getFirst(Iterables.filter(targets, Card.class), null);
    }

    public final Player getFirstTargetedPlayer() {
        return Iterables.getFirst(getTargetPlayers(), null);
    }

    public final SpellAbility getFirstTargetedSpell() {
        return Iterables.getFirst(getTargetSpells(), null);
    }

    @Override
    public TargetChoices clone() {
        TargetChoices tc = new TargetChoices();
        tc.targets.addAll(targets);
        tc.dividedMap.putAll(dividedMap);
        return tc;
    }
    @Override
    protected List<GameObject> delegate() {
        return targets;
    }

    public final void addDividedAllocation(final GameObject tgt, final Integer portionAllocated) {
        this.dividedMap.put(tgt, portionAllocated);
    }
    public Integer getDividedValue(GameObject c) {
        return dividedMap.get(c);
    }

    public Collection<Integer> getDividedValues() {
        return dividedMap.values();
    }

    public int getTotalDividedValue() {
        int result = 0;
        for (Integer i : getDividedValues()) {
            if (i != null)
                result += i;
        }
        return result;
    }
}
