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

import com.google.common.base.Function;
import com.google.common.collect.*;
import forge.game.*;
import forge.game.ability.AbilityKey;
import forge.game.ability.ApiType;
import forge.game.card.*;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.staticability.StaticAbilityAssignCombatDamageAsUnblocked;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.CardTranslation;
import forge.util.Localizer;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.Map.Entry;

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
    private AttackConstraints attackConstraints;
    // Defenders, as they are attacked by hostile forces
    private final FCollection<GameEntity> attackableEntries = new FCollection<>();

    // Keyed by attackable defender (player or planeswalker or battle)
    private final Multimap<GameEntity, AttackingBand> attackedByBands = Multimaps.synchronizedMultimap(ArrayListMultimap.create());
    private final Multimap<AttackingBand, Card> blockedBands = Multimaps.synchronizedMultimap(ArrayListMultimap.create());

    private Map<Card, CardCollection> attackersOrderedForDamageAssignment = Maps.newHashMap();
    private Map<Card, CardCollection> blockersOrderedForDamageAssignment = Maps.newHashMap();
    private CardCollection lkiCache = new CardCollection();
    private CardDamageMap damageMap = new CardDamageMap();

    // List holds creatures who have dealt 1st strike damage to disallow them deal damage on regular basis (unless they have double-strike KW)
    private CardCollection combatantsThatDealtFirstStrikeDamage = new CardCollection();

    public Combat(final Player attacker) {
        playerWhoAttacks = attacker;
        initConstraints();
    }

    public Combat(Combat combat, GameObjectMap map) {
        playerWhoAttacks = map.map(combat.playerWhoAttacks);
        for (GameEntity entry : combat.attackableEntries) {
            attackableEntries.add(map.map(entry));
        }

        HashMap<AttackingBand, AttackingBand> bandsMap = new HashMap<>();
        for (Entry<GameEntity, AttackingBand> entry : combat.attackedByBands.entries()) {
            AttackingBand origBand = entry.getValue();
            ArrayList<Card> attackers = new ArrayList<>();
            for (Card c : origBand.getAttackers()) {
                attackers.add(map.map(c));
            }
            AttackingBand newBand = new AttackingBand(attackers);
            Boolean blocked = entry.getValue().isBlocked();
            if (blocked != null) {
                newBand.setBlocked(blocked);
            }
            bandsMap.put(origBand, newBand);
            attackedByBands.put(map.map(entry.getKey()), newBand);
        }
        for (Entry<AttackingBand, Card> entry : combat.blockedBands.entries()) {
            blockedBands.put(bandsMap.get(entry.getKey()), map.map(entry.getValue()));
        }

        for (Entry<Card, CardCollection> entry : combat.attackersOrderedForDamageAssignment.entrySet()) {
            attackersOrderedForDamageAssignment.put(map.map(entry.getKey()), map.mapCollection(entry.getValue()));
        }
        for (Entry<Card, CardCollection> entry : combat.blockersOrderedForDamageAssignment.entrySet()) {
            blockersOrderedForDamageAssignment.put(map.map(entry.getKey()), map.mapCollection(entry.getValue()));
        }
        // Note: Doesn't currently set up lkiCache, since it's just a cache and not strictly needed...
        for (Table.Cell<Card, GameEntity, Integer> entry : combat.damageMap.cellSet()) {
            damageMap.put(map.map(entry.getRowKey()), map.map(entry.getColumnKey()), entry.getValue());
        }

        attackConstraints = new AttackConstraints(this);
    }

    public void initConstraints() {
        attackableEntries.clear();
        // Create keys for all possible attack targets
        attackableEntries.addAll(CombatUtil.getAllPossibleDefenders(playerWhoAttacks));
        attackConstraints = new AttackConstraints(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (GameEntity defender : attackableEntries) {
            CardCollection attackers = getAttackersOf(defender);
            if (attackers.isEmpty()) {
                continue;
            }
            sb.append(defender);
            sb.append(" is being attacked by:\n");
            for (Card attacker : attackers) {
                sb.append("  ").append(attacker).append("\n");
                for (Card blocker : getBlockers(attacker)) {
                    sb.append("  ... blocked by: ").append(blocker).append("\n");
                }
            }
        }
        if (sb.length() == 0) {
            return "<no attacks>";
        }
        return sb.toString();
    }

    public void endCombat() {
        //backup attackers and blockers
        CardCollection attackers = getAttackers();
        CardCollection blockers = getAllBlockers();

        //clear all combat-related collections
        attackableEntries.clear();
        attackedByBands.clear();
        blockedBands.clear();
        attackersOrderedForDamageAssignment.clear();
        blockersOrderedForDamageAssignment.clear();
        lkiCache.clear();
        combatantsThatDealtFirstStrikeDamage.clear();

        //clear tracking for cards that care about "this combat"
        Game game = playerWhoAttacks.getGame();
        for (Card c : game.getCardsIncludePhasingIn(ZoneType.Battlefield)) {
            c.getDamageHistory().endCombat();
        }
        playerWhoAttacks.clearAttackedPlayersMyCombat();

        //update view for all attackers and blockers
        for (Card c : attackers) {
            c.updateAttackingForView();
        }
        for (Card c : blockers) {
            c.updateBlockingForView();
        }
    }

    public final void clearAttackers() {
        for (final Card attacker : getAttackers()) {
            removeFromCombat(attacker);
        }
    }

    public final Player getAttackingPlayer() {
        return playerWhoAttacks;
    }

    public final AttackConstraints getAttackConstraints() {
        return attackConstraints;
    }
    public final FCollectionView<GameEntity> getDefenders() {
        return attackableEntries;
    }

    //gets attacked player opponents (ignores planeswalkers)
    public final FCollection<Player> getAttackedOpponents(Player atk) {
        FCollection<Player> attackedOpps = new FCollection<>();
        if (atk == playerWhoAttacks) {
            for (Player defender : getDefendingPlayers()) {
                if (!getAttackersOf(defender).isEmpty()) {
                    attackedOpps.add(defender);
                }
            }
        }
        return attackedOpps;
    }

    public final FCollection<GameEntity> getDefendersControlledBy(Player who) {
        FCollection<GameEntity> res = new FCollection<>();
        for (GameEntity ge : attackableEntries) {
            // if defender is the player himself or his cards
            if (ge == who || ge instanceof Card && ((Card) ge).getController() == who) {
                res.add(ge);
            }
        }
        return res;
    }

    public final FCollectionView<Player> getDefendingPlayers() {
        return new FCollection<>(Iterables.filter(attackableEntries, Player.class));
    }

    public final CardCollection getDefendingPlaneswalkers() {
        return CardLists.filter(Iterables.filter(attackableEntries, Card.class), CardPredicates.isType("Planeswalker"));
    }

    public final CardCollection getDefendingBattles() {
        return CardLists.filter(Iterables.filter(attackableEntries, Card.class), CardPredicates.isType("Battle"));
    }

    public final Map<Card, GameEntity> getAttackersAndDefenders() {
        return Maps.asMap(getAttackers().asSet(), new Function<Card, GameEntity>() {
            @Override
            public GameEntity apply(final Card attacker) {
                return getDefenderByAttacker(attacker);
            }
        });
    }

    public final List<AttackingBand> getAttackingBandsOf(GameEntity defender) {
        return Lists.newArrayList(attackedByBands.get(defender));
    }

    public final CardCollection getAttackersOf(GameEntity defender) {
        CardCollection result = new CardCollection();
        if (!attackedByBands.containsKey(defender))
            return result;
        for (AttackingBand v : attackedByBands.get(defender)) {
            result.addAll(v.getAttackers());
        }
        return result;
    }

    public final void addAttacker(final Card c, GameEntity defender) {
        addAttacker(c, defender, null);
    }
    public final void addAttacker(final Card c, GameEntity defender, AttackingBand band) {
        Collection<AttackingBand> attackersOfDefender = attackedByBands.get(defender);
        if (attackersOfDefender == null) {
            System.out.println("Trying to add Attacker " + c + " to missing defender " + defender);
            return;
        }

        // This is trying to fix the issue of an attacker existing in two bands at once
        AttackingBand existingBand = getBandOfAttacker(c);
        if (existingBand != null) {
            existingBand.removeAttacker(c);
        }

        if (band == null || !attackersOfDefender.contains(band)) {
            band = new AttackingBand(c);
            attackersOfDefender.add(band);
        } else {
            band.addAttacker(c);
        }
        c.updateAttackingForView();
    }

    public final GameEntity getDefenderByAttacker(final Card c) {
        return getDefenderByAttacker(getBandOfAttacker(c));
    }
    public final GameEntity getDefenderByAttacker(final AttackingBand c) {
        for (Entry<GameEntity, AttackingBand> e : attackedByBands.entries()) {
            if (e.getValue() == c) {
                return e.getKey();
            }
        }
        return null;
    }

    public final Player getDefenderPlayerByAttacker(final Card c) {
        GameEntity defender = getDefenderByAttacker(c);

        if (defender instanceof Player) {
            return (Player) defender;
        }

        // maybe attack on a controlled planeswalker?
        if (defender instanceof Card) {
            Card def = (Card)defender;
            if (def.isBattle()) {
                return def.getProtectingPlayer();
            } else {
                return def.getController();
            }
        }

        return null;
    }

    // takes LKI into consideration, should use it at all times (though a single iteration over multimap seems faster)
    public final AttackingBand getBandOfAttacker(final Card c) {
        if (c == null) {
            return null;
        }
        for (AttackingBand ab : attackedByBands.values()) {
            if (ab.contains(c)) {
                return ab;
            }
        }
        CombatLki lki = lkiCache.get(c).getCombatLKI();
        return lki == null || !lki.isAttacker ? null : lki.getFirstBand();
    }

    public final AttackingBand getBandOfAttackerNotNull(final Card c) {
        AttackingBand band = getBandOfAttacker(c);
        if (band == null) {
            throw new NullPointerException("No band for attacker " + c);
        }
        return band;
    }

    public final List<AttackingBand> getAttackingBands() {
        return Lists.newArrayList(attackedByBands.values());
    }

    public boolean isAttacking(Card card, GameEntity defender) {
        AttackingBand ab = getBandOfAttacker(card);
        for (Entry<GameEntity, AttackingBand> ee : attackedByBands.entries()) {
            if (ee.getValue() == ab) {
                return ee.getKey() == defender;
            }
        }
        return false;
    }

    /**
     * Checks if a card is currently attacking, returns false if the card is not currently attacking, even if its LKI was.
     */
    public final boolean isAttacking(Card card) {
        for (AttackingBand ab : attackedByBands.values()) {
            if (ab.contains(card)) {
                return true;
            }
        }
        return false;
    }

    public final CardCollection getAttackers() {
        CardCollection result = new CardCollection();
        for (AttackingBand ab : attackedByBands.values()) {
            result.addAll(ab.getAttackers());
        }
        return result;
    }

    public final boolean isBlocked(final Card attacker) {
        AttackingBand band = getBandOfAttacker(attacker);
        return band != null && Boolean.TRUE.equals(band.isBlocked());
    }

    // Some cards in Alpha may UNBLOCK an attacker, so second parameter is not always-true
    public final void setBlocked(final Card attacker, boolean value) {
        getBandOfAttackerNotNull(attacker).setBlocked(value); // called by Curtain of Light, Dazzling Beauty, Trap Runner
    }

    public final void addBlocker(final Card attacker, final Card blocker) {
        final AttackingBand band = getBandOfAttackerNotNull(attacker);
        blockedBands.put(band, blocker);
        // If damage is already assigned, add this blocker as a "late entry"
        if (blockersOrderedForDamageAssignment.containsKey(attacker)) {
            addBlockerToDamageAssignmentOrder(attacker, blocker);
        }
        blocker.updateBlockingForView();
    }

    // remove blocker from specific attacker
    public final void removeBlockAssignment(final Card attacker, final Card blocker) {
        AttackingBand band = getBandOfAttackerNotNull(attacker);
        Collection<Card> cc = blockedBands.get(band);
        if (cc != null) {
            cc.remove(blocker);
        }
        blocker.updateBlockingForView();
    }

    // remove blocker from everywhere
    public final void undoBlockingAssignment(final Card blocker) {
        CardCollection toRemove = new CardCollection(blocker);
        blockedBands.values().removeAll(toRemove);
        blocker.updateBlockingForView();
    }

    public final CardCollection getAllBlockers() {
        CardCollection result = new CardCollection();
        for (Card blocker : blockedBands.values()) {
            if (!result.contains(blocker)) {
                result.add(blocker);
            }
        }
        return result;
    }

    public final CardCollection getDefendersCreatures() {
        CardCollection result = new CardCollection();
        for (Card attacker : getAttackers()) {
            CardCollection cc = getDefenderPlayerByAttacker(attacker).getCreaturesInPlay();
            result.addAll(cc);
        }
        return result;
    }

    public final CardCollection getBlockers(final Card card) {
        // If requesting the ordered blocking list pass true, directly.
        return getBlockers(getBandOfAttacker(card));
    }
    public final CardCollection getBlockers(final AttackingBand band) {
        Collection<Card> blockers = blockedBands.get(band);
        return blockers == null ? new CardCollection() : new CardCollection(blockers);
    }

    public final CardCollection getAttackersBlockedBy(final Card blocker) {
        CardCollection blocked =  new CardCollection();
        for (Entry<AttackingBand, Card> s : blockedBands.entries()) {
            if (s.getValue().equals(blocker)) {
                blocked.addAll(s.getKey().getAttackers());
            }
        }
        return blocked;
    }

    public final FCollectionView<AttackingBand> getAttackingBandsBlockedBy(Card blocker) {
        FCollection<AttackingBand> bands = new FCollection<>();
        for (Entry<AttackingBand, Card> kv : blockedBands.entries()) {
            if (kv.getValue().equals(blocker)) {
                bands.add(kv.getKey());
            }
        }
        return bands;
    }

    public Player getDefendingPlayerRelatedTo(final Card source) {
        Card attacker = source;
        if (source.isAura() || source.isFortification()) {
            attacker = source.getEnchantingCard();
        }
        else if (source.isEquipment()) {
            attacker = source.getEquipping();
        }

        // return the corresponding defender
        return getDefenderPlayerByAttacker(attacker);
    }

    /** If there are multiple blockers, the Attacker declares the Assignment Order */
    public void orderBlockersForDamageAssignment() { // this method performs controller's role
        List<Pair<Card, CardCollection>> blockersNeedManualOrdering = new ArrayList<>();
        for (AttackingBand band : attackedByBands.values()) {
            if (band.isEmpty()) continue;

            Collection<Card> blockers = blockedBands.get(band);
            if (blockers == null || blockers.isEmpty()) {
                continue;
            }

            for (Card attacker : band.getAttackers()) {
                if (blockers.size() <= 1) {
                    orderBlockersForDamageAssignment(attacker, new CardCollection(blockers));
                }
                else { // process it a bit later
                    blockersNeedManualOrdering.add(Pair.of(attacker, new CardCollection(blockers))); // we know there's a list
                }
            }
        }

        // brought this out of iteration on bands to avoid concurrency problems
        for (Pair<Card, CardCollection> pair : blockersNeedManualOrdering) {
            orderBlockersForDamageAssignment(pair.getLeft(), pair.getRight());
        }
    }

    /** If there are multiple blockers, the Attacker declares the Assignment Order */
    public void orderBlockersForDamageAssignment(Card attacker, CardCollection blockers) { // this method performs controller's role
        if (blockers.size() <= 1) {
            blockersOrderedForDamageAssignment.put(attacker, new CardCollection(blockers));
            return;
        }

        // Damage Ordering needs to take cards like Melee into account, is that happening?
        CardCollection orderedBlockers = playerWhoAttacks.getController().orderBlockers(attacker, blockers); // we know there's a list
        blockersOrderedForDamageAssignment.put(attacker, orderedBlockers);

        // Display the chosen order of blockers in the log
        // TODO: this is best done via a combat panel update
        StringBuilder sb = new StringBuilder();
        sb.append(playerWhoAttacks.getName());
        sb.append(" has ordered blockers for ");
        sb.append(attacker);
        sb.append(": ");
        for (int i = 0; i < orderedBlockers.size(); i++) {
            sb.append(orderedBlockers.get(i));
            if (i != orderedBlockers.size() - 1) {
                sb.append(", ");
            }
        }
        playerWhoAttacks.getGame().getGameLog().add(GameLogEntryType.COMBAT, sb.toString());
    }

    /**
     * Add a blocker to the damage assignment order of an attacker. The
     * relative order of creatures already blocking the attacker may not be
     * changed. Performs controller's role.
     *
     * @param attacker the attacking creature.
     * @param blocker the blocking creature.
     */
    public void addBlockerToDamageAssignmentOrder(Card attacker, Card blocker) {
        final CardCollection oldBlockers = blockersOrderedForDamageAssignment.get(attacker);
    	if (oldBlockers == null || oldBlockers.isEmpty()) {
   			blockersOrderedForDamageAssignment.put(attacker, new CardCollection(blocker));
    	} else {
    		CardCollection orderedBlockers = playerWhoAttacks.getController().orderBlocker(attacker, blocker, oldBlockers);
            blockersOrderedForDamageAssignment.put(attacker, orderedBlockers);
    	}
    }

    public void orderAttackersForDamageAssignment() { // this method performs controller's role
        // If there are multiple blockers, the Attacker declares the Assignment Order
        for (final Card blocker : getAllBlockers()) {
            orderAttackersForDamageAssignment(blocker);
        }
    }

    public void orderAttackersForDamageAssignment(Card blocker) { // this method performs controller's role
        CardCollection attackers = getAttackersBlockedBy(blocker);
        // They need a reverse map here: Blocker => List<Attacker>

        Player blockerCtrl = blocker.getController();
        CardCollection orderedAttacker = attackers.size() <= 1 ? attackers : blockerCtrl.getController().orderAttackers(blocker, attackers);

        // Damage Ordering needs to take cards like Melee into account, is that happening?
        attackersOrderedForDamageAssignment.put(blocker, orderedAttacker);
    }

    // removes references to this attacker from all indices and orders
    public void unregisterAttacker(final Card c, AttackingBand ab) {
        blockersOrderedForDamageAssignment.remove(c);

        Collection<Card> blockers = blockedBands.get(ab);
        if (blockers != null) {
            for (Card b : blockers) {
                // Clear removed attacker from assignment order
                if (attackersOrderedForDamageAssignment.containsKey(b)) {
                    attackersOrderedForDamageAssignment.get(b).remove(c);
                }
            }
        }

        // restore the original defender in case it was changed before the creature was
        // removed from combat but before the trigger resolved (e.g. Ulamog, the Ceaseless
        // Hunger + Portal Mage + Unsummon)
        Game game = c.getGame();
        for (SpellAbilityStackInstance si : game.getStack()) {
            if (si.isTrigger() && c.equals(si.getSourceCard())) {
                GameEntity origDefender = (GameEntity)si.getTriggeringObject(AbilityKey.OriginalDefender);
                if (origDefender != null) {
                    si.updateTriggeringObject(AbilityKey.Defender, origDefender);
                    if (origDefender instanceof Player) {
                        si.updateTriggeringObject(AbilityKey.DefendingPlayer, origDefender);
                    } else if (origDefender instanceof Card) {
                        si.updateTriggeringObject(AbilityKey.DefendingPlayer, ((Card)origDefender).getController());
                    }
                }
            }
        }
    }

    // removes references to this defender from all indices and orders
    public void unregisterDefender(final Card c, AttackingBand bandBeingBlocked) {
        attackersOrderedForDamageAssignment.remove(c);
        for (Card atk : bandBeingBlocked.getAttackers()) {
            if (blockersOrderedForDamageAssignment.containsKey(atk)) {
                blockersOrderedForDamageAssignment.get(atk).remove(c);
            }
        }
    }

    // remove a combatant whose side is unknown
    public final void removeFromCombat(final Card c) {
        AttackingBand ab = getBandOfAttacker(c);
        if (ab != null) {
            unregisterAttacker(c, ab);
            ab.removeAttacker(c);
            c.updateAttackingForView();
            return;
        }

        // if not found in attackers, look for this card in blockers
        for (Entry<AttackingBand, Card> be : blockedBands.entries()) {
            if (be.getValue().equals(c)) {
                unregisterDefender(c, be.getKey());
            }
        }

        for (Card battleOrPW : Iterables.filter(attackableEntries, Card.class)) {
            if (battleOrPW.equals(c)) {
                Multimap<GameEntity, AttackingBand> attackerBuffer = ArrayListMultimap.create();
                Collection<AttackingBand> bands = attackedByBands.get(c);
                for (AttackingBand abDef : bands) {
                    unregisterDefender(c, abDef);
                    // Rule 506.4c workaround to keep creatures in combat
                    Card fake = new Card(-1, c.getGame());
                    fake.setName("<Nothing>");
                    fake.setController(c.getController(), 0);
                    attackerBuffer.put(fake, abDef);
                }
                bands.clear();
                attackedByBands.putAll(attackerBuffer);
                break;
            }
        }

        // remove card from map
        while (blockedBands.values().remove(c));
        c.updateBlockingForView();
    }

    public final boolean removeAbsentCombatants() {
        // iterate all attackers and remove illegal declarations
        CardCollection missingCombatants = new CardCollection();
        for (Entry<GameEntity, AttackingBand> ee : attackedByBands.entries()) {
            for (Card c : ee.getValue().getAttackers()) {
                if (!c.isInPlay() || !c.isCreature()) {
                    missingCombatants.add(c);
                }
            }
        }

        for (Entry<AttackingBand, Card> be : blockedBands.entries()) {
            Card blocker = be.getValue();
            if (!blocker.isInPlay() || !blocker.isCreature()) {
                missingCombatants.add(blocker);
            }
        }

        if (missingCombatants.isEmpty()) { return false; }

        for (Card c : missingCombatants) {
            removeFromCombat(c);
        }
        return true;
    }

    // Call this method right after turn-based action of declare blockers has been performed
    public final void fireTriggersForUnblockedAttackers(final Game game) {
        boolean bFlag = false;
        List<GameEntity> defenders = Lists.newArrayList();
        for (AttackingBand ab : attackedByBands.values()) {
            Collection<Card> blockers = blockedBands.get(ab);
            boolean isBlocked = blockers != null && !blockers.isEmpty();
            ab.setBlocked(isBlocked);

            if (!isBlocked) {
                bFlag = true;
                defenders.add(getDefenderByAttacker(ab));
                for (Card attacker : ab.getAttackers()) {
                    // Run Unblocked Trigger
                    final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
                    runParams.put(AbilityKey.Attacker, attacker);
                    runParams.put(AbilityKey.Defender, getDefenderByAttacker(attacker));
                    runParams.put(AbilityKey.DefendingPlayer, getDefenderPlayerByAttacker(attacker));
                    game.getTriggerHandler().runTrigger(TriggerType.AttackerUnblocked, runParams, false);
                }
            }
        }
        if (bFlag) {
            // triggers for Coveted Jewel
            // currently there is only one attacking player
            // should be updated when two-headed-giant is done
            final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            runParams.put(AbilityKey.AttackingPlayer, getAttackingPlayer());
            runParams.put(AbilityKey.Defenders, defenders);
            game.getTriggerHandler().runTrigger(TriggerType.AttackerUnblockedOnce, runParams, false);
        }
    }

    private final boolean assignBlockersDamage(boolean firstStrikeDamage) {
        // Assign damage by Blockers
        final CardCollection blockers = getAllBlockers();
        boolean assignedDamage = false;

        for (final Card blocker : blockers) {
            if (!dealDamageThisPhase(blocker, firstStrikeDamage)) {
                continue;
            }

            if (firstStrikeDamage) {
                combatantsThatDealtFirstStrikeDamage.add(blocker);
            }

            // Run replacement effects
            blocker.getGame().getReplacementHandler().run(ReplacementType.AssignDealDamage, AbilityKey.mapFromAffected(blocker));

            CardCollection attackers = attackersOrderedForDamageAssignment.get(blocker);

            final int damage = blocker.getNetCombatDamage();

            if (!attackers.isEmpty()) {
                Player attackingPlayer = getAttackingPlayer();
                Player assigningPlayer = blocker.getController();

                Player defender = null;
                boolean divideCombatDamageAsChoose = blocker.hasKeyword("You may assign CARDNAME's combat damage divided as you choose among " +
                                "defending player and/or any number of creatures they control.")
                        && blocker.getController().getController().confirmStaticApplication(blocker, PlayerActionConfirmMode.AlternativeDamageAssignment,
                        Localizer.getInstance().getMessage("lblAssignCombatDamageAsChoose",
                                CardTranslation.getTranslatedName(blocker.getName())), null);
                // choose defending player
                if (divideCombatDamageAsChoose) {
                    defender = blocker.getController().getController().chooseSingleEntityForEffect(attackingPlayer.getOpponents(), null, Localizer.getInstance().getMessage("lblChoosePlayer"), null);
                    attackers = defender.getCreaturesInPlay();
                }

                if (AttackingBand.isValidBand(attackers, true))
                    assigningPlayer = attackingPlayer;

                assignedDamage = true;
                Map<Card, Integer> map = assigningPlayer.getController().assignCombatDamage(blocker, attackers, null, damage, defender, divideCombatDamageAsChoose || assigningPlayer != blocker.getController());
                for (Entry<Card, Integer> dt : map.entrySet()) {
                    // Butcher Orgg
                    if (dt.getKey() == null && dt.getValue() > 0) {
                        damageMap.put(blocker, defender, dt.getValue());
                    } else {
                        dt.getKey().addAssignedDamage(dt.getValue(), blocker);
                        damageMap.put(blocker, dt.getKey(), dt.getValue());
                    }
                }
            }
        }
        return assignedDamage;
    }

    private final boolean assignAttackersDamage(boolean firstStrikeDamage) {
        // Assign damage by Attackers
        CardCollection orderedBlockers = null;
        final CardCollection attackers = getAttackers();
        boolean assignedDamage = false;
        while (!attackers.isEmpty()) {
            final Card attacker = attackers.getFirst();
            if (!dealDamageThisPhase(attacker, firstStrikeDamage)) {
                attackers.remove(attacker);
                continue;
            }

            if (firstStrikeDamage) {
                combatantsThatDealtFirstStrikeDamage.add(attacker);
            }

            // Run replacement effects
            attacker.getGame().getReplacementHandler().run(ReplacementType.AssignDealDamage, AbilityKey.mapFromAffected(attacker));

            // If potential damage is 0, continue along
            final int damageDealt = attacker.getNetCombatDamage();
            if (damageDealt <= 0) {
                attackers.remove(attacker);
                continue;
            }

            AttackingBand band = getBandOfAttacker(attacker);
            if (band == null) {
                attackers.remove(attacker);
                continue;
            }

            GameEntity defender = getDefenderByAttacker(band);
            Player assigningPlayer = getAttackingPlayer();
            orderedBlockers = blockersOrderedForDamageAssignment.get(attacker);
            // Defensive Formation is very similar to Banding with Blockers
            // It allows the defending player to assign damage instead of the attacking player
            if (defender instanceof Player && defender.hasKeyword("You assign combat damage of each creature attacking you.")) {
                assigningPlayer = (Player)defender;
            }
            else if (orderedBlockers != null && AttackingBand.isValidBand(orderedBlockers, true)) {
                assigningPlayer = orderedBlockers.get(0).getController();
            }

            boolean assignToPlayer = false;
            if (StaticAbilityAssignCombatDamageAsUnblocked.assignCombatDamageAsUnblocked(attacker, false)) {
                assignToPlayer = true;
            }
            if (!assignToPlayer && attacker.getGame().getCombat().isBlocked(attacker)
                    && StaticAbilityAssignCombatDamageAsUnblocked.assignCombatDamageAsUnblocked(attacker)) {
                assignToPlayer = assigningPlayer.getController().confirmStaticApplication(attacker, PlayerActionConfirmMode.AlternativeDamageAssignment,
                        Localizer.getInstance().getMessage("lblAssignCombatDamageWerentBlocked",
                                CardTranslation.getTranslatedName(attacker.getName())), null);
            }

            boolean divideCombatDamageAsChoose = false;
            boolean assignCombatDamageToCreature = false;
            boolean trampler = attacker.hasKeyword(Keyword.TRAMPLE);
            if (!assignToPlayer) {
                divideCombatDamageAsChoose = getDefendersCreatures().size() > 0 &&
                        attacker.hasKeyword("You may assign CARDNAME's combat damage divided as you choose among " +
                                "defending player and/or any number of creatures they control.")
                        && assigningPlayer.getController().confirmStaticApplication(attacker, PlayerActionConfirmMode.AlternativeDamageAssignment,
                                Localizer.getInstance().getMessage("lblAssignCombatDamageAsChoose",
                                        CardTranslation.getTranslatedName(attacker.getName())), null);
                if (defender instanceof Card && divideCombatDamageAsChoose) {
                    defender = getDefenderPlayerByAttacker(attacker);
                }

                assignCombatDamageToCreature = !attacker.getGame().getCombat().isBlocked(attacker) && getDefendersCreatures().size() > 0 &&
                        attacker.hasKeyword("If CARDNAME is unblocked, you may have it assign its combat damage to a creature defending player controls.") &&
                        assigningPlayer.getController().confirmStaticApplication(attacker, PlayerActionConfirmMode.AlternativeDamageAssignment,
                                Localizer.getInstance().getMessage("lblAssignCombatDamageToCreature", CardTranslation.getTranslatedName(attacker.getName())), null);
                if (divideCombatDamageAsChoose) {
                    if (orderedBlockers == null || orderedBlockers.isEmpty()) {
                        orderedBlockers = getDefendersCreatures();
                    } else {
                        for (Card c : getDefendersCreatures()) {
                            if (!orderedBlockers.contains(c)) {
                                orderedBlockers.add(c);
                            }
                        }
                    }
                }
            }

            assignedDamage = true;
            // If the Attacker is unblocked, or it's a trampler and has 0 blockers, deal damage to defender
            if (defender instanceof Card && !((Card) defender).isBattle() && attacker.hasKeyword("Trample:Planeswalker")) {
                if (orderedBlockers == null || orderedBlockers.isEmpty()) {
                    orderedBlockers = new CardCollection((Card) defender);
                } else {
                    orderedBlockers.add((Card) defender);
                }
                defender = getDefenderPlayerByAttacker(attacker);
            }
            if (assignToPlayer) {
                attackers.remove(attacker);
                damageMap.put(attacker, defender, damageDealt);
            }
            else if (orderedBlockers == null || orderedBlockers.isEmpty()) {
                attackers.remove(attacker);
                if (assignCombatDamageToCreature) {
                    final SpellAbility emptySA = new SpellAbility.EmptySa(ApiType.Cleanup, attacker);
                    Card chosen = attacker.getController().getController().chooseCardsForEffect(getDefendersCreatures(),
                            emptySA, Localizer.getInstance().getMessage("lblChooseCreature"), 1, 1, false, null).get(0);
                    damageMap.put(attacker, chosen, damageDealt);
                } else if (trampler || !band.isBlocked()) { // this is called after declare blockers, no worries 'bout nulls in isBlocked
                    damageMap.put(attacker, defender, damageDealt);
                } // No damage happens if blocked but no blockers left
            } else {
                Map<Card, Integer> map = assigningPlayer.getController().assignCombatDamage(attacker, orderedBlockers, attackers,
                        damageDealt, defender, divideCombatDamageAsChoose || getAttackingPlayer() != assigningPlayer);

                attackers.remove(attacker);
                // player wants to assign another first
                if (map == null) {
                    // add to end
                    attackers.add(attacker);
                    continue;
                }

                for (Entry<Card, Integer> dt : map.entrySet()) {
                    if (dt.getKey() == null) {
                        if (dt.getValue() > 0) {
                            if (defender instanceof Card) {
                                ((Card) defender).addAssignedDamage(dt.getValue(), attacker);
                            }
                            damageMap.put(attacker, defender, dt.getValue());
                        }
                    } else {
                        dt.getKey().addAssignedDamage(dt.getValue(), attacker);
                        damageMap.put(attacker, dt.getKey(), dt.getValue());
                    }
                }
            } // if !hasFirstStrike ...
        } // for
        return assignedDamage;
    }

    private final boolean dealDamageThisPhase(Card combatant, boolean firstStrikeDamage) {
        // During first strike damage, double strike and first strike deal damage
        // During regular strike damage, double strike and anyone who hasn't dealt damage deal damage
        if (combatant.hasDoubleStrike()) {
            return true;
        }
        if (firstStrikeDamage && combatant.hasFirstStrike()) {
            return true;
        }
        return !firstStrikeDamage && !combatantsThatDealtFirstStrikeDamage.contains(combatant);
    }

    public final boolean assignCombatDamage(boolean firstStrikeDamage) {
        boolean assignedDamage = assignAttackersDamage(firstStrikeDamage);
        assignedDamage |= assignBlockersDamage(firstStrikeDamage);
        if (!firstStrikeDamage) {
            // Clear first strike damage list since it doesn't matter anymore
            combatantsThatDealtFirstStrikeDamage.clear();
        }
        return assignedDamage;
    }

    public void dealAssignedDamage() {
        final Game game = playerWhoAttacks.getGame();
        game.copyLastState();

        CardDamageMap preventMap = new CardDamageMap();
        GameEntityCounterTable counterTable = new GameEntityCounterTable();

        game.getAction().dealDamage(true, damageMap, preventMap, counterTable, null);

        // copy last state again for dying replacement effects
        game.copyLastState();
    }

    public final boolean isUnblocked(final Card att) {
        AttackingBand band = getBandOfAttacker(att);
        return band != null && Boolean.FALSE.equals(band.isBlocked());
    }

    public final CardCollection getUnblockedAttackers() {
        CardCollection unblocked = new CardCollection();
        for (AttackingBand ab : attackedByBands.values()) {
            if (Boolean.FALSE.equals(ab.isBlocked())) {
                unblocked.addAll(ab.getAttackers());
            }
        }
        return unblocked;
    }

    public boolean isPlayerAttacked(Player who) {
        for (GameEntity defender : attackedByBands.keySet()) {
            Card defenderAsCard = defender instanceof Card ? (Card)defender : null;
            if ((null != defenderAsCard && (defenderAsCard.getController() != who && defenderAsCard.getProtectingPlayer() != who)) ||
                (null == defenderAsCard && defender != who)) {
                continue; // defender is not related to player 'who'
            }
            for (AttackingBand ab : attackedByBands.get(defender)) {
                if (!ab.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isBlocking(Card blocker) {
        if (blockedBands.containsValue(blocker)) {
            return true; // is blocking something at the moment
        }

        if (!blocker.isLKI()) {
            return false;
        }

        CombatLki lki = lkiCache.get(blocker).getCombatLKI();
        return null != lki && !lki.isAttacker; // was blocking something anyway
    }

    public boolean isBlocking(Card blocker, Card attacker) {
        AttackingBand ab = getBandOfAttacker(attacker);
        Collection<Card> blockers = blockedBands.get(ab);
        if (blockers != null && blockers.contains(blocker)) {
            return true; // is blocking the attacker's band at the moment
        }

        if (!blocker.isLKI()) {
            return false;
        }

        CombatLki lki = lkiCache.get(blocker).getCombatLKI();
        return null != lki && !lki.isAttacker && lki.relatedBands.contains(ab); // was blocking that very band
    }

    public CombatLki saveLKI(Card lki) {
        if (!lki.isLKI()) {
            lki = CardUtil.getLKICopy(lki);
        }
        FCollectionView<AttackingBand> attackersBlocked = null;
        final AttackingBand attackingBand = getBandOfAttacker(lki);
        final boolean isAttacker = attackingBand != null;
        if (isAttacker) {
            boolean found = false;
            for (AttackingBand ab : attackedByBands.values()) {
                if (ab.contains(lki)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return null;
            }
        } else {
            attackersBlocked = getAttackingBandsBlockedBy(lki);
            if (attackersBlocked.isEmpty()) {
                return null; // card was not even in combat
            }
        }
        lkiCache.add(lki);
        final FCollectionView<AttackingBand> relatedBands = isAttacker ? new FCollection<>(attackingBand) : attackersBlocked;
        return new CombatLki(isAttacker, relatedBands);
    }
}
