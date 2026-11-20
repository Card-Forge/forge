package forge.ai.simulation;

import java.util.ArrayList;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * More rules-result tests for the static lookahead shortcuts: replacement effects with choices, copy effects,
 * counters, several permanents entering at once, and replacement effects that are granted by static abilities.
 * Like {@link StaticLookaheadTest} these look at game results only, so they must pass on both the original code and
 * the optimized code. Where the AI makes a choice (optional replacements), the assertions allow every legal outcome
 * but not an inconsistent one.
 */
public class StaticLookaheadScenarioTest extends SimulationTest {

    private Game setup(Player[] out) {
        Game game = initAndCreateGame();
        out[0] = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, out[0]);
        return game;
    }

    private Card enter(Game game, Player p, String name) {
        Card c = addCardToZone(name, p, ZoneType.Hand);
        Card onTable = game.getAction().moveToPlay(c, null, AbilityKey.newMap());
        game.getAction().checkStateEffects(true);
        return onTable == null ? c : onTable;
    }

    private static List<Card> battlefieldCardsNamed(Game game, String name) {
        List<Card> res = new ArrayList<>();
        for (Card c : game.getCardsIn(ZoneType.Battlefield)) {
            if (c.getName().equals(name)) {
                res.add(c);
            }
        }
        return res;
    }

    // ---- replacement effects that put counters on entering permanents ----

    private int hydraCounters(String... enchantments) {
        Player[] p = new Player[1];
        Game game = setup(p);
        for (String e : enchantments) {
            addCard(e, p[0]);
        }
        game.getAction().checkStateEffects(true);
        Card hydra = enter(game, p[0], "Kalonian Hydra");
        AssertJUnit.assertTrue(hydra.isInZone(ZoneType.Battlefield));
        return hydra.getCounters(CounterEnumType.P1P1);
    }

    @Test
    public void testEtbCountersWithoutModifiers() {
        AssertJUnit.assertEquals(4, hydraCounters());
    }

    @Test
    public void testEtbCountersWithHardenedScales() {
        AssertJUnit.assertEquals(5, hydraCounters("Hardened Scales"));
    }

    @Test
    public void testEtbCountersWithDoublingSeason() {
        AssertJUnit.assertEquals(8, hydraCounters("Doubling Season"));
    }

    @Test
    public void testEtbCountersWithBothCompetingModifiers() {
        // the player chooses the order: (4 + 1) * 2 or 4 * 2 + 1
        int counters = hydraCounters("Hardened Scales", "Doubling Season");
        AssertJUnit.assertTrue("unexpected counter count " + counters, counters == 9 || counters == 10);
    }

    // ---- optional replacement effects ----

    @Test
    public void testShockLandIsEitherTappedOrCostsTwoLife() {
        Player[] p = new Player[1];
        Game game = setup(p);
        addCard("Urborg, Tomb of Yawgmoth", p[0]);
        game.getAction().checkStateEffects(true);
        for (int i = 0; i < 4; i++) {
            int before = p[0].getLife();
            Card land = enter(game, p[0], "Steam Vents");
            AssertJUnit.assertTrue(land.isInZone(ZoneType.Battlefield));
            int paid = before - p[0].getLife();
            AssertJUnit.assertTrue("either tapped without paying, or untapped after paying 2 life",
                    (land.isTapped() && paid == 0) || (!land.isTapped() && paid == 2));
            AssertJUnit.assertTrue("Urborg still applies", land.getType().hasSubtype("Swamp"));
        }
    }

    // ---- replacement effects of other permanents (no static ability involved) ----

    @Test
    public void testOpponentsCreaturesEnterTappedButOwnDoNot() {
        Player[] p = new Player[1];
        Game game = setup(p);
        Player opp = game.getPlayers().get(0);
        addCard("Blind Obedience", p[0]);
        addCard("Enchanted Evening", p[0]);
        game.getAction().checkStateEffects(true);

        Card theirs = enter(game, opp, "Runeclaw Bear");
        Card mine = enter(game, p[0], "Runeclaw Bear");
        AssertJUnit.assertTrue("an opponent's creature enters tapped", theirs.isTapped());
        AssertJUnit.assertFalse("own creature does not", mine.isTapped());
        AssertJUnit.assertTrue(theirs.isEnchantment());
        AssertJUnit.assertTrue(mine.isEnchantment());
    }

    // ---- replacement effects granted by a static ability (the case the lookahead exists for) ----

    @Test
    public void testRiotGrantedByStaticAbilityIsStillApplied() {
        Player[] p = new Player[1];
        Game game = setup(p);
        addCard("Rhythm of the Wild", p[0]);
        game.getAction().checkStateEffects(true);

        for (int i = 0; i < 4; i++) {
            Card bear = enter(game, p[0], "Runeclaw Bear");
            AssertJUnit.assertTrue(bear.isInZone(ZoneType.Battlefield));
            boolean counter = bear.getCounters(CounterEnumType.P1P1) == 1;
            boolean haste = bear.hasKeyword("Haste");
            AssertJUnit.assertTrue("riot: exactly one of +1/+1 counter or haste (counter=" + counter + ", haste=" + haste + ")",
                    counter != haste);
        }
    }

    @Test
    public void testRiotCombinedWithHardenedScales() {
        Player[] p = new Player[1];
        Game game = setup(p);
        addCard("Rhythm of the Wild", p[0]);
        addCard("Hardened Scales", p[0]);
        game.getAction().checkStateEffects(true);

        Card bear = enter(game, p[0], "Runeclaw Bear");
        int counters = bear.getCounters(CounterEnumType.P1P1);
        boolean haste = bear.hasKeyword("Haste");
        // choosing the counter gives 1 + 1 under Hardened Scales, choosing haste gives none
        AssertJUnit.assertTrue("counters=" + counters + ", haste=" + haste,
                (counters == 2 && !haste) || (counters == 0 && haste));
    }

    // ---- copy effects ----

    @Test
    public void testCloneEntersAsCopyOrIsNotKept() {
        Player[] p = new Player[1];
        Game game = setup(p);
        addCard("Enchanted Evening", p[0]);
        addCard("Runeclaw Bear", p[0]);
        game.getAction().checkStateEffects(true);

        Card clone = enter(game, p[0], "Clone");
        if (clone.isInZone(ZoneType.Battlefield)) {
            AssertJUnit.assertEquals("a Clone on the battlefield is a copy of the Bear", "Runeclaw Bear", clone.getName());
            AssertJUnit.assertTrue(clone.isCreature());
            AssertJUnit.assertTrue(clone.isEnchantment());
        } else {
            // the optional copy was declined, so it is a 0/0 and died
            AssertJUnit.assertTrue(clone.isInZone(ZoneType.Graveyard));
        }
        int bears = battlefieldCardsNamed(game, "Runeclaw Bear").size();
        AssertJUnit.assertEquals("the original Bear is untouched, plus the Clone if it stayed",
                clone.isInZone(ZoneType.Battlefield) ? 2 : 1, bears);
    }

    // ---- several permanents entering at the same time ----

    @Test
    public void testPlanarBirthReturnsSeveralLandsAtOnceUnderBlanketEffects() {
        Player[] p = new Player[1];
        Game game = setup(p);
        Player opp = game.getPlayers().get(0);
        addCards("Plains", 3, p[0]);
        addCard("Urborg, Tomb of Yawgmoth", p[0]);
        addCard("Enchanted Evening", p[0]);
        addCardToZone("Forest", p[0], ZoneType.Graveyard);
        addCardToZone("Forest", p[0], ZoneType.Graveyard);
        addCardToZone("Island", opp, ZoneType.Graveyard);
        addCardToZone("Dismal Backwater", p[0], ZoneType.Graveyard); // not basic, must stay
        Card spell = addCardToZone("Planar Birth", p[0], ZoneType.Hand);
        game.getAction().checkStateEffects(true);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p[0]);

        SpellAbility sa = spell.getSpellAbilities().get(0);
        GameSimulator sim = createSimulator(p[0]);
        sim.simulateSpellAbility(sa);
        Game simGame = sim.getSimulatedGameState();

        List<Card> forests = battlefieldCardsNamed(simGame, "Forest");
        List<Card> islands = battlefieldCardsNamed(simGame, "Island");
        AssertJUnit.assertEquals(2, forests.size());
        AssertJUnit.assertEquals(1, islands.size());
        AssertJUnit.assertEquals("the non-basic land stays in the graveyard", 0,
                battlefieldCardsNamed(simGame, "Dismal Backwater").size());
        List<Card> returned = new ArrayList<>(forests);
        returned.addAll(islands);
        for (Card land : returned) {
            AssertJUnit.assertTrue(land.getName() + " enters tapped", land.isTapped());
            AssertJUnit.assertTrue(land.getName() + " is a Swamp under Urborg", land.getType().hasSubtype("Swamp"));
            AssertJUnit.assertTrue(land.getName() + " is an enchantment under Enchanted Evening", land.isEnchantment());
        }
        AssertJUnit.assertEquals("the Island returns under its owner's control", islands.get(0).getOwner(),
                islands.get(0).getController());
    }
}
