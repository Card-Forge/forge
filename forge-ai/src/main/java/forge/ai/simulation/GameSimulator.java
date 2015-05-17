package forge.ai.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilAbility;
import forge.ai.PlayerControllerAi;
import forge.ai.simulation.GameStateEvaluator.Score;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetChoices;
import forge.game.spellability.TargetRestrictions;

public class GameSimulator {
    public static boolean COPY_STACK = false;
    final private SimulationController controller;
    private GameCopier copier;
    private Game simGame;
    private Player aiPlayer;
    private GameStateEvaluator eval;
    private List<String> origLines;
    private Score origScore;
    
    public GameSimulator(final SimulationController controller, final Game origGame, final Player origAiPlayer) {
        this.controller = controller;
        copier = new GameCopier(origGame);
        simGame = copier.makeCopy();

        aiPlayer = (Player) copier.find(origAiPlayer);
        eval = new GameStateEvaluator();
        
        origLines = new ArrayList<String>();
        debugLines = origLines;

        debugPrint = false;
        origScore = eval.getScoreForGameState(origGame, origAiPlayer);

        eval.setDebugging(true);
        List<String> simLines = new ArrayList<String>();
        debugLines = simLines;
        Score simScore = eval.getScoreForGameState(simGame, aiPlayer);
        if (!simScore.equals(origScore)) {
            // Re-eval orig with debug printing.
            origLines = new ArrayList<String>();
            debugLines = origLines;
            eval.getScoreForGameState(origGame, origAiPlayer);
            // Print debug info.
            printDiff(origLines, simLines);
            throw new RuntimeException("Game copy error");
        }
        eval.setDebugging(false);

        // If the stack on the original game is not empty, resolve it
        // first and get the updated eval score, since this is what we'll
        // want to compare to the eval score after simulating.
        if (COPY_STACK && !origGame.getStackZone().isEmpty()) {
            origLines = new ArrayList<String>();
            debugLines = origLines;
            Game copyOrigGame = copier.makeCopy();
            Player copyOrigAiPlayer = copyOrigGame.getPlayers().get(1);
            resolveStack(copyOrigGame, copyOrigGame.getPlayers().get(0));
            origScore = eval.getScoreForGameState(copyOrigGame, copyOrigAiPlayer);
        }

        debugPrint = false;
        debugLines = null;
    }

    private void printDiff(List<String> lines1, List<String> lines2) {
        int i = 0;
        int j = 0;
        Collections.sort(lines1);
        Collections.sort(lines2);
        while (i < lines1.size() && j < lines2.size()) {
            String left = lines1.get(i);
            String right = lines2.get(j);
            int cmp = left.compareTo(right);
            if (cmp == 0) {
                i++; j++;
            } else if (cmp < 0) {
                System.out.println("-" + left);
                i++;
            } else { 
                System.out.println("+"  + right);
                j++;
            }
        }
        while (i < lines1.size()) {
            System.out.println("-" + lines1.get(i++));
        }
        while (j < lines2.size()) {
            System.out.println("+" + lines2.get(j++));
        }
    }

    public static boolean debugPrint;
    public static List<String> debugLines;
    public static void debugPrint(String str) {
        if (debugPrint) {
            System.out.println(str);
        }
        if (debugLines!=null) {
            debugLines.add(str);
        }
    }
    
    private SpellAbility findSaInSimGame(SpellAbility sa) {
        Card origHostCard = sa.getHostCard();
        Card hostCard = (Card) copier.find(origHostCard);
        // FIXME: This is a hack that makes testManifest pass - figure out why it's needed.
        String desc = sa.getDescription().replace("Unmanifest {0}", "Unmanifest no cost");
        for (SpellAbility cSa : hostCard.getSpellAbilities()) {
            if (desc.equals(cSa.getDescription())) {
                return cSa;
            }
        }
        return null;
    }

