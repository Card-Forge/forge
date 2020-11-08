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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.util.collect.FCollection;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Target_Choices class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class TargetChoices implements Cloneable {

    private final FCollection<GameObject> targets = new FCollection<GameObject>();

    public final int getNumTargeted() {
        return targets.size();
    }

    public final int getTotalTargetedCMC() {
        int totalCMC = 0;
        for (Card c : Iterables.filter(targets, Card.class)) {
            totalCMC += c.getCMC();
        }
        return totalCMC;
    }

    public final boolean add(final GameObject o) {
        if (o instanceof Player || o instanceof Card || o instanceof SpellAbility) {
            return targets.add(o);
        }
        return false;
    }

    public final boolean remove(final GameObject target) {
        // remove returns true if element was found in given list
        return targets.remove(target);
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

    public final List<GameObject> getTargets() {
        return this.targets;
    }


    public final String getTargetedString() {
        final List<GameObject> tgts = getTargets();
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (final Object o : tgts) {
            if (!first) {
                sb.append(" ");
            }
            first = false;
            if (o instanceof Player) {
                final Player p = (Player) o;
                sb.append(p.getName());
            }
            if (o instanceof Card) {
                final Card c = (Card) o;
                sb.append(c);
            }
            if (o instanceof SpellAbility) {
                final SpellAbility sa = (SpellAbility) o;
                sb.append(sa);
            }
        }
        return sb.toString();
    }

    @Override
    public final String toString() {
        return this.getTargetedString();
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

    public final boolean isTargeting(GameObject e) {
        return targets.contains(e);
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

    public final boolean isEmpty() {
        return targets.isEmpty();
    }
    
    @Override
    public TargetChoices clone() {
        TargetChoices tc = new TargetChoices();
        tc.targets.addAll(targets);
        return tc;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TargetChoices) {
            TargetChoices compare = (TargetChoices)obj;

            if (getNumTargeted() != compare.getNumTargeted()) {
                return false;
            }
            return getTargets().equals(compare.getTargets());
        } else {
            return false;
        }
    }
}
