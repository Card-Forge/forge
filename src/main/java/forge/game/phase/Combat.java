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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardUtil;
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
    // value is CardList of blockers
    private final Map<Card, CardList> map = new TreeMap<Card, CardList>();
    private final Set<Card> blocked = new HashSet<Card>();

    private final HashMap<Card, CardList> unblockedMap = new HashMap<Card, CardList>();
    private final HashMap<Card, Integer> defendingDamageMap = new HashMap<Card, Integer>();

    // Defenders are the Defending Player + Each Planeswalker that player
    // controls
    private List<GameEntity> defenders = new ArrayList<GameEntity>();
    private int currentDefender = 0;
    private int nextDefender = 0;

    // This Hash keeps track of
    private final HashMap<Card, Object> attackerToDefender = new HashMap<Card, Object>();

    private int attackingDamage;

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

        this.attackingDamage = 0;
        this.defendingDamageMap.clear();

        this.attackingPlayer = null;
        this.defendingPlayer = null;
        this.currentDefender = 0;
        this.nextDefender = 0;

        this.initiatePossibleDefenders(Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn().getOpponent());
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
        this.defenders.add(defender);
        CardList planeswalkers = defender.getCardsIn(ZoneType.Battlefield);
        planeswalkers = planeswalkers.getType("Planeswalker");
        for (final Card pw : planeswalkers) {
            this.defenders.add(pw);
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
        if (this.nextDefender >= this.defenders.size()) {
            return null;
        }

        this.currentDefender = this.nextDefender;
        this.nextDefender++;

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
    public final void setCurrentDefender(final int def) {
        this.currentDefender = def;
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
     * getDeclaredAttackers.
     * </p>
     * 
     * @return a int.
     */
    public final int getDeclaredAttackers() {
        return this.attackerToDefender.size();
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
            return Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();
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
            return Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn().getOpponent();
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
     * getTotalDefendingDamage.
     * </p>
     * 
     * @return a int.
     */
    public final int getTotalDefendingDamage() {
        int total = 0;

        final Collection<Integer> c = this.defendingDamageMap.values();

        final Iterator<Integer> itr = c.iterator();
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
        this.defendingDamageMap.clear();
        final CardList att = this.getAttackerList();
        // sum unblocked attackers' power
        for (int i = 0; i < att.size(); i++) {
            if (!this.isBlocked(att.get(i))
                    || ((this.getBlockers(att.get(i)).size() == 0) && att.get(i).hasKeyword("Trample"))) {

                final int damageDealt = att.get(i).getNetCombatDamage();

                if (damageDealt > 0) {
                    // if the creature has first strike do not do damage in the
                    // normal combat phase
                    if (!att.get(i).hasFirstStrike() || att.get(i).hasDoubleStrike()) {
                        this.addDefendingDamage(damageDealt, att.get(i));
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
        this.defendingDamageMap.clear();
        final CardList att = this.getAttackerList();
        // sum unblocked attackers' power
        for (int i = 0; i < att.size(); i++) {
            if (!this.isBlocked(att.get(i))
                    || ((this.getBlockers(att.get(i)).size() == 0) && att.get(i).hasKeyword("Trample"))) {

                final int damageDealt = att.get(i).getNetCombatDamage();

                if (damageDealt > 0) {
                    // if the creature has first strike or double strike do
                    // damage in the first strike combat phase
                    if (att.get(i).hasFirstStrike() || att.get(i).hasDoubleStrike()) {
                        this.addDefendingDamage(damageDealt, att.get(i));
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
        final String slot = this.getDefenderByAttacker(source).toString();
        final Object o = this.defenders.get(Integer.parseInt(slot));

        if (o instanceof Card) {
            final Card pw = (Card) o;
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
     * addAttackingDamage.
     * </p>
     * 
     * @param n
     *            a int.
     */
    public final void addAttackingDamage(final int n) {
        this.attackingDamage += n;
    }

    /**
     * <p>
     * Getter for the field <code>attackingDamage</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getAttackingDamage() {
        return this.attackingDamage;
    }

    /**
     * <p>
     * sortAttackerByDefender.
     * </p>
     * 
     * @return an array of {@link forge.CardList} objects.
     */
    public final CardList[] sortAttackerByDefender() {
        final CardList[] attackers = new CardList[this.defenders.size()];
        for (int i = 0; i < attackers.length; i++) {
            attackers[i] = new CardList();
        }

        for (final Card atk : this.attackerToDefender.keySet()) {
            final Object o = this.attackerToDefender.get(atk);
            final int i = Integer.parseInt(o.toString());
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
        return this.map.get(c) != null;
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
        this.map.put(c, new CardList());
        this.attackerToDefender.put(c, this.currentDefender);
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
        return this.attackerToDefender.get(c);
    }

    /**
     * <p>
     * resetAttackers.
     * </p>
     */
    public final void resetAttackers() {
        this.map.clear();
        this.attackerToDefender.clear();
    }

    /**
     * <p>
     * getAttackers.
     * </p>
     * 
     * @return an array of {@link forge.Card} objects.
     */
    public final List<Card> getAttackers() {
        return new ArrayList<Card>(this.map.keySet());
    } // getAttackers()

    /**
     * <p>
     * getAttackerList.
     * </p>
     * 
     * @return an array of {@link forge.Card} objects.
     */
    public final CardList getAttackerList() {
        return new CardList(this.map.keySet());
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
        this.getList(attacker).add(blocker);
    }

    /**
     * <p>
     * getAllBlockers.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    public final CardList getAllBlockers() {
        final CardList att = this.getAttackerList();
        final CardList block = new CardList();

        for (int i = 0; i < att.size(); i++) {
            block.addAll(this.getBlockers(att.get(i)));
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
        if (this.getList(attacker) == null) {
            return new CardList();
        } else {
            return new CardList(this.getList(attacker));
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
        final CardList att = this.getAttackerList();

        for (int i = 0; i < att.size(); i++) {
            if (this.getBlockers(att.get(i)).contains(blocker)) {
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
        return this.map.get(attacker);
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
        final CardList att = this.getAttackerList();
        if (att.contains(c)) {
            this.map.remove(c);
            this.attackerToDefender.remove(c);
        } else { // card is a blocker
            for (final Card a : att) {
                if (this.getBlockers(a).contains(c)) {
                    this.getList(a).remove(c);
                    // TODO if Declare Blockers and Declare Blockers (Abilities)
                    // merge this logic needs to be tweaked
                    if ((this.getBlockers(a).size() == 0)
                            && Singletons.getModel().getGameState().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                        this.blocked.remove(a);
                    }
                }
            }
        }
    } // removeFromCombat()

    /**
     * <p>
     * verifyCreaturesInPlay.
     * </p>
     */
    public final void verifyCreaturesInPlay() {
        final CardList all = new CardList();
        all.addAll(this.getAttackers());
        all.addAll(this.getAllBlockers());

        for (int i = 0; i < all.size(); i++) {
            if (!AllZoneUtil.isCardInPlay(all.get(i))) {
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
        final CardList attacking = this.getAttackerList();

        for (final Card attacker : attacking) {
            final CardList block = this.getBlockers(attacker);

            if (block.size() == 0) {
                // this damage is assigned to a player by setPlayerDamage()
                this.addUnblockedAttacker(attacker);

                // Run Unblocked Trigger
                final HashMap<String, Object> runParams = new HashMap<String, Object>();
                runParams.put("Attacker", attacker);
                AllZone.getTriggerHandler().runTrigger(TriggerType.AttackerUnblocked, runParams);

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

        boolean needFirstStrike = this.setDefendingFirstStrikeDamage();

        CardList block;
        final CardList attacking = this.getAttackerList();

        for (int i = 0; i < attacking.size(); i++) {

            final Card attacker = attacking.get(i);
            block = this.getBlockers(attacker);

            final int damageDealt = attacker.getNetCombatDamage();

            // attacker always gets all blockers' attack

            for (final Card b : block) {
                if (b.hasFirstStrike() || b.hasDoubleStrike()) {
                    needFirstStrike = true;
                    final int attack = b.getNetCombatDamage();
                    attacker.addAssignedDamage(attack, b);
                }
            }

            if (block.size() == 0) {
                // this damage is assigned to a player by
                // setDefendingFirstStrikeDamage()
            } else if (attacker.hasFirstStrike() || attacker.hasDoubleStrike()) {
                needFirstStrike = true;
                if (this.getAttackingPlayer().isHuman()) { // human attacks
                    if ((attacker.hasKeyword("You may have CARDNAME assign its combat damage as though it weren't blocked.")
                            && GameActionUtil.showYesNoDialog(attacker, "Do you want to assign its combat damage as"
                                    + " though it weren't blocked?"))
                                    || attacker.hasKeyword("CARDNAME assigns its combat damage as though it weren't blocked.")) {
                        this.addDefendingDamage(damageDealt, attacker);
                    } else {
                        if (attacker.hasKeyword("Trample") || (block.size() > 1)) {
                            CMatchUI.SINGLETON_INSTANCE.assignDamage(attacker, block, damageDealt);
                        } else {
                            block.get(0).addAssignedDamage(damageDealt, attacking.get(i));
                        }
                    }
                } else { // computer attacks
                    this.distributeAIDamage(attacker, block, damageDealt);
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
        this.setDefendingDamage();

        CardList block;
        final CardList attacking = this.getAttackerList();
        for (int i = 0; i < attacking.size(); i++) {

            final Card attacker = attacking.get(i);
            block = this.getBlockers(attacker);

            final int damageDealt = attacker.getNetCombatDamage();

            // attacker always gets all blockers' attack
            for (final Card b : block) {
                if (!b.hasFirstStrike() || b.hasDoubleStrike()) {
                    final int attack = b.getNetCombatDamage();
                    attacker.addAssignedDamage(attack, b);
                }
            }

            if (block.size() == 0) {
                // this damage is assigned to a player by setDefendingDamage()
            } else if (!attacker.hasFirstStrike() || attacker.hasDoubleStrike()) {

                if (this.getAttackingPlayer().isHuman()) { // human attacks

                    if ((attacker.hasKeyword("You may have CARDNAME assign its combat damage as though it weren't blocked.")
                        && GameActionUtil.showYesNoDialog(attacker, "Do you want to assign its combat damage as"
                                + " though it weren't blocked?"))
                                || attacker.hasKeyword("CARDNAME assigns its combat damage as though it weren't blocked.")) {
                        this.addDefendingDamage(damageDealt, attacker);
                    } else {
                        if (attacker.hasKeyword("Trample") || (block.size() > 1)) {
                            CMatchUI.SINGLETON_INSTANCE.assignDamage(attacker, block, damageDealt);
                        } else {
                            block.get(0).addAssignedDamage(damageDealt, attacking.get(i));
                        }
                    }
                } else { // computer attacks
                    this.distributeAIDamage(attacker, block, damageDealt);
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
        final Card c = attacker;

        if (attacker.hasKeyword("You may have CARDNAME assign its combat damage as though it weren't blocked.")
                || attacker.hasKeyword("CARDNAME assigns its combat damage as though it weren't blocked.")) {
            this.addDefendingDamage(damage, attacker);
            return;
        }

        if (block.size() == 1) {

            final Card blocker = block.get(0);

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
            boolean killsAllBlockers = true; // Does the attacker deal lethal
                                             // damage to all blockers
            for (final Card b : block) {
                final int enoughDamageToKill = b.getEnoughDamageToKill(damage, attacker, true);
                if (enoughDamageToKill <= damage) {
                    damage -= enoughDamageToKill;
                    final CardList cl = new CardList();
                    cl.add(attacker);

                    b.addAssignedDamage(enoughDamageToKill, c);
                } else {
                    killsAllBlockers = false;
                }
            } // for

            // if attacker has no trample, and there's damage left, assign the
            // rest
            // to a random blocker
            if ((damage > 0) && !(c.hasKeyword("Trample") && killsAllBlockers)) {
                final int index = CardUtil.getRandomIndex(block);
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
        final Player player = AllZone.getCombat().getDefendingPlayer();

        final boolean bFirstStrike = Singletons.getModel().getGameState().getPhaseHandler().is(PhaseType.COMBAT_FIRST_STRIKE_DAMAGE);

        final HashMap<Card, Integer> defMap = AllZone.getCombat().getDefendingDamageMap();

        for (final Entry<Card, Integer> entry : defMap.entrySet()) {
            player.addCombatDamage(entry.getValue(), entry.getKey());
        }

        final CardList unblocked = new CardList(bFirstStrike ? AllZone.getCombat().getUnblockedAttackers() : AllZone
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

        final CardList combatants = new CardList();
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
        this.unblockedMap.put(c, new CardList());
    }

} // Class Combat
