package forge.ai.controller;

import forge.ai.ComputerUtilMana;
import forge.ai.CastabilityProbe;
import forge.ai.PlayerControllerAi;
import forge.ai.simulation.GameSimulator;
import forge.ai.simulation.Plan;
import forge.ai.simulation.SimulationTest;
import forge.ai.simulation.SpellAbilityPicker;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.game.Game;
import forge.card.CardStateName;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordInterface;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class AutoPaymentTest extends SimulationTest {
    // Tests are written with the CastabilityProbe enabled.
    @BeforeMethod(alwaysRun = true)
    public void enableCastabilityProbeForPaymentTests() {
        CastabilityProbe.enableForTests();
    }

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

    private int countOnBattlefield(Game game, String name) {
        int i = 0;
        for (Card c : game.getCardsIn(ZoneType.Battlefield)) {
            if (c.getName().equals(name)) {
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
    public void trevasAttendantBeforeAshnodsAltar() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Forest", p);
        addCard("Treva's Attendant", p);
        addCard("Ashnod's Altar", p);
        Card elf = addCard("Llanowar Elves", p);
        elf.setSickness(false);
        Card spell = addCardToZone("Mind Stone", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        assertProductionPayment(game, p, cost("2"), spell.getFirstSpellAbility());
        AssertJUnit.assertEquals(0, countOnBattlefield(game, "Treva's Attendant"));
        AssertJUnit.assertEquals(1, countOnBattlefield(game, "Llanowar Elves"));
        AssertJUnit.assertEquals(1, countOnBattlefield(game, "Ashnod's Altar"));
    }

    // Springleaf Drum taps another creature, so a plain land should be preferred for a colored pip.
    @Test
    public void springleafDrumAfterForestBeforeTreasure() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Forest", p);
        addCard("Springleaf Drum", p);
        addToken("c_a_treasure_sac", p);
        Card elf = addCard("Llanowar Elves", p);
        elf.setSickness(false);
        Card spell = addCardToZone("Grizzly Bears", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        assertProductionPayment(game, p, cost("G"), spell.getFirstSpellAbility());
        AssertJUnit.assertEquals("Forest should pay {G}", 1, countTapped(game, "Forest"));
        AssertJUnit.assertEquals("Springleaf Drum should stay untapped", 0, countTapped(game, "Springleaf Drum"));
        AssertJUnit.assertEquals("Treasure should stay unused", 1, countOnBattlefield(game, "Treasure Token"));
    }

    // Without a land, Springleaf Drum (reusable but taps a creature) should still beat sacrificing a Treasure.
    @Test
    public void springleafDrumBeforeTreasureWhenNoLand() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Springleaf Drum", p);
        addToken("c_a_treasure_sac", p);
        Card bear = addCard("Bear Cub", p);
        bear.setSickness(false);
        Card spell = addCardToZone("Grizzly Bears", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        assertProductionPayment(game, p, cost("G"), spell.getFirstSpellAbility());
        AssertJUnit.assertEquals("Springleaf Drum should pay {G}", 1, countTapped(game, "Springleaf Drum"));
        AssertJUnit.assertEquals("Treasure should stay on the battlefield", 1, countOnBattlefield(game, "Treasure Token"));
    }

    @Test
    public void yunaGrandSummonPaysCreatureWithYuna() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        Card yuna = addCard("Yuna, Grand Summoner", p);
        yuna.setSickness(false);
        addCards("Forest", 4, p);
        Card spell = addCardToZone("Grizzly Bears", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        assertProductionPayment(game, p, cost("1 G"), sa);
        AssertJUnit.assertEquals(1, countTapped(game, "Yuna, Grand Summoner"));
        AssertJUnit.assertEquals("A single Forest could pay alone; Yuna should be preferred",
                1, countTapped(game, "Forest"));
        AssertJUnit.assertEquals(4, countOnBattlefield(game, "Forest"));
    }

    @Test
    public void jadeOrbPrefersOrbForDragon() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Jade Orb of Dragonkind", p);
        addCard("Forest", p);
        addCards("Mountain", 4, p);
        Card spell = addCardToZone("Swift Warkite", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        CardCollection sources = predictedManaSources(game, p, cost("4 R G"), sa);
        AssertJUnit.assertTrue("Jade Orb should pay the {G} for a Dragon",
                sources.anyMatch(c -> "Jade Orb of Dragonkind".equals(c.getName())));
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

    // {G} with Forest + Signpost Scarecrow ({2}: any): tap the Forest, not the expensive filter.
    @Test
    public void skipsSignpostScarecrowWhenDirectSourceExists() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Forest", p);
        addCard("Signpost Scarecrow", p);
        Card spell = addCardToZone("Grizzly Bears", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        assertProductionPayment(game, p, cost("G"), spell.getFirstSpellAbility());
        AssertJUnit.assertEquals("Forest should be tapped for {G}", 1, countTapped(game, "Forest"));
        AssertJUnit.assertEquals("Signpost Scarecrow should not be used", 0, countTapped(game, "Signpost Scarecrow"));
    }

    // {G} with Forest + Prismite ({2}: any): same as Signpost — direct basic wins.
    @Test
    public void skipsPrismiteWhenDirectSourceExists() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Forest", p);
        addCard("Prismite", p);
        Card spell = addCardToZone("Grizzly Bears", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        assertProductionPayment(game, p, cost("G"), spell.getFirstSpellAbility());
        AssertJUnit.assertEquals("Forest should be tapped for {G}", 1, countTapped(game, "Forest"));
        AssertJUnit.assertEquals("Prismite should not be used", 0, countTapped(game, "Prismite"));
    }

    // {R} with Study Hall ({1}: any) + Signpost ({2}: any) + one Plains: prefer the cheaper filter.
    @Test
    public void prefersStudyHallOverSignpostForOffColor() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Study Hall", p);
        addCard("Signpost Scarecrow", p);
        addCard("Plains", p);
        Card spell = addCardToZone("Shock", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        assertProductionPayment(game, p, cost("R"), spell.getFirstSpellAbility());
        AssertJUnit.assertEquals("Study Hall should pay {R}", 1, countTapped(game, "Study Hall"));
        AssertJUnit.assertEquals("Signpost Scarecrow should stay unused", 0, countTapped(game, "Signpost Scarecrow"));
    }

    // {W} with Plains + Karakas: tap Plains; keep Karakas for its bounce ability.
    @Test
    public void karakasReservedWhenPlainsAvailable() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Plains", p);
        addCard("Karakas", p);
        Card spell = addCardToZone("Healing Salve", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        assertProductionPayment(game, p, cost("W"), sa);

        AssertJUnit.assertEquals("Plains should be tapped for W", 1, countTapped(game, "Plains"));
        AssertJUnit.assertEquals("Karakas should stay untapped", 0, countTapped(game, "Karakas"));
    }

    // {W} with only Karakas: still payable — reserve is soft depriorization, not a hard block.
    @Test
    public void karakasTappedWhenOnlyWhiteSource() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Karakas", p);
        Card spell = addCardToZone("Healing Salve", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        assertProductionPayment(game, p, cost("W"), sa);

        AssertJUnit.assertEquals("Karakas should be tapped when it is the only source", 1, countTapped(game, "Karakas"));
    }

    // Generic {1} with Mind Stone + Library of Alexandria: spend the rock, not the draw land.
    @Test
    public void libraryReservedWhenColorlessRockAvailable() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Mind Stone", p);
        addCard("Library of Alexandria", p);
        Card spell = addCardToZone("Expedition Map", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        CardCollection sources = predictedManaSources(game, p, cost("1"), sa);
        AssertJUnit.assertTrue("Mind Stone should pay generic {1}", sources.anyMatch(c -> "Mind Stone".equals(c.getName())));
        AssertJUnit.assertFalse("Library should not pay generic {1}",
                sources.anyMatch(c -> "Library of Alexandria".equals(c.getName())));

        assertProductionPayment(game, p, cost("1"), sa);
        AssertJUnit.assertEquals("Mind Stone should be tapped", 1, countTapped(game, "Mind Stone"));
        AssertJUnit.assertEquals("Library should stay untapped", 0, countTapped(game, "Library of Alexandria"));
    }

    // {2} with Goldspan Treasure (2 mana) + Lotus Petal (1): sacrifice the Treasure, keep the Petal.
    @Test
    public void goldspanTreasurePaysDoubleGenericOverSingleManaDisposable() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Goldspan Dragon", p);
        addToken("c_a_treasure_sac", p);
        addCard("Lotus Petal", p);
        Card spell = addCardToZone("Darksteel Pendant", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        assertProductionPayment(game, p, cost("2"), sa);

        AssertJUnit.assertEquals("Lotus Petal should stay on the battlefield", 1, countOnBattlefield(game, "Lotus Petal"));
        AssertJUnit.assertEquals("Treasure should be sacrificed for {2}", 0, countOnBattlefield(game, "Treasure Token"));
    }

    // {R}{R} with Goldspan + one Treasure: one activation pays both red pips.
    @Test
    public void goldspanTreasurePaysDoubleRedInOneActivation() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Goldspan Dragon", p);
        addToken("c_a_treasure_sac", p);
        Card spell = addCardToZone("Doublecast", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        assertProductionPayment(game, p, cost("R R"), sa);

        AssertJUnit.assertEquals("Only one Treasure should be sacrificed", 0, countOnBattlefield(game, "Treasure Token"));
    }

    // Reusable basics still beat Goldspan Treasures for a single colored pip.
    @Test
    public void reusableStillBeatsGoldspanTreasure() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Goldspan Dragon", p);
        addToken("c_a_treasure_sac", p);
        addCard("Forest", p);
        Card spell = addCardToZone("Grizzly Bears", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        assertProductionPayment(game, p, cost("G"), sa);

        AssertJUnit.assertEquals("Forest should be tapped for {G}", 1, countTapped(game, "Forest"));
        AssertJUnit.assertEquals("Treasure should stay unused", 1, countOnBattlefield(game, "Treasure Token"));
    }

    // Signet {1} nested activation: Lotus Petal beats Goldspan Treasure (don't waste 2-mana disposable).
    @Test
    public void petalPreferredOverGoldspanTreasureForSignetActivation() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Goldspan Dragon", p);
        addToken("c_a_treasure_sac", p);
        addCard("Lotus Petal", p);
        addCard("Boros Signet", p);
        Card spell = addCardToZone("Lightning Helix", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        ManaCostBeingPaid mc = cost("R W");
        AssertJUnit.assertTrue(canAutoPay(game, p, mc, sa));

        CardCollection sources = predictedManaSources(game, p, mc, sa);
        AssertJUnit.assertTrue("Lotus Petal should activate the Signet",
                sources.anyMatch(c -> "Lotus Petal".equals(c.getName())));
        AssertJUnit.assertFalse("Goldspan Treasure should not pay {R}{W} directly",
                sources.size() == 1 && sources.anyMatch(c -> "Treasure Token".equals(c.getName())));

        assertProductionPayment(game, p, mc, sa);
        AssertJUnit.assertEquals("Treasure should stay unused", 1, countOnBattlefield(game, "Treasure Token"));
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

    // Castability probe: no red sources — skip dry-runs for hand spells that need {R}.
    @Test
    public void castabilityProbeSkipsRedDependentsWhenNoRed() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Plains", p);
        addCard("Forest", p);
        addCard("Llanowar Elves", p);
        addCard("Study Hall", p);
        addCardToZone("Shock", p, ZoneType.Hand);
        addCardToZone("Shock", p, ZoneType.Hand);
        addCardToZone("Shock", p, ZoneType.Hand);
        Card spell = addCardToZone("Healing Salve", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        AssertJUnit.assertTrue(canAutoPay(game, p, cost("W"), sa));
        final int dryRuns = castabilityProbeDryRunsForPaymentPrompt(game, p, cost("W"), sa);
        AssertJUnit.assertEquals("Red spells should be pruned without nested dry-runs", 0, dryRuns);
        assertProductionPayment(game, p, cost("W"), sa);
    }

    // Castability probe: one Mountain — both {R} spells still get full dry-runs (no quantity over-prune).
    @Test
    public void castabilityProbeDoesNotSkipWhenOneRedRemains() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Mountain", p);
        addCard("Plains", p);
        addCard("Boros Signet", p);
        addCardToZone("Shock", p, ZoneType.Hand);
        addCardToZone("Shock", p, ZoneType.Hand);
        Card spell = addCardToZone("Lightning Helix", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        AssertJUnit.assertTrue(canAutoPay(game, p, cost("R W"), sa));
        final int dryRuns = castabilityProbeDryRunsForPaymentPrompt(game, p, cost("R W"), sa);
        AssertJUnit.assertTrue("Both red spells should still be probed when {R} remains available", dryRuns >= 2);
        assertProductionPayment(game, p, cost("R W"), sa);
    }

    // Soft CMC cap: skip dry-run for 5-drop when total mana < CMC; still probe low-CMC spells.
    @Test
    public void castabilityProbeSoftCmcCapSkipsWhenTotalManaInsufficient() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Mountain", p);
        addCard("Plains", p);
        addCard("Boros Signet", p);
        addCardToZone("Air Elemental", p, ZoneType.Hand);
        addCardToZone("Divine Favor", p, ZoneType.Hand);
        Card spell = addCardToZone("Lightning Helix", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        AssertJUnit.assertTrue(canAutoPay(game, p, cost("R W"), sa));
        final int dryRuns = castabilityProbeDryRunsForPaymentPrompt(game, p, cost("R W"), sa);
        AssertJUnit.assertTrue("High-CMC spell should be skipped; low-CMC spell still probed", dryRuns >= 1);
        assertProductionPayment(game, p, cost("R W"), sa);
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
        p.runWithController(() -> sources[0] = ComputerUtilMana.getManaSourcesToPayCostForPaymentPrompt(mc, sa, p, false),
                new PlayerControllerAi(game, p, p.getOriginalLobbyPlayer()));
        return sources[0];
    }

    /** Runs payment-prompt preview and returns castability nested dry-run count (see {@link ComputerUtilMana}). */
    private int castabilityProbeDryRunsForPaymentPrompt(Game game, Player p, ManaCostBeingPaid mc, SpellAbility sa) {
        final int[] count = new int[1];
        p.runWithController(() -> {
            ComputerUtilMana.resetCastabilityProbeDryRunCountForTests();
            ComputerUtilMana.getManaSourcesToPayCostForPaymentPrompt(mc, sa, p, false);
            count[0] = ComputerUtilMana.getCastabilityProbeDryRunCountForTests();
        }, new PlayerControllerAi(game, p, p.getOriginalLobbyPlayer()));
        return count[0];
    }

    private SpellAbility findEquipAbility(final Card equipment) {
        for (KeywordInterface kw : equipment.getKeywords(Keyword.EQUIP)) {
            for (SpellAbility sa : kw.getAbilities()) {
                if (sa.isEquip()) {
                    return sa;
                }
            }
        }
        return null;
    }

    private String capturePaymentPlan(Game game, Player p, ManaCostBeingPaid mc, SpellAbility sa, final boolean test) {
        final String prevPlan = System.getProperty("forge.debugManaPayment.plan");
        final PrintStream prevOut = System.out;
        final ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try {
            System.setProperty("forge.debugManaPayment.plan", "true");
            System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
            if (test) {
                final CardCollection[] sources = new CardCollection[1];
                p.runWithController(() -> sources[0] = ComputerUtilMana.getManaSourcesToPayCostForPaymentPrompt(
                        new ManaCostBeingPaid(mc), sa, p, false),
                        new PlayerControllerAi(game, p, p.getOriginalLobbyPlayer()));
                AssertJUnit.assertNotNull(sources[0]);
            } else {
                final boolean[] result = new boolean[1];
                p.runWithController(() -> result[0] = ComputerUtilMana.payManaCostFromPaymentPrompt(
                        new ManaCostBeingPaid(mc), sa, p, false),
                        new PlayerControllerAi(game, p, p.getOriginalLobbyPlayer()));
                AssertJUnit.assertTrue(result[0]);
            }
            return captured.toString(StandardCharsets.UTF_8);
        } finally {
            System.setOut(prevOut);
            if (prevPlan == null) {
                System.clearProperty("forge.debugManaPayment.plan");
            } else {
                System.setProperty("forge.debugManaPayment.plan", prevPlan);
            }
        }
    }

    private List<String> extractPaymentPlanSteps(final String log, final boolean test) {
        final String marker = "MANA_PAYMENT_PLAN [" + (test ? "test" : "prod") + "]";
        final int start = log.indexOf(marker);
        if (start < 0) {
            return List.of();
        }
        final List<String> steps = Lists.newArrayList();
        final String[] lines = log.substring(start).split("\\R");
        for (int i = 1; i < lines.length; i++) {
            final String line = lines[i].trim();
            if (line.isEmpty()) {
                break;
            }
            if (!line.matches("\\d+\\. .*")) {
                break;
            }
            steps.add(line.replaceFirst("^\\d+\\. ", ""));
        }
        return steps;
    }

    /** Cards added directly to a zone skip ETB; register triggers for TapsForMana auras/enchantments. */
    private void registerBattlefieldTriggers(Game game, Card... cards) {
        for (final Card c : cards) {
            if (c != null) {
                game.getTriggerHandler().registerActiveTrigger(c, false);
            }
        }
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

    // {W} with tapped Plains: Forest pays Study Hall's {1}, not Lotus Petal.
    @Test
    public void studyHallBeatsLotusPetalWhenPlainsTapped() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        Card plains = addCard("Plains", p);
        plains.setTapped(true);
        Card prairie = addCard("Sungrass Prairie", p);
        prairie.setTapped(true);
        Card petal2 = addCard("Lotus Petal", p);
        petal2.setTapped(true);
        addCard("Forest", p);
        addCard("Study Hall", p);
        addCard("Lotus Petal", p);
        addCardToZone("Healing Salve", p, ZoneType.Hand);
        Card spell = addSpellOnStack(game, "Speaker of the Heavens", p);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        ManaCostBeingPaid mc = cost("W");
        AssertJUnit.assertTrue(canAutoPay(game, p, mc, sa));

        CardCollection sources = predictedManaSources(game, p, mc, sa);
        AssertJUnit.assertTrue("Study Hall should produce {W}", sources.anyMatch(c -> "Study Hall".equals(c.getName())));
        AssertJUnit.assertTrue("Forest should pay Study Hall's {1}", sources.anyMatch(c -> "Forest".equals(c.getName())));
        AssertJUnit.assertFalse("Lotus Petal should not be sacrificed", sources.anyMatch(c -> "Lotus Petal".equals(c.getName())));

        AssertJUnit.assertTrue("Production auto-pay should match feasibility", prodAutoPay(game, p, mc, sa));
    }

    // {1}{W} on stack: Mind Stone activates Sungrass Prairie; Study Hall filter stays for later.
    @Test
    public void shelteredByGhostsUsesMindStoneNotStudyHallForPrairie() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Mind Stone", p);
        addCard("Study Hall", p);
        addCard("Sungrass Prairie", p);
        Card spell = addSpellOnStack(game, "Sheltered by Ghosts", p);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        ManaCostBeingPaid mc = cost("1 W");
        AssertJUnit.assertTrue(canAutoPay(game, p, mc, sa));

        CardCollection sources = predictedManaSources(game, p, mc, sa);
        AssertJUnit.assertTrue("Sungrass Prairie should pay {1}{W}",
                sources.anyMatch(c -> "Sungrass Prairie".equals(c.getName())));
        AssertJUnit.assertTrue("Mind Stone should activate Sungrass Prairie",
                sources.anyMatch(c -> "Mind Stone".equals(c.getName())));
        AssertJUnit.assertFalse("Study Hall should not be tapped for Prairie's {1}",
                sources.anyMatch(c -> "Study Hall".equals(c.getName())));

        final String testLog = capturePaymentPlan(game, p, mc, sa, true);
        final String prodLog = capturePaymentPlan(game, p, mc, sa, false);
        final List<String> testSteps = extractPaymentPlanSteps(testLog, true);
        final List<String> prodSteps = extractPaymentPlanSteps(prodLog, false);
        AssertJUnit.assertFalse("Test plan should not be empty", testSteps.isEmpty());
        AssertJUnit.assertFalse("Prod plan should not be empty", prodSteps.isEmpty());
        AssertJUnit.assertEquals("Preview and production plans should match", prodSteps, testSteps);

        AssertJUnit.assertTrue("Production auto-pay should match feasibility", prodAutoPay(game, p, mc, sa));
    }

    // {1}{W} on stack: preview should match prod (Haven for generic, not off-color Forest).
    @Test
    public void ainokBondKinStackPreviewMatchesProduction() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        Card lair = addCard("Valgavoth's Lair", p);
        lair.setChosenColors(Lists.newArrayList("white"));
        addCard("Snow-Covered Forest", p);
        Card haven = addCard("Strength of the Harvest", p);
        haven.setState(CardStateName.Backside, false, true);
        haven.setTapped(false);
        addCardToZone("Healing Salve", p, ZoneType.Hand);
        Card spell = addSpellOnStack(game, "Ainok Bond-Kin", p);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        ManaCostBeingPaid mc = cost("1 W");
        AssertJUnit.assertTrue(canAutoPay(game, p, mc, sa));

        CardCollection sources = predictedManaSources(game, p, mc, sa);
        AssertJUnit.assertTrue("Valgavoth's Lair should pay {W}",
                sources.anyMatch(c -> "Valgavoth's Lair".equals(c.getName())));
        AssertJUnit.assertTrue("Haven of the Harvest should pay generic {1}",
                sources.anyMatch(c -> "Haven of the Harvest".equals(c.getName())));
        AssertJUnit.assertFalse("Forest should not pay generic {1}",
                sources.anyMatch(c -> "Snow-Covered Forest".equals(c.getName())));

        final String testLog = capturePaymentPlan(game, p, mc, sa, true);
        final String prodLog = capturePaymentPlan(game, p, mc, sa, false);
        AssertJUnit.assertEquals("Preview and production plans should match",
                extractPaymentPlanSteps(prodLog, false), extractPaymentPlanSteps(testLog, true));

        AssertJUnit.assertTrue("Production auto-pay should match feasibility", prodAutoPay(game, p, mc, sa));
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

    // {2} with Sungrass Prairie + Study Hall should still consolidate when Eternal Witness ({1}{G}{G})
    // is in hand — Forest covers {G}, so Prairie must not be reserved for Witness.
    @Test
    public void sungrassPrairieConsolidatesDespiteMonoColorHandSpell() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Snow-Covered Forest", p);
        addCard("Study Hall", p);
        addCard("Sungrass Prairie", p);
        addCard("Lotus Petal", p);
        addCardToZone("Eternal Witness", p, ZoneType.Hand);
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
        AssertJUnit.assertFalse("Lotus Petal should not be sacrificed for {2}",
                sources.anyMatch(c -> "Lotus Petal".equals(c.getName())));

        AssertJUnit.assertTrue("Production auto-pay should match feasibility",
                prodAutoPay(game, p, cost("2"), sa));
    }

    // Same as above without a Forest: Witness's {G} can come from Lotus Petal, so Prairie still pays {2}.
    @Test
    public void sungrassPrairieConsolidatesWhenHandGreenCoveredByPetal() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Study Hall", p);
        addCard("Sungrass Prairie", p);
        addCard("Lotus Petal", p);
        addCardToZone("Eternal Witness", p, ZoneType.Hand);
        Card spell = addCardToZone("Shadowspear", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        AssertJUnit.assertTrue(canAutoPay(game, p, cost("2"), sa));

        CardCollection sources = predictedManaSources(game, p, cost("2"), sa);
        AssertJUnit.assertTrue("Sungrass Prairie should pay {2}",
                sources.anyMatch(c -> "Sungrass Prairie".equals(c.getName())));
        AssertJUnit.assertFalse("Lotus Petal should not be sacrificed for {2}",
                sources.anyMatch(c -> "Lotus Petal".equals(c.getName())));
    }

    // Casting {2} on stack: one colorless rock + one colored basic, not two colored basics; test == prod.
    @Test
    public void doubleGenericUsesColorlessRockNotSecondColoredBasic() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Forest", p);
        addCard("Reliquary Tower", p);
        addCards("Snow-Covered Forest", 1, p);
        addCardToZone("Thought-Knot Seer", p, ZoneType.Hand);
        Card spell = addSpellOnStack(game, "Fellwar Stone", p);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        ManaCostBeingPaid mc = cost("2");
        AssertJUnit.assertTrue(canAutoPay(game, p, mc, sa));

        CardCollection sources = predictedManaSources(game, p, mc, sa);
        AssertJUnit.assertTrue("Reliquary Tower should pay generic {1}",
                sources.anyMatch(c -> "Reliquary Tower".equals(c.getName())));
        AssertJUnit.assertEquals("Should tap exactly two sources for {2}", 2, sources.size());
        AssertJUnit.assertFalse("Should not tap two colored basics for {2}",
                sources.stream().filter(c -> c.getName().contains("Forest")).count() >= 2);

        AssertJUnit.assertTrue("Production auto-pay should match preview", prodAutoPay(game, p, mc, sa));
    }

    // {2} Mind Stone on stack: Snow-Covered Forest + Study Hall {T}:{C}, not two colored basics; test == prod.
    @Test
    public void stackMindStoneUsesStudyHallNotSecondForestForGeneric() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Snow-Covered Forest", p);
        addCard("Forest", p);
        addCard("Study Hall", p);
        Card spell = addSpellOnStack(game, "Mind Stone", p);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        ManaCostBeingPaid mc = cost("2");
        AssertJUnit.assertTrue(canAutoPay(game, p, new ManaCostBeingPaid(mc), sa));

        CardCollection sources = predictedManaSources(game, p, new ManaCostBeingPaid(mc), sa);
        AssertJUnit.assertTrue("Study Hall should pay generic {1}",
                sources.anyMatch(c -> "Study Hall".equals(c.getName())));
        AssertJUnit.assertEquals("Should tap exactly two sources for {2}", 2, sources.size());
        AssertJUnit.assertFalse("Should not tap two colored basics for {2}",
                sources.stream().filter(c -> c.getName().contains("Forest")).count() >= 2);

        AssertJUnit.assertTrue("Production auto-pay should match preview", prodAutoPay(game, p, new ManaCostBeingPaid(mc), sa));
        AssertJUnit.assertEquals("Study Hall should be tapped in production", 1, countTapped(game, "Study Hall"));
        AssertJUnit.assertEquals("Only one Forest should be tapped in production", 1,
                countTapped(game, "Snow-Covered Forest") + countTapped(game, "Forest"));
    }

    // Equip {1}: payment-prompt preview and Auto commit both emit MANA_PAYMENT_PLAN tagged [equip].
    @Test
    public void equipAbilityEmitsPaymentPromptPlans() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Plains", p);
        Card bear = addCard("Runeclaw Bear", p);
        bear.setSickness(false);
        Card equipment = addCard("Bonesplitter", p);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility equipSa = findEquipAbility(equipment);
        AssertJUnit.assertNotNull(equipSa);
        equipSa.setActivatingPlayer(p);
        equipSa.getTargets().add(bear);

        ManaCostBeingPaid mc = new ManaCostBeingPaid(equipSa.getPayCosts().getCostMana().getMana());

        final String testLog = capturePaymentPlan(game, p, mc, equipSa, true);
        AssertJUnit.assertTrue("Test plan should be emitted for equip", testLog.contains("MANA_PAYMENT_PLAN [test]"));
        AssertJUnit.assertTrue("Test plan should tag equip abilities", testLog.contains("Bonesplitter [equip]"));

        final String prodLog = capturePaymentPlan(game, p, mc, equipSa, false);
        AssertJUnit.assertTrue("Prod plan should be emitted for equip", prodLog.contains("MANA_PAYMENT_PLAN [prod]"));
        AssertJUnit.assertTrue("Prod plan should tag equip abilities", prodLog.contains("Bonesplitter [equip]"));

        final List<String> testSteps = extractPaymentPlanSteps(testLog, true);
        final List<String> prodSteps = extractPaymentPlanSteps(prodLog, false);
        AssertJUnit.assertFalse("Test plan should list tap steps", testSteps.isEmpty());
        AssertJUnit.assertEquals("Prod plan steps should match test", testSteps, prodSteps);
    }

    // Companion {3}: ST$ put-into-hand is AbilityStatic; preview and Auto commit emit [companion] plans.
    @Test
    public void companionAbilityEmitsPaymentPromptPlans() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Plains", p);
        addCard("Island", p);
        addCard("Swamp", p);
        Card companion = addCardToZone("Grizzly Bears", p, ZoneType.Command);
        p.getZone(ZoneType.Command).add(Player.createCompanionEffect(companion));

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility companionSa = null;
        for (final SpellAbility sa : companion.getNonManaAbilities()) {
            final String desc = sa.getDescription();
            if (desc != null && desc.contains("Companion")
                    && sa.getPayCosts() != null && sa.getPayCosts().hasManaCost()) {
                companionSa = sa;
                break;
            }
        }
        AssertJUnit.assertNotNull("Companion put-into-hand ability should be granted", companionSa);
        companionSa.setActivatingPlayer(p);

        final ManaCostBeingPaid mc = new ManaCostBeingPaid(companionSa.getPayCosts().getCostMana().getMana());
        AssertJUnit.assertTrue(canAutoPay(game, p, mc, companionSa));

        final String testLog = capturePaymentPlan(game, p, mc, companionSa, true);
        AssertJUnit.assertTrue("Test plan should be emitted for companion", testLog.contains("MANA_PAYMENT_PLAN [test]"));
        AssertJUnit.assertTrue("Test plan should tag companion abilities", testLog.contains("Grizzly Bears [companion]"));

        final String prodLog = capturePaymentPlan(game, p, mc, companionSa, false);
        AssertJUnit.assertTrue("Prod plan should be emitted for companion", prodLog.contains("MANA_PAYMENT_PLAN [prod]"));
        AssertJUnit.assertTrue("Prod plan should tag companion abilities", prodLog.contains("Grizzly Bears [companion]"));

        final List<String> testSteps = extractPaymentPlanSteps(testLog, true);
        final List<String> prodSteps = extractPaymentPlanSteps(prodLog, false);
        AssertJUnit.assertFalse("Test plan should list tap steps", testSteps.isEmpty());
        AssertJUnit.assertEquals("Prod plan steps should match test", testSteps, prodSteps);
    }

    // {1}{W}{G} on stack: Plains {W}, Forest {G}, Reliquary Tower {C} for generic — not Sol Ring.
    @Test
    public void stackCalixUsesReliquaryTowerNotSolRingForGeneric() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Plains", p);
        addCard("Forest", p);
        addCard("Reliquary Tower", p);
        addCard("Sol Ring", p);
        Card spell = addSpellOnStack(game, "Calix, Guided by Fate", p);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        ManaCostBeingPaid mc = cost("1 W G");
        AssertJUnit.assertTrue(canAutoPay(game, p, mc, sa));

        CardCollection sources = predictedManaSources(game, p, mc, sa);
        AssertJUnit.assertTrue("Plains should pay {W}", sources.anyMatch(c -> "Plains".equals(c.getName())));
        AssertJUnit.assertTrue("Forest should pay {G}", sources.anyMatch(c -> "Forest".equals(c.getName())));
        AssertJUnit.assertTrue("Reliquary Tower should pay generic {1}",
                sources.anyMatch(c -> "Reliquary Tower".equals(c.getName())));
        AssertJUnit.assertFalse("Sol Ring should not pay generic {1} when Reliquary Tower can",
                sources.anyMatch(c -> "Sol Ring".equals(c.getName())));

        AssertJUnit.assertTrue("Production auto-pay should match feasibility", prodAutoPay(game, p, mc, sa));
    }

    // {2}{G} should tap Sol Ring once for {2}, not Reliquary Tower + Sol Ring.
    @Test
    public void solRingPaysDoubleGenericWithColoredPip() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Snow-Covered Forest", p);
        addCard("Reliquary Tower", p);
        addCard("Sol Ring", p);
        Card spell = addCardToZone("Chomping Changeling", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        AssertJUnit.assertTrue(canAutoPay(game, p, cost("2 G"), sa));

        CardCollection sources = predictedManaSources(game, p, cost("2 G"), sa);
        AssertJUnit.assertTrue("Forest should pay {G}",
                sources.anyMatch(c -> c.getName().contains("Forest")));
        AssertJUnit.assertTrue("Sol Ring should pay {2}", sources.anyMatch(c -> "Sol Ring".equals(c.getName())));
        AssertJUnit.assertFalse("Reliquary Tower should not pay generic when Sol Ring covers {2}",
                sources.anyMatch(c -> "Reliquary Tower".equals(c.getName())));

        AssertJUnit.assertTrue("Production auto-pay should match feasibility",
                prodAutoPay(game, p, cost("2 G"), sa));
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

    // Jacked Rabbit is {X}{1}{W}; X=0 matches the Petal + Signet routing case on stack.
    @Test
    public void lotusPetalActivatesSignetForJackedRabbitOnStack() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Lotus Petal", p);
        addCard("Selesnya Signet", p);
        Card spell = addSpellOnStack(game, "Jacked Rabbit", p);

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

    // {1}{W} on stack: preview and production both tap the on-color dual for generic (no castability probe).
    @Test
    public void stackPaymentUsesOffColorRockForGeneric() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        addCard("Swamp", opp);
        addCard("Snow-Covered Plains", p);
        addCard("Razorverge Thicket", p);
        addCard("Fellwar Stone", p);
        addCardToZone("Healing Salve", p, ZoneType.Hand);
        Card spell = addSpellOnStack(game, "Luminarch Aspirant", p);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        ManaCostBeingPaid mc = cost("1 W");
        AssertJUnit.assertTrue(canAutoPay(game, p, mc, sa));

        CardCollection sources = predictedManaSources(game, p, mc, sa);
        AssertJUnit.assertTrue("Plains should pay {W}",
                sources.anyMatch(c -> c.getName().contains("Plains")));
        AssertJUnit.assertTrue("Razorverge Thicket should pay generic {1}",
                sources.anyMatch(c -> "Razorverge Thicket".equals(c.getName())));

        final String testLog = capturePaymentPlan(game, p, mc, sa, true);
        final String prodLog = capturePaymentPlan(game, p, mc, sa, false);
        AssertJUnit.assertEquals("Preview and production plans should match",
                extractPaymentPlanSteps(prodLog, false), extractPaymentPlanSteps(testLog, true));

        AssertJUnit.assertTrue("Production auto-pay should match feasibility", prodAutoPay(game, p, mc, sa));
    }

    // Tapped land must not block Petal from activating Signet when the land cannot actually pay {1}.
    @Test
    public void lotusPetalActivatesSignetWhenOnlyLandIsTapped() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        Card plains = addCard("Plains", p);
        plains.setTapped(true);
        addCard("Lotus Petal", p);
        addCard("Selesnya Signet", p);
        Card spell = addSpellOnStack(game, "Jacked Rabbit", p);

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

    // Two Plains + Study Hall + Petal on stack: Petal {G}, one Plains {W}, Study Hall {C} for {1}.
    @Test
    public void calixOnStackDoesNotBurnStudyHallForGreenWithTwoPlains() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Plains", p);
        addCard("Snow-Covered Plains", p);
        addCard("Lotus Petal", p);
        addCard("Study Hall", p);
        Card spell = addSpellOnStack(game, "Calix, Guided by Fate", p);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        ManaCostBeingPaid mc = cost("1 G W");
        AssertJUnit.assertTrue(canAutoPay(game, p, mc, sa));

        CardCollection sources = predictedManaSources(game, p, mc, sa);
        AssertJUnit.assertTrue("Lotus Petal should pay {G}", sources.anyMatch(c -> "Lotus Petal".equals(c.getName())));
        AssertJUnit.assertTrue("Study Hall should pay generic {1}", sources.anyMatch(c -> "Study Hall".equals(c.getName())));
        AssertJUnit.assertEquals("Only one Plains should be tapped", 1,
                sources.stream().filter(c -> c.getName().contains("Plains")).count());

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

    // {1}{W}{G} with two Plains, Study Hall, Reliquary Tower, and Lotus Petal: Petal -> {G}, one Plains
    // -> {W}, Reliquary Tower {C} -> generic {1}; Study Hall stays untapped for its filter ability.
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
        AssertJUnit.assertTrue("Lotus Petal should pay {G}", sources.anyMatch(c -> "Lotus Petal".equals(c.getName())));
        AssertJUnit.assertTrue("Reliquary Tower should pay generic {1}",
                sources.anyMatch(c -> "Reliquary Tower".equals(c.getName())));
        AssertJUnit.assertEquals("Only one Plains should be tapped", 1,
                sources.stream().filter(c -> c.getName().contains("Plains")).count());
        AssertJUnit.assertFalse("Study Hall should stay untapped when Reliquary Tower can pay generic {1}",
                sources.anyMatch(c -> "Study Hall".equals(c.getName())));

        AssertJUnit.assertTrue("Production auto-pay should match feasibility", prodAutoPay(game, p, mc, sa));
    }

    // Forest + Utopia Sprawl (chosen blue): one tap produces {G}{U} via TapsForMana trigger simulation.
    @Test
    public void utopiaSprawlPaysGreenAndChosenColorFromOneForestTap() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        Card forest = addCard("Forest", p);
        Card sprawl = addCard("Utopia Sprawl", p);
        sprawl.setChosenColors(Lists.newArrayList("blue"));
        sprawl.attachToEntity(forest, null);
        registerBattlefieldTriggers(game, sprawl);
        Card spell = addCardToZone("Growth Spiral", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        AssertJUnit.assertTrue(canAutoPay(game, p, cost("G U"), sa));

        CardCollection sources = predictedManaSources(game, p, cost("G U"), sa);
        AssertJUnit.assertEquals("Only the Sprawl'd Forest should be tapped", 1,
                sources.stream().filter(c -> "Forest".equals(c.getName())).count());
        AssertJUnit.assertFalse("Utopia Sprawl is not a mana source host",
                sources.anyMatch(c -> "Utopia Sprawl".equals(c.getName())));

        AssertJUnit.assertTrue("Production auto-pay should match feasibility", prodAutoPay(game, p, cost("G U"), sa));
        AssertJUnit.assertEquals(1, countTapped(game, "Forest"));
    }

    // Forest + Market Festival: one tap produces {G} plus two any ({G}{U}{R} from a single source).
    @Test
    public void marketFestivalProducesThreeManaFromOneForestTap() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        Card forest = addCard("Forest", p);
        Card festival = addCard("Market Festival", p);
        festival.attachToEntity(forest, null);
        registerBattlefieldTriggers(game, festival);
        Card spell = addCardToZone("Temur Charm", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        AssertJUnit.assertTrue("One Festival'd Forest should pay {G}{U}{R}",
                canAutoPay(game, p, cost("G U R"), sa));

        CardCollection sources = predictedManaSources(game, p, cost("G U R"), sa);
        AssertJUnit.assertEquals("Only the Festival'd Forest should be tapped", 1,
                sources.stream().filter(c -> "Forest".equals(c.getName())).count());

        AssertJUnit.assertTrue("Production auto-pay should match feasibility",
                prodAutoPay(game, p, cost("G U R"), sa));
        AssertJUnit.assertEquals(1, countTapped(game, "Forest"));
    }

    // Gemstone Caverns with a luck counter: tap adds one mana of any color.
    @Test
    public void gemstoneCavernsWithLuckCounterPaysColoredMana() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        Card caverns = addCard("Gemstone Caverns", p);
        caverns.addCounterInternal(forge.game.card.CounterEnumType.LUCK, 1, p, false, null, null);
        Card spell = addCardToZone("Lightning Bolt", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        AssertJUnit.assertTrue("Luck-counter Gemstone Caverns should pay {R}",
                canAutoPay(game, p, cost("R"), sa));

        CardCollection sources = predictedManaSources(game, p, cost("R"), sa);
        AssertJUnit.assertTrue("Gemstone Caverns should be the mana source",
                sources.anyMatch(c -> "Gemstone Caverns".equals(c.getName())));

        AssertJUnit.assertTrue("Production auto-pay should match feasibility",
                prodAutoPay(game, p, cost("R"), sa));
        AssertJUnit.assertEquals(1, countTapped(game, "Gemstone Caverns"));
    }

    // Gemstone Caverns without a luck counter: tap adds {C} only.
    @Test
    public void gemstoneCavernsWithoutLuckCounterPaysColorlessOnly() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Gemstone Caverns", p);
        Card spell = addCardToZone("Expedition Map", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        AssertJUnit.assertTrue(canAutoPay(game, p, cost("1"), sa));

        CardCollection sources = predictedManaSources(game, p, cost("1"), sa);
        AssertJUnit.assertTrue(sources.anyMatch(c -> "Gemstone Caverns".equals(c.getName())));

        AssertJUnit.assertTrue(prodAutoPay(game, p, cost("1"), sa));
        AssertJUnit.assertEquals(1, countTapped(game, "Gemstone Caverns"));
    }

    // Mana Flare doubles land output: one Plains pays {W}{W}.
    @Test
    public void manaFlareDoublesLandOutputForDoubleWhite() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        Card manaFlare = addCard("Mana Flare", p);
        addCards("Plains", 2, p);
        registerBattlefieldTriggers(game, manaFlare);
        Card spell = addCardToZone("Raise the Alarm", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        AssertJUnit.assertTrue(canAutoPay(game, p, cost("W W"), sa));

        CardCollection sources = predictedManaSources(game, p, cost("W W"), sa);
        AssertJUnit.assertEquals("Only one Plains should be tapped", 1,
                sources.stream().filter(c -> "Plains".equals(c.getName())).count());

        AssertJUnit.assertTrue("Production auto-pay should match feasibility", prodAutoPay(game, p, cost("W W"), sa));
        AssertJUnit.assertEquals(1, countTapped(game, "Plains"));
    }

    // Sprawl'd Forest covers {G}{U}; Lotus Petal should stay unused.
    @Test
    public void utopiaSprawlForestBeatsLotusPetalForGreenAndBlue() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        Card forest = addCard("Forest", p);
        Card sprawl = addCard("Utopia Sprawl", p);
        sprawl.setChosenColors(Lists.newArrayList("blue"));
        sprawl.attachToEntity(forest, null);
        registerBattlefieldTriggers(game, sprawl);
        addCard("Lotus Petal", p);
        Card spell = addCardToZone("Growth Spiral", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = spell.getFirstSpellAbility();
        AssertJUnit.assertTrue(canAutoPay(game, p, cost("G U"), sa));

        CardCollection sources = predictedManaSources(game, p, cost("G U"), sa);
        AssertJUnit.assertTrue("Forest should pay both pips", sources.anyMatch(c -> "Forest".equals(c.getName())));
        AssertJUnit.assertFalse("Lotus Petal should not be sacrificed",
                sources.anyMatch(c -> "Lotus Petal".equals(c.getName())));

        AssertJUnit.assertTrue("Production auto-pay should match feasibility", prodAutoPay(game, p, cost("G U"), sa));
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
        CardCollection sources = predictedManaSources(game, p, cost("R W"), sa);
        AssertJUnit.assertTrue("A signet should pay", sources.anyMatch(c -> "Boros Signet".equals(c.getName())));
        AssertJUnit.assertTrue("At least one land should be tapped for signet activation",
                sources.stream().filter(c -> c.getName().contains("Plains") || c.getName().contains("Mountain")).count() >= 1);

        assertProductionPayment(game, p, cost("R W"), sa);
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
