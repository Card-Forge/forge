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
package forge.card.spellability;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Iterables;

import forge.Card;
import forge.ITargetable;
import forge.game.player.Player;

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
    private final List<Card> targetCards = new ArrayList<Card>();
    private final List<Player> targetPlayers = new ArrayList<Player>();
    private final List<SpellAbility> targetSpells = new ArrayList<SpellAbility>();

    /**
     * <p>
     * Getter for the field <code>numTargeted</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getNumTargeted() {
        return this.numTargeted;
    }

    /**
     * <p>
     * addTarget.
     * </p>
     * 
     * @param o
     *            a {@link java.lang.Object} object.
     * @return a boolean.
     */
    public final boolean add(final ITargetable o) {
        if (o instanceof Player) {
            return this.addTarget((Player) o);
        } else if (o instanceof Card) {
            return this.addTarget((Card) o);
        } else if (o instanceof SpellAbility) {
            return this.addTarget((SpellAbility) o);
        }

        return false;
    }

    /**
     * <p>
     * addTarget.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    private final boolean addTarget(final Card c) {
        if (!this.targetCards.contains(c)) {
            this.targetCards.add(c);
            this.numTargeted++;
            return true;
        }
        return false;
    }

    /**
     * <p>
     * addTarget.
     * </p>
     * 
     * @param p
     *            a {@link forge.game.player.Player} object.
     * @return a boolean.
     */
    private final boolean addTarget(final Player p) {
        if (!this.targetPlayers.contains(p)) {
            this.targetPlayers.add(p);
            this.numTargeted++;
            return true;
        }
        return false;
    }

    /**
     * <p>
     * addTarget.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private final boolean addTarget(final SpellAbility sa) {
        if (!this.targetSpells.contains(sa)) {
            this.targetSpells.add(sa);
            this.numTargeted++;
            return true;
        }
        return false;
    }

    /**
     * <p>
     * removeTarget.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean remove(final ITargetable target) {
        // remove returns true if element was found in given list
        if (this.targetCards.remove(target) || this.targetPlayers.remove(target) || this.targetSpells.remove(target)) {
            this.numTargeted--;
            return true;
        }
        return false;
    }

    /**
     * <p>
     * Getter for the field <code>targetCards</code>.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final Iterable<Card> getTargetCards() {
        return this.targetCards;
    }

    /**
     * <p>
     * Getter for the field <code>targetPlayers</code>.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final Iterable<Player> getTargetPlayers() {
        return this.targetPlayers;
    }

    /**
     * <p>
     * Getter for the field <code>targetSAs</code>.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final Iterable<SpellAbility> getTargetSpells() {
        return this.targetSpells;
    }

    /**
     * <p>
     * getTargets.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final List<ITargetable> getTargets() {
        final ArrayList<ITargetable> tgts = new ArrayList<ITargetable>();
        tgts.addAll(this.targetPlayers);
        tgts.addAll(this.targetCards);
        tgts.addAll(this.targetSpells);

        return tgts;
    }


    public final String getTargetedString() {
        final List<ITargetable> tgts = this.getTargets();
        final StringBuilder sb = new StringBuilder();
        for (final Object o : tgts) {
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
            sb.append(" ");
        }

        return sb.toString();
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

    public final boolean isTargeting(ITargetable e) {
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

    /* (non-Javadoc)
     * @see forge.card.spellability.ITargetsChosen#isEmpty()
     */
    public final boolean isEmpty() {
        return targetCards.isEmpty() && targetSpells.isEmpty() && targetPlayers.isEmpty();
    }
    
    @Override
    public TargetChoices clone() {
        TargetChoices tc = new TargetChoices();
        tc.targetCards.addAll(this.targetCards);
        tc.targetPlayers.addAll(this.targetPlayers);
        tc.targetSpells.addAll(this.targetSpells);
        return tc;
    }
}
