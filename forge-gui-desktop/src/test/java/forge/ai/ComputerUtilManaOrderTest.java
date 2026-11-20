package forge.ai;

import forge.ai.simulation.GameSimulator;
import forge.ai.simulation.Plan;
import forge.ai.simulation.SimulationTest;
import forge.ai.simulation.SpellAbilityPicker;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CounterEnumType;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.gamesimulationtests.util.PlayerControllerForTests;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Which source {@link ComputerUtilMana} pays with when several could. A human's auto-pay runs the same code.
 * A payment leaves at least one source of each colour untapped when it can, without using more sources.
 * Free sources pay first, then sources that hurt when tapped, and sources that are used up go last.
 * A life loss on a trigger other than tapping is not charged as pain.
 */
public class ComputerUtilManaOrderTest extends SimulationTest {

    /**
     * Pays the cost in test mode with the given basic lands and returns how many of each land
     * name stay untapped. Land specs look like "Swamp:4".
     */
    private Map<String, Integer> payAndCountUntapped(final String cost, final int expectedTapped, final String... landSpecs) {
        return payAndCountUntapped(cost, expectedTapped, new String[0], landSpecs);
    }

    private Map<String, Integer> payAndCountUntapped(final String cost, final int expectedTapped, final String[] hand, final String... landSpecs) {
        Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);

        final Map<String, Integer> untapped = new HashMap<>();
        for (final String spec : landSpecs) {
            final String[] parts = spec.split(":");
            final int count = Integer.parseInt(parts[1]);
            for (int i = 0; i < count; i++) {
                // creatures that make mana must be able to tap right away
                addCard(parts[0], p).setSickness(false);
            }
            untapped.put(parts[0], count);
        }

        for (final String name : hand) {
            addCardToZone(name, p, ZoneType.Hand);
        }

        // any spell will do as the host, only the cost matters
        final Card host = addCardToZone("Grizzly Bears", p, ZoneType.Hand);
        final SpellAbility sa = host.getFirstSpellAbility();
        sa.setActivatingPlayer(p);

        final ManaCostBeingPaid toPay = new ManaCostBeingPaid(new ManaCost(new ManaCostParser(cost)));
        final CardCollection used = ComputerUtilMana.getManaSourcesToPayCost(toPay, sa, p, false);
        assertNotNull(used, "cost " + cost + " should be payable");
        assertEquals(used.size(), expectedTapped, "number of sources tapped for " + cost);

