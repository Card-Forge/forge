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
package forge.game.combat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Lists;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.GameEntity;
import forge.card.trigger.TriggerType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.maps.CollectionSuppliers;
import forge.util.maps.HashMapOfLists;
import forge.util.maps.MapOfLists;

/**
 * <p>
 * Combat class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Combat {
    private final Player playerWhoAttacks;
    // Defenders, as they are attacked by hostile forces
    private final MapOfLists<GameEntity, AttackingBand> attackedEntities = new HashMapOfLists<GameEntity, AttackingBand>(CollectionSuppliers.<AttackingBand>arrayLists());
    // Blockers to stop the hostile invaders
    private final MapOfLists<AttackingBand, Card> blockedBands = new HashMapOfLists<AttackingBand, Card>(CollectionSuppliers.<Card>arrayLists());

    private final HashMap<Card, Integer> defendingDamageMap = new HashMap<Card, Integer>();
    
    
    private Map<Card, List<Card>> attackersOrderedForDamageAssignment = new HashMap<Card, List<Card>>();
    private Map<Card, List<Card>> blockersOrderedForDamageAssignment = new HashMap<Card, List<Card>>();
    private Map<GameEntity, CombatLki> lkiCache = new HashMap<GameEntity, CombatLki>();


    public Combat(Player attacker) {
        playerWhoAttacks = attacker;

        // Create keys for all possible attack targets
        for (Player defender : playerWhoAttacks.getOpponents()) {
            this.attackedEntities.ensureCollectionFor(defender);
            List<Card> planeswalkers = CardLists.filter(defender.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.PLANEWALKERS);
            for (final Card pw : planeswalkers) {
                this.attackedEntities.ensureCollectionFor(pw);
            }
        }
    }
    
    public final Player getAttackingPlayer() {
        return this.playerWhoAttacks;
    }

    public final List<GameEntity> getDefenders() {
        return Lists.newArrayList(attackedEntities.keySet());
    }

    public final List<Card> getDefendingPlaneswalkers() {
        final List<Card> pwDefending = new ArrayList<Card>();
        for (final GameEntity o : attackedEntities.keySet()) {
            if (o instanceof Card) {
                pwDefending.add((Card) o);
            }
        }
        return pwDefending;
    }

    public final List<AttackingBand> getAttackingBandsOf(GameEntity defender) {
        return Lists.newArrayList(attackedEntities.get(defender));
    }
    
    public final List<Card> getAttackersOf(GameEntity defender) {
        List<Card> result = new ArrayList<Card>();
        for(AttackingBand v : attackedEntities.get(defender)) {
            result.addAll(v.getAttackers());
        }
        return result;
    }

    public final void addAttacker(final Card c, GameEntity defender) {
        addAttacker(c, defender, null);
    }
    
    public final void addAttacker(final Card c, GameEntity defender, AttackingBand band) {
        Collection<AttackingBand> attackersOfDefender = attackedEntities.get(defender);
        if (attackersOfDefender == null) {
            System.out.println("Trying to add Attacker " + c + " to missing defender " + defender);
            return;
        }

        if (band == null || !attackersOfDefender.contains(band)) {
            band = new AttackingBand(c, defender);
            attackersOfDefender.add(band);
        } else {
            band.addAttacker(c);
        }
    }

    public final GameEntity getDefenderByAttacker(final Card c) {
        return getDefenderByAttacker(getBandOfAttacker(c));
    }
    
    public final GameEntity getDefenderByAttacker(final AttackingBand c) {
        for(Entry<GameEntity, Collection<AttackingBand>> e : attackedEntities.entrySet()) {
            for(AttackingBand ab : e.getValue()) {
                if ( ab == c )
                    return e.getKey();
            }
        }
        return null;
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

    public final AttackingBand getBandOfAttacker(final Card c) {
        if ( !c.isInPlay() ) {
            CombatLki lki = lkiCache.get(c); 
            return lki == null ? null : lki.getFirstBand();
        }
        for(Collection<AttackingBand> abs : attackedEntities.values()) {
            for(AttackingBand ab : abs) {
                if ( ab.contains(c) )
                    return ab;
            }
        }
        return null;
    }

    public final List<AttackingBand> getAttackingBands() {
        List<AttackingBand> result = Lists.newArrayList();
        for(Collection<AttackingBand> abs : attackedEntities.values()) 
            result.addAll(abs);
        return result;
    } 
    
    public final boolean isAttacking(final Card c) {
        AttackingBand ab = getBandOfAttacker(c);
        return ab != null;
    }

    public boolean isAttacking(Card card, GameEntity defender) {
        AttackingBand ab = getBandOfAttacker(card);

        for(Entry<GameEntity, Collection<AttackingBand>> ee : attackedEntities.entrySet()) 
            if ( ee.getValue().contains(ab) )
                return ee.getKey() == defender;
        return false;
    }

    public final List<Card> getAttackers() {
        List<Card> result = Lists.newArrayList();
        for(Collection<AttackingBand> abs : attackedEntities.values()) 
            for(AttackingBand ab : abs)
                result.addAll(ab.getAttackers());
        return result;
    }

    public final List<Card> getBlockers(final Card card) {
        // If requesting the ordered blocking list pass true, directly. 
        AttackingBand band = getBandOfAttacker(card);
        Collection<Card> blockers = blockedBands.get(band);
        return blockers == null ? Lists.<Card>newArrayList() : Lists.newArrayList(blockers);
    }

    public final boolean isBlocked(final Card attacker) {
        AttackingBand band = getBandOfAttacker(attacker);
        return band == null ? false : Boolean.TRUE.equals(band.isBlocked());
        
    }

    // Some cards in Alpha may UNBLOCK an attacker, so second parameter is not always-true
    public final void setBlocked(final Card attacker, boolean value) {
        getBandOfAttacker(attacker).setBlocked(value); // called by Curtain of Light, Dazzling Beauty, Trap Runner
    }

    public final void addBlocker(final Card attacker, final Card blocker) {
        AttackingBand band = getBandOfAttacker(attacker);
        blockedBands.add(band, blocker);
    }

    // remove blocked from specific attacker
    public final void removeBlockAssignment(final Card attacker, final Card blocker) {
        AttackingBand band = getBandOfAttacker(attacker);
        Collection<Card> cc = blockedBands.get(band);
        if( cc != null)
            cc.remove(blocker);
    }

    // remove blocker from everywhere
    public final void undoBlockingAssignment(final Card blocker) {
        for(Collection<Card> blockers : blockedBands.values()) {
            blockers.remove(blocker);
        }
    }

    public final List<Card> getAllBlockers() {
        List<Card> result = new ArrayList<Card>();
        for(Collection<Card> blockers : blockedBands.values()) {
            if(!result.contains(blockers))
                result.addAll(blockers);
        }
        return result;
    }

    public final List<Card> getBlockers(final AttackingBand band) {
        Collection<Card> blockers = blockedBands.get(band);
        return blockers == null ? Lists.<Card>newArrayList() : Lists.newArrayList(blockers);
    }


    public final List<Card> getAttackersBlockedBy(final Card blocker) {
        List<Card> blocked =  new ArrayList<Card>();
        for(Entry<AttackingBand, Collection<Card>> s : blockedBands.entrySet()) {
            if (s.getValue().contains(blocker)) 
                blocked.addAll(s.getKey().getAttackers());
        }
        return blocked;
    }
    
    public final List<AttackingBand> getAttackingBandsBlockedBy(Card blocker) {
        List<AttackingBand> bands = Lists.newArrayList();
        for( Entry<AttackingBand, Collection<Card>> kv : blockedBands.entrySet()) {
            if (kv.getValue().contains(blocker))
                bands.add(kv.getKey());
        }
        return bands;
    }

    public Player getDefendingPlayerRelatedTo(final Card source) {
        Card attacker = source;
        if (source.isAura()) {
            attacker = source.getEnchantingCard();
        } else if (source.isEquipment()) {
            attacker = source.getEquippingCard();
        } else if (source.isFortification()) {
            attacker = source.getFortifyingCard();
        }

        // return the corresponding defender
        return getDefenderPlayerByAttacker(attacker);
    }

    /** If there are multiple blockers, the Attacker declares the Assignment Order */
    public void orderBlockersForDamageAssignment() { // this method performs controller's role 

        for(Collection<AttackingBand> abs : attackedEntities.values())
            for (final AttackingBand band : abs) {
                if (band.isEmpty()) continue;

                Collection<Card> blockers = blockedBands.get(band);
                if ( blockers == null || blockers.isEmpty() )
                    continue;

                for(Card attacker : band.getAttackers()) {
                    List<Card> orderedBlockers = blockers.size() <= 1 
                            ? Lists.newArrayList(blockers) 
                            : playerWhoAttacks.getController().orderBlockers(attacker, (List<Card>)blockers); // we know there's a list
                    // Damage Ordering needs to take cards like Melee into account, is that happening?
                    blockersOrderedForDamageAssignment.put(attacker, orderedBlockers);
                }
            }
    }
    
    public void orderAttackersForDamageAssignment() { // this method performs controller's role
        // If there are multiple blockers, the Attacker declares the Assignment Order
        for (final Card blocker : getAllBlockers()) {
            List<Card> attackers = getAttackersBlockedBy(blocker);
            // They need a reverse map here: Blocker => List<Attacker>
            
            Player blockerCtrl = blocker.getController();
            List<Card> orderedAttacker = attackers.size() <= 1 ? attackers : blockerCtrl.getController().orderAttackers(blocker, attackers);

            // Damage Ordering needs to take cards like Melee into account, is that happening?
            attackersOrderedForDamageAssignment.put(blocker, orderedAttacker);
        }
    }
    
    // removes references to this attacker from all indices and orders
    private void unregisterAttacker(final Card c, AttackingBand ab) {
        blockersOrderedForDamageAssignment.remove(c);
        
        Collection<Card> blockers = blockedBands.get(ab);
        if ( blockers != null ) {
            for (Card b : blockers) {
                // Clear removed attacker from assignment order 
                if (this.attackersOrderedForDamageAssignment.containsKey(b)) {
                    this.attackersOrderedForDamageAssignment.get(b).remove(c);
                }
            }
        }
        return;
    }

    // removes references to this defender from all indices and orders
    private void unregisterDefender(final Card c, AttackingBand bandBeingBlocked) {
        this.attackersOrderedForDamageAssignment.remove(c);
        for(Card atk : bandBeingBlocked.getAttackers()) {
            if (this.blockersOrderedForDamageAssignment.containsKey(atk)) {
                this.blockersOrderedForDamageAssignment.get(atk).remove(c);
            }
        }
    }

    // remove a combatant whose side is unknown
    public final void removeFromCombat(final Card c) {
        AttackingBand ab = getBandOfAttacker(c);
        if (ab != null) {
            unregisterAttacker(c, ab);
            ab.removeAttacker(c);
        }

        // if not found in attackers, look for this card in blockers
        for(Entry<AttackingBand, Collection<Card>> be : blockedBands.entrySet()) {
            Collection<Card> blockers = be.getValue();
            if(blockers.contains(c)) {
                unregisterDefender(c, be.getKey());
                blockers.remove(c);
            }
        }
    } // removeFromCombat()

    public final void removeAbsentCombatants() {
        // iterate all attackers and remove them
        for(Entry<GameEntity, Collection<AttackingBand>> ee : attackedEntities.entrySet()) {
            for(AttackingBand ab : ee.getValue()) {
                List<Card> atk = ab.getAttackers();
                for(int i = atk.size() - 1; i >= 0; i--) { // might remove items from collection, so no iterators
                    Card c = atk.get(i);
                    if ( !c.isInPlay() ) {
                        unregisterAttacker(c, ab);
                    }
                }
            }
        }

        Collection<Card> toRemove = Lists.newArrayList();
        for(Entry<AttackingBand, Collection<Card>> be : blockedBands.entrySet()) {
            toRemove.clear();
            for( Card b : be.getValue()) {
                if ( !b.isInPlay() ) {
                    unregisterDefender(b, be.getKey());
                }
            }
            be.getValue().removeAll(toRemove);
        }
    } // verifyCreaturesInPlay()

    
    // Call this method right after turn-based action of declare blockers has been performed
    public final void onBlockersDeclared() {
        for(Collection<AttackingBand> abs : attackedEntities.values()) {
            for(AttackingBand ab : abs) {
                Collection<Card> blockers = blockedBands.get(ab);
                boolean isBlocked = blockers != null && !blockers.isEmpty();
                ab.setBlocked(isBlocked);

                if (!isBlocked )
                    for (Card attacker : ab.getAttackers()) {
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
                List<Card> attackers = this.attackersOrderedForDamageAssignment.get(blocker);

                final int damage = blocker.getNetCombatDamage();

                if (!attackers.isEmpty()) {
                    Player attackingPlayer = this.getAttackingPlayer();
                    Player assigningPlayer = blocker.getController();

                    if (AttackingBand.isValidBand(attackers, true))
                        assigningPlayer = attackingPlayer;

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
        List<Card> orderedBlockers = null;
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
            
            AttackingBand band = this.getBandOfAttacker(attacker);
            if (band == null) {
                continue;
            }

            boolean trampler = attacker.hasKeyword("Trample");
            orderedBlockers = this.blockersOrderedForDamageAssignment.get(attacker);
            assignedDamage = true;
            // If the Attacker is unblocked, or it's a trampler and has 0 blockers, deal damage to defender
            if (orderedBlockers == null || orderedBlockers.isEmpty()) {
                if (trampler || !band.isBlocked()) { // this is called after declare blockers, no worries 'bout nulls in isBlocked
                    this.addDefendingDamage(damageDealt, attacker);
                } // No damage happens if blocked but no blockers left
            } else {
                GameEntity defender = getDefenderByAttacker(band);
                Player assigningPlayer = this.getAttackingPlayer();
                // Defensive Formation is very similar to Banding with Blockers
                // It allows the defending player to assign damage instead of the attacking player
                if (defender instanceof Player && defender.hasKeyword("You assign combat damage of each creature attacking you.")) {
                    assigningPlayer = (Player)defender;
                } else if ( AttackingBand.isValidBand(orderedBlockers, true)){
                    assigningPlayer = orderedBlockers.get(0).getController();
                }

                Map<Card, Integer> map = assigningPlayer.getController().assignCombatDamage(attacker, orderedBlockers, damageDealt, defender, this.getAttackingPlayer() != assigningPlayer);
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

    // Damage to whatever was protected there. 
    private final void addDefendingDamage(final int n, final Card source) {
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
            GameEntity defender = getDefenderByAttacker(entry.getKey());
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
        AttackingBand band = getBandOfAttacker(att);
        return band == null ? false : Boolean.FALSE.equals(band.isBlocked());
    }

    /**
     * <p>
     * getUnblockedAttackers.
     * </p>
     * 
     * @return an array of {@link forge.Card} objects.
     */
    public final List<Card> getUnblockedAttackers() {
        List<Card> unblocked = new ArrayList<Card>();
        for (Collection<AttackingBand> abs : attackedEntities.values())
            for (AttackingBand ab : abs)
                if ( Boolean.TRUE.equals(ab.isBlocked()) )
                    unblocked.addAll(ab.getAttackers());

        return unblocked;
    }

    public boolean isPlayerAttacked(Player who) {
        for(Entry<GameEntity, Collection<AttackingBand>> ee : attackedEntities.entrySet() ) {
            GameEntity defender = ee.getKey();
            Card defenderAsCard = defender instanceof Card ? (Card)defender : null;
            if ((null != defenderAsCard && defenderAsCard.getController() != who ) || 
                (null == defenderAsCard && defender != who) )
                continue; // defender is not related to player 'who'

            for(AttackingBand ab : ee.getValue()) {
                if ( !ab.isEmpty() )
                    return true;
            }
        }
        return false;
    }

    public boolean isBlocking(Card blocker) {
        if ( !blocker.isInPlay() ) {
            CombatLki lki = lkiCache.get(blocker);
            return null != lki && !lki.isAttacker; // was blocking something anyway
        }
        for (Collection<Card> blockers : blockedBands.values())
            if (blockers.contains(blocker))
                return true;
        return false;
    }

    public boolean isBlocking(Card blocker, Card attacker) {
        AttackingBand ab = getBandOfAttacker(attacker);
        if ( !blocker.isInPlay() ) {
            CombatLki lki = lkiCache.get(blocker);
            return null != lki && !lki.isAttacker && lki.relatedBands.contains(ab); // was blocking that very band
        }
        Collection<Card> blockers = blockedBands.get(ab);
        return blockers != null && blockers.contains(blocker);
    }

    /**
     * TODO: Write javadoc for this method.
     * @param lastKnownInfo
     */
    public void saveLKI(Card lastKnownInfo) {
        List<AttackingBand> attackersBlocked = null;
        AttackingBand attackingBand = getBandOfAttacker(lastKnownInfo);
        boolean isAttacker = attackingBand != null;
        if ( !isAttacker ) {
            attackersBlocked= getAttackingBandsBlockedBy(lastKnownInfo);
            if ( attackersBlocked.isEmpty() )
                return; // card was not even in combat 
        }
        List<AttackingBand> relatedBands = isAttacker ? Lists.newArrayList(attackingBand) : attackersBlocked;
        lkiCache.put(lastKnownInfo, new CombatLki(isAttacker, relatedBands));
    }

} // Class Combat
