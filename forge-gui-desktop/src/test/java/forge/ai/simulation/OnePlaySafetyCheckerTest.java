package forge.ai.simulation;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.AiController;
import forge.ai.AiPlayDecision;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
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
    public void testAllowsExplicitDiscardCost() {
        Game game = initAndCreateGame(true);
        Player ai = game.getPlayers().get(1);

        addCards("Swamp", 3, ai);
        Card phantasmagorian = addCardToZone("Phantasmagorian", ai, ZoneType.Graveyard);
        addCardToZone("Griselbrand", ai, ZoneType.Hand);
        addCardToZone("Jin-Gitaxias, Core Augur", ai, ZoneType.Hand);
        addCardToZone("Elesh Norn, Grand Cenobite", ai, ZoneType.Hand);
        moveToMain2(game, ai);

        SpellAbility ability = phantasmagorian.getSpellAbilities().stream()
                .filter(SpellAbility::isActivatedAbility).findFirst().orElseThrow();
        ability.setActivatingPlayer(ai);
        AssertJUnit.assertTrue("The safety check should evaluate the play from its post-cost state",
                OnePlaySafetyChecker.isAcceptable(ai, ability));
    }

    @Test
    public void testAllowsScheduledGraveyardReturn() {
        Game game = initAndCreateGame(true);
        Player ai = game.getPlayers().get(1);

        addCards("Swamp", 3, ai);
        Card familiar = addCard("Nine-Lives Familiar", ai);
        familiar.setCounters(CounterEnumType.REVIVAL, 1);
        Card murder = addCardToZone("Murder", ai, ZoneType.Hand);
        moveToMain2(game, ai);

        SpellAbility ability = murder.getFirstSpellAbility();
        ability.setActivatingPlayer(ai);
        ability.getTargets().add(familiar);
        AssertJUnit.assertTrue("The Familiar is scheduled to return from the graveyard",
                OnePlaySafetyChecker.isAcceptable(ai, ability));
    }

    @Test
    public void testAllowsDelayedHandReturn() {
        Game game = initAndCreateGame(true);
        Player ai = game.getPlayers().get(1);

        addCards("Mountain", 2, ai);
        Card bliss = addCardToZone("Ignorant Bliss", ai, ZoneType.Hand);
        addCardToZone("Shivan Dragon", ai, ZoneType.Hand);
        addCardToZone("Serra Angel", ai, ZoneType.Hand);
        fillLibrary(ai, 1);
        moveToMain2(game, ai);

        SpellAbility ability = bliss.getFirstSpellAbility();
        ability.setActivatingPlayer(ai);
        AssertJUnit.assertTrue("Cards exiled by Ignorant Bliss return before its delayed draw",
                OnePlaySafetyChecker.isAcceptable(ai, ability));
    }

    @Test
    public void testAllowsMemoryJarTemporaryHands() {
        Game game = initAndCreateGame(true);
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        Card jar = addCard("Memory Jar", ai);
        addCardToZone("Shivan Dragon", ai, ZoneType.Hand);
        addCardToZone("Serra Angel", ai, ZoneType.Hand);
        addCardToZone("Forest", opponent, ZoneType.Hand);
        addCardToZone("Island", opponent, ZoneType.Hand);
        fillLibrary(ai, 10);
        fillLibrary(opponent, 10);
        moveToMain2(game, ai);

        SpellAbility ability = jar.getSpellAbilities().stream()
                .filter(SpellAbility::isActivatedAbility).findFirst().orElseThrow();
        ability.setActivatingPlayer(ai);
        AssertJUnit.assertTrue("Restoring the original hands should not make the activation unsafe",
                OnePlaySafetyChecker.isAcceptable(ai, ability));
    }

    @Test
    public void testAllowsNextTurnDelayedDraw() {
        Game game = initAndCreateGame(true);
        Player ai = game.getPlayers().get(1);

        addCards("Island", 2, ai);
        Card legacy = addCardToZone("Lat-Nam's Legacy", ai, ZoneType.Hand);
        addCardToZone("Runeclaw Bear", ai, ZoneType.Hand);
        fillLibrary(ai, 2);
        moveToMain2(game, ai);

        SpellAbility ability = legacy.getFirstSpellAbility();
        ability.setActivatingPlayer(ai);
        AssertJUnit.assertTrue("The shuffled card is repaid by the next-turn draw",
                OnePlaySafetyChecker.isAcceptable(ai, ability));
    }

    @Test
    public void testDelayedDrawDoesNotExcuseImmediateLethalTrigger() {
        AssertJUnit.assertFalse("A next-turn draw must not excuse an immediate loss",
                evaluateBaubleAtOneLife("Disciple of the Vault"));
    }

    @Test
    public void testRejectsLethalNextTurnDelayedDraw() {
        AssertJUnit.assertFalse("The scheduled draw is lethal while Nekusar remains in play",
                evaluateBaubleAtOneLife("Nekusar, the Mindrazer"));
    }

    private boolean evaluateBaubleAtOneLife(String opponentPermanent) {
        Game game = initAndCreateGame(true);
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        ai.setLife(1, null);

        Card bauble = addCard("Urza's Bauble", ai);
        addCard(opponentPermanent, opponent);
        addCardToZone("Forest", opponent, ZoneType.Hand);
        fillLibrary(ai, 1);
        moveToMain2(game, ai);

        SpellAbility ability = bauble.getSpellAbilities().stream()
                .filter(SpellAbility::isActivatedAbility).findFirst().orElseThrow();
        ability.setActivatingPlayer(ai);
        ability.getTargets().add(opponent);
        return OnePlaySafetyChecker.isAcceptable(ai, ability);
    }

    @Test
    public void testAllowsWideBoardPhasingRescue() {
        Game game = initAndCreateGame(true);
        Player ai = game.getPlayers().get(1);

        addCards("Plains", 4, ai);
        Card[] creatures = {
                addCard("Shivan Dragon", ai),
                addCard("Serra Angel", ai),
                addCard("Runeclaw Bear", ai)
        };
        // Keep Convoke's tap choice from affecting this phasing-only assertion.
        for (Card creature : creatures) {
            creature.setTapped(true);
        }
        Card concealment = addCardToZone("Clever Concealment", ai, ZoneType.Hand);
        moveToMain2(game, ai);

        SpellAbility ability = concealment.getFirstSpellAbility();
        ability.setActivatingPlayer(ai);
        for (Card creature : creatures) {
            ability.getTargets().add(creature);
        }
        AssertJUnit.assertTrue("A guaranteed phase-in should retain strategic value",
                OnePlaySafetyChecker.isAcceptable(ai, ability));
    }

    @Test
    public void testRejectsOublietteOnOwnCreature() {
        Game game = initAndCreateGame(true);
        Player ai = game.getPlayers().get(1);

        addCards("Swamp", 3, ai);
        addCard("Shivan Dragon", ai);
        Card oubliette = addCardToZone("Oubliette", ai, ZoneType.Hand);
        moveToMain2(game, ai);

        SpellAbility ability = oubliette.getFirstSpellAbility();
        ability.setActivatingPlayer(ai);
        AssertJUnit.assertFalse("A contingent phase-in should remain a strategic loss",
                OnePlaySafetyChecker.isAcceptable(ai, ability));
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
