package forge.ai.simulation;

import java.util.ArrayList;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.AiController;
import forge.ai.AiPlayDecision;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
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
    public void testMain1SafetyBaselineDoesNotResolveProposedWheel() {
        Scenario scenario = createDrawScenario(ZoneType.Hand, false, 20);
        addCard("Runeclaw Bear", scenario.ai);
        scenario.game.getPhaseHandler().devModeSet(PhaseType.MAIN1, scenario.ai);
        scenario.game.getAction().checkStateEffects(true);

        AssertJUnit.assertFalse("Combat lookahead must not resolve the proposed play in its own baseline",
                OnePlaySafetyChecker.isAcceptable(scenario.ai, scenario.drawAbility));
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
    public void testCascadeDeclinesUnsafeWheelIntoXyris() {
        Game game = createGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCards("Mountain", 2, ai);
        addCards("Forest", 2, ai);
        Card bloodbraid = addCardToZone("Bloodbraid Elf", ai, ZoneType.Hand);
        Card wheel = addCardToZone("Wheel of Fortune", ai, ZoneType.Library);
        fillLibrary(ai, 20);
        fillLibrary(opponent, 20);
        addCard("Xyris, the Writhing Storm", opponent);
        moveToMain2(game, ai);

        controller(ai).playChosenSpellAbility(ability(bloodbraid, ai));
        GameSimulator.resolveStack(game, opponent);

        AssertJUnit.assertTrue("Bloodbraid Elf should still resolve",
                hasCardInZone(ai, "Bloodbraid Elf", ZoneType.Battlefield));
        AssertJUnit.assertTrue("The declined cascade card should return to the library",
                hasCardInZone(ai, wheel.getName(), ZoneType.Library));
        AssertJUnit.assertEquals("Declining the Wheel should not create Xyris tokens",
                1, opponent.getCreaturesInPlay().size());
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
        Game game = createGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        ai.setLife(2, null);

        Card forest = addCardToZone("Forest", ai, ZoneType.Hand);
        addCard("Zo-Zu the Punisher", opponent);
        moveToMain2(game, ai);

        AssertJUnit.assertFalse("The land simulation should detect lethal Zo-Zu damage",
                OnePlaySafetyChecker.isAcceptable(ai, landAbility(forest, ai)));
        AssertJUnit.assertNull("The selected land play would kill the AI",
                controller(ai).chooseSpellAbilityToPlay());
    }

    @Test
    public void testAllowsLotusField() {
        Game game = createGame();
        Player ai = game.getPlayers().get(1);

        addCards("Forest", 3, ai);
        Card field = addCardToZone("Lotus Field", ai, ZoneType.Hand);
        moveToMain2(game, ai);

        AssertJUnit.assertTrue("Lotus Field should receive credit for replacing sacrificed lands",
                OnePlaySafetyChecker.isAcceptable(ai, landAbility(field, ai)));
        assertChosenCard(ai, "Lotus Field");
    }

    @Test
    public void testAllowsDelayedBlink() {
        Game game = createGame();
        Player ai = game.getPlayers().get(1);

        addCards("Plains", 3, ai);
        addCard("Shivan Dragon", ai);
        Card ghostway = addCardToZone("Ghostway", ai, ZoneType.Hand);
        moveToMain2(game, ai);

        SpellAbility ability = ability(ghostway, ai);
        AssertJUnit.assertTrue("Creatures exiled by Ghostway return at the next end step",
                OnePlaySafetyChecker.isAcceptable(ai, ability));
    }

    @Test
    public void testAllowsPhasingRescue() {
        Game game = createGame();
        Player ai = game.getPlayers().get(1);

        addCards("Plains", 4, ai);
        Card dragon = addCard("Shivan Dragon", ai);
        Card concealment = addCardToZone("Clever Concealment", ai, ZoneType.Hand);
        moveToMain2(game, ai);

        SpellAbility ability = ability(concealment, ai);
        ability.getTargets().add(dragon);
        AssertJUnit.assertTrue("Phased-out permanents return before their controller untaps",
                OnePlaySafetyChecker.isAcceptable(ai, ability));
    }

    @Test
    public void testAllowsFlickerformDelayedBlink() {
        Game game = createGame();
        Player ai = game.getPlayers().get(1);

        addCards("Plains", 4, ai);
        Card dragon = addCard("Shivan Dragon", ai);
        Card flickerform = addCard("Flickerform", ai);
        flickerform.attachToEntity(dragon, null);
        moveToMain2(game, ai);

        SpellAbility ability = activatedAbility(flickerform, ai);
        AssertJUnit.assertTrue("An Effect-card trigger returns the exiled creature and Auras",
                OnePlaySafetyChecker.isAcceptable(ai, ability));
    }

    @Test
    public void testDelayedDrawDoesNotExcuseImmediateLethalTrigger() {
        Game game = createGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        ai.setLife(1, null);

        Card bauble = addCard("Urza's Bauble", ai);
        addCard("Disciple of the Vault", opponent);
        addCardToZone("Forest", opponent, ZoneType.Hand);
        fillLibrary(ai, 1);
        moveToMain2(game, ai);

        SpellAbility ability = activatedAbility(bauble, ai);
        ability.getTargets().add(opponent);
        AssertJUnit.assertFalse("A next-turn draw must not excuse an immediate loss",
                OnePlaySafetyChecker.isAcceptable(ai, ability));
    }

    @Test
    public void testAllowsNextTurnDelayedDraw() {
        Game game = createGame();
        Player ai = game.getPlayers().get(1);

        addCards("Island", 2, ai);
        Card legacy = addCardToZone("Lat-Nam's Legacy", ai, ZoneType.Hand);
        addCardToZone("Runeclaw Bear", ai, ZoneType.Hand);
        fillLibrary(ai, 2);
        moveToMain2(game, ai);

        AssertJUnit.assertTrue("The shuffled card is repaid by the next-turn draw",
                OnePlaySafetyChecker.isAcceptable(ai, ability(legacy, ai)));
    }

    @Test
    public void testSimulationPreservesKickerChoice() {
        Game game = createGame();
        Player ai = game.getPlayers().get(1);

        addCards("Forest", 6, ai);
        Card woodreaders = addCardToZone("Citanul Woodreaders", ai, ZoneType.Hand);
        fillLibrary(ai, 2);
        moveToMain2(game, ai);

        SpellAbility baseAbility = ability(woodreaders, ai);
        SpellAbility kickedAbility = GameActionUtil.addOptionalCosts(
                baseAbility, GameActionUtil.getOptionalCostValues(baseAbility));
        kickedAbility.setActivatingPlayer(ai);

        GameSimulator simulator = simulate(game, ai, kickedAbility);
        Player simulatedAi = (Player) simulator.getGameCopier().find(ai);

        AssertJUnit.assertEquals("The kicked ETB should draw two cards",
                2, simulatedAi.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void testSimulationPreservesAnnouncedX() {
        Game game = createGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCards("Forest", 5, ai);
        Card hurricane = addCardToZone("Hurricane", ai, ZoneType.Hand);
        moveToMain2(game, ai);

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
    public void testSimulationPreservesChosenMode() {
        Game game = createGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCard("Plains", ai);
        addCard("Swamp", ai);
        addCard("Forest", ai);
        Card charm = addCardToZone("Abzan Charm", ai, ZoneType.Hand);
        addCard("Shivan Dragon", opponent);
        fillLibrary(ai, 2);
        moveToMain2(game, ai);

        SpellAbility ability = ability(charm, ai);
        chooseMode(ability, "You draw");

        GameSimulator simulator = simulate(game, ai, ability);
        Player simulatedAi = (Player) simulator.getGameCopier().find(ai);
        Player simulatedOpponent = (Player) simulator.getGameCopier().find(opponent);

        AssertJUnit.assertEquals("The copied Charm must use the selected draw mode",
                2, simulatedAi.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertNotNull("The simulation must not substitute the exile mode",
                findCardWithName(simulatedOpponent.getGame(), "Shivan Dragon"));
    }

    @Test
    public void testSimulationPreservesDividedModalAllocations() {
        Game game = createGame();
        Player ai = game.getPlayers().get(1);

        addCard("Plains", ai);
        addCard("Swamp", ai);
        addCard("Forest", ai);
        Card first = addCard("Runeclaw Bear", ai);
        Card second = addCard("Raging Goblin", ai);
        Card charm = addCardToZone("Abzan Charm", ai, ZoneType.Hand);
        moveToMain2(game, ai);

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

    @Test
    public void testMultiplayerSimulationResolvesEveryOpponentsTriggers() {
        Game game = createThreePlayerGame();
        Player xyrisPlayer = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        Player nekusarPlayer = game.getPlayers().get(2);

        addCards("Mountain", 3, ai);
        Card wheel = addCardToZone("Wheel of Fortune", ai, ZoneType.Hand);
        addCard("Xyris, the Writhing Storm", xyrisPlayer);
        addCard("Nekusar, the Mindrazer", nekusarPlayer);
        for (Player player : game.getPlayers()) {
            fillLibrary(player, 20);
        }
        moveToMain2(game, ai);

        GameSimulator simulator = simulate(game, ai, ability(wheel, ai));
        Player simulatedAi = (Player) simulator.getGameCopier().find(ai);
        Player simulatedXyrisPlayer = (Player) simulator.getGameCopier().find(xyrisPlayer);
        AssertJUnit.assertEquals("Nekusar should damage both of its opponents",
                13, simulatedAi.getLife());
        AssertJUnit.assertEquals(13, simulatedXyrisPlayer.getLife());
        AssertJUnit.assertEquals("Xyris should see draws by both of its opponents",
                14, simulatedXyrisPlayer.getCreaturesInPlay().size() - 1);
    }

    @Test
    public void testSafetyRejectsLethalThirdPlayerTrigger() {
        Game game = createThreePlayerGame();
        Player ai = game.getPlayers().get(1);
        Player thirdPlayer = game.getPlayers().get(2);
        ai.setLife(6, null);

        addCards("Mountain", 3, ai);
        Card wheel = addCardToZone("Wheel of Fortune", ai, ZoneType.Hand);
        addCard("Nekusar, the Mindrazer", thirdPlayer);
        for (Player player : game.getPlayers()) {
            fillLibrary(player, 20);
        }
        moveToMain2(game, ai);

        AssertJUnit.assertFalse("A trigger controlled by the third player should veto the Wheel",
                OnePlaySafetyChecker.isAcceptable(ai, ability(wheel, ai)));
    }

    @Test
    public void testScoreRejectsBadRemovalIntoGravePact() {
        AssertJUnit.assertFalse("Trading the AI's best creature for Serra Angel is a regression",
                evaluateMurderIntoGravePact("Blightsteel Colossus", "Serra Angel"));
    }

    private boolean evaluateMurderIntoGravePact(
            String aiCreatureName, String opponentCreatureName) {
        Game game = createGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCards("Swamp", 3, ai);
        addCard(aiCreatureName, ai);
        Card murder = addCardToZone("Murder", ai, ZoneType.Hand);
        addCard("Grave Pact", opponent);
        Card target = addCard(opponentCreatureName, opponent);
        moveToMain2(game, ai);

        SpellAbility ability = ability(murder, ai);
        ability.getTargets().add(target);
        return OnePlaySafetyChecker.isAcceptable(ai, ability);
    }

    private SpellAbility ability(Card card, Player player) {
        SpellAbility ability = card.getFirstSpellAbility();
        ability.setActivatingPlayer(player);
        return ability;
    }

    private SpellAbility activatedAbility(Card card, Player player) {
        SpellAbility ability = card.getSpellAbilities().stream()
                .filter(SpellAbility::isActivatedAbility).findFirst().orElseThrow();
        ability.setActivatingPlayer(player);
        return ability;
    }

    private SpellAbility landAbility(Card card, Player player) {
        SpellAbility ability = card.getAllPossibleAbilities(player, true).stream()
                .filter(SpellAbility::isLandAbility).findFirst().orElseThrow();
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
                    .findFirst().orElseThrow());
        }
        ability.setChosenList(modes);
        CharmEffect.chainAbilities(ability, modes);
    }

    private GameSimulator safetySimulator(Game game, Player ai) {
        return new GameSimulator(
                new SimulationController(new GameStateEvaluator.Score(0), 0),
                game, ai, null);
    }

    private GameSimulator simulate(Game game, Player ai, SpellAbility ability) {
        GameSimulator simulator = safetySimulator(game, ai);
        simulator.simulateSpellAbility(ability);
        return simulator;
    }

    private Scenario createDrawScenario(ZoneType zone, boolean impactTremors, int life) {
        Game game = createGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        ai.setLife(life, null);

        addCards("Mountain", 6, ai);
        Card wheel = addCardToZone("Wheel of Fortune", ai, zone);
        fillLibrary(ai, 20);
        fillLibrary(opponent, 20);

        addCard("Xyris, the Writhing Storm", opponent);
        if (impactTremors) {
            addCard("Impact Tremors", opponent);
        }
        moveToMain2(game, ai);

        SpellAbility drawAbility = ability(wheel, ai);
        return new Scenario(game, ai, opponent, drawAbility);
    }

    private Spell makeOptionalFreeCast(Scenario scenario) {
        Spell freeCast = (Spell) scenario.drawAbility.copyWithNoManaCost(scenario.ai);
        freeCast.setActivatingPlayer(scenario.ai);
        freeCast.setCastFromPlayEffect(true);
        return freeCast;
    }

    private void addPendingSpellToStack(Scenario scenario) {
        Card pendingCard = addCardToZone("Runeclaw Bear", scenario.opponent, ZoneType.Hand);
        SpellAbility pendingSpell = ability(pendingCard, scenario.opponent);
        scenario.game.getStackZone().add(pendingCard);
        scenario.game.getStack().add(pendingSpell);
    }

    private AiController ai(Player player) {
        return controller(player).getAi();
    }

    private void assertChosenCard(Player player, String cardName) {
        List<SpellAbility> choices = ai(player).chooseSpellAbilityToPlay();
        AssertJUnit.assertNotNull("The AI should select " + cardName, choices);
        AssertJUnit.assertEquals(cardName, choices.get(0).getHostCard().getName());
    }

    private boolean hasCardInZone(Player player, String cardName, ZoneType zone) {
        return player.getCardsIn(zone).stream()
                .anyMatch(card -> cardName.equals(card.getName()));
    }

    private PlayerControllerAi controller(Player player) {
        return (PlayerControllerAi) player.getController();
    }

    private void fillLibrary(Player player, int count) {
        for (int i = 0; i < count; i++) {
            addCardToZone("Runeclaw Bear", player, ZoneType.Library);
        }
    }

    private Game createGame() {
        Game game = initAndCreateGame();
        game.getPlayers().get(1).setTeam(0);
        game.getPlayers().get(0).setTeam(1);
        controller(game.getPlayers().get(1)).setUseSimulation(false);
        return game;
    }

    private Game createThreePlayerGame() {
        Game game = initAndCreateThreePlayerGame();
        controller(game.getPlayers().get(1)).setUseSimulation(false);
        return game;
    }

    private void moveToMain2(Game game, Player player) {
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, player);
        game.getAction().checkStateEffects(true);
    }

    private static final class Scenario {
        private final Game game;
        private final Player ai;
        private final Player opponent;
        private final SpellAbility drawAbility;

        private Scenario(Game game, Player ai, Player opponent, SpellAbility drawAbility) {
            this.game = game;
            this.ai = ai;
            this.opponent = opponent;
            this.drawAbility = drawAbility;
        }
    }
}
