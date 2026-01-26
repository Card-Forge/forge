package forge.ai.controller;

import forge.ai.simulation.GameSimulator;
import forge.ai.simulation.Plan;
import forge.ai.simulation.SimulationTest;
import forge.ai.simulation.SpellAbilityPicker;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.List;

public class AutoPaymentTest extends SimulationTest {

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

    @Test
    public void payWithSignets() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        Card signet = addCard("Dimir Signet", p);
        Card island = addCard("Island", p);
        Card strix = addCardToZone("Tidehollow Strix", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        GameSimulator sim = createSimulator(game, p);
        // AI doesn't know how to use Signets. So the score here is going to be bad

        int score = sim.simulateSpellAbility(strix.getFirstSpellAbility()).value;
        AssertJUnit.assertTrue(score > 0);

        Game simGame = sim.getSimulatedGameState();
        Card strixBF = findCardWithName(simGame, strix.getName());
        AssertJUnit.assertNotNull(strixBF);
        AssertJUnit.assertEquals(ZoneType.Battlefield, strixBF.getZone().getZoneType());

        Card signetBF = findCardWithName(simGame, signet.getName());
        Card islandBF = findCardWithName(simGame, island.getName());
        AssertJUnit.assertTrue(signetBF.isTapped());
        AssertJUnit.assertTrue(islandBF.isTapped());
    }

    @Test
    public void leaveUpManaOptions() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        // Add 6 lands that can produce multiple colors including red, blue, and black
        addCard("Steam Vents", p); // UR
        addCard("Steam Vents", p); // UR
        addCard("Blood Crypt", p); // BR
        addCard("Blood Crypt", p); // BR
        addCard("Watery Grave", p); // UB
        addCard("Watery Grave", p); // UB

        // Add a card to hand that requires specific mana
        Card spell = addCardToZone("Phyrexian Tyranny", p, ZoneType.Hand);

        addCardToZone("Final Fortune", p, ZoneType.Hand);
        addCardToZone("Counterspell", p, ZoneType.Hand);
        addCardToZone("Hymn to Tourach", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        GameSimulator sim = createSimulator(game, p);
        int score = sim.simulateSpellAbility(spell.getFirstSpellAbility()).value;

        AssertJUnit.assertTrue(score > 0);
        Game simGame = sim.getSimulatedGameState();

        Card spellBF = findCardWithName(simGame, "Phyrexian Tyranny");
        AssertJUnit.assertNotNull(spellBF);
        AssertJUnit.assertEquals(ZoneType.Battlefield, spellBF.getZone().getZoneType());

        // Verify the AI used mana efficiently and left up the most versatile mana
        Player simPlayer = simGame.getPlayers().get(1);

        // Get untapped lands after casting the spell
        List<Card> untappedLands = simPlayer.getCardsIn(ZoneType.Battlefield).stream()
                .filter(card -> card.isLand() && !card.isTapped())
                .collect(java.util.stream.Collectors.toList());

//        System.out.println("Untapped lands after casting Phyrexian Tyranny:");
//        for (Card land : untappedLands) {
//            System.out.println("- " + land.getName());
//        }

        // Count mana abilities by color
        int redSources = 0;
        int blueSources = 0;
        int blackSources = 0;

        for (Card land : untappedLands) {
            for (SpellAbility sa : land.getManaAbilities()) {
                if (sa.getManaPart() != null) {
                    AbilityManaPart mana = sa.getManaPart();

                    if (mana.canProduce("R", sa)) redSources++;
                    if (mana.canProduce("U", sa)) blueSources++;
                    if (mana.canProduce("B", sa)) blackSources++;
                }
            }
        }

        System.out.println("Untapped mana sources by color:");
        System.out.println("Red: " + redSources);
        System.out.println("Blue: " + blueSources);
        System.out.println("Black: " + blackSources);

        // Phyrexian costs UBR, so the AI should tap lands that provide UBR
        // and leave up the most versatile combination of remaining lands

        int totalSources = redSources + blueSources + blackSources;
        AssertJUnit.assertTrue("AI should leave up at least some lands", totalSources > 0);
        AssertJUnit.assertFalse("AI should leave up multiple red sources", redSources <= 1);
        AssertJUnit.assertFalse("AI should leave up multiple blue sources", blueSources <= 1);
        AssertJUnit.assertFalse("AI should leave up multiple black sources", blackSources <= 1);
    }

