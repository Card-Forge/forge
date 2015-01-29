package simulation;

import java.util.HashSet;
import java.util.Set;

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
    private Game origGame;
    private GameCopier copier;
    private Game simGame;
    private Player aiPlayer;
    private Player opponent;
    private GameStateEvaluator eval;

    public GameSimulator(final Game origGame) {
        this.origGame = origGame;
        copier = new GameCopier(origGame);
        simGame = copier.makeCopy();
        // TODO:
        aiPlayer = simGame.getPlayers().get(1);
        opponent = simGame.getPlayers().get(0);
        eval = new GameStateEvaluator();
        int simScore = eval.getScoreForGameState(simGame, aiPlayer, opponent);
        int origScore = getScoreForOrigGame();
        debugPrint = true;
        if (simScore != origScore) {
            // Print debug info.
            eval.getScoreForGameState(simGame, aiPlayer, opponent);
            getScoreForOrigGame();
            throw new RuntimeException("Game copy error");
        }
    }

    public static boolean debugPrint;
    public static void debugPrint(String str) {
        if (debugPrint) {
            System.out.println(str);
        }
    }
    
    private SpellAbility findSaInSimGame(SpellAbility sa) {
        Card origHostCard = sa.getHostCard();
        ZoneType zone = origHostCard.getZone().getZoneType();
        for (Card c : simGame.getCardsIn(zone)) {
            if (!c.getOwner().getController().isAI()) {
                continue;
            }
            debugPrint(c.getName()+"->");
            if (c.getName().equals(origHostCard.getName())) {
                for (SpellAbility cSa : c.getSpellAbilities()) {
                    debugPrint("    "+cSa);
                    if (cSa.getDescription().equals(sa.getDescription())) {
                        return cSa;
                    }
                }
            }
        }
        return null;
    }
    
    
    public int simulateSpellAbility(SpellAbility origSa) {
        // TODO: optimize: prune identical SA (e.g. two of the same card in hand)

        boolean found = false;
        SpellAbility sa = findSaInSimGame(origSa);
        if (sa != null) {
            found = true;
        } else {
            System.err.println("SA not found! " + sa);
            return Integer.MIN_VALUE;
        }

        Player origActivatingPlayer = sa.getActivatingPlayer();
        sa.setActivatingPlayer(aiPlayer);
        if (origSa.usesTargeting()) {
            for (GameObject o : origSa.getTargets().getTargets()) {
                debugPrint("Copying over target " +o);
                debugPrint("  found: "+copier.find(o));
                sa.getTargets().add(copier.find(o));
            }
        }

        debugPrint("Simulating playing sa: " + sa + " found="+found);
        if (sa == Ability.PLAY_LAND_SURROGATE) {
            aiPlayer.playLand(sa.getHostCard(), false);
        }
        else {
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
        opponent.runWithController(new Runnable() {
            @Override
            public void run() {
                final Set<Card> allAffectedCards = new HashSet<Card>();
                do {
                    // Resolve the top effect on the stack.
                    simGame.getStack().resolveStack();
                    // Evaluate state based effects as a result of resolving stack.
                    // Note: Needs to happen after resolve stack rather than at the
                    // top of the loop to ensure state effects are evaluated after the
                    // last resolved effect
                    simGame.getAction().checkStateEffects(false, allAffectedCards);
                    // Add any triggers as a result of resolving the effect.
                    simGame.getStack().addAllTriggeredAbilitiesToStack();
                    // Continue until stack is empty.
                } while (!simGame.getStack().isEmpty() && !simGame.isGameOver());
            }
        }, new PlayerControllerAi(simGame, opponent, opponent.getLobbyPlayer()));

        // TODO: If this is during combat, before blockers are declared,
        // we should simulate how combat will resolve and evaluate that
        // state instead!
        debugPrint("SimGame:");
        int score = eval.getScoreForGameState(simGame, aiPlayer, opponent);

        sa.setActivatingPlayer(origActivatingPlayer);

        return score;
    }

    public int getScoreForOrigGame() {
        // TODO: Make this logic more bulletproof.        
        Player origAiPlayer = origGame.getPlayers().get(1);
        Player origOpponent = origGame.getPlayers().get(0);
        return eval.getScoreForGameState(origGame, origAiPlayer, origOpponent);
    }
    
}
