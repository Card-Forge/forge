package forge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import forge.Constant.Zone;

/**
 * <p>
 * Combat class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Combat {
    // key is attacker Card
    // value is CardList of blockers
    private Map<Card, CardList> map = new TreeMap<Card, CardList>();
    private Set<Card> blocked = new HashSet<Card>();

    private HashMap<Card, CardList> unblockedMap = new HashMap<Card, CardList>();
    private HashMap<Card, Integer> defendingDamageMap = new HashMap<Card, Integer>();

    // Defenders are the Defending Player + Each Planeswalker that player
    // controls
    private ArrayList<Object> defenders = new ArrayList<Object>();
    private int currentDefender = 0;
    private int nextDefender = 0;

    // This Hash keeps track of
    private HashMap<Card, Object> attackerToDefender = new HashMap<Card, Object>();

    private int attackingDamage;

    private Player attackingPlayer = null;
    private Player defendingPlayer = null;

    private CardList attackersWithLure = new CardList();
    private CardList canBlockAttackerWithLure = new CardList();

    /**
     * <p>
     * Constructor for Combat.
     * </p>
     */
    public Combat() {
        // Let the Begin Turn/Untap Phase Reset Combat properly
    }

    /**
     * <p>
     * reset.
     * </p>
     */
    public final void reset() {
        resetAttackers();
        blocked.clear();

        unblockedMap.clear();

        attackingDamage = 0;
        defendingDamageMap.clear();

        attackingPlayer = null;
        defendingPlayer = null;

        attackersWithLure.clear();
        canBlockAttackerWithLure.clear();

        defenders.clear();
        currentDefender = 0;
        nextDefender = 0;

        initiatePossibleDefenders(AllZone.getPhase().getPlayerTurn().getOpponent());
    }

    /**
     * <p>
     * initiatePossibleDefenders.
     * </p>
     * 
     * @param defender
     *            a {@link forge.Player} object.
     */
    public final void initiatePossibleDefenders(final Player defender) {
        defenders.add(defender);
        CardList planeswalkers = defender.getCardsIn(Zone.Battlefield);
        planeswalkers = planeswalkers.getType("Planeswalker");
        for (Card pw : planeswalkers) {
            defenders.add(pw);
        }
    }

    /**
     * <p>
     * nextDefender.
     * </p>
     * 
     * @return a {@link java.lang.Object} object.
     */
    public final Object nextDefender() {
        if (nextDefender >= defenders.size()) {
            return null;
        }

        currentDefender = nextDefender;
        nextDefender++;

        return defenders.get(currentDefender);
    }

    /**
     * <p>
     * Setter for the field <code>currentDefender</code>.
     * </p>
     * 
     * @param def
     *            a int.
     */
    public final void setCurrentDefender(final int def) {
        currentDefender = def;
    }

    /**
     * <p>
     * getRemainingDefenders.
     * </p>
     * 
     * @return a int.
     */
    public final int getRemainingDefenders() {
        return defenders.size() - nextDefender;
    }

    /**
     * <p>
     * Getter for the field <code>defenders</code>.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<Object> getDefenders() {
        return defenders;
    }

    /**
     * <p>
     * Setter for the field <code>defenders</code>.
     * </p>
     * 
     * @param newDef
     *            a {@link java.util.ArrayList} object.
     */
    public final void setDefenders(final ArrayList<Object> newDef) {
        defenders = newDef;
    }

    /**
     * <p>
     * getDefendingPlaneswalkers.
     * </p>
     * 
     * @return an array of {@link forge.Card} objects.
     */
    public final Card[] getDefendingPlaneswalkers() {
        Card[] pwDefending = new Card[defenders.size() - 1];

        int i = 0;

        for (Object o : defenders) {
            if (o instanceof Card) {
                pwDefending[i] = (Card) o;
                i++;
            }
        }

        return pwDefending;
    }

    /**
     * <p>
     * getDeclaredAttackers.
     * </p>
     * 
     * @return a int.
     */
    public final int getDeclaredAttackers() {
        return attackerToDefender.size();
    }

    /**
     * <p>
     * Setter for the field <code>attackingPlayer</code>.
     * </p>
     * 
     * @param player
     *            a {@link forge.Player} object.
     */
    public final void setAttackingPlayer(final Player player) {
        attackingPlayer = player;
    }

    /**
     * <p>
     * Setter for the field <code>defendingPlayer</code>.
     * </p>
     * 
     * @param player
     *            a {@link forge.Player} object.
     */
    public final void setDefendingPlayer(final Player player) {
        defendingPlayer = player;
    }

    /**
     * <p>
     * Getter for the field <code>attackingPlayer</code>.
     * </p>
     * 
     * @return a {@link forge.Player} object.
     */
    public final Player getAttackingPlayer() {
        return attackingPlayer;
    }

    /**
     * <p>
     * Getter for the field <code>defendingPlayer</code>.
     * </p>
     * 
     * @return a {@link forge.Player} object.
     */
    public final Player getDefendingPlayer() {
        return defendingPlayer;
    }

    /**
     * <p>
     * Getter for the field <code>defendingDamageMap</code>.
     * </p>
     * 
     * @return a {@link java.util.HashMap} object.
     */
    public final HashMap<Card, Integer> getDefendingDamageMap() {
        return defendingDamageMap;
    }

    /**
     * <p>
     * getTotalDefendingDamage.
     * </p>
     * 
     * @return a int.
     */
    public final int getTotalDefendingDamage() {
        int total = 0;

        Collection<Integer> c = defendingDamageMap.values();

        Iterator<Integer> itr = c.iterator();
        while (itr.hasNext()) {
            total += itr.next();
        }

        return total;
    }

    /**
     * <p>
     * setDefendingDamage.
     * </p>
     */
    public final void setDefendingDamage() {
        defendingDamageMap.clear();
        CardList att = new CardList(getAttackers());
        // sum unblocked attackers' power
        for (int i = 0; i < att.size(); i++) {
            if (!isBlocked(att.get(i)) || (getBlockers(att.get(i)).size() == 0 && att.get(i).hasKeyword("Trample"))) {

                int damageDealt = att.get(i).getNetCombatDamage();

                if (damageDealt > 0) {
                    // if the creature has first strike do not do damage in the
                    // normal combat phase
                    if (!att.get(i).hasFirstStrike() || att.get(i).hasDoubleStrike()) {
                        addDefendingDamage(damageDealt, att.get(i));
                    }
                }
            } // ! isBlocked...
        } // for
    }

    /**
     * <p>
     * setDefendingFirstStrikeDamage.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean setDefendingFirstStrikeDamage() {
        boolean needsFirstStrike = false;
        defendingDamageMap.clear();
        CardList att = new CardList(getAttackers());
        // sum unblocked attackers' power
        for (int i = 0; i < att.size(); i++) {
            if (!isBlocked(att.get(i))) {

                int damageDealt = att.get(i).getNetCombatDamage();

                if (damageDealt > 0) {
                    // if the creature has first strike or double strike do
                    // damage in the first strike combat phase
                    if (att.get(i).hasFirstStrike() || att.get(i).hasDoubleStrike()) {
                        addDefendingDamage(damageDealt, att.get(i));
                        needsFirstStrike = true;
                    }
                }
            }
        } // for

        return needsFirstStrike;
    }

    /**
     * <p>
     * addDefendingDamage.
     * </p>
     * 
     * @param n
     *            a int.
     * @param source
     *            a {@link forge.Card} object.
     */
    public final void addDefendingDamage(final int n, final Card source) {
        String slot = getDefenderByAttacker(source).toString();
        Object o = defenders.get(Integer.parseInt(slot));

        if (o instanceof Card) {
            Card pw = (Card) o;
            pw.addAssignedDamage(n, source);

            return;
        }

        if (!defendingDamageMap.containsKey(source)) {
            defendingDamageMap.put(source, n);
        } else {
            defendingDamageMap.put(source, defendingDamageMap.get(source) + n);
        }
    }

    /**
     * <p>
     * addAttackingDamage.
     * </p>
     * 
     * @param n
     *            a int.
     */
    public final void addAttackingDamage(final int n) {
        attackingDamage += n;
    }

    /**
     * <p>
     * Getter for the field <code>attackingDamage</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getAttackingDamage() {
        return attackingDamage;
    }

    /**
     * <p>
     * sortAttackerByDefender.
     * </p>
     * 
     * @return an array of {@link forge.CardList} objects.
     */
    public final CardList[] sortAttackerByDefender() {
        CardList[] attackers = new CardList[defenders.size()];
        for (int i = 0; i < attackers.length; i++) {
            attackers[i] = new CardList();
        }

        for (Card atk : attackerToDefender.keySet()) {
            Object o = attackerToDefender.get(atk);
            int i = Integer.parseInt(o.toString());
            attackers[i].add(atk);
        }

        return attackers;
    }

    /**
     * <p>
     * isAttacking.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean isAttacking(final Card c) {
        return map.get(c) != null;
    }

    /**
     * <p>
     * addAttacker.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void addAttacker(final Card c) {
        map.put(c, new CardList());
        attackerToDefender.put(c, currentDefender);
    }

    /**
     * <p>
     * getDefenderByAttacker.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link java.lang.Object} object.
     */
    public final Object getDefenderByAttacker(final Card c) {
        return attackerToDefender.get(c);
    }

    /**
     * <p>
     * resetAttackers.
     * </p>
     */
    public final void resetAttackers() {
        map.clear();
        attackerToDefender.clear();
    }

    /**
     * <p>
     * getAttackers.
     * </p>
     * 
     * @return an array of {@link forge.Card} objects.
     */
    public final Card[] getAttackers() {
        CardList out = new CardList();
        Iterator<Card> it = map.keySet().iterator();

        while (it.hasNext()) {
            out.add((Card) it.next());
        }

        return out.toArray();
    } // getAttackers()

    /**
     * <p>
     * isBlocked.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean isBlocked(final Card attacker) {
        return blocked.contains(attacker);
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
    public final void addBlocker(final Card attacker, final Card blocker) {
        blocked.add(attacker);
        getList(attacker).add(blocker);
    }

    /**
     * <p>
     * resetBlockers.
     * </p>
     */
    public final void resetBlockers() {
        reset();

        CardList att = new CardList(getAttackers());
        for (int i = 0; i < att.size(); i++) {
            addAttacker(att.get(i));
        }
    }

    /**
     * <p>
     * getAllBlockers.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    public final CardList getAllBlockers() {
        CardList att = new CardList(getAttackers());
        CardList block = new CardList();

        for (int i = 0; i < att.size(); i++) {
            block.addAll(getBlockers(att.get(i)));
        }

        return block;
    } // getAllBlockers()

    /**
     * <p>
     * getBlockers.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @return a {@link forge.CardList} object.
     */
    public final CardList getBlockers(final Card attacker) {
        if (getList(attacker) == null) {
            return new CardList();
        } else {
            return new CardList(getList(attacker));
        }
    }

    /**
     * <p>
     * getAttackerBlockedBy.
     * </p>
     * 
     * @param blocker
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    public final Card getAttackerBlockedBy(final Card blocker) {
        CardList att = new CardList(getAttackers());

        for (int i = 0; i < att.size(); i++) {
            if (getBlockers(att.get(i)).contains(blocker)) {
                return att.get(i);
            }
        } // for

        return null;
    }

    /**
     * <p>
     * getList.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @return a {@link forge.CardList} object.
     */
    private CardList getList(final Card attacker) {
        return (CardList) map.get(attacker);
    }

    /**
     * <p>
     * removeFromCombat.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void removeFromCombat(final Card c) {
        // is card an attacker?
        CardList att = new CardList(getAttackers());
        if (att.contains(c)) {
            map.remove(c);
            attackerToDefender.remove(c);
        } else { // card is a blocker
            for (Card a : att) {
                if (getBlockers(a).contains(c)) {
                    getList(a).remove(c);
                    // TODO if Declare Blockers and Declare Blockers (Abilities)
                    // merge this logic needs to be tweaked
                    if (getBlockers(a).size() == 0 && AllZone.getPhase().is(Constant.Phase.COMBAT_DECLARE_BLOCKERS)) {
                        blocked.remove(a);
                    }
                }
            }
        }
        // update combat
        CombatUtil.showCombat();
    } // removeFromCombat()

    /**
     * <p>
     * verifyCreaturesInPlay.
     * </p>
     */
    public final void verifyCreaturesInPlay() {
        CardList all = new CardList();
        all.addAll(getAttackers());
        all.addAll(getAllBlockers());

        for (int i = 0; i < all.size(); i++) {
            if (!AllZoneUtil.isCardInPlay(all.get(i))) {
                removeFromCombat(all.get(i));
            }
        }
    } // verifyCreaturesInPlay()

    /**
     * <p>
     * setUnblocked.
     * </p>
     */
    public final void setUnblocked() {
        CardList attacking = new CardList(getAttackers());

        for (Card attacker : attacking) {
            CardList block = getBlockers(attacker);

            if (block.size() == 0) {
                // this damage is assigned to a player by setPlayerDamage()
                addUnblockedAttacker(attacker);

                // Run Unblocked Trigger
                HashMap<String, Object> runParams = new HashMap<String, Object>();
                runParams.put("Attacker", attacker);
                AllZone.getTriggerHandler().runTrigger("AttackerUnblocked", runParams);

            }
        }
    }

    // set Card.setAssignedDamage() for all creatures in combat
    // also assigns player damage by setPlayerDamage()
    /**
     * <p>
     * setAssignedFirstStrikeDamage.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean setAssignedFirstStrikeDamage() {

        boolean needFirstStrike = setDefendingFirstStrikeDamage();

        CardList block;
        CardList attacking = new CardList(getAttackers());

        for (int i = 0; i < attacking.size(); i++) {

            Card attacker = attacking.get(i);
            block = getBlockers(attacker);

            int damageDealt = attacker.getNetCombatDamage();

            // attacker always gets all blockers' attack

            for (Card b : block) {
                if (b.hasFirstStrike() || b.hasDoubleStrike()) {
                    needFirstStrike = true;
                    int attack = b.getNetCombatDamage();
                    attacker.addAssignedDamage(attack, b);
                }
            }

            if (block.size() == 0) {
                // this damage is assigned to a player by
                // setDefendingFirstStrikeDamage()
            } else if (attacker.hasFirstStrike() || attacker.hasDoubleStrike()) {
                needFirstStrike = true;
                if (getAttackingPlayer().isHuman()) { // human attacks
                    if (attacker.hasKeyword("Trample") || block.size() > 1) {
                        AllZone.getDisplay().assignDamage(attacker, block, damageDealt);
                    } else {
                        block.get(0).addAssignedDamage(damageDealt, attacking.get(i));
                    }
                } else { // computer attacks
                    distributeAIDamage(attacker, block, damageDealt);
                }
            } // if(hasFirstStrike || doubleStrike)
        } // for
        return needFirstStrike;
    } // setAssignedFirstStrikeDamage()

    // set Card.setAssignedDamage() for all creatures in combat
    // also assigns player damage by setPlayerDamage()
    /**
     * <p>
     * setAssignedDamage.
     * </p>
     */
    public final void setAssignedDamage() {
        setDefendingDamage();

        CardList block;
        CardList attacking = new CardList(getAttackers());
        for (int i = 0; i < attacking.size(); i++) {

            Card attacker = attacking.get(i);
            block = getBlockers(attacker);

            int damageDealt = attacker.getNetCombatDamage();

            // attacker always gets all blockers' attack
            for (Card b : block) {
                if (!b.hasFirstStrike() || b.hasDoubleStrike()) {
                    int attack = b.getNetCombatDamage();
                    attacker.addAssignedDamage(attack, b);
                }
            }

            if (block.size() == 0) {
                // this damage is assigned to a player by setDefendingDamage()
            } else if (!attacker.hasFirstStrike() || attacker.hasDoubleStrike()) {

                if (getAttackingPlayer().isHuman()) { // human attacks

                    if (attacker.hasKeyword("Trample") || block.size() > 1) {
                        AllZone.getDisplay().assignDamage(attacker, block, damageDealt);
                    } else {
                        block.get(0).addAssignedDamage(damageDealt, attacking.get(i));
                    }
                } else { // computer attacks
                    distributeAIDamage(attacker, block, damageDealt);
                }
            } // if !hasFirstStrike ...
        } // for

        // should first strike affect the following?

    } // assignDamage()

    /**
     * <p>
     * distributeAIDamage.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @param block
     *            a {@link forge.CardList} object.
     * @param damage
     *            a int.
     */
    private void distributeAIDamage(final Card attacker, final CardList block, int damage) {
        Card c = attacker;

        if (block.size() == 1) {

            Card blocker = block.get(0);

            // trample
            if (attacker.hasKeyword("Trample")) {

                int damageNeeded = 0;

                // TODO if the human can be killed distribute only the minimum
                // of damage to the blocker

                damageNeeded = blocker.getEnoughDamageToKill(damage, attacker, true);

                if (damageNeeded > damage) {
                    damageNeeded = Math.min(blocker.getLethalDamage(), damage);
                } else {
                    damageNeeded = Math.max(blocker.getLethalDamage(), damageNeeded);
                }

                int trample = damage - damageNeeded;

                // If Extra trample damage, assign to defending
                // player/planeswalker
                if (0 < trample) {
                    this.addDefendingDamage(trample, attacker);
                }

                blocker.addAssignedDamage(damageNeeded, attacker);
            } else {
                blocker.addAssignedDamage(damage, attacker);
            }
        } // 1 blocker
        else {
            boolean killsAllBlockers = true; // Does the attacker deal lethal
                                             // damage to all blockers
            for (Card b : block) {
                int enoughDamageToKill = b.getEnoughDamageToKill(damage, attacker, true);
                if (enoughDamageToKill <= damage) {
                    damage -= enoughDamageToKill;
                    CardList cl = new CardList();
                    cl.add(attacker);

                    b.addAssignedDamage(enoughDamageToKill, c);
                } else {
                    killsAllBlockers = false;
                }
            } // for

            // if attacker has no trample, and there's damage left, assign the
            // rest
            // to a random blocker
            if (damage > 0 && !(c.hasKeyword("Trample") && killsAllBlockers)) {
                int index = CardUtil.getRandomIndex(block);
                block.get(index).addAssignedDamage(damage, c);
                damage = 0;
            } else if (c.hasKeyword("Trample") && killsAllBlockers) {
                this.addDefendingDamage(damage, c);
            }
        }
    } // setAssignedDamage()

    /**
     * <p>
     * dealAssignedDamage.
     * </p>
     */
    public static void dealAssignedDamage() {
        // This function handles both Regular and First Strike combat assignment
        Player player = AllZone.getCombat().getDefendingPlayer();

        boolean bFirstStrike = AllZone.getPhase().is(Constant.Phase.COMBAT_FIRST_STRIKE_DAMAGE);

        HashMap<Card, Integer> defMap = AllZone.getCombat().getDefendingDamageMap();

        for (Entry<Card, Integer> entry : defMap.entrySet()) {
            player.addCombatDamage(entry.getValue(), entry.getKey());
        }

        CardList unblocked = new CardList(bFirstStrike ? AllZone.getCombat().getUnblockedAttackers() : AllZone
                .getCombat().getUnblockedFirstStrikeAttackers());

        for (int j = 0; j < unblocked.size(); j++) {
            if (bFirstStrike) {
                CombatUtil.checkUnblockedAttackers(unblocked.get(j));
            } else {
                if (!unblocked.getCard(j).hasFirstStrike() && !unblocked.getCard(j).hasDoubleStrike()) {
                    CombatUtil.checkUnblockedAttackers(unblocked.get(j));
                }
            }
        }

        // this can be much better below here...

        CardList combatants = new CardList();
        combatants.addAll(AllZone.getCombat().getAttackers());
        combatants.addAll(AllZone.getCombat().getAllBlockers());
        combatants.addAll(AllZone.getCombat().getDefendingPlaneswalkers());

        Card c;
        for (int i = 0; i < combatants.size(); i++) {
            c = combatants.get(i);

            // if no assigned damage to resolve, move to next
            if (c.getTotalAssignedDamage() == 0) {
                continue;
            }

            Map<Card, Integer> assignedDamageMap = c.getAssignedDamageMap();
            HashMap<Card, Integer> damageMap = new HashMap<Card, Integer>();

            for (Entry<Card, Integer> entry : assignedDamageMap.entrySet()) {
                Card crd = entry.getKey();
                damageMap.put(crd, entry.getValue());
            }
            c.addCombatDamage(damageMap);

            damageMap.clear();
            c.clearAssignedDamage();
        }

        // This was deeper before, but that resulted in the stack entry acting
        // like before.

    }

    /**
     * <p>
     * isUnblocked.
     * </p>
     * 
     * @param att
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean isUnblocked(final Card att) {
        return unblockedMap.containsKey(att);
    }

    /**
     * <p>
     * getUnblockedAttackers.
     * </p>
     * 
     * @return an array of {@link forge.Card} objects.
     */
    public final Card[] getUnblockedAttackers() {
        CardList out = new CardList();
        Iterator<Card> it = unblockedMap.keySet().iterator();
        while (it.hasNext()) { // only add creatures without firstStrike to this
            // list.
            Card c = (Card) it.next();
            if (!c.hasFirstStrike()) {
                out.add(c);
            }
        }

        return out.toArray();
    } // getUnblockedAttackers()

    /**
     * <p>
     * getUnblockedFirstStrikeAttackers.
     * </p>
     * 
     * @return an array of {@link forge.Card} objects.
     */
    public final Card[] getUnblockedFirstStrikeAttackers() {
        CardList out = new CardList();
        Iterator<Card> it = unblockedMap.keySet().iterator();
        while (it.hasNext()) { // only add creatures without firstStrike to this
            // list.
            Card c = (Card) it.next();
            if (c.hasFirstStrike() || c.hasDoubleStrike()) {
                out.add(c);
            }
        }

        return out.toArray();
    } // getUnblockedAttackers()

    /**
     * <p>
     * addUnblockedAttacker.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void addUnblockedAttacker(final Card c) {
        unblockedMap.put(c, new CardList());
    }

} // Class Combat
