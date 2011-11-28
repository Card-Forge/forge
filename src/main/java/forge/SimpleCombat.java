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
package forge;

import java.util.HashMap;
import java.util.Iterator;

/**
 * <p>
 * SimpleCombat class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
class SimpleCombat {
    private final HashMap<Card, CardList> map = new HashMap<Card, CardList>();
    private final CardList attackers = new CardList();

    /**
     * <p>
     * Constructor for SimpleCombat.
     * </p>
     */
    public SimpleCombat() {
    }

    /**
     * <p>
     * Constructor for SimpleCombat.
     * </p>
     * 
     * @param attackingCreatures
     *            a {@link forge.CardList} object.
     */
    public SimpleCombat(final CardList attackingCreatures) {
        final CardList a = attackingCreatures;
        for (int i = 0; i < a.size(); i++) {
            this.addAttacker(a.get(i));
        }
    }

    /**
     * <p>
     * Getter for the field <code>attackers</code>.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    public CardList getAttackers() {
        return this.attackers;
    }

    /**
     * <p>
     * addAttacker.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public void addAttacker(final Card c) {
        this.attackers.add(c);
        this.map.put(c, new CardList());
    }

    /**
     * <p>
     * getBlockers.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @return a {@link forge.CardList} object.
     */
    public CardList getBlockers(final Card attacker) {
        return this.map.get(attacker);
    }

    /**
     * <p>
     * addBlocker.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @param blocker
     *            a {@link forge.Card} object.
     */
    public void addBlocker(final Card attacker, final Card blocker) {
        final CardList list = this.map.get(attacker);
        if (list == null) {
            throw new RuntimeException("SimpleCombat : addBlocker() attacker not found - " + attacker);
        }

        list.add(blocker);
    }

    /**
     * <p>
     * getUnblockedAttackers.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    public CardList getUnblockedAttackers() {
        final CardList list = new CardList();
        final Iterator<Card> it = this.map.keySet().iterator();
        while (it.hasNext()) {
            final Card attack = it.next();
            final CardList block = this.map.get(attack);
            if (block.size() == 0) {
                list.add(attack);
            }
        }

        return list;
    }

    // creatures destroy each other in combat damage
    /**
     * <p>
     * combatDamage.
     * </p>
     * 
     * @return an array of {@link forge.CardList} objects.
     */
    public CardList[] combatDamage() {
        // aDestroy holds the number of creatures of A's that were destroyed
        final CardList aDestroy = new CardList();
        final CardList bDestroy = new CardList();

        final CardList allAttackers = this.getAttackers();
        for (int i = 0; i < allAttackers.size(); i++) {
            final Card attack = allAttackers.get(i);
            // for now, CardList blockers should only hold 1 Card
            final CardList blockers = this.map.get(attack);
            if (blockers.size() == 0) {
            } else {

                final Card block = blockers.get(0);
                final int blockerDamage = block.getNetCombatDamage();
                final int attackerDamage = attack.getNetCombatDamage();

                if (attack.getNetDefense() <= blockerDamage) {
                    aDestroy.add(attack);
                }

                if (block.getNetDefense() <= attackerDamage) {
                    bDestroy.add(block);
                }
            }
        } // while
        return new CardList[] { aDestroy, bDestroy };
    } // combatDamage()

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        final CardList attack = this.getAttackers();
        CardList block;
        for (int i = 0; i < attack.size(); i++) {
            block = this.getBlockers(attack.get(i));
            if (block.isEmpty()) {
                sb.append(attack.get(i));
                sb.append(" ");
            } else {
                sb.append(attack.get(i));
                sb.append(" - ");
                sb.append(block.get(0));
                sb.append(" ");
            }
        }

        return sb.toString();
    }
} // end class SimpleCombat
