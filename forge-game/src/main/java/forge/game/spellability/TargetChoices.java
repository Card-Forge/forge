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

import com.google.common.collect.Iterables;

import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;

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
    private int numTargeted = 0;

    // Card or Player are legal targets.
    private final CardCollection targetCards = new CardCollection();
    private final List<Player> targetPlayers = new ArrayList<>();
    private final List<SpellAbility> targetSpells = new ArrayList<>();

    public final int getNumTargeted() {
        return numTargeted;
    }

    public final int getTotalTargetedCMC() {
        int totalCMC = 0;
        for (Card c : targetCards) {
            totalCMC += c.getCMC();
        }
        return totalCMC;
    }

    public final boolean add(final GameObject o) {
        if (o instanceof Player) {
            return addTarget((Player) o);
        } else if (o instanceof Card) {
            return addTarget((Card) o);
        } else if (o instanceof SpellAbility) {
            return addTarget((SpellAbility) o);
        }

        return false;
    }

    private final boolean addTarget(final Card c) {
        if (!targetCards.contains(c)) {
            targetCards.add(c);
            numTargeted++;
            return true;
        }
        return false;
    }

    private final boolean addTarget(final Player p) {
        if (!targetPlayers.contains(p)) {
            targetPlayers.add(p);
            numTargeted++;
            return true;
        }
        return false;
    }

    private final boolean addTarget(final SpellAbility sa) {
        if (!targetSpells.contains(sa)) {
            targetSpells.add(sa);
            numTargeted++;
            return true;
        }
        return false;
    }

    public final boolean remove(final GameObject target) {
        // remove returns true if element was found in given list
        if (targetCards.remove(target) || targetPlayers.remove(target) || targetSpells.remove(target)) {
            numTargeted--;
            return true;
        }
        return false;
    }

    public final CardCollectionView getTargetCards() {
        return targetCards;
    }

    public final Iterable<Player> getTargetPlayers() {
        return targetPlayers;
    }

    public final Iterable<SpellAbility> getTargetSpells() {
        return targetSpells;
    }

    public final List<GameEntity> getTargetEntities() {
        final List<GameEntity> tgts = new ArrayList<>();
        tgts.addAll(targetPlayers);
        tgts.addAll(targetCards);

        return tgts;
    }

    public final List<GameObject> getTargets() {
        final List<GameObject> tgts = new ArrayList<>();
        tgts.addAll(targetPlayers);
        tgts.addAll(targetCards);
        tgts.addAll(targetSpells);

        return tgts;
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
        return !targetCards.isEmpty();
    }

    public final boolean isTargetingAnyPlayer() {
        return !targetPlayers.isEmpty();
    }


    public final boolean isTargetingAnySpell() {
        return !targetSpells.isEmpty();
    }

    public final boolean isTargeting(GameObject e) {
        return targetCards.contains(e) || targetSpells.contains(e) || targetPlayers.contains(e); 
    }

    public final Card getFirstTargetedCard() {
        return Iterables.getFirst(targetCards, null);
    }

    public final Player getFirstTargetedPlayer() {
        return Iterables.getFirst(targetPlayers, null);
    }

    public final SpellAbility getFirstTargetedSpell() {
        return Iterables.getFirst(targetSpells, null);
    }

    public final boolean isEmpty() {
        return targetCards.isEmpty() && targetSpells.isEmpty() && targetPlayers.isEmpty();
    }
    
    @Override
    public TargetChoices clone() {
        TargetChoices tc = new TargetChoices();
        tc.targetCards.addAll(targetCards);
        tc.targetPlayers.addAll(targetPlayers);
        tc.targetSpells.addAll(targetSpells);
        tc.numTargeted = numTargeted;
        return tc;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TargetChoices) {
            TargetChoices compare = (TargetChoices)obj;

            if (this.getNumTargeted() != compare.getNumTargeted()) {
                return false;
            }
            for (int i = 0; i < this.getTargets().size(); i++) {
                if (!compare.getTargets().get(i).equals(this.getTargets().get(i))) {
                    return false;
                }
            }
            return true;

        } else {
            return false;
        }
    }
}
