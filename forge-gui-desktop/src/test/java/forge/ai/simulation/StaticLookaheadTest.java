package forge.ai.simulation;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Rules-result tests for permanent blanket static effects (Teferi, Mage of Zhalfir, Urborg, Enchanted Evening, ...)
 * in combination with the lookahead static checks done by GameActionUtil.getAlternativeCosts and
 * ReplacementHandler.getReplacementList. They only look at game results, never at how often anything is computed,
 * so they must pass both with and without the optimizations of those lookaheads.
 *
 * Run with: mvn -pl forge-gui-desktop -am test -Dtest=StaticLookaheadTest -Dsurefire.failIfNoSpecifiedTests=false
 */
public class StaticLookaheadTest extends SimulationTest {

    private static final int REPEAT = 25;

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

    @Test
    public void testFlashbackCostStillOfferedUnderTeferiAndNothingLeaks() {
        Player[] p = new Player[1];
        Game game = setup(p);
        Card teferi = addCard("Teferi, Mage of Zhalfir", p[0]);
        Card spell = addCardToZone("Think Twice", p[0], ZoneType.Graveyard);
        Card creature = addCardToZone("Cadaver Imp", p[0], ZoneType.Graveyard);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue("Teferi grants Flash to a creature card in the graveyard", creature.hasKeyword("Flash"));
        AssertJUnit.assertFalse("but not to an instant", spell.hasKeyword("Flash"));

        SpellAbility base = spell.getFirstSpellAbility();
        List<SpellAbility> first = GameActionUtil.getAlternativeCosts(base, p[0], false);
        AssertJUnit.assertFalse("flashback option expected from the graveyard", first.isEmpty());
        for (int i = 0; i < REPEAT; i++) {
            AssertJUnit.assertEquals(first.size(), GameActionUtil.getAlternativeCosts(base, p[0], false).size());
        }
        GameActionUtil.getAlternativeCosts(creature.getFirstSpellAbility(), p[0], false);

        AssertJUnit.assertTrue(spell.isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue(creature.isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue("Flash still granted after the lookahead", creature.hasKeyword("Flash"));
        AssertJUnit.assertFalse(spell.hasKeyword("Flash"));
        AssertJUnit.assertTrue(teferi.isInZone(ZoneType.Battlefield));
    }

    @Test
    public void testEscapeCostStillOfferedUnderTeferi() {
        Player[] p = new Player[1];
        Game game = setup(p);
        addCard("Teferi, Mage of Zhalfir", p[0]);
        // a creature card with Escape in the graveyard: Teferi affects it, and Escape is an alternative cost
        Card uro = addCardToZone("Uro, Titan of Nature's Wrath", p[0], ZoneType.Graveyard);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(uro.hasKeyword("Flash"));

        List<SpellAbility> alts = GameActionUtil.getAlternativeCosts(uro.getFirstSpellAbility(), p[0], false);
        AssertJUnit.assertFalse("escape option expected from the graveyard", alts.isEmpty());
        AssertJUnit.assertTrue(uro.isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue("Flash still granted after the lookahead", uro.hasKeyword("Flash"));
    }

    @Test
    public void testLandEnteringUnderUrborgIsASwampAndEntersTappedWhenItShould() {
        Player[] p = new Player[1];
        Game game = setup(p);
        addCard("Urborg, Tomb of Yawgmoth", p[0]);
        game.getAction().checkStateEffects(true);

        Card tapLand = enter(game, p[0], "Dismal Backwater");
        AssertJUnit.assertTrue(tapLand.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertTrue("Dismal Backwater enters tapped", tapLand.isTapped());
        AssertJUnit.assertTrue(tapLand.getType().hasSubtype("Swamp"));

        Card forest = enter(game, p[0], "Forest");
        AssertJUnit.assertFalse("a basic land must not enter tapped", forest.isTapped());
        AssertJUnit.assertTrue("Forest is also a Swamp", forest.getType().hasSubtype("Swamp"));
        AssertJUnit.assertTrue(forest.getType().hasSubtype("Forest"));
    }

    @Test
    public void testCreatureEnteringUnderUrborgIsUnaffected() {
        Player[] p = new Player[1];
        Game game = setup(p);
        addCard("Urborg, Tomb of Yawgmoth", p[0]);
        game.getAction().checkStateEffects(true);

        Card bear = enter(game, p[0], "Runeclaw Bear");
        AssertJUnit.assertTrue(bear.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertTrue(bear.isCreature());
        AssertJUnit.assertFalse(bear.isLand());
        AssertJUnit.assertFalse(bear.getType().hasSubtype("Swamp"));
        AssertJUnit.assertFalse(bear.isTapped());
    }

    @Test
    public void testEnteringCreatureGetsBlanketTypeFromEnchantedEvening() {
        Player[] p = new Player[1];
        Game game = setup(p);
        addCard("Enchanted Evening", p[0]);
        game.getAction().checkStateEffects(true);

        Card bear = enter(game, p[0], "Runeclaw Bear");
        AssertJUnit.assertTrue(bear.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertTrue("Enchanted Evening makes every permanent an enchantment", bear.isEnchantment());
        AssertJUnit.assertTrue(bear.isCreature());
    }

    @Test
    public void testEtbCountersStillAppliedUnderBlanketEffects() {
        Player[] p = new Player[1];
        Game game = setup(p);
        addCard("Enchanted Evening", p[0]);
        addCard("Urborg, Tomb of Yawgmoth", p[0]);
        game.getAction().checkStateEffects(true);

        Card hydra = enter(game, p[0], "Kalonian Hydra");
        AssertJUnit.assertTrue(hydra.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertEquals("enters with four +1/+1 counters", 4, hydra.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(4, hydra.getNetPower());
        AssertJUnit.assertTrue(hydra.isEnchantment());
    }

    @Test
    public void testSeveralPermanentsEnteringOneAfterAnother() {
        Player[] p = new Player[1];
        Game game = setup(p);
        addCard("Urborg, Tomb of Yawgmoth", p[0]);
        addCard("Enchanted Evening", p[0]);
        game.getAction().checkStateEffects(true);

        for (int i = 0; i < 5; i++) {
            Card land = enter(game, p[0], "Forest");
            AssertJUnit.assertTrue(land.getType().hasSubtype("Swamp"));
            AssertJUnit.assertTrue(land.isEnchantment());
            Card bear = enter(game, p[0], "Runeclaw Bear");
            AssertJUnit.assertTrue(bear.isEnchantment());
            AssertJUnit.assertFalse(bear.getType().hasSubtype("Swamp"));
        }
        AssertJUnit.assertEquals(10, p[0].getCardsIn(ZoneType.Battlefield).size() - 2);
    }

    @Test
    public void testRepeatedChecksAreStableAndDoNotAccumulateEffects() {
        Player[] p = new Player[1];
        Game game = setup(p);
        addCard("Urborg, Tomb of Yawgmoth", p[0]);
        Card forest = addCard("Forest", p[0]);
        game.getAction().checkStateEffects(true);
        int subtypes = forest.getType().getSubtypes().size();
        for (int i = 0; i < REPEAT; i++) {
            game.getAction().checkStateEffects(true);
        }
        AssertJUnit.assertEquals(subtypes, forest.getType().getSubtypes().size());
        AssertJUnit.assertTrue(forest.getType().hasSubtype("Swamp"));
    }
}
