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
package forge.ai;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.game.CardTraitBase;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactory;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardLists;
import forge.game.card.CounterType;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.phase.Untap;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.AbilityActivated;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * <p>
 * ComputerCombatUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id: ComputerUtil.java 19179 2013-01-25 18:48:29Z Max mtg  $
 */
public class ComputerUtilCombat {

    /**
     * <p>
     * canAttackNextTurn.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.game.card.Card} object.
     * @return a boolean.
     */
    public static boolean canAttackNextTurn(final Card attacker) {
        final Iterable<GameEntity> defenders = CombatUtil.getAllPossibleDefenders(attacker.getController());
        return Iterables.any(defenders, new Predicate<GameEntity>() {
            @Override public boolean apply(final GameEntity input) {
                return ComputerUtilCombat.canAttackNextTurn(attacker, input);
            };
        });
    } // canAttackNextTurn(Card)

    /**
     * <p>
     * canAttackNextTurn.
     * </p>
     * 
     * @param atacker
     *            a {@link forge.game.card.Card} object.
     * @param defender
     *            the defending {@link GameEntity}.
     * @return a boolean.
     */
    public static boolean canAttackNextTurn(final Card atacker, final GameEntity defender) {
        if (!atacker.isCreature()) {
            return false;
        }
        if (!CombatUtil.canAttackNextTurn(atacker, defender)) {
            return false;
        }

        for (final String keyword : atacker.getKeywords()) {
            if (keyword.startsWith("CARDNAME attacks specific player each combat if able")) {
                final String defined = keyword.split(":")[1];
                final Player player = AbilityUtils.getDefinedPlayers(atacker, defined, null).get(0);
                if (!defender.equals(player)) {
                    return false;
                }
            }
        }

        // The creature won't untap next turn
        if (atacker.isTapped() && !Untap.canUntap(atacker)) {
            return false;
        }

        return true;
    } // canAttackNextTurn(Card, GameEntity)

