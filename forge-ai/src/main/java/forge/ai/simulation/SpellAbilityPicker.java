package forge.ai.simulation;

import java.util.ArrayList;
import java.util.List;

import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilAbility;
import forge.ai.ComputerUtilCost;
import forge.ai.simulation.GameStateEvaluator.Score;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityCondition;
import forge.game.spellability.TargetChoices;

public class SpellAbilityPicker {
    private Game game;
    private Player player;
    private Score bestScore;
    private boolean printOutput;

    public SpellAbilityPicker(Game game, Player player) {
        this.game = game;
        this.player = player;
    }
    
    private void print(String str) {
        if (printOutput) {
            System.out.println(str);
        }
    }
    
    public SpellAbility chooseSpellAbilityToPlay(SimulationController controller, final List<SpellAbility> all, boolean skipCounter) {
        printOutput = false;
        if (controller == null) {
            controller = new SimulationController();
            printOutput = true;
        }
        print("---- choose ability  (phase = " +  game.getPhaseHandler().getPhase() + ")");
        
        List<SpellAbility> candidateSAs = new ArrayList<>();
        for (final SpellAbility sa : ComputerUtilAbility.getOriginalAndAltCostAbilities(all, player)) {
            // Don't add Counterspells to the "normal" playcard lookups
            if (skipCounter && sa.getApi() == ApiType.Counter) {
                continue;
            }
            if (sa.isManaAbility()) {
                continue;
            }
            sa.setActivatingPlayer(player);
            
            AiPlayDecision opinion = canPlayAndPayForSim(sa);
            print("  " + opinion + ": " + sa);
            // PhaseHandler ph = game.getPhaseHandler();
            // System.out.printf("Ai thinks '%s' of %s -> %s @ %s %s >>> \n", opinion, sa.getHostCard(), sa, Lang.getPossesive(ph.getPlayerTurn().getName()), ph.getPhase());
            
            if (opinion != AiPlayDecision.WillPlay)
                continue;
            candidateSAs.add(sa);
        }
        if (candidateSAs.isEmpty()) {
            return null;
        }
        SpellAbility bestSa = null;
        print("Evaluating...");
        GameSimulator simulator = new GameSimulator(controller, game, player);
        // FIXME: This is wasteful, we should re-use the same simulator...
        Score origGameScore = simulator.getScoreForOrigGame();
        Score bestSaValue = origGameScore;
        for (final SpellAbility sa : candidateSAs) {
            print(abilityToString(sa));;
            Score value = evaluateSa(controller, sa);
            if (value.value > bestSaValue.value) {
                bestSaValue = value;
                bestSa = sa;
            }
        }

        // To make the AI hold-off on playing creatures in MAIN1 if they give no other benefits,
        // check the score for the bestSA while counting summon sick creatures for 0.
        // Do it here on the best SA, rather than for all evaluations, so that if the best SA
        // is indeed a creature spell, we don't pick something else to play now and then have
        // no mana to play the truly best SA post-combat.
        if (bestSa != null && bestSaValue.summonSickValue <= origGameScore.summonSickValue) {
            bestSa = null;
        }

        print("BEST: " + abilityToString(bestSa) + " SCORE: " + bestSaValue);
        this.bestScore = bestSaValue;
        return bestSa;
    }
 
    public Score getScoreForChosenAbility() {
        return bestScore;
    }

    public static String abilityToString(SpellAbility sa) {
        String saString = "N/A";
        if (sa != null) {
            saString = sa.toString();
            if (sa.usesTargeting()) {
                saString += " (targets: " + sa.getTargets().getTargetedString() + ")";
            }
        }
        return saString;
    }

    private boolean shouldWaitForLater(final SpellAbility sa) {
        final PhaseType phase = game.getPhaseHandler().getPhase();
        final boolean isEarlyPhase = phase == PhaseType.UNTAP || phase == PhaseType.UPKEEP || phase == PhaseType.DRAW;

        // Until the AI can be made smarter, hold off playing instants until MAIN1,
        // so that they can be compared to sorcery-speed spells. Else, the AI is too
        // eager to play them.
        if (isEarlyPhase) {
            // Only hold off if this spell can actually be played in MAIN1.
            final SpellAbilityCondition conditions = sa.getConditions();
            if (conditions == null) {
                return true;
            }
            List<PhaseType> phases = conditions.getPhases();
            if (phases.isEmpty() || phases.contains(PhaseType.MAIN1)) {
                return true;
            }
        }

        return false;
    }
    
    private AiPlayDecision canPlayAndPayForSim(final SpellAbility sa) {
        if (!sa.canPlay()) {
            return AiPlayDecision.CantPlaySa;
        }
        SpellAbilityCondition conditions = sa.getConditions();
        if (conditions != null && !conditions.areMet(sa)) {
            return AiPlayDecision.CantPlaySa;
        }

        if (!ComputerUtilCost.canPayCost(sa, player)) {
            return AiPlayDecision.CantAfford;
        }

        if (shouldWaitForLater(sa)) {
            return AiPlayDecision.AnotherTime;
        }

        return AiPlayDecision.WillPlay;
    }

    private Score evaluateSa(SimulationController controller, SpellAbility sa) {
        GameSimulator.debugPrint("Evaluate SA: " + sa);
        if (!sa.usesTargeting()) {
            GameSimulator simulator = new GameSimulator(controller, game, player);
            return simulator.simulateSpellAbility(sa);
        }
        GameSimulator.debugPrint("Checking out targets");
        PossibleTargetSelector selector = new PossibleTargetSelector(game, player, sa);
        Score bestScore = new Score(Integer.MIN_VALUE);
        TargetChoices tgt = null;
        while (selector.selectNextTargets()) {
            GameSimulator.debugPrint("Trying targets: " + sa.getTargets().getTargetedString());
            GameSimulator simulator = new GameSimulator(controller, game, player);
            Score score = simulator.simulateSpellAbility(sa);
            if (score.value > bestScore.value) {
                bestScore = score;
                tgt = sa.getTargets();
                sa.resetTargets();
            }
        }
        if (tgt != null) {
            sa.setTargets(tgt);
        }
        return bestScore;
    }

}
