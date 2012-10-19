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
package forge.game.phase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import forge.Card;

import forge.CardLists;
import forge.CardPredicates;
import forge.GameActionUtil;
import forge.GameEntity;
import forge.Singletons;
import forge.card.trigger.TriggerType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.match.CMatchUI;

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
    // value is List<Card> of blockers
    private final Map<Card, List<Card>> attackerMap = new TreeMap<Card, List<Card>>();
    private final Map<Card, List<Card>> blockerMap = new TreeMap<Card, List<Card>>();

    private final Set<Card> blocked = new HashSet<Card>();
    private final HashMap<Card, List<Card>> unblockedMap = new HashMap<Card, List<Card>>();
    private final HashMap<Card, Integer> defendingDamageMap = new HashMap<Card, Integer>();

    // Defenders are the Defending Player + Each controlled Planeswalker
    private List<GameEntity> defenders = new ArrayList<GameEntity>();
    private Map<GameEntity, List<Card>> defenderMap = new HashMap<GameEntity, List<Card>>();
    private int currentDefender = 0;
    private int nextDefender = 0;

    // This Hash keeps track of
    private final HashMap<Card, GameEntity> attackerToDefender = new HashMap<Card, GameEntity>();

    private Player attackingPlayer = null;
    private Player defendingPlayer = null;

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
        this.resetAttackers();
        this.blocked.clear();

        this.unblockedMap.clear();
        this.defendingDamageMap.clear();

        this.attackingPlayer = null;
        this.defendingPlayer = null;
        this.currentDefender = 0;
        this.nextDefender = 0;

        this.initiatePossibleDefenders(Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn().getOpponent());
    }

    /**
     * <p>
     * initiatePossibleDefenders.
     * </p>
     * 
     * @param defender
     *            a {@link forge.game.player.Player} object.
     */
    public final void initiatePossibleDefenders(final Player defender) {
        this.defenders.clear();
        this.defenderMap.clear();
        this.defenders.add(defender);
        this.defenderMap.put(defender, new ArrayList<Card>());
        List<Card> planeswalkers = defender.getCardsIn(ZoneType.Battlefield);
        planeswalkers = CardLists.filter(planeswalkers, CardPredicates.Presets.PLANEWALKERS);
        for (final Card pw : planeswalkers) {
            this.defenders.add(pw);
            this.defenderMap.put(pw, new ArrayList<Card>());
        }
    }

    /**
     * <p>
     * nextDefender.
     * </p>
     * 
     * @return a {@link java.lang.Object} object.
     */
    public final GameEntity nextDefender() {
        if (this.nextDefender >= this.defenders.size()) {
            return null;
        }

        this.currentDefender = this.nextDefender;
        this.nextDefender++;

        return this.defenders.get(this.currentDefender);
    }

    /**
     * <p>
     * getDefender.
     * </p>
     * 
     * @return a {@link java.lang.Object} object.
     */
    public final GameEntity getDefender() {
        return this.defenders.get(this.currentDefender);
    }

    /**
     * <p>
     * Setter for the field <code>currentDefender</code>.
     * </p>
     * 
     * @param def
     *            a int.
     */
    public final void setCurrentDefenderNumber(final int def) {
        this.currentDefender = def;
    }

    /**
     * <p>
     * Setter for the field <code>currentDefender</code>.
     * </p>
     * 
     *  @return a int.
     */
    public final int getCurrentDefenderNumber() {
        return this.currentDefender;
    }

    /**
     * <p>
     * getRemainingDefenders.
     * </p>
     * 
     * @return a int.
     */
    public final int getRemainingDefenders() {
        return this.defenders.size() - this.nextDefender;
    }

    /**
     * <p>
     * Getter for the field <code>defenders</code>.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final List<GameEntity> getDefenders() {
        return this.defenders;
    }

    /**
     * <p>
     * Setter for the field <code>defenders</code>.
     * </p>
     * 
     * @param newDef
     *            a {@link java.util.ArrayList} object.
     */
    public final void setDefenders(final List<GameEntity> newDef) {
        this.defenders = newDef;
        for (GameEntity entity : this.defenders) {
            this.defenderMap.put(entity, new ArrayList<Card>());
        }
    }

    /**
     * <p>
     * getDefendingPlaneswalkers.
     * </p>
     * 
     * @return an array of {@link forge.Card} objects.
     */
    public final List<Card> getDefendingPlaneswalkers() {
        final List<Card> pwDefending = new ArrayList<Card>();

        for (final GameEntity o : this.defenders) {
            if (o instanceof Card) {
                pwDefending.add((Card) o);
            }
        }

        return pwDefending;
    }

    /**
     * <p>
     * Setter for the field <code>attackingPlayer</code>.
     * </p>
     * 
     * @param player
     *            a {@link forge.game.player.Player} object.
     */
    public final void setAttackingPlayer(final Player player) {
        this.attackingPlayer = player;
    }

    /**
     * <p>
     * Setter for the field <code>defendingPlayer</code>.
     * </p>
     * 
     * @param player
     *            a {@link forge.game.player.Player} object.
     */
    public final void setDefendingPlayer(final Player player) {
        this.defendingPlayer = player;
    }

    /**
     * <p>
     * Getter for the field <code>attackingPlayer</code>.
     * </p>
     * 
     * @return a {@link forge.game.player.Player} object.
     */
    public final Player getAttackingPlayer() {
        if (this.attackingPlayer != null) {
            return this.attackingPlayer;
        } else {
            return Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn();
        }
    }

    /**
     * <p>
     * Getter for the field <code>defendingPlayer</code>.
     * </p>
     * 
     * @return a {@link forge.game.player.Player} object.
     */
    public final Player getDefendingPlayer() {
        if (this.attackingPlayer != null) {
            return this.defendingPlayer;
        } else {
            return Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn().getOpponent();
        }
    }

    /**
     * <p>
     * Getter for the field <code>defendingDamageMap</code>.
     * </p>
     * 
     * @return a {@link java.util.HashMap} object.
     */
    public final HashMap<Card, Integer> getDefendingDamageMap() {
        return this.defendingDamageMap;
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
        final GameEntity ge = this.getDefenderByAttacker(source);

        if (ge instanceof Card) {
            final Card pw = (Card) ge;
            pw.addAssignedDamage(n, source);

            return;
        }

        if (!this.defendingDamageMap.containsKey(source)) {
            this.defendingDamageMap.put(source, n);
        } else {
            this.defendingDamageMap.put(source, this.defendingDamageMap.get(source) + n);
        }
    }

    /**
     * <p>
     * sortAttackerByDefender.
     * </p>
     * 
     * @return an array of {@link forge.CardList} objects.
     */
    public final List<List<Card>> sortAttackerByDefender() {
        int size = this.defenders.size();
        final ArrayList<List<Card>> attackers = new ArrayList<List<Card>>(size);
        for (int i = 0; i < size; i++) {
            attackers.add(getAttackersByDefenderSlot(i));
        }
        return attackers;
    }

    public final List<Card> getAttackersByDefenderSlot(int slot) {
        GameEntity entity = this.defenders.get(slot);
        return this.defenderMap.get(entity);
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
        return this.attackerMap.get(c) != null;
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
        this.addAttacker(c, defenders.get(this.currentDefender));
    }

    /**
     * <p>
     * addAttacker.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param defender
     *            a GameEntity object.
     */
    public final void addAttacker(final Card c, GameEntity defender) {
        if (!defenders.contains(defender)) {
            System.out.println("Trying to add Attacker " + c + " to missing defender " + defender);
            return;
        }

        this.attackerMap.put(c, new ArrayList<Card>());
        this.attackerToDefender.put(c, defender);
        this.defenderMap.get(defender).add(c);
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
    public final GameEntity getDefenderByAttacker(final Card c) {
        return this.attackerToDefender.get(c);
    }

    public final GameEntity getDefendingEntity(final Card c) {
        GameEntity defender = this.attackerToDefender.get(c);

        if (this.defenders.contains(defender)) {
            return defender;
        }

        System.out.println("Attacker " + c + " missing defender " + defender);

        return null;
    }

    /**
     * <p>
     * resetAttackers.
     * </p>
     */
    public final void resetAttackers() {
        this.attackerMap.clear();
        this.attackerToDefender.clear();
        this.blockerMap.clear();
    }

    /**
     * <p>
     * getAttackers.
     * </p>
     * 
     * @return an array of {@link forge.Card} objects.
     */
    public final List<Card> getAttackers() {
        return new ArrayList<Card>(this.attackerMap.keySet());
    } // getAttackers()

    /**
     * <p>
     * getAttackerList.
     * </p>
     * 
     * @return an array of {@link forge.Card} objects.
     */
    public final List<Card> getAttackerList() {
        return new ArrayList<Card>(this.attackerMap.keySet());
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
        return this.blocked.contains(attacker);
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
        this.blocked.add(attacker);
        this.attackerMap.get(attacker).add(blocker);
        if (!this.blockerMap.containsKey(blocker)) {
            this.blockerMap.put(blocker, CardLists.createCardList(attacker));
        }
        else {
            this.blockerMap.get(blocker).add(attacker);
        }
    }

    /**
     * <p>
     * getAllBlockers.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    public final List<Card> getAllBlockers() {
        final List<Card> block = new ArrayList<Card>();
        block.addAll(blockerMap.keySet());

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
    public final List<Card> getBlockers(final Card attacker) {
        if (this.getBlockingAttackerList(attacker) == null) {
            return new ArrayList<Card>();
        } else {
            return new ArrayList<Card>(this.getBlockingAttackerList(attacker));
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
    public final List<Card> getAttackersBlockedBy(final Card blocker) {
        if (blockerMap.containsKey(blocker)) {
            return blockerMap.get(blocker);
        }
        return new ArrayList<Card>();
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
    private List<Card> getBlockingAttackerList(final Card attacker) {
        return this.attackerMap.get(attacker);
    }

    /**
     * <p>
     * setBlockerList.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @param blockers
     *            a {@link forge.CardList} object.
     */
    public void setBlockerList(final Card attacker, final List<Card> blockers) {
        this.attackerMap.put(attacker, blockers);
    }

    public void setAttackersBlockedByList(final Card blocker, final List<Card> attackers) {
        this.blockerMap.put(blocker, attackers);
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
        // todo(sol) add some more solid error checking in here
        // is card an attacker?
        if (this.attackerMap.containsKey(c)) {
            // Keep track of all of the different maps
            List<Card> blockers = this.attackerMap.get(c);
            this.attackerMap.remove(c);
            for (Card b : blockers) {
                this.blockerMap.get(b).remove(c);
            }

            // Keep track of all of the different maps
            GameEntity entity = this.attackerToDefender.get(c);
            this.attackerToDefender.remove(c);
            this.defenderMap.get(entity).remove(c);
        } else if (this.blockerMap.containsKey(c)) { // card is a blocker
            List<Card> attackers = this.blockerMap.get(c);

            boolean stillDeclaring = Singletons.getModel().getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS);
            this.blockerMap.remove(c);
            for (Card a : attackers) {
                this.attackerMap.get(a).remove(c);
                if (stillDeclaring && this.attackerMap.get(a).size() == 0) {
                    this.blocked.remove(a);
                }
            }
        }
    } // removeFromCombat()

    /**
     * <p>
     * undoBlockingAssignment.
     * </p>
     * 
     * @param blocker
     *            a {@link forge.Card} object.
     */
    public final void undoBlockingAssignment(final Card blocker) {
        final List<Card> att = this.getAttackerList();
        for (final Card attacker : att) {
            if (this.getBlockers(attacker).contains(blocker)) {
                this.getBlockingAttackerList(attacker).remove(blocker);
                if (this.getBlockers(attacker).size() == 0) {
                    this.blocked.remove(attacker);
                }
            }
        }
        this.blockerMap.remove(blocker);
    } // undoBlockingAssignment(Card)

    /**
     * <p>
     * verifyCreaturesInPlay.
     * </p>
     */
    public final void verifyCreaturesInPlay() {
        final List<Card> all = new ArrayList<Card>();
        all.addAll(this.getAttackers());
        all.addAll(this.getAllBlockers());

        for (int i = 0; i < all.size(); i++) {
            if (!all.get(i).isInPlay()) {
                this.removeFromCombat(all.get(i));
            }
        }
    } // verifyCreaturesInPlay()

    /**
     * <p>
     * setUnblocked.
     * </p>
     */
    public final void setUnblocked() {
        final List<Card> attacking = this.getAttackerList();

        for (final Card attacker : attacking) {
            final List<Card> block = this.getBlockers(attacker);

            if (block.size() == 0) {
                // this damage is assigned to a player by setPlayerDamage()
                this.addUnblockedAttacker(attacker);

                // Run Unblocked Trigger
                final HashMap<String, Object> runParams = new HashMap<String, Object>();
                runParams.put("Attacker", attacker);
                Singletons.getModel().getGame().getTriggerHandler().runTrigger(TriggerType.AttackerUnblocked, runParams);

            }
        }
    }

    private final boolean assignBlockersDamage(boolean firstStrikeDamage) {
        final List<Card> blockers = this.getAllBlockers();
        boolean assignedDamage = false;

        for (final Card blocker : blockers) {
            if (blocker.hasDoubleStrike() || blocker.hasFirstStrike() == firstStrikeDamage) {
                List<Card> attackers = this.getAttackersBlockedBy(blocker);

                final int damage = blocker.getNetCombatDamage();

                if (attackers.size() == 0) {
                    // Just in case it was removed or something
                } else {
                    assignedDamage = true;
                    if (this.getAttackingPlayer().isComputer()) { // ai attacks
                        if (attackers.size() > 1) {
                            CMatchUI.SINGLETON_INSTANCE.assignDamage(blocker, attackers, damage, null);
                        } else {
                            attackers.get(0).addAssignedDamage(damage, blocker);
                        }
                    } else { // computer attacks
                        this.distributeAIDamage(blocker, attackers, damage);
                    }
                }
            }
        }

        return assignedDamage;
    }

    private final boolean assignDamageAsIfNotBlocked(Card attacker) {
        return attacker.hasKeyword("CARDNAME assigns its combat damage as though it weren't blocked.")
                || (attacker.hasKeyword("You may have CARDNAME assign its combat damage as though it weren't blocked.")
                && GameActionUtil.showYesNoDialog(attacker, "Do you want to assign its combat damage as though it weren't blocked?"));
    }

    private final boolean assignAttackersDamage(boolean firstStrikeDamage) {
        this.defendingDamageMap.clear(); // this should really happen in deal damage
        List<Card> blockers = null;
        final List<Card> attackers = this.getAttackerList();
        boolean assignedDamage = false;
        for (final Card attacker : attackers) {
            // If attacker isn't in the right first/regular strike section, continue along
            if (!(attacker.hasDoubleStrike() || attacker.hasFirstStrike() == firstStrikeDamage)) {
                continue;
            }

            // If potential damage is 0, continue along
            final int damageDealt = attacker.getNetCombatDamage();
            if (damageDealt <= 0) {
                continue;
            }

            boolean trampler = attacker.hasKeyword("Trample");
            blockers = this.getBlockers(attacker);
            assignedDamage = true;
            // If the Attacker is unblocked, or it's a trampler and has 0 blockers, deal damage to defender
            if (blockers.size() == 0) {
                if (trampler || this.isUnblocked(attacker)) {
                    this.addDefendingDamage(damageDealt, attacker);
                }
                // Else no damage can be dealt anywhere
            } else {
                if (this.getAttackingPlayer().isHuman()) { // human attacks
                    if (assignDamageAsIfNotBlocked(attacker)) {
                        this.addDefendingDamage(damageDealt, attacker);
                    } else {
                        if (trampler || (blockers.size() > 1)) {
                            CMatchUI.SINGLETON_INSTANCE.assignDamage(attacker, blockers, damageDealt, this.getDefenderByAttacker(attacker));
                        } else {
                            blockers.get(0).addAssignedDamage(damageDealt, attacker);
                        }
                    }
                } else { // computer attacks
                    this.distributeAIDamage(attacker, blockers, damageDealt);
                }
            } // if !hasFirstStrike ...
        } // for
        return assignedDamage;
    }

    public final boolean assignCombatDamage(boolean firstStrikeDamage) {
        boolean assignedDamage = assignAttackersDamage(firstStrikeDamage);
        assignedDamage |= assignBlockersDamage(firstStrikeDamage);
        return assignedDamage;
    }

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
    private void distributeAIDamage(final Card attacker, final List<Card> block, int damage) {
        final Card c = attacker;

        if (attacker.hasKeyword("You may have CARDNAME assign its combat damage as though it weren't blocked.")
                || attacker.hasKeyword("CARDNAME assigns its combat damage as though it weren't blocked.")) {
            this.addDefendingDamage(damage, attacker);
            return;
        }

        final boolean hasTrample = attacker.hasKeyword("Trample");

        if (block.size() == 1) {

            final Card blocker = block.get(0);

            // trample
            if (hasTrample) {

                int damageNeeded = 0;

                // TODO if the human can be killed distribute only the minimum
                // of damage to the blocker

                damageNeeded = blocker.getEnoughDamageToKill(damage, attacker, true);

                if (damageNeeded > damage) {
                    damageNeeded = Math.min(blocker.getLethalDamage(), damage);
                } else {
                    damageNeeded = Math.max(blocker.getLethalDamage(), damageNeeded);
                }

                final int trample = damage - damageNeeded;

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
            boolean killsAllBlockers = true;
            // Does the attacker deal lethal damage to all blockers
            //Blocking Order now determined after declare blockers
            Card lastBlocker = null;
            for (final Card b : block) {
                final int enoughDamageToKill = b.getEnoughDamageToKill(damage, attacker, true);
                if (enoughDamageToKill <= damage) {
                    damage -= enoughDamageToKill;
                    final List<Card> cl = new ArrayList<Card>();
                    cl.add(attacker);

                    b.addAssignedDamage(enoughDamageToKill, c);
                } else {
                    killsAllBlockers = false;
                }
                lastBlocker = b;
            } // for

            if (killsAllBlockers && damage > 0) {
            // if attacker has no trample, and there's damage left, assign the rest to the last blocker
                if (!hasTrample && lastBlocker != null) {
                    lastBlocker.addAssignedDamage(damage, c);
                    damage = 0;
                } else if (hasTrample) {
                    this.addDefendingDamage(damage, c);
                }
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
        final Player player = Singletons.getModel().getGame().getCombat().getDefendingPlayer();

        final boolean bFirstStrike = Singletons.getModel().getGame().getPhaseHandler().is(PhaseType.COMBAT_FIRST_STRIKE_DAMAGE);

        final HashMap<Card, Integer> defMap = Singletons.getModel().getGame().getCombat().getDefendingDamageMap();

        for (final Entry<Card, Integer> entry : defMap.entrySet()) {
            player.addCombatDamage(entry.getValue(), entry.getKey());
        }

        final List<Card> unblocked = new ArrayList<Card>(bFirstStrike ? Singletons.getModel().getGame().getCombat().getUnblockedAttackers() : Singletons.getModel().getGame().getCombat().getUnblockedFirstStrikeAttackers());

        for (int j = 0; j < unblocked.size(); j++) {
            if (bFirstStrike) {
                CombatUtil.checkUnblockedAttackers(unblocked.get(j));
            } else {
                if (!unblocked.get(j).hasFirstStrike() && !unblocked.get(j).hasDoubleStrike()) {
                    CombatUtil.checkUnblockedAttackers(unblocked.get(j));
                }
            }
        }

        // this can be much better below here...

        final List<Card> combatants = new ArrayList<Card>();
        combatants.addAll(Singletons.getModel().getGame().getCombat().getAttackers());
        combatants.addAll(Singletons.getModel().getGame().getCombat().getAllBlockers());
        combatants.addAll(Singletons.getModel().getGame().getCombat().getDefendingPlaneswalkers());

        Card c;
        for (int i = 0; i < combatants.size(); i++) {
            c = combatants.get(i);

            // if no assigned damage to resolve, move to next
            if (c.getTotalAssignedDamage() == 0) {
                continue;
            }

            final Map<Card, Integer> assignedDamageMap = c.getAssignedDamageMap();
            final HashMap<Card, Integer> damageMap = new HashMap<Card, Integer>();

            for (final Entry<Card, Integer> entry : assignedDamageMap.entrySet()) {
                final Card crd = entry.getKey();
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
        return this.unblockedMap.containsKey(att);
    }

    /**
     * <p>
     * getUnblockedAttackers.
     * </p>
     * 
     * @return an array of {@link forge.Card} objects.
     */
    public final List<Card> getUnblockedAttackers() {
        final List<Card> out = new ArrayList<Card>();
        for (Card c : this.unblockedMap.keySet()) {
            if (!c.hasFirstStrike()) {
                out.add(c);
            }
        }
        return out;
    } // getUnblockedAttackers()

    /**
     * <p>
     * getUnblockedFirstStrikeAttackers.
     * </p>
     * 
     * @return an array of {@link forge.Card} objects.
     */
    public final List<Card> getUnblockedFirstStrikeAttackers() {
        final List<Card> out = new ArrayList<Card>();
        for (Card c : this.unblockedMap.keySet()) { // only add creatures without firstStrike to this
            if (c.hasFirstStrike() || c.hasDoubleStrike()) {
                out.add(c);
            }
        }
        return out;
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
        this.unblockedMap.put(c, new ArrayList<Card>());
    }

} // Class Combat
