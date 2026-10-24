package forge.ai.simulation;


import forge.ai.AIOption;
import forge.ai.ComputerUtil;
import forge.ai.PlayerControllerAi;
import forge.ai.simulation.GameStateEvaluator.Score;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetChoices;
import forge.util.collect.FCollectionView;

import java.util.*;

public class GameSimulator {
    public static boolean COPY_STACK = false;
    final private SimulationController controller;
    private GameCopier copier;
    private Game simGame;
    private Player aiPlayer;
    private GameStateEvaluator eval;
    private List<String> origLines;
    private Score origScore;
    private SpellAbilityChoicesIterator interceptor;

    // Verifying that the copied game scores identically to the original costs a second full
    // evaluation (with debug string building enabled) for every simulator that gets constructed.
    // That's worth paying for in development and in the test suite, where a game copy bug should
    // fail loudly, but not in a shipped game where it only slows the AI down. Assertions are
    // enabled by Maven Surefire, so the check still runs for every test.
    private static final boolean CHECK_GAME_COPY_SCORE = areAssertionsEnabled();

    @SuppressWarnings("AssertWithSideEffects")
    private static boolean areAssertionsEnabled() {
        boolean enabled = false;
        assert enabled = true;
        return enabled;
    }

    public GameSimulator(SimulationController controller, Game origGame, Player origAiPlayer, PhaseType advanceToPhase) {
        this.controller = controller;
        copier = new GameCopier(origGame);
        simGame = copier.makeCopy(advanceToPhase, origAiPlayer);

        aiPlayer = copier.find(origAiPlayer);
        eval = new GameStateEvaluator();

        origLines = new ArrayList<>();
        debugLines = origLines;

        debugPrint = false;
        origScore = eval.getScoreForGameState(origGame, origAiPlayer);

        if (advanceToPhase == null && CHECK_GAME_COPY_SCORE) {
            ensureGameCopyScoreMatches(origGame, origAiPlayer);
        }

        // If the stack on the original game is not empty, resolve it
        // first and get the updated eval score, since this is what we'll
        // want to compare to the eval score after simulating.
        if (COPY_STACK && !origGame.getStackZone().isEmpty()) {
            origLines = new ArrayList<>();
            debugLines = origLines;
            Game copyOrigGame = copier.makeCopy();
            Player copyOrigAiPlayer = copyOrigGame.getPlayers().get(1);
            resolveStack(copyOrigGame, copyOrigGame.getPlayers().get(0));
            origScore = eval.getScoreForGameState(copyOrigGame, copyOrigAiPlayer);
        }

        debugPrint = false;
        debugLines = null;
    }

    /**
     * Lightweight constructor for Monte Carlo rollouts. It only copies the game: unlike the full constructor it
     * neither scores the original game (which can cost a second game copy to simulate the upcoming combat) nor
     * verifies that the copy scores identically. {@link #getScoreForOrigGame()} is therefore null for such a
     * simulator, and it is meant to be used with {@link #applySpellAbility(SpellAbility)} and
     * {@link #getSimulatedGameState()} only.
     */
    public GameSimulator(Game origGame, Player origAiPlayer) {
        this.controller = new SimulationController(new Score(0), 0);
        copier = new GameCopier(origGame);
        simGame = copier.makeCopy(null, origAiPlayer);

        aiPlayer = copier.find(origAiPlayer);
        eval = new GameStateEvaluator();
        origLines = new ArrayList<>();
    }

    /** As the lightweight constructor, optionally copying the spells on the stack (for decisions made in response to one). */
    public GameSimulator(Game origGame, Player origAiPlayer, boolean copyStack) {
        this.controller = new SimulationController(new Score(0), 0);
        copier = new GameCopier(origGame).withStack(copyStack);
        simGame = copier.makeCopy(null, origAiPlayer);

        aiPlayer = copier.find(origAiPlayer);
        eval = new GameStateEvaluator();
        origLines = new ArrayList<>();
    }

    private void ensureGameCopyScoreMatches(Game origGame, Player origAiPlayer) {
        eval.setDebugging(true);
        List<String> simLines = new ArrayList<>();
        debugLines = simLines;
        Score simScore = eval.getScoreForGameState(simGame, aiPlayer);
        if (!simScore.equals(origScore)) {
            // Re-eval orig with debug printing.
            origLines = new ArrayList<>();
            debugLines = origLines;
            eval.getScoreForGameState(origGame, origAiPlayer);
            // Print debug info.
            printDiff(origLines, simLines);
            // make sure it gets printed
            System.out.flush();
            throw new RuntimeException("Game copy error. See diff output above for details.");
        }
        eval.setDebugging(false);
    }

    public void setInterceptor(SpellAbilityChoicesIterator interceptor) {
        this.interceptor = interceptor;
        ((PlayerControllerAi) aiPlayer.getController()).getAi().getSimulationPicker().setInterceptor(interceptor);
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
        if (debugLines != null) {
            debugLines.add(str);
        }
    }

