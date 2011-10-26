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
    private HashMap<Card, CardList> map = new HashMap<Card, CardList>();
    private CardList attackers = new CardList();

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
        CardList a = attackingCreatures;
        for (int i = 0; i < a.size(); i++) {
            addAttacker(a.get(i));
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
        return attackers;
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
        attackers.add(c);
        map.put(c, new CardList());
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
        return map.get(attacker);
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
        CardList list = map.get(attacker);
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
        CardList list = new CardList();
        Iterator<Card> it = map.keySet().iterator();
        while (it.hasNext()) {
            Card attack = it.next();
            CardList block = map.get(attack);
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
        CardList aDestroy = new CardList();
        CardList bDestroy = new CardList();

        CardList allAttackers = this.getAttackers();
        for (int i = 0; i < allAttackers.size(); i++) {
            Card attack = allAttackers.get(i);
            // for now, CardList blockers should only hold 1 Card
            CardList blockers = map.get(attack);
            if (blockers.size() == 0) {
            } else {

                Card block = blockers.get(0);
                int blockerDamage = block.getNetCombatDamage();
                int attackerDamage = attack.getNetCombatDamage();

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
        StringBuilder sb = new StringBuilder();
        CardList attack = this.getAttackers();
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