    @Test
    public void dontOverpayWithTwoManaSources() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        // Add 3 Plains and 1 Sol Ring
        Card plains1 = addCard("Plains", p);
        Card plains2 = addCard("Plains", p);
        Card plains3 = addCard("Plains", p);
        Card solRing = addCard("Sol Ring", p);

        // Add Hero of Bladehold to hand (costs 2WW)
        Card hero = addCardToZone("Hero of Bladehold", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        GameSimulator sim = createSimulator(game, p);
        int score = sim.simulateSpellAbility(hero.getFirstSpellAbility()).value;

        AssertJUnit.assertTrue("AI should be able to cast Hero of Bladehold", score > 0);
        Game simGame = sim.getSimulatedGameState();

        // Verify Hero of Bladehold was cast successfully
        Card heroBF = findCardWithName(simGame, "Hero of Bladehold");
        AssertJUnit.assertNotNull("Hero of Bladehold should be found", heroBF);
        AssertJUnit.assertEquals("Hero should be on the battlefield", ZoneType.Battlefield, heroBF.getZone().getZoneType());

        // Check which lands were tapped
        Player simPlayer = simGame.getPlayers().get(1);
        List<Card> permanents = simPlayer.getCardsIn(ZoneType.Battlefield).stream().toList();

        int tappedPlainsCount = 0;
        boolean solRingTapped = false;
        int untappedLandCount = 0;

        for (Card permanent : permanents) {
            if (permanent.isTapped()) {
                if (permanent.getName().equals("Plains")) {
                    tappedPlainsCount++;
                } else if (permanent.getName().equals("Sol Ring")) {
                    solRingTapped = true;
                }
            } else if (permanent.getName().equals("Plains")) {
                untappedLandCount++;
            }
        }

        // Exactly 3 permanents should be tapped (2 Plains + Sol Ring)
        // The remaining Plains should be untapped (no overpayment)
        AssertJUnit.assertEquals("Should have tapped exactly 2 Plains", 2, tappedPlainsCount);
        AssertJUnit.assertTrue("Sol Ring should be tapped", solRingTapped);
        AssertJUnit.assertEquals("Should have exactly 1 untapped Plains", 1, untappedLandCount);

        // Verify no floating mana left (this is handled implicitly by checking untapped lands)
        // since we're checking exactly which lands are tapped/untapped
    }

    @Test
    public void dontOverpayWithXManaSources() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        // Add 3 Plains and 1 Serra's Sanctum instead of Sol Ring
        Card plains1 = addCard("Plains", p);
        Card plains2 = addCard("Plains", p);
        Card plains3 = addCard("Plains", p);
        Card serrasSanctum = addCard("Serra's Sanctum", p);

        // Add 3 enchantments that don't affect spell casting
        addCard("Circle of Protection: Red", p);
        addCard("Aegis of the Gods", p);
        addCard("Propaganda", p);

