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

import java.util.ArrayList;
import java.util.List;

import forge.game.staticability.StaticAbilityAssignCombatDamageAsUnblocked;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.ai.ability.AnimateAi;
import forge.card.CardTypeView;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.effects.ProtectEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardUtil;
import forge.game.card.CounterEnumType;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.combat.GlobalAttackRestrictions;
import forge.game.cost.Cost;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityPredicates;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.Expressions;
import forge.util.MyRandom;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;


/**
 * <p>
 * ComputerUtil_Attack2 class.
 * </p>
 *
 * @author Forge
 * @version $Id$
 */
public class AiAttackController {

    // possible attackers and blockers
    private List<Card> attackers;
    private List<Card> blockers;

    private List<Card> oppList; // holds human player creatures
    private List<Card> myList; // holds computer creatures

    private final Player ai;
    private Player defendingOpponent;

    private int aiAggression = 0; // how aggressive the ai is attack will be depending on circumstances
    private final boolean nextTurn; // include creature that can only attack/block next turn

    /**
     * <p>
     * Constructor for ComputerUtil_Attack2.
     * </p>
     *
     */
    public AiAttackController(final Player ai) {
        this(ai, false);
    } // constructor

    public AiAttackController(final Player ai, boolean nextTurn) {
        this.ai = ai;
        defendingOpponent = choosePreferredDefenderPlayer(ai);
        myList = ai.getCreaturesInPlay();
        this.nextTurn = nextTurn;
        refreshCombatants(defendingOpponent);
    } // overloaded constructor to evaluate attackers that should attack next turn

    public AiAttackController(final Player ai, Card attacker) {
        this.ai = ai;
        defendingOpponent = choosePreferredDefenderPlayer(ai);
        this.oppList = getOpponentCreatures(defendingOpponent);
        myList = ai.getCreaturesInPlay();
        this.nextTurn = false;
        this.attackers = new ArrayList<>();
        if (CombatUtil.canAttack(attacker, defendingOpponent)) {
            attackers.add(attacker);
        }
        this.blockers = getPossibleBlockers(oppList, this.attackers, this.nextTurn);
    } // overloaded constructor to evaluate single specified attacker

    private void refreshCombatants(GameEntity defender) {
        this.oppList = getOpponentCreatures(defendingOpponent);
        this.attackers = new ArrayList<>();
        for (Card c : myList) {
            if (canAttackWrapper(c, defender)) {
                attackers.add(c);
            }
        }
        this.blockers = getPossibleBlockers(oppList, this.attackers, this.nextTurn);
    }

