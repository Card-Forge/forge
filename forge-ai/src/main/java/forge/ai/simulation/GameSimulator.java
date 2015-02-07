package forge.ai.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;

import forge.ai.ComputerUtil;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.Ability;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetChoices;
import forge.game.zone.ZoneType;

public class GameSimulator {
    public static boolean COPY_STACK = false;
    private GameCopier copier;
    private Game simGame;
    private Player aiPlayer;
    private GameStateEvaluator eval;
    private ArrayList<String> origLines;
    private int origScore;
    
    public GameSimulator(final Game origGame, final Player origAiPlayer) {
        copier = new GameCopier(origGame);
        simGame = copier.makeCopy();

        aiPlayer = (Player) copier.find(origAiPlayer);
        eval = new GameStateEvaluator();
        
        origLines = new ArrayList<String>();
        debugLines = origLines;

        debugPrint = false;
        origScore = eval.getScoreForGameState(origGame, origAiPlayer);

        eval.setDebugging(true);
        ArrayList<String> simLines = new ArrayList<String>();
        debugLines = simLines;
        int simScore = eval.getScoreForGameState(simGame, aiPlayer);
        if (simScore != origScore) {
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

        debugPrint = true;
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
    public static ArrayList<String> debugLines;
    public static Function<String, Void> debugPrintFunction = new Function<String, Void>() {
        @Override
        public Void apply(String str) {
            debugPrint(str);
            return null;
        }
    };
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
        ZoneType zone = origHostCard.getZone().getZoneType();
        for (Card c : simGame.getCardsIn(zone)) {
            if (!c.getOwner().getController().isAI()) {
                continue;
            }
            if (c.getName().equals(origHostCard.getName())) {
                for (SpellAbility cSa : c.getSpellAbilities()) {
                    if (cSa.getDescription().equals(sa.getDescription())) {
                        return cSa;
                    }
                }
            }
        }
        return null;
    }

    public int simulateSpellAbility(SpellAbility origSa) {
        return simulateSpellAbility(origSa, this.eval);
    }
    public int simulateSpellAbility(SpellAbility origSa, GameStateEvaluator eval) {
        // TODO: optimize: prune identical SA (e.g. two of the same card in hand)
        SpellAbility sa = findSaInSimGame(origSa);
        if (sa == null) {
            System.err.println("SA not found! " + sa);
            return Integer.MIN_VALUE;
        }

        sa.setActivatingPlayer(aiPlayer);
        if (origSa.usesTargeting()) {
            for (GameObject o : origSa.getTargets().getTargets()) {
                sa.getTargets().add(copier.find(o));
            }
        }

        if (sa == Ability.PLAY_LAND_SURROGATE) {
            aiPlayer.playLand(sa.getHostCard(), false);
        } else {
            if (!sa.getAllTargetChoices().isEmpty()) {
                debugPrint("Targets: ");
                for (TargetChoices target : sa.getAllTargetChoices()) {
                    System.out.print(target.getTargetedString());
                }
                System.out.println();
            }
            ComputerUtil.handlePlayingSpellAbility(aiPlayer, sa, simGame);
        }

        if (simGame.getStack().isEmpty()) {
            System.err.println("Stack empty: " + sa);
            return Integer.MIN_VALUE;
        }
        // TODO: Support multiple opponents.
        Player opponent = null;
        for (Player p : simGame.getPlayers()) {
            if (p != aiPlayer) {
                opponent = p;
                break;
            }
        }
        resolveStack(simGame, opponent);

        // TODO: If this is during combat, before blockers are declared,
        // we should simulate how combat will resolve and evaluate that
        // state instead!
        debugPrint("SimGame:");
        ArrayList<String> simLines = new ArrayList<String>();
        debugLines = simLines;
        debugPrint = false;
        int score = eval.getScoreForGameState(simGame, aiPlayer);
        debugLines = null;
        debugPrint = true;
        printDiff(origLines, simLines);

        return score;
    }

    private static void resolveStack(final Game game, final Player opponent) {
        // TODO: This needs to set an AI controller for all opponents, in case of multiplayer.
        opponent.runWithController(new Runnable() {
            @Override
            public void run() {
                final Set<Card> allAffectedCards = new HashSet<Card>();
                game.getStack().addAllTriggeredAbilitiesToStack();
                do {
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
                } while (!game.getStack().isEmpty() && !game.isGameOver());
            }
        }, new PlayerControllerAi(game, opponent, opponent.getLobbyPlayer()));
    }
    
    public Game getSimulatedGameState() {
        return simGame;
    }

    public int getScoreForOrigGame() {
        return origScore;
    }
}