    /**
     * <p>
     * getTotalFirstStrikeBlockPower.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.game.card.Card} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a int.
     */
    public static int getTotalFirstStrikeBlockPower(final Card attacker, final Player player) {
        final Card att = attacker;

        List<Card> list = player.getCreaturesInPlay();
        list = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return CombatUtil.canBlock(att, c) && (c.hasFirstStrike() || c.hasDoubleStrike());
            }
        });

        return ComputerUtilCombat.totalDamageOfBlockers(attacker, list);
    }
    

    // This function takes Doran and Double Strike into account
    /**
     * <p>
     * getAttack.
     * </p>
     * 
     * @param c
     *            a {@link forge.game.card.Card} object.
     * @return a int.
     */
    public static int getAttack(final Card c) {
        int n = c.getNetCombatDamage();

        if (c.hasDoubleStrike()) {
            n *= 2;
        }

        return n;
    }
    

    // Returns the damage an unblocked attacker would deal
    /**
     * <p>
     * damageIfUnblocked.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.game.card.Card} object.
     * @param attacked
     *            a {@link forge.game.player.Player} object.
     * @param combat
     *            a {@link forge.game.combat.Combat} object.
     * @return a int.
     */
    public static int damageIfUnblocked(final Card attacker, final Player attacked, final Combat combat, boolean withoutAbilities) {
        int damage = attacker.getNetCombatDamage();
        int sum = 0;
        if (!attacked.canLoseLife()) {
            return 0;
        }
        damage += ComputerUtilCombat.predictPowerBonusOfAttacker(attacker, null, combat, withoutAbilities);
        if (!attacker.hasKeyword("Infect")) {
            sum = ComputerUtilCombat.predictDamageTo(attacked, damage, attacker, true);
            if (attacker.hasKeyword("Double Strike")) {
                sum += ComputerUtilCombat.predictDamageTo(attacked, damage, attacker, true);
            }
        }
        return sum;
    }

    // Returns the poison an unblocked attacker would deal
    /**
     * <p>
     * poisonIfUnblocked.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.game.card.Card} object.
     * @param attacked
     *            a {@link forge.game.player.Player} object.
     * @return a int.
     */
    public static int poisonIfUnblocked(final Card attacker, final Player attacked) {
        int damage = attacker.getNetCombatDamage();
        int poison = 0;
        damage += ComputerUtilCombat.predictPowerBonusOfAttacker(attacker, null, null, false);
        if (attacker.hasKeyword("Infect")) {
            poison += ComputerUtilCombat.predictDamageTo(attacked, damage, attacker, true);
            if (attacker.hasKeyword("Double Strike")) {
                poison += ComputerUtilCombat.predictDamageTo(attacked, damage, attacker, true);
            }
        }
        if (attacker.hasKeyword("Poisonous") && (damage > 0)) {
            poison += attacker.getKeywordMagnitude("Poisonous");
        }
        return poison;
    }

    // Returns the damage unblocked attackers would deal
    /**
     * <p>
     * sumDamageIfUnblocked.
     * </p>
     * 
     * @param attackers
     * @param attacked
     *            a {@link forge.game.player.Player} object.
     * @return a int.
     */
    public static int sumDamageIfUnblocked(final Iterable<Card> attackers, final Player attacked) {
        int sum = 0;
        for (final Card attacker : attackers) {
            sum += ComputerUtilCombat.damageIfUnblocked(attacker, attacked, null, false);
        }
        return sum;
    }

    // Returns the number of poison counters unblocked attackers would deal
    /**
     * <p>
     * sumPoisonIfUnblocked.
     * </p>
     * 
     * @param attackers
     * @param attacked
     *            a {@link forge.game.player.Player} object.
     * @return a int.
     */
    public static int sumPoisonIfUnblocked(final List<Card> attackers, final Player attacked) {
        int sum = 0;
        for (final Card attacker : attackers) {
            sum += ComputerUtilCombat.poisonIfUnblocked(attacker, attacked);
        }
        return sum;
    }

    // calculates the amount of life that will remain after the attack
    /**
     * <p>
     * lifeThatWouldRemain.
     * </p>
     * 
     * @param combat
     *            a {@link forge.game.combat.Combat} object.
     * @return a int.
     */
    public static int lifeThatWouldRemain(final Player ai, final Combat combat) {

        int damage = 0;

        final List<Card> attackers = combat.getAttackersOf(ai);
        final List<Card> unblocked = new ArrayList<Card>();

        for (final Card attacker : attackers) {

            final List<Card> blockers = combat.getBlockers(attacker);

            if ((blockers.size() == 0)
                    || attacker.hasKeyword("You may have CARDNAME assign its combat damage "
                            + "as though it weren't blocked.")) {
                unblocked.add(attacker);
            } else if (attacker.hasKeyword("Trample")
                    && (ComputerUtilCombat.getAttack(attacker) > ComputerUtilCombat.totalShieldDamage(attacker, blockers))) {
                if (!attacker.hasKeyword("Infect")) {
                    damage += ComputerUtilCombat.getAttack(attacker) - ComputerUtilCombat.totalShieldDamage(attacker, blockers);
                }
            }
        }

        damage += ComputerUtilCombat.sumDamageIfUnblocked(unblocked, ai);

        if (!ai.canLoseLife()) {
            damage = 0;
        }

        return ai.getLife() - damage;
    }

    // calculates the amount of poison counters after the attack
    /**
     * <p>
     * resultingPoison.
     * </p>
     * 
     * @param combat
     *            a {@link forge.game.combat.Combat} object.
     * @return a int.
     */
    public static int resultingPoison(final Player ai, final Combat combat) {

        int poison = 0;

        final List<Card> attackers = combat.getAttackersOf(ai);
        final List<Card> unblocked = new ArrayList<Card>();

        for (final Card attacker : attackers) {

            final List<Card> blockers = combat.getBlockers(attacker);

            if ((blockers.size() == 0)
                    || attacker.hasKeyword("You may have CARDNAME assign its combat damage"
                            + " as though it weren't blocked.")) {
                unblocked.add(attacker);
            } else if (attacker.hasKeyword("Trample")
                    && (ComputerUtilCombat.getAttack(attacker) > ComputerUtilCombat.totalShieldDamage(attacker, blockers))) {
                if (attacker.hasKeyword("Infect")) {
                    poison += ComputerUtilCombat.getAttack(attacker) - ComputerUtilCombat.totalShieldDamage(attacker, blockers);
                }
                if (attacker.hasKeyword("Poisonous")) {
                    poison += attacker.getKeywordMagnitude("Poisonous");
                }
            }
        }

        poison += ComputerUtilCombat.sumPoisonIfUnblocked(unblocked, ai);

        return ai.getPoisonCounters() + poison;
    }
    
    public static List<Card> getLifeThreateningCommanders(final Player ai, final Combat combat) {
        List<Card> res = new ArrayList<Card>();
        for (Card c : combat.getAttackers()) {
            if (c.isCommander()) {
                int currentCommanderDamage = ai.getCommanderDamage(c);       
                if (damageIfUnblocked(c, ai, combat, false) + currentCommanderDamage >= 21) {
                    res.add(c);
                }
            }
        }
        return res;
    }

    // Checks if the life of the attacked Player/Planeswalker is in danger
    /**
     * <p>
     * lifeInDanger.
     * </p>
     * 
     * @param combat
     *            a {@link forge.game.combat.Combat} object.
     * @return a boolean.
     */
    public static boolean lifeInDanger(final Player ai, final Combat combat) {
        // life in danger only cares about the player's life. Not Planeswalkers' life
        if (ai.cantLose() || combat == null || combat.getAttackingPlayer() == ai) {
            return false;
        }

        // check for creatures that must be blocked
        final List<Card> attackers = combat.getAttackersOf(ai);
        
        final List<Card> threateningCommanders = getLifeThreateningCommanders(ai,combat);

        for (final Card attacker : attackers) {

            final List<Card> blockers = combat.getBlockers(attacker);           

            if (blockers.isEmpty()) {
                if (!attacker.getSVar("MustBeBlocked").equals("")) {
                    return true;
                }
            }
            if (threateningCommanders.contains(attacker)) {
                return true;
            }
        }

        if (ComputerUtilCombat.lifeThatWouldRemain(ai, combat) < Math.min(4, ai.getLife())
                && !ai.cantLoseForZeroOrLessLife()) {
            return true;
        }

        return (ComputerUtilCombat.resultingPoison(ai, combat) > Math.max(7, ai.getPoisonCounters()));
    }

    // Checks if the life of the attacked Player would be reduced
    /**
     * <p>
     * wouldLoseLife.
     * </p>
     * 
     * @param combat
     *            a {@link forge.game.combat.Combat} object.
     * @return a boolean.
     */
    public static boolean wouldLoseLife(final Player ai, final Combat combat) {

        return (ComputerUtilCombat.lifeThatWouldRemain(ai, combat) < ai.getLife());
    }

    // Checks if the life of the attacked Player/Planeswalker is in danger
    /**
     * <p>
     * lifeInSeriousDanger.
     * </p>
     * 
     * @param combat
     *            a {@link forge.game.combat.Combat} object.
     * @return a boolean.
     */
    public static boolean lifeInSeriousDanger(final Player ai, final Combat combat) {
        // life in danger only cares about the player's life. Not about a
        // Planeswalkers life
        if (ai.cantLose() || combat == null) {
            return false;
        }
        
        final List<Card> threateningCommanders = ComputerUtilCombat.getLifeThreateningCommanders(ai, combat);

        // check for creatures that must be blocked
        final List<Card> attackers = combat.getAttackersOf(ai);

        for (final Card attacker : attackers) {

            final List<Card> blockers = combat.getBlockers(attacker);

            if (blockers.isEmpty()) {
                if (!attacker.getSVar("MustBeBlocked").equals("")) {
                    return true;
                }
            }
            if(threateningCommanders.contains(attacker)) {
                return true;
            }
        }

        if (ComputerUtilCombat.lifeThatWouldRemain(ai, combat) < 1 && !ai.cantLoseForZeroOrLessLife()) {
            return true;
        }

        return (ComputerUtilCombat.resultingPoison(ai, combat) > 9);
    }
    

    // This calculates the amount of damage a blockgang can deal to the attacker
    // (first strike not supported)
    /**
     * <p>
     * totalDamageOfBlockers.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.game.card.Card} object.
     * @param defenders
     * @return a int.
     */
    public static int totalDamageOfBlockers(final Card attacker, final List<Card> defenders) {
        int damage = 0;

        if (attacker.isEquippedBy("Godsend") && !defenders.isEmpty()) {
            defenders.remove(0);
        }

        for (final Card defender : defenders) {
            damage += ComputerUtilCombat.dealsDamageAsBlocker(attacker, defender);
        }
        return damage;
    }
    /**
     * Overload of totalDamageOfBlockers() for first-strike damage only.
     * @param attacker creature to be blocked
     * @param defenders first-strike blockers
     * @return sum of first-strike damage from blockers
     */
    public static int totalFirstStrikeDamageOfBlockers(final Card attacker, final List<Card> defenders) {
        int damage = 0;
        
        if (attacker.isEquippedBy("Godsend") && !defenders.isEmpty()) {
            defenders.remove(0);
        }

        for (final Card defender : defenders) {
            damage += ComputerUtilCombat.predictDamageByBlockerWithoutDoubleStrike(attacker, defender);
        }
        return damage;
    }

    // This calculates the amount of damage a blocker in a blockgang can deal to
    // the attacker
    /**
     * <p>
     * dealsDamageAsBlocker.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.game.card.Card} object.
     * @param defender
     *            a {@link forge.game.card.Card} object.
     * @return a int.
     */
    public static int dealsDamageAsBlocker(final Card attacker, final Card defender) {

        int defenderDamage = predictDamageByBlockerWithoutDoubleStrike(attacker, defender);

        if (defender.hasKeyword("Double Strike")) {
            defenderDamage += predictDamageTo(attacker, defenderDamage, defender, true);
        }

        return defenderDamage;
    }

    /**
     * Predicts the damage to an attacker by a defending creature without double-strike.
     * @param attacker
     * @param defender
     * @return
     */
    private static int predictDamageByBlockerWithoutDoubleStrike(final Card attacker, final Card defender) {
        if (attacker.getName().equals("Sylvan Basilisk") && !defender.hasKeyword("Indestructible")) {
            return 0;
        }

        int flankingMagnitude = 0;
        if (attacker.hasKeyword("Flanking") && !defender.hasKeyword("Flanking")) {

            flankingMagnitude = attacker.getAmountOfKeyword("Flanking");

            if (flankingMagnitude >= defender.getNetToughness()) {
                return 0;
            }
            if ((flankingMagnitude >= (defender.getNetToughness() - defender.getDamage()))
                    && !defender.hasKeyword("Indestructible")) {
                return 0;
            }

        } // flanking
        if (attacker.hasKeyword("Indestructible") && !(defender.hasKeyword("Wither") || defender.hasKeyword("Infect"))) {
            return 0;
        }

        int defenderDamage;
        if (defender.toughnessAssignsDamage()) {
            defenderDamage = defender.getNetToughness() + ComputerUtilCombat.predictToughnessBonusOfBlocker(attacker, defender, true);
        } else {
        	defenderDamage = defender.getNetPower() + ComputerUtilCombat.predictPowerBonusOfBlocker(attacker, defender, true);
        }

        // consider static Damage Prevention
        defenderDamage = predictDamageTo(attacker, defenderDamage, defender, true);
        return defenderDamage;
    }

    // This calculates the amount of damage a blocker in a blockgang can take
    // from the attacker (for trampling attackers)
    /**
     * <p>
     * totalShieldDamage.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.game.card.Card} object.
     * @param defenders
     * @return a int.
     */
    public static int totalShieldDamage(final Card attacker, final List<Card> defenders) {

        int defenderDefense = 0;

        for (final Card defender : defenders) {
            defenderDefense += ComputerUtilCombat.shieldDamage(attacker, defender);
        }

        return defenderDefense;
    }

    // This calculates the amount of damage a blocker in a blockgang can take
    // from the attacker (for trampling attackers)
    /**
     * <p>
     * shieldDamage.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.game.card.Card} object.
     * @param blocker
     *            a {@link forge.game.card.Card} object.
     * @return a int.
     */
    public static int shieldDamage(final Card attacker, final Card blocker) {

        if (ComputerUtilCombat.canDestroyBlockerBeforeFirstStrike(blocker, attacker, false)) {
        	return 0;
        }

        int flankingMagnitude = 0;
        if (attacker.hasKeyword("Flanking") && !blocker.hasKeyword("Flanking")) {

            flankingMagnitude = attacker.getAmountOfKeyword("Flanking");

            if (flankingMagnitude >= blocker.getNetToughness()) {
                return 0;
            }
            if ((flankingMagnitude >= (blocker.getNetToughness() - blocker.getDamage()))
                    && !blocker.hasKeyword("Indestructible")) {
                return 0;
            }

        } // flanking

        final int defBushidoMagnitude = blocker.getKeywordMagnitude("Bushido");

        final int defenderDefense = (blocker.getLethalDamage() - flankingMagnitude) + defBushidoMagnitude;

        return defenderDefense;
    } // shieldDamage

    // For AI safety measures like Regeneration
    /**
     * <p>
     * combatantWouldBeDestroyed.
     * </p>
     * @param ai 
     * 
     * @param combatant
     *            a {@link forge.game.card.Card} object.
     * @return a boolean.
     */
    public static boolean combatantWouldBeDestroyed(Player ai, final Card combatant, Combat combat) {

        if (combat.isAttacking(combatant)) {
            return ComputerUtilCombat.attackerWouldBeDestroyed(ai, combatant, combat);
        }
        if (combat.isBlocking(combatant)) {
            return ComputerUtilCombat.blockerWouldBeDestroyed(ai, combatant, combat);
        }
        return false;
    }

    // For AI safety measures like Regeneration
    /**
     * <p>
     * attackerWouldBeDestroyed.
     * </p>
     * @param ai 
     * 
     * @param attacker
     *            a {@link forge.game.card.Card} object.
     * @return a boolean.
     */
    public static boolean attackerWouldBeDestroyed(Player ai, final Card attacker, Combat combat) {
        final List<Card> blockers = combat.getBlockers(attacker);

        for (final Card defender : blockers) {
            if (ComputerUtilCombat.canDestroyAttacker(ai, attacker, defender, combat, true)
                    && !(defender.hasKeyword("Wither") || defender.hasKeyword("Infect"))) {
                return true;
            }
        }

        return ComputerUtilCombat.totalDamageOfBlockers(attacker, blockers) >= ComputerUtilCombat.getDamageToKill(attacker);
    }

    // Will this trigger trigger?
    /**
     * <p>
     * combatTriggerWillTrigger.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.game.card.Card} object.
     * @param defender
     *            a {@link forge.game.card.Card} object.
     * @param trigger
     *            a {@link forge.game.trigger.Trigger} object.
     * @param combat
     *            a {@link forge.game.combat.Combat} object.
     * @return a boolean.
     */
    public static boolean combatTriggerWillTrigger(final Card attacker, final Card defender, final Trigger trigger,
            Combat combat) {
        final Game game = attacker.getGame();
        final Map<String, String> trigParams = trigger.getMapParams();
        boolean willTrigger = false;
        final Card source = trigger.getHostCard();
        if (combat == null) {
            combat = game.getCombat();
            if (combat == null) {
                return false;
            }
        }

        if (!trigger.zonesCheck(game.getZoneOf(trigger.getHostCard()))) {
            return false;
        }
        if (!trigger.requirementsCheck(game)) {
            return false;
        }

        TriggerType mode = trigger.getMode();
        if (mode == TriggerType.Attacks) {
            willTrigger = true;
            if (combat.isAttacking(attacker)) {
                return false; // The trigger should have triggered already
            }
            if (trigParams.containsKey("ValidCard")) {
                if (!CardTraitBase.matchesValid(attacker, trigParams.get("ValidCard").split(","), source)
                        && !(combat.isAttacking(source) && CardTraitBase.matchesValid(source,
                        trigParams.get("ValidCard").split(","), source)
                            && !trigParams.containsKey("Alone"))) {
                    return false;
                }
            }
            if (trigParams.containsKey("Attacked")) {
            	if (combat.isAttacking(attacker)) {
	            	GameEntity attacked = combat.getDefenderByAttacker(attacker);
	                if (!CardTraitBase.matchesValid(attacked, trigParams.get("Attacked").split(","), source)) {
	                    return false;
	                }
            	} else {
            		if ("You,Planeswalker.YouCtrl".equals(trigParams.get("Attacked"))) {
            			if (source.getController() == attacker.getController()) {
            				return false;
            			}
            		}
            	}
            }
        }

        // defender == null means unblocked
        if ((defender == null) && mode == TriggerType.AttackerUnblocked) {
            willTrigger = true;
            if (trigParams.containsKey("ValidCard")) {
                if (!CardTraitBase.matchesValid(attacker, trigParams.get("ValidCard").split(","), source)) {
                    return false;
                }
            }
        }

        if (defender == null) {
            return willTrigger;
        }

        if (mode == TriggerType.Blocks) {
            willTrigger = true;
            if (trigParams.containsKey("ValidBlocked")) {
                String validBlocked = trigParams.get("ValidBlocked");
                if (validBlocked.contains(".withLesserPower")) {
                    // Have to check this restriction here as triggering objects aren't set yet, so
                    // ValidBlocked$Creature.powerLTX where X:TriggeredBlocker$CardPower crashes with NPE
                    validBlocked = validBlocked.replace(".withLesserPower", "");
                    if (defender.getCurrentPower() <= attacker.getCurrentPower()) {
                        return false;
                    }
                }
                if (!CardTraitBase.matchesValid(attacker, validBlocked.split(","), source)) {
                    return false;
                }
            }
            if (trigParams.containsKey("ValidCard")) {
                String validBlocker = trigParams.get("ValidCard");
                if (validBlocker.contains(".withLesserPower")) {
                    // Have to check this restriction here as triggering objects aren't set yet, so
                    // ValidCard$Creature.powerLTX where X:TriggeredAttacker$CardPower crashes with NPE
                    validBlocker = validBlocker.replace(".withLesserPower", "");
                    if (defender.getCurrentPower() >= attacker.getCurrentPower()) {
                        return false;
                    }
                }
                if (!CardTraitBase.matchesValid(defender, validBlocker.split(","), source)) {
                    return false;
                }
            }
        } else if (mode == TriggerType.AttackerBlocked || mode == TriggerType.AttackerBlockedByCreature) {
            willTrigger = true;
            if (trigParams.containsKey("ValidBlocker")) {
                if (!CardTraitBase.matchesValid(defender, trigParams.get("ValidBlocker").split(","), source)) {
                    return false;
                }
            }
            if (trigParams.containsKey("ValidCard")) {
                if (!CardTraitBase.matchesValid(attacker, trigParams.get("ValidCard").split(","), source)) {
                    return false;
                }
            }
        } else if (mode == TriggerType.DamageDone) {
            willTrigger = true;
            if (trigParams.containsKey("ValidSource")) {
                if (CardTraitBase.matchesValid(defender, trigParams.get("ValidSource").split(","), source)
                        && defender.getNetCombatDamage() > 0
                        && (!trigParams.containsKey("ValidTarget")
                                || CardTraitBase.matchesValid(attacker, trigParams.get("ValidTarget").split(","), source))) {
                    return true;
                }
                if (CardTraitBase.matchesValid(attacker, trigParams.get("ValidSource").split(","), source)
                        && attacker.getNetCombatDamage() > 0
                        && (!trigParams.containsKey("ValidTarget")
                                || CardTraitBase.matchesValid(defender, trigParams.get("ValidTarget").split(","), source))) {
                    return true;
                }
            }
            return false;
        }

        return willTrigger;
    }

    // Predict the Power bonus of the blocker if blocking the attacker
    // (Flanking, Bushido and other triggered abilities)
    /**
     * <p>
     * predictPowerBonusOfBlocker.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.game.card.Card} object.
     * @param blocker
     *            a {@link forge.game.card.Card} object.
     * @return a int.
     */
    public static int predictPowerBonusOfBlocker(final Card attacker, final Card blocker, boolean withoutAbilities) {
        int power = 0;

        if (attacker.hasKeyword("Flanking") && !blocker.hasKeyword("Flanking")) {
            power -= attacker.getAmountOfKeyword("Flanking");
        }
        
        // Serene Master switches power with attacker
        if (blocker.getName().equals("Serene Master")) {
            power += attacker.getNetPower() - blocker.getNetPower();
        } else if (blocker.getName().equals("Shape Stealer")) {
            power += attacker.getNetPower() - blocker.getNetPower();
        } 

        // if the attacker has first strike and wither the blocker will deal
        // less damage than expected
        if (dealsFirstStrikeDamage(attacker, withoutAbilities, null)
                && (attacker.hasKeyword("Wither") || attacker.hasKeyword("Infect"))
                && !dealsFirstStrikeDamage(blocker, withoutAbilities, null)
                && !blocker.hasKeyword("CARDNAME can't have counters placed on it.")) {
            power -= attacker.getNetCombatDamage();
        }

        power += blocker.getKeywordMagnitude("Bushido");

        final Game game = attacker.getGame();
        // look out for continuous static abilities that only care for blocking
        // creatures
        final CardCollectionView cardList = CardCollection.combine(game.getCardsIn(ZoneType.Battlefield), game.getCardsIn(ZoneType.Command));
        for (final Card card : cardList) {
            for (final StaticAbility stAb : card.getStaticAbilities()) {
                final Map<String, String> params = stAb.getMapParams();
                if (!params.get("Mode").equals("Continuous")) {
                    continue;
                }
                if (!params.containsKey("Affected") || !params.get("Affected").contains("blocking")) {
                    continue;
                }
                final String valid = params.get("Affected").replace("blocking", "Creature");
                if (!blocker.isValid(valid, card.getController(), card, null)) {
                    continue;
                }
                if (params.containsKey("AddPower")) {
                    if (params.get("AddPower").equals("X")) {
                        power += CardFactoryUtil.xCount(card, card.getSVar("X"));
                    } else if (params.get("AddPower").equals("Y")) {
                        power += CardFactoryUtil.xCount(card, card.getSVar("Y"));
                    } else {
                        power += Integer.valueOf(params.get("AddPower"));
                    }
                }
            }
        }

        final FCollection<Trigger> theTriggers = new FCollection<Trigger>();
        for (Card card : game.getCardsIn(ZoneType.Battlefield)) {
            theTriggers.addAll(card.getTriggers());
        }
        for (Card card : game.getCardsIn(ZoneType.Command)) {
            theTriggers.addAll(card.getTriggers());
        }
        theTriggers.addAll(attacker.getTriggers());
        for (final Trigger trigger : theTriggers) {
            final Map<String, String> trigParams = trigger.getMapParams();
            final Card source = trigger.getHostCard();

            if (!ComputerUtilCombat.combatTriggerWillTrigger(attacker, blocker, trigger, null)
                    || !trigParams.containsKey("Execute")) {
                continue;
            }
            final String ability = source.getSVar(trigParams.get("Execute"));
            final Map<String, String> abilityParams = AbilityFactory.getMapParams(ability);
            if (abilityParams.containsKey("AB") && !abilityParams.get("AB").equals("Pump")) {
                continue;
            }
            if (abilityParams.containsKey("DB") && !abilityParams.get("DB").equals("Pump")) {
                continue;
            }
            if (abilityParams.containsKey("ValidTgts") || abilityParams.containsKey("Tgt")) {
                continue; // targeted pumping not supported
            }
            final List<Card> list = AbilityUtils.getDefinedCards(source, abilityParams.get("Defined"), null);
            if (abilityParams.containsKey("Defined") && abilityParams.get("Defined").equals("TriggeredBlocker")) {
                list.add(blocker);
            }
            if (list.isEmpty()) {
                continue;
            }
            if (!list.contains(blocker)) {
                continue;
            }
            if (!abilityParams.containsKey("NumAtt")) {
                continue;
            }

            String att = abilityParams.get("NumAtt");
            if (att.startsWith("+")) {
                att = att.substring(1);
            }
            try {
                power += Integer.parseInt(att);
            } catch (final NumberFormatException nfe) {
                // can't parse the number (X for example)
                power += 0;
            }
        }
        if (withoutAbilities) {
            return power;
        }
        for (SpellAbility ability : blocker.getAllSpellAbilities()) {
            if (!(ability instanceof AbilityActivated) || ability.getPayCosts() == null) {
                continue;
            }                
            if (ability.hasParam("ActivationPhases") || ability.hasParam("SorcerySpeed") || ability.hasParam("ActivationZone")) {
                continue;
            }

            if (ability.getApi() == ApiType.Pump) {
                if (!ability.hasParam("NumAtt")) {
                    continue;
                }
    
                if (ComputerUtilCost.canPayCost(ability, blocker.getController())) {
                    int pBonus = AbilityUtils.calculateAmount(ability.getHostCard(), ability.getParam("NumAtt"), ability);
                    if (pBonus > 0) {
                        power += pBonus;
                    }
                }
            } else if (ability.getApi() == ApiType.PutCounter) {
                if (!ability.hasParam("CounterType") || !ability.getParam("CounterType").equals("P1P1")) {
                    continue;
                }
                
                if (ability.hasParam("Monstrosity") && blocker.isMonstrous()) {
                	continue;
                }
                
                if (ComputerUtilCost.canPayCost(ability, blocker.getController())) {
                    int pBonus = AbilityUtils.calculateAmount(ability.getHostCard(), ability.getParam("CounterNum"), ability);
                    if (pBonus > 0) {
                        power += pBonus;
                    }
                }
            }
        }
        
        return power;
    }

    // Predict the Toughness bonus of the blocker if blocking the attacker
    // (Flanking, Bushido and other triggered abilities)
    /**
     * <p>
     * predictToughnessBonusOfBlocker.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.game.card.Card} object.
     * @param blocker
     *            a {@link forge.game.card.Card} object.
     * @return a int.
     */
    public static int predictToughnessBonusOfBlocker(final Card attacker, final Card blocker, boolean withoutAbilities) {
        int toughness = 0;

        if (attacker.hasKeyword("Flanking") && !blocker.hasKeyword("Flanking")) {
            toughness -= attacker.getAmountOfKeyword("Flanking");
        }
        
        if (blocker.getName().equals("Shape Stealer")) {
            toughness += attacker.getNetToughness() - blocker.getNetToughness();
        } 

        toughness += blocker.getKeywordMagnitude("Bushido");
        final Game game = attacker.getGame();
        final FCollection<Trigger> theTriggers = new FCollection<Trigger>();
        for (Card card : game.getCardsIn(ZoneType.Battlefield)) {
            theTriggers.addAll(card.getTriggers());
        }
        for (Card card : game.getCardsIn(ZoneType.Command)) {
            theTriggers.addAll(card.getTriggers());
        }
        theTriggers.addAll(attacker.getTriggers());
        for (final Trigger trigger : theTriggers) {
            final Map<String, String> trigParams = trigger.getMapParams();
            final Card source = trigger.getHostCard();

            if (!ComputerUtilCombat.combatTriggerWillTrigger(attacker, blocker, trigger, null)
                    || !trigParams.containsKey("Execute")) {
                continue;
            }
            final String ability = source.getSVar(trigParams.get("Execute"));
            final Map<String, String> abilityParams = AbilityFactory.getMapParams(ability);
            String abType = "";
            if (abilityParams.containsKey("AB")) {
            	abType = abilityParams.get("AB");
            } else if (abilityParams.containsKey("DB")) {
            	abType = abilityParams.get("DB");
            }

            // DealDamage triggers
            if (abType.equals("DealDamage")) {
                if (!abilityParams.containsKey("Defined") || !abilityParams.get("Defined").equals("TriggeredBlocker")) {
                    continue;
                }
                int damage = 0;
                try {
                    damage = Integer.parseInt(abilityParams.get("NumDmg"));
                } catch (final NumberFormatException nfe) {
                    // can't parse the number (X for example)
                    continue;
                }
                toughness -= predictDamageTo(blocker, damage, 0, source, false);
                continue;
            }

            // -1/-1 PutCounter triggers
            if (abType.equals("PutCounter")) {
                if (!abilityParams.containsKey("Defined") || !abilityParams.get("Defined").equals("TriggeredBlocker")) {
                    continue;
                }
                if (!abilityParams.containsKey("CounterType") || !abilityParams.get("CounterType").equals("M1M1")) {
                    continue;
                }
                int num = 0;
                try {
                    num = Integer.parseInt(abilityParams.get("CounterNum"));
                } catch (final NumberFormatException nfe) {
                    // can't parse the number (X for example)
                    continue;
                }
                toughness -= num;
                continue;
            }

            // Pump triggers
            if (!abType.equals("Pump")) {
                continue;
            }
            if (abilityParams.containsKey("ValidTgts") || abilityParams.containsKey("Tgt")) {
                continue; // targeted pumping not supported
            }
            final List<Card> list = AbilityUtils.getDefinedCards(source, abilityParams.get("Defined"), null);
            if (abilityParams.containsKey("Defined") && abilityParams.get("Defined").equals("TriggeredBlocker")) {
                list.add(blocker);
            }
            if (list.isEmpty()) {
                continue;
            }
            if (!list.contains(blocker)) {
                continue;
            }
            if (!abilityParams.containsKey("NumDef")) {
                continue;
            }

            String def = abilityParams.get("NumDef");
            if (def.startsWith("+")) {
                def = def.substring(1);
            }
            try {
                toughness += Integer.parseInt(def);
            } catch (final NumberFormatException nfe) {
                // can't parse the number (X for example)

            }
        }
        if (withoutAbilities) {
            return toughness;
        }
        for (SpellAbility ability : blocker.getAllSpellAbilities()) {
            if (!(ability instanceof AbilityActivated) || ability.getPayCosts() == null) {
                continue;
            }

            if (ability.hasParam("ActivationPhases") || ability.hasParam("SorcerySpeed") || ability.hasParam("ActivationZone")) {
                continue;
            }

            if (ability.getApi() == ApiType.Pump) {
                if (!ability.hasParam("NumDef")) {
                    continue;
                }
                
                if (ComputerUtilCost.canPayCost(ability, blocker.getController())) {
                    int tBonus = AbilityUtils.calculateAmount(ability.getHostCard(), ability.getParam("NumDef"), ability);
                    if (tBonus > 0) {
                        toughness += tBonus;
                    }
                }
            } else if (ability.getApi() == ApiType.PutCounter) {
                if (!ability.hasParam("CounterType") || !ability.getParam("CounterType").equals("P1P1")) {
                    continue;
                }
                
                if (ability.hasParam("Monstrosity") && blocker.isMonstrous()) {
                	continue;
                }
                
                if (ComputerUtilCost.canPayCost(ability, blocker.getController())) {
                    int tBonus = AbilityUtils.calculateAmount(ability.getHostCard(), ability.getParam("CounterNum"), ability);
                    if (tBonus > 0) {
                        toughness += tBonus;
                    }
                }
            }
        }
        return toughness;
    }

    // Predict the Power bonus of the blocker if blocking the attacker
    // (Flanking, Bushido and other triggered abilities)
    /**
     * <p>
     * predictPowerBonusOfAttacker.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.game.card.Card} object.
     * @param blocker
     *            a {@link forge.game.card.Card} object.
     * @param combat
     *            a {@link forge.game.combat.Combat} object.
     * @return a int.
     */
    public static int predictPowerBonusOfAttacker(final Card attacker, final Card blocker, final Combat combat , boolean withoutAbilities) {
        int power = 0;

        power += attacker.getKeywordMagnitude("Bushido");
        //check Exalted only for the first attacker
        if (combat != null && combat.getAttackers().isEmpty()) {
            for (Card card : attacker.getController().getCardsIn(ZoneType.Battlefield)) {
                power += card.getAmountOfKeyword("Exalted");
            }
        }

        // Serene Master switches power with attacker
        if (blocker!= null && blocker.getName().equals("Serene Master")) {
            power += blocker.getNetPower() - attacker.getNetPower();
        } else if (blocker != null && attacker.getName().equals("Shape Stealer")) {
            power += blocker.getNetPower() - attacker.getNetPower();
        }

        final Game game = attacker.getGame();
        final FCollection<Trigger> theTriggers = new FCollection<Trigger>();
        for (Card card : game.getCardsIn(ZoneType.Battlefield)) {
            theTriggers.addAll(card.getTriggers());
        }
        for (Card card : game.getCardsIn(ZoneType.Command)) {
            theTriggers.addAll(card.getTriggers());
        }
        // if the defender has first strike and wither the attacker will deal
        // less damage than expected
        if (null != blocker) {
            if (ComputerUtilCombat.dealsFirstStrikeDamage(blocker, withoutAbilities, combat)
                    && (blocker.hasKeyword("Wither") || blocker.hasKeyword("Infect"))
                    && !ComputerUtilCombat.dealsFirstStrikeDamage(attacker, withoutAbilities, combat) 
                    && !attacker.hasKeyword("CARDNAME can't have counters placed on it.")) {
                power -= blocker.getNetCombatDamage();
            }
            theTriggers.addAll(blocker.getTriggers());
        }

        // look out for continuous static abilities that only care for attacking
        // creatures
        final CardCollectionView cardList = CardCollection.combine(game.getCardsIn(ZoneType.Battlefield), game.getCardsIn(ZoneType.Command));
        for (final Card card : cardList) {
            for (final StaticAbility stAb : card.getStaticAbilities()) {
                final Map<String, String> params = stAb.getMapParams();
                if (!params.get("Mode").equals("Continuous")) {
                    continue;
                }
                if (!params.containsKey("Affected") || !params.get("Affected").contains("attacking")) {
                    continue;
                }
                final String valid = params.get("Affected").replace("attacking", "Creature");
                if (!attacker.isValid(valid, card.getController(), card, null)) {
                    continue;
                }
                if (params.containsKey("AddPower")) {
                    if (params.get("AddPower").equals("X")) {
                        power += CardFactoryUtil.xCount(card, card.getSVar("X"));
                    } else if (params.get("AddPower").equals("Y")) {
                        power += CardFactoryUtil.xCount(card, card.getSVar("Y"));
                    } else {
                        power += Integer.valueOf(params.get("AddPower"));
                    }
                }
            }
        }

        for (final Trigger trigger : theTriggers) {
            final Map<String, String> trigParams = trigger.getMapParams();
            final Card source = trigger.getHostCard();

            if (!ComputerUtilCombat.combatTriggerWillTrigger(attacker, blocker, trigger, combat)
                    || !trigParams.containsKey("Execute")) {
                continue;
            }
            final String ability = source.getSVar(trigParams.get("Execute"));
            final Map<String, String> abilityParams = AbilityFactory.getMapParams(ability);
            if (abilityParams.containsKey("ValidTgts") || abilityParams.containsKey("Tgt")) {
                continue; // targeted pumping not supported
            }
            if (abilityParams.containsKey("AB") && !abilityParams.get("AB").equals("Pump")
                    && !abilityParams.get("AB").equals("PumpAll")) {
                continue;
            }
            if (abilityParams.containsKey("DB") && !abilityParams.get("DB").equals("Pump")
                    && !abilityParams.get("DB").equals("PumpAll")) {
                continue;
            }
            List<Card> list = new ArrayList<Card>();
            if (!abilityParams.containsKey("ValidCards")) {
                list = AbilityUtils.getDefinedCards(source, abilityParams.get("Defined"), null);
            }
            if (abilityParams.containsKey("Defined") && abilityParams.get("Defined").equals("TriggeredAttacker")) {
                list.add(attacker);
            }
            if (abilityParams.containsKey("ValidCards")) {
                if (attacker.isValid(abilityParams.get("ValidCards").split(","), source.getController(), source, null)
                        || attacker.isValid(abilityParams.get("ValidCards").replace("attacking+", "").split(","),
                                source.getController(), source, null)) {
                    list.add(attacker);
                }
            }
            if (list.isEmpty()) {
                continue;
            }
            if (!list.contains(attacker)) {
                continue;
            }
            if (!abilityParams.containsKey("NumAtt")) {
                continue;
            }

            String att = abilityParams.get("NumAtt");
            if (att.startsWith("+")) {
                att = att.substring(1);
            }
            if (att.matches("[0-9][0-9]?") || att.matches("-" + "[0-9][0-9]?")) {
                power += Integer.parseInt(att);
            } else {
                String bonus = new String(source.getSVar(att));
                if (bonus.contains("TriggerCount$NumBlockers")) {
                    bonus = bonus.replace("TriggerCount$NumBlockers", "Number$1");
                }
                power += CardFactoryUtil.xCount(source, bonus);

            }
        }
        if (withoutAbilities) {
            return power;
        }
        for (SpellAbility ability : attacker.getAllSpellAbilities()) {
            if (!(ability instanceof AbilityActivated) || ability.getPayCosts() == null) {
                continue;
            }
            if (ability.hasParam("ActivationPhases") || ability.hasParam("SorcerySpeed") || ability.hasParam("ActivationZone")) {
                continue;
            }

            if (ability.getApi() == ApiType.Pump) {
                if (!ability.hasParam("NumAtt")) {
                    continue;
                }
    
                if (!ability.getPayCosts().hasTapCost() && ComputerUtilCost.canPayCost(ability, attacker.getController())) {
                    int pBonus = AbilityUtils.calculateAmount(ability.getHostCard(), ability.getParam("NumAtt"), ability);
                    if (pBonus > 0) {
                        power += pBonus;
                    }
                }
            } else if (ability.getApi() == ApiType.PutCounter) {
                if (!ability.hasParam("CounterType") || !ability.getParam("CounterType").equals("P1P1")) {
                    continue;
                }
                
                if (ability.hasParam("Monstrosity") && attacker.isMonstrous()) {
                	continue;
                }
                
                if (!ability.getPayCosts().hasTapCost() && ComputerUtilCost.canPayCost(ability, attacker.getController())) {
                    int pBonus = AbilityUtils.calculateAmount(ability.getHostCard(), ability.getParam("CounterNum"), ability);
                    if (pBonus > 0) {
                        power += pBonus;
                    }
                }
            }
        }
        return power;
    }

    // Predict the Toughness bonus of the attacker if blocked by the blocker
    // (Flanking, Bushido and other triggered abilities)
    /**
     * <p>
     * predictToughnessBonusOfAttacker.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.game.card.Card} object.
     * @param blocker
     *            a {@link forge.game.card.Card} object.
     * @param combat
     *            a {@link forge.game.combat.Combat} object.
     * @return a int.
     */
    public static int predictToughnessBonusOfAttacker(final Card attacker, final Card blocker, final Combat combat
            , boolean withoutAbilities) {
        int toughness = 0;

        //check Exalted only for the first attacker
        if (combat != null && combat.getAttackers().isEmpty()) {
            for (Card card : attacker.getController().getCardsIn(ZoneType.Battlefield)) {
                toughness += card.getAmountOfKeyword("Exalted");
            }
        }

        if (blocker != null && attacker.getName().equals("Shape Stealer")) {
            toughness += blocker.getNetToughness() - attacker.getNetToughness();
        }

        final Game game = attacker.getGame();
        final FCollection<Trigger> theTriggers = new FCollection<Trigger>();
        for (Card card : game.getCardsIn(ZoneType.Battlefield)) {
            theTriggers.addAll(card.getTriggers());
        }
        for (Card card : game.getCardsIn(ZoneType.Command)) {
            theTriggers.addAll(card.getTriggers());
        }
        if (blocker != null) {
            toughness += attacker.getKeywordMagnitude("Bushido");
            theTriggers.addAll(blocker.getTriggers());
        }

        // look out for continuous static abilities that only care for attacking
        // creatures
        final CardCollectionView cardList = game.getCardsIn(ZoneType.Battlefield);
        for (final Card card : cardList) {
            for (final StaticAbility stAb : card.getStaticAbilities()) {
                final Map<String, String> params = stAb.getMapParams();
                if (!params.get("Mode").equals("Continuous")) {
                    continue;
                }
                if (!params.containsKey("Affected") || !params.get("Affected").contains("attacking")) {
                    continue;
                }
                final String valid = params.get("Affected").replace("attacking", "Creature");
                if (!attacker.isValid(valid, card.getController(), card, null)) {
                    continue;
                }
                if (params.containsKey("AddToughness")) {
                    if (params.get("AddToughness").equals("X")) {
                        toughness += CardFactoryUtil.xCount(card, card.getSVar("X"));
                    } else if (params.get("AddToughness").equals("Y")) {
                        toughness += CardFactoryUtil.xCount(card, card.getSVar("Y"));
                    } else {
                        toughness += Integer.valueOf(params.get("AddToughness"));
                    }
                }
            }
        }

        for (final Trigger trigger : theTriggers) {
            final Map<String, String> trigParams = trigger.getMapParams();
            final Card source = trigger.getHostCard();

            if (!ComputerUtilCombat.combatTriggerWillTrigger(attacker, blocker, trigger, combat)
                    || !trigParams.containsKey("Execute")) {
                continue;
            }
            final String ability = source.getSVar(trigParams.get("Execute"));
            final Map<String, String> abilityParams = AbilityFactory.getMapParams(ability);
            if (abilityParams.containsKey("ValidTgts") || abilityParams.containsKey("Tgt")) {
                continue; // targeted pumping not supported
            }

            // DealDamage triggers
            if ((abilityParams.containsKey("AB") && abilityParams.get("AB").equals("DealDamage"))
                    || (abilityParams.containsKey("DB") && abilityParams.get("DB").equals("DealDamage"))) {
                if (!abilityParams.containsKey("Defined") || !abilityParams.get("Defined").equals("TriggeredAttacker")) {
                    continue;
                }
                int damage = 0;
                try {
                    damage = Integer.parseInt(abilityParams.get("NumDmg"));
                } catch (final NumberFormatException nfe) {
                    // can't parse the number (X for example)
                    continue;
                }
                toughness -= predictDamageTo(attacker, damage, 0, source, false);
                continue;
            }

            // Pump triggers
            if (abilityParams.containsKey("AB") && !abilityParams.get("AB").equals("Pump")
                    && !abilityParams.get("AB").equals("PumpAll")) {
                continue;
            }
            if (abilityParams.containsKey("DB") && !abilityParams.get("DB").equals("Pump")
                    && !abilityParams.get("DB").equals("PumpAll")) {
                continue;
            }
            List<Card> list = new ArrayList<Card>();
            if (!abilityParams.containsKey("ValidCards")) {
                list = AbilityUtils.getDefinedCards(source, abilityParams.get("Defined"), null);
            }
            if (abilityParams.containsKey("Defined") && abilityParams.get("Defined").equals("TriggeredAttacker")) {
                list.add(attacker);
            }
            if (abilityParams.containsKey("ValidCards")) {
                if (attacker.isValid(abilityParams.get("ValidCards").split(","), source.getController(), source, null)
                        || attacker.isValid(abilityParams.get("ValidCards").replace("attacking+", "").split(","),
                                source.getController(), source, null)) {
                    list.add(attacker);
                }
            }
            if (list.isEmpty()) {
                continue;
            }
            if (!list.contains(attacker)) {
                continue;
            }
            if (!abilityParams.containsKey("NumDef")) {
                continue;
            }

            String def = abilityParams.get("NumDef");
            if (def.startsWith("+")) {
                def = def.substring(1);
            }
            if (def.matches("[0-9][0-9]?") || def.matches("-" + "[0-9][0-9]?")) {
                toughness += Integer.parseInt(def);
            } else {
                String bonus = new String(source.getSVar(def));
                if (bonus.contains("TriggerCount$NumBlockers")) {
                    bonus = bonus.replace("TriggerCount$NumBlockers", "Number$1");
                }
                toughness += CardFactoryUtil.xCount(source, bonus);
            }
        }
        if (withoutAbilities) {
            return toughness;
        }
        for (SpellAbility ability : attacker.getAllSpellAbilities()) {
            if (!(ability instanceof AbilityActivated) || ability.getPayCosts() == null) {
                continue;
            }

            if (ability.hasParam("ActivationPhases") || ability.hasParam("SorcerySpeed") || ability.hasParam("ActivationZone")) {
                continue;
            }

            if (ability.getApi() == ApiType.Pump) {
                if (!ability.hasParam("NumDef")) {
                    continue;
                }

                if (!ability.getPayCosts().hasTapCost() && ComputerUtilCost.canPayCost(ability, attacker.getController())) {
                    int tBonus = AbilityUtils.calculateAmount(ability.getHostCard(), ability.getParam("NumDef"), ability);
                    if (tBonus > 0) {
                        toughness += tBonus;
                    }
                }
            } else if (ability.getApi() == ApiType.PutCounter) {
                if (!ability.hasParam("CounterType") || !ability.getParam("CounterType").equals("P1P1")) {
                    continue;
                }
                
                if (ability.hasParam("Monstrosity") && attacker.isMonstrous()) {
                	continue;
                }
                
                if (!ability.getPayCosts().hasTapCost() && ComputerUtilCost.canPayCost(ability, attacker.getController())) {
                    int tBonus = AbilityUtils.calculateAmount(ability.getHostCard(), ability.getParam("CounterNum"), ability);
                    if (tBonus > 0) {
                        toughness += tBonus;
                    }
                }
            }
        }
        return toughness;
    }
    
    // check whether the attacker will be destroyed by triggered abilities before First Strike damage
    public static boolean canDestroyAttackerBeforeFirstStrike(final Card attacker, final Card blocker, final Combat combat,
            final boolean withoutAbilities) {
        if (blocker.isEquippedBy("Godsend")) {
           return true;
        }
        if (attacker.hasKeyword("Indestructible") || ComputerUtil.canRegenerate(attacker.getController(), attacker)) {
            return false;
        }
        
        //Check triggers that deal damage or shrink the attacker
        if (ComputerUtilCombat.getDamageToKill(attacker)
        		+ ComputerUtilCombat.predictToughnessBonusOfAttacker(attacker, blocker, combat, withoutAbilities) <= 0) {
        	return true;
        }

        // check Destroy triggers (Cockatrice and friends)
        final FCollection<Trigger> theTriggers = new FCollection<Trigger>();
        for (Card card : attacker.getGame().getCardsIn(ZoneType.Battlefield)) {
            theTriggers.addAll(card.getTriggers());
        }
        for (Trigger trigger : theTriggers) {
            Map<String, String> trigParams = trigger.getMapParams();
            final Card source = trigger.getHostCard();

            if (!ComputerUtilCombat.combatTriggerWillTrigger(attacker, blocker, trigger, null)) {
                continue;
            }
            //consider delayed triggers
            if (trigParams.containsKey("DelayedTrigger")) {
                String sVarName = trigParams.get("DelayedTrigger");
                trigger = TriggerHandler.parseTrigger(source.getSVar(sVarName), trigger.getHostCard(), true);
                trigParams = trigger.getMapParams();
            }
            if (!trigParams.containsKey("Execute")) {
                continue;
            }
            String ability = source.getSVar(trigParams.get("Execute"));
            final Map<String, String> abilityParams = AbilityFactory.getMapParams(ability);
            if ((abilityParams.containsKey("AB") && abilityParams.get("AB").equals("Destroy"))
                    || (abilityParams.containsKey("DB") && abilityParams.get("DB").equals("Destroy"))) {
                if (!abilityParams.containsKey("Defined")) {
                    continue;
                }
                if (abilityParams.get("Defined").equals("TriggeredAttacker")) {
                    return true;
                }
                if (abilityParams.get("Defined").equals("Self") && source.equals(attacker)) {
                    return true;
                }
                if (abilityParams.get("Defined").equals("TriggeredTarget") && source.equals(blocker)) {
                    return true;
                }
            }
        }
        return false;
    }

    // can the attacker be potentially destroyed in combat or is it potentially indestructible?
    /**
     * <p>
     * attackerCantBeDestroyedNow.
     * </p>
     * @param ai 
     * 
     * @param attacker
     *            a {@link forge.game.card.Card} object.
     * @return a boolean.
     */
    public static boolean attackerCantBeDestroyedInCombat(Player ai, final Card attacker) {
        // attacker is either indestructible or may regenerate
        if (attacker.hasKeyword("Indestructible") || (ComputerUtil.canRegenerate(ai, attacker))) {
            return true;
        }

        // attacker will regenerate
        if (attacker.getShieldCount() > 0 && !attacker.hasKeyword("CARDNAME can't be regenerated.")) {
            return true;
        }

        // all damage will be prevented
        if (attacker.hasKeyword("PreventAllDamageBy Creature.blockingSource")) {
            return true;
        }

        return false;
    }

    // can the blocker destroy the attacker?
    /**
     * <p>
     * canDestroyAttacker.
     * </p>
     * @param ai 
     * 
     * @param attacker
     *            a {@link forge.game.card.Card} object.
     * @param blocker
     *            a {@link forge.game.card.Card} object.
     * @param combat
     *            a {@link forge.game.combat.Combat} object.
     * @param withoutAbilities
     *            a boolean.
     * @return a boolean.
     */
    public static boolean canDestroyAttacker(Player ai, Card attacker, Card blocker, final Combat combat,
            final boolean withoutAbilities) {
        // Can activate transform ability
        if (!withoutAbilities) {
            attacker = canTransform(attacker);
            blocker = canTransform(blocker);
        }
    	if (canDestroyAttackerBeforeFirstStrike(attacker, blocker, combat, withoutAbilities)) {
    		return true;
    	}

    	if (canDestroyBlockerBeforeFirstStrike(blocker, attacker, withoutAbilities)) {
    		return false;
    	}

        int flankingMagnitude = 0;
        if (attacker.hasKeyword("Flanking") && !blocker.hasKeyword("Flanking")) {

            flankingMagnitude = attacker.getAmountOfKeyword("Flanking");

            if (flankingMagnitude >= blocker.getNetToughness()) {
                return false;
            }
            if ((flankingMagnitude >= (blocker.getNetToughness() - blocker.getDamage()))
                    && !blocker.hasKeyword("Indestructible")) {
                return false;
            }
        } // flanking

        if (((attacker.hasKeyword("Indestructible") || (ComputerUtil.canRegenerate(ai, attacker) && !withoutAbilities)) 
                && !(blocker.hasKeyword("Wither") || blocker.hasKeyword("Infect")))
                || (attacker.hasKeyword("Persist") && !attacker.canReceiveCounters(CounterType.M1M1) && (attacker
                        .getCounters(CounterType.M1M1) == 0))
                || (attacker.hasKeyword("Undying") && !attacker.canReceiveCounters(CounterType.P1P1) && (attacker
                        .getCounters(CounterType.P1P1) == 0))) {
            return false;
        }

        if (attacker.hasKeyword("PreventAllDamageBy Creature.blockingSource")) {
            return false;
        }

        int defenderDamage;
        int attackerDamage;
        if (blocker.toughnessAssignsDamage()) {
            defenderDamage = blocker.getNetToughness()
                    + ComputerUtilCombat.predictToughnessBonusOfBlocker(attacker, blocker, withoutAbilities);
        } else {
        	defenderDamage = blocker.getNetPower()
                    + ComputerUtilCombat.predictPowerBonusOfBlocker(attacker, blocker, withoutAbilities);
        }
        if (attacker.toughnessAssignsDamage()) {
            attackerDamage = attacker.getNetToughness()
                    + ComputerUtilCombat.predictToughnessBonusOfAttacker(attacker, blocker, combat, withoutAbilities);
        } else {
        	attackerDamage = attacker.getNetPower()
                    + ComputerUtilCombat.predictPowerBonusOfAttacker(attacker, blocker, combat, withoutAbilities);
        }

        int possibleDefenderPrevention = 0;
        int possibleAttackerPrevention = 0;
        if (!withoutAbilities) {
            possibleDefenderPrevention = ComputerUtil.possibleDamagePrevention(blocker);
            possibleAttackerPrevention = ComputerUtil.possibleDamagePrevention(attacker);
        }

        // consider Damage Prevention/Replacement
        defenderDamage = predictDamageTo(attacker, defenderDamage, possibleAttackerPrevention, blocker, true);
        attackerDamage = predictDamageTo(blocker, attackerDamage, possibleDefenderPrevention, attacker, true);

        final int defenderLife = ComputerUtilCombat.getDamageToKill(blocker)
                + ComputerUtilCombat.predictToughnessBonusOfBlocker(attacker, blocker, withoutAbilities);
        final int attackerLife = ComputerUtilCombat.getDamageToKill(attacker)
                + ComputerUtilCombat.predictToughnessBonusOfAttacker(attacker, blocker, combat, withoutAbilities);

        if (blocker.hasKeyword("Double Strike")) {
            if (defenderDamage > 0 && (hasKeyword(blocker, "Deathtouch", withoutAbilities, combat) || attacker.hasSVar("DestroyWhenDamaged"))) {
                return true;
            }
            if (defenderDamage >= attackerLife) {
                return true;
            }

            // Attacker may kill the blocker before he can deal normal
            // (secondary) damage
            if (dealsFirstStrikeDamage(attacker, withoutAbilities, combat) && !blocker.hasKeyword("Indestructible")) {
                if (attackerDamage >= defenderLife) {
                    return false;
                }
                if (attackerDamage > 0 && (hasKeyword(attacker, "Deathtouch", withoutAbilities, combat) || blocker.hasSVar("DestroyWhenDamaged"))) {
                    return false;
                }
            }
            if (attackerLife <= 2 * defenderDamage) {
                return true;
            }
        } // defender double strike

        else { // no double strike for defender
               // Attacker may kill the blocker before he can deal any damage
            if (dealsFirstStrikeDamage(attacker, withoutAbilities, combat)
                    && !blocker.hasKeyword("Indestructible") 
                    && !dealsFirstStrikeDamage(blocker, withoutAbilities, combat)) {

                if (attackerDamage >= defenderLife) {
                    return false;
                }
                if (attackerDamage > 0 && (hasKeyword(attacker, "Deathtouch", withoutAbilities, combat) || blocker.hasSVar("DestroyWhenDamaged"))) {
                    return false;
                }
            }

            if (defenderDamage > 0 && (hasKeyword(blocker, "Deathtouch", withoutAbilities, combat) || attacker.hasSVar("DestroyWhenDamaged"))) {
                return true;
            }

            return defenderDamage >= attackerLife;

        } // defender no double strike
        return false; // should never arrive here
    } // canDestroyAttacker

    // For AI safety measures like Regeneration
    /**
     * <p>
     * blockerWouldBeDestroyed.
     * </p>
     * @param ai 
     * 
     * @param blocker
     *            a {@link forge.game.card.Card} object.
     * @return a boolean.
     */
    public static boolean blockerWouldBeDestroyed(Player ai, final Card blocker, Combat combat) {
        // TODO THis function only checks if a single attacker at a time would destroy a blocker
        // This needs to expand to tally up damage
        final List<Card> attackers = combat.getAttackersBlockedBy(blocker);

        for (Card attacker : attackers) {
            if (ComputerUtilCombat.canDestroyBlocker(ai, blocker, attacker, combat, true)
                    && !(attacker.hasKeyword("Wither") || attacker.hasKeyword("Infect"))) {
                return true;
            }
        }
        return false;
    }

    public static boolean canDestroyBlockerBeforeFirstStrike(final Card blocker, final Card attacker, final boolean withoutAbilities) {

    	if (attacker.isEquippedBy("Godsend")) {
            return true;
        }
        
        if (attacker.getName().equals("Elven Warhounds")) {
        	return true;
        }

        int flankingMagnitude = 0;
        if (attacker.hasKeyword("Flanking") && !blocker.hasKeyword("Flanking")) {

            flankingMagnitude = attacker.getAmountOfKeyword("Flanking");

            if (flankingMagnitude >= blocker.getNetToughness()) {
                return true;
            }
            if ((flankingMagnitude >= ComputerUtilCombat.getDamageToKill(blocker)) && !blocker.hasKeyword("Indestructible")) {
                return true;
            }
        } // flanking
        
        if (blocker.hasKeyword("Indestructible") || ComputerUtil.canRegenerate(blocker.getController(), blocker)) {
            return false;
        }

        if (ComputerUtilCombat.getDamageToKill(blocker)
        		+ ComputerUtilCombat.predictToughnessBonusOfBlocker(attacker, blocker, withoutAbilities) <= 0) {
        	return true;
        }

        final Game game = blocker.getGame();
        final FCollection<Trigger> theTriggers = new FCollection<Trigger>();
        for (Card card : game.getCardsIn(ZoneType.Battlefield)) {
            theTriggers.addAll(card.getTriggers());
        }
        for (Trigger trigger : theTriggers) {
            Map<String, String> trigParams = trigger.getMapParams();
            final Card source = trigger.getHostCard();

            if (!ComputerUtilCombat.combatTriggerWillTrigger(attacker, blocker, trigger, null)) {
                continue;
            }
            //consider delayed triggers
            if (trigParams.containsKey("DelayedTrigger")) {
                String sVarName = trigParams.get("DelayedTrigger");
                trigger = TriggerHandler.parseTrigger(source.getSVar(sVarName), trigger.getHostCard(), true);
                trigParams = trigger.getMapParams();
            }
            if (!trigParams.containsKey("Execute")) {
                continue;
            }
            String ability = source.getSVar(trigParams.get("Execute"));
            final Map<String, String> abilityParams = AbilityFactory.getMapParams(ability);
            // Destroy triggers
            if ((abilityParams.containsKey("AB") && abilityParams.get("AB").equals("Destroy"))
                    || (abilityParams.containsKey("DB") && abilityParams.get("DB").equals("Destroy"))) {
                if (!abilityParams.containsKey("Defined")) {
                    continue;
                }
                if (abilityParams.get("Defined").equals("TriggeredBlocker")) {
                    return true;
                }
                if (abilityParams.get("Defined").equals("Self") && source.equals(blocker)) {
                    return true;
                }
                if (abilityParams.get("Defined").equals("TriggeredTarget") && source.equals(attacker)) {
                    return true;
                }
            }
        }
        
    	return false;
    }
    
    // can the attacker destroy this blocker?
    /**
     * <p>
     * canDestroyBlocker.
     * </p>
     * @param ai 
     * 
     * @param blocker
     *            a {@link forge.game.card.Card} object.
     * @param attacker
     *            a {@link forge.game.card.Card} object.
     * @param combat
     *            a {@link forge.game.combat.Combat} object.
     * @param withoutAbilities
     *            a boolean.
     * @return a boolean.
     */
    public static boolean canDestroyBlocker(Player ai, Card blocker, Card attacker, final Combat combat,
            final boolean withoutAbilities) {
        // Can activate transform ability
        if (!withoutAbilities) {
            attacker = canTransform(attacker);
            blocker = canTransform(blocker);
        }
    	if (canDestroyBlockerBeforeFirstStrike(blocker, attacker, withoutAbilities)) {
    		return true;
    	}

        if (((blocker.hasKeyword("Indestructible") || (ComputerUtil.canRegenerate(ai, blocker) && !withoutAbilities)) && !(attacker
                .hasKeyword("Wither") || attacker.hasKeyword("Infect")))
                || (blocker.hasKeyword("Persist") && !blocker.canReceiveCounters(CounterType.M1M1) && (blocker
                        .getCounters(CounterType.M1M1) == 0))
                || (blocker.hasKeyword("Undying") && !blocker.canReceiveCounters(CounterType.P1P1) && (blocker
                        .getCounters(CounterType.P1P1) == 0))) {
            return false;
        }

    	if (canDestroyAttackerBeforeFirstStrike(attacker, blocker, combat, withoutAbilities)) {
    		return false;
    	}
        
        int defenderDamage;
        int attackerDamage;
        if (blocker.toughnessAssignsDamage()) {
            defenderDamage = blocker.getNetToughness()
                    + ComputerUtilCombat.predictToughnessBonusOfBlocker(attacker, blocker, withoutAbilities);
        } else {
        	defenderDamage = blocker.getNetPower()
                    + ComputerUtilCombat.predictPowerBonusOfBlocker(attacker, blocker, withoutAbilities);
        }
        if (attacker.toughnessAssignsDamage()) {
            attackerDamage = attacker.getNetToughness()
                    + ComputerUtilCombat.predictToughnessBonusOfAttacker(attacker, blocker, combat, withoutAbilities);
        } else {
        	attackerDamage = attacker.getNetPower()
                    + ComputerUtilCombat.predictPowerBonusOfAttacker(attacker, blocker, combat, withoutAbilities);
        }

        int possibleDefenderPrevention = 0;
        int possibleAttackerPrevention = 0;
        if (!withoutAbilities) {
            possibleDefenderPrevention = ComputerUtil.possibleDamagePrevention(blocker);
            possibleAttackerPrevention = ComputerUtil.possibleDamagePrevention(attacker);
        }

        // consider Damage Prevention/Replacement
        defenderDamage = predictDamageTo(attacker, defenderDamage, possibleAttackerPrevention, blocker, true);
        attackerDamage = predictDamageTo(blocker, attackerDamage, possibleDefenderPrevention, attacker, true);

        if (combat != null) {
            for (Card atkr : combat.getAttackersBlockedBy(blocker)) {
                if (!atkr.equals(attacker)) {
                    attackerDamage += predictDamageTo(blocker, atkr.getNetCombatDamage(), 0, atkr, true);
                }
            }
        }

        final int defenderLife = ComputerUtilCombat.getDamageToKill(blocker)
                + ComputerUtilCombat.predictToughnessBonusOfBlocker(attacker, blocker, withoutAbilities);
        final int attackerLife = ComputerUtilCombat.getDamageToKill(attacker)
                + ComputerUtilCombat.predictToughnessBonusOfAttacker(attacker, blocker, combat, withoutAbilities);

        if (attacker.hasKeyword("Double Strike")) {
            if (attackerDamage > 0 && (hasKeyword(attacker, "Deathtouch", withoutAbilities, combat) || blocker.hasSVar("DestroyWhenDamaged"))) {
                return true;
            }
            if (attackerDamage >= defenderLife) {
                return true;
            }

            // Attacker may kill the blocker before he can deal normal
            // (secondary) damage
            if (dealsFirstStrikeDamage(blocker, withoutAbilities, combat) && !attacker.hasKeyword("Indestructible")) {
                if (defenderDamage >= attackerLife) {
                    return false;
                }
                if (defenderDamage > 0 && (hasKeyword(blocker, "Deathtouch", withoutAbilities, combat) || attacker.hasSVar("DestroyWhenDamaged"))) {
                    return false;
                }
            }
            if (defenderLife <= 2 * attackerDamage) {
                return true;
            }
        } // attacker double strike

        else { // no double strike for attacker
               // Defender may kill the attacker before he can deal any damage
            if (dealsFirstStrikeDamage(blocker, withoutAbilities, combat) && !attacker.hasKeyword("Indestructible") 
                    && !dealsFirstStrikeDamage(attacker, withoutAbilities, combat)) {

                if (defenderDamage >= attackerLife) {
                    return false;
                }
                if (defenderDamage > 0 && (hasKeyword(blocker, "Deathtouch", withoutAbilities, combat) || attacker.hasSVar("DestroyWhenDamaged"))) {
                    return false;
                }
            }

            if (attackerDamage > 0 && (hasKeyword(attacker, "Deathtouch", withoutAbilities, combat) || blocker.hasSVar("DestroyWhenDamaged"))) {
                return true;
            }

            return attackerDamage >= defenderLife;

        } // attacker no double strike
        return false; // should never arrive here
    } // canDestroyBlocker


    /**
     * <p>
     * distributeAIDamage.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.game.card.Card} object.
     * @param block
     * @param dmgCanDeal
     *            a int.
     * @param defender 
     * @param overrideOrder overriding combatant order
     */
    public static Map<Card, Integer> distributeAIDamage(final Card attacker, final CardCollectionView block, int dmgCanDeal, GameEntity defender, boolean overrideOrder) {
        // TODO: Distribute defensive Damage (AI controls how damage is dealt to own cards) for Banding and Defensive Formation
        Map<Card, Integer> damageMap = new HashMap<Card, Integer>();
        
        boolean isAttacking = defender != null;
        
        if (isAttacking && (attacker.hasKeyword("You may have CARDNAME assign its combat damage as though it weren't blocked.")
                || attacker.hasKeyword("CARDNAME assigns its combat damage as though it weren't blocked."))) {
            damageMap.put(null, dmgCanDeal);
            return damageMap;
        }
        
        final boolean hasTrample = attacker.hasKeyword("Trample");
    
        if (block.size() == 1) {
            final Card blocker = block.getFirst();

            // trample
            if (hasTrample) {
    
                int dmgToKill = ComputerUtilCombat.getEnoughDamageToKill(blocker, dmgCanDeal, attacker, true);
    
                if (dmgCanDeal < dmgToKill) {
                    dmgToKill = Math.min(blocker.getLethalDamage(), dmgCanDeal);
                } else {
                    dmgToKill = Math.max(blocker.getLethalDamage(), dmgToKill);
                }
                
                if (!isAttacking) { // no entity to deliver damage via trample
                    dmgToKill = dmgCanDeal;
                }
    
                final int remainingDmg = dmgCanDeal - dmgToKill;
    
                // If Extra trample damage, assign to defending player/planeswalker (when there is one)
                if (remainingDmg > 0) {
                    damageMap.put(null, remainingDmg);
                }
    
                damageMap.put(blocker, dmgToKill);
            } else {
                damageMap.put(blocker, dmgCanDeal);
            }
        } // 1 blocker
        else {
            // Does the attacker deal lethal damage to all blockers
            //Blocking Order now determined after declare blockers
            Card lastBlocker = null;
            for (final Card b : block) {
                lastBlocker = b;
                final int dmgToKill = ComputerUtilCombat.getEnoughDamageToKill(b, dmgCanDeal, attacker, true);
                if (dmgToKill <= dmgCanDeal) {
                    damageMap.put(b, dmgToKill);
                    dmgCanDeal -= dmgToKill;
                } else {
                    // if it can't be killed choose the minimum damage
                    int dmg = Math.min(b.getLethalDamage(), dmgCanDeal);
                    damageMap.put(b, dmg);
                    dmgCanDeal -= dmg;
                    if (dmgCanDeal <= 0) {
                        break;
                    }
                }
            } // for
    
            if (dmgCanDeal > 0 ) { // if any damage left undistributed, 
                if (hasTrample && isAttacking) // if you have trample, deal damage to defending entity
                    damageMap.put(null, dmgCanDeal);
                else if ( lastBlocker != null ) { // otherwise flush it into last blocker
                    damageMap.put(lastBlocker, dmgCanDeal + damageMap.get(lastBlocker));
                }
            }
        }
        return damageMap;
    } // setAssignedDamage()
    
    
    // how much damage is enough to kill the creature (for AI)
    /**
     * <p>
     * getEnoughDamageToKill.
     * </p>
     * 
     * @param maxDamage
     *            a int.
     * @param source
     *            a {@link forge.game.card.Card} object.
     * @param isCombat
     *            a boolean.
     * @return a int.
     */
    public final static int getEnoughDamageToKill(final Card c, final int maxDamage, final Card source, final boolean isCombat) {
        return getEnoughDamageToKill(c, maxDamage, source, isCombat, false);
    }

    /**
     * <p>
     * getEnoughDamageToKill.
     * </p>
     * 
     * @param maxDamage
     *            a int.
     * @param source
     *            a {@link forge.game.card.Card} object.
     * @param isCombat
     *            a boolean.
     * @param noPrevention
     *            a boolean.
     * @return a int.
     */
    public static final int getEnoughDamageToKill(final Card c, final int maxDamage, final Card source, final boolean isCombat,
            final boolean noPrevention) {
        final int killDamage = c.isPlaneswalker() ? c.getCurrentLoyalty() : ComputerUtilCombat.getDamageToKill(c);

        if (c.hasKeyword("Indestructible") || c.getShieldCount() > 0) {
            if (!(source.hasKeyword("Wither") || source.hasKeyword("Infect"))) {
                return maxDamage + 1;
            }
        } else if (source.hasKeyword("Deathtouch")) {
            for (int i = 1; i <= maxDamage; i++) {
                if (noPrevention) {
                    if (c.staticReplaceDamage(i, source, isCombat) > 0) {
                        return i;
                    }
                } else if (predictDamageTo(c, i, source, isCombat) > 0) {
                    return i;
                }
            }
        }

        for (int i = 1; i <= maxDamage; i++) {
            if (noPrevention) {
                if (c.staticReplaceDamage(i, source, isCombat) >= killDamage) {
                    return i;
                }
            } else {
                if (predictDamageTo(c, i, source, isCombat) >= killDamage) {
                    return i;
                }
            }
        }

        return maxDamage + 1;
    }

    // the amount of damage needed to kill the creature (for AI)
    /**
     * <p>
     * getKillDamage.
     * </p>
     * 
     * @return a int.
     */
    public final static int getDamageToKill(final Card c) {
        int killDamage = c.getLethalDamage() + c.getPreventNextDamageTotalShields();
        if ((killDamage > c.getPreventNextDamageTotalShields())
                && c.hasSVar("DestroyWhenDamaged")) {
            killDamage = 1 + c.getPreventNextDamageTotalShields();
        }

        return killDamage;
    }
    
    
    /**
     * <p>
     * predictDamage.
     * </p>
     * 
     * @param damage
     *            a int.
     * @param source
     *            a {@link forge.game.card.Card} object.
     * @param isCombat
     *            a boolean.
     * @return a int.
     */

    public final static int predictDamageTo(final Player target, final int damage, final Card source, final boolean isCombat) {

        final Game game = target.getGame();
        int restDamage = damage;

        restDamage = target.staticReplaceDamage(restDamage, source, isCombat);

        // Predict replacement effects
        for (final Card ca : game.getCardsIn(ZoneType.listValueOf("Battlefield,Command"))) {
            for (final ReplacementEffect re : ca.getReplacementEffects()) {
                Map<String, String> params = re.getMapParams();
                if (!"DamageDone".equals(params.get("Event")) || !params.containsKey("PreventionEffect")) {
                    continue;
                }
                // Immortal Coil prevents the damage but has a similar negative effect
                if ("Immortal Coil".equals(ca.getName())) {
                    continue;
                }
                if (params.containsKey("ValidSource")
                        && !source.isValid(params.get("ValidSource"), ca.getController(), ca, null)) {
                    continue;
                }
                if (params.containsKey("ValidTarget")
                        && !target.isValid(params.get("ValidTarget"), ca.getController(), ca, null)) {
                    continue;
                }
                if (params.containsKey("IsCombat")) {
                    if (params.get("IsCombat").equals("True")) {
                        if (!isCombat) {
                            continue;
                        }
                    } else {
                        if (isCombat) {
                            continue;
                        }
                    }

                }
                return 0;
            }
        }

        restDamage = target.staticDamagePrevention(restDamage, source, isCombat, true);

        return restDamage;
    }
    
    /**
     * <p>
     * predictDamage.
     * </p>
     * 
     * @param damage
     *            a int.
     * @param source
     *            a {@link forge.game.card.Card} object.
     * @param isCombat
     *            a boolean.
     * @return a int.
     */
    // This function helps the AI calculate the actual amount of damage an
    // effect would deal
    public final static int predictDamageTo(final Card target, final int damage, final Card source, final boolean isCombat) {

        int restDamage = damage;

        restDamage = target.staticReplaceDamage(restDamage, source, isCombat);
        restDamage = target.staticDamagePrevention(restDamage, source, isCombat, true);

        return restDamage;
    }


    // This function helps the AI calculate the actual amount of damage an
    // effect would deal
    /**
     * <p>
     * predictDamage.
     * </p>
     * 
     * @param damage
     *            a int.
     * @param possiblePrevention
     *            a int.
     * @param source
     *            a {@link forge.game.card.Card} object.
     * @param isCombat
     *            a boolean.
     * @return a int.
     */
    public final static int predictDamageTo(final Card target, final int damage, final int possiblePrevention, final Card source, final boolean isCombat) {
    
        int restDamage = damage;
    
        restDamage = target.staticReplaceDamage(restDamage, source, isCombat);
        restDamage = target.staticDamagePrevention(restDamage, possiblePrevention, source, isCombat);
    
        return restDamage;
    }

    public final static boolean dealsFirstStrikeDamage(final Card combatant, final boolean withoutAbilities, final Combat combat) {
        
        if (combatant.hasKeyword("Double Strike") || combatant.hasKeyword("First Strike")) {
            return true;
        }
        
        if (!withoutAbilities) {
        	List<String> keywords = new ArrayList<String>();
        	keywords.add("Double Strike");
        	keywords.add("First Strike");
            return canGainKeyword(combatant, keywords, combat);
        }
    
        return false;
    }

    /**
     * Refactored version of canGainKeyword(final Card combatant, final String keyword) that specifies if abilities are
     * to be considered.
     * @param combatant target card
     * @param keyword keyword to consider
     * @param withoutAbilities flag that determines if activated abilities are to be considered
     * @return
     */
    public final static boolean hasKeyword(final Card combatant, final String keyword, final boolean withoutAbilities, final Combat combat) {
        if (combatant.hasKeyword(keyword)) {
            return true;
        }
        if (!withoutAbilities) {
        	List<String> keywords = new ArrayList<String>();
        	keywords.add(keyword);
            return canGainKeyword(combatant, keywords, combat);
        } else {
            return false;
        }
    }

    public final static boolean canGainKeyword(final Card combatant, final List<String> keywords, final Combat combat) {
    	final Player controller = combatant.getController();
    	for (Card c : controller.getCardsIn(ZoneType.Battlefield)) {
	    	for (SpellAbility ability : c.getAllSpellAbilities()) {
	            if (!(ability instanceof AbilityActivated) || ability.getPayCosts() == null) {
	                continue;
	            }
	            if (ability.getApi() != ApiType.Pump) {
	                continue;
	            }
	
	            if (ability.hasParam("ActivationPhases") || ability.hasParam("SorcerySpeed")) {
	                continue;
	            }
	
	            if (!ability.hasParam("KW") || !ComputerUtilCost.canPayCost(ability, controller)) {
	                continue;
	            }
	            if (c != combatant) {
	            	if (ability.getTargetRestrictions() == null || !ability.canTarget(combatant)) {
	            		continue;
	            	}
	            	//the AI will will fail to predict tapping of attackers
	            	if (controller.getGame().getPhaseHandler().isPlayerTurn(controller)) {
		            	if (combat == null || !combat.isAttacking(combatant) || combat.isAttacking(c)) {
		            		continue;
		            	}
	            	}

	            }
	            for (String keyword : keywords) {
	            	if (ability.getParam("KW").contains(keyword)) {
	            		return true;
	            	}
	            }
	        }
    	}
    
        return false;
    }
    
    /**
     * Transforms into alternate state if possible
     * @param original original creature
     * @return transform creature if possible, original creature otherwise
     */
    private final static Card canTransform(Card original) {
        if (original.isDoubleFaced() && !original.isInAlternateState()) {
            for (SpellAbility sa : original.getSpellAbilities()) {
                if (sa.getApi() == ApiType.SetState && ComputerUtilCost.canPayCost(sa, original.getController())) {
                    Card transformed = CardFactory.copyCard(original, true);    // clone into new object
                    if (transformed.changeCardState("Transform", null)) {
                        return transformed;
                    }
                }
            }
        }
        return original;
    }
}