    public static List<Card> getOpponentCreatures(final Player defender) {
        List<Card> defenders = defender.getCreaturesInPlay();
        Predicate<Card> canAnimate = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return !c.isTapped() && !c.isCreature() && !c.isPlaneswalker();
            }
        };
        for (Card c : CardLists.filter(defender.getCardsIn(ZoneType.Battlefield), canAnimate)) {
            if (c.isToken() && c.getCopiedPermanent() == null) {
                continue;
            }
            for (SpellAbility sa : Iterables.filter(c.getSpellAbilities(), SpellAbilityPredicates.isApi(ApiType.Animate))) {
                if (ComputerUtilCost.canPayCost(sa, defender, false)
                        && sa.getRestrictions().checkOtherRestrictions(c, sa, defender)) {
                    Card animatedCopy = AnimateAi.becomeAnimated(c, sa);
                    defenders.add(animatedCopy);
                }
            }
        }
        return defenders;
    }

    public void removeBlocker(Card blocker) {
    	this.oppList.remove(blocker);
        this.blockers.remove(blocker);
    }

    private boolean canAttackWrapper(final Card attacker, final GameEntity defender) {
        if (nextTurn) {
            return CombatUtil.canAttackNextTurn(attacker, defender);
        } else {
            return CombatUtil.canAttack(attacker, defender);
        }
    }

    /**
     * Choose opponent for AI to attack here. Expand as necessary.
     * No strategy to secure a second place instead, since Forge has no variant for that
     */
    public static Player choosePreferredDefenderPlayer(Player ai) {
        Player defender = ai.getWeakestOpponent(); //Concentrate on opponent within easy kill range

        // TODO for multiplayer combat avoid players with cantLose or (if not playing infect) cantLoseForZeroOrLessLife and !canLoseLife

        if (defender.getLife() > 8) {
            // TODO connect with evaluateBoardPosition and only fall back to random when no player is the biggest threat by a fair margin

            // TODO should we cache the random for each turn? some functions like shouldPumpCard base their decisions on the assumption who will be attacked

            //Otherwise choose a random opponent to ensure no ganging up on players
            return Aggregates.random(ai.getOpponents());
        }
        return defender;
    }

    /**
     * <p>
     * sortAttackers.
     * </p>
     *
     */
    public final static List<Card> sortAttackers(final List<Card> in) {
        final List<Card> list = new ArrayList<>();

        // Cards with triggers should come first (for Battle Cry)
        for (final Card attacker : in) {
            for (final Trigger trigger : attacker.getTriggers()) {
                if (trigger.getMode() == TriggerType.Attacks) {
                    list.add(attacker);
                    break;
                }
            }
        }

        for (final Card attacker : in) {
            if (!list.contains(attacker)) {
                list.add(attacker);
            }
        }

        return list;
    }

    // Is there any reward for attacking? (for 0/1 creatures there is not)
    /**
     * <p>
     * isEffectiveAttacker.
     * </p>
     *
     * @param attacker
     *            a {@link forge.game.card.Card} object.
     * @param combat
     *            a {@link forge.game.combat.Combat} object.
     * @return a boolean.
     */
    public final boolean isEffectiveAttacker(final Player ai, final Card attacker, final Combat combat, final GameEntity defender) {
        // if the attacker will die when attacking don't attack
        if (attacker.getNetToughness() + ComputerUtilCombat.predictToughnessBonusOfAttacker(attacker, null, combat, true) <= 0) {
            return false;
        }

        // the attacker will die to a triggered ability (e.g. Sarkhan the Masterless)
        for (Card c : ai.getOpponents().getCardsIn(ZoneType.Battlefield)) {
            for (Trigger t : c.getTriggers()) {
                if (t.getMode() == TriggerType.Attacks) {
                    SpellAbility sa = t.ensureAbility();
                    if (sa == null) {
                        continue;
                    }

                    if (sa.getApi() == ApiType.EachDamage && "TriggeredAttacker".equals(sa.getParam("DefinedPlayers"))) {
                        List<Card> valid = CardLists.getValidCards(c.getController().getCreaturesInPlay(), sa.getParam("ValidCards"), c.getController(), c, sa);
                        // TODO: this assumes that 1 damage is dealt per creature. Improve this to check the parameter/X to determine
                        // how much damage is dealt by each of the creatures in the valid list.
                        if (attacker.getNetToughness() <= valid.size()) {
                            return false;
                        }
                    }
                }
            }
        }

        if ("TRUE".equals(attacker.getSVar("HasAttackEffect"))) {
        	return true;
        }

        // Damage opponent if unblocked
        final int dmgIfUnblocked = ComputerUtilCombat.damageIfUnblocked(attacker, defender, combat, true);
        if (dmgIfUnblocked > 0) {
            boolean onlyIfExalted = false;
            if (combat.getAttackers().isEmpty() && ai.countExaltedBonus() > 0
                    && dmgIfUnblocked - ai.countExaltedBonus() == 0) {
                // Make sure we're not counting on the Exalted bonus when the AI is planning to attack with more than one creature
                onlyIfExalted = true;
            }

            if (!onlyIfExalted || this.attackers.size() == 1 || aiAggression == 6 /* 6 is Exalted attack */) {
                return true;
            }
        }
        // Poison opponent if unblocked
        if (defender instanceof Player && ComputerUtilCombat.poisonIfUnblocked(attacker, (Player) defender) > 0) {
            return true;
        }

        // TODO check if that makes sense
        int exalted = ai.countExaltedBonus();
        if (this.attackers.size() == 1 && exalted > 0
                && ComputerUtilCombat.predictDamageTo(defender, exalted, attacker, true) > 0) {
            return true;
        }

        final CardCollectionView controlledByCompy = ai.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES);
        for (final Card c : controlledByCompy) {
            for (final Trigger trigger : c.getTriggers()) {
                if (ComputerUtilCombat.combatTriggerWillTrigger(attacker, null, trigger, combat, this.attackers)) {
                    return true;
                }
            }
        }
        return false;
    }

    public final static List<Card> getPossibleBlockers(final List<Card> blockers, final List<Card> attackers, final boolean nextTurn) {
        return CardLists.filter(blockers, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return canBlockAnAttacker(c, attackers, nextTurn);
            }
        });
    }

    public final static boolean canBlockAnAttacker(final Card c, final List<Card> attackers, final boolean nextTurn) {
        return getCardCanBlockAnAttacker(c, attackers, nextTurn) != null;
    }

    public final static Card getCardCanBlockAnAttacker(final Card c, final List<Card> attackers, final boolean nextTurn) {
        final List<Card> attackerList = new ArrayList<>(attackers);
        if (!c.isCreature()) {
            return null;
        }
        for (final Card attacker : attackerList) {
            if (CombatUtil.canBlock(attacker, c, nextTurn)) {
                return attacker;
            }
        }
        return null;
    }

    // this checks to make sure that the computer player doesn't lose when the human player attacks
    public final List<Card> notNeededAsBlockers(final List<Card> attackers) {
        //check for time walks
        if (ai.getGame().getPhaseHandler().getNextTurn().equals(ai)) {
            return attackers;
        }
        // no need to block (already holding mana to cast fog next turn)
        if (!AiCardMemory.isMemorySetEmpty(ai, AiCardMemory.MemorySet.CHOSEN_FOG_EFFECT)) {
            // Don't send the card that'll do the fog effect to attack, it's unsafe!

            List<Card> toRemove = Lists.newArrayList();
            for (Card c : attackers) {
                if (AiCardMemory.isRememberedCard(ai, c, AiCardMemory.MemorySet.CHOSEN_FOG_EFFECT)) {
                    toRemove.add(c);
                }
            }
            attackers.removeAll(toRemove);
            return attackers;
        }

        final List<Card> vigilantes = new ArrayList<>();
        for (final Card c : myList) {
            if (c.getName().equals("Masako the Humorless")) {
                // "Tapped creatures you control can block as though they were untapped."
                return attackers;
            }
            // no need to block if an effect is in play which untaps all creatures
            // (pseudo-Vigilance akin to Awakening or Prophet of Kruphix)
            if (c.hasKeyword(Keyword.VIGILANCE) || ComputerUtilCard.willUntap(ai, c)) {
                vigilantes.add(c);
            }
        }
        // reduce the search space
        final List<Card> opponentsAttackers = CardLists.filter(ai.getOpponents().getCreaturesInPlay(), new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return !c.hasSVar("EndOfTurnLeavePlay")
                        && (c.toughnessAssignsDamage() || c.getNetCombatDamage() > 0 // performance shortcuts
                                || c.getNetCombatDamage() + ComputerUtilCombat.predictPowerBonusOfAttacker(c, null, null, true) > 0)
                        && ComputerUtilCombat.canAttackNextTurn(c);
            }
        });

        final List<Card> notNeededAsBlockers = new CardCollection(attackers);

        // don't hold back creatures that can't block any of the human creatures
        final List<Card> blockers = getPossibleBlockers(attackers, opponentsAttackers, true);

        if (!blockers.isEmpty()) {
            notNeededAsBlockers.removeAll(blockers);

            boolean playAggro = false;
            boolean pilotsNonAggroDeck = false;
            if (ai.getController().isAI()) {
                PlayerControllerAi aic = ((PlayerControllerAi) ai.getController());
                pilotsNonAggroDeck = aic.pilotsNonAggroDeck();
                playAggro = !pilotsNonAggroDeck || aic.getAi().getBooleanProperty(AiProps.PLAY_AGGRO);
            }
            // TODO make switchable via AI property
            int thresholdMod = 0;
            int lastAcceptableBaselineLife = 0;
            if (pilotsNonAggroDeck) {
                lastAcceptableBaselineLife = ComputerUtil.predictNextCombatsRemainingLife(ai, playAggro, pilotsNonAggroDeck, 0, new CardCollection(notNeededAsBlockers));
                if (!ai.isCardInPlay("Laboratory Maniac")) {
                    // AI is getting milled out
                    thresholdMod += 3 - Math.min(ai.getCardsIn(ZoneType.Library).size(), 3);
                }
                if (aiAggression > 4) {
                    thresholdMod += 1;
                }
            }

            // try to use strongest as attacker first
            CardLists.sortByPowerDesc(blockers);

            for (Card c : blockers) {
                if (vigilantes.contains(c)) {
                    // TODO predict the chance it might die if attacking
                    continue;
                }
                notNeededAsBlockers.add(c);
                int currentBaselineLife = ComputerUtil.predictNextCombatsRemainingLife(ai, playAggro, pilotsNonAggroDeck, 0, new CardCollection(notNeededAsBlockers));
                // AI doesn't know from what it will lose, so it might still keep an unnecessary blocker back sometimes
                if (currentBaselineLife == Integer.MIN_VALUE) {
                    notNeededAsBlockers.remove(c);
                    break;
                }

                // in Aggro Decks AI wants to deal as much damage as it can
                if (pilotsNonAggroDeck) {
                    int ownAttackerDmg = c.getNetCombatDamage();
                    // TODO maybe add performance switch to skip these predictions?
                    if (c.toughnessAssignsDamage()) {
                        ownAttackerDmg += ComputerUtilCombat.predictToughnessBonusOfAttacker(c, null, null, true);
                    } else {
                        ownAttackerDmg += ComputerUtilCombat.predictPowerBonusOfAttacker(c, null, null, true);
                    }
                    if (c.hasDoubleStrike()) {
                        ownAttackerDmg *= 2;
                    }
                    ownAttackerDmg += thresholdMod;
                    // bail if it would cause AI more life loss from counterattack than the damage it provides as attacker
                    if (Math.abs(currentBaselineLife - lastAcceptableBaselineLife) > ownAttackerDmg) {
                        notNeededAsBlockers.remove(c);
                        // try find more
                        continue;
                    } else if (Math.abs(currentBaselineLife - lastAcceptableBaselineLife) == ownAttackerDmg) {
                        // TODO add non sim-AI property for life trade chance that scales down with amount and when difference increases
                    }
                    lastAcceptableBaselineLife = currentBaselineLife;
                }
            }
        }

        // these creatures will be available to block anyway
        notNeededAsBlockers.addAll(vigilantes);

        // Increase the total number of blockers needed by 1 if Finest Hour in play
        // (human will get an extra first attack with a creature that untaps)
        // In addition, if the computer guesses it needs no blockers, make sure
        // that it won't be surprised by Exalted
        final int humanExaltedBonus = defendingOpponent.countExaltedBonus();
        int blockersNeeded = attackers.size() - notNeededAsBlockers.size();

        if (humanExaltedBonus > 0) {
            final boolean finestHour = defendingOpponent.isCardInPlay("Finest Hour");

            if ((blockersNeeded <= 0 || finestHour) && !this.oppList.isEmpty()) {
                // total attack = biggest creature + exalted, *2 if Rafiq is in play
                int humanBasePower = ComputerUtilCombat.getAttack(this.oppList.get(0)) + humanExaltedBonus;
                if (finestHour) {
                    // For Finest Hour, one creature could attack and get the bonus TWICE
                    humanBasePower += humanExaltedBonus;
                }
                final int totalExaltedAttack = defendingOpponent.isCardInPlay("Rafiq of the Many") ? 2 * humanBasePower
                        : humanBasePower;
                if (ai.getLife() - 3 <= totalExaltedAttack) {
                    // We will lose if there is an Exalted attack -- keep one blocker
                    if (blockersNeeded == 0 && !notNeededAsBlockers.isEmpty()) {
                        notNeededAsBlockers.remove(0);
                    }

                    // Finest Hour allows a second Exalted attack: keep a blocker for that too
                    if (finestHour && !notNeededAsBlockers.isEmpty()) {
                        notNeededAsBlockers.remove(0);
                    }
                }
            }
        }
        return notNeededAsBlockers;
    }

    private boolean doAssault() {
        // Beastmaster Ascension
        if (ai.isCardInPlay("Beastmaster Ascension") && this.attackers.size() > 1) {
            final CardCollectionView beastions = ai.getCardsIn(ZoneType.Battlefield, "Beastmaster Ascension");
            int minCreatures = 7;
            for (final Card beastion : beastions) {
                final int counters = beastion.getCounters(CounterEnumType.QUEST);
                minCreatures = Math.min(minCreatures, 7 - counters);
            }
            if (this.attackers.size() >= minCreatures) {
                return true;
            }
        }

        CardLists.sortByPowerDesc(this.attackers);

        CardCollection unblockedAttackers = new CardCollection();
        final CardCollection remainingAttackers = new CardCollection(this.attackers);
        final CardCollection remainingBlockers = new CardCollection(this.blockers);
        final CardCollection blockedAttackers = new CardCollection();

        // Conservative prediction for vehicles: the AI tries to acknowledge the fact that
        // at least one creature will tap to crew a blocking vehicle when predicting if an
        // alpha strike for lethal is viable
        int maxBlockersAfterCrew = remainingBlockers.size();
        for (Card c : this.blockers) {
            CardTypeView cardType = c.getCurrentState().getType();
            Zone oppBattlefield = c.getController().getZone(ZoneType.Battlefield);

            if (c.getName().equals("Heart of Kiran")) {
                if (Iterables.any(oppBattlefield, CardPredicates.Presets.PLANESWALKERS)) {
                    // can be activated by removing a loyalty counter instead of tapping a creature
                    continue;
                }
            } else if (c.getName().equals("Peacewalker Colossus")) {
                // can activate other vehicles for {1}{W}
                // TODO: the AI should ideally predict how many times it can activate
                // for now, unless the opponent is tapped out, break at this point
                // and do not predict the blocker limit (which is safer)
                if (Iterables.any(oppBattlefield, Predicates.and(CardPredicates.Presets.UNTAPPED, CardPredicates.Presets.LANDS))) {
                    maxBlockersAfterCrew = Integer.MAX_VALUE;
                    break;
                }
                maxBlockersAfterCrew--;
            } else if (cardType.hasSubtype("Vehicle") && !cardType.isCreature()) {
                maxBlockersAfterCrew--;
            }
        }

        // if true, the AI will attempt to identify which blockers will already be taken,
        // thus attempting to predict how many creatures with evasion can actively block
        boolean predictEvasion = false;
        if (ai.getController().isAI()) {
            AiController aic = ((PlayerControllerAi) ai.getController()).getAi();
            if (aic.getBooleanProperty(AiProps.COMBAT_ASSAULT_ATTACK_EVASION_PREDICTION)) {
                predictEvasion = true;
            }
        }

        CardCollection accountedBlockers = new CardCollection(this.blockers);
        CardCollection categorizedAttackers = new CardCollection();

        if (predictEvasion) {
            // split categorizedAttackers such that the ones with evasion come first and
            // can be properly accounted for. Note that at this point the attackers need
            // to be sorted by power already (see the Collections.sort call above).
            categorizedAttackers.addAll(ComputerUtilCombat.categorizeAttackersByEvasion(this.attackers));
        } else {
            categorizedAttackers.addAll(this.attackers);
        }

        for (Card attacker : categorizedAttackers) {
            if (!CombatUtil.canBeBlocked(attacker, accountedBlockers, null)
                    || StaticAbilityAssignCombatDamageAsUnblocked.assignCombatDamageAsUnblocked(attacker)) {
                unblockedAttackers.add(attacker);
            } else {
                if (predictEvasion) {
                    List<Card> potentialBestBlockers = CombatUtil.getPotentialBestBlockers(attacker, accountedBlockers, null);
                    accountedBlockers.removeAll(potentialBestBlockers);
                }
            }
        }

        remainingAttackers.removeAll(unblockedAttackers);

        for (Card blocker : this.blockers) {
            if (blocker.canBlockAny()) {
                for (Card attacker : this.attackers) {
                    if (CombatUtil.canBlock(attacker, blocker)) {
                        remainingAttackers.remove(attacker);
                        blockedAttackers.add(attacker);
                    }
                }
                remainingBlockers.remove(blocker);
            }
        }

        // presumes the Human will block
        for (Card blocker : remainingBlockers) {
            if (remainingAttackers.isEmpty() || maxBlockersAfterCrew == 0) {
                break;
            }

            int numExtraBlocks = blocker.canBlockAdditional();
            if (numExtraBlocks > 0) {
                // TODO should be limited to how much getBlockCost the opp can pay
                while (numExtraBlocks-- > 0 && !remainingAttackers.isEmpty()) {
                    blockedAttackers.add(remainingAttackers.remove(0));
                    maxBlockersAfterCrew--;
                }
            }

            if (remainingAttackers.isEmpty()) {
                break;
            }
            blockedAttackers.add(remainingAttackers.remove(0));
            maxBlockersAfterCrew--;
        }
        unblockedAttackers.addAll(remainingAttackers);

        int totalCombatDamage = 0;

        // TODO might want to only calculate that if it's needed
        // TODO might want to factor in isManaSourceReserved
        int myFreeMana = ComputerUtilMana.getAvailableManaEstimate(ai, !nextTurn);
        // skip attackers exceeding the attack tax that's payable
        // (this prevents the AI from only making a partial attack that could backfire)
        final Pair<Integer, Integer> tramplerFirst = getDamageFromBlockingTramplers(blockedAttackers, remainingBlockers, myFreeMana);
        int trampleDamage = tramplerFirst.getLeft();
        int tramplerTaxPaid = tramplerFirst.getRight();

        // see how far we can get if paying for the unblockable first instead
        if (tramplerTaxPaid > 0) {
            int unblockableAttackTax = 0;
            final CardCollection unblockableWithPaying = new CardCollection();
            final CardCollection unblockableCantPayFor = new CardCollection();
            final CardCollection unblockableWithoutCost = new CardCollection();
            // TODO also check poison
            for (Card attacker : unblockedAttackers) {
                Cost tax = CombatUtil.getAttackCost(ai.getGame(), attacker, defendingOpponent);
                if (tax == null) {
                    unblockableWithoutCost.add(attacker);
                } else {
                    int taxCMC = tax.getCostMana().getMana().getCMC();
                    if (myFreeMana < unblockableAttackTax + taxCMC) {
                        unblockableCantPayFor.add(attacker);
                        continue;
                    }
                    unblockableAttackTax += taxCMC;
                    unblockableWithPaying.add(attacker);
                }
            }
            int dmgUnblockableAfterPaying = ComputerUtilCombat.sumDamageIfUnblocked(unblockableWithPaying, defendingOpponent);
            unblockedAttackers = unblockableWithoutCost;
            if (dmgUnblockableAfterPaying > trampleDamage) {
                myFreeMana -= unblockableAttackTax;
                totalCombatDamage = dmgUnblockableAfterPaying;
                // recalculate the trampler damage with the reduced mana available now
                trampleDamage = getDamageFromBlockingTramplers(blockedAttackers, remainingBlockers, myFreeMana).getLeft();
            } else {
                myFreeMana -= tramplerTaxPaid;
                // find out if we can still pay for some left
                for (Card attacker : unblockableWithPaying) {
                    Cost tax = CombatUtil.getAttackCost(ai.getGame(), attacker, defendingOpponent);
                    int taxCMC = tax.getCostMana().getMana().getCMC();
                    if (myFreeMana < unblockableAttackTax + taxCMC) {
                        continue;
                    }
                    unblockableAttackTax += taxCMC;
                    unblockedAttackers.add(attacker);
                }
            }
        }

        totalCombatDamage += ComputerUtilCombat.sumDamageIfUnblocked(unblockedAttackers, defendingOpponent) + trampleDamage;
        if (totalCombatDamage + ComputerUtil.possibleNonCombatDamage(ai, defendingOpponent) >= defendingOpponent.getLife()
                && !((defendingOpponent.cantLoseForZeroOrLessLife() || ai.cantWin()) && defendingOpponent.getLife() < 1)) {
            return true;
        }

        // TODO tramplers
        int totalPoisonDamage = ComputerUtilCombat.sumPoisonIfUnblocked(unblockedAttackers, defendingOpponent);
        if (totalPoisonDamage >= 10 - defendingOpponent.getPoisonCounters()) {
            return true;
        }

        return false;
    }

    private final Pair<Integer, Integer> getDamageFromBlockingTramplers(final List<Card> blockedAttackers, final List<Card> blockers, final int myFreeMana) {
        int currentAttackTax = 0;
        int trampleDamage = 0;
        CardCollection remainingBlockers = new CardCollection(blockers);
        for (Card attacker : CardLists.getKeyword(blockedAttackers, Keyword.TRAMPLE)) {
            // TODO might sort by quotient of dmg/cost for best combination
            Cost tax = CombatUtil.getAttackCost(ai.getGame(), attacker, defendingOpponent);
            int taxCMC = tax != null ? tax.getCostMana().getMana().getCMC() : 0;
            if (myFreeMana < currentAttackTax + taxCMC) {
                continue;
            }
            currentAttackTax += taxCMC;

            int damage = ComputerUtilCombat.getAttack(attacker);
            for (Card blocker : remainingBlockers.threadSafeIterable()) {
                if (CombatUtil.canBlock(attacker, blocker) && damage > 0) {
                    damage -= ComputerUtilCombat.shieldDamage(attacker, blocker);
                    remainingBlockers.remove(blocker);
                }
            }
            if (damage > 0) {
                trampleDamage += damage;
            }
        }
        return Pair.of(trampleDamage, currentAttackTax);
    }

    private final GameEntity chooseDefender(final Combat c, final boolean bAssault) {
        final FCollectionView<GameEntity> defs = c.getDefenders();
        if (defs.size() == 1) {
            return defs.getFirst();
        }
        GameEntity prefDefender = defs.contains(defendingOpponent) ? defendingOpponent : defs.get(0);

        // 1. assault the opponent if you can kill him
        if (bAssault) {
            return prefDefender;
        }
        // 2. attack planeswalkers
        List<Card> pwDefending = c.getDefendingPlaneswalkers();
        if (!pwDefending.isEmpty()) {
            final Card pwNearUlti = ComputerUtilCard.getBestPlaneswalkerToDamage(pwDefending);
            return pwNearUlti != null ? pwNearUlti : ComputerUtilCard.getBestPlaneswalkerAI(pwDefending);
        }

        return prefDefender;
    }

    final boolean LOG_AI_ATTACKS = false;

    /**
     * <p>
     * Getter for the field <code>attackers</code>.
     * </p>
     *
     * @return a {@link forge.game.combat.Combat} object.
     */
    public final int declareAttackers(final Combat combat) {
        // something prevents attacking, try another
        if (this.attackers.isEmpty() && ai.getOpponents().size() > 1) {
            final PlayerCollection opps = ai.getOpponents();
            opps.remove(defendingOpponent);
            defendingOpponent = Aggregates.random(opps);
            refreshCombatants(defendingOpponent);
        }

        final boolean bAssault = doAssault();

        // Determine who will be attacked
        GameEntity defender = chooseDefender(combat, bAssault);

        // decided to attack another defender so related lists need to be updated
        // (though usually rather try to avoid this situation for performance reasons)
        if (defender != defendingOpponent) {
            defendingOpponent = defender instanceof Player ? (Player) defender : ((Card)defender).getController();
            refreshCombatants(defender);
        }
        if (this.attackers.isEmpty()) {
            return aiAggression;
        }

        // Aggro options
        boolean playAggro = false;
        int chanceToAttackToTrade = 0;
        boolean tradeIfTappedOut = false;
        int extraChanceIfOppHasMana = 0;
        boolean tradeIfLowerLifePressure = false;
        boolean predictEvasion = false;
        boolean simAI = false;
        if (ai.getController().isAI()) {
            AiController aic = ((PlayerControllerAi) ai.getController()).getAi();
            simAI = aic.usesSimulation();
            if (!simAI) {
                playAggro = aic.getBooleanProperty(AiProps.PLAY_AGGRO);
                chanceToAttackToTrade = aic.getIntProperty(AiProps.CHANCE_TO_ATTACK_INTO_TRADE);
                tradeIfTappedOut = aic.getBooleanProperty(AiProps.ATTACK_INTO_TRADE_WHEN_TAPPED_OUT);
                extraChanceIfOppHasMana = aic.getIntProperty(AiProps.CHANCE_TO_ATKTRADE_WHEN_OPP_HAS_MANA);
                tradeIfLowerLifePressure = aic.getBooleanProperty(AiProps.RANDOMLY_ATKTRADE_ONLY_ON_LOWER_LIFE_PRESSURE);
                predictEvasion = aic.getBooleanProperty(AiProps.COMBAT_ATTRITION_ATTACK_EVASION_PREDICTION);
            }
        }

        // TODO: detect Lightmine Field by presence of a card with a specific trigger
        final boolean lightmineField = ai.getGame().isCardInPlay("Lightmine Field");
        // TODO: detect Season of the Witch by presence of a card with a specific trigger
        final boolean seasonOfTheWitch = ai.getGame().isCardInPlay("Season of the Witch");

        List<Card> attackersLeft = new ArrayList<>(this.attackers);

        // TODO probably use AttackConstraints instead of only GlobalAttackRestrictions?
        GlobalAttackRestrictions restrict = GlobalAttackRestrictions.getGlobalRestrictions(ai, combat.getDefenders());
        int attackMax = restrict.getMax();
        if (attackMax == -1) {
            // check with the local limitations vs. the chosen defender
            attackMax = restrict.getDefenderMax().get(defender) == null ? -1 : restrict.getDefenderMax().get(defender);
        }

        if (attackMax == 0) {
            // can't attack anymore
            return aiAggression;
        }

        // Attackers that don't really have a choice
        int numForcedAttackers = 0;
        // nextTurn is now only used by effect from Oracle en-Vec, which can skip check must attack,
        // because creatures not chosen can't attack.
        if (!nextTurn) {
            for (final Card attacker : this.attackers) {
                GameEntity mustAttackDef = null;
                if (attacker.getSVar("MustAttack").equals("True")) {
                    mustAttackDef = defender;
                } else if (attacker.hasSVar("EndOfTurnLeavePlay")
                        && isEffectiveAttacker(ai, attacker, combat, defender)) {
                    mustAttackDef = defender;
                } else if (seasonOfTheWitch) {
                    //TODO: if there are other ways to tap this creature (like mana creature), then don't need to attack
                    mustAttackDef = defender;
                } else {
                    if (combat.getAttackConstraints().getRequirements().get(attacker) == null) continue;
                    // check defenders in order of maximum requirements
                    for (Pair<GameEntity, Integer> e : combat.getAttackConstraints().getRequirements().get(attacker).getSortedRequirements()) {
                        if (e.getRight() == 0) continue;
                        GameEntity mustAttackDefMaybe = e.getLeft();
                        // Gideon Jura returns LKI
                        if (mustAttackDefMaybe instanceof Card) {
                            mustAttackDefMaybe = ai.getGame().getCardState((Card) mustAttackDefMaybe);
                        }
                        if (canAttackWrapper(attacker, mustAttackDefMaybe) && CombatUtil.getAttackCost(ai.getGame(), attacker, mustAttackDefMaybe) == null) {
                            mustAttackDef = mustAttackDefMaybe;
                            break;
                        }
                    }
                }
                if (mustAttackDef != null) {
                    combat.addAttacker(attacker, mustAttackDef);
                    attackersLeft.remove(attacker);
                    numForcedAttackers++;
                }
            }
            if (attackersLeft.isEmpty()) {
                return aiAggression;
            }
        }

        // Lightmine Field: make sure the AI doesn't wipe out its own creatures
        if (lightmineField) {
            doLightmineFieldAttackLogic(attackersLeft, numForcedAttackers, playAggro);
        }
        // Revenge of Ravens: make sure the AI doesn't kill itself and doesn't damage itself unnecessarily
        if (!doRevengeOfRavensAttackLogic(defender, attackersLeft, numForcedAttackers, attackMax)) {
            return aiAggression;
        }

        if (bAssault && defender == defendingOpponent) { // in case we are forced to attack someone else
            if (LOG_AI_ATTACKS)
                System.out.println("Assault");
            CardLists.sortByPowerDesc(attackersLeft);
            for (Card attacker : attackersLeft) {
                // reached max, breakup
                if (attackMax != -1 && combat.getAttackers().size() >= attackMax)
                    return aiAggression;

                if (canAttackWrapper(attacker, defender) && isEffectiveAttacker(ai, attacker, combat, defender)) {
                    combat.addAttacker(attacker, defender);
                }
            }
            // no more creatures to attack
            return aiAggression;
        }

        // Cards that are remembered to attack anyway (e.g. temporarily stolen creatures)
        if (ai.getController() instanceof PlayerControllerAi) {
            // Only do this if |ai| is actually an AI - as we could be trying to predict how the human will attack.
            for (Card attacker : this.attackers) {
                if (AiCardMemory.isRememberedCard(ai, attacker, AiCardMemory.MemorySet.MANDATORY_ATTACKERS)) {
                    combat.addAttacker(attacker, defender);
                    attackersLeft.remove(attacker);
                }
            }
        }

        // Exalted
        if (combat.getAttackers().isEmpty()) {
            boolean exalted = ai.countExaltedBonus() > 2;

            if (!exalted) {
                for (Card c : ai.getCardsIn(ZoneType.Battlefield)) {
                    if (c.getName().equals("Rafiq of the Many") || c.getName().equals("Battlegrace Angel")) {
                        exalted = true;
                        break;
                    }
                    if (c.getName().equals("Finest Hour") && ai.getGame().getPhaseHandler().isFirstCombat()) {
                        exalted = true;
                        break;
                    }
                }
            }
            if (exalted) {
                CardLists.sortByPowerDesc(this.attackers);
                if (LOG_AI_ATTACKS)
                    System.out.println("Exalted");
                aiAggression = 6;
                for (Card attacker : this.attackers) {
                    if (canAttackWrapper(attacker, defender) && shouldAttack(attacker, this.blockers, combat, defender)) {
                        combat.addAttacker(attacker, defender);
                        return aiAggression;
                    }
                }
            }
        }

        if (attackMax != -1) {
            // should attack with only max if able.
            CardLists.sortByPowerDesc(this.attackers);
            aiAggression = 6;
            for (Card attacker : this.attackers) {
                // reached max, breakup
                if (attackMax != -1 && combat.getAttackers().size() >= attackMax)
                    break;
                if (canAttackWrapper(attacker, defender) && shouldAttack(attacker, this.blockers, combat, defender)) {
                    combat.addAttacker(attacker, defender);
                }
            }
            // no more creatures to attack
            return aiAggression;
        }

        // TODO move this lower so it can also switch defender
        if (simAI && ComputerUtilCard.isNonDisabledCardInPlay(ai, "Reconnaissance")) {
            for (Card attacker : attackersLeft) {
                if (canAttackWrapper(attacker, defender)) {
                    // simulation will decide if attacker stays in combat based on blocks
                    combat.addAttacker(attacker, defender);
                }
            }
            // safe to exert
            aiAggression = 6;
            return aiAggression;
        }

        // *******************
        // Evaluate the creature forces
        // *******************

        int computerForces = 0;
        int humanForces = 0;
        int humanForcesForAttritionalAttack = 0;

        // examine the potential forces
        final List<Card> nextTurnAttackers = new ArrayList<>();
        int candidateCounterAttackDamage = 0;

        // get the potential damage and strength of the AI forces
        final List<Card> candidateAttackers = new ArrayList<>();
        int candidateUnblockedDamage = 0;
        for (final Card pCard : myList) {
            // if the creature can attack then it's a potential attacker this
            // turn, assume summoning sickness creatures will be able to
            if (ComputerUtilCombat.canAttackNextTurn(pCard) && pCard.getNetCombatDamage() > 0) {
                candidateAttackers.add(pCard);
                candidateUnblockedDamage += ComputerUtilCombat.damageIfUnblocked(pCard, defendingOpponent, null, false);
                computerForces++;
            }
        }

        CardCollection categorizedOppList = new CardCollection();
        if (predictEvasion) {
            // If predicting evasion, make sure that attackers with evasion are considered first
            // (to avoid situations where the AI would predict his non-flyers to be blocked with
            // flying creatures and then believe that flyers will necessarily be left unblocked)
            categorizedOppList.addAll(ComputerUtilCombat.categorizeAttackersByEvasion(this.oppList));
        } else {
            categorizedOppList.addAll(this.oppList);
        }

        for (final Card pCard : categorizedOppList) {
            // if the creature can attack next turn add it to counter attackers list
            if (pCard.getNetCombatDamage() > 0 && ComputerUtilCombat.canAttackNextTurn(pCard)) {
                nextTurnAttackers.add(pCard);
                candidateCounterAttackDamage += pCard.getNetCombatDamage();
                humanForces++; // player forces they might use to attack
            }
            // increment player forces that are relevant to an attritional attack - includes walls

            Card potentialOppBlocker = getCardCanBlockAnAttacker(pCard, candidateAttackers, true);
            if (potentialOppBlocker != null) {
                humanForcesForAttritionalAttack++;
                if (predictEvasion) {
                    candidateAttackers.remove(potentialOppBlocker);
                }
            }
        }

        // find the potential counter attacking damage compared to AI life total
        double aiLifeToPlayerDamageRatio = 1000000;
        if (candidateCounterAttackDamage > 0) {
            aiLifeToPlayerDamageRatio = (double) ai.getLife() / candidateCounterAttackDamage;
        }

        // find the potential damage ratio the AI can cause
        double humanLifeToDamageRatio = 1000000;
        if (candidateUnblockedDamage > 0) {
            humanLifeToDamageRatio = (double) (defendingOpponent.getLife() - ComputerUtil.possibleNonCombatDamage(ai, defendingOpponent)) / candidateUnblockedDamage;
        }

        // determine if the ai outnumbers the player
        final int outNumber = computerForces - humanForces;

        for (Card blocker : this.blockers) {
            if (blocker.canBlockAny()) {
                aiLifeToPlayerDamageRatio--;
            }
        }

        // compare the ratios, higher = better for ai
        final double ratioDiff = aiLifeToPlayerDamageRatio - humanLifeToDamageRatio;

        // *********************
        // if outnumber and superior ratio work out whether attritional all out
        // attacking will work  attritional attack will expect some creatures to die but to achieve
        // victory by sheer weight of numbers attacking turn after turn. It's not calculate very
        // carefully, the accuracy can probably be improved
        // *********************
        boolean doAttritionalAttack = false;
        // get list of attackers ordered from low power to high
        CardLists.sortByPowerAsc(this.attackers);
        // get player life total
        int humanLife = defendingOpponent.getLife();
        // get the list of attackers up to the first blocked one
        final List<Card> attritionalAttackers = new ArrayList<>();
        for (int x = 0; x < (this.attackers.size() - humanForces); x++) {
            attritionalAttackers.add(this.attackers.get(x));
        }
        // until the attackers are used up or the player would run out of life
        int attackRounds = 1;
        while (!attritionalAttackers.isEmpty() && humanLife > 0 && attackRounds < 99) {
            // sum attacker damage
            int damageThisRound = 0;
            for (int y = 0; y < attritionalAttackers.size(); y++) {
                damageThisRound += attritionalAttackers.get(y).getNetCombatDamage();
            }
            // remove from player life
            humanLife -= damageThisRound;
            // shorten attacker list by the length of the blockers - assuming
            // all blocked are killed for convenience
            for (int z = 0; z < humanForcesForAttritionalAttack; z++) {
                if (!attritionalAttackers.isEmpty()) {
                    attritionalAttackers.remove(attritionalAttackers.size() - 1);
                }
            }
            attackRounds += 1;
            if (humanLife <= 0) {
                doAttritionalAttack = true;
            }
        }
        // *********************
        // end attritional attack calculation
        // *********************

        // *********************
        // see how long until unblockable attackers will be fatal
        // *********************
        double unblockableDamage = 0;
        double nextUnblockableDamage = 0;
        double turnsUntilDeathByUnblockable = 0;
        boolean doUnblockableAttack = false;
        for (final Card attacker : this.attackers) {
            boolean isUnblockableCreature = true;
            // check blockers individually, as the bulk canBeBlocked doesn't
            // check all circumstances
            for (final Card blocker : this.blockers) {
                if (CombatUtil.canBlock(attacker, blocker)) {
                    isUnblockableCreature = false;
                    break;
                }
            }
            if (isUnblockableCreature) {
                unblockableDamage += ComputerUtilCombat.damageIfUnblocked(attacker, defendingOpponent, combat, false);
            }
        }
        for (final Card attacker : nextTurnAttackers) {
            boolean isUnblockableCreature = true;
            // check blockers individually, as the bulk canBeBlocked doesn't
            // check all circumstances
            for (final Card blocker : myList) {
                if (CombatUtil.canBlock(attacker, blocker, true)) {
                    isUnblockableCreature = false;
                    break;
                }
            }
            if (isUnblockableCreature) {
                nextUnblockableDamage += ComputerUtilCombat.damageIfUnblocked(attacker, defendingOpponent, null, false);
            }
        }
        if (unblockableDamage > 0 && !defendingOpponent.cantLoseForZeroOrLessLife() && defendingOpponent.canLoseLife()) {
            turnsUntilDeathByUnblockable = 1 + (defendingOpponent.getLife() - unblockableDamage) / nextUnblockableDamage;
        }
        if (defendingOpponent.canLoseLife()) {
            doUnblockableAttack = true;
        }
        // *****************
        // end see how long until unblockable attackers will be fatal
        // *****************

        // decide on attack aggression based on a comparison of forces, life
        // totals and other considerations some bad "magic numbers" here
        // TODO replace with nice descriptive variable names
        if (ratioDiff > 0 && doAttritionalAttack) {
            aiAggression = 5; // attack at all costs
        } else if ((ratioDiff >= 1 && this.attackers.size() > 1 && (humanLifeToDamageRatio < 2 || outNumber > 0))
        		|| (playAggro && MyRandom.percentTrue(chanceToAttackToTrade) && humanLifeToDamageRatio > 1)) {
            aiAggression = 4; // attack expecting to trade or damage player.
        } else if (MyRandom.percentTrue(chanceToAttackToTrade) && humanLifeToDamageRatio > 1
                && defendingOpponent != null
                && ComputerUtil.countUsefulCreatures(ai) > ComputerUtil.countUsefulCreatures(defendingOpponent)
                && ai.getLife() > defendingOpponent.getLife()
                && !ComputerUtilCombat.lifeInDanger(ai, combat) // this isn't really doing anything unless the attacking player in combat isn't the AI (which currently isn't used like that)
                && (ComputerUtilMana.getAvailableManaEstimate(ai) > 0) || tradeIfTappedOut
                && (ComputerUtilMana.getAvailableManaEstimate(defendingOpponent) == 0) || MyRandom.percentTrue(extraChanceIfOppHasMana)
                && (!tradeIfLowerLifePressure || (ai.getLifeLostLastTurn() + ai.getLifeLostThisTurn() <
                        defendingOpponent.getLifeLostThisTurn() + defendingOpponent.getLifeLostThisTurn()))) {
            aiAggression = 4; // random (chance-based) attack expecting to trade or damage player.
        } else if (ratioDiff >= 0 && this.attackers.size() > 1) {
            aiAggression = 3; // attack expecting to make good trades or damage player.
        } else if (ratioDiff + outNumber >= -1 || aiLifeToPlayerDamageRatio > 1
                || ratioDiff * -1 < turnsUntilDeathByUnblockable) {
            // at 0 ratio expect to potentially gain an advantage by attacking first
            // if the ai has a slight advantage
            // or the ai has a significant advantage numerically but only a slight disadvantage damage/life
            aiAggression = 2; // attack expecting to destroy creatures/be unblockable
        } else if (doUnblockableAttack) {
            aiAggression = 1;
            // look for unblockable creatures that might be
            // able to attack for a bit of fatal damage even if the player is significantly better
        } else {
            aiAggression = 0;
        } // stay at home to block

        if ( LOG_AI_ATTACKS )
            System.out.println(aiAggression + " = ai aggression");

        // ****************
        // Evaluation the end
        // ****************

        if ( LOG_AI_ATTACKS )
            System.out.println("Normal attack");

        attackersLeft = notNeededAsBlockers(attackersLeft);
        attackersLeft = sortAttackers(attackersLeft);

        if ( LOG_AI_ATTACKS )
            System.out.println("attackersLeft = " + attackersLeft);

        FCollection<GameEntity> possibleDefenders = new FCollection<>(defendingOpponent);
        possibleDefenders.addAll(defendingOpponent.getPlaneswalkersInPlay());

        while (!attackersLeft.isEmpty()) {
            CardCollection attackersAssigned = new CardCollection();
            for (int i = 0; i < attackersLeft.size(); i++) {
                final Card attacker = attackersLeft.get(i);
                if (aiAggression < 5 && !attacker.hasFirstStrike() && !attacker.hasDoubleStrike()
                        && ComputerUtilCombat.getTotalFirstStrikeBlockPower(attacker, defendingOpponent)
                        >= ComputerUtilCombat.getDamageToKill(attacker, false)) {
                    continue;
                }

                // TODO logic for Questing Beast to prefer players

                if (shouldAttack(attacker, this.blockers, combat, defender) && canAttackWrapper(attacker, defender)) {
                    combat.addAttacker(attacker, defender);
                    attackersAssigned.add(attacker);

                    // check if attackers are enough to finish the attacked planeswalker
                    if (i < attackersLeft.size() - 1 && defender instanceof Card) {
                        final int blockNum = this.blockers.size();
                        int attackNum = 0;
                        int damage = 0;
                        List<Card> attacking = combat.getAttackersOf(defender);
                        CardLists.sortByPowerDesc(attacking);
                        for (Card atta : attacking) {
                            if (attackNum >= blockNum || !CombatUtil.canBeBlocked(atta, this.blockers, combat)) {
                                damage += ComputerUtilCombat.damageIfUnblocked(atta, defender, null, false);
                            } else {
                                attackNum++;
                            }
                        }
                        // if enough damage: switch to next planeswalker
                        if (damage >= ComputerUtilCombat.getDamageToKill((Card) defender, true)) {
                            break;
                        }
                    }
                }
            }

            attackersLeft.removeAll(attackersAssigned);
            possibleDefenders.remove(defender);
            if (attackersLeft.isEmpty() || possibleDefenders.isEmpty()) {
                break;
            }
            CardCollection pwDefending = new CardCollection(Iterables.filter(possibleDefenders, Card.class));
            if (pwDefending.isEmpty()) {
                // TODO for now only looks at same player as we'd have to check the others from start too
                //defender = new PlayerCollection(Iterables.filter(possibleDefenders, Player.class)).min(PlayerPredicates.compareByLife());
                defender = defendingOpponent;
            } else {
                final Card pwNearUlti = ComputerUtilCard.getBestPlaneswalkerToDamage(pwDefending);
                defender = pwNearUlti != null ? pwNearUlti : ComputerUtilCard.getBestPlaneswalkerAI(pwDefending);
            }
        }

        return aiAggression;
    }

    /**
     * <p>
     * shouldAttack.
     * </p>
     *
     * @param attacker
     *            a {@link forge.game.card.Card} object.
     * @param defenders
     *            a object.
     * @param combat
     *            a {@link forge.game.combat.Combat} object.
     * @return a boolean.
     */
    public final boolean shouldAttack(final Card attacker, final List<Card> defenders, final Combat combat, final GameEntity defender) {
        boolean canBeKilled = false; // indicates if the attacker can be killed
        boolean canBeKilledByOne = false; // indicates if the attacker can be killed by a single blocker
        boolean canKillAll = true; // indicates if the attacker can kill all single blockers
        boolean canKillAllDangerous = true; // indicates if the attacker can kill all single blockers with wither or infect
        boolean isWorthLessThanAllKillers = true;
        boolean canBeBlocked = false;
        int numberOfPossibleBlockers = 0;

        // Is it a creature that has a more valuable ability with a tap cost than what it can do by attacking?
        if (attacker.hasSVar("NonCombatPriority") && !attacker.hasKeyword(Keyword.VIGILANCE)) {
            // For each level of priority, enemy has to have life as much as the creature's power
            // so a priority of 4 means the creature will not attack unless it can defeat that player in 4 successful attacks.
            // the lower the priroity, the less willing the AI is to use the creature for attacking.
            // TODO Somehow subtract expected damage of other attacking creatures from enemy life total (how? other attackers not yet declared? Can the AI guesstimate which of their creatures will not get blocked?)
            if (attacker.getCurrentPower() * Integer.parseInt(attacker.getSVar("NonCombatPriority")) < ai.getOpponentsSmallestLifeTotal()) {
                // Check if the card actually has an ability the AI can and wants to play, if not, attacking is fine!
                for (SpellAbility sa : attacker.getSpellAbilities()) {
                    // Do not attack if we can afford using the ability.
                    if (sa.isActivatedAbility()) {
                        if (ComputerUtilCost.canPayCost(sa, ai, false)) {
                            return false;
                        }
                        // TODO Eventually The Ai will need to learn to predict if they have any use for the ability before next untap or not.
                        // TODO abilities that tap enemy creatures should probably only be saved if the enemy has nonzero creatures? Haste can be a threat though...
                    }
                }
            }
        }

        if (!isEffectiveAttacker(ai, attacker, combat, defender)) {
            return false;
        }
        boolean hasAttackEffect = attacker.getSVar("HasAttackEffect").equals("TRUE") || attacker.hasStartOfKeyword("Annihilator");
        // is there a gain in attacking even when the blocker is not killed (Lifelink, Wither,...)
        boolean hasCombatEffect = attacker.getSVar("HasCombatEffect").equals("TRUE") || "Blocked".equals(attacker.getSVar("HasAttackEffect"));

        if (!hasCombatEffect) {
            if (attacker.hasKeyword(Keyword.WITHER) || attacker.hasKeyword(Keyword.INFECT)
                    || attacker.hasKeyword(Keyword.LIFELINK) || attacker.hasKeyword(Keyword.AFFLICT)) {
                hasCombatEffect = true;
            }
        }

        // contains only the defender's blockers that can actually block the attacker
        CardCollection validBlockers = CardLists.filter(defenders, new Predicate<Card>() {
            @Override
            public boolean apply(Card defender) {
                return CombatUtil.canBlock(attacker, defender);
            }
        });

        boolean canTrampleOverDefenders = attacker.hasKeyword(Keyword.TRAMPLE) && attacker.getNetCombatDamage() > Aggregates.sum(validBlockers, CardPredicates.Accessors.fnGetNetToughness);

        // used to check that CanKillAllDangerous check makes sense in context where creatures with dangerous abilities are present
        boolean dangerousBlockersPresent = Iterables.any(validBlockers, Predicates.or(
                CardPredicates.hasKeyword(Keyword.WITHER), CardPredicates.hasKeyword(Keyword.INFECT),
                CardPredicates.hasKeyword(Keyword.LIFELINK)));

        // total power of the defending creatures, used in predicting whether a gang block can kill the attacker
        int defPower = CardLists.getTotalPower(validBlockers, true, false);

        // look at the attacker in relation to the blockers to establish a
        // number of factors about the attacking context that will be relevant
        // to the attackers decision according to the selected strategy
        for (final Card blocker : validBlockers) {
            // if both isWorthLessThanAllKillers and canKillAllDangerous are false there's nothing more to check
            if (isWorthLessThanAllKillers || canKillAllDangerous || numberOfPossibleBlockers < 2) {
                numberOfPossibleBlockers += 1;
                if (isWorthLessThanAllKillers && ComputerUtilCombat.canDestroyAttacker(ai, attacker, blocker, combat, false)
                        && !(attacker.hasKeyword(Keyword.UNDYING) && attacker.getCounters(CounterEnumType.P1P1) == 0)) {
                    canBeKilledByOne = true; // there is a single creature on the battlefield that can kill the creature
                    // see if the defending creature is of higher or lower
                    // value. We don't want to attack only to lose value
                    if (isWorthLessThanAllKillers && !attacker.hasSVar("SacMe")
                            && ComputerUtilCard.evaluateCreature(blocker) <= ComputerUtilCard.evaluateCreature(attacker)) {
                        isWorthLessThanAllKillers = false;
                    }
                }
                // see if this attacking creature can destroy this defender, if
                // not record that it can't kill everything
                if (canKillAllDangerous && !ComputerUtilCombat.canDestroyBlocker(ai, blocker, attacker, combat, false)) {
                    canKillAll = false;
                    if (blocker.getSVar("HasCombatEffect").equals("TRUE") || blocker.getSVar("HasBlockEffect").equals("TRUE")) {
                        canKillAllDangerous = false;
                    } else {
                        if (blocker.hasKeyword(Keyword.WITHER) || blocker.hasKeyword(Keyword.INFECT)
                                || blocker.hasKeyword(Keyword.LIFELINK)) {
                            canKillAllDangerous = false;
                            // there is a creature that can survive an attack from this creature
                            // and combat will have negative effects
                        }

                        // Check if maybe we are too reckless in adding this attacker
                        if (canKillAllDangerous) {
                            boolean avoidAttackingIntoBlock = ai.getController().isAI()
                                    && ((PlayerControllerAi) ai.getController()).getAi().getBooleanProperty(AiProps.TRY_TO_AVOID_ATTACKING_INTO_CERTAIN_BLOCK);
                            boolean attackerWillDie = defPower >= attacker.getNetToughness();
                            boolean uselessAttack = !hasCombatEffect && !hasAttackEffect;
                            boolean noContributionToAttack = this.attackers.size() <= defenders.size() || attacker.getNetPower() <= 0;

                            // We are attacking too recklessly if we can't kill a single blocker and:
                            // - our creature will die for sure (chump attack)
                            // - our attack will not do anything special (no attack/combat effect to proc)
                            // - we can't deal damage to our opponent with sheer number of attackers and/or our attacker's power is 0 or less
                            if (attackerWillDie || (avoidAttackingIntoBlock && uselessAttack && noContributionToAttack)) {
                                canKillAllDangerous = false;
                            }
                        }
                    }
                }
            }
        }

        if (!attacker.hasKeyword(Keyword.VIGILANCE) && ComputerUtilCard.canBeKilledByRoyalAssassin(ai, attacker)) {
            canKillAllDangerous = false;
            canBeKilled = true;
            canBeKilledByOne = true;
            isWorthLessThanAllKillers = false;
            hasCombatEffect = false;
        } else if ((canKillAllDangerous || !canBeKilled) && ComputerUtilCard.canBeBlockedProfitably(defendingOpponent, attacker, true)) {
            canKillAllDangerous = false;
            canBeKilled = true;
        }

        // if the creature cannot block and can kill all opponents they might as
        // well attack, they do nothing staying back
        if (canKillAll && isWorthLessThanAllKillers && !CombatUtil.canBlock(attacker)) {
            if (LOG_AI_ATTACKS)
                System.out.println(attacker.getName() + " = attacking because they can't block, expecting to kill or damage player");
            return true;
        } else if (!canBeKilled && !dangerousBlockersPresent && canTrampleOverDefenders) {
            if (LOG_AI_ATTACKS)
                System.out.println(attacker.getName() + " = expecting to survive and get some Trample damage through");
            return true;
        }

        if (numberOfPossibleBlockers > 2
                || (numberOfPossibleBlockers >= 1 && CombatUtil.canAttackerBeBlockedWithAmount(attacker, 1, defendingOpponent))
                || (numberOfPossibleBlockers == 2 && CombatUtil.canAttackerBeBlockedWithAmount(attacker, 2, defendingOpponent))) {
            canBeBlocked = true;
        }
        // decide if the creature should attack based on the prevailing strategy choice in aiAggression
        switch (aiAggression) {
        case 6: // Exalted: expecting to at least kill a creature of equal value or not be blocked
            if ((canKillAll && isWorthLessThanAllKillers) || !canBeBlocked) {
                if (LOG_AI_ATTACKS)
                    System.out.println(attacker.getName() + " = attacking expecting to kill creature, or is unblockable");
                return true;
            }
            break;
        case 5: // all out attacking
            if (LOG_AI_ATTACKS)
                System.out.println(attacker.getName() + " = all out attacking");
            return true;
        case 4: // expecting to at least trade with something, or can attack "for free", expecting no counterattack
            if (canKillAll || (dangerousBlockersPresent && canKillAllDangerous && !canBeKilledByOne) || !canBeBlocked
                    || (defPower == 0 && !ComputerUtilCombat.lifeInDanger(ai, combat))) {
                if (LOG_AI_ATTACKS)
                    System.out.println(attacker.getName() + " = attacking expecting to at least trade with something");
                return true;
            }
            break;
        case 3: // expecting to at least kill a creature of equal value or not be blocked
            if ((canKillAll && isWorthLessThanAllKillers)
                    || (((dangerousBlockersPresent && canKillAllDangerous) || hasAttackEffect || hasCombatEffect) && !canBeKilledByOne)
                    || !canBeBlocked) {
                if (LOG_AI_ATTACKS)
                    System.out.println(attacker.getName() + " = attacking expecting to kill creature or cause damage, or is unblockable");
                return true;
            }
            break;
        case 2: // attack expecting to attract a group block or destroying a single blocker and surviving
            if (!canBeBlocked || ((canKillAll || hasAttackEffect || hasCombatEffect) && !canBeKilledByOne &&
                    ((dangerousBlockersPresent && canKillAllDangerous) || !canBeKilled))) {
                if (LOG_AI_ATTACKS)
                    System.out.println(attacker.getName() + " = attacking expecting to survive or attract group block");
                return true;
            }
            break;
        case 1: // unblockable creatures only
            if (!canBeBlocked || (numberOfPossibleBlockers == 1 && canKillAll && !canBeKilledByOne)) {
                if (LOG_AI_ATTACKS)
                    System.out.println(attacker.getName() + " = attacking expecting not to be blocked");
                return true;
            }
            break;
        default:
            break;
        }
        return false; // don't attack
    }

    public static List<Card> exertAttackers(final List<Card> attackers, int aggression) {
        List<Card> exerters = Lists.newArrayList();
        for (Card c : attackers) {
            boolean shouldExert = false;

            if (c.hasSVar("EndOfTurnLeavePlay")) {
                // creature would leave the battlefield
                // no pain in exerting it
                shouldExert = true;
            } else if (c.hasKeyword(Keyword.VIGILANCE) || ComputerUtilCard.willUntap(c.getController(), c)) {
                // Free exert - why not?
                shouldExert = true;
            }

            // if card has a Exert Trigger which would target,
            // but there are no creatures it can target, no need to exert with it
            boolean missTarget = false;
            for (Trigger t : c.getTriggers()) {
                if (!TriggerType.Exerted.equals(t.getMode())) {
                    continue;
                }
                SpellAbility sa = t.ensureAbility();
                if (sa == null) {
                    continue;
                }
                if (sa.usesTargeting()) {
                    sa.setActivatingPlayer(c.getController());
                    List<Card> validTargets = CardUtil.getValidCardsToTarget(sa.getTargetRestrictions(), sa);
                    if (validTargets.isEmpty()) {
                        missTarget = true;
                        break;
                    } else if (sa.isCurse() && !Iterables.any(validTargets,
                            CardPredicates.isControlledByAnyOf(c.getController().getOpponents()))) {
                        // e.g. Ahn-Crop Crasher - the effect is only good when aimed at opponent's creatures
                        missTarget = true;
                        break;
                    }
                }
            }

            if (missTarget) {
                continue;
            }

            if (!shouldExert) {
                // TODO Improve when the AI wants to use Exert powers
                shouldExert = aggression > 3;
            }

            // A specific AI condition for Exert: if specified on the card, the AI will always
            // exert creatures that meet this condition
            if (!shouldExert && c.hasSVar("AIExertCondition")) {
                if (!c.getSVar("AIExertCondition").isEmpty()) {
                    final String needsToExert = c.getSVar("AIExertCondition");
                    String sVar = needsToExert.split(" ")[0];
                    String comparator = needsToExert.split(" ")[1];
                    String compareTo = comparator.substring(2);

                    int x = AbilityUtils.calculateAmount(c, sVar, null);
                    int y = AbilityUtils.calculateAmount(c, compareTo, null);
                    if (Expressions.compare(x, comparator, y)) {
                        shouldExert = true;
                    }
                }
            }

            if (shouldExert) {
                exerters.add(c);
            }
        }

        return exerters;
    }

    /**
     * Find a protection type that will make an attacker unblockable.
     * @param sa ability belonging to ApiType.Protection
     * @return colour string or "artifacts", null if no possible choice exists
     */
    public String toProtectAttacker(SpellAbility sa) {
        //AiAttackController is created with the selected attacker as the only entry in "attackers"
        if (sa.getApi() != ApiType.Protection || oppList.isEmpty() || getPossibleBlockers(oppList, attackers, nextTurn).isEmpty()) {
            return null; //not protection sa or attacker is already unblockable
        }
        final List<String> choices = ProtectEffect.getProtectionList(sa);
        String color = ComputerUtilCard.getMostProminentColor(getPossibleBlockers(oppList, attackers, nextTurn)), artifact = null;
        if (choices.contains("artifacts")) {
            artifact = "artifacts"; //flag to indicate that protection from artifacts is available
        }
        if (!choices.contains(color)) {
            color = null;
        }
        for (Card c : oppList) { //find a blocker that ignores the currently selected protection
            if (artifact != null && !c.isArtifact()) {
                artifact = null;
            }
            if (color != null) {
                switch (color) {
                case "black":
                    if (!c.isBlack()) {
                        color = null;
                    }
                    break;
                case "blue":
                    if (!c.isBlue()) {
                        color = null;
                    }
                    break;
                case "green":
                    if (!c.isGreen()) {
                        color = null;
                    }
                    break;
                case "red":
                    if (!c.isRed()) {
                        color = null;
                    }
                    break;
                case "white":
                    if (!c.isWhite()) {
                        color = null;
                    }
                    break;
                }
            }
            if (color == null && artifact == null) { //nothing can make the attacker unblockable
                return null;
            }
        }
        if (color != null) {
            return color;
        }
        if (artifact != null) {
            return artifact;
        }
        return null; //should never get here
    }

    private void doLightmineFieldAttackLogic(final List<Card> attackersLeft, int numForcedAttackers, boolean playAggro) {
        CardCollection attSorted = new CardCollection(attackersLeft);
        CardCollection attUnsafe = new CardCollection();
        CardLists.sortByToughnessDesc(attSorted);

        int i = numForcedAttackers;
        int refPowerValue = 0; // Aggro profiles do not account for the possible blockers' power, conservative profiles do.

        if (!playAggro && this.blockers.size() > 0) {
            // Conservative play: check to ensure that the card can't be killed off while damaged
            // TODO: currently sorting a copy of this.blockers, but it looks safe to operate on this.blockers directly?
            // Also, this should ideally somehow account for double blocks, unblockability, etc. Difficult to do without
            // running simulations.
            CardCollection blkSorted = new CardCollection(this.blockers);
            CardLists.sortByPowerDesc(blkSorted);
            refPowerValue += blkSorted.get(0).getCurrentPower();
        }

        for (Card cre : attSorted) {
            i++;
            if (i + refPowerValue >= cre.getCurrentToughness()) {
                attUnsafe.add(cre);
            } else {
                continue;
            }
        }

        attackersLeft.removeAll(attUnsafe);
    }

    private boolean doRevengeOfRavensAttackLogic(final GameEntity defender, final List<Card> attackersLeft, int numForcedAttackers, int maxAttack) {
        // TODO: detect Revenge of Ravens by the trigger instead of by name
        boolean revengeOfRavens = false;
        if (defender instanceof Player) {
            revengeOfRavens = !CardLists.filter(((Player)defender).getCardsIn(ZoneType.Battlefield), CardPredicates.nameEquals("Revenge of Ravens")).isEmpty();
        } else if (defender instanceof Card) {
            revengeOfRavens = !CardLists.filter(((Card)defender).getController().getCardsIn(ZoneType.Battlefield), CardPredicates.nameEquals("Revenge of Ravens")).isEmpty();
        }

        if (!revengeOfRavens) {
            return true;
        }

        int life = ai.canLoseLife() && !ai.cantLoseForZeroOrLessLife() ? ai.getLife() : Integer.MAX_VALUE;
        maxAttack = maxAttack < 0 ? Integer.MAX_VALUE - 1 : maxAttack;
        if (Math.min(maxAttack, numForcedAttackers) >= life) {
            return false;
        }

        // Remove all 1-power attackers since they usually only hurt the attacker
        // TODO: improve to account for possible combat effects coming from attackers like that
        CardCollection attUnsafe = new CardCollection();
        for (Card attacker : attackersLeft) {
            if (attacker.getNetCombatDamage() <= 1) {
                attUnsafe.add(attacker);
            }
        }
        attackersLeft.removeAll(attUnsafe);
        if (Math.min(maxAttack, attackersLeft.size()) >= life) {
            return false;
        }

        return true;
    }

}
