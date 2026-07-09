package forge.ai.controller;

import forge.ai.ComputerUtilMana;
import forge.ai.PlayerControllerAi;
import forge.ai.simulation.GameSimulator;
import forge.ai.simulation.Plan;
import forge.ai.simulation.SimulationTest;
import forge.ai.simulation.SpellAbilityPicker;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
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

import java.util.List;

public class AutoPaymentTest extends SimulationTest {
    private static ManaCostBeingPaid cost(String s) {
        return new ManaCostBeingPaid(new ManaCost(new ManaCostParser(s)));
    }

    private boolean canAutoPay(Game game, Player p, ManaCostBeingPaid mc, SpellAbility sa) {
        final boolean[] result = new boolean[1];
        p.runWithController(() -> result[0] = ComputerUtilMana.canPayManaCost(mc, sa, p, false),
                new PlayerControllerAi(game, p, p.getOriginalLobbyPlayer()));
        return result[0];
    }

    private boolean prodAutoPay(Game game, Player p, ManaCostBeingPaid mc, SpellAbility sa) {
        final boolean[] result = new boolean[1];
        p.runWithController(() -> result[0] = ComputerUtilMana.payManaCost(mc, sa, p, false),
                new PlayerControllerAi(game, p, p.getOriginalLobbyPlayer()));
        return result[0];
    }

    private void assertProductionPayment(Game game, Player p, ManaCostBeingPaid mc, SpellAbility sa) {
        AssertJUnit.assertTrue(canAutoPay(game, p, mc, sa));
        AssertJUnit.assertTrue(prodAutoPay(game, p, mc, sa));
    }

    private int countTapped(Game game, String name) {
        int i = 0;
        for (Card c : game.getCardsIn(ZoneType.Battlefield)) {
            if (c.getName().equals(name) && c.isTapped()) {
                i++;
            }
        }
        return i;
    }

