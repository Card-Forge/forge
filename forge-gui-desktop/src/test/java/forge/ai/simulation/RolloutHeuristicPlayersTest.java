package forge.ai.simulation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import forge.LobbyPlayer;
import forge.ai.AIOption;
import forge.ai.LobbyPlayerAi;
import forge.ai.AiController;
import forge.ai.PlayerControllerAi;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.Match;
import forge.game.phase.PhaseType;
import forge.game.player.IGameEntitiesFactory;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.AITimeoutTracker;

/**
 * A rollout plays every side by the heuristics. The copy of a game gives the stand-in of a human player the full
 * simulation AI (GameCopier.clonePlayer), which searches in copies of its own outside of the deadline of the decision
 * being made; these tests show that this is what a plain copy does, and that a rollout's simulator removes it.
 */
public class RolloutHeuristicPlayersTest extends SimulationTest {

    /** A player that is not a LobbyPlayerAi, as a human is; it is driven by a plain AI controller so the test needs no GUI. */
    private static final class HumanStandIn extends LobbyPlayer implements IGameEntitiesFactory {
        HumanStandIn(final String name) {
            super(name);
        }

        @Override
        public PlayerController createMindSlaveController(final Player master, final Player slave) {
            return new PlayerControllerAi(slave.getGame(), slave, this);
        }

        @Override
        public Player createIngamePlayer(final Game game, final int id) {
            final Player p = new Player(getName(), game, id);
            p.setFirstController(new PlayerControllerAi(game, p, this));
            return p;
        }

        @Override
        public void hear(final LobbyPlayer player, final String message) {
        }
    }

    private Game newGame(final LobbyPlayer aiSide, final LobbyPlayer otherSide) {
        final Deck deck = new Deck();
        final List<RegisteredPlayer> players = Lists.newArrayList();
        players.add(new RegisteredPlayer(deck).setPlayer(otherSide));
        players.add(new RegisteredPlayer(deck).setPlayer(aiSide));
        final GameRules rules = new GameRules(GameType.Constructed);
        final Game g = new Game(players, rules, new Match(rules, players, "Test"));
        g.setAge(GameStage.Play);
        g.EXPERIMENTAL_RESTORE_SNAPSHOT = false;
        g.AI_TIMEOUT = FModel.getPreferences().getPrefInt(FPref.MATCH_AI_TIMEOUT);
        return g;
    }

    private static int searching(final Game g) {
        int n = 0;
        for (final Player p : g.getPlayers()) {
            if (p.getController() instanceof PlayerControllerAi pc && pc.getAi().usesSimulation()) {
                n++;
            }
        }
        return n;
    }

    @Test
    public void aPlainCopyGivesTheHumansStandInTheFullSimulation() {
        final Game game = newGame(new LobbyPlayerAi("ai", null), new HumanStandIn("human"));
        final Player ai = game.getPlayers().get(1);
        final Game copy = new GameSimulator(game, ai, false).getSimulatedGameState();
        // this is the cause of the 50 s decision: the claim the fix rests on
        AssertJUnit.assertEquals(1, searching(copy));
        AssertJUnit.assertTrue(((PlayerControllerAi) copy.getPlayers().get(0).getController()).getAi().usesFullSimulation());
    }

    @Test
    public void aRolloutSimulatorPlaysEverySideByTheHeuristics() {
        final Game game = newGame(new LobbyPlayerAi("ai", null), new HumanStandIn("human"));
        final Player ai = game.getPlayers().get(1);
        for (final boolean stack : new boolean[] { false, true }) {
            final Game copy = GameSimulator.forRollout(game, ai, stack).getSimulatedGameState();
            AssertJUnit.assertEquals(0, searching(copy));
        }
    }

    @Test
    public void anAiOpponentWithSimulationOnIsAlsoPlayedByTheHeuristics() {
        final Set<AIOption> options = new HashSet<>();
        options.add(AIOption.USE_FULL_SIMULATION);
        final Game game = newGame(new LobbyPlayerAi("ai", null), new LobbyPlayerAi("other", options));
        final Player ai = game.getPlayers().get(1);
        AssertJUnit.assertEquals(1, searching(game));
        AssertJUnit.assertEquals(0, searching(GameSimulator.forRollout(game, ai, false).getSimulatedGameState()));
    }