    private SpellAbility findSaInSimGame(final SpellAbility sa) {
        // is already an ability from sim game
        if (sa.getHostCard().getGame().equals(this.simGame)) {
            return sa;
        }
        Card origHostCard = sa.getHostCard();
        Card hostCard = copier.find(origHostCard);
        String desc = sa.getDescription();
        // TODO tests fail if this isn't checked first
        FCollectionView<SpellAbility> candidates = hostCard.getSpellAbilities();

        SpellAbility result = saMatcher(candidates, desc);
        if (result == null) {
            // could try and reimplement this so a quick match doesn't require building the rest first
            result = saMatcher(hostCard.getAllPossibleAbilities(aiPlayer, true), desc);
        }

        if (result == null || !desc.equals(result.getDescription())) {
            // Only the printed ability was found (or nothing): a play paid with an alternative cost (Force of Will,
            // Daze, Fireblast, evoke...) is built on demand from the printed one, so build it in the simulated game too.
            final SpellAbility alt = findAlternativeCostSa(hostCard, desc);
            if (alt != null) {
                result = alt;
            }
        }
        if (result == null && Boolean.getBoolean("flatmc.trace")) {
            System.err.println("[sim] no match for: " + desc + " in " + hostCard.getAllPossibleAbilities(aiPlayer, true) + " / unfiltered "
                    + hostCard.getAllPossibleAbilities(aiPlayer, false));
        }
        if (result != null) {
            result = SpellAbilityChoiceCopier.copyCastChoices(sa, result, aiPlayer);
        }

        return result;
    }

    private SpellAbility findAlternativeCostSa(final Card hostCard, final String desc) {
        // The same expansion the AI uses to pick its plays: additional-cost choices (Bitter Triumph: discard or pay
        // life), optional costs (kicker) and alternative costs (Force of Will, Daze, evoke) are separate plays, and
        // the description tells which one was meant.
        try {
            for (final SpellAbility printed : hostCard.getSpellAbilities()) {
                if (!printed.isSpell()) {
                    continue;
                }
                printed.setActivatingPlayer(aiPlayer);
                for (final SpellAbility variant : forge.ai.ComputerUtilAbility.getOriginalAndAltCostAbilities(
                        Collections.singletonList(printed), aiPlayer)) {
                    if (desc.equals(variant.getDescription())) {
                        return variant;
                    }
                }
            }
        } catch (final RuntimeException e) {
            // not available in the copy: the caller treats it as not found
        }
        return null;
    }

    private SpellAbility saMatcher(Iterable<SpellAbility> candidates, String desc) {
        // first pass for accuracy (spells with alternative costs)
        for (SpellAbility cSa : candidates) {
            if (desc.equals(cSa.getDescription())) {
                return cSa;
            }
        }
        // fall back for safety
        for (SpellAbility cSa : candidates) {
            if (desc.startsWith(cSa.getDescription())) {
                return cSa;
            }
        }
        return null;
    }

    public Score simulateSpellAbility(SpellAbility origSa) {
        return simulateSpellAbility(origSa, this.eval, true);
    }
    public Score simulateSpellAbility(SpellAbility origSa, boolean resolve) {
        return simulateSpellAbility(origSa, this.eval, resolve);
    }
    public Score simulateSpellAbility(SpellAbility origSa, GameStateEvaluator eval, boolean resolve) {
        SpellAbility sa;
        if (origSa.isLandAbility()) {
            Card hostCard = (Card) copier.find(origSa.getHostCard());
            if (origSa.canPlay()) {
                aiPlayer.playLand(hostCard, origSa);
            } else {
                System.err.println("Simulation: Couldn't play land! " + origSa);
            }
            sa = origSa;
        } else {
            // TODO: optimize: prune identical SA (e.g. two of the same card in hand)
            sa = findSaInSimGame(origSa);
            if (sa == null) {
                System.err.println("Simulation: SA not found! " + origSa + " / " + origSa.getClass());
                return new Score(Integer.MIN_VALUE);
            }

            debugPrint("Found SA " + sa + " on host card " + sa.getHostCard() + " with owner:"+ sa.getHostCard().getOwner());
            sa.setActivatingPlayer(aiPlayer);

            simGame.copyLastState();
            boolean success = ComputerUtil.handlePlayingSpellAbility(aiPlayer, sa, playingSa -> {
                if (interceptor != null) {
                    interceptor.announceX(playingSa);
                    interceptor.chooseTargets(playingSa, GameSimulator.this);
                } else {
                    SpellAbilityChoiceCopier.copyTargets(origSa, playingSa, copier::find);
                }
                if (debugPrint && !playingSa.getAllTargetChoices().isEmpty()) {
                    debugPrint("Targets: ");
                    for (TargetChoices target : playingSa.getAllTargetChoices()) {
                        System.out.print(target);
                    }
                    System.out.println();
                }
            });
            if (!success) {
                return new Score(Integer.MIN_VALUE);
            }
        }

        if (resolve) {
            // TODO: Support multiple opponents.
            Player opponent = aiPlayer.getWeakestOpponent();
            resolveStack(simGame, opponent);
        }

        // TODO: If this is during combat, before blockers are declared,
        // we should simulate how combat will resolve and evaluate that
        // state instead!
        List<String> simLines = null;
        if (debugPrint) {
            debugPrint("SimGame:");
            simLines = new ArrayList<>();
            debugLines = simLines;
            debugPrint = false;
        }
        Score score = eval.getScoreForGameState(simGame, aiPlayer);
        if (simLines != null) {
            debugLines = null;
            debugPrint = true;
            printDiff(origLines, simLines);
        }
        controller.possiblyCacheResult(score, origSa);
        if (controller.shouldRecurse() && !simGame.isGameOver()) {
            controller.push(sa, score, this);
            SpellAbilityPicker sim = new SpellAbilityPicker(aiPlayer);
            SpellAbility nextSa = sim.chooseSpellAbilityToPlay(controller);
            if (nextSa != null) {
                score = sim.getScoreForChosenAbility();
            }
            controller.pop(score, nextSa);
        }

        return score;
    }

