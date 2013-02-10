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
package forge.game.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.CounterType;
import forge.Singletons;
import forge.card.TriggerReplacementBase;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.ApiType;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.replacement.ReplacementEffect;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.SpellAbility;
import forge.card.staticability.StaticAbility;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;
import forge.card.trigger.TriggerType;
import forge.game.GameState;
import forge.game.GlobalRuleChange;
import forge.game.phase.Combat;
import forge.game.phase.CombatUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;


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
     * getTotalFirstStrikeBlockPower.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
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
     *            a {@link forge.Card} object.
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
     *            a {@link forge.Card} object.
     * @param attacked
     *            a {@link forge.game.player.Player} object.
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @return a int.
     */
    public static int damageIfUnblocked(final Card attacker, final Player attacked, final Combat combat) {
        int damage = attacker.getNetCombatDamage();
        int sum = 0;
        if (!attacked.canLoseLife()) {
            return 0;
        }
        damage += ComputerUtilCombat.predictPowerBonusOfAttacker(attacker, null, combat, false);
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
     *            a {@link forge.Card} object.
     * @param attacked
     *            a {@link forge.game.player.Player} object.
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
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
     *            a {@link forge.CardList} object.
     * @param attacked
     *            a {@link forge.game.player.Player} object.
     * @return a int.
     */
    public static int sumDamageIfUnblocked(final List<Card> attackers, final Player attacked) {
        int sum = 0;
        for (final Card attacker : attackers) {
            sum += ComputerUtilCombat.damageIfUnblocked(attacker, attacked, null);
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
     *            a {@link forge.CardList} object.
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
     *            a {@link forge.game.phase.Combat} object.
     * @return a int.
     */
    public static int lifeThatWouldRemain(final Player ai, final Combat combat) {

        int damage = 0;

        final List<Card> attackers = combat.getAttackersByDefenderSlot(0);
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
     *            a {@link forge.game.phase.Combat} object.
     * @return a int.
     */
    public static int resultingPoison(final Player ai, final Combat combat) {

        int poison = 0;

        final List<Card> attackers = combat.getAttackersByDefenderSlot(0);
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

    // Checks if the life of the attacked Player/Planeswalker is in danger
    /**
     * <p>
     * lifeInDanger.
     * </p>
     * 
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @return a boolean.
     */
    public static boolean lifeInDanger(final Player ai, final Combat combat) {
        // life in danger only cares about the player's life. Not Planeswalkers' life
        if (ai.cantLose()) {
            return false;
        }

        // check for creatures that must be blocked
        final List<Card> attackers = combat.getAttackersByDefenderSlot(0);

        for (final Card attacker : attackers) {

            final List<Card> blockers = combat.getBlockers(attacker);

            if (blockers.size() == 0) {
                if (!attacker.getSVar("MustBeBlocked").equals("")) {
                    return true;
                }
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
     *            a {@link forge.game.phase.Combat} object.
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
     *            a {@link forge.game.phase.Combat} object.
     * @return a boolean.
     */
    public static boolean lifeInSeriousDanger(final Player ai, final Combat combat) {
        // life in danger only cares about the player's life. Not about a
        // Planeswalkers life
        if (ai.cantLose()) {
            return false;
        }

        // check for creatures that must be blocked
        final List<Card> attackers = combat.getAttackersByDefenderSlot(0);

        for (final Card attacker : attackers) {

            final List<Card> blockers = combat.getBlockers(attacker);

            if (blockers.size() == 0) {
                if (!attacker.getSVar("MustBeBlocked").equals("")) {
                    return true;
                }
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
     *            a {@link forge.Card} object.
     * @param defenders
     *            a {@link forge.CardList} object.
     * @return a int.
     */
    public static int totalDamageOfBlockers(final Card attacker, final List<Card> defenders) {
        int damage = 0;

        for (final Card defender : defenders) {
            damage += ComputerUtilCombat.dealsDamageAsBlocker(attacker, defender);
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
     *            a {@link forge.Card} object.
     * @param defender
     *            a {@link forge.Card} object.
     * @return a int.
     */
    public static int dealsDamageAsBlocker(final Card attacker, final Card defender) {

        if (attacker.getName().equals("Sylvan Basilisk") && !defender.hasKeyword("Indestructible")) {
            return 0;
        }

        int flankingMagnitude = 0;
        if (attacker.hasKeyword("Flanking") && !defender.hasKeyword("Flanking")) {

            flankingMagnitude = attacker.getAmountOfKeyword("Flanking");

            if (flankingMagnitude >= defender.getNetDefense()) {
                return 0;
            }
            if ((flankingMagnitude >= (defender.getNetDefense() - defender.getDamage()))
                    && !defender.hasKeyword("Indestructible")) {
                return 0;
            }

        } // flanking
        if (attacker.hasKeyword("Indestructible") && !(defender.hasKeyword("Wither") || defender.hasKeyword("Infect"))) {
            return 0;
        }

        int defenderDamage = defender.getNetAttack() + ComputerUtilCombat.predictPowerBonusOfBlocker(attacker, defender, true);
        if (Singletons.getModel().getGame().getStaticEffects().getGlobalRuleChange(GlobalRuleChange.toughnessAssignsDamage)) {
            defenderDamage = defender.getNetDefense() + ComputerUtilCombat.predictToughnessBonusOfBlocker(attacker, defender, true);
        }

        // consider static Damage Prevention
        defenderDamage = predictDamageTo(attacker, defenderDamage, defender, true);

        if (defender.hasKeyword("Double Strike")) {
            defenderDamage += predictDamageTo(attacker, defenderDamage, defender, true);
        }

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
     *            a {@link forge.Card} object.
     * @param defenders
     *            a {@link forge.CardList} object.
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
     *            a {@link forge.Card} object.
     * @param defender
     *            a {@link forge.Card} object.
     * @return a int.
     */
    public static int shieldDamage(final Card attacker, final Card defender) {

        int flankingMagnitude = 0;
        if (attacker.hasKeyword("Flanking") && !defender.hasKeyword("Flanking")) {

            flankingMagnitude = attacker.getAmountOfKeyword("Flanking");

            if (flankingMagnitude >= defender.getNetDefense()) {
                return 0;
            }
            if ((flankingMagnitude >= (defender.getNetDefense() - defender.getDamage()))
                    && !defender.hasKeyword("Indestructible")) {
                return 0;
            }

        } // flanking

        final int defBushidoMagnitude = defender.getKeywordMagnitude("Bushido");

        final int defenderDefense = (defender.getLethalDamage() - flankingMagnitude) + defBushidoMagnitude;

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
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean combatantWouldBeDestroyed(Player ai, final Card combatant) {

        if (combatant.isAttacking()) {
            return ComputerUtilCombat.attackerWouldBeDestroyed(ai, combatant);
        }
        if (combatant.isBlocking()) {
            return ComputerUtilCombat.blockerWouldBeDestroyed(ai, combatant);
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
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean attackerWouldBeDestroyed(Player ai, final Card attacker) {
        final List<Card> blockers = Singletons.getModel().getGame().getCombat().getBlockers(attacker);

        for (final Card defender : blockers) {
            if (ComputerUtilCombat.canDestroyAttacker(ai, attacker, defender, Singletons.getModel().getGame().getCombat(), true)
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
     *            a {@link forge.Card} object.
     * @param defender
     *            a {@link forge.Card} object.
     * @param trigger
     *            a {@link forge.card.trigger.Trigger} object.
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @return a boolean.
     */
    public static boolean combatTriggerWillTrigger(final Card attacker, final Card defender, final Trigger trigger,
            Combat combat) {
        final HashMap<String, String> trigParams = trigger.getMapParams();
        boolean willTrigger = false;
        final Card source = trigger.getHostCard();
        if (combat == null) {
            combat = Singletons.getModel().getGame().getCombat();
        }

        if (!trigger.zonesCheck(Singletons.getModel().getGame().getZoneOf(trigger.getHostCard()))) {
            return false;
        }
        if (!trigger.requirementsCheck()) {
            return false;
        }

        TriggerType mode = trigger.getMode();
        if (mode == TriggerType.Attacks) {
            willTrigger = true;
            if (attacker.isAttacking()) {
                return false; // The trigger should have triggered already
            }
            if (trigParams.containsKey("ValidCard")) {
                if (!TriggerReplacementBase.matchesValid(attacker, trigParams.get("ValidCard").split(","), source)
                        && !(combat.isAttacking(source) && TriggerReplacementBase.matchesValid(source,
                                trigParams.get("ValidCard").split(","), source)
                            && !trigParams.containsKey("Alone"))) {
                    return false;
                }
            }
        }

        // defender == null means unblocked
        if ((defender == null) && mode == TriggerType.AttackerUnblocked) {
            willTrigger = true;
            if (trigParams.containsKey("ValidCard")) {
                if (!TriggerReplacementBase.matchesValid(attacker, trigParams.get("ValidCard").split(","), source)) {
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
                if (!TriggerReplacementBase.matchesValid(attacker, trigParams.get("ValidBlocked").split(","), source)) {
                    return false;
                }
            }
            if (trigParams.containsKey("ValidCard")) {
                if (!TriggerReplacementBase.matchesValid(defender, trigParams.get("ValidCard").split(","), source)) {
                    return false;
                }
            }
        } else if (mode == TriggerType.AttackerBlocked) {
            willTrigger = true;
            if (trigParams.containsKey("ValidBlocker")) {
                if (!TriggerReplacementBase.matchesValid(defender, trigParams.get("ValidBlocker").split(","), source)) {
                    return false;
                }
            }
            if (trigParams.containsKey("ValidCard")) {
                if (!TriggerReplacementBase.matchesValid(attacker, trigParams.get("ValidCard").split(","), source)) {
                    return false;
                }
            }
        } else if (mode == TriggerType.DamageDone) {
            willTrigger = true;
            if (trigParams.containsKey("ValidSource")) {
                if (TriggerReplacementBase.matchesValid(defender, trigParams.get("ValidSource").split(","), source)
                        && defender.getNetCombatDamage() > 0
                        && (!trigParams.containsKey("ValidTarget")
                                || TriggerReplacementBase.matchesValid(attacker, trigParams.get("ValidTarget").split(","), source))) {
                    return true;
                }
                if (TriggerReplacementBase.matchesValid(attacker, trigParams.get("ValidSource").split(","), source)
                        && attacker.getNetCombatDamage() > 0
                        && (!trigParams.containsKey("ValidTarget")
                                || TriggerReplacementBase.matchesValid(defender, trigParams.get("ValidTarget").split(","), source))) {
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
     *            a {@link forge.Card} object.
     * @param defender
     *            a {@link forge.Card} object.
     * @return a int.
     */
    public static int predictPowerBonusOfBlocker(final Card attacker, final Card defender, boolean withoutAbilities) {
        int power = 0;

        if (attacker.hasKeyword("Flanking") && !defender.hasKeyword("Flanking")) {
            power -= attacker.getAmountOfKeyword("Flanking");
        }

        // if the attacker has first strike and wither the blocker will deal
        // less damage than expected
        if ((attacker.hasKeyword("First Strike") || attacker.hasKeyword("Double Strike"))
                && (attacker.hasKeyword("Wither") || attacker.hasKeyword("Infect"))
                && !(defender.hasKeyword("First Strike") || defender.hasKeyword("Double Strike") || defender
                        .hasKeyword("CARDNAME can't have counters placed on it."))) {
            power -= attacker.getNetCombatDamage();
        }

        power += defender.getKeywordMagnitude("Bushido");

        // look out for continuous static abilities that only care for blocking
        // creatures
        final List<Card> cardList = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
        for (final Card card : cardList) {
            for (final StaticAbility stAb : card.getStaticAbilities()) {
                final HashMap<String, String> params = stAb.getMapParams();
                if (!params.get("Mode").equals("Continuous")) {
                    continue;
                }
                if (!params.containsKey("Affected") || !params.get("Affected").contains("blocking")) {
                    continue;
                }
                final String valid = params.get("Affected").replace("blocking", "Creature");
                if (!defender.isValid(valid, card.getController(), card)) {
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

        final ArrayList<Trigger> theTriggers = new ArrayList<Trigger>();
        for (Card card : Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield)) {
            theTriggers.addAll(card.getTriggers());
        }
        theTriggers.addAll(attacker.getTriggers());
        for (final Trigger trigger : theTriggers) {
            final HashMap<String, String> trigParams = trigger.getMapParams();
            final Card source = trigger.getHostCard();

            if (!ComputerUtilCombat.combatTriggerWillTrigger(attacker, defender, trigger, null)
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
            final List<Card> list = AbilityFactory.getDefinedCards(source, abilityParams.get("Defined"), null);
            if (abilityParams.containsKey("Defined") && abilityParams.get("Defined").equals("TriggeredBlocker")) {
                list.add(defender);
            }
            if (list.isEmpty()) {
                continue;
            }
            if (!list.contains(defender)) {
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
        for (SpellAbility ability : defender.getAllSpellAbilities()) {
            if (!(ability instanceof AbilityActivated) || ability.getPayCosts() == null) {
                continue;
            }
            if (ability.getApi() != ApiType.Pump) {
                continue;
            }

            if (!ability.hasParam("NumAtt")) {
                continue;
            }

            if (ComputerUtilCost.canPayCost(ability, defender.getController())) {
                int pBonus = AbilityFactory.calculateAmount(ability.getSourceCard(), ability.getParam("NumAtt"), ability);
                if (pBonus > 0) {
                    power += pBonus;
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
     *            a {@link forge.Card} object.
     * @param defender
     *            a {@link forge.Card} object.
     * @return a int.
     */
    public static int predictToughnessBonusOfBlocker(final Card attacker, final Card defender, boolean withoutAbilities) {
        int toughness = 0;

        if (attacker.hasKeyword("Flanking") && !defender.hasKeyword("Flanking")) {
            toughness -= attacker.getAmountOfKeyword("Flanking");
        }

        toughness += defender.getKeywordMagnitude("Bushido");

        final ArrayList<Trigger> theTriggers = new ArrayList<Trigger>();
        for (Card card : Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield)) {
            theTriggers.addAll(card.getTriggers());
        }
        theTriggers.addAll(attacker.getTriggers());
        for (final Trigger trigger : theTriggers) {
            final HashMap<String, String> trigParams = trigger.getMapParams();
            final Card source = trigger.getHostCard();

            if (!ComputerUtilCombat.combatTriggerWillTrigger(attacker, defender, trigger, null)
                    || !trigParams.containsKey("Execute")) {
                continue;
            }
            final String ability = source.getSVar(trigParams.get("Execute"));
            final Map<String, String> abilityParams = AbilityFactory.getMapParams(ability);

            // DealDamage triggers
            if ((abilityParams.containsKey("AB") && abilityParams.get("AB").equals("DealDamage"))
                    || (abilityParams.containsKey("DB") && abilityParams.get("DB").equals("DealDamage"))) {
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
                toughness -= predictDamageTo(defender, damage, 0, source, false);
                continue;
            }

            // Pump triggers
            if (abilityParams.containsKey("AB") && !abilityParams.get("AB").equals("Pump")) {
                continue;
            }
            if (abilityParams.containsKey("DB") && !abilityParams.get("DB").equals("Pump")) {
                continue;
            }
            if (abilityParams.containsKey("ValidTgts") || abilityParams.containsKey("Tgt")) {
                continue; // targeted pumping not supported
            }
            final List<Card> list = AbilityFactory.getDefinedCards(source, abilityParams.get("Defined"), null);
            if (abilityParams.containsKey("Defined") && abilityParams.get("Defined").equals("TriggeredBlocker")) {
                list.add(defender);
            }
            if (list.isEmpty()) {
                continue;
            }
            if (!list.contains(defender)) {
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
        for (SpellAbility ability : defender.getAllSpellAbilities()) {
            if (!(ability instanceof AbilityActivated) || ability.getPayCosts() == null) {
                continue;
            }

            if (ability.getApi() != ApiType.Pump || !ability.hasParam("NumDef")) {
                continue;
            }

            if (ComputerUtilCost.canPayCost(ability, defender.getController())) {
                int tBonus = AbilityFactory.calculateAmount(ability.getSourceCard(), ability.getParam("NumDef"), ability);
                if (tBonus > 0) {
                    toughness += tBonus;
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
     *            a {@link forge.Card} object.
     * @param defender
     *            a {@link forge.Card} object.
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @return a int.
     */
    public static int predictPowerBonusOfAttacker(final Card attacker, final Card defender, final Combat combat
            , boolean withoutAbilities) {
        int power = 0;

        power += attacker.getKeywordMagnitude("Bushido");
        //check Exalted only for the first attacker
        if (combat != null && combat.getAttackers().isEmpty()) {
            for (Card card : attacker.getController().getCardsIn(ZoneType.Battlefield)) {
                power += card.getKeywordAmount("Exalted");
            }
        }

        final ArrayList<Trigger> theTriggers = new ArrayList<Trigger>();
        for (Card card : Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield)) {
            theTriggers.addAll(card.getTriggers());
        }
        // if the defender has first strike and wither the attacker will deal
        // less damage than expected
        if (null != defender) {
            if ((defender.hasKeyword("First Strike") || defender.hasKeyword("Double Strike"))
                    && (defender.hasKeyword("Wither") || defender.hasKeyword("Infect"))
                    && !(attacker.hasKeyword("First Strike") || attacker.hasKeyword("Double Strike") || attacker
                            .hasKeyword("CARDNAME can't have counters placed on it."))) {
                power -= defender.getNetCombatDamage();
            }
            theTriggers.addAll(defender.getTriggers());
        }

        // look out for continuous static abilities that only care for attacking
        // creatures
        final List<Card> cardList = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
        for (final Card card : cardList) {
            for (final StaticAbility stAb : card.getStaticAbilities()) {
                final HashMap<String, String> params = stAb.getMapParams();
                if (!params.get("Mode").equals("Continuous")) {
                    continue;
                }
                if (!params.containsKey("Affected") || !params.get("Affected").contains("attacking")) {
                    continue;
                }
                final String valid = params.get("Affected").replace("attacking", "Creature");
                if (!attacker.isValid(valid, card.getController(), card)) {
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
            final HashMap<String, String> trigParams = trigger.getMapParams();
            final Card source = trigger.getHostCard();

            if (!ComputerUtilCombat.combatTriggerWillTrigger(attacker, defender, trigger, combat)
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
                list = AbilityFactory.getDefinedCards(source, abilityParams.get("Defined"), null);
            }
            if (abilityParams.containsKey("Defined") && abilityParams.get("Defined").equals("TriggeredAttacker")) {
                list.add(attacker);
            }
            if (abilityParams.containsKey("ValidCards")) {
                if (attacker.isValid(abilityParams.get("ValidCards").split(","), source.getController(), source)
                        || attacker.isValid(abilityParams.get("ValidCards").replace("attacking+", "").split(","),
                                source.getController(), source)) {
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
            if (ability.getApi() != ApiType.Pump) {
                continue;
            }

            if (!ability.hasParam("NumAtt")) {
                continue;
            }

            if (!ability.getPayCosts().hasTapCost() && ComputerUtilCost.canPayCost(ability, attacker.getController())) {
                int pBonus = AbilityFactory.calculateAmount(ability.getSourceCard(), ability.getParam("NumAtt"), ability);
                if (pBonus > 0) {
                    power += pBonus;
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
     *            a {@link forge.Card} object.
     * @param defender
     *            a {@link forge.Card} object.
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @return a int.
     */
    public static int predictToughnessBonusOfAttacker(final Card attacker, final Card defender, final Combat combat
            , boolean withoutAbilities) {
        int toughness = 0;

        //check Exalted only for the first attacker
        if (combat != null && combat.getAttackers().isEmpty()) {
            for (Card card : attacker.getController().getCardsIn(ZoneType.Battlefield)) {
                toughness += card.getKeywordAmount("Exalted");
            }
        }

        final ArrayList<Trigger> theTriggers = new ArrayList<Trigger>();
        for (Card card : Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield)) {
            theTriggers.addAll(card.getTriggers());
        }
        if (defender != null) {
            toughness += attacker.getKeywordMagnitude("Bushido");
            theTriggers.addAll(defender.getTriggers());
        }

        // look out for continuous static abilities that only care for attacking
        // creatures
        final List<Card> cardList = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
        for (final Card card : cardList) {
            for (final StaticAbility stAb : card.getStaticAbilities()) {
                final HashMap<String, String> params = stAb.getMapParams();
                if (!params.get("Mode").equals("Continuous")) {
                    continue;
                }
                if (!params.containsKey("Affected") || !params.get("Affected").contains("attacking")) {
                    continue;
                }
                final String valid = params.get("Affected").replace("attacking", "Creature");
                if (!attacker.isValid(valid, card.getController(), card)) {
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
            final HashMap<String, String> trigParams = trigger.getMapParams();
            final Card source = trigger.getHostCard();

            if (!ComputerUtilCombat.combatTriggerWillTrigger(attacker, defender, trigger, combat)
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
                list = AbilityFactory.getDefinedCards(source, abilityParams.get("Defined"), null);
            }
            if (abilityParams.containsKey("Defined") && abilityParams.get("Defined").equals("TriggeredAttacker")) {
                list.add(attacker);
            }
            if (abilityParams.containsKey("ValidCards")) {
                if (attacker.isValid(abilityParams.get("ValidCards").split(","), source.getController(), source)
                        || attacker.isValid(abilityParams.get("ValidCards").replace("attacking+", "").split(","),
                                source.getController(), source)) {
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

            if (ability.getApi() != ApiType.Pump || !ability.hasParam("NumDef")) {
                continue;
            }

            if (!ability.getPayCosts().hasTapCost() && ComputerUtilCost.canPayCost(ability, attacker.getController())) {
                int tBonus = AbilityFactory.calculateAmount(ability.getSourceCard(), ability.getParam("NumDef"), ability);
                if (tBonus > 0) {
                    toughness += tBonus;
                }
            }
        }
        return toughness;
    }

    // Sylvan Basilisk and friends
    /**
     * <p>
     * checkDestroyBlockerTrigger.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @param defender
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean checkDestroyBlockerTrigger(final Card attacker, final Card defender) {
        final ArrayList<Trigger> theTriggers = new ArrayList<Trigger>();
        for (Card card : Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield)) {
            theTriggers.addAll(card.getTriggers());
        }
        for (Trigger trigger : theTriggers) {
            HashMap<String, String> trigParams = trigger.getMapParams();
            final Card source = trigger.getHostCard();

            if (!ComputerUtilCombat.combatTriggerWillTrigger(attacker, defender, trigger, null)) {
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
                if (abilityParams.get("Defined").equals("Self") && source.equals(defender)) {
                    return true;
                }
                if (abilityParams.get("Defined").equals("TriggeredTarget") && source.equals(attacker)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Cockatrice and friends
    /**
     * <p>
     * checkDestroyBlockerTrigger.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @param defender
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean checkDestroyAttackerTrigger(final Card attacker, final Card defender) {
        final ArrayList<Trigger> theTriggers = new ArrayList<Trigger>();
        for (Card card : Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield)) {
            theTriggers.addAll(card.getTriggers());
        }
        for (Trigger trigger : theTriggers) {
            HashMap<String, String> trigParams = trigger.getMapParams();
            final Card source = trigger.getHostCard();

            if (!ComputerUtilCombat.combatTriggerWillTrigger(attacker, defender, trigger, null)) {
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
                if (abilityParams.get("Defined").equals("TriggeredAttacker")) {
                    return true;
                }
                if (abilityParams.get("Defined").equals("Self") && source.equals(attacker)) {
                    return true;
                }
                if (abilityParams.get("Defined").equals("TriggeredTarget") && source.equals(defender)) {
                    return true;
                }
            }
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
     *            a {@link forge.Card} object.
     * @param defender
     *            a {@link forge.Card} object.
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @param withoutAbilities
     *            a boolean.
     * @return a boolean.
     */
    public static boolean canDestroyAttacker(Player ai, final Card attacker, final Card defender, final Combat combat,
            final boolean withoutAbilities) {

        if (attacker.getName().equals("Sylvan Basilisk") && !defender.hasKeyword("Indestructible")) {
            return false;
        }

        int flankingMagnitude = 0;
        if (attacker.hasKeyword("Flanking") && !defender.hasKeyword("Flanking")) {

            flankingMagnitude = attacker.getAmountOfKeyword("Flanking");

            if (flankingMagnitude >= defender.getNetDefense()) {
                return false;
            }
            if ((flankingMagnitude >= (defender.getNetDefense() - defender.getDamage()))
                    && !defender.hasKeyword("Indestructible")) {
                return false;
            }
        } // flanking

        if (((attacker.hasKeyword("Indestructible") || (ComputerUtil.canRegenerate(ai, attacker) && !withoutAbilities)) && !(defender
                .hasKeyword("Wither") || defender.hasKeyword("Infect")))
                || (attacker.hasKeyword("Persist") && !attacker.canHaveCountersPlacedOnIt(CounterType.M1M1) && (attacker
                        .getCounters(CounterType.M1M1) == 0))
                || (attacker.hasKeyword("Undying") && !attacker.canHaveCountersPlacedOnIt(CounterType.P1P1) && (attacker
                        .getCounters(CounterType.P1P1) == 0))) {
            return false;
        }
        if (checkDestroyAttackerTrigger(attacker, defender) && !attacker.hasKeyword("Indestructible")) {
            return true;
        }

        int defenderDamage = defender.getNetAttack()
                + ComputerUtilCombat.predictPowerBonusOfBlocker(attacker, defender, withoutAbilities);
        int attackerDamage = attacker.getNetAttack()
                + ComputerUtilCombat.predictPowerBonusOfAttacker(attacker, defender, combat, withoutAbilities);
        if (Singletons.getModel().getGame().getStaticEffects().getGlobalRuleChange(GlobalRuleChange.toughnessAssignsDamage)) {
            defenderDamage = defender.getNetDefense()
                    + ComputerUtilCombat.predictToughnessBonusOfBlocker(attacker, defender, withoutAbilities);
            attackerDamage = attacker.getNetDefense()
                    + ComputerUtilCombat.predictToughnessBonusOfAttacker(attacker, defender, combat, withoutAbilities);
        }

        int possibleDefenderPrevention = 0;
        int possibleAttackerPrevention = 0;
        if (!withoutAbilities) {
            possibleDefenderPrevention = ComputerUtil.possibleDamagePrevention(defender);
            possibleAttackerPrevention = ComputerUtil.possibleDamagePrevention(attacker);
        }

        // consider Damage Prevention/Replacement
        defenderDamage = predictDamageTo(attacker, defenderDamage, possibleAttackerPrevention, defender, true);
        attackerDamage = predictDamageTo(defender, attackerDamage, possibleDefenderPrevention, attacker, true);

        final int defenderLife = ComputerUtilCombat.getDamageToKill(defender)
                + ComputerUtilCombat.predictToughnessBonusOfBlocker(attacker, defender, withoutAbilities);
        final int attackerLife = ComputerUtilCombat.getDamageToKill(attacker)
                + ComputerUtilCombat.predictToughnessBonusOfAttacker(attacker, defender, combat, withoutAbilities);

        if (defender.hasKeyword("Double Strike")) {
            if (defenderDamage > 0 && (defender.hasKeyword("Deathtouch")
                        || attacker.hasStartOfKeyword("When CARDNAME is dealt damage, destroy it."))) {
                return true;
            }
            if (defenderDamage >= attackerLife) {
                return true;
            }

            // Attacker may kill the blocker before he can deal normal
            // (secondary) damage
            if ((attacker.hasKeyword("Double Strike") || attacker.hasKeyword("First Strike"))
                    && !defender.hasKeyword("Indestructible")) {
                if (attackerDamage >= defenderLife) {
                    return false;
                }
                if (attackerDamage > 0 && (attacker.hasKeyword("Deathtouch")
                        || defender.hasStartOfKeyword("When CARDNAME is dealt damage, destroy it."))) {
                    return false;
                }
            }
            if (attackerLife <= (2 * defenderDamage)) {
                return true;
            }
        } // defender double strike

        else { // no double strike for defender
               // Attacker may kill the blocker before he can deal any damage
            if ((attacker.hasKeyword("Double Strike") || attacker.hasKeyword("First Strike"))
                    && !defender.hasKeyword("Indestructible")
                    && !defender.hasKeyword("First Strike")) {

                if (attackerDamage >= defenderLife) {
                    return false;
                }
                if (attackerDamage > 0 && (attacker.hasKeyword("Deathtouch")
                        || defender.hasStartOfKeyword("When CARDNAME is dealt damage, destroy it."))) {
                    return false;
                }
            }

            if (defenderDamage > 0 && (defender.hasKeyword("Deathtouch")
                        || attacker.hasStartOfKeyword("When CARDNAME is dealt damage, destroy it."))) {
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
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean blockerWouldBeDestroyed(Player ai, final Card blocker) {
        // TODO THis function only checks if a single attacker at a time would destroy a blocker
        // This needs to expand to tally up damage
        final List<Card> attackers = Singletons.getModel().getGame().getCombat().getAttackersBlockedBy(blocker);

        for (Card attacker : attackers) {
            if (ComputerUtilCombat.canDestroyBlocker(ai, blocker, attacker, Singletons.getModel().getGame().getCombat(), true)
                    && !(attacker.hasKeyword("Wither") || attacker.hasKeyword("Infect"))) {
                return true;
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
     * @param defender
     *            a {@link forge.Card} object.
     * @param attacker
     *            a {@link forge.Card} object.
     * @param combat
     *            a {@link forge.game.phase.Combat} object.
     * @param withoutAbilities
     *            a boolean.
     * @return a boolean.
     */
    public static boolean canDestroyBlocker(Player ai, final Card defender, final Card attacker, final Combat combat,
            final boolean withoutAbilities) {

        int flankingMagnitude = 0;
        if (attacker.hasKeyword("Flanking") && !defender.hasKeyword("Flanking")) {

            flankingMagnitude = attacker.getAmountOfKeyword("Flanking");

            if (flankingMagnitude >= defender.getNetDefense()) {
                return true;
            }
            if ((flankingMagnitude >= ComputerUtilCombat.getDamageToKill(defender)) && !defender.hasKeyword("Indestructible")) {
                return true;
            }
        } // flanking

        if (((defender.hasKeyword("Indestructible") || (ComputerUtil.canRegenerate(ai, defender) && !withoutAbilities)) && !(attacker
                .hasKeyword("Wither") || attacker.hasKeyword("Infect")))
                || (defender.hasKeyword("Persist") && !defender.canHaveCountersPlacedOnIt(CounterType.M1M1) && (defender
                        .getCounters(CounterType.M1M1) == 0))
                || (defender.hasKeyword("Undying") && !defender.canHaveCountersPlacedOnIt(CounterType.P1P1) && (defender
                        .getCounters(CounterType.P1P1) == 0))) {
            return false;
        }

        if (checkDestroyBlockerTrigger(attacker, defender) && !defender.hasKeyword("Indestructible")) {
            return true;
        }

        int defenderDamage = defender.getNetAttack()
                + ComputerUtilCombat.predictPowerBonusOfBlocker(attacker, defender, withoutAbilities);
        int attackerDamage = attacker.getNetAttack()
                + ComputerUtilCombat.predictPowerBonusOfAttacker(attacker, defender, combat, withoutAbilities);
        if (Singletons.getModel().getGame().getStaticEffects().getGlobalRuleChange(GlobalRuleChange.toughnessAssignsDamage)) {
            defenderDamage = defender.getNetDefense()
                    + ComputerUtilCombat.predictToughnessBonusOfBlocker(attacker, defender, withoutAbilities);
            attackerDamage = attacker.getNetDefense()
                    + ComputerUtilCombat.predictToughnessBonusOfAttacker(attacker, defender, combat, withoutAbilities);
        }

        int possibleDefenderPrevention = 0;
        int possibleAttackerPrevention = 0;
        if (!withoutAbilities) {
            possibleDefenderPrevention = ComputerUtil.possibleDamagePrevention(defender);
            possibleAttackerPrevention = ComputerUtil.possibleDamagePrevention(attacker);
        }

        // consider Damage Prevention/Replacement
        defenderDamage = predictDamageTo(attacker, defenderDamage, possibleAttackerPrevention, defender, true);
        attackerDamage = predictDamageTo(defender, attackerDamage, possibleDefenderPrevention, attacker, true);

        if (combat != null) {
            for (Card atkr : combat.getAttackersBlockedBy(defender)) {
                if (!atkr.equals(attacker)) {
                    attackerDamage += predictDamageTo(defender, atkr.getNetCombatDamage(), 0, atkr, true);
                }
            }
        }

        final int defenderLife = ComputerUtilCombat.getDamageToKill(defender)
                + ComputerUtilCombat.predictToughnessBonusOfBlocker(attacker, defender, withoutAbilities);
        final int attackerLife = ComputerUtilCombat.getDamageToKill(attacker)
                + ComputerUtilCombat.predictToughnessBonusOfAttacker(attacker, defender, combat, withoutAbilities);

        if (attacker.hasKeyword("Double Strike")) {
            if (attackerDamage > 0 && (attacker.hasKeyword("Deathtouch")
                    || defender.hasStartOfKeyword("When CARDNAME is dealt damage, destroy it."))) {
                return true;
            }
            if (attackerDamage >= defenderLife) {
                return true;
            }

            // Attacker may kill the blocker before he can deal normal
            // (secondary) damage
            if ((defender.hasKeyword("Double Strike") || defender.hasKeyword("First Strike"))
                    && !attacker.hasKeyword("Indestructible")) {
                if (defenderDamage >= attackerLife) {
                    return false;
                }
                if (defenderDamage > 0 && (defender.hasKeyword("Deathtouch")
                        || attacker.hasStartOfKeyword("When CARDNAME is dealt damage, destroy it."))) {
                    return false;
                }
            }
            if (defenderLife <= (2 * attackerDamage)) {
                return true;
            }
        } // attacker double strike

        else { // no double strike for attacker
               // Defender may kill the attacker before he can deal any damage
            if (defender.hasKeyword("Double Strike")
                    || (defender.hasKeyword("First Strike") && !attacker.hasKeyword("Indestructible") && !attacker
                            .hasKeyword("First Strike"))) {

                if (defenderDamage >= attackerLife) {
                    return false;
                }
                if (defenderDamage > 0 && (defender.hasKeyword("Deathtouch")
                        || attacker.hasStartOfKeyword("When CARDNAME is dealt damage, destroy it."))) {
                    return false;
                }
            }

            if (attackerDamage > 0 && (attacker.hasKeyword("Deathtouch")
                    || defender.hasStartOfKeyword("When CARDNAME is dealt damage, destroy it."))) {
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
     *            a {@link forge.Card} object.
     * @param block
     *            a {@link forge.CardList} object.
     * @param damage
     *            a int.
     */
    public static void distributeAIDamage(final Card attacker, final List<Card> block, int damage, Combat combat) {
        final Card c = attacker;
    
        if (attacker.hasKeyword("You may have CARDNAME assign its combat damage as though it weren't blocked.")
                || attacker.hasKeyword("CARDNAME assigns its combat damage as though it weren't blocked.")) {
            combat.addDefendingDamage(damage, attacker);
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
    
                damageNeeded = ComputerUtilCombat.getEnoughDamageToKill(blocker, damage, attacker, true);
    
                if (damageNeeded > damage) {
                    damageNeeded = Math.min(blocker.getLethalDamage(), damage);
                } else {
                    damageNeeded = Math.max(blocker.getLethalDamage(), damageNeeded);
                }
    
                final int trample = damage - damageNeeded;
    
                // If Extra trample damage, assign to defending
                // player/planeswalker
                if (0 < trample) {
                    combat.addDefendingDamage(trample, attacker);
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
                final int enoughDamageToKill = ComputerUtilCombat.getEnoughDamageToKill(b, damage, attacker, true);
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
                    combat.addDefendingDamage(damage, c);
                }
            }
        }
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
     *            a {@link forge.Card} object.
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
     *            a {@link forge.Card} object.
     * @param isCombat
     *            a boolean.
     * @param noPrevention
     *            a boolean.
     * @return a int.
     */
    public static final int getEnoughDamageToKill(final Card c, final int maxDamage, final Card source, final boolean isCombat,
            final boolean noPrevention) {
        final int killDamage = ComputerUtilCombat.getDamageToKill(c);

        if (c.hasKeyword("Indestructible") || (c.getShield() > 0)) {
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
        int killDamage = c.getLethalDamage() + c.getPreventNextDamage();
        if ((killDamage > c.getPreventNextDamage())
                && c.hasStartOfKeyword("When CARDNAME is dealt damage, destroy it.")) {
            killDamage = 1 + c.getPreventNextDamage();
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
     *            a {@link forge.Card} object.
     * @param isCombat
     *            a boolean.
     * @return a int.
     */

    public final static int predictDamageTo(final Player target, final int damage, final Card source, final boolean isCombat) {

        final GameState game = Singletons.getModel().getGame();
        int restDamage = damage;

        restDamage = target.staticReplaceDamage(restDamage, source, isCombat);

        // Predict replacement effects
        for (final Card ca : game.getCardsIn(ZoneType.Battlefield)) {
            for (final ReplacementEffect re : ca.getReplacementEffects()) {
                HashMap<String, String> params = re.getMapParams();
                if (!"DamageDone".equals(params.get("Event")) || !params.containsKey("PreventionEffect")) {
                    continue;
                }
                if (params.containsKey("ValidSource")
                        && !source.isValid(params.get("ValidSource"), ca.getController(), ca)) {
                    continue;
                }
                if (params.containsKey("ValidTarget")
                        && !target.isValid(params.get("ValidTarget"), ca.getController(), ca)) {
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

        restDamage = target.staticDamagePrevention(restDamage, source, isCombat);

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
     *            a {@link forge.Card} object.
     * @param isCombat
     *            a boolean.
     * @return a int.
     */
    // This function helps the AI calculate the actual amount of damage an
    // effect would deal
    public final static int predictDamageTo(final Card target, final int damage, final Card source, final boolean isCombat) {

        int restDamage = damage;

        restDamage = target.staticReplaceDamage(restDamage, source, isCombat);
        restDamage = target.staticDamagePrevention(restDamage, source, isCombat);

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
     *            a {@link forge.Card} object.
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

}


