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
import java.util.Random;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

import forge.ai.ability.AnimateAi;
import forge.game.GameEntity;
import forge.game.ability.ApiType;
import forge.game.ability.effects.ProtectEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactory;
import forge.game.card.CardLists;
import forge.game.card.CounterType;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.combat.GlobalAttackRestrictions;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;
import forge.util.MyRandom;


//doesHumanAttackAndWin() uses the global variable AllZone.getComputerPlayer()
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

    private final static Random random = new Random();
    private final static int randomInt = random.nextInt();

    private List<Card> oppList; // holds human player creatures
    private List<Card> myList; // holds computer creatures
    
    private final Player ai;
    private Player defendingOpponent;
    
    private int aiAggression = 0; // added by Masher, how aggressive the ai is attack will be depending on circumstances


    /**
     * <p>
     * Constructor for ComputerUtil_Attack2.
     * </p>
     * 
     * @param possibleAttackers
     *            a {@link forge.CardList} object.
     * @param possibleBlockers
     *            a {@link forge.CardList} object.
     */
    public AiAttackController(final Player ai) {
        this.ai = ai;
        this.defendingOpponent = choosePreferredDefenderPlayer();       
        this.oppList = getOpponentCreatures(this.defendingOpponent);
        this.myList = ai.getCreaturesInPlay();
        this.attackers = new ArrayList<Card>();
        for (Card c : myList) {
            if (CombatUtil.canAttack(c, this.defendingOpponent)) {
                attackers.add(c);
            }
        }
        this.blockers = getPossibleBlockers(oppList, this.attackers);
    } // constructor

    public AiAttackController(final Player ai, Card attacker) {
        this.ai = ai;
        this.defendingOpponent = choosePreferredDefenderPlayer();       
        this.oppList = getOpponentCreatures(this.defendingOpponent);
        this.myList = ai.getCreaturesInPlay();
        this.attackers = new ArrayList<Card>();
        if (CombatUtil.canAttack(attacker, this.defendingOpponent)) {
            attackers.add(attacker);
        }
        this.blockers = getPossibleBlockers(oppList, this.attackers);
    } // overloaded constructor to evaluate single specified attacker
    
    public static List<Card> getOpponentCreatures(final Player defender) {
        List<Card> defenders = Lists.newArrayList();
        defenders.addAll(defender.getCreaturesInPlay());
        Predicate<Card> canAnimate = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return !c.isCreature() && !c.isPlaneswalker();
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
                        Card animatedCopy = CardFactory.copyCard(c, true);
                        AnimateAi.becomeAnimated(animatedCopy, c.hasSickness(), sa);
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

    /** Choose opponent for AI to attack here. Expand as necessary. */
    private Player choosePreferredDefenderPlayer() {
        Player defender = ai.getWeakestOpponent(); //Gets opponent with the least life

        if (defender.getLife() < 8) { //Concentrate on opponent within easy kill range
            return defender;
        } else { //Otherwise choose a random opponent to ensure no ganging up on players
            defender = ai.getOpponents().get(MyRandom.getRandom().nextInt(ai.getOpponents().size()));
        }
        return defender;
    }

    /**
     * <p>
     * sortAttackers.
     * </p>
     * 
     * @param in
     *            a {@link forge.CardList} object.
     * @return a {@link forge.CardList} object.
     */
    public final static List<Card> sortAttackers(final List<Card> in) {
        final List<Card> list = new ArrayList<Card>();

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
        
        if ("TRUE".equals(attacker.getSVar("HasAttackEffect"))) {
        	return true;
        }

        final Player opp = this.defendingOpponent;
        if (ComputerUtilCombat.damageIfUnblocked(attacker, opp, combat, true) > 0) {
            return true;
        }
        if (ComputerUtilCombat.poisonIfUnblocked(attacker, opp) > 0) {
            return true;
        }
        if (this.attackers.size() == 1 && attacker.hasKeyword("Exalted") 
        		&& ComputerUtilCombat.predictDamageTo(opp, 1, attacker, true) > 0) {
            return true;
        }

        final CardCollectionView controlledByCompy = ai.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES);
        for (final Card c : controlledByCompy) {
            for (final Trigger trigger : c.getTriggers()) {
                if (ComputerUtilCombat.combatTriggerWillTrigger(attacker, null, trigger, combat)) {
                    return true;
                }
            }
        }
        return false;
    }

    public final static List<Card> getPossibleBlockers(final List<Card> blockers, final List<Card> attackers) {
        List<Card> possibleBlockers = new ArrayList<Card>(blockers);
        possibleBlockers = CardLists.filter(possibleBlockers, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return canBlockAnAttacker(c, attackers, false);
            }
        });
        return possibleBlockers;
    }

    public final static boolean canBlockAnAttacker(final Card c, final List<Card> attackers, final boolean nextTurn) {
        final List<Card> attackerList = new ArrayList<Card>(attackers);
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

    // this checks to make sure that the computer player doesn't lose when the human player attacks
    // this method is used by getAttackers()
    public final List<Card> notNeededAsBlockers(final Player ai, final List<Card> attackers) {
        final List<Card> notNeededAsBlockers = new ArrayList<Card>(attackers);
        int fixedBlockers = 0;
        final List<Card> vigilantes = new ArrayList<Card>();
        //check for time walks
        if (ai.getGame().getPhaseHandler().getNextTurn().equals(ai)) {
            return attackers;
        }
        List<Card> opponentsAttackers = new ArrayList<Card>(oppList);
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
            if (c.hasKeyword("Vigilance")) {
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
            if (!this.doesHumanAttackAndWin(ai, i)) {
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

        // Increase the total number of blockers needed by 1 if Finest Hour in
        // play
        // (human will get an extra first attack with a creature that untaps)
        // In addition, if the computer guesses it needs no blockers, make sure
        // that
        // it won't be surprised by Exalted
        final int humanExaltedBonus = countExaltedBonus(opp);

        if (humanExaltedBonus > 0) {
            final boolean finestHour = opp.isCardInPlay("Finest Hour");

            if ((blockersNeeded == 0 || finestHour) && !this.oppList.isEmpty()) {
                // total attack = biggest creature + exalted, *2 if Rafiq is in play
                int humanBasePower = getAttack(this.oppList.get(0)) + humanExaltedBonus;
                if (finestHour) {
                    // For Finest Hour, one creature could attack and get the
                    // bonus TWICE
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

    // this uses a global variable, which isn't perfect
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
            totalAttack += ComputerUtilCombat.damageIfUnblocked(attacker, ai, null, false);
            totalPoison += ComputerUtilCombat.poisonIfUnblocked(attacker, ai);
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
                final int counters = beastion.getCounters(CounterType.QUEST);
                minCreatures = Math.min(minCreatures, 7 - counters);
            }
            if (this.attackers.size() >= minCreatures) {
                return true;
            }
        }

        CardLists.sortByPowerDesc(this.attackers);

        final List<Card> unblockedAttackers = new ArrayList<Card>();
        final List<Card> remainingAttackers = new ArrayList<Card>(this.attackers);
        final List<Card> remainingBlockers = new ArrayList<Card>(this.blockers);
        final List<Card> blockedAttackers = new ArrayList<Card>();
        final Player opp = this.defendingOpponent;

        for (Card attacker : attackers) {
            if (!CombatUtil.canBeBlocked(attacker, this.blockers, null)
                    || attacker.hasKeyword("You may have CARDNAME assign its combat damage as though it weren't blocked.")) {
                unblockedAttackers.add(attacker);
            }
        }

        for (Card blocker : this.blockers) {
            if (blocker.hasKeyword("CARDNAME can block any number of creatures.")
                    || blocker.hasKeyword("CARDNAME can block an additional ninety-nine creatures.")) {
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
            if (remainingAttackers.isEmpty()) {
                break;
            }
            if (blocker.hasKeyword("CARDNAME can block an additional creature.")) {
                blockedAttackers.add(remainingAttackers.get(0));
                remainingAttackers.remove(0);
                if (remainingAttackers.isEmpty()) {
                    break;
                }
            }
            blockedAttackers.add(remainingAttackers.get(0));
            remainingAttackers.remove(0);
        }
        unblockedAttackers.addAll(remainingAttackers);
        
        int trampleDamage = 0;
        for (Card attacker : blockedAttackers) {
            if (attacker.hasKeyword("Trample")) {
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

        if (ComputerUtilCombat.sumDamageIfUnblocked(unblockedAttackers, opp) + ComputerUtil.possibleNonCombatDamage(ai) 
                + trampleDamage >= opp.getLife()
                && !((opp.cantLoseForZeroOrLessLife() || ai.cantWin()) && opp.getLife() < 1)) {
            return true;
        }

        if (ComputerUtilCombat.sumPoisonIfUnblocked(unblockedAttackers, opp) >= 10 - opp.getPoisonCounters()) {
            return true;
        }

        return false;
    }

    private final GameEntity chooseDefender(final Combat c, final boolean bAssault) {
        final FCollectionView<GameEntity> defs = c.getDefenders();
        if (defs.size() == 1) {
            return defs.getFirst();
        }
        Player prefDefender = (Player) (defs.contains(this.defendingOpponent) ? this.defendingOpponent : defs.get(0));

        final GameEntity entity = ai.getMustAttackEntity();
        if (null != entity) {
            int n = defs.indexOf(entity);
            if (-1 == n) {
                System.out.println("getMustAttackEntity() returned something not in defenders.");
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
                return pwDefending.get(0);
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
        // if this method is called multiple times during a turn,
        // it will always return the same value
        // randomInt is used so that the computer doesn't always
        // do the same thing on turn 3 if he had the same creatures in play
        // I know this is a little confusing
        
        random.setSeed(ai.getGame().getPhaseHandler().getTurn() + AiAttackController.randomInt);

        if (this.attackers.isEmpty()) {
            return;
        }

        final boolean bAssault = this.doAssault(ai);
        // Determine who will be attacked
        GameEntity defender = this.chooseDefender(combat, bAssault);
        List<Card> attackersLeft = new ArrayList<Card>(this.attackers);

        // TODO probably use AttackConstraints instead of only GlobalAttackRestrictions?
        GlobalAttackRestrictions restrict = GlobalAttackRestrictions.getGlobalRestrictions(ai, combat.getDefenders());
        final int attackMax = restrict.getMax();

        if (attackMax == 0) {
            //  can't attack anymore
            return;
        }

        // Attackers that don't really have a choice
        for (final Card attacker : this.attackers) {
            if (!CombatUtil.canAttack(attacker, defender)) {
                attackersLeft.remove(attacker);
                continue;
            }
            boolean mustAttack = false;
            if (attacker.getSVar("MustAttack").equals("True")) {
                mustAttack = true;
            } else if (attacker.getSVar("EndOfTurnLeavePlay").equals("True")
                    && isEffectiveAttacker(ai, attacker, combat)) {
                mustAttack = true;
            } else {
                for (String s : attacker.getKeywords()) {
                    if (s.equals("CARDNAME attacks each turn if able.")
                            || s.startsWith("CARDNAME attacks specific player each combat if able")
                            || s.equals("CARDNAME attacks each combat if able.")) {
                        mustAttack = true;
                        break;
                    }
                }
            }
            if (mustAttack || attacker.getController().getMustAttackEntity() != null) {
                combat.addAttacker(attacker, defender);
                attackersLeft.remove(attacker);
            }
        }
        if (attackersLeft.isEmpty()) {
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

                if (CombatUtil.canAttack(attacker, defender) && this.isEffectiveAttacker(ai, attacker, combat)) {
                    combat.addAttacker(attacker, defender);
                }
            }
            // no more creatures to attack
            return;
        }

        // Cards that are remembered to attack anyway (e.g. temporarily stolen creatures)
        if (ai.getController() instanceof PlayerControllerAi) {
            // Only do this if |ai| is actually an AI - as we could be trying to predict how the human will attack.
            AiCardMemory aiMemory = ((PlayerControllerAi) ai.getController()).getAi().getCardMemory();
            for (Card attacker : this.attackers) {
                if (aiMemory.isRememberedCard(attacker, AiCardMemory.MemorySet.MANDATORY_ATTACKERS)) {
                    combat.addAttacker(attacker, defender);
                    attackersLeft.remove(attacker);
                }
            }
        }
        
        // Exalted
        if (combat.getAttackers().isEmpty()) {
            boolean exalted = false;
            int exaltedCount = 0;
            for (Card c : ai.getCardsIn(ZoneType.Battlefield)) {
                if (c.getName().equals("Rafiq of the Many") || c.getName().equals("Battlegrace Angel")) {
                    exalted = true;
                    break;
                }
                if (c.getName().equals("Finest Hour") && ai.getGame().getPhaseHandler().isFirstCombat()) {
                    exalted = true;
                    break;
                }
                if (c.hasKeyword("Exalted")) {
                    exaltedCount++;
                    if (exaltedCount > 2) {
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
                    if (CombatUtil.canAttack(attacker, defender) && this.shouldAttack(ai, attacker, this.blockers, combat)) {
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
                if (CombatUtil.canAttack(attacker, defender) && this.shouldAttack(ai, attacker, this.blockers, combat)) {
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
        final List<Card> nextTurnAttackers = new ArrayList<Card>();
        int candidateCounterAttackDamage = 0;
        
        final Player opp = this.defendingOpponent;
        // get the potential damage and strength of the AI forces
        final List<Card> candidateAttackers = new ArrayList<Card>();
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

        for (final Card pCard : this.oppList) {
            // if the creature can attack next turn add it to counter attackers list
            if (ComputerUtilCombat.canAttackNextTurn(pCard) && pCard.getNetCombatDamage() > 0) {
                nextTurnAttackers.add(pCard);
                candidateCounterAttackDamage += pCard.getNetCombatDamage();
                humanForces += 1; // player forces they might use to attack
            }
            // increment player forces that are relevant to an attritional attack - includes walls
            if (canBlockAnAttacker(pCard, candidateAttackers, true)) {
                humanForcesForAttritionalAttack += 1;
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
            humanLifeToDamageRatio = (double) (opp.getLife() - ComputerUtil.possibleNonCombatDamage(ai)) / candidateUnblockedDamage;
        }

        // determine if the ai outnumbers the player
        final int outNumber = computerForces - humanForces;

        for (Card blocker : this.blockers) {
            if (blocker.hasKeyword("CARDNAME can block any number of creatures.")) {
                aiLifeToPlayerDamageRatio--;
            }
        }

        // compare the ratios, higher = better for ai
        final double ratioDiff = aiLifeToPlayerDamageRatio - humanLifeToDamageRatio;

        // *********************
        // if outnumber and superior ratio work out whether attritional all out
        // attacking will work
        // attritional attack will expect some creatures to die but to achieve
        // victory by sheer weight
        // of numbers attacking turn after turn. It's not calculate very
        // carefully, the accuracy
        // can probably be improved
        // *********************
        boolean doAttritionalAttack = false;
        // get list of attackers ordered from low power to high
        CardLists.sortByPowerAsc(this.attackers);
        // get player life total
        int humanLife = opp.getLife();
        // get the list of attackers up to the first blocked one
        final List<Card> attritionalAttackers = new ArrayList<Card>();
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
        // System.out.println(doAttritionalAttack + " = do attritional attack");
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
        // totals and other considerations
        // some bad "magic numbers" here, TODO replace with nice descriptive
        // variable names
        boolean playAggro = false;
        if (ai.getController().isAI()) {
            playAggro = ((PlayerControllerAi) ai.getController()).getAi().getProperty(AiProps.PLAY_AGGRO).equals("true");
        }

        if (ratioDiff > 0 && doAttritionalAttack) {
            this.aiAggression = 5; // attack at all costs
        } else if ((ratioDiff >= 1 && this.attackers.size() > 1 && (humanLifeToDamageRatio < 2 || outNumber > 0))
        		|| (playAggro && humanLifeToDamageRatio > 1)) {
            this.aiAggression = 4; // attack expecting to trade or damage player.
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
            System.out.println(String.valueOf(this.aiAggression) + " = ai aggression");

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

            if (this.shouldAttack(ai, attacker, this.blockers, combat) && CombatUtil.canAttack(attacker, defender)) {
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
                    if (damage >= pw.getCounters(CounterType.LOYALTY)) {
                        List<Card> pwDefending = combat.getDefendingPlaneswalkers();
                        boolean found = false;
                        // look for next planeswalker
                        for (Card walker : pwDefending) {
                            if (combat.getAttackersOf(walker).isEmpty()) {
                                defender = walker;
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            defender = combat.getDefendingPlayers().get(0);
                        }
                    }
                }
            }
        }
    } // getAttackers()

    /**
     * <p>
     * countExaltedBonus.
     * </p>
     * 
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a int.
     */
    public final static int countExaltedBonus(final Player player) {
        int bonus = 0;
        for (Card c : player.getCardsIn(ZoneType.Battlefield)) {
            bonus += c.getAmountOfKeyword("Exalted");
        }

        return bonus;
    }

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

        if (c.hasKeyword("Double Strike")) {
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
     *            a {@link forge.CardList} object.
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
        

        if (!this.isEffectiveAttacker(ai, attacker, combat)) {
            return false;
        }
        boolean hasAttackEffect = attacker.getSVar("HasAttackEffect").equals("TRUE") || attacker.hasStartOfKeyword("Annihilator");
        // is there a gain in attacking even when the blocker is not killed (Lifelink, Wither,...)
        boolean hasCombatEffect = attacker.getSVar("HasCombatEffect").equals("TRUE") 
        		|| "Blocked".equals(attacker.getSVar("HasAttackEffect"));
        if (!hasCombatEffect) {
            for (String keyword : attacker.getKeywords()) {
                if (keyword.equals("Wither") || keyword.equals("Infect") || keyword.equals("Lifelink")) {
                    hasCombatEffect = true;
                    break;
                }
            }
        }

        // look at the attacker in relation to the blockers to establish a
        // number of factors about the attacking
        // context that will be relevant to the attackers decision according to
        // the selected strategy
        for (final Card defender : defenders) {
            // if both isWorthLessThanAllKillers and canKillAllDangerous are false there's nothing more to check
            if ((isWorthLessThanAllKillers || canKillAllDangerous || numberOfPossibleBlockers < 2)
                    && CombatUtil.canBlock(attacker, defender)) {
                numberOfPossibleBlockers += 1;
                if (isWorthLessThanAllKillers && ComputerUtilCombat.canDestroyAttacker(ai, attacker, defender, combat, false)
                        && !(attacker.hasKeyword("Undying") && attacker.getCounters(CounterType.P1P1) == 0)) {
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
                        for (String keyword : defender.getKeywords()) {
                            if (keyword.equals("Wither") || keyword.equals("Infect") || keyword.equals("Lifelink")) {
                                canKillAllDangerous = false;
                                break;
                                // there is a creature that can survive an attack from this creature
                                // and combat will have negative effects
                            }
                        }
                    }
                }
            }
        }
        
        if (!attacker.hasKeyword("vigilance") && ComputerUtilCard.canBeKilledByRoyalAssassin(ai, attacker)) {
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
        }

        if (numberOfPossibleBlockers > 2 
                || (numberOfPossibleBlockers >= 1 && CombatUtil.canAttackerBeBlockedWithAmount(attacker, 1, combat))
                || (numberOfPossibleBlockers == 2 && CombatUtil.canAttackerBeBlockedWithAmount(attacker, 2, combat))) {
            canBeBlocked = true;
        }
        /*System.out.println(attacker + " canBeKilledByOne: " + canBeKilledByOne + " canKillAll: "
                + canKillAll + " isWorthLessThanAllKillers: " + isWorthLessThanAllKillers + " canBeBlocked: " + canBeBlocked);*/
        // decide if the creature should attack based on the prevailing strategy
        // choice in aiAggression
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
        case 4: // expecting to at least trade with something
            if (canKillAll || (canKillAllDangerous && !canBeKilledByOne) || !canBeBlocked) {
                if (LOG_AI_ATTACKS)
                    System.out.println(attacker.getName() + " = attacking expecting to at least trade with something");
                return true;
            }
            break;
        case 3: // expecting to at least kill a creature of equal value or not be blocked
            if ((canKillAll && isWorthLessThanAllKillers)
                    || ((canKillAllDangerous || hasAttackEffect || hasCombatEffect) && !canBeKilledByOne)
                    || !canBeBlocked) {
                if (LOG_AI_ATTACKS)
                    System.out.println(attacker.getName() + " = attacking expecting to kill creature or cause damage, or is unblockable");
                return true;
            }
            break;
        case 2: // attack expecting to attract a group block or destroying a single blocker and surviving
            if (!canBeBlocked || ((canKillAll || hasAttackEffect || hasCombatEffect) && !canBeKilledByOne &&
                    (canKillAllDangerous || !canBeKilled))) {
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
    
    /**
     * Find a protection type that will make an attacker unblockable.
     * @param sa ability belonging to ApiType.Protection
     * @return colour string or "artifacts", null if no possible choice exists
     */
    public String toProtectAttacker(SpellAbility sa) {
        //AiAttackController is created with the selected attacker as the only entry in "attackers"
        if (sa.getApi() != ApiType.Protection || oppList.isEmpty() || getPossibleBlockers(oppList, attackers).isEmpty()) {
            return null;    //not protection sa or attacker is already unblockable
        }
        final List<String> choices = ProtectEffect.getProtectionList(sa);
        String color = ComputerUtilCard.getMostProminentColor(getPossibleBlockers(oppList, attackers)), artifact = null;
        if (choices.contains("artifacts")) {
            artifact = "artifacts"; //flag to indicate that protection from artifacts is available
        }
        if (!choices.contains(color)) {
            color = null;
        }
        for (Card c : oppList) {   //find a blocker that ignores the currently selected protection
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
            if (color == null && artifact == null) {  //nothing can make the attacker unblockable
                return null;
            }
        }
        if (color != null) {
            return color;
        }
        if (artifact != null) {
            return artifact;
        }
        return null;    //should never get here
    }

} // end class ComputerUtil_Attack2
