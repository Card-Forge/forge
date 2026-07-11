package forge.ai.controller;

import forge.ai.ComputerUtilMana;
import forge.ai.PlayerControllerAi;
import forge.ai.simulation.GameSimulator;
import forge.ai.simulation.Plan;
import forge.ai.simulation.SimulationTest;
import forge.ai.simulation.SpellAbilityPicker;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

public class AutoPaymentTest extends SimulationTest {

    /** Place a spell on the game stack for payment tests (player zones have no Stack). */
    private Card addSpellOnStack(Game game, String name, Player p) {
        Card spell = createCard(name, p);
        spell.setGameTimestamp(game.getNextTimestamp());
        SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        game.getStack().addAndUnfreeze(sa);
        return spell;
    }

    @Test
    public void emitsPaymentPlanWhenDebugEnabled() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCards("Forest", 2, p);
        Card bear = addSpellOnStack(game, "Grizzly Bears", p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = bear.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        ManaCostBeingPaid mc = new ManaCostBeingPaid(sa.getPayCosts().getCostMana().getMana());

        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        String prevPlan = System.getProperty("forge.debugManaPayment.plan");
        try {
            System.setOut(new PrintStream(captured));
            System.setProperty("forge.debugManaPayment.plan", "true");
            final CardCollection[] sources = new CardCollection[1];
            p.runWithController(() -> sources[0] = ComputerUtilMana.getManaSourcesToPayCostForPaymentPrompt(
                    new ManaCostBeingPaid(mc), sa, p, false),
                    new PlayerControllerAi(game, p, p.getOriginalLobbyPlayer()));
            AssertJUnit.assertNotNull(sources[0]);
            AssertJUnit.assertFalse("Preview should list cards Auto would tap", sources[0].isEmpty());
            String log = captured.toString();
            AssertJUnit.assertTrue(log.contains("MANA_PAYMENT_PLAN [test]"));
            AssertJUnit.assertTrue(log.contains("Grizzly Bears"));
            AssertJUnit.assertTrue(log.contains("Forest") || log.contains("Tap"));
        } finally {
            System.setOut(originalOut);
            if (prevPlan == null) {
                System.clearProperty("forge.debugManaPayment.plan");
            } else {
                System.setProperty("forge.debugManaPayment.plan", prevPlan);
            }
        }
    }

    @Test
    public void canPayManaCostFromHandDoesNotEmitPaymentPlan() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCards("Forest", 2, p);
        Card bear = addCardToZone("Grizzly Bears", p, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = bear.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        ManaCostBeingPaid mc = new ManaCostBeingPaid(sa.getPayCosts().getCostMana().getMana());

        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        String prevPlan = System.getProperty("forge.debugManaPayment.plan");
        try {
            System.setOut(new PrintStream(captured));
            System.setProperty("forge.debugManaPayment.plan", "true");
            AssertJUnit.assertTrue(ComputerUtilMana.canPayManaCost(new ManaCostBeingPaid(mc), sa, p, false));
            AssertJUnit.assertFalse(captured.toString().contains("MANA_PAYMENT_PLAN"));
        } finally {
            System.setOut(originalOut);
            if (prevPlan == null) {
                System.clearProperty("forge.debugManaPayment.plan");
            } else {
                System.setProperty("forge.debugManaPayment.plan", prevPlan);
            }
        }
    }

    @Test
    public void dontPayWithAshnodsAltar() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        String llanowar = "Llanowar Elves";

        Card elf = addCard(llanowar,  p);
        elf.setSickness(false);
        Card altar = addCard("Ashnod's Altar", p);
        Card treasure = addToken("c_a_treasure_sac", p);

        // Two choices tap elf and sac treasure
        // OR Sac elf to Altar

        String stone = "Mind Stone";
        Card mindstone = addCardToZone(stone, p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        GameSimulator sim = createSimulator(game, p);
        int score = sim.simulateSpellAbility(mindstone.getFirstSpellAbility()).value;

        AssertJUnit.assertTrue(score > 0);
        Game simGame = sim.getSimulatedGameState();

        Card mindstoneBF = findCardWithName(simGame, stone);
        AssertJUnit.assertNotNull(mindstoneBF);

        Card elfCopy = findCardWithName(simGame, llanowar);
        AssertJUnit.assertNotNull(elfCopy);
    }

    @Test
    public void payWithTreasuresOverPhyrexianAltar() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        String squire = "Squire";

        List<Card> squires = addCards(squire, 6,  p);
        Card altar = addCard("Phyrexian Altar", p);
        List<Card> treasures = addTokens("c_a_treasure_sac", 6, p);

        String shivan = "Shivan Dragon";
        Card dragon = addCardToZone(shivan, p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        GameSimulator sim = createSimulator(game, p);
        int score = sim.simulateSpellAbility(dragon.getFirstSpellAbility()).value;

        AssertJUnit.assertTrue(score > 0);
        Game simGame = sim.getSimulatedGameState();

        Card dragonBF = findCardWithName(simGame, shivan);
        AssertJUnit.assertNotNull(dragonBF);
        AssertJUnit.assertEquals(dragonBF.getZone().getZoneType(), ZoneType.Battlefield);

        Card squireCopy = findCardWithName(simGame, squire);
        AssertJUnit.assertNotNull(squireCopy);

        Card treasureCopy = findCardWithName(simGame, "Treasure Token");
        AssertJUnit.assertNull(treasureCopy);
    }

    @Test
    public void payWithCreaturesOverSacrificeLands() {
        // Do not sacrifice debris. It can be tapped for Blue or Plains tapped for white. Tap elf instead.
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        Card elf = addCard("Llanowar Elves",  p);
        addCard("Seafloor Debris", p);
        addCard("Plains", p);
        addCard("Fervor", p);

        String griz = "Grizzly Bears";
        Card bears = addCardToZone(griz, p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        GameSimulator sim = createSimulator(game, p);
        int score = sim.simulateSpellAbility(bears.getFirstSpellAbility()).value;

        AssertJUnit.assertTrue(score > 0);
        Game simGame = sim.getSimulatedGameState();

        // Grizzly cast, Seafloor not sacrificed, Elf tapped
        Card grizBF = findCardWithName(simGame, griz);
        AssertJUnit.assertNotNull(grizBF);
        AssertJUnit.assertEquals(ZoneType.Battlefield, grizBF.getZone().getZoneType());

        Card debrisCopy = findCardWithName(simGame, "Seafloor Debris");
        AssertJUnit.assertNotNull(debrisCopy);

        Card elfCopy = findCardWithName(simGame, "Llanowar Elves");
        AssertJUnit.assertNotNull(elfCopy);
        AssertJUnit.assertTrue(elfCopy.isTapped());
    }

    @Test
    public void testKeepColorsOpen() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCards("Forest", 2, p);
        addCards("Swamp", 2, p);
        addCardToZone("Bear Cub", p, ZoneType.Hand);
        addCardToZone("Bear Cub", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        AssertJUnit.assertTrue(sa.getHostCard().isCreature());

        // AI able to cast both creatures
        Plan plan = picker.getPlan();
        AssertJUnit.assertEquals(2, plan.getDecisions().size());
    }
}