        for (final Card c : used) {
            untapped.merge(c.getName(), -1, Integer::sum);
        }
        return untapped;
    }

    private static void assertLeft(final Map<String, Integer> untapped, final String... lands) {
        for (final String land : lands) {
            assertTrue(untapped.get(land) >= 1, "expected at least one untapped " + land + ", got " + untapped);
        }
    }

    /** The sources a test payment of this cost uses, for a spell of this name in hand. */
    private CardCollection sourcesToPay(final Player p, final String spell, final String cost) {
        final SpellAbility sa = addCardToZone(spell, p, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        final ManaCostBeingPaid toPay = new ManaCostBeingPaid(new ManaCost(new ManaCostParser(cost)));
        final CardCollection used = ComputerUtilMana.getManaSourcesToPayCost(toPay, sa, p, false);
        assertNotNull(used, "cost " + cost + " should be payable");
        return used;
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

        GameSimulator sim = createSimulator(p);
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

        GameSimulator sim = createSimulator(p);
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

        GameSimulator sim = createSimulator(p);
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

        SpellAbilityPicker picker = new SpellAbilityPicker(p);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        AssertJUnit.assertTrue(sa.getHostCard().isCreature());

        // AI able to cast both creatures
        Plan plan = picker.getPlan();
        AssertJUnit.assertEquals(2, plan.getDecisions().size());
    }

    @Test
    public void useRequiredMultiManaSourceFirst() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        Card ring = addCard("Sol Ring", p);
        addCard("Chromatic Orrery", p);
        Card golem = addCardToZone("Steel Golem", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = golem.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        AssertJUnit.assertTrue(ComputerUtilMana.payManaCost(sa.getPayCosts(), p, sa, false));

        // the Orrery is needed either way and covers the cost alone, so the Ring stays untapped
        AssertJUnit.assertTrue(ring.isUntapped());
    }

    @Test
    public void cavernOfSoulsPaysColorlessWhenNoOtherSourceCan() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Cavern of Souls", p).setChosenType("Eldrazi");
        addCards("Forest", 2, p);
        Card fleshraker = addCardToZone("Glaring Fleshraker", p, ZoneType.Hand); // {2}{C}

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = fleshraker.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        AssertJUnit.assertTrue(ComputerUtilMana.canPayManaCost(sa, p, 0, false));
        AssertJUnit.assertTrue(ComputerUtilMana.payManaCost(sa.getPayCosts(), p, sa, false));
    }

    @Test
    public void delightedHalflingPaysGenericSoTheLegendarySpellCantBeCountered() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Delighted Halfling", p).setSickness(false);
        addCard("Mountain", p);
        addCard("Forest", p);
        Card squee = addCardToZone("Squee, Goblin Nabob", p, ZoneType.Hand); // {2}{R}

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = squee.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        AssertJUnit.assertTrue(ComputerUtilMana.payManaCost(sa.getPayCosts(), p, sa, false));
        AssertJUnit.assertTrue(sa.getPayingMana().stream().anyMatch(m -> m.addsNoCounterMagic(sa)));
    }

    @Test
    public void payColoredWithGemstoneCavernsLuckCounter() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card caverns = addCard("Gemstone Caverns", p);
        caverns.addCounterInternal(CounterEnumType.LUCK, 1, p, false, null, null);
        Card sentinel = addCardToZone("Esper Sentinel", p, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = sentinel.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        AssertJUnit.assertTrue(ComputerUtilMana.canPayManaCost(sa, p, 0, false));
    }

    @Test
    public void genericPartKeepsBothColoursAvailable() {
        // {3}{B} with 4 Swamp + 2 Island: 3 Swamp + 1 Island leaves one of each
        assertLeft(payAndCountUntapped("3 B", 4, "Swamp:4", "Island:2"), "Swamp", "Island");
    }

    @Test
    public void genericPartPrefersTheOtherColour() {
        // {1}{B} with 2 Swamp + 4 Island: must not use both Swamps
        assertLeft(payAndCountUntapped("1 B", 2, "Swamp:2", "Island:4"), "Swamp", "Island");
    }

    @Test
    public void hybridShardLeavesBothColoursAvailable() {
        // {U/B} with 1 Swamp + 2 Island: pay with an Island so the Swamp stays
        assertLeft(payAndCountUntapped("U/B", 1, "Swamp:1", "Island:2"), "Swamp", "Island");
    }

    @Test
    public void coloredShardDoesNotDrainItsColour() {
        // {3}{W} with 2 Plains + 4 Island: only one Plains is needed
        assertLeft(payAndCountUntapped("3 W", 4, "Plains:2", "Island:4"), "Plains", "Island");
    }

    @Test
    public void threeColoursAreAllKept() {
        // {2}{R} with 2 of each of three types: one Mountain for R, the generic from Forest and Plains
        assertLeft(payAndCountUntapped("2 R", 3, "Mountain:2", "Forest:2", "Plains:2"), "Mountain", "Forest", "Plains");
    }

    @Test
    public void multiColourSourcesKeepWhatTheHandNeeds() {
        // {X}{G}{G} with X=2: Vigorbloom Charm {G}{W} is in hand, so a Plains and a green source must stay
        final Map<String, Integer> untapped = payAndCountUntapped("2 G G", 4, new String[] { "Vigorbloom Charm" },
                "Plains:2", "Forest:1", "Formidable Commons:2", "Greenhouse Propagator:1");
        assertLeft(untapped, "Plains");
        assertTrue(untapped.get("Forest") + untapped.get("Formidable Commons") + untapped.get("Greenhouse Propagator") >= 1,
                "expected a green source to stay untapped, got " + untapped);
    }

    @Test
    public void unavoidableDrainStillPaysWithSameNumberOfLands() {
        // {2}{B} with 2 Swamp + 2 Island taps 3 of 4 lands, so one colour is gone either way
        final Map<String, Integer> untapped = payAndCountUntapped("2 B", 3, "Swamp:2", "Island:2");
        assertEquals(untapped.get("Swamp") + untapped.get("Island"), 1);
    }

    @Test
    public void spiritGuideIsNotExiledToKeepAHeldColour() {
        // the last Stomping Ground is the only green source for Giant Growth, but the lands pay {5} on their own
        final Map<String, Integer> untapped = payAndCountUntapped("5", 5,
                new String[] { "Simian Spirit Guide", "Giant Growth" }, "Stomping Ground:5");
        assertEquals((int) untapped.get("Stomping Ground"), 0, "expected every land tapped, got " + untapped);
    }

    @Test
    public void spiritGuideDoesNotCountAsASourceOfItsColour() {
        // Stomping Ground is the only land that makes green, so Steam Vents has to pay the {R}
        final Map<String, Integer> untapped = payAndCountUntapped("3 R G", 5,
                new String[] { "Elvish Spirit Guide" }, "Island:3", "Stomping Ground:1", "Steam Vents:1");
        assertEquals((int) untapped.get("Steam Vents"), 0, "expected every land tapped, got " + untapped);
    }

    @Test
    public void painlessTapBeforePainfulTapBeforeSpentSource() {
        // {1} with Caves of Koilos, City of Brass and a Spirit Guide: the Caves' free {C} pays,
        // the City tap that loses 1 life comes second, and exiling the Guide stays the last resort
        final Map<String, Integer> untapped = payAndCountUntapped("1", 1,
                new String[] { "Simian Spirit Guide" }, "Caves of Koilos:1", "City of Brass:1");
        assertEquals((int) untapped.get("Caves of Koilos"), 0, "expected the Caves to pay, got " + untapped);
        assertEquals((int) untapped.get("City of Brass"), 1, "expected the City to stay untapped, got " + untapped);
    }

    @Test
    public void painLandIsTappedBeforeASpiritGuideIsSpent() {
        // losing 1 life to City of Brass still beats exiling a card from the hand for good
        final Map<String, Integer> untapped = payAndCountUntapped("1", 1,
                new String[] { "Simian Spirit Guide" }, "City of Brass:1");
        assertEquals((int) untapped.get("City of Brass"), 0, "expected the City to pay, got " + untapped);
    }

    @Test
    public void lifeLostOnEnteringIsNotTapPain() {
        // Oscorp Industries only loses life when it enters from a graveyard, so tapping it costs nothing
        // and it pays before a painless one-shot source is spent
        final Map<String, Integer> untapped = payAndCountUntapped("1", 1,
                new String[] { "Simian Spirit Guide" }, "Oscorp Industries:1");
        assertEquals((int) untapped.get("Oscorp Industries"), 0, "expected the Oscorp to pay, got " + untapped);
    }

    @Test
    public void painIsWeighedAgainstOtherCostsWithoutHeldColours() {
        // {1} with Caves of Koilos and City of Brass and nothing in hand to steer by colour:
        // the Caves' free {C} still pays, since a tap that loses 1 life is scored as a cost
        final Map<String, Integer> untapped = payAndCountUntapped("1", 1, "Caves of Koilos:1", "City of Brass:1");
        assertEquals((int) untapped.get("Caves of Koilos"), 0, "expected the Caves to pay, got " + untapped);
        assertEquals((int) untapped.get("City of Brass"), 1, "expected the City to stay untapped, got " + untapped);
    }

    @Test
    public void landPaysBeforeASourceThatIsUsedUp() {
        // a Treasure or a Lotus Petal is gone once it pays, so City of Brass pays {1} even though it deals 1 damage
        Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        final Card city = addCard("City of Brass", p);
        addCard("Lotus Petal", p);
        addToken("c_a_treasure_sac", p);

        final CardCollection used = sourcesToPay(p, "Grizzly Bears", "1");
        assertEquals(used.size(), 1);
        assertTrue(used.contains(city), "expected City of Brass to pay, got " + used);
    }

    @Test
    public void cityOfBrassDoesNotPayAtOneLife() {
        // at 1 life the City's damage would be lethal, so a Treasure or a Lotus Petal pays instead
        Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        p.setLife(1, null);
        final Card city = addCard("City of Brass", p);
        addCard("Lotus Petal", p);
        addToken("c_a_treasure_sac", p);

        final CardCollection used = sourcesToPay(p, "Grizzly Bears", "1");
        assertEquals(used.size(), 1);
        assertFalse(used.contains(city), "expected City of Brass to stay untapped, got " + used);
    }

    @Test
    public void cavesPaysGenericWithoutDamage() {
        // Caves of Koilos pays {1} with its {C} ability, not the colored one that deals 1 damage
        Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        final Card caves = addCard("Caves of Koilos", p);
        final int life = p.getLife();

        final SpellAbility sa = addCardToZone("Grizzly Bears", p, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        final ManaCostBeingPaid cost = new ManaCostBeingPaid(new ManaCost(new ManaCostParser("1")));
        assertTrue(ComputerUtilMana.payManaCost(cost, sa, p, false) && cost.isPaid());
        assertTrue(caves.isTapped());
        assertEquals(p.getLife(), life);
    }

    @Test
    public void treasurePreferenceSpendsOneTreasure() {
        // Hired Hexblade is stronger when Treasure mana is spent on it, and one Treasure is enough for that
        Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        addCards("Swamp", 2, p);
        addTokens("c_a_treasure_sac", 2, p);

        final CardCollection used = sourcesToPay(p, "Hired Hexblade", "1 B");
        assertEquals(used.stream().filter(c -> c.getType().hasSubtype("Treasure")).count(), 1L,
                "expected exactly one Treasure, got " + used);
    }

    @Test
    public void cavernOfSoulsPaysGenericSoTheSpellCantBeCountered() {
        // Mind Stone ranks below Cavern of Souls, but still pays the {C} and leaves the Cavern for the generic part
        Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        addCard("Cavern of Souls", p).setChosenType("Eldrazi");
        addCard("Mind Stone", p);
        addCard("Forest", p);
        final Card fleshraker = addCardToZone("Glaring Fleshraker", p, ZoneType.Hand); // {2}{C}

        final SpellAbility sa = fleshraker.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        assertTrue(ComputerUtilMana.payManaCost(sa.getPayCosts(), p, sa, false));
        assertTrue(sa.getPayingMana().stream().anyMatch(m -> m.addsNoCounterMagic(sa)),
                "expected Cavern of Souls mana to be spent on the spell");
    }

    @Test
    public void humanAutoPayTapsALandBeforeSacrificingToKeepAColour() {
        // Hall of Oracles is the only blue source for the Counterspell in hand, but sparing it is not worth a creature
        Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        addCard("Mountain", p);
        final Card hall = addCard("Hall of Oracles", p);
        addCard("Ashnod's Altar", p);
        final Card spirit = addToken("w_1_1_spirit_flying", p);
        addCardToZone("Counterspell", p, ZoneType.Hand);

        // the Auto button pays for a human seat under a temporary AI controller
        p.dangerouslySetController(new PlayerControllerForTests(game, p, p.getLobbyPlayer()));
        p.runWithController(() -> {
            final SpellAbility sa = addCardToZone("Lightning Strike", p, ZoneType.Hand).getFirstSpellAbility();
            sa.setActivatingPlayer(p);
            final ManaCostBeingPaid cost = new ManaCostBeingPaid(new ManaCost(new ManaCostParser("1 R")));
            assertTrue(ComputerUtilMana.payManaCost(cost, sa, p, false) && cost.isPaid());
        }, new PlayerControllerAi(game, p, p.getLobbyPlayer()));
        assertTrue(hall.isTapped(), "expected Hall of Oracles to pay");
        assertTrue(spirit.isInPlay(), "expected the Spirit to stay on the battlefield");
    }

    @Test
    public void landPaysBeforeAManaCreature() {
        // Llanowar Elves can still attack or block if the Forest pays
        final Map<String, Integer> untapped = payAndCountUntapped("1", 1, "Forest:1", "Llanowar Elves:1");
        assertEquals((int) untapped.get("Forest"), 0, "expected the Forest to pay, got " + untapped);
    }
}