    /** Find a spell card after simulation (hand, stack, graveyard, or battlefield). */
    private Card findSpellCard(Game game, String name) {
        for (ZoneType zone : new ZoneType[] { ZoneType.Hand, ZoneType.Stack, ZoneType.Graveyard, ZoneType.Battlefield }) {
            for (Card c : game.getCardsIn(zone)) {
                if (c.getName().equals(name)) {
                    return c;
                }
            }
        }
        return null;
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

    // {R}{W} with Mountain + Plains + Signet should consolidate onto one basic + Signet (rule A):
    // exactly one basic tapped, Signet used, one basic left untapped.
    @Test
    public void consolidatesTwoColoredShardsOntoSignet() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Mountain", p);
        addCard("Plains", p);
        addCard("Boros Signet", p);
        Card spell = addCardToZone("Lightning Helix", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        assertProductionPayment(game, p, cost("R W"), sa);

        AssertJUnit.assertEquals("Signet should be tapped", 1, countTapped(game, "Boros Signet"));
        int tappedBasics = countTapped(game, "Mountain") + countTapped(game, "Plains");
        AssertJUnit.assertEquals("Only one basic should pay the Signet's {1}", 1, tappedBasics);
    }

    // {R} alone with Mountain + Signet should tap the Mountain, not the Signet (single-shard penalty).
    @Test
    public void singleShardPrefersBasicOverSignet() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Mountain", p);
        addCard("Boros Signet", p);
        Card spell = addCardToZone("Shock", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        assertProductionPayment(game, p, cost("R"), sa);

        AssertJUnit.assertEquals("Mountain should be tapped for R", 1, countTapped(game, "Mountain"));
        AssertJUnit.assertEquals("Signet should be untapped", 0, countTapped(game, "Boros Signet"));
    }

    // {R}{W} with two Signets + one Plains should use one Signet + Plains, not both Signets (anti-chain rule D).
    @Test
    public void doesNotChainTwoSignets() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Boros Signet", p);
        addCard("Boros Signet", p);
        addCard("Plains", p);
        Card spell = addCardToZone("Lightning Helix", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        assertProductionPayment(game, p, cost("R W"), sa);

        AssertJUnit.assertEquals("Exactly one Signet should be used", 1, countTapped(game, "Boros Signet"));
        AssertJUnit.assertEquals("Plains pays the Signet's {1}", 1, countTapped(game, "Plains"));
    }

    // {1}{R}{R} with Plains + two Rakdos Signets (3 mana total): Plains pays the first Signet's {1},
    // the Signet's {B} pays the second Signet's {1}, and both Signets' {R} plus a {B} pay Aisha.
    @Test
    public void chainedSignetsPayTripleRedSpell() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Plains", p);
        addCard("Rakdos Signet", p);
        addCard("Rakdos Signet", p);
        Card spell = addCardToZone("Aisha of Sparks and Smoke", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        AssertJUnit.assertEquals(3, spell.getManaCost().getCMC());
        ManaCostBeingPaid mc = new ManaCostBeingPaid(spell.getManaCost());

        AssertJUnit.assertTrue(canAutoPay(game, p, mc, sa));

        CardCollection sources = predictedManaSources(game, p, new ManaCostBeingPaid(spell.getManaCost()), sa);
        AssertJUnit.assertTrue("Plains should pay the first Signet's {1}",
                sources.anyMatch(c -> "Plains".equals(c.getName())));
        AssertJUnit.assertEquals("Both Rakdos Signets should be used", 2,
                sources.stream().filter(c -> "Rakdos Signet".equals(c.getName())).count());

        AssertJUnit.assertTrue("Production auto-pay should chain Signets for a 3 CMC spell",
                prodAutoPay(game, p, new ManaCostBeingPaid(spell.getManaCost()), sa));
        AssertJUnit.assertEquals("Plains should be tapped", 1, countTapped(game, "Plains"));
        AssertJUnit.assertEquals("Both Signets should be tapped", 2, countTapped(game, "Rakdos Signet"));
    }

    // {1}{R}{R} with Plains + three Rakdos Signets: only two Signets are needed; the third must stay untapped.
    @Test
    public void chainedSignetsIgnoreExtraSignet() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Plains", p);
        addCards("Rakdos Signet", 3, p);
        Card spell = addCardToZone("Aisha of Sparks and Smoke", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        ManaCostBeingPaid mc = new ManaCostBeingPaid(spell.getManaCost());

        AssertJUnit.assertTrue(canAutoPay(game, p, mc, sa));
        AssertJUnit.assertTrue(prodAutoPay(game, p, new ManaCostBeingPaid(spell.getManaCost()), sa));
        AssertJUnit.assertEquals("Only two Signets should be tapped", 2, countTapped(game, "Rakdos Signet"));
    }

    // {1}{R}{R} with Plains + Rakdos Signet + Cascade Bluffs: Plains -> Signet {B}{R}, pool {R} pays
    // Bluffs' {U/R} for {R}{R}, then {R}{R}{B} pays Aisha.
    @Test
    public void signetAndCascadeBluffsPayTripleRedSpell() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Plains", p);
        addCard("Rakdos Signet", p);
        addCard("Cascade Bluffs", p);
        Card spell = addCardToZone("Aisha of Sparks and Smoke", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        ManaCostBeingPaid mc = new ManaCostBeingPaid(spell.getManaCost());

        AssertJUnit.assertTrue(canAutoPay(game, p, mc, sa));

        CardCollection sources = predictedManaSources(game, p, new ManaCostBeingPaid(spell.getManaCost()), sa);
        AssertJUnit.assertTrue("Plains should pay the Signet's {1}",
                sources.anyMatch(c -> "Plains".equals(c.getName())));
        AssertJUnit.assertTrue("Rakdos Signet should be used",
                sources.anyMatch(c -> "Rakdos Signet".equals(c.getName())));
        AssertJUnit.assertTrue("Cascade Bluffs should be used",
                sources.anyMatch(c -> "Cascade Bluffs".equals(c.getName())));
        AssertJUnit.assertFalse("A second Signet is not required",
                sources.stream().filter(c -> "Rakdos Signet".equals(c.getName())).count() > 1);

        AssertJUnit.assertTrue(prodAutoPay(game, p, new ManaCostBeingPaid(spell.getManaCost()), sa));
        AssertJUnit.assertEquals("Plains should be tapped", 1, countTapped(game, "Plains"));
        AssertJUnit.assertEquals("Signet should be tapped", 1, countTapped(game, "Rakdos Signet"));
        AssertJUnit.assertEquals("Cascade Bluffs should be tapped", 1, countTapped(game, "Cascade Bluffs"));
    }

    // {B} with Swamp + Initiates: tap the Swamp, never the useless 1:1 filter (rule H).
    @Test
    public void skipsUselessFilterWhenDirectSourceExists() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Swamp", p);
        Card initiates = addCard("Initiates of the Ebon Hand", p);
        initiates.setSickness(false);
        Card spell = addCardToZone("Duress", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        assertProductionPayment(game, p, cost("B"), sa);

        AssertJUnit.assertEquals("Swamp should be tapped for B", 1, countTapped(game, "Swamp"));
        AssertJUnit.assertEquals("Initiates should not be used", 0, countTapped(game, "Initiates of the Ebon Hand"));
    }

    // {R}{W} with only Plains + Signet (no Mountain) is still payable: Plains -> Signet {1}, Signet -> {R}{W}.
    @Test
    public void filterOnlyPathIsFeasible() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Plains", p);
        addCard("Boros Signet", p);
        Card spell = addCardToZone("Lightning Helix", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue(canAutoPay(game, p, cost("R W"), spell.getFirstSpellAbility()));
    }

    // {1}{R}{W} with only 1 Plains + Signet is not enough mana; Auto must report infeasible.
    @Test
    public void insufficientManaIsInfeasible() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Plains", p);
        addCard("Boros Signet", p);
        Card spell = addCardToZone("Lightning Helix", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertFalse(canAutoPay(game, p, cost("1 R W"), spell.getFirstSpellAbility()));
    }

    // Skycloud Expanse ({1}{T}: Add {W}{U}) with a Wastes for its {1} can pay {W}{U}.
    @Test
    public void filterLandOnlyBaseIsFeasible() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Wastes", p);
        addCard("Skycloud Expanse", p);
        Card spell = addCardToZone("Azorius Charm", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue(canAutoPay(game, p, cost("W U"), spell.getFirstSpellAbility()));
    }

    // Selesnya Signet should cover {G}{W} (with a reusable source for its {1}) instead of sacrificing Lotus Petal.
    @Test
    public void signetConsolidatesColoredShardsOverLotusPetal() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Wastes", p);
        addCard("Selesnya Signet", p);
        addCard("Lotus Petal", p);
        Card spell = addCardToZone("Arcus Acolyte", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        GameSimulator sim = createSimulator(game, p);
        int score = sim.simulateSpellAbility(spell.getFirstSpellAbility()).value;
        AssertJUnit.assertTrue(score > 0);
        Game simGame = sim.getSimulatedGameState();

        AssertJUnit.assertNotNull(findSpellCard(simGame, "Arcus Acolyte"));
        AssertJUnit.assertNotNull("Lotus Petal should not be sacrificed", findSpellCard(simGame, "Lotus Petal"));
        AssertJUnit.assertEquals("Signet should be tapped", 1, countTapped(simGame, "Selesnya Signet"));
    }

    // {W}{G}: Selesnya Signet + Mox Emerald for its {1} beats sacrificing Lotus Petal.
    @Test
    public void signetConsolidatesOverPetalWithMoxEmerald() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Mox Emerald", p);
        addCard("Selesnya Signet", p);
        addCard("Lotus Petal", p);
        Card spell = addCardToZone("Arcus Acolyte", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue(canAutoPay(game, p, cost("W G"), spell.getFirstSpellAbility()));
    }

    // Study Hall ({1}: any) can pay the second {W} when Plains pays the first.
    @Test
    public void filterLandPaysSecondColoredShardAfterBasic() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Plains", p);
        addCard("Plains", p);
        addCard("Study Hall", p);
        Card spell = addCardToZone("Flowering of the White Tree", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue(canAutoPay(game, p, cost("W W"), spell.getFirstSpellAbility()));
    }

    // Signet {1} should tap Mountain, not Plains, when a {W} commander in command zone still needs white mana.
    @Test
    public void filterActivationPreservesCommandZoneCastability() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Mountain", p);
        addCard("Plains", p);
        addCard("Boros Signet", p);
        addCardToZone("Yoshimaru, Ever Faithful", p, ZoneType.Command);
        Card spell = addCardToZone("Lightning Helix", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        assertProductionPayment(game, p, cost("R W"), sa);

        AssertJUnit.assertEquals("Signet should be tapped", 1, countTapped(game, "Boros Signet"));
        AssertJUnit.assertEquals("Mountain should pay the Signet's {1}", 1, countTapped(game, "Mountain"));
        AssertJUnit.assertEquals("Plains should stay untapped for the command-zone spell", 0, countTapped(game, "Plains"));
    }

    // Canopy Vista produces {W} directly; Study Hall should not be used for a lone {W} pip.
    @Test
    public void studyHallDoesNotBeatDualLandForSingleWhite() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Study Hall", p);
        addCard("Canopy Vista", p);
        addCard("Snow-Covered Forest", p);
        Card spell = addCardToZone("Healing Salve", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        AssertJUnit.assertTrue(canAutoPay(game, p, cost("W"), sa));

        CardCollection sources = predictedManaSources(game, p, cost("W"), sa);
        AssertJUnit.assertTrue("Canopy Vista should pay {W} directly", sources.anyMatch(c -> "Canopy Vista".equals(c.getName())));
        AssertJUnit.assertFalse("Study Hall should not be used for a single {W}", sources.anyMatch(c -> "Study Hall".equals(c.getName())));
    }

    private CardCollection predictedManaSources(Game game, Player p, ManaCostBeingPaid mc, SpellAbility sa) {
        final CardCollection[] sources = new CardCollection[1];
        p.runWithController(() -> sources[0] = ComputerUtilMana.getManaSourcesToPayCost(mc, sa, p),
                new PlayerControllerAi(game, p, p.getOriginalLobbyPlayer()));
        return sources[0];
    }

    // {G} with Plains + Study Hall + Lotus Petal should tap Plains for Study Hall's {1}, not sacrifice Petal.
    @Test
    public void studyHallBeatsLotusPetalForSingleGreen() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Plains", p);
        addCard("Study Hall", p);
        addCard("Lotus Petal", p);
        Card spell = addCardToZone("Hardened Scales", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        AssertJUnit.assertTrue(canAutoPay(game, p, cost("G"), sa));

        CardCollection sources = predictedManaSources(game, p, cost("G"), sa);
        AssertJUnit.assertTrue("Study Hall should produce {G}", sources.anyMatch(c -> "Study Hall".equals(c.getName())));
        AssertJUnit.assertTrue("Plains should pay Study Hall's {1}", sources.anyMatch(c -> "Plains".equals(c.getName())));
        AssertJUnit.assertFalse("Lotus Petal should not be sacrificed", sources.anyMatch(c -> "Lotus Petal".equals(c.getName())));
    }

    // Generic {1} should tap colorless mana, not an any-mana signet, when {W}{W} also need paying.
    @Test
    public void genericShardPrefersColorlessOverSignet() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Mind Stone", p);
        addCard("Plains", p);
        addCard("Plains", p);
        addCard("Arcane Signet", p);
        Card spell = addCardToZone("Akroma's Vengeance", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        AssertJUnit.assertTrue(canAutoPay(game, p, cost("1 W W"), sa));

        CardCollection sources = predictedManaSources(game, p, cost("1 W W"), sa);
        AssertJUnit.assertTrue("Mind Stone should pay generic {1}", sources.anyMatch(c -> "Mind Stone".equals(c.getName())));
        AssertJUnit.assertFalse("Arcane Signet should not pay generic {1}", sources.anyMatch(c -> "Arcane Signet".equals(c.getName())));
    }

    // Lone generic {1} should still prefer a colorless rock over a colored basic.
    @Test
    public void genericShardPrefersColorlessWithoutColorlessDemand() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Mind Stone", p);
        addCard("Plains", p);
        Card spell = addCardToZone("Expedition Map", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        CardCollection sources = predictedManaSources(game, p, cost("1"), sa);
        AssertJUnit.assertTrue("Mind Stone should pay generic {1}", sources.anyMatch(c -> "Mind Stone".equals(c.getName())));
        AssertJUnit.assertFalse("Plains should not pay generic {1}", sources.anyMatch(c -> "Plains".equals(c.getName())));
    }

    // {1}{W}: Plains pays {W}, colorless source pays generic {1}; Forest stays untapped for future {G}.
    @Test
    public void colorlessGenericPreservesColoredBasicsForColoredPips() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Plains", p);
        addCard("Forest", p);
        addCard("Reliquary Tower", p);
        Card spell = addCardToZone("Sheltered by Ghosts", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        ManaCostBeingPaid mc = cost("1 W");
        AssertJUnit.assertTrue(canAutoPay(game, p, mc, sa));

        CardCollection sources = predictedManaSources(game, p, mc, sa);
        AssertJUnit.assertTrue("Plains should pay {W}", sources.anyMatch(c -> "Plains".equals(c.getName())));
        AssertJUnit.assertTrue("Colorless source should pay generic {1}",
                sources.anyMatch(c -> "Reliquary Tower".equals(c.getName())));
        AssertJUnit.assertFalse("Forest should not pay generic {1}", sources.anyMatch(c -> "Forest".equals(c.getName())));

        AssertJUnit.assertTrue("Production auto-pay should match feasibility", prodAutoPay(game, p, mc, sa));
    }

    // Eldrazi and other {C}-heavy hands should spend colored mana on generic pips and keep rocks for {C}.
    @Test
    public void reservesColorlessWhenHandNeedsColorlessPips() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Mind Stone", p);
        addCard("Plains", p);
        addCardToZone("Thought-Knot Seer", p, ZoneType.Hand);
        Card spell = addCardToZone("Expedition Map", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        CardCollection sources = predictedManaSources(game, p, cost("1"), sa);
        AssertJUnit.assertTrue("Plains should pay generic {1}", sources.anyMatch(c -> "Plains".equals(c.getName())));
        AssertJUnit.assertFalse("Mind Stone should be reserved for {C}", sources.anyMatch(c -> "Mind Stone".equals(c.getName())));
    }

    // Signet {1} should prefer colorless mana (Mind Stone) over a colored basic when both can pay generic.
    @Test
    public void nestedActivationPrefersColorlessOverColoredBasic() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Mind Stone", p);
        addCard("Mountain", p);
        addCard("Boros Signet", p);
        Card spell = addCardToZone("Lightning Helix", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        AssertJUnit.assertTrue(canAutoPay(game, p, cost("R W"), sa));

        CardCollection sources = predictedManaSources(game, p, cost("R W"), sa);
        AssertJUnit.assertTrue("Mind Stone should pay the Signet's {1}", sources.anyMatch(c -> "Mind Stone".equals(c.getName())));
        AssertJUnit.assertFalse("Mountain should not pay generic {1}", sources.anyMatch(c -> "Mountain".equals(c.getName())));
        AssertJUnit.assertTrue("Signet should produce both colors", sources.anyMatch(c -> "Boros Signet".equals(c.getName())));
    }

    // {2} with Sungrass Prairie + Study Hall should tap both (Prairie pays both generic pips), not also a basic.
    @Test
    public void multiManaFilterLandConsolidatesGenericCost() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Snow-Covered Forest", p);
        addCard("Study Hall", p);
        addCard("Sungrass Prairie", p);
        addCard("Lotus Petal", p);
        Card spell = addCardToZone("Shadowspear", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        AssertJUnit.assertTrue(canAutoPay(game, p, cost("2"), sa));

        CardCollection sources = predictedManaSources(game, p, cost("2"), sa);
        AssertJUnit.assertTrue("Sungrass Prairie should pay {2}",
                sources.anyMatch(c -> "Sungrass Prairie".equals(c.getName())));
        AssertJUnit.assertTrue("Study Hall should pay Sungrass Prairie's {1}",
                sources.anyMatch(c -> "Study Hall".equals(c.getName())));
        AssertJUnit.assertFalse("Forest should not be tapped for {2}",
                sources.anyMatch(c -> "Snow-Covered Forest".equals(c.getName())));
        AssertJUnit.assertFalse("Lotus Petal should not be sacrificed for {2}",
                sources.anyMatch(c -> "Lotus Petal".equals(c.getName())));

        AssertJUnit.assertTrue("Production auto-pay should match feasibility",
                prodAutoPay(game, p, cost("2"), sa));
    }

    // {2}{G}{G} should tap Sol Ring once for {2}, not Thought Vessel + Sol Ring.
    @Test
    public void solRingPaysDoubleGenericWithoutExtraRock() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Snow-Covered Forest", p);
        addCard("Forest", p);
        addCard("Sol Ring", p);
        addCard("Thought Vessel", p);
        Card spell = addCardToZone("Ouroboroid", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        AssertJUnit.assertTrue(canAutoPay(game, p, cost("2 G G"), sa));

        CardCollection sources = predictedManaSources(game, p, cost("2 G G"), sa);
        AssertJUnit.assertTrue("Sol Ring should pay {2}", sources.anyMatch(c -> "Sol Ring".equals(c.getName())));
        AssertJUnit.assertFalse("Thought Vessel should not pay generic",
                sources.anyMatch(c -> "Thought Vessel".equals(c.getName())));

        AssertJUnit.assertTrue("Production auto-pay should match feasibility",
                prodAutoPay(game, p, cost("2 G G"), sa));
    }

    // {G}{G} with two Forests should not waste Gilded Lotus's third mana.
    @Test
    public void forestsPayDoubleGreenWithoutLotus() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Gilded Lotus", p);
        addCards("Forest", 2, p);
        Card spell = addCardToZone("Slith Predator", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        AssertJUnit.assertTrue(canAutoPay(game, p, cost("G G"), sa));

        CardCollection sources = predictedManaSources(game, p, cost("G G"), sa);
        AssertJUnit.assertFalse("Gilded Lotus should not be tapped",
                sources.anyMatch(c -> "Gilded Lotus".equals(c.getName())));
        AssertJUnit.assertEquals("Both Forests should pay {G}{G}", 2,
                sources.stream().filter(c -> "Forest".equals(c.getName())).count());

        AssertJUnit.assertTrue("Production auto-pay should match feasibility",
                prodAutoPay(game, p, cost("G G"), sa));
    }

    // {2}{R} with M/P/F + Lotus should tap Lotus when Lightning Helix ({R}{W}) is still in hand.
    @Test
    public void gildedLotusUsedWhenHandNeedsMultipleColors() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Mountain", p);
        addCard("Plains", p);
        addCard("Forest", p);
        addCard("Gilded Lotus", p);
        addCardToZone("Lightning Helix", p, ZoneType.Hand);
        Card spell = addCardToZone("Homing Sliver", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        AssertJUnit.assertTrue(canAutoPay(game, p, cost("2 R"), sa));

        CardCollection sources = predictedManaSources(game, p, cost("2 R"), sa);
        AssertJUnit.assertTrue("Gilded Lotus should pay part of {2}{R}",
                sources.anyMatch(c -> "Gilded Lotus".equals(c.getName())));

        AssertJUnit.assertTrue("Production auto-pay should match feasibility",
                prodAutoPay(game, p, cost("2 R"), sa));
    }

    // {2} for Thought Vessel should not spend Selesnya Signet when Phelia ({1}{W}) is still in hand.
    @Test
    public void genericCostPreservesSignetForHandSpell() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Wastes", p);
        addCards("Forest", 2, p);
        addCard("Selesnya Signet", p);
        addCardToZone("Phelia, Exuberant Shepherd", p, ZoneType.Hand);
        Card spell = addCardToZone("Thought Vessel", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        AssertJUnit.assertTrue(canAutoPay(game, p, cost("2"), sa));

        CardCollection sources = predictedManaSources(game, p, cost("2"), sa);
        AssertJUnit.assertFalse("Signet should stay available for Phelia",
                sources.anyMatch(c -> "Selesnya Signet".equals(c.getName())));
    }

    // Lotus Petal should be reserved to activate the Signet; Signet then produces {G}{W} to pay both
    // Luminarch Aspirant's {W} and generic {1}. Spending Petal directly on {W} strands the Signet.
    @Test
    public void lotusPetalCanActivateSignetForOneWhiteCost() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Lotus Petal", p);
        addCard("Selesnya Signet", p);
        Card spell = addCardToZone("Luminarch Aspirant", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        ManaCostBeingPaid mc = cost("1 W");
        AssertJUnit.assertTrue(canAutoPay(game, p, mc, sa));

        CardCollection sources = predictedManaSources(game, p, mc, sa);
        AssertJUnit.assertTrue("Signet should pay", sources.anyMatch(c -> "Selesnya Signet".equals(c.getName())));
        AssertJUnit.assertFalse("Lotus Petal should not pay {W} directly",
                sources.size() == 1 && sources.anyMatch(c -> "Lotus Petal".equals(c.getName())));

        AssertJUnit.assertTrue("Production auto-pay should match feasibility", prodAutoPay(game, p, mc, sa));
    }

    /** Place a spell on the game stack for payment tests (player zones have no Stack). */
    private Card addSpellOnStack(Game game, String name, Player p) {
        Card spell = createCard(name, p);
        spell.setGameTimestamp(game.getNextTimestamp());
        SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        game.getStack().addAndUnfreeze(sa);
        return spell;
    }

    // Same as above when the spell is already on the stack (Luminarch Aspirant is {1}{W}).
    @Test
    public void lotusPetalActivatesSignetForOneWhiteCostOnStack() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Lotus Petal", p);
        addCard("Selesnya Signet", p);
        Card spell = addSpellOnStack(game, "Luminarch Aspirant", p);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        ManaCostBeingPaid mc = cost("1 W");
        AssertJUnit.assertTrue(canAutoPay(game, p, mc, sa));

        CardCollection sources = predictedManaSources(game, p, mc, sa);
        AssertJUnit.assertTrue("Signet should pay", sources.anyMatch(c -> "Selesnya Signet".equals(c.getName())));
        AssertJUnit.assertTrue("Lotus Petal should activate the Signet",
                sources.anyMatch(c -> "Lotus Petal".equals(c.getName())));

        AssertJUnit.assertTrue("Production auto-pay should match feasibility", prodAutoPay(game, p, mc, sa));
    }

