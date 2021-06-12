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
import java.util.Collections;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
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
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordInterface;
import forge.game.player.Player;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.Expressions;
import forge.util.MyRandom;
import forge.util.TextUtil;
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
    private final List<Card> attackers;
    private final List<Card> blockers;

    private List<Card> oppList; // holds human player creatures
    private List<Card> myList; // holds computer creatures
    
    private final Player ai;
    private Player defendingOpponent;
    
    private int aiAggression = 0; // added by Masher, how aggressive the ai is attack will be depending on circumstances
    private final boolean nextTurn;


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
        this.defendingOpponent = choosePreferredDefenderPlayer(ai);
        this.oppList = getOpponentCreatures(this.defendingOpponent);
        this.myList = ai.getCreaturesInPlay();
        this.attackers = new ArrayList<>();
        for (Card c : myList) {
            if (nextTurn && CombatUtil.canAttackNextTurn(c, this.defendingOpponent) ||
                    CombatUtil.canAttack(c, this.defendingOpponent)) {
                attackers.add(c);
            }
        }
        this.blockers = getPossibleBlockers(oppList, this.attackers);
        this.nextTurn = nextTurn;
    } // overloaded constructor to evaluate attackers that should attack next turn

    public AiAttackController(final Player ai, Card attacker) {
        this.ai = ai;
        this.defendingOpponent = choosePreferredDefenderPlayer(ai);       
        this.oppList = getOpponentCreatures(this.defendingOpponent);
        this.myList = ai.getCreaturesInPlay();
        this.attackers = new ArrayList<>();
        if (CombatUtil.canAttack(attacker, this.defendingOpponent)) {
            attackers.add(attacker);
        }
        this.blockers = getPossibleBlockers(oppList, this.attackers);
        this.nextTurn = false;
    } // overloaded constructor to evaluate single specified attacker
    
    public static List<Card> getOpponentCreatures(final Player defender) {
        List<Card> defenders = new ArrayList<>(defender.getCreaturesInPlay());
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
            for (SpellAbility sa : c.getSpellAbilities()) {
                if (sa.getApi() == ApiType.Animate) {
                    if (ComputerUtilCost.canPayCost(sa, defender) 
                            && sa.getRestrictions().checkOtherRestrictions(c, sa, defender)) {
                        Card animatedCopy = AnimateAi.becomeAnimated(c, sa);
                        defenders.add(animatedCopy);
                    }
                }
            }
        }
        return defenders;
    }
    
    public void removeBlocker(Card blocker) {
    	this.oppList.remove(blocker);
    }

    private boolean canAttackWrapper(final Card attacker, final GameEntity defender) {
        if (nextTurn) {
            return CombatUtil.canAttackNextTurn(attacker, defender);
        } else {
            return CombatUtil.canAttack(attacker, defender);
        }
    }

    /** Choose opponent for AI to attack here. Expand as necessary. */
    public static Player choosePreferredDefenderPlayer(Player ai) {
        Player defender = ai.getWeakestOpponent(); //Concentrate on opponent within easy kill range

        if (defender.getLife() > 8) { //Otherwise choose a random opponent to ensure no ganging up on players
            // TODO should we cache the random for each turn? some functions like shouldPumpCard base their decisions on the assumption who will be attacked
            return ai.getOpponents().get(MyRandom.getRandom().nextInt(ai.getOpponents().size()));
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
    } // sortAttackers()

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
    public final boolean isEffectiveAttacker(final Player ai, final Card attacker, final Combat combat) {

        // if the attacker will die when attacking don't attack
        if ((attacker.getNetToughness() + ComputerUtilCombat.predictToughnessBonusOfAttacker(attacker, null, combat, true)) <= 0) {
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

        final Player opp = this.defendingOpponent;

        // Damage opponent if unblocked
        final int dmgIfUnblocked = ComputerUtilCombat.damageIfUnblocked(attacker, opp, combat, true);
        if (dmgIfUnblocked > 0) {
            boolean onlyIfExalted = false;
            if (combat.getAttackers().isEmpty() && ai.countExaltedBonus() > 0
                    && dmgIfUnblocked - ai.countExaltedBonus() == 0) {
                // Make sure we're not counting on the Exalted bonus when the AI is planning to attack with more than one creature
                onlyIfExalted = true;
            }

            if (!onlyIfExalted || this.attackers.size() == 1 || this.aiAggression == 6 /* 6 is Exalted attack */) {
                return true;
            }
        }
        // Poison opponent if unblocked
        if (ComputerUtilCombat.poisonIfUnblocked(attacker, opp) > 0) {
            return true;
        }

        // TODO check if that makes sense
        int exalted = ai.countExaltedBonus();
        if (this.attackers.size() == 1 && exalted > 0
                && ComputerUtilCombat.predictDamageTo(opp, exalted, attacker, true) > 0) {
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

    public final static List<Card> getPossibleBlockers(final List<Card> blockers, final List<Card> attackers) {
        List<Card> possibleBlockers = new ArrayList<>(blockers);
        possibleBlockers = CardLists.filter(possibleBlockers, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return canBlockAnAttacker(c, attackers, false);
            }
        });
        return possibleBlockers;
    }

    public final static boolean canBlockAnAttacker(final Card c, final List<Card> attackers, final boolean nextTurn) {
        final List<Card> attackerList = new ArrayList<>(attackers);
        if (!c.isCreature()) {
            return false;
        }
        for (final Card attacker : attackerList) {
            if (CombatUtil.canBlock(attacker, c, nextTurn)) {
                return true;
            }
        }
        return false;
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
    // this method is used by getAttackers()
    public final List<Card> notNeededAsBlockers(final Player ai, final List<Card> attackers) {
        final List<Card> notNeededAsBlockers = new ArrayList<>(attackers);
        int fixedBlockers = 0;
        final List<Card> vigilantes = new ArrayList<>();
        //check for time walks
        if (ai.getGame().getPhaseHandler().getNextTurn().equals(ai)) {
            return attackers;
        }
        // no need to block (already holding mana to cast fog next turn)
        if (!AiCardMemory.isMemorySetEmpty(ai, AiCardMemory.MemorySet.CHOSEN_FOG_EFFECT)) {
            // Don't send the card that'll do the fog effect to attack, it's unsafe!

            List<Card> toRemove = Lists.newArrayList();
            for(Card c : attackers) {
                if (AiCardMemory.isRememberedCard(ai, c, AiCardMemory.MemorySet.CHOSEN_FOG_EFFECT)) {
                    toRemove.add(c);
                }
            }
            attackers.removeAll(toRemove);

            return attackers;
        }

        // no need to block if an effect is in play which untaps all creatures (pseudo-Vigilance akin to
        // Awakening or Prophet of Kruphix)
        for (Card card : ai.getGame().getCardsIn(ZoneType.Battlefield)) {
            boolean untapsEachTurn = card.hasSVar("UntapsEachTurn");
            boolean untapsEachOtherTurn = card.hasSVar("UntapsEachOtherPlayerTurn");

            if (untapsEachTurn || untapsEachOtherTurn) {
                String affected = untapsEachTurn ? card.getSVar("UntapsEachTurn")
                        : card.getSVar("UntapsEachOtherPlayerTurn");

                for (String aff : TextUtil.split(affected, ',')) {
                    if (aff.equals("Creature")
                            && (untapsEachTurn || (untapsEachOtherTurn && ai.equals(card.getController())))) {
                        return attackers;
                    }
                }
            }
        }

        List<Card> opponentsAttackers = new ArrayList<>(oppList);
        opponentsAttackers = CardLists.filter(opponentsAttackers, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return ComputerUtilCombat.canAttackNextTurn(c) && c.getNetCombatDamage() > 0;
            }
        });
        for (final Card c : this.myList) {
            if (c.getName().equals("Masako the Humorless")) {
                // "Tapped creatures you control can block as though they were untapped."
                return attackers;
            }
            if (!attackers.contains(c)) { // this creature can't attack anyway
                if (canBlockAnAttacker(c, opponentsAttackers, false)) {
                    fixedBlockers++;
                }
                continue;
            }
            if (c.hasKeyword(Keyword.VIGILANCE)) {
                vigilantes.add(c);
                notNeededAsBlockers.remove(c); // they will be re-added later
                if (canBlockAnAttacker(c, opponentsAttackers, false)) {
                    fixedBlockers++;
                }
            }
        }
        CardLists.sortByPowerAsc(attackers);
        int blockersNeeded = opponentsAttackers.size();

        // don't hold back creatures that can't block any of the human creatures
        final List<Card> list = getPossibleBlockers(attackers, opponentsAttackers);

        //Calculate the amount of creatures necessary
        for (int i = 0; i < list.size(); i++) {
            if (!doesHumanAttackAndWin(ai, i)) {
                blockersNeeded = i;
                break;
            }
        }
        int blockersStillNeeded = blockersNeeded - fixedBlockers;
        blockersStillNeeded = Math.min(blockersNeeded, list.size());
        for (int i = 0; i < blockersStillNeeded; i++) {
            notNeededAsBlockers.remove(list.get(i));
        }

        // re-add creatures with vigilance
        notNeededAsBlockers.addAll(vigilantes);

        if (blockersNeeded > 1) {
            return notNeededAsBlockers;
        }

        final Player opp = this.defendingOpponent;

        // Increase the total number of blockers needed by 1 if Finest Hour in play
        // (human will get an extra first attack with a creature that untaps)
        // In addition, if the computer guesses it needs no blockers, make sure
        // that it won't be surprised by Exalted
        final int humanExaltedBonus = opp.countExaltedBonus();

        if (humanExaltedBonus > 0) {
            final boolean finestHour = opp.isCardInPlay("Finest Hour");

            if ((blockersNeeded == 0 || finestHour) && !this.oppList.isEmpty()) {
                // total attack = biggest creature + exalted, *2 if Rafiq is in play
                int humanBasePower = getAttack(this.oppList.get(0)) + humanExaltedBonus;
                if (finestHour) {
                    // For Finest Hour, one creature could attack and get the bonus TWICE
                    humanBasePower = humanBasePower + humanExaltedBonus;
                }
                final int totalExaltedAttack = opp.isCardInPlay("Rafiq of the Many") ? 2 * humanBasePower
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

    public final boolean doesHumanAttackAndWin(final Player ai, final int nBlockingCreatures) {
        int totalAttack = 0;
        int totalPoison = 0;
        int blockersLeft = nBlockingCreatures;

        if (ai.cantLose()) {
            return false;
        }

        for (Card attacker : oppList) {
            if (!ComputerUtilCombat.canAttackNextTurn(attacker)) {
                continue;
            }
            if (blockersLeft > 0 && CombatUtil.canBeBlocked(attacker, ai)) {
                blockersLeft--;
                continue;
            }

            // Test for some special triggers that can change the creature in combat
            Card effectiveAttacker = ComputerUtilCombat.applyPotentialAttackCloneTriggers(attacker);

            totalAttack += ComputerUtilCombat.damageIfUnblocked(effectiveAttacker, ai, null, false);
            totalPoison += ComputerUtilCombat.poisonIfUnblocked(effectiveAttacker, ai);
        }

        if (totalAttack > 0 && ai.getLife() <= totalAttack && !ai.cantLoseForZeroOrLessLife()) {
            return true;
        }
        return ai.getPoisonCounters() + totalPoison > 9;
    }

    private boolean doAssault(final Player ai) {
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

        final CardCollection unblockedAttackers = new CardCollection();
        final CardCollection remainingAttackers = new CardCollection(this.attackers);
        final CardCollection remainingBlockers = new CardCollection(this.blockers);
        final CardCollection blockedAttackers = new CardCollection();

        // Conservative prediction for vehicles: the AI tries to acknowledge the fact that
        // at least one creature will tap to crew a blocking vehicle when predicting if an
        // alpha strike for lethal is viable
        int maxBlockersAfterCrew = remainingBlockers.size(); 
        for (Card c : this.blockers) {
            CardTypeView cardType = c.getCurrentState().getType();
            CardCollectionView oppBattlefield = c.getController().getCardsIn(ZoneType.Battlefield);

            if (c.getName().equals("Heart of Kiran")) {
                if (!CardLists.filter(oppBattlefield, CardPredicates.Presets.PLANESWALKERS).isEmpty()) {
                    // can be activated by removing a loyalty counter instead of tapping a creature
                    continue;
                }
            } else if (c.getName().equals("Peacewalker Colossus")) {
                // can activate other vehicles for {1}{W}
                // TODO: the AI should ideally predict how many times it can activate
                // for now, unless the opponent is tapped out, break at this point 
                // and do not predict the blocker limit (which is safer)
                if (!CardLists.filter(oppBattlefield, Predicates.and(CardPredicates.Presets.UNTAPPED, CardPredicates.Presets.LANDS)).isEmpty()) {
                    maxBlockersAfterCrew = Integer.MAX_VALUE;
                    break;
                } else {
                    maxBlockersAfterCrew--;
                }
            } else if (cardType.hasSubtype("Vehicle") && !cardType.isCreature()) {
                maxBlockersAfterCrew--;
            }
        }

        final Player opp = this.defendingOpponent;

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
                    || attacker.hasKeyword("You may have CARDNAME assign its combat damage as though it weren't blocked.")) {
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
                while (numExtraBlocks-- > 0 && !remainingAttackers.isEmpty()) {
                    blockedAttackers.add(remainingAttackers.get(0));
                    remainingAttackers.remove(0);
                    maxBlockersAfterCrew--;
                }
            }

            if (remainingAttackers.isEmpty()) {
                break;
            }
            blockedAttackers.add(remainingAttackers.get(0));
            remainingAttackers.remove(0);
            maxBlockersAfterCrew--;
        }
        unblockedAttackers.addAll(remainingAttackers);
        
        int trampleDamage = 0;
        for (Card attacker : blockedAttackers) {
            if (attacker.hasKeyword(Keyword.TRAMPLE)) {
                int damage = ComputerUtilCombat.getAttack(attacker);
                for (Card blocker : this.blockers) {
                    if (CombatUtil.canBlock(attacker, blocker)) {
                        damage -= ComputerUtilCombat.shieldDamage(attacker, blocker);
                    }
                }
                if (damage > 0) {
                    trampleDamage += damage;
                }
            }
        }

        int totalCombatDamage = ComputerUtilCombat.sumDamageIfUnblocked(unblockedAttackers, opp) + trampleDamage;
        int totalPoisonDamage = ComputerUtilCombat.sumPoisonIfUnblocked(unblockedAttackers, opp);

        if (totalCombatDamage + ComputerUtil.possibleNonCombatDamage(ai, opp) >= opp.getLife()
                && !((opp.cantLoseForZeroOrLessLife() || ai.cantWin()) && opp.getLife() < 1)) {
            return true;
        }

        if (totalPoisonDamage >= 10 - opp.getPoisonCounters()) {
            return true;
        }

        return false;
    }

    private final GameEntity chooseDefender(final Combat c, final boolean bAssault) {
        final FCollectionView<GameEntity> defs = c.getDefenders();
        if (defs.size() == 1) {
            return defs.getFirst();
        }
        GameEntity prefDefender = defs.contains(this.defendingOpponent) ? this.defendingOpponent : defs.get(0);

        // Attempt to see if there's a defined entity that must be attacked strictly this turn...
        GameEntity entity = ai.getMustAttackEntityThisTurn();
        if (entity == null) {
            // ...or during the attacking creature controller's turn
            entity = ai.getMustAttackEntity();
        }
        if (null != entity) {
            int n = defs.indexOf(entity);
            if (-1 == n) {
                System.out.println("getMustAttackEntity() or getMustAttackEntityThisTurn() returned something not in defenders.");
                return prefDefender;
            } else {
                return entity;
            }
        } else {
            // 1. assault the opponent if you can kill him
            if (bAssault) {
                return prefDefender;
            }
            // 2. attack planeswalkers
            List<Card> pwDefending = c.getDefendingPlaneswalkers();
            if (!pwDefending.isEmpty()) {
                return ComputerUtilCard.getBestPlaneswalkerToDamage(pwDefending);
            } else {
                return prefDefender;
            }
        }
    }

    final boolean LOG_AI_ATTACKS = false;

    /**
     * <p>
     * Getter for the field <code>attackers</code>.
     * </p>
     * 
     * @return a {@link forge.game.combat.Combat} object.
     */
    public final void declareAttackers(final Combat combat) {
        if (this.attackers.isEmpty()) {
            return;
        }

        // Aggro options
        boolean playAggro = false;
        int chanceToAttackToTrade = 0;
        boolean tradeIfTappedOut = false;
        int extraChanceIfOppHasMana = 0;
        boolean tradeIfLowerLifePressure = false;
        if (ai.getController().isAI()) {
            AiController aic = ((PlayerControllerAi) ai.getController()).getAi();
            playAggro = aic.getBooleanProperty(AiProps.PLAY_AGGRO);
            chanceToAttackToTrade = aic.getIntProperty(AiProps.CHANCE_TO_ATTACK_INTO_TRADE);
            tradeIfTappedOut = aic.getBooleanProperty(AiProps.ATTACK_INTO_TRADE_WHEN_TAPPED_OUT);
            extraChanceIfOppHasMana = aic.getIntProperty(AiProps.CHANCE_TO_ATKTRADE_WHEN_OPP_HAS_MANA);
            tradeIfLowerLifePressure = aic.getBooleanProperty(AiProps.RANDOMLY_ATKTRADE_ONLY_ON_LOWER_LIFE_PRESSURE);
        }

        final boolean bAssault = doAssault(ai);
        // TODO: detect Lightmine Field by presence of a card with a specific trigger
        final boolean lightmineField = ComputerUtilCard.isPresentOnBattlefield(ai.getGame(), "Lightmine Field");
        // TODO: detect Season of the Witch by presence of a card with a specific trigger
        final boolean seasonOfTheWitch = ComputerUtilCard.isPresentOnBattlefield(ai.getGame(), "Season of the Witch");

        // Determine who will be attacked
        GameEntity defender = chooseDefender(combat, bAssault);
        List<Card> attackersLeft = new ArrayList<>(this.attackers);

        // TODO probably use AttackConstraints instead of only GlobalAttackRestrictions?
        GlobalAttackRestrictions restrict = GlobalAttackRestrictions.getGlobalRestrictions(ai, combat.getDefenders());
        int attackMax = restrict.getMax();
        if (attackMax == -1) {
            // check with the local limitations vs. the chosen defender
            attackMax = restrict.getDefenderMax().get(defender) == null ? -1 : restrict.getDefenderMax().get(defender);
        }

        if (attackMax == 0) {
            //  can't attack anymore
            return;
        }

        // Attackers that don't really have a choice
        int numForcedAttackers = 0;
        // nextTurn is now only used by effect from Oracle en-Vec, which can skip check must attack,
        // because creatures not chosen can't attack.
        if (!nextTurn) {
            for (final Card attacker : this.attackers) {
                if (!CombatUtil.canAttack(attacker, defender)) {
                    attackersLeft.remove(attacker);
                    continue;
                }
                boolean mustAttack = false;
                if (attacker.isGoaded()) {
                    mustAttack = true;
                } else if (attacker.getSVar("MustAttack").equals("True")) {
                    mustAttack = true;
                } else if (attacker.hasSVar("EndOfTurnLeavePlay")
                        && isEffectiveAttacker(ai, attacker, combat)) {
                    mustAttack = true;
                } else if (seasonOfTheWitch) {
                    // TODO: if there are other ways to tap this creature (like mana creature), then don't need to attack
                    mustAttack = true;
                } else {
                    for (KeywordInterface inst : attacker.getKeywords()) {
                        String s = inst.getOriginal();
                        if (s.equals("CARDNAME attacks each turn if able.")
                                || s.startsWith("CARDNAME attacks specific player each combat if able")
                                || s.equals("CARDNAME attacks each combat if able.")) {
                            mustAttack = true;
                            break;
                        }
                    }
                }
                if (mustAttack || attacker.getController().getMustAttackEntity() != null || attacker.getController().getMustAttackEntityThisTurn() != null) {
                    combat.addAttacker(attacker, defender);
                    attackersLeft.remove(attacker);
                    numForcedAttackers++;
                }
            }
            if (attackersLeft.isEmpty()) {
                return;
            }
        }

        // Lightmine Field: make sure the AI doesn't wipe out its own creatures
        if (lightmineField) {
            doLightmineFieldAttackLogic(attackersLeft, numForcedAttackers, playAggro);
        }
        // Revenge of Ravens: make sure the AI doesn't kill itself and doesn't damage itself unnecessarily
        if (!doRevengeOfRavensAttackLogic(ai, defender, attackersLeft, numForcedAttackers, attackMax)) {
            return;
        }

        if (bAssault) {
            if (LOG_AI_ATTACKS)
                System.out.println("Assault");
            CardLists.sortByPowerDesc(attackersLeft);
            for (Card attacker : attackersLeft) {
                // reached max, breakup
                if (attackMax != -1 && combat.getAttackers().size() >= attackMax)
                    return;

                if (canAttackWrapper(attacker, defender) && this.isEffectiveAttacker(ai, attacker, combat)) {
                    combat.addAttacker(attacker, defender);
                }
            }
            // no more creatures to attack
            return;
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
                this.aiAggression = 6;
                for (Card attacker : this.attackers) {
                    if (canAttackWrapper(attacker, defender) && this.shouldAttack(ai, attacker, this.blockers, combat)) {
                        combat.addAttacker(attacker, defender);
                        return;
                    }
                }
            }
        }

        if (attackMax != -1) {
            // should attack with only max if able.
            CardLists.sortByPowerDesc(this.attackers);
            this.aiAggression = 6;
            for (Card attacker : this.attackers) {
                // reached max, breakup
                if (attackMax != -1 && combat.getAttackers().size() >= attackMax)
                    break;
                if (canAttackWrapper(attacker, defender) && this.shouldAttack(ai, attacker, this.blockers, combat)) {
                    combat.addAttacker(attacker, defender);
                }
            }
            // no more creatures to attack
            return;
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
        
        final Player opp = this.defendingOpponent;
        // get the potential damage and strength of the AI forces
        final List<Card> candidateAttackers = new ArrayList<>();
        int candidateUnblockedDamage = 0;
        for (final Card pCard : this.myList) {
            // if the creature can attack then it's a potential attacker this
            // turn, assume summoning sickness creatures will be able to
            if (ComputerUtilCombat.canAttackNextTurn(pCard) && pCard.getNetCombatDamage() > 0) {
                candidateAttackers.add(pCard);
                candidateUnblockedDamage += ComputerUtilCombat.damageIfUnblocked(pCard, opp, null, false);
                computerForces += 1;
            }
        }

        boolean predictEvasion = (ai.getController().isAI()
                && ((PlayerControllerAi)ai.getController()).getAi().getBooleanProperty(AiProps.COMBAT_ATTRITION_ATTACK_EVASION_PREDICTION));

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
            if (ComputerUtilCombat.canAttackNextTurn(pCard) && pCard.getNetCombatDamage() > 0) {
                nextTurnAttackers.add(pCard);
                candidateCounterAttackDamage += pCard.getNetCombatDamage();
                humanForces += 1; // player forces they might use to attack
            }
            // increment player forces that are relevant to an attritional attack - includes walls

            Card potentialOppBlocker = getCardCanBlockAnAttacker(pCard, candidateAttackers, true);
            if (potentialOppBlocker != null) {
                humanForcesForAttritionalAttack += 1;
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
            humanLifeToDamageRatio = (double) (opp.getLife() - ComputerUtil.possibleNonCombatDamage(ai, opp)) / candidateUnblockedDamage;
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
        int humanLife = opp.getLife();
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
                unblockableDamage += ComputerUtilCombat.damageIfUnblocked(attacker, opp, combat, false);
            }
        }
        for (final Card attacker : nextTurnAttackers) {
            boolean isUnblockableCreature = true;
            // check blockers individually, as the bulk canBeBlocked doesn't
            // check all circumstances
            for (final Card blocker : this.myList) {
                if (CombatUtil.canBlock(attacker, blocker, true)) {
                    isUnblockableCreature = false;
                    break;
                }
            }
            if (isUnblockableCreature) {
                nextUnblockableDamage += ComputerUtilCombat.damageIfUnblocked(attacker, opp, null, false);
            }
        }
        if (unblockableDamage > 0 && !opp.cantLoseForZeroOrLessLife() && opp.canLoseLife()) {
            turnsUntilDeathByUnblockable = 1 + (opp.getLife() - unblockableDamage) / nextUnblockableDamage;
        }
        if (opp.canLoseLife()) {
            doUnblockableAttack = true;
        }
        // *****************
        // end see how long until unblockable attackers will be fatal
        // *****************

        // decide on attack aggression based on a comparison of forces, life
        // totals and other considerations some bad "magic numbers" here
        // TODO replace with nice descriptive variable names
        if (ratioDiff > 0 && doAttritionalAttack) {
            this.aiAggression = 5; // attack at all costs
        } else if ((ratioDiff >= 1 && this.attackers.size() > 1 && (humanLifeToDamageRatio < 2 || outNumber > 0))
        		|| (playAggro && MyRandom.percentTrue(chanceToAttackToTrade) && humanLifeToDamageRatio > 1)) {
            this.aiAggression = 4; // attack expecting to trade or damage player.
        } else if (MyRandom.percentTrue(chanceToAttackToTrade) && humanLifeToDamageRatio > 1
                && defendingOpponent != null
                && ComputerUtil.countUsefulCreatures(ai) > ComputerUtil.countUsefulCreatures(defendingOpponent)
                && ai.getLife() > defendingOpponent.getLife()
                && !ComputerUtilCombat.lifeInDanger(ai, combat)
                && (ComputerUtilMana.getAvailableManaEstimate(ai) > 0) || tradeIfTappedOut
                && (ComputerUtilMana.getAvailableManaEstimate(defendingOpponent) == 0) || MyRandom.percentTrue(extraChanceIfOppHasMana)
                && (!tradeIfLowerLifePressure || (ai.getLifeLostLastTurn() + ai.getLifeLostThisTurn() <
                        defendingOpponent.getLifeLostThisTurn() + defendingOpponent.getLifeLostThisTurn()))) {
            this.aiAggression = 4; // random (chance-based) attack expecting to trade or damage player.
        } else if (ratioDiff >= 0 && this.attackers.size() > 1) {
            this.aiAggression = 3; // attack expecting to make good trades or damage player.
        } else if (ratioDiff + outNumber >= -1 || aiLifeToPlayerDamageRatio > 1
                || ratioDiff * -1 < turnsUntilDeathByUnblockable) {
            // at 0 ratio expect to potentially gain an advantage by attacking first
            // if the ai has a slight advantage
            // or the ai has a significant advantage numerically but only a slight disadvantage damage/life
            this.aiAggression = 2; // attack expecting to destroy creatures/be unblockable
        } else if (doUnblockableAttack) {
            this.aiAggression = 1;
            // look for unblockable creatures that might be
            // able to attack for a bit of fatal damage even if the player is significantly better
        } else {
            this.aiAggression = 0;
        } // stay at home to block

        if ( LOG_AI_ATTACKS )
            System.out.println(this.aiAggression + " = ai aggression");

        // ****************
        // Evaluation the end
        // ****************

        if ( LOG_AI_ATTACKS )
            System.out.println("Normal attack");

        attackersLeft = notNeededAsBlockers(ai, attackersLeft);
        attackersLeft = sortAttackers(attackersLeft);

        if ( LOG_AI_ATTACKS )
            System.out.println("attackersLeft = " + attackersLeft);

        for (int i = 0; i < attackersLeft.size(); i++) {
            final Card attacker = attackersLeft.get(i);
            if (this.aiAggression < 5 && !attacker.hasFirstStrike() && !attacker.hasDoubleStrike()
                    && ComputerUtilCombat.getTotalFirstStrikeBlockPower(attacker, this.defendingOpponent)
                    >= ComputerUtilCombat.getDamageToKill(attacker)) {
                continue;
            }

            if (this.shouldAttack(ai, attacker, this.blockers, combat) && canAttackWrapper(attacker, defender)) {
                combat.addAttacker(attacker, defender);
                // check if attackers are enough to finish the attacked planeswalker
                if (defender instanceof Card) {
                    Card pw = (Card) defender;
                    final int blockNum = this.blockers.size();
                    int attackNum = 0;
                    int damage = 0;
                    List<Card> attacking = combat.getAttackersOf(defender);
                    CardLists.sortByPowerAsc(attacking);
                    for (Card atta : attacking) {
                        if (attackNum >= blockNum || !CombatUtil.canBeBlocked(attacker, this.blockers, combat)) {
                            damage += ComputerUtilCombat.damageIfUnblocked(atta, opp, null, false);
                        } else if (CombatUtil.canBeBlocked(attacker, this.blockers, combat)) {
                            attackNum++;
                        }
                    }
                    // if enough damage: switch to next planeswalker or player
                    if (damage >= pw.getCounters(CounterEnumType.LOYALTY)) {
                        List<Card> pwDefending = combat.getDefendingPlaneswalkers();
                        // look for next planeswalker
                        for (Card walker : Lists.newArrayList(pwDefending)) {
                            if (!combat.getAttackersOf(walker).isEmpty()) {
                                pwDefending.remove(walker);
                            }
                        }
                        if (pwDefending.isEmpty()) {
                            defender = Collections.min(Lists.newArrayList(combat.getDefendingPlayers()), PlayerPredicates.compareByLife());
                        }
                        else {
                            defender = ComputerUtilCard.getBestPlaneswalkerToDamage(pwDefending);
                        }
                    }
                }
            }
        }
    } // getAttackers()

    /**
     * <p>
     * getAttack.
     * </p>
     * 
     * @param c
     *            a {@link forge.game.card.Card} object.
     * @return a int.
     */
    public final static int getAttack(final Card c) {
        int n = c.getNetCombatDamage();

        if (c.hasKeyword(Keyword.DOUBLE_STRIKE)) {
            n *= 2;
        }

        return n;
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
    public final boolean shouldAttack(final Player ai, final Card attacker, final List<Card> defenders, final Combat combat) {
        boolean canBeKilled = false; // indicates if the attacker can be killed
        boolean canBeKilledByOne = false; // indicates if the attacker can be killed by a single blocker
        boolean canKillAll = true; // indicates if the attacker can kill all single blockers
        boolean canKillAllDangerous = true; // indicates if the attacker can kill all single blockers with wither or infect
        boolean isWorthLessThanAllKillers = true;
        boolean canBeBlocked = false;
        int numberOfPossibleBlockers = 0;

        // Is it a creature that has a more valuable ability with a tap cost than what it can do by attacking?
        if ((attacker.hasSVar("NonCombatPriority"))
                && (!attacker.hasKeyword(Keyword.VIGILANCE))) {
            // For each level of priority, enemy has to have life as much as the creature's power
            // so a priority of 4 means the creature will not attack unless it can defeat that player in 4 successful attacks.
            // the lower the priroity, the less willing the AI is to use the creature for attacking.
            // TODO Somehow subtract expected damage of other attacking creatures from enemy life total (how? other attackers not yet declared? Can the AI guesstimate which of their creatures will not get blocked?)
            if (attacker.getCurrentPower() * Integer.parseInt(attacker.getSVar("NonCombatPriority")) < ai.getOpponentsSmallestLifeTotal()) {
                // Check if the card actually has an ability the AI can and wants to play, if not, attacking is fine!
                for (SpellAbility sa : attacker.getSpellAbilities()) {
                    // Do not attack if we can afford using the ability.
                    if (sa.isAbility()) {
                        if (ComputerUtilCost.canPayCost(sa, ai)) {
                            return false;
                        }
                        // TODO Eventually The Ai will need to learn to predict if they have any use for the ability before next untap or not.
                        // TODO abilities that tap enemy creatures should probably only be saved if the enemy has nonzero creatures? Haste can be a threat though...
                    }
                }
            }
        }

        if (!this.isEffectiveAttacker(ai, attacker, combat)) {
            return false;
        }
        boolean hasAttackEffect = attacker.getSVar("HasAttackEffect").equals("TRUE") || attacker.hasStartOfKeyword("Annihilator");
        // is there a gain in attacking even when the blocker is not killed (Lifelink, Wither,...)
        boolean hasCombatEffect = attacker.getSVar("HasCombatEffect").equals("TRUE") 
        		|| "Blocked".equals(attacker.getSVar("HasAttackEffect"));

        // contains only the defender's blockers that can actually block the attacker
        CardCollection validBlockers = CardLists.filter(defenders, new Predicate<Card>() {
            @Override
            public boolean apply(Card defender) {
                return CombatUtil.canBlock(attacker, defender);
            }
        });

        boolean canTrampleOverDefenders = attacker.hasKeyword(Keyword.TRAMPLE) && attacker.getNetCombatDamage() > Aggregates.sum(validBlockers, CardPredicates.Accessors.fnGetNetToughness);

        // used to check that CanKillAllDangerous check makes sense in context where creatures with dangerous abilities are present
        boolean dangerousBlockersPresent = !CardLists.filter(validBlockers, Predicates.or(
                CardPredicates.hasKeyword(Keyword.WITHER), CardPredicates.hasKeyword(Keyword.INFECT),
                CardPredicates.hasKeyword(Keyword.LIFELINK))).isEmpty();

        // total power of the defending creatures, used in predicting whether a gang block can kill the attacker
        int defPower = CardLists.getTotalPower(validBlockers, true, false);

        if (!hasCombatEffect) {
            for (KeywordInterface inst : attacker.getKeywords()) {
                String keyword = inst.getOriginal();
                if (keyword.equals("Wither") || keyword.equals("Infect")
                        || keyword.equals("Lifelink") || keyword.startsWith("Afflict")) {
                    hasCombatEffect = true;
                    break;
                }
            }
        }

        // look at the attacker in relation to the blockers to establish a
        // number of factors about the attacking  context that will be relevant
        // to the attackers decision according to the selected strategy
        for (final Card defender : validBlockers) {
            // if both isWorthLessThanAllKillers and canKillAllDangerous are false there's nothing more to check
            if (isWorthLessThanAllKillers || canKillAllDangerous || numberOfPossibleBlockers < 2) {
                numberOfPossibleBlockers += 1;
                if (isWorthLessThanAllKillers && ComputerUtilCombat.canDestroyAttacker(ai, attacker, defender, combat, false)
                        && !(attacker.hasKeyword(Keyword.UNDYING) && attacker.getCounters(CounterEnumType.P1P1) == 0)) {
                    canBeKilledByOne = true; // there is a single creature on the battlefield that can kill the creature
                    // see if the defending creature is of higher or lower
                    // value. We don't want to attack only to lose value
                    if (isWorthLessThanAllKillers && !attacker.hasSVar("SacMe")
                            && ComputerUtilCard.evaluateCreature(defender) <= ComputerUtilCard.evaluateCreature(attacker)) {
                        isWorthLessThanAllKillers = false;
                    }
                }
                // see if this attacking creature can destroy this defender, if
                // not record that it can't kill everything
                if (canKillAllDangerous && !ComputerUtilCombat.canDestroyBlocker(ai, defender, attacker, combat, false)) {
                    canKillAll = false;
                    if (defender.getSVar("HasCombatEffect").equals("TRUE") || defender.getSVar("HasBlockEffect").equals("TRUE")) {
                        canKillAllDangerous = false;
                    } else {
                        if (defender.hasKeyword(Keyword.WITHER) || defender.hasKeyword(Keyword.INFECT)
                                || defender.hasKeyword(Keyword.LIFELINK)) {
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
        } else if ((canKillAllDangerous || !canBeKilled) && ComputerUtilCard.canBeBlockedProfitably(defendingOpponent, attacker)) {
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
                || (numberOfPossibleBlockers >= 1 && CombatUtil.canAttackerBeBlockedWithAmount(attacker, 1, this.defendingOpponent))
                || (numberOfPossibleBlockers == 2 && CombatUtil.canAttackerBeBlockedWithAmount(attacker, 2, this.defendingOpponent))) {
            canBeBlocked = true;
        }
        // decide if the creature should attack based on the prevailing strategy choice in aiAggression
        switch (this.aiAggression) {
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

    public static List<Card> exertAttackers(List<Card> attackers) {
        List<Card> exerters = Lists.newArrayList();
        for(Card c : attackers) {
            boolean shouldExert = false;

            if (c.hasSVar("EndOfTurnLeavePlay")) {
                // creature would leave the battlefield
                // no pain in exerting it
                shouldExert = true;
            } else if (c.hasKeyword(Keyword.VIGILANCE)) {
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
                    } else if (sa.isCurse() && CardLists.filter(validTargets,
                            CardPredicates.isControlledByAnyOf(c.getController().getOpponents())).isEmpty()) {
                        // e.g. Ahn-Crop Crasher - the effect is only good when aimed at opponent's creatures
                        missTarget = true;
                        break;
                    }
                }

            }

            if (missTarget) {
                continue;
            }

            // A specific AI condition for Exert: if specified on the card, the AI will always
            // exert creatures that meet this condition
            if (c.hasSVar("AIExertCondition")) {
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

            if (!shouldExert && MyRandom.getRandom().nextBoolean()) {
                // TODO Improve when the AI wants to use Exert powers
                shouldExert = true;
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
        if (sa.getApi() != ApiType.Protection || oppList.isEmpty() || getPossibleBlockers(oppList, attackers).isEmpty()) {
            return null; //not protection sa or attacker is already unblockable
        }
        final List<String> choices = ProtectEffect.getProtectionList(sa);
        String color = ComputerUtilCard.getMostProminentColor(getPossibleBlockers(oppList, attackers)), artifact = null;
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
        return null;//should never get here
    }

    private void doLightmineFieldAttackLogic(List<Card> attackersLeft, int numForcedAttackers, boolean playAggro) {
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

    private boolean doRevengeOfRavensAttackLogic(Player ai, GameEntity defender, List<Card> attackersLeft, int numForcedAttackers, int maxAttack) {
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
