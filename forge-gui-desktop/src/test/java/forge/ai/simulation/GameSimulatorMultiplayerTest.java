package forge.ai.simulation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.AIOption;
import forge.ai.LobbyPlayerAi;
import forge.ai.PlayerControllerAi;
import forge.ai.simulation.GameStateEvaluator.Score;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class GameSimulatorMultiplayerTest extends SimulationTest {
    @Test
    public void testSimulationResolvesAllOpponentChoicesDeterministically() {
        Scenario scenario = createSacrificeScenario();
        Integer expectedScore = null;

        for (int i = 0; i < 3; i++) {
            GameSimulator simulator = createSimulator(scenario.game, scenario.ai);
            Score result = simulator.simulateSpellAbility(scenario.spellAbility);
            Game simulatedGame = simulator.getSimulatedGameState();

            AssertJUnit.assertTrue("The complete multiplayer result should improve the AI's score",
                    result.value > simulator.getScoreForOrigGame().value);
            if (expectedScore == null) {
                expectedScore = result.value;
            } else {
                AssertJUnit.assertEquals("Repeated simulations should produce the same score",
                        expectedScore.intValue(), result.value);
            }

            assertInZone(simulatedGame.getPlayers().get(0), "Runeclaw Bear", ZoneType.Graveyard);
            assertInZone(simulatedGame.getPlayers().get(0), "Serra Angel", ZoneType.Battlefield);
            assertInZone(simulatedGame.getPlayers().get(2), "Ornithopter", ZoneType.Graveyard);
            assertInZone(simulatedGame.getPlayers().get(2), "Shivan Dragon", ZoneType.Battlefield);
            AssertJUnit.assertTrue("Simulation should fully resolve the stack",
                    simulatedGame.getStack().isEmpty());
        }
    }

    @Test
    public void testPickerChoosesBeneficialMultiplayerSpellWithoutUsingMatchControllers() {
        Scenario scenario = createSacrificeScenario();
        PlayerController firstOpponentController = scenario.firstOpponent.getController();
        PlayerController secondOpponentController = scenario.secondOpponent.getController();

        SpellAbility chosen = new SpellAbilityPicker(scenario.game, scenario.ai).chooseSpellAbilityToPlay(null);

        AssertJUnit.assertNotNull("The AI should find a beneficial multiplayer play", chosen);
        AssertJUnit.assertEquals("Innocent Blood", chosen.getHostCard().getName());
        AssertJUnit.assertSame("Simulation must not replace a controller in the real game",
                firstOpponentController, scenario.firstOpponent.getController());
        AssertJUnit.assertSame("Simulation must not replace a controller in the real game",
                secondOpponentController, scenario.secondOpponent.getController());
    }

    private Scenario createSacrificeScenario() {
        Game game = createThreePlayerGame();
        Player firstOpponent = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        Player secondOpponent = game.getPlayers().get(2);
        firstOpponent.setTeam(0);
        ai.setTeam(1);
        secondOpponent.setTeam(2);

        addCard("Swamp", ai);
        Card innocentBlood = addCardToZone("Innocent Blood", ai, ZoneType.Hand);

        addCard("Runeclaw Bear", firstOpponent);
        addCard("Serra Angel", firstOpponent);
        addCard("Ornithopter", secondOpponent);
        addCard("Shivan Dragon", secondOpponent);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility spellAbility = innocentBlood.getFirstSpellAbility();
        spellAbility.setActivatingPlayer(ai);
        return new Scenario(game, ai, firstOpponent, secondOpponent, spellAbility);
    }

    private Game createThreePlayerGame() {
        List<RegisteredPlayer> players = Lists.newArrayList();
        Deck deck = new Deck();
        players.add(new RegisteredPlayer(deck).setPlayer(new FailOnChoiceLobbyPlayer("opponent-1")));

        Set<AIOption> options = new HashSet<>();
        options.add(AIOption.USE_SIMULATION);
        players.add(new RegisteredPlayer(deck).setPlayer(new LobbyPlayerAi("ai", options)));
        players.add(new RegisteredPlayer(deck).setPlayer(new FailOnChoiceLobbyPlayer("opponent-2")));

        GameRules rules = new GameRules(GameType.Constructed);
        Match match = new Match(rules, players, "Multiplayer simulation test");
        Game game = new Game(players, rules, match);
        game.setAge(GameStage.Play);
        game.EXPERIMENTAL_RESTORE_SNAPSHOT = false;
        return game;
    }

    private static void assertInZone(Player player, String cardName, ZoneType zone) {
        for (Card card : player.getCardsIn(zone)) {
            if (cardName.equals(card.getName())) {
                return;
            }
        }
        AssertJUnit.fail(cardName + " should be in " + player.getName() + "'s " + zone);
    }

    private static final class Scenario {
        private final Game game;
        private final Player ai;
        private final Player firstOpponent;
        private final Player secondOpponent;
        private final SpellAbility spellAbility;

        private Scenario(Game game, Player ai, Player firstOpponent, Player secondOpponent,
                SpellAbility spellAbility) {
            this.game = game;
            this.ai = ai;
            this.firstOpponent = firstOpponent;
            this.secondOpponent = secondOpponent;
            this.spellAbility = spellAbility;
        }
    }

    private static final class FailOnChoiceLobbyPlayer extends LobbyPlayerAi {
        private FailOnChoiceLobbyPlayer(String name) {
            super(name, null);
        }

        @Override
        public Player createIngamePlayer(Game game, int id) {
            Player player = new Player(getName(), game, id);
            player.setFirstController(new FailOnChoiceController(game, player, this));
            return player;
        }
    }

    private static final class FailOnChoiceController extends PlayerControllerAi {
        private FailOnChoiceController(Game game, Player player, LobbyPlayerAi lobbyPlayer) {
            super(game, player, lobbyPlayer);
        }

        @Override
        public CardCollectionView choosePermanentsToSacrifice(SpellAbility sa, int min, int max,
                CardCollectionView validTargets, String message) {
            throw new AssertionError("Copied simulation requested a choice from the match controller");
        }
    }
}