    /**
     * Plays the given ability (an ability of the original game) in the simulated game and resolves the stack,
     * like {@link #simulateSpellAbility(SpellAbility)} does, but without scoring or recursing. The caller scores
     * the resulting {@link #getSimulatedGameState()} itself, possibly after more plays.
     *
     * @return false if the ability could not be found or played in the simulated game
     */
    public boolean applySpellAbility(SpellAbility origSa) {
        return applySpellAbility(origSa, true);
    }

    /**
     * @param resolveStack false to leave the play on the stack: used when responding to a spell, where only what the AI
     *                     put there should resolve before it gets priority again (see {@link #resolveOwnTop})
     */
    public boolean applySpellAbility(SpellAbility origSa, boolean resolveStack) {
        if (origSa.isLandAbility()) {
            if (!origSa.canPlay()) {
                return false;
            }
            Card hostCard = copier.find(origSa.getHostCard());
            aiPlayer.playLand(hostCard, origSa);
        } else {
            SpellAbility sa = findSaInSimGame(origSa);
            if (sa == null) {
                if (Boolean.getBoolean("flatmc.trace")) {
                    System.err.println("[sim] applySpellAbility: not found " + origSa);
                }
                return false;
            }
            sa.setActivatingPlayer(aiPlayer);

            simGame.copyLastState();
            boolean success = ComputerUtil.handlePlayingSpellAbility(aiPlayer, sa,
                    playingSa -> SpellAbilityChoiceCopier.copyTargets(origSa, playingSa, copier::find));
            if (!success) {
                if (Boolean.getBoolean("flatmc.trace")) {
                    System.err.println("[sim] applySpellAbility: could not pay/play " + sa + " cost=" + sa.getPayCosts());
                }
                return false;
            }
        }
        if (resolveStack) {
            resolveStack(simGame, aiPlayer.getWeakestOpponent());
        }
        return true;
    }

    /**
     * Resolves the top of the stack for as long as it belongs to {@code owner}, then stops: the rest (what the
     * opponent put there earlier) stays for the owner to respond to or let resolve.
     */
    public static void resolveOwnTop(final Game game, final Player owner, final Player opponent) {
        PlayerControllerAi sim = new PlayerControllerAi(game, opponent, opponent.getLobbyPlayer());
        sim.getAi().setUseSimulation(AIOption.USE_FULL_SIMULATION);
        opponent.runWithController(() -> {
            final Set<Card> allAffectedCards = new HashSet<>();
            game.getAction().checkStateEffects(false, allAffectedCards);
            game.getStack().addAllTriggeredAbilitiesToStack();
            while (!game.getStack().isEmpty() && !game.isGameOver()
                    && owner.equals(game.getStack().peekAbility().getActivatingPlayer())) {
                game.getStack().resolveStack();
                game.getAction().checkStateEffects(false, allAffectedCards);
                game.getStack().addAllTriggeredAbilitiesToStack();
            }
        }, sim);
    }

    /** The copy of the AI player (given to the constructor) living in the simulated game. */
    public Player getSimulatedAiPlayer() {
        return aiPlayer;
    }

    public static void resolveStack(final Game game, final Player opponent) {
        // TODO: This needs to set an AI controller for all opponents, in case of multiplayer.
        PlayerControllerAi sim = new PlayerControllerAi(game, opponent, opponent.getLobbyPlayer());
        sim.getAi().setUseSimulation(AIOption.USE_FULL_SIMULATION);
        opponent.runWithController(() -> {
            final Set<Card> allAffectedCards = new HashSet<>();
            game.getAction().checkStateEffects(false, allAffectedCards);
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

                // Add any triggers additional triggers as a result of the above.
                // Must be below state effects, since legendary rule is evaluated
                // as part of state effects and trigger come afterward. (e.g. to
                // correctly handle two Dark Depths - one having no counters).
                game.getStack().addAllTriggeredAbilitiesToStack();

                // Continue until stack is empty.
            }
        }, sim);
    }

    public Game getSimulatedGameState() {
        return simGame;
    }

    public Score getScoreForOrigGame() {
        return origScore;
    }

    public GameCopier getGameCopier() {
        return copier;
    }
}
