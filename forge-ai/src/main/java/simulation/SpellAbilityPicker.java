package simulation;

import java.util.ArrayList;

import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCost;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetChoices;

public class SpellAbilityPicker {
    private static boolean USE_SIMULATION = false;
    private Game game;
    private Player player;

    public SpellAbilityPicker(Game game, Player player) {
        this.game = game;
        this.player = player;
    }

    public SpellAbility chooseSpellAbilityToPlay(final ArrayList<SpellAbility> originalAndAltCostAbilities, boolean skipCounter) {
        if (!USE_SIMULATION)
            return null;

        System.out.println("----\nchooseSpellAbilityToPlay game " + game.toString());
        System.out.println("---- (phase = " +  game.getPhaseHandler().getPhase() + ")");

        ArrayList<SpellAbility> candidateSAs = new ArrayList<>();
        for (final SpellAbility sa : originalAndAltCostAbilities) {
            // Don't add Counterspells to the "normal" playcard lookups
            if (skipCounter && sa.getApi() == ApiType.Counter) {
                continue;
            }
            sa.setActivatingPlayer(player);
            
            AiPlayDecision opinion = canPlayAndPayForSim(sa);
            System.out.println("  " + opinion + ": " + sa);
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
        System.out.println("Evaluating...");
        GameSimulator simulator = new GameSimulator(game);
        // FIXME: This is wasteful, we should re-use the same simulator...
        int bestSaValue = simulator.getScoreForOrigGame();        
        for (final SpellAbility sa : candidateSAs) {
            int value = evaluateSa(sa);
            if (value > bestSaValue) {
                bestSaValue = value;
                bestSa = sa;
            }
        }
        
        System.out.println("BEST: " + bestSa + " SCORE: " + bestSaValue);
        return bestSa;
    }     

    private AiPlayDecision canPlayAndPayForSim(final SpellAbility sa) {
        if (!sa.canPlay()) {
            return AiPlayDecision.CantPlaySa;
        }
        if (sa.getConditions() != null && !sa.getConditions().areMet(sa)) {
            return AiPlayDecision.CantPlaySa;
        }

        /*
        AiPlayDecision op = canPlaySa(sa);
        if (op != AiPlayDecision.WillPlay) {
            return op;
        }
        */
        return ComputerUtilCost.canPayCost(sa, player) ? AiPlayDecision.WillPlay : AiPlayDecision.CantAfford;
    }

    private int evaluateSa(SpellAbility sa) {
        System.out.println("Evaluate SA: " + sa);
        if (!sa.usesTargeting()) {
            GameSimulator simulator = new GameSimulator(game);
            return simulator.simulateSpellAbility(sa);
        }
        PossibleTargetSelector selector = new PossibleTargetSelector(game, player, sa);
        int bestScore = Integer.MIN_VALUE;
        TargetChoices tgt = null;
        while (selector.selectNextTargets()) {
            System.out.println("Trying targets: " + sa.getTargets().getTargetedString());
            GameSimulator simulator = new GameSimulator(game);
            int score = simulator.simulateSpellAbility(sa);
            if (score > bestScore) {
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
