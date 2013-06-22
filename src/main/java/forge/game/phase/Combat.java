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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.common.collect.Lists;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.GameEntity;
import forge.card.trigger.TriggerType;
import forge.game.combat.AttackingBand;
import forge.game.event.GameEventBlockerAssigned;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * <p>
 * Combat class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Combat {
    // List of AttackingBands
    private final List<AttackingBand> attackingBands = new ArrayList<AttackingBand>();
    // Attacker -> AttackingBand (Attackers can only be in a single band)
    private final Map<Card, AttackingBand> attackerToBandMap = new TreeMap<Card, AttackingBand>();
    // Blocker -> AttackingBands (Blockers can block multiple bands/creatures
    private final Map<Card, List<AttackingBand>> blockerToBandsMap = new TreeMap<Card, List<AttackingBand>>();

    private final HashMap<Card, Integer> defendingDamageMap = new HashMap<Card, Integer>();

    // Defenders are all Opposing Players + Planeswalker's Controller By Opposing Players
    private Map<GameEntity, List<Card>> defenderMap = new HashMap<GameEntity, List<Card>>();
    
    private Map<Card, List<Card>> blockerDamageAssignmentOrder = new TreeMap<Card, List<Card>>();
    private Map<Card, List<Card>> attackerDamageAssignmentOrder = new TreeMap<Card, List<Card>>();

    private Player attackingPlayer = null;

    /**
     * <p>
     * reset.
     * </p>
     */
    public final void reset(Player playerTurn) {
        this.resetAttackers();
        this.defendingDamageMap.clear();
        this.attackingPlayer = playerTurn;

        this.initiatePossibleDefenders(playerTurn.getOpponents());
    }

    /**
     * <p>
     * initiatePossibleDefenders.
     * </p>
     * 
     * @param defender
     *            a {@link forge.game.player.Player} object.
     */
    public final void initiatePossibleDefenders(final Iterable<Player> defenders) {
        this.defenderMap.clear();
        for (Player defender : defenders) {
            fillDefenderMaps(defender);
        }
    }

    public final void initiatePossibleDefenders(final Player defender) {
        this.defenderMap.clear();
        fillDefenderMaps(defender);
    }
    
    public final boolean isCombat() {
        return !attackingBands.isEmpty();
    }

    private void fillDefenderMaps(final Player defender) {
        this.defenderMap.put(defender, new ArrayList<Card>());
        List<Card> planeswalkers =
                CardLists.filter(defender.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.PLANEWALKERS);
        for (final Card pw : planeswalkers) {
            this.defenderMap.put(pw, new ArrayList<Card>());
        }
    }

    /**
     * <p>
     * Getter for the field <code>defenders</code>.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final List<GameEntity> getDefenders() {
        return new ArrayList<GameEntity>(this.defenderMap.keySet());
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
        this.defenderMap.clear();
        for (GameEntity entity : newDef) {
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
    public final List<Player> getDefendingPlayers() {
        final List<Player> defending = new ArrayList<Player>();

        for (final GameEntity o : this.defenderMap.keySet()) {
            if (o instanceof Player) {
                defending.add((Player) o);
            }
        }

        return defending;
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

        for (final GameEntity o : this.defenderMap.keySet()) {
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
     * Getter for the field <code>attackingPlayer</code>.
     * </p>
     * 
     * @return a {@link forge.game.player.Player} object.
     */
    public final Player getAttackingPlayer() {
        return this.attackingPlayer;
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
            final Card planeswalker = (Card) ge;
            planeswalker.addAssignedDamage(n, source);

            return;
        }

        if (!this.defendingDamageMap.containsKey(source)) {
            this.defendingDamageMap.put(source, n);
        } else {
            this.defendingDamageMap.put(source, this.defendingDamageMap.get(source) + n);
        }
    }

    public final List<Card> getAttackersOf(GameEntity defender) {
        return defenderMap.get(defender);
    }
    
    public final List<AttackingBand> getAttackingBandsOf(GameEntity defender) {
        List<AttackingBand> bands = new ArrayList<AttackingBand>();
        for(AttackingBand band : this.attackingBands) {
            if (band.getDefender().equals(defender)) {
                bands.add(band);
            }
        }
        return bands;
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
        return this.attackerToBandMap.containsKey(c);
    }

    public final void addAttacker(final Card c, GameEntity defender) {
        addAttacker(c, defender, null);
    }
    
    public final void addAttacker(final Card c, GameEntity defender, AttackingBand band) {
        addAttacker(c, defender, band, null);
    }
    
    public final void addAttacker(final Card c, GameEntity defender, boolean blocked) {
        addAttacker(c, defender, null, blocked);
    }
    
    public final void addAttacker(final Card c, GameEntity defender, AttackingBand band, Boolean blocked) {
        if (!defenderMap.containsKey(defender)) {
            System.out.println("Trying to add Attacker " + c + " to missing defender " + defender);
            return;
        }
        
        if (band == null || !this.attackingBands.contains(band)) {
            band = new AttackingBand(c, defender);
            if (blocked != null) {
                band.setBlocked(blocked.booleanValue());
            }
            this.attackingBands.add(band);
        } else {
            band.addAttacker(c);
        }
        // Attacker -> Defender and Defender -> Attacker map to Bands?
        this.defenderMap.get(defender).add(c);
        this.attackerToBandMap.put(c, band);
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
        if (!this.attackerToBandMap.containsKey(c)) {
            return null;
        }
        return this.attackerToBandMap.get(c).getDefender();
    }

    public final Player getDefenderPlayerByAttacker(final Card c) {
        GameEntity defender = getDefenderByAttacker(c);

        // System.out.println(c.toString() + " attacks " + defender.toString());
        if (defender instanceof Player) {
            return (Player) defender;
        }

        // maybe attack on a controlled planeswalker?
        if (defender instanceof Card) {
            return ((Card) defender).getController();
        }
        return null;
    }

    public final GameEntity getDefendingEntity(final Card c) {
        GameEntity defender = this.getDefenderByAttacker(c);
        if (this.defenderMap.containsKey(defender)) {
            return defender;
        }

        System.out.println("Attacker " + c + " missing defender " + defender);
        return null;
    }

    public final AttackingBand getBandByAttacker(final Card c) {
        return this.attackerToBandMap.get(c);
    }

    /**
     * <p>
     * resetAttackers.
     * </p>
     */
    public final void resetAttackers() {
        this.attackingBands.clear();
        this.blockerToBandsMap.clear();
        this.attackerToBandMap.clear();
        this.blockerDamageAssignmentOrder.clear();
        this.attackerDamageAssignmentOrder.clear();
    }

    /**
     * <p>
     * getAttackers.
     * </p>
     * 
     * @return an array of {@link forge.Card} objects.
     */
    public final List<AttackingBand> getAttackingBands() {
        return attackingBands;
    } // getAttackers()
    
    public final List<Card> getAttackers() {
        List<Card> attackers = new ArrayList<Card>();
        for(AttackingBand band : attackingBands) {
            attackers.addAll(band.getAttackers());
        }
        return attackers;
    }

    public final boolean isBlocked(final Card attacker) {
        return this.attackerToBandMap.get(attacker).getBlocked();
    }

    public final void setBlocked(final Card attacker) {
        this.attackerToBandMap.get(attacker).setBlocked(true);
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
        AttackingBand band = this.attackerToBandMap.get(attacker);
        band.addBlocker(blocker);
        
        if (!this.blockerToBandsMap.containsKey(blocker)) {
            this.blockerToBandsMap.put(blocker, Lists.newArrayList(band));
        } else {
            this.blockerToBandsMap.get(blocker).add(band);
        }
        attacker.getGame().fireEvent(new GameEventBlockerAssigned());
    }

    public final void removeBlockAssignment(final Card attacker, final Card blocker) {
        AttackingBand band = this.attackerToBandMap.get(attacker);
        band.removeBlocker(blocker);
        this.blockerToBandsMap.get(blocker).remove(attacker);
        if (this.blockerToBandsMap.get(blocker).isEmpty()) {
            this.blockerToBandsMap.remove(blocker);
        }
    }

    /**
     * <p>
     * undoBlockingAssignment.
     * </p>
     * 
     * @param blocker
     *            a {@link forge.Card} object.
     */
    public final void undoBlockingAssignment(final Card blocker) {
        final List<AttackingBand> att = this.blockerToBandsMap.get(blocker);
        for (final AttackingBand band : att) {
            band.removeBlocker(blocker);
        }
        this.blockerToBandsMap.remove(blocker);
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
        block.addAll(blockerToBandsMap.keySet());

        return block;
    }

    public final List<Card> getBlockers(final AttackingBand band) {
        List<Card> list = band.getBlockers();
        if (list == null) {
            return new ArrayList<Card>();
        } else {
            return new ArrayList<Card>(list);
        }
    }

    public final List<Card> getBlockers(final Card card) {
        return getBlockers(card, false);
    }

    public final List<Card> getBlockers(final Card card, boolean ordered) {
        // If requesting the ordered blocking list pass true, directly. 
        List<Card> list = null;
        if (ordered) {
            list = this.attackerDamageAssignmentOrder.containsKey(card) ? this.attackerDamageAssignmentOrder.get(card) : null;
        } else {
            list = this.getBandByAttacker(card) != null ? this.getBandByAttacker(card).getBlockers() : null;
        }

        if (list == null) {
            return new ArrayList<Card>();
        } else {
            return new ArrayList<Card>(list);
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
        List<Card> blocked =  new ArrayList<Card>();

        if (blockerToBandsMap.containsKey(blocker)) {
            for(AttackingBand band : blockerToBandsMap.get(blocker)) {
                blocked.addAll(band.getAttackers());
            }
        }
        return blocked;
    }

    /**
     * <p>
     * getDefendingPlayer.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @return a {@link forge.Player} object.
     */
    public List<Player> getDefendingPlayerRelatedTo(final Card source) {
        List<Player> players = new ArrayList<Player>();
        Card attacker = source;
        if (source.isAura()) {
            attacker = source.getEnchantingCard();
        } else if (source.isEquipment()) {
            attacker = source.getEquippingCard();
        } else if (source.isFortification()) {
            attacker = source.getFortifyingCard();
        }

        // return the corresponding defender
        Player defender = getDefenderPlayerByAttacker(attacker);
        if (null != defender) {
            players.add(defender);
            return players;
        }
        
        // Can't figure out who it's related to... just return all???
        // return all defending players
        List<GameEntity> defenders = this.getDefenders();
        for (GameEntity ge : defenders) {
            if (ge instanceof Player) {
                players.add((Player) ge);
            }
        }
        return players;
    }

    public void setAttackerDamageAssignmentOrder(final Card attacker, final List<Card> blockers) {
        this.attackerDamageAssignmentOrder.put(attacker, blockers);
    }

    public void setBlockerDamageAssignmentOrder(final Card blocker, final List<Card> attackers) {
        this.blockerDamageAssignmentOrder.put(blocker, attackers);
    }

    public final void removeFromCombat(final Card c) {
        // is card an attacker?
        if (this.attackerToBandMap.containsKey(c)) {
            // Soooo many maps to keep track of
            AttackingBand band = this.attackerToBandMap.get(c);
            band.removeAttacker(c);
            this.attackerToBandMap.remove(c);
            this.attackerDamageAssignmentOrder.remove(c);

            List<Card> blockers = band.getBlockers();
            for (Card b : blockers) {
                if (band.getAttackers().isEmpty()) {
                    this.blockerToBandsMap.get(b).remove(c);
                }
                // Clear removed attacker from assignment order
                if (this.blockerDamageAssignmentOrder.containsKey(b)) {
                    this.blockerDamageAssignmentOrder.get(b).remove(c);
                }
            }

            this.defenderMap.get(band.getDefender()).remove(c);
            
            if (band.getAttackers().isEmpty() && band.getBlockers().isEmpty()) {
                this.getAttackingBands().remove(band);
            }
        } else if (this.blockerToBandsMap.containsKey(c)) { // card is a blocker
            List<AttackingBand> attackers = this.blockerToBandsMap.get(c);

            this.blockerToBandsMap.remove(c);
            this.blockerDamageAssignmentOrder.remove(c);
            for (AttackingBand a : attackers) {
                a.removeBlocker(c);
                for(Card atk : a.getAttackers()) {
                    if (this.attackerDamageAssignmentOrder.containsKey(atk)) {
                        this.attackerDamageAssignmentOrder.get(atk).remove(c);
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
    public final void removeAbsentCombatants() {
        final List<Card> all = new ArrayList<Card>();
        for(AttackingBand band : this.getAttackingBands()) {
            all.addAll(band.getAttackers());
        }
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
    public final void setUnblockedAttackers() {
        final List<AttackingBand> attacking = this.getAttackingBands();
        for (final AttackingBand band : attacking) {
            band.calculateBlockedState();

            if (!band.getBlocked()) {
                for (Card attacker : band.getAttackers()) {
                    // Run Unblocked Trigger
                    final HashMap<String, Object> runParams = new HashMap<String, Object>();
                    runParams.put("Attacker", attacker);
                    runParams.put("Defender",this.getDefenderByAttacker(attacker));
                    attacker.getGame().getTriggerHandler().runTrigger(TriggerType.AttackerUnblocked, runParams, false);
                }
            }
        }
    }

    private final boolean assignBlockersDamage(boolean firstStrikeDamage) {
        // Assign damage by Blockers
        final List<Card> blockers = this.getAllBlockers();
        boolean assignedDamage = false;

        for (final Card blocker : blockers) {
            if (blocker.hasDoubleStrike() || blocker.hasFirstStrike() == firstStrikeDamage) {
                List<Card> attackers = this.blockerDamageAssignmentOrder.get(blocker);

                final int damage = blocker.getNetCombatDamage();

                if (!attackers.isEmpty()) {
                    Player attackingPlayer = this.getAttackingPlayer();
                    Player assigningPlayer = blocker.getController();

                    List<Card> bandingAttackers = CardLists.getKeyword(attackers, "Banding");
                    if (!bandingAttackers.isEmpty()) {
                        assigningPlayer = attackingPlayer;
                    } else {
                        // TODO Get each bands with other creature
                        // Check if any other valid creatures matches the bands with other
                        // assigningPlayer = blockingBand.get(0).getController();
                    }

                    assignedDamage = true;
                    Map<Card, Integer> map = assigningPlayer.getController().assignCombatDamage(blocker, attackers, damage, null, assigningPlayer != blocker.getController());
                    for (Entry<Card, Integer> dt : map.entrySet()) {
                        dt.getKey().addAssignedDamage(dt.getValue(), blocker);
                        dt.getKey().updateObservers();
                    }
                }
            }
        }

        return assignedDamage;
    }

    private final boolean assignAttackersDamage(boolean firstStrikeDamage) {
     // Assign damage by Attackers
        this.defendingDamageMap.clear(); // this should really happen in deal damage
        List<Card> blockers = null;
        final List<Card> attackers = this.getAttackers();
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
            
            AttackingBand band = this.getBandByAttacker(attacker);

            boolean trampler = attacker.hasKeyword("Trample");
            blockers = this.attackerDamageAssignmentOrder.get(attacker);
            assignedDamage = true;
            // If the Attacker is unblocked, or it's a trampler and has 0 blockers, deal damage to defender
            if (blockers == null || blockers.isEmpty()) {
                if (trampler || !band.getBlocked()) {
                    this.addDefendingDamage(damageDealt, attacker);
                } // No damage happens if blocked but no blockers left
            } else {
                GameEntity defender = band.getDefender();
                Player assigningPlayer = this.getAttackingPlayer();
                // Defensive Formation is very similar to Banding with Blockers
                // It allows the defending player to assign damage instead of the attacking player
                if (defender instanceof Player && defender.hasKeyword("You assign combat damage of each creature attacking you.")) {
                    assigningPlayer = (Player)defender;
                } else {
                    List<Card> blockingBand = CardLists.getKeyword(blockers, "Banding");
                    if (!blockingBand.isEmpty()) {
                        assigningPlayer = blockingBand.get(0).getController();
                    } else {
                        // TODO Get each bands with other creature
                        // Check if any other valid creatures matches the bands with other
                        // assigningPlayer = blockingBand.get(0).getController();
                    }
                }

                Map<Card, Integer> map = assigningPlayer.getController().assignCombatDamage(attacker, blockers, damageDealt, defender, this.getAttackingPlayer() != assigningPlayer);
                for (Entry<Card, Integer> dt : map.entrySet()) {
                    if( dt.getKey() == null) {
                        if (dt.getValue() > 0) 
                            addDefendingDamage(dt.getValue(), attacker);
                    } else {
                        dt.getKey().addAssignedDamage(dt.getValue(), attacker);
                        dt.getKey().updateObservers();
                    }
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
     * dealAssignedDamage.
     * </p>
     */
    public void dealAssignedDamage() {
        // This function handles both Regular and First Strike combat assignment

        final HashMap<Card, Integer> defMap = this.defendingDamageMap;
        final HashMap<GameEntity, List<Card>> wasDamaged = new HashMap<GameEntity, List<Card>>();

        for (final Entry<Card, Integer> entry : defMap.entrySet()) {
            GameEntity defender = getDefendingEntity(entry.getKey());
            if (defender instanceof Player) { // player
                if (((Player) defender).addCombatDamage(entry.getValue(), entry.getKey())) {
                    if (wasDamaged.containsKey(defender)) {
                        wasDamaged.get(defender).add(entry.getKey());
                    } else {
                        List<Card> l = new ArrayList<Card>();
                        l.add(entry.getKey());
                        wasDamaged.put(defender, l);
                    }
                }
            } else if (defender instanceof Card) { // planeswalker
                if (((Card) defender).getController().addCombatDamage(entry.getValue(), entry.getKey())) {
                    if (wasDamaged.containsKey(defender)) {
                        wasDamaged.get(defender).add(entry.getKey());
                    } else {
                        List<Card> l = new ArrayList<Card>();
                        l.add(entry.getKey());
                        wasDamaged.put(defender, l);
                    }
                }
            }
        }

        // this can be much better below here...

        final List<Card> combatants = new ArrayList<Card>();
        combatants.addAll(this.getAttackers());
        combatants.addAll(this.getAllBlockers());
        combatants.addAll(this.getDefendingPlaneswalkers());

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
        
        // Run triggers
        for (final GameEntity ge : wasDamaged.keySet()) {
            final HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("DamageSources", wasDamaged.get(ge));
            runParams.put("DamageTarget", ge);
            ge.getGame().getTriggerHandler().runTrigger(TriggerType.CombatDamageDoneOnce, runParams, false);
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
        return !this.attackerToBandMap.get(att).getBlocked();
    }

    /**
     * <p>
     * getUnblockedAttackers.
     * </p>
     * 
     * @return an array of {@link forge.Card} objects.
     */
    public final List<Card> getUnblockedAttackers() {
        ArrayList<Card> unblocked = new ArrayList<Card>();
        for (AttackingBand band : this.attackingBands) {
            if (!band.getBlocked()) {
                unblocked.addAll(band.getAttackers());
            }
        }

        return unblocked;
    }

    public boolean isPlayerAttacked(Player priority) {
        for(GameEntity defender : defenderMap.keySet()) {
            if ((defender instanceof Player && priority.equals(defender)) ||
                    (defender instanceof Card && priority.equals(((Card)defender).getController()))) {
                List<Card> attackers = defenderMap.get(defender);
                if (attackers != null && !attackers.isEmpty())
                    return true;
            }
        }

        return false;
    }
} // Class Combat
