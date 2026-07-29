package forge.ai.simulation;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.AiController;
import forge.ai.AiPlayDecision;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.Spell;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class OnePlaySafetyCheckerTest extends SimulationTest {

    @Test
    public void testWheelIntoXyrisImpactTremorsReportsLethal() {
        Scenario scenario = createDrawScenario(ZoneType.Hand, true, 6);

        AssertJUnit.assertFalse("The simulation should reject a play causing an immediate loss",
                OnePlaySafetyChecker.isAcceptable(scenario.ai, scenario.drawAbility));
    }

    @Test
    public void testAiAvoidsWheelGivingXyrisSevenSnakes() {
        Scenario scenario = createDrawScenario(ZoneType.Hand, false, 20);

        AssertJUnit.assertNull("The AI should decline a Wheel that gives an opponent seven Snakes",
                ai(scenario.ai).chooseSpellAbilityToPlay());
    }

    @Test
    public void testAiAllowsWheelIntoCommandZoneXyris() {
        Scenario scenario = createDrawScenario(ZoneType.Hand, false, 20);
        scenario.game.getAction().moveToCommand(scenario.xyris, null);
        scenario.game.getAction().checkStateEffects(true);

        List<SpellAbility> choices = ai(scenario.ai).chooseSpellAbilityToPlay();
        AssertJUnit.assertNotNull("Command-zone Xyris cannot trigger from the Wheel", choices);
        AssertJUnit.assertEquals("Wheel of Fortune", choices.get(0).getHostCard().getName());
    }

    @Test
    public void testAiDeclinesOptionalFreeWheelIntoXyris() {
        Scenario scenario = createDrawScenario(ZoneType.Exile, false, 20);
        Spell freeCast = makeOptionalFreeCast(scenario);

        AssertJUnit.assertEquals(AiPlayDecision.CurseEffects,
                ai(scenario.ai).canPlayFromEffectAI(freeCast, false, true));
        AssertJUnit.assertEquals("Mandatory casts cannot be declined",
                AiPlayDecision.WillPlay,
                ai(scenario.ai).canPlayFromEffectAI(freeCast, true, true));
    }

    @Test
    public void testOnePlaySafetyOnlyChecksResolvingStack() {
        Scenario scenario = createDrawScenario(ZoneType.Exile, false, 20);
        addPendingSpellToStack(scenario);
        Spell freeCast = makeOptionalFreeCast(scenario);

        AssertJUnit.assertTrue("Unsupported priority responses should fail open",
                OnePlaySafetyChecker.isAcceptable(scenario.ai, freeCast));

        scenario.game.getStack().setResolving(true);
        AssertJUnit.assertFalse("A resolving parent ability permits an incremental check",
                OnePlaySafetyChecker.isAcceptable(scenario.ai, freeCast));
    }

    @Test
    public void testAiAvoidsLethalLandPlay() {
        Game game = initAndCreateGame(true);
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        ai.setLife(2, null);

        Card forest = addCardToZone("Forest", ai, ZoneType.Hand);
        addCard("Zo-Zu the Punisher", opponent);
        moveToMain2(game, ai);

        SpellAbility ability = forest.getAllPossibleAbilities(ai, true).stream()
                .filter(SpellAbility::isLandAbility)
                .findFirst()
                .orElseThrow();
        AssertJUnit.assertFalse("The land simulation should detect lethal Zo-Zu damage",
                OnePlaySafetyChecker.isAcceptable(ai, ability));
        AssertJUnit.assertNull("The selected land play would kill the AI",
                ai.getController().chooseSpellAbilityToPlay());
    }

    @Test
    public void testOnePlaySafetyRewardsWheelWhenXyrisTokensAreLethalToOpponent() {
        Scenario scenario = createDrawScenario(ZoneType.Hand, false, 20);
        scenario.opponent.setLife(6, null);
        addCard("Suture Priest", scenario.ai);
        scenario.game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue("Simulation should recognize when the Xyris triggers win the game",
                OnePlaySafetyChecker.isAcceptable(scenario.ai, scenario.drawAbility));
    }

    @Test
    public void testNormalAiTriesAnotherCandidateAfterUnsafeWheel() {
        Scenario scenario = createDrawScenario(ZoneType.Hand, false, 20);
        addCards("Forest", 2, scenario.ai);
        addCardToZone("Runeclaw Bear", scenario.ai, ZoneType.Hand);

        List<SpellAbility> choices = ai(scenario.ai).chooseSpellAbilityToPlay();
        AssertJUnit.assertNotNull("Rejecting the Wheel should not force the AI to pass", choices);
        AssertJUnit.assertEquals("Runeclaw Bear", choices.get(0).getHostCard().getName());
    }

    @Test
    public void testEvaluationStopsAfterProposedPlay() {
        Game game = initAndCreateGame(true);
        Player ai = game.getPlayers().get(1);

        addCard("Island", ai);
        addCards("Forest", 2, ai);
        Card cantrip = addCardToZone("Reach Through Mists", ai, ZoneType.Hand);
        addCardToZone("Runeclaw Bear", ai, ZoneType.Hand);
        fillLibrary(ai, 6);
        moveToMain2(game, ai);

        SpellAbility ability = cantrip.getFirstSpellAbility();
        ability.setActivatingPlayer(ai);
        GameStateEvaluator.Score originalScore = new GameStateEvaluator().getScoreForGameState(game, ai);
        GameSimulator simulator = new GameSimulator(new SimulationController(originalScore, 0), game, ai, null);
        AssertJUnit.assertEquals("A depth-zero simulation must not include the Bear as a follow-up",
                originalScore.value, simulator.simulateSpellAbility(ability).value);
    }

    @Test
    public void testNormalAiAllowsExpectedCardCost() {
        Game game = initAndCreateGame(true);
        Player ritualAi = game.getPlayers().get(1);
        addCard("Swamp", ritualAi);
        addCardToZone("Dark Ritual", ritualAi, ZoneType.Hand);
        addCardToZone("Dark Confidant", ritualAi, ZoneType.Hand);
        moveToMain2(game, ritualAi);

        List<SpellAbility> choices = ai(ritualAi).chooseSpellAbilityToPlay();
        AssertJUnit.assertNotNull("The safety check should allow a useful mana ritual", choices);
        AssertJUnit.assertEquals("Dark Ritual", choices.get(0).getHostCard().getName());
    }

    @Test
    public void testScoreRejectsBadRemovalIntoGravePact() {
        AssertJUnit.assertFalse("Trading the AI's best creature for Serra Angel is a regression",
                evaluateMurderIntoGravePact("Blightsteel Colossus", "Serra Angel"));
    }

    @Test
    public void testScoreAllowsGoodRemovalIntoGravePact() {
        AssertJUnit.assertTrue("Trading Runeclaw Bear for Shivan Dragon improves the position",
                evaluateMurderIntoGravePact("Runeclaw Bear", "Shivan Dragon"));
    }

    private boolean evaluateMurderIntoGravePact(String aiCreatureName, String opponentCreatureName) {
        Game game = initAndCreateGame(true);
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCards("Swamp", 3, ai);
        addCard(aiCreatureName, ai);
        Card murder = addCardToZone("Murder", ai, ZoneType.Hand);
        addCard("Grave Pact", opponent);
        Card target = addCard(opponentCreatureName, opponent);
        moveToMain2(game, ai);

        SpellAbility ability = murder.getFirstSpellAbility();
        ability.setActivatingPlayer(ai);
        ability.getTargets().add(target);
        return OnePlaySafetyChecker.isAcceptable(ai, ability);
    }

    private Scenario createDrawScenario(ZoneType zone, boolean impactTremors, int life) {
        Game game = initAndCreateGame(true);
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        ai.setLife(life, null);

        addCards("Mountain", 6, ai);
        Card wheel = addCardToZone("Wheel of Fortune", ai, zone);
        fillLibrary(ai, 20);
        fillLibrary(opponent, 20);

        Card xyris = addCard("Xyris, the Writhing Storm", opponent);
        if (impactTremors) {
            addCard("Impact Tremors", opponent);
        }
        moveToMain2(game, ai);

        SpellAbility drawAbility = wheel.getFirstSpellAbility();
        drawAbility.setActivatingPlayer(ai);
        return new Scenario(game, ai, opponent, xyris, drawAbility);
    }

    private Spell makeOptionalFreeCast(Scenario scenario) {
        Spell freeCast = (Spell) scenario.drawAbility.copyWithNoManaCost(scenario.ai);
        freeCast.setCastFromPlayEffect(true);
        return freeCast;
    }

    private void addPendingSpellToStack(Scenario scenario) {
        Card pendingCard = addCardToZone("Runeclaw Bear", scenario.opponent, ZoneType.Hand);
        SpellAbility pendingSpell = pendingCard.getFirstSpellAbility();
        pendingSpell.setActivatingPlayer(scenario.opponent);
        scenario.game.getStackZone().add(pendingCard);
        scenario.game.getStack().add(pendingSpell);
    }

    private AiController ai(Player player) {
        return ((PlayerControllerAi) player.getController()).getAi();
    }

    private void fillLibrary(Player player, int count) {
        for (int i = 0; i < count; i++) {
            addCardToZone("Runeclaw Bear", player, ZoneType.Library);
        }
    }

    private void moveToMain2(Game game, Player player) {
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, player);
        game.getAction().checkStateEffects(true);
    }

    private static final class Scenario {
        private final Game game;
        private final Player ai;
        private final Player opponent;
        private final Card xyris;
        private final SpellAbility drawAbility;

        private Scenario(Game game, Player ai, Player opponent, Card xyris, SpellAbility drawAbility) {
            this.game = game;
            this.ai = ai;
            this.opponent = opponent;
            this.xyris = xyris;
            this.drawAbility = drawAbility;
        }
    }
}