    // {2} with imprinted Chrome Mox {G} + Study Hall {T}:{C} should not sacrifice Lotus Petal.
    @Test
    public void chromeMoxAndStudyHallPayDoubleGenericWithoutPetal() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        Card imprint = addCardToZone("Llanowar Elves", p, ZoneType.Exile);
        Card mox = addCard("Chrome Mox", p);
        mox.addImprintedCard(imprint);
        addCard("Study Hall", p);
        addCard("Lotus Petal", p);
        Card spell = addSpellOnStack(game, "Selesnya Signet", p);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        ManaCostBeingPaid mc = cost("2");
        AssertJUnit.assertTrue(canAutoPay(game, p, mc, sa));

        CardCollection sources = predictedManaSources(game, p, mc, sa);
        AssertJUnit.assertTrue("Chrome Mox should pay", sources.anyMatch(c -> "Chrome Mox".equals(c.getName())));
        AssertJUnit.assertTrue("Study Hall should pay", sources.anyMatch(c -> "Study Hall".equals(c.getName())));
        AssertJUnit.assertFalse("Lotus Petal should not be sacrificed",
                sources.anyMatch(c -> "Lotus Petal".equals(c.getName())));

        AssertJUnit.assertTrue("Production auto-pay should match feasibility", prodAutoPay(game, p, mc, sa));
    }

    // {5}{W}{W}: Signet + reusable activator should cover a {W} pip instead of sacrificing Lotus Petal.
    @Test
    public void signetBeatsLotusPetalForWhiteWithLargeGeneric() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Reliquary Tower", p);
        addCard("Selesnya Signet", p);
        addCard("Lotus Petal", p);
        addCards("Plains", 6, p);
        Card spell = addCardToZone("Elesh Norn, Grand Cenobite", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        ManaCostBeingPaid mc = cost("5 W W");
        AssertJUnit.assertTrue(canAutoPay(game, p, mc, sa));

        CardCollection sources = predictedManaSources(game, p, mc, sa);
        AssertJUnit.assertTrue("Selesnya Signet should help pay white",
                sources.anyMatch(c -> "Selesnya Signet".equals(c.getName())));
        AssertJUnit.assertFalse("Lotus Petal should be preserved",
                sources.anyMatch(c -> "Lotus Petal".equals(c.getName())));
    }

    // {1}{W}{G} with Plains + Lotus Petal + Study Hall must be payable: Plains -> {W}, Lotus Petal -> {G},
    // Study Hall {C} -> generic {1}. Using Study Hall (and its nested Plains activation) for {G} strands {1}.
    @Test
    public void filterActivationDoesNotStrandGenericPip() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Plains", p);
        addCard("Lotus Petal", p);
        addCard("Study Hall", p);
        Card spell = addCardToZone("Calix, Guided by Fate", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        ManaCostBeingPaid mc = cost("1 G W");
        AssertJUnit.assertTrue(canAutoPay(game, p, mc, sa));

        CardCollection sources = predictedManaSources(game, p, mc, sa);
        AssertJUnit.assertTrue("Plains should pay {W}", sources.anyMatch(c -> "Plains".equals(c.getName())));
        AssertJUnit.assertTrue("Lotus Petal should pay {G}", sources.anyMatch(c -> "Lotus Petal".equals(c.getName())));
        AssertJUnit.assertTrue("Study Hall should pay generic {1}", sources.anyMatch(c -> "Study Hall".equals(c.getName())));

        AssertJUnit.assertTrue("Production auto-pay should match feasibility", prodAutoPay(game, p, mc, sa));
    }

    // {1}{W}{G} with dedicated sources for each pip: Plains {W}, Forest {G}, Study Hall {T}:{C} for
    // generic {1}. Lotus Petal must not be sacrificed when reusables cover every shard.
    @Test
    public void dedicatedSourcesBeatLotusPetalForMulticolorWithGeneric() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Plains", p);
        addCard("Forest", p);
        addCard("Study Hall", p);
        addCard("Lotus Petal", p);
        Card spell = addCardToZone("Calix, Guided by Fate", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        ManaCostBeingPaid mc = cost("1 G W");
        AssertJUnit.assertTrue(canAutoPay(game, p, mc, sa));

        CardCollection sources = predictedManaSources(game, p, mc, sa);
        AssertJUnit.assertTrue("Plains should pay {W}", sources.anyMatch(c -> "Plains".equals(c.getName())));
        AssertJUnit.assertTrue("Forest should pay {G}", sources.anyMatch(c -> "Forest".equals(c.getName())));
        AssertJUnit.assertTrue("Study Hall should pay generic {1}", sources.anyMatch(c -> "Study Hall".equals(c.getName())));
        AssertJUnit.assertFalse("Lotus Petal should not be sacrificed",
                sources.anyMatch(c -> "Lotus Petal".equals(c.getName())));

        AssertJUnit.assertTrue("Production auto-pay should match feasibility", prodAutoPay(game, p, mc, sa));
    }

    // {1}{W}{G} with Arcane Signet (GW commander), Forest, Study Hall, Reliquary Tower, and Lotus Petal:
    // Signet -> {W}, Forest -> {G}, colorless land -> {1}; Petal unused.
    @Test
    public void arcaneSignetBeatsLotusPetalForMulticolorWithGeneric() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Forest", p);
        addCard("Arcane Signet", p);
        addCard("Study Hall", p);
        addCard("Reliquary Tower", p);
        addCard("Lotus Petal", p);
        Card commander = addCardToZone("Calix, Guided by Fate", p, ZoneType.Command);
        p.addCommander(commander);
        Card spell = addCardToZone("Knight of Autumn", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        ManaCostBeingPaid mc = cost("1 G W");
        AssertJUnit.assertTrue(canAutoPay(game, p, mc, sa));

        CardCollection sources = predictedManaSources(game, p, mc, sa);
        AssertJUnit.assertTrue("Arcane Signet should pay {W}", sources.anyMatch(c -> "Arcane Signet".equals(c.getName())));
        AssertJUnit.assertTrue("Forest should pay {G}", sources.anyMatch(c -> "Forest".equals(c.getName())));
        AssertJUnit.assertTrue("A colorless source should pay generic {1}",
                sources.anyMatch(c -> "Study Hall".equals(c.getName()) || "Reliquary Tower".equals(c.getName())));
        AssertJUnit.assertFalse("Lotus Petal should not be sacrificed",
                sources.anyMatch(c -> "Lotus Petal".equals(c.getName())));

        AssertJUnit.assertTrue("Production auto-pay should match feasibility", prodAutoPay(game, p, mc, sa));
    }

    // {1}{W}{G} with Arcane Signet, Forest, Study Hall ({T}:{C} is free), and Basalt Monolith:
    // Signet -> {W}, Forest -> {G}, Study Hall's free {C} -> generic {1}; not Basalt's {C}{C}{C}.
    @Test
    public void studyHallFreeColorlessBeatsBasaltForSingleGeneric() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Forest", p);
        addCard("Arcane Signet", p);
        addCard("Study Hall", p);
        addCard("Basalt Monolith", p);
        Card commander = addCardToZone("Calix, Guided by Fate", p, ZoneType.Command);
        p.addCommander(commander);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = commander.getFirstSpellAbility();
        ManaCostBeingPaid mc = cost("1 G W");
        AssertJUnit.assertTrue(canAutoPay(game, p, mc, sa));

        CardCollection sources = predictedManaSources(game, p, mc, sa);
        AssertJUnit.assertTrue("Arcane Signet should pay {W}", sources.anyMatch(c -> "Arcane Signet".equals(c.getName())));
        AssertJUnit.assertTrue("Forest should pay {G}", sources.anyMatch(c -> "Forest".equals(c.getName())));
        AssertJUnit.assertTrue("Study Hall should pay generic {1} via free {C}",
                sources.anyMatch(c -> "Study Hall".equals(c.getName())));
        AssertJUnit.assertFalse("Basalt Monolith should not pay a lone generic {1}",
                sources.anyMatch(c -> "Basalt Monolith".equals(c.getName())));

        AssertJUnit.assertTrue("Production auto-pay should match feasibility", prodAutoPay(game, p, mc, sa));
    }

    // {1}{G} with Plains + Study Hall + Lotus Petal (no Forest): sacrifice Petal for {G}, tap Plains
    // for generic {1}. Petal only wins because Study Hall's filter is a 2:1 trade that strands {1};
    // with a Forest available the disposable would stay unused.
    @Test
    public void disposablePaysColoredWhenGenericAlsoUnpaid() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Plains", p);
        addCard("Study Hall", p);
        addCard("Lotus Petal", p);
        Card spell = addCardToZone("Michelangelo, Weirdness to 11", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        ManaCostBeingPaid mc = cost("1 G");
        AssertJUnit.assertTrue(canAutoPay(game, p, mc, sa));

        CardCollection sources = predictedManaSources(game, p, mc, sa);
        AssertJUnit.assertTrue("Lotus Petal should pay {G}", sources.anyMatch(c -> "Lotus Petal".equals(c.getName())));
        AssertJUnit.assertTrue("Plains should pay generic {1} directly", sources.anyMatch(c -> "Plains".equals(c.getName())));
        AssertJUnit.assertFalse("Study Hall should be saved when Petal pays {G}",
                sources.anyMatch(c -> "Study Hall".equals(c.getName())));

        AssertJUnit.assertTrue("Production auto-pay should match feasibility", prodAutoPay(game, p, mc, sa));
    }

    // {1}{W}{G} with two Plains, Study Hall, Reliquary Tower, and Lotus Petal: Reliquary pays Study
    // Hall's filter {1} for {G}; both Plains cover {W} and generic {1}; Petal unused.
    @Test
    public void studyHallFilterWithReliquaryTowerBeatsLotusPetal() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCards("Plains", 2, p);
        addCard("Study Hall", p);
        addCard("Reliquary Tower", p);
        addCard("Lotus Petal", p);
        Card spell = addCardToZone("Calix, Guided by Fate", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        ManaCostBeingPaid mc = cost("1 G W");
        AssertJUnit.assertTrue(canAutoPay(game, p, mc, sa));

        CardCollection sources = predictedManaSources(game, p, mc, sa);
        AssertJUnit.assertTrue("Study Hall should pay {G}", sources.anyMatch(c -> "Study Hall".equals(c.getName())));
        AssertJUnit.assertTrue("Reliquary Tower should pay Study Hall's {1}",
                sources.anyMatch(c -> "Reliquary Tower".equals(c.getName())));
        AssertJUnit.assertTrue("Both Plains should be tapped",
                sources.stream().filter(c -> c.getName().contains("Plains")).count() >= 2);
        AssertJUnit.assertFalse("Lotus Petal should not be sacrificed",
                sources.anyMatch(c -> "Lotus Petal".equals(c.getName())));

        AssertJUnit.assertTrue("Production auto-pay should match feasibility", prodAutoPay(game, p, mc, sa));
    }

    // {1}{G} with Forest available: tap Forest for {G}, keep Lotus Petal unused (disposable is last resort).
    @Test
    public void forestBeatsLotusPetalWhenReusableSourceExists() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Plains", p);
        addCard("Forest", p);
        addCard("Study Hall", p);
        addCard("Lotus Petal", p);
        Card spell = addCardToZone("Michelangelo, Weirdness to 11", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        ManaCostBeingPaid mc = cost("1 G");
        AssertJUnit.assertTrue(canAutoPay(game, p, mc, sa));

        CardCollection sources = predictedManaSources(game, p, mc, sa);
        AssertJUnit.assertTrue("Forest should pay {G}", sources.anyMatch(c -> "Forest".equals(c.getName())));
        AssertJUnit.assertFalse("Lotus Petal should not be sacrificed",
                sources.anyMatch(c -> "Lotus Petal".equals(c.getName())));

        AssertJUnit.assertTrue("Production auto-pay should match feasibility", prodAutoPay(game, p, mc, sa));
    }

    // Cascade Bluffs ({U/R}{T}: Add {U}{U}, {U}{R}, or {R}{R}) with an Island paying its hybrid
    // activation cost can pay {U}{R} on its own.
    @Test
    public void comboFilterLandOnlyBaseIsFeasible() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Island", p);
        addCard("Cascade Bluffs", p);
        Card spell = addCardToZone("Goblin Electromancer", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        AssertJUnit.assertTrue(canAutoPay(game, p, cost("U R"), sa));
        AssertJUnit.assertTrue("Production auto-pay should match feasibility",
                prodAutoPay(game, p, cost("U R"), sa));
    }

    // {U}{R} with Island + Mountain + Cascade Bluffs should consolidate onto one basic + Bluffs,
    // like the Boros Signet consolidation, instead of tapping both basics.
    @Test
    public void comboFilterConsolidatesTwoColoredShards() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Island", p);
        addCard("Mountain", p);
        addCard("Cascade Bluffs", p);
        Card spell = addCardToZone("Goblin Electromancer", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        GameSimulator sim = createSimulator(game, p);
        int score = sim.simulateSpellAbility(spell.getFirstSpellAbility()).value;
        AssertJUnit.assertTrue(score > 0);
        Game simGame = sim.getSimulatedGameState();

        AssertJUnit.assertNotNull(findSpellCard(simGame, "Goblin Electromancer"));
        AssertJUnit.assertEquals("Cascade Bluffs should be tapped", 1, countTapped(simGame, "Cascade Bluffs"));
        int tappedBasics = countTapped(simGame, "Island") + countTapped(simGame, "Mountain");
        AssertJUnit.assertEquals("Only one basic should pay the Bluffs' {U/R}", 1, tappedBasics);
    }

    // Bluffs' hybrid {U/R} can be paid by either basic; with Shock ({R}) still in hand, the Island
    // should be tapped so the Mountain stays available.
    @Test
    public void comboFilterActivationPreservesHandCastability() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Island", p);
        addCard("Mountain", p);
        addCard("Cascade Bluffs", p);
        addCardToZone("Shock", p, ZoneType.Hand);
        Card spell = addCardToZone("Goblin Electromancer", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        GameSimulator sim = createSimulator(game, p);
        int score = sim.simulateSpellAbility(spell.getFirstSpellAbility()).value;
        AssertJUnit.assertTrue(score > 0);
        Game simGame = sim.getSimulatedGameState();

        AssertJUnit.assertNotNull(findSpellCard(simGame, "Goblin Electromancer"));
        AssertJUnit.assertEquals("Cascade Bluffs should be tapped", 1, countTapped(simGame, "Cascade Bluffs"));
        AssertJUnit.assertEquals("Island should pay the Bluffs' {U/R}", 1, countTapped(simGame, "Island"));
        AssertJUnit.assertEquals("Mountain should stay untapped for Shock", 0, countTapped(simGame, "Mountain"));
    }

    // {B} with only Study Hall + Lotus Petal should sacrifice the Petal directly, not route it through Study Hall.
    @Test
    public void studyHallDoesNotRouteDisposableThroughFilter() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Study Hall", p);
        addCard("Lotus Petal", p);
        Card spell = addCardToZone("Duress", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        AssertJUnit.assertTrue(canAutoPay(game, p, cost("B"), sa));

        CardCollection sources = predictedManaSources(game, p, cost("B"), sa);
        AssertJUnit.assertTrue("Lotus Petal should pay {B} directly", sources.anyMatch(c -> "Lotus Petal".equals(c.getName())));
        AssertJUnit.assertFalse("Study Hall should not be used when only a Petal can activate it",
                sources.anyMatch(c -> "Study Hall".equals(c.getName())));
    }

    // {W}{G} with Forest + Study Hall + Lotus Petal: Forest pays {G}, Petal pays {W}. Study Hall must not
    // burn Forest on its {1} activation when the disposable can cover the other colored pip.
    @Test
    public void forestAndPetalBeatStudyHallForPureMulticolor() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Snow-Covered Forest", p);
        addCard("Study Hall", p);
        addCard("Lotus Petal", p);
        Card spell = addCardToZone("Arcus Acolyte", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        ManaCostBeingPaid mc = cost("W G");
        AssertJUnit.assertTrue(canAutoPay(game, p, mc, sa));

        CardCollection sources = predictedManaSources(game, p, mc, sa);
        AssertJUnit.assertTrue("Forest should pay {G}",
                sources.anyMatch(c -> "Snow-Covered Forest".equals(c.getName())));
        AssertJUnit.assertTrue("Lotus Petal should pay {W}", sources.anyMatch(c -> "Lotus Petal".equals(c.getName())));
        AssertJUnit.assertFalse("Study Hall should not burn Forest for {W}",
                sources.anyMatch(c -> "Study Hall".equals(c.getName())));

        AssertJUnit.assertTrue("Production auto-pay should match feasibility", prodAutoPay(game, p, mc, sa));
    }

    // Only Sol Ring + Boros Signet (3 mana total): Ring {C}{C} pays Signet's {1} and leaves {C} in the pool
    // for the spell's generic; Signet pays {R}{W}. Casts a real 3 CMC spell ({1}{R}{W}).
    @Test
    public void nestedManaSurplusPaysOuterGeneric() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Sol Ring", p);
        addCard("Boros Signet", p);
        Card spell = addCardToZone("Goblin Trenches", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        AssertJUnit.assertEquals(3, spell.getManaCost().getCMC());
        ManaCostBeingPaid mc = new ManaCostBeingPaid(spell.getManaCost());

        AssertJUnit.assertTrue(canAutoPay(game, p, mc, sa));
        AssertJUnit.assertTrue("Only Ring + Signet should pay a 3 CMC spell via surplus {C}",
                prodAutoPay(game, p, mc, sa));
    }

    // TODO: Move the CantCast tests to another test suite.
    // Powerstone mana ({C}{C}, can't cast nonartifact spells) may pay artifact spells.
    @Test
    public void powerstoneManaCanPayArtifactSpell() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("The Mightstone and Weakstone", p);
        Card spell = addCardToZone("Arcane Signet", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        assertProductionPayment(game, p, cost("2"), sa);
        AssertJUnit.assertEquals(1, countTapped(game, "The Mightstone and Weakstone"));
    }

    // Powerstone mana must not pay nonartifact spells.
    @Test
    public void powerstoneManaCannotPayNonArtifactSpell() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("The Mightstone and Weakstone", p);
        Card spell = addCardToZone("Lightning Bolt", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        AssertJUnit.assertFalse(canAutoPay(game, p, cost("R"), sa));
    }

    // Many duplicate basics: payment planning must still succeed on large boards.
    @Test
    public void manyDuplicateBasicsPayMulticolor() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCards("Plains", 7, p);
        addCards("Island", 7, p);
        addCards("Swamp", 6, p);
        Card spell = addCardToZone("Dromar's Charm", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        assertProductionPayment(game, p, cost("W U B"), sa);
    }

    // Many identical signets: consolidation and nested activation must still work.
    @Test
    public void manyDuplicateSignetsPayMulticolor() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCards("Boros Signet", 10, p);
        addCard("Mountain", p);
        addCard("Plains", p);
        Card spell = addCardToZone("Lightning Helix", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        assertProductionPayment(game, p, cost("R W"), sa);

        CardCollection sources = predictedManaSources(game, p, cost("R W"), sa);
        AssertJUnit.assertTrue("A signet should pay", sources.anyMatch(c -> "Boros Signet".equals(c.getName())));
        AssertJUnit.assertTrue("At least one land should be tapped for signet activation",
                countTapped(game, "Mountain") + countTapped(game, "Plains") >= 1);
    }

    // Genju animate: with duplicate Forests, do not tap the enchanted Forest for {2}.
    @Test
    public void genjuAnimateAvoidsEnchantedForestWhenDuplicateExists() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        List<Card> forests = addCards("Forest", 20, p);
        Card enchantedForest = forests.get(0);
        Card genju = addCard("Genju of the Cedars", p);
        genju.attachToEntity(enchantedForest, null);

        SpellAbility animate = null;
        for (SpellAbility ab : genju.getSpellAbilities()) {
            if (ab.isActivatedAbility() && ab.getPayCosts() != null && ab.getPayCosts().hasManaCost()) {
                animate = ab;
                break;
            }
        }
        AssertJUnit.assertNotNull(animate);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        assertProductionPayment(game, p, cost("2"), animate);
        AssertJUnit.assertFalse("Enchanted Forest should not be tapped for {2}",
                enchantedForest.isTapped());
        AssertJUnit.assertTrue("Another Forest should be tapped", countTapped(game, "Forest") >= 1);
    }
}