    @Test(expectedExceptions = AITimeoutTracker.Expired.class)
    public void aRolloutsStackResolutionStopsAtTheDecisionsDeadline() {
        final Game game = newGame(new LobbyPlayerAi("ai", null), new HumanStandIn("human"));
        final Game copy = GameSimulator.forRollout(game, game.getPlayers().get(1), false).getSimulatedGameState();
        final Object limit = AITimeoutTracker.arm(System.nanoTime() - 1, false); // the deadline has passed
        try {
            GameSimulator.resolveStack(copy, copy.getPlayers().get(0));
        } finally {
            AITimeoutTracker.disarm(limit);
        }
    }

    @Test
    public void stackResolutionIsNotCutShortWithoutADeadline() {
        final Game game = newGame(new LobbyPlayerAi("ai", null), new HumanStandIn("human"));
        final Game copy = GameSimulator.forRollout(game, game.getPlayers().get(1), false).getSimulatedGameState();
        GameSimulator.resolveStack(copy, copy.getPlayers().get(0)); // nothing armed: no exception
    }

    /**
     * The scene of the bug report in miniature: the opponent is a human's stand-in with a board, the AI has mana and
     * spells. The decision has to end inside twice its AI timeout and be made by the rollouts, not given up
     * ("deadline") or failed ("error"). This is the check to run on a slow machine or after touching the rollouts.
     */
    @Test
    public void aDecisionAgainstAHumanEndsInTimeAndIsNotGivenUp() {
        final Game game = newGame(new LobbyPlayerAi("ai", null), new HumanStandIn("human"));
        game.AI_FLAT_MONTE_CARLO = true;
        game.AI_TIMEOUT = 5;
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 12; i++) {
            addCardToZone("Grizzly Bears", ai, ZoneType.Library);
            addCardToZone("Grizzly Bears", human, ZoneType.Library);
        }
        for (int i = 0; i < 5; i++) {
            addCardToZone("Forest", ai, ZoneType.Battlefield);
        }
        for (int i = 0; i < 4; i++) {
            addCardToZone("Mountain", human, ZoneType.Battlefield);
        }
        for (int i = 0; i < 3; i++) {
            addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
            addCardToZone("Runeclaw Bear", ai, ZoneType.Hand);
            addCardToZone("Grizzly Bears", human, ZoneType.Battlefield);
        }
        addCardToZone("Giant Growth", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        final AiController brain = ((PlayerControllerAi) ai.getController()).getAi();
        final List<SpellAbility> plays = new SpellAbilityPicker(ai).getCandidateSpellsAndAbilities();
        AssertJUnit.assertFalse("there must be something to play", plays.isEmpty());
        final long start = System.nanoTime();
        final FlatMonteCarloPicker.Result r = FlatMonteCarloPicker.choose(ai, plays,
                FlatMonteCarloPicker.Config.fromProfile(brain, ai).maximized(), null);
        final long seconds = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start);
        AssertJUnit.assertTrue("took " + seconds + " s: " + r, seconds <= 2L * game.AI_TIMEOUT);
        AssertJUnit.assertFalse("gave up: " + r, "deadline".equals(r.reason) || "error".equals(r.reason));
        AssertJUnit.assertTrue("the rollouts must have run: " + r, r.rollouts > 0);
    }

    @Test
    public void theRealGameIsLeftAlone() {
        final Set<AIOption> options = new HashSet<>();
        options.add(AIOption.USE_FULL_SIMULATION);
        final Game game = newGame(new LobbyPlayerAi("ai", null), new LobbyPlayerAi("other", options));
        GameSimulator.forRollout(game, game.getPlayers().get(1), false);
        AssertJUnit.assertEquals(1, searching(game));
    }
}