    public Score simulateSpellAbility(SpellAbility origSa) {
        return simulateSpellAbility(origSa, this.eval);
    }
    public Score simulateSpellAbility(SpellAbility origSa, GameStateEvaluator eval) {
        // TODO: optimize: prune identical SA (e.g. two of the same card in hand)
        SpellAbility sa = findSaInSimGame(origSa);
        if (sa == null) {
            System.err.println("SA not found! " + sa);
            return new Score(Integer.MIN_VALUE, Integer.MIN_VALUE);
        }

        debugPrint("Found SA " + sa + " on host card " + sa.getHostCard() + " with owner:"+ sa.getHostCard().getOwner());
        sa.setActivatingPlayer(aiPlayer);
        if (origSa.usesTargeting()) {
            final boolean divided = sa.hasParam("DividedAsYouChoose");
            final TargetRestrictions origTgtRes = origSa.getTargetRestrictions();
            final TargetRestrictions tgtRes = sa.getTargetRestrictions();
            for (final GameObject o : origSa.getTargets().getTargets()) {
                final GameObject target = copier.find(o);
                sa.getTargets().add(target);
                if (divided) {
                    tgtRes.addDividedAllocation(target, origTgtRes.getDividedValue(o));
                }
            }
        }

        if (sa == origSa.getHostCard().getGame().PLAY_LAND_SURROGATE) {
            aiPlayer.playLand(sa.getHostCard(), false);
        } else {
            if (debugPrint && !sa.getAllTargetChoices().isEmpty()) {
                debugPrint("Targets: ");
                for (TargetChoices target : sa.getAllTargetChoices()) {
                    System.out.print(target.getTargetedString());
                }
                System.out.println();
            }
            ComputerUtil.handlePlayingSpellAbility(aiPlayer, sa, simGame);
        }

        // TODO: Support multiple opponents.
        Player opponent = aiPlayer.getOpponent();

        resolveStack(simGame, opponent);

        // TODO: If this is during combat, before blockers are declared,
        // we should simulate how combat will resolve and evaluate that
        // state instead!
        List<String> simLines = null;
        if (debugPrint) {
            debugPrint("SimGame:");
            simLines = new ArrayList<String>();
            debugLines = simLines;
            debugPrint = false;
        }
        Score score = eval.getScoreForGameState(simGame, aiPlayer);
        if (simLines != null) {
            debugLines = null;
            debugPrint = true;
            printDiff(origLines, simLines);
        }
        controller.printState(score, origSa);
        if (controller.shouldRecurse() && !simGame.isGameOver()) {
            controller.push(sa);
            SpellAbilityPicker sim = new SpellAbilityPicker(simGame, aiPlayer);
            CardCollection cards = ComputerUtilAbility.getAvailableCards(simGame, aiPlayer);
            List<SpellAbility> all = ComputerUtilAbility.getSpellAbilities(cards, aiPlayer);
            SpellAbility nextSa = sim.chooseSpellAbilityToPlay(controller, all, true);
            if (nextSa != null) {
                score = sim.getScoreForChosenAbility();
            }
            controller.pop(score, nextSa);
        }

        return score;
    }

    private static void resolveStack(final Game game, final Player opponent) {
        // TODO: This needs to set an AI controller for all opponents, in case of multiplayer.
        opponent.runWithController(new Runnable() {
            @Override
            public void run() {
                final Set<Card> allAffectedCards = new HashSet<Card>();
                game.getStack().addAllTriggeredAbilitiesToStack();
                while (!game.getStack().isEmpty() && !game.isGameOver()) {
                    debugPrint("Resolving:" + game.getStack().peekAbility());
                    // Resolve the top effect on the stack.
                    game.getStack().resolveStack();
                    // Evaluate state based effects as a result of resolving stack.
                    // Note: Needs to happen after resolve stack rather than at the
                    // top of the loop to ensure state effects are evaluated after the
                    // last resolved effect
                    game.getAction().checkStateEffects(false, allAffectedCards);
                    // Add any triggers as a result of resolving the effect.
                    game.getStack().addAllTriggeredAbilitiesToStack();
                    // Continue until stack is empty.
                }
            }
        }, new PlayerControllerAi(game, opponent, opponent.getLobbyPlayer()));
    }
    
    public Game getSimulatedGameState() {
        return simGame;
    }

    public Score getScoreForOrigGame() {
        return origScore;
    }
}
