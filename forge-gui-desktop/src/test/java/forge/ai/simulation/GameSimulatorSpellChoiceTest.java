package forge.ai.simulation;

import java.util.ArrayList;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class GameSimulatorSpellChoiceTest extends SimulationTest {

    @Test
    public void testPreservesKickerChoice() {
        Game game = createGame();
        Player ai = game.getPlayers().get(1);

        addCards("Forest", 6, ai);
        Card woodreaders = addCardToZone("Citanul Woodreaders", ai, ZoneType.Hand);
        fillLibrary(ai, 2);

        SpellAbility baseAbility = ability(woodreaders, ai);
        SpellAbility kickedAbility = GameActionUtil.addOptionalCosts(
                baseAbility, GameActionUtil.getOptionalCostValues(baseAbility));
        kickedAbility.setActivatingPlayer(ai);

        Player simulatedAi = simulatedPlayer(game, ai, kickedAbility);
        AssertJUnit.assertEquals("The kicked ETB should draw two cards",
                2, simulatedAi.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void testPreservesMultikickerCount() {
        Game game = createGame();
        Player ai = game.getPlayers().get(1);

        addCards("Forest", 6, ai);
        Card elemental = addCardToZone("Wolfbriar Elemental", ai, ZoneType.Hand);

        SpellAbility twiceKicked = GameActionUtil.addExtraKeywordCost(ability(elemental, ai));
        AssertJUnit.assertEquals(2,
                twiceKicked.getOptionalKeywordAmount(Keyword.MULTIKICKER));
        addCard("Forest", ai);

        Player simulatedAi = simulatedPlayer(game, ai, twiceKicked);
        AssertJUnit.assertEquals("The simulation must not reselect a larger multikicker count",
                3, simulatedAi.getCreaturesInPlay().size());
    }

    @Test
    public void testPreservesTargetsAfterAddingKeywordCosts() {
        Game game = createGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCards("Mountain", 6, ai);
        Card pyromatics = addCardToZone("Pyromatics", ai, ZoneType.Hand);

        SpellAbility twiceReplicated = GameActionUtil.addExtraKeywordCost(ability(pyromatics, ai));
        AssertJUnit.assertEquals(2,
                twiceReplicated.getOptionalKeywordAmount(Keyword.REPLICATE));
        twiceReplicated.getTargets().add(opponent);
        addCards("Mountain", 2, ai);

        GameSimulator simulator = simulate(game, ai, twiceReplicated);
        Player simulatedOpponent = (Player) simulator.getGameCopier().find(opponent);
        AssertJUnit.assertEquals("The original spell and both copies should keep a legal target",
                17, simulatedOpponent.getLife());
    }

    @Test
    public void testPreservesAnnouncedX() {
        Game game = createGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCards("Forest", 5, ai);
        Card hurricane = addCardToZone("Hurricane", ai, ZoneType.Hand);

        SpellAbility ability = ability(hurricane, ai);
        ability.setXManaCostPaid(4);
        GameSimulator simulator = simulate(game, ai, ability);
        Player simulatedAi = (Player) simulator.getGameCopier().find(ai);
        Player simulatedOpponent = (Player) simulator.getGameCopier().find(opponent);

        AssertJUnit.assertEquals("The copied spell must retain the announced value of X",
                16, simulatedAi.getLife());
        AssertJUnit.assertEquals(16, simulatedOpponent.getLife());
    }

    @Test
    public void testPreservesChosenMode() {
        Game game = createGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCard("Plains", ai);
        addCard("Swamp", ai);
        addCard("Forest", ai);
        Card charm = addCardToZone("Abzan Charm", ai, ZoneType.Hand);
        addCard("Shivan Dragon", opponent);
        fillLibrary(ai, 2);

        SpellAbility ability = ability(charm, ai);
        chooseModes(ability, "You draw");

        GameSimulator simulator = simulate(game, ai, ability);
        Player simulatedAi = (Player) simulator.getGameCopier().find(ai);
        Player simulatedOpponent = (Player) simulator.getGameCopier().find(opponent);

        AssertJUnit.assertEquals("The copied Charm must use the selected draw mode",
                2, simulatedAi.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertNotNull("The simulation must not substitute the exile mode",
                findCardWithName(simulatedOpponent.getGame(), "Shivan Dragon"));
    }

    @Test
    public void testPreservesMultipleChosenModes() {
        Game game = createGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCards("Island", 4, ai);
        Card command = addCardToZone("Cryptic Command", ai, ZoneType.Hand);
        addCards("Runeclaw Bear", 3, opponent);
        fillLibrary(ai, 1);

        SpellAbility ability = ability(command, ai);
        chooseModes(ability, "Tap all", "Draw a card");

        GameSimulator simulator = simulate(game, ai, ability);
        Player simulatedAi = (Player) simulator.getGameCopier().find(ai);
        Player simulatedOpponent = (Player) simulator.getGameCopier().find(opponent);

        AssertJUnit.assertEquals("The draw mode should resolve",
                1, simulatedAi.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertTrue("The tap mode should also resolve",
                simulatedOpponent.getCreaturesInPlay().stream().allMatch(Card::isTapped));
    }

    @Test
    public void testPreservesDividedModalAllocations() {
        Game game = createGame();
        Player ai = game.getPlayers().get(1);

        addCard("Plains", ai);
        addCard("Swamp", ai);
        addCard("Forest", ai);
        Card first = addCard("Runeclaw Bear", ai);
        Card second = addCard("Raging Goblin", ai);
        Card charm = addCardToZone("Abzan Charm", ai, ZoneType.Hand);

        AbilitySub counters = chooseMode(ability(charm, ai), "Distribute");
        counters.getTargets().add(first);
        counters.getTargets().add(second);
        counters.addDividedAllocation(first, 1);
        counters.addDividedAllocation(second, 1);

        GameSimulator simulator = simulate(game, ai, counters.getRootAbility());
        Card simulatedFirst = (Card) simulator.getGameCopier().find(first);
        Card simulatedSecond = (Card) simulator.getGameCopier().find(second);

        AssertJUnit.assertEquals(1, simulatedFirst.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(1, simulatedSecond.getCounters(CounterEnumType.P1P1));
    }

    private Game createGame() {
        Game game = initAndCreateGame();
        moveToMain2(game, game.getPlayers().get(1));
        return game;
    }

    private Player simulatedPlayer(Game game, Player player, SpellAbility ability) {
        return (Player) simulate(game, player, ability).getGameCopier().find(player);
    }

    private GameSimulator simulate(Game game, Player player, SpellAbility ability) {
        GameSimulator simulator = createSimulator(game, player);
        simulator.simulateSpellAbility(ability);
        return simulator;
    }

    private SpellAbility ability(Card card, Player player) {
        SpellAbility ability = card.getFirstSpellAbility();
        ability.setActivatingPlayer(player);
        return ability;
    }

    private AbilitySub chooseMode(SpellAbility ability, String descriptionPrefix) {
        chooseModes(ability, descriptionPrefix);
        return ability.getSubAbility();
    }

    private void chooseModes(SpellAbility ability, String... descriptionPrefixes) {
        List<AbilitySub> options = CharmEffect.makePossibleOptions(ability);
        List<AbilitySub> modes = new ArrayList<>();
        for (String prefix : descriptionPrefixes) {
            modes.add(options.stream()
                    .filter(option -> option.getParam("SpellDescription").startsWith(prefix))
                    .findFirst()
                    .orElseThrow());
        }
        ability.setChosenList(modes);
        CharmEffect.chainAbilities(ability, modes);
    }

}