        // Add Hero of Bladehold to hand (costs 2WW)
        Card hero = addCardToZone("Hero of Bladehold", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        GameSimulator sim = createSimulator(game, p);
        int score = sim.simulateSpellAbility(hero.getFirstSpellAbility()).value;

        AssertJUnit.assertTrue("AI should be able to cast Hero of Bladehold", score > 0);
        Game simGame = sim.getSimulatedGameState();

        // Verify Hero of Bladehold was cast successfully
        Card heroBF = findCardWithName(simGame, "Hero of Bladehold");
        AssertJUnit.assertNotNull("Hero of Bladehold should be found", heroBF);
        AssertJUnit.assertEquals("Hero should be on the battlefield", ZoneType.Battlefield, heroBF.getZone().getZoneType());

        // Check which lands were tapped
        Player simPlayer = simGame.getPlayers().get(1);
        List<Card> permanents = simPlayer.getCardsIn(ZoneType.Battlefield).stream().toList();

        int tappedPlainsCount = 0;
        boolean sanctumTapped = false;
        int untappedLandCount = 0;

        for (Card permanent : permanents) {
            if (permanent.isTapped()) {
                if (permanent.getName().equals("Plains")) {
                    tappedPlainsCount++;
                } else if (permanent.getName().equals("Serra's Sanctum")) {
                    sanctumTapped = true;
                }
            } else if (permanent.getName().equals("Plains")) {
                untappedLandCount++;
            }
        }

        // Exactly 3 permanents should be tapped (2 Plains + Serra's Sanctum)
        // The remaining Plains should be untapped (no overpayment)
        AssertJUnit.assertEquals("Should have tapped exactly 1 Plains", 1, tappedPlainsCount);
        AssertJUnit.assertTrue("Serra's Sanctum should be tapped", sanctumTapped);
        AssertJUnit.assertEquals("Should have exactly 2 untapped Plains", 2, untappedLandCount);

        // Verify no floating mana left (this is handled implicitly by checking untapped lands)
        // since we're checking exactly which lands are tapped/untapped
    }

    @Test
    public void payGGWithForestAndWildGrowth() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        // Add one Forest with Wild Growth attached and one regular Forest
        Card forest2 = addCard("Forest", p);
        Card forest1 = addCard("Forest", p);
        Card wildGrowth = addCard("Wild Growth", p);

        // Attach Wild Growth to first Forest
        wildGrowth.attachToEntity(forest1, wildGrowth.getFirstSpellAbility());

        // Add Rofellos, Llanowar Emissary to hand (costs GG)
        Card rofellos = addCardToZone("Rofellos, Llanowar Emissary", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        GameSimulator sim = createSimulator(game, p);
        int score = sim.simulateSpellAbility(rofellos.getFirstSpellAbility()).value;

        AssertJUnit.assertTrue("AI should be able to cast Rofellos", score > 0);
        Game simGame = sim.getSimulatedGameState();

        // Verify Rofellos was cast successfully
        Card rofellosOnBF = findCardWithName(simGame, "Rofellos, Llanowar Emissary");
        AssertJUnit.assertNotNull("Rofellos should be found", rofellosOnBF);
        AssertJUnit.assertEquals("Rofellos should be on the battlefield", ZoneType.Battlefield, rofellosOnBF.getZone().getZoneType());

        // Check which lands were tapped
        Player simPlayer = simGame.getPlayers().get(1);
        List<Card> permanents = simPlayer.getCardsIn(ZoneType.Battlefield).stream().toList();

        boolean enchantedForestTapped = false;
        boolean regularForestTapped = false;

        for (Card permanent : permanents) {
            if (permanent.isTapped() && permanent.getName().equals("Forest")) {
                // Check if this is the enchanted Forest
                boolean hasWildGrowth = permanent.hasCardAttachment("Wild Growth");
                if (hasWildGrowth) {
                    enchantedForestTapped = true;
                } else {
                    regularForestTapped = true;
                }
            }
        }

        // Verify that only the enchanted Forest was tapped
        AssertJUnit.assertTrue("The Forest with Wild Growth should be tapped", enchantedForestTapped);
        AssertJUnit.assertFalse("The regular Forest should not be tapped", regularForestTapped);

        // Verify Wild Growth is still attached to the Forest
        Card wildGrowthOnBF = findCardWithName(simGame, "Wild Growth");
        AssertJUnit.assertNotNull("Wild Growth should still be on the battlefield", wildGrowthOnBF);
        AssertJUnit.assertTrue("Wild Growth should be attached to a card", wildGrowthOnBF.isAttachedToEntity());
    }
}
