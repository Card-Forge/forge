package forge.ai.ability;

import com.google.common.collect.Iterables;

import forge.ai.*;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.combat.Combat;
import forge.game.cost.Cost;
import forge.game.cost.CostPayLife;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class PumpAllAi extends PumpAiBase {

    // What the AI charges itself for spending life, relative to how much of its total is going.
    // Spending half of a healthy life total costs about as much as losing one creature.
    private static final int LIFE_VALUE = 400;

    // How many values of X we are willing to simulate a combat for before settling.
    private static final int SURVIVAL_CHECKS = 3;

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        final Cost abCost = sa.getPayCosts();
        final String logic = sa.getParamOrDefault("AILogic", "");

        if (logic.equals("UntapCombatTrick")) {
            PhaseHandler ph = ai.getGame().getPhaseHandler();
            if (!(ph.is(PhaseType.COMBAT_DECLARE_BLOCKERS, ai)
                    || (!ph.getPlayerTurn().equals(ai) && ph.is(PhaseType.COMBAT_DECLARE_ATTACKERS)))) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }

        if (abCost != null && source.hasSVar("AIPreference")) {
            if (!ComputerUtilCost.checkSacrificeCost(ai, abCost, source, sa, true)) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }
        
        final Player opp = ai.getStrongestOpponent();

        if (sa.usesTargeting()) {
            if (sa.canTarget(opp) && sa.isCurse()) {
                sa.resetTargets();
                sa.getTargets().add(opp);
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }

            if (sa.canTarget(ai) && !sa.isCurse()) {
                sa.resetTargets();
                sa.getTargets().add(ai);
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
        }

        final String valid = sa.getParamOrDefault("ValidCards", "");

        CardCollection comp = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), valid, source.getController(), source, sa);
        // A mass pump lands on every opponent's board at once, so weigh all of them. In a two
        // player game this is the same list getStrongestOpponent() was giving.
        CardCollection human = CardLists.getValidCards(ai.getOpponents().getCardsIn(ZoneType.Battlefield), valid, source.getController(), source, sa);

        // Nothing else announces X for a -X/-X sweep: PumpAll has an AI class, so the generic X
        // handling in AiController is skipped, and on cards like Toxic Deluge the X lives in a
        // non-mana cost (PayLife<X>) that path would not see either. Without this the amounts
        // below both read 0 and the sweep evaluates as -0/-0.
        if (sa.isCurse() && usesPaidX(sa)) {
            final SweepChoice sweep = chooseXForSweep(ai, sa, comp, human);
            if (sweep.x() <= 0) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            // Clearing the way for a lethal swing is worth it whatever the card values below say.
            if (sweep.lethal()) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
        }

        final int power = AbilityUtils.calculateAmount(source, sa.getParam("NumAtt"), sa);
        final int defense = AbilityUtils.calculateAmount(source, sa.getParam("NumDef"), sa);
        final List<String> keywords = sa.hasParam("KW") ? Arrays.asList(sa.getParam("KW").split(" & ")) : new ArrayList<>();
        final PhaseType phase = game.getPhaseHandler().getPhase();

        if (sa.isCurse()) {
            if (defense < 0) { // try to destroy creatures
                // leaves all creatures that will be destroyed
                comp = CardLists.filter(comp, c -> diesToCurse(c, defense));
                human = CardLists.filter(human, c -> diesToCurse(c, defense));
            } // -X/-X end
            else if (power < 0) { // -X/-0
                if (phase.isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS)
                        || phase.isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)
                        || game.getPhaseHandler().isPlayerTurn(sa.getActivatingPlayer())
                        || game.getReplacementHandler().isPreventCombatDamageThisTurn()) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
                int totalPower = 0;
                for (Card c : human) {
                    if (combat == null || !combat.isAttacking(c)) {
                        continue;
                    }
                    totalPower += Math.min(c.getNetPower(), power * -1);
                    if (phase == PhaseType.COMBAT_DECLARE_BLOCKERS && combat.isUnblocked(c)) {
                        if (ComputerUtilCombat.lifeInDanger(sa.getActivatingPlayer(), combat)) {
                            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                        }
                        totalPower += Math.min(c.getNetPower(), power * -1);
                    }
                    if (totalPower >= power * -2) {
                        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                    }
                }
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            } // -X/-0 end
            
            if (comp.isEmpty() && ComputerUtil.activateForCost(sa, ai)) {
            	return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }

            // evaluate both lists and pass only if human creatures are more valuable
            boolean result = (ComputerUtilCard.evaluateCreatureList(comp) + 200) < ComputerUtilCard.evaluateCreatureList(human);
            return result ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        } // end Curse

        if (!game.getStack().isEmpty()) {
            boolean result = pumpAgainstRemoval(ai, sa, comp);
            return result ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        boolean result = ai.getCreaturesInPlay().anyMatch(c -> c.isValid(valid, source.getController(), source, sa)
                && ComputerUtilCard.shouldPumpCard(ai, sa, c, defense, power, keywords));
        return result ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    @Override
    public AiAbilityDecision chkDrawback(Player aiPlayer, SpellAbility sa) {
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, SpellAbility sa, boolean mandatory) {
        // it might help so take it
        if (!sa.usesTargeting() && !sa.isCurse() && sa.hasParam("ValidCards") && sa.getParam("ValidCards").contains("YouCtrl")) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        // important to call canPlay first so targets are added if needed
        AiAbilityDecision decision = canPlay(ai, sa);
        if (mandatory && !decision.decision().willingToPlay()) {
            return new AiAbilityDecision(50, AiPlayDecision.MandatoryPlay);
        }
        return decision;
    }

    /** The X the AI settled on for a sweep, and whether taking it simply wins this turn. */
    private record SweepChoice(int x, boolean lethal) {}

    private static boolean usesPaidX(final SpellAbility sa) {
        if (!sa.getParamOrDefault("NumDef", "").endsWith("X") || !"Count$xPaid".equals(sa.getSVar("X"))) {
            return false;
        }
        // Only choose X where this ability is the thing announcing it. On triggers and sub-abilities
        // (The Meathook Massacre, Orcus) X was already paid when the spell was cast, so there is no
        // X left to find here and the AI would refuse an otherwise fine sweep.
        final Cost cost = sa.getPayCosts();
        return cost != null && cost.hasXInAnyCostPart();
    }

    private static boolean pumpsPower(final SpellAbility sa) {
        return sa.getParamOrDefault("NumAtt", "").startsWith("+");
    }

    private static boolean paysLifeForX(final SpellAbility sa) {
        final Cost cost = sa.getPayCosts();
        final CostPayLife part = cost == null ? null : cost.getCostPartByType(CostPayLife.class);
        return part != null && "X".equals(part.getAmount());
    }

    /**
     * Picks how big a -X/-X sweep to pay for and records it on the ability, returning the chosen X.
     * Returns 0 when no value of X is worth casting at, so the caller can bail out.
     */
    private SweepChoice chooseXForSweep(final Player ai, final SpellAbility sa, final CardCollection own, final CardCollection foes) {
        // Also sets X to its maximum, which is what the amounts would otherwise be read at.
        final int maxX = ComputerUtilCost.setMaxXValue(sa, ai, sa.isTrigger());
        if (maxX <= 0) {
            return new SweepChoice(0, false);
        }

        // Which creatures die only changes at the toughnesses actually on the battlefield, so try
        // those values instead of every point up to maxX - otherwise the AI walks its whole life
        // total one point at a time to reach the same answer. A superset is fine; diesToCurse stays
        // the single authority on what any given X actually kills.
        final SortedSet<Integer> candidates = new TreeSet<>();
        for (Card c : Iterables.concat(own, foes)) {
            addCandidate(candidates, c.getNetToughness(), maxX);
            addCandidate(candidates, ComputerUtilCombat.getDamageToKill(c, false), maxX);
        }

        final boolean paysLife = paysLifeForX(sa);
        // Power the pump hands out lasts only until end of turn, so it matters to whoever still has
        // a combat coming: on an opponent's turn their survivors swing at us with it, on our own
        // turn ours use it and theirs has expired long before they untap.
        final boolean pumps = pumpsPower(sa);
        final boolean ourTurn = ai.getGame().getPhaseHandler().isPlayerTurn(ai);

        // Rank the candidates on card value alone. Both checks below simulate a combat, so they
        // stay out of this loop and are asked only about the few values that could be chosen.
        final Map<Integer, Integer> scores = new HashMap<>();
        int cheapestLethal = 0;
        for (final int x : candidates) {
            int score = ComputerUtilCard.evaluateCreatureList(CardLists.filter(foes, c -> diesToCurse(c, -x)))
                    - ComputerUtilCard.evaluateCreatureList(CardLists.filter(own, c -> diesToCurse(c, -x)));
            if (paysLife) {
                score -= LIFE_VALUE * x / Math.max(1, ai.getLife());
            }
            if (score > 0) {
                scores.put(x, score);
            }
            // Candidates ascend, so this keeps the cheapest X that could reach.
            if (cheapestLethal == 0 && couldReachLethal(ai, own, x, pumps && ourTurn ? x : 0)) {
                cheapestLethal = x;
            }
        }

        // Mirrors DamageAllAi: a sweep that just wins beats any card-value comparison. Only the
        // cost has to be affordable here - if the swing is lethal there is no next combat to live
        // through, so what the survivors could have hit back with does not matter.
        if (cheapestLethal > 0 && canAfford(ai, sa, cheapestLethal, paysLife)
                && winsThisTurn(ai, own, foes, cheapestLethal, pumps && ourTurn ? cheapestLethal : 0)) {
            sa.setXManaCostPaid(cheapestLethal);
            return new SweepChoice(cheapestLethal, true);
        }

        // Otherwise take the most valuable sweep we can actually live through. Sorted by value,
        // then by size, so the cheapest X wins when a larger one kills nothing extra.
        final List<Integer> ranked = new ArrayList<>(scores.keySet());
        ranked.sort((a, b) -> {
            final int byValue = Integer.compare(scores.get(b), scores.get(a));
            return byValue != 0 ? byValue : Integer.compare(a, b);
        });
        int checked = 0;
        for (final int x : ranked) {
            if (checked++ >= SURVIVAL_CHECKS) {
                break;
            }
            if (survivesSweep(ai, sa, own, foes, x, paysLife, pumps && !ourTurn ? x : 0)) {
                sa.setXManaCostPaid(x);
                return new SweepChoice(x, false);
            }
        }
        return new SweepChoice(0, false);
    }

    /**
     * Cheap necessary condition for winsThisTurn: no attack can deal more than every creature we
     * keep connecting unblocked, so anything short of that is not worth planning an attack for.
     */
    private static boolean couldReachLethal(final Player ai, final CardCollection own, final int x, final int boost) {
        final CardCollection survivors = CardLists.filter(own, c -> !diesToCurse(c, -x));
        for (final Player opp : ai.getOpponents()) {
            if (ComputerUtilCombat.sumDamageIfUnblocked(survivors, opp) + boost * survivors.size() >= opp.getLife()) {
                return true;
            }
        }
        return false;
    }

    private static void addCandidate(final SortedSet<Integer> candidates, final int x, final int maxX) {
        if (x > 0 && x <= maxX) {
            candidates.add(x);
        }
    }

    /**
     * Whether the AI still stands up to the coming combats once it has paid for this sweep and both
     * boards have lost whatever it kills.
     */
    /** Whether the cost can be paid at all, and paying it does not kill the AI outright. */
    private static boolean canAfford(final Player ai, final SpellAbility sa, final int x, final boolean paysLife) {
        if (!paysLife) {
            return true;
        }
        return ai.canPayLife(x, false, sa) && (ai.getLife() > x || ai.cantLoseForZeroOrLessLife());
    }

    private static boolean survivesSweep(final Player ai, final SpellAbility sa, final CardCollection own,
            final CardCollection foes, final int x, final boolean paysLife, final int boost) {
        if (!canAfford(ai, sa, x, paysLife)) {
            return false;
        }
        final CardCollection ourDead = CardLists.filter(own, c -> diesToCurse(c, -x));
        final CardCollection theirDead = CardLists.filter(foes, c -> diesToCurse(c, -x));

        // Cheap and pessimistic first: assume nothing gets blocked. Real blocks only ever reduce
        // that, so if we live through the worst case there is nothing to simulate.
        final CardCollection survivors = CardLists.filter(foes, c -> !diesToCurse(c, -x));
        final int worstCase = ComputerUtilCombat.sumDamageIfUnblocked(survivors, ai) + boost * survivors.size();
        if (ai.getLife() - (paysLife ? x : 0) > worstCase) {
            return true;
        }
        // The life goes before that combat, and a survivor's boost is charged per head, which the
        // simulation below has no way to model.
        final int lifeCost = (paysLife ? x : 0) + boost * survivors.size();
        return ComputerUtil.predictNextCombatsRemainingLife(ai, true, false, lifeCost, ourDead,
                ai.getOpponents(), theirDead) != Integer.MIN_VALUE;
    }

    /**
     * Whether clearing the board at this X leaves the AI able to finish an opponent off this turn.
     * Asks the attack planner rather than guessing, so fog, evasion and the rest are accounted for.
     */
    private static boolean winsThisTurn(final Player ai, final CardCollection own, final CardCollection foes,
            final int x, final int boost) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            return false; // no attack step left to spend it on
        }

        final AiAttackController atk = new AiAttackController(ai);
        for (final Card c : foes) {
            if (diesToCurse(c, -x)) {
                atk.removeBlocker(c);
            }
        }
        for (final Card c : own) {
            if (diesToCurse(c, -x)) {
                atk.removeAttacker(c);
            }
        }

        final Combat planned = new Combat(ai);
        atk.declareAttackers(planned);
        for (final Player opp : ai.getOpponents()) {
            final int attacking = planned.getAttackersOf(opp).size();
            if (attacking > 0 && ComputerUtilCombat.lifeThatWouldRemain(opp, planned) - boost * attacking <= 0) {
                return true;
            }
        }
        return false;
    }

    boolean pumpAgainstRemoval(Player ai, SpellAbility sa, List<Card> comp) {
        final List<GameObject> objects = ComputerUtil.predictThreatenedObjects(sa.getActivatingPlayer(), sa, true);
        for (final Card c : comp) {
            if (objects.contains(c)) {
                return true;
            }
        }
        return false;
    }
}
