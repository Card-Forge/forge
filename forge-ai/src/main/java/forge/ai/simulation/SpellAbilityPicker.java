package forge.ai.simulation;

import java.util.ArrayList;
import java.util.List;

import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCost;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityCondition;
import forge.game.spellability.TargetChoices;

public class SpellAbilityPicker {
    private Game game;
    private Player player;

    public SpellAbilityPicker(Game game, Player player) {
        this.game = game;
        this.player = player;
    }

    public SpellAbility chooseSpellAbilityToPlay(final ArrayList<SpellAbility> originalAndAltCostAbilities, boolean skipCounter) {
        System.out.println("---- choose ability  (phase = " +  game.getPhaseHandler().getPhase() + ")");

        ArrayList<SpellAbility> candidateSAs = new ArrayList<>();
        for (final SpellAbility sa : originalAndAltCostAbilities) {
            // Don't add Counterspells to the "normal" playcard lookups
            if (skipCounter && sa.getApi() == ApiType.Counter) {
                continue;
            }
            if (sa.isManaAbility()) {
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
        GameSimulator simulator = new GameSimulator(game, player);
        // FIXME: This is wasteful, we should re-use the same simulator...
        int origGameScore = simulator.getScoreForOrigGame();
        int bestSaValue = origGameScore;
        for (final SpellAbility sa : candidateSAs) {
            int value = evaluateSa(sa);
            if (value > bestSaValue) {
                bestSaValue = value;
                bestSa = sa;
            }
        }
        
        if (bestSa != null && game.getPhaseHandler().getPhase() == PhaseType.MAIN1) {
            // To make the AI hold-off on playing creatures in MAIN1 if they give no other benefits,
            // check the score for the bestSA while counting summon sick creatures for 0.
            // Do it here on the best SA, rather than for all evaluations, so that if the best SA
            // is indeed a creature spell, we don't pick something else to play now and then have
            // no mana to play the truly best SA post-combat.
            // TODO: This can be done more efficiently by keeping track of both scores during original
            // evaluation.
            GameStateEvaluator summonSickEval = new GameStateEvaluator() {
                @Override
                protected int evalCard(Game game, Player aiPlayer, Card c) {
                    if (c.isSick() && c.getController() == aiPlayer) {
                        return 0;
                    }
                    return super.evalCard(game, aiPlayer, c);
                }
            };
            if (simulator.simulateSpellAbility(bestSa,summonSickEval) <= origGameScore) {
                bestSa = null;
            }
        }
        
        System.out.println("BEST: " + abilityToString(bestSa) + " SCORE: " + bestSaValue);
        return bestSa;
    }

    private String abilityToString(SpellAbility sa) {
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

    private int evaluateSa(SpellAbility sa) {
        System.out.println("Evaluate SA: " + sa);
        if (!sa.usesTargeting()) {
            GameSimulator simulator = new GameSimulator(game, player);
            return simulator.simulateSpellAbility(sa);
        }
        PossibleTargetSelector selector = new PossibleTargetSelector(game, player, sa);
        int bestScore = Integer.MIN_VALUE;
        TargetChoices tgt = null;
        while (selector.selectNextTargets()) {
            System.out.println("Trying targets: " + sa.getTargets().getTargetedString());
            GameSimulator simulator = new GameSimulator(game, player);
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
