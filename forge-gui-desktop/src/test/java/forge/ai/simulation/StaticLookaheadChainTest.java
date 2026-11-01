package forge.ai.simulation;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Chains where a replacement effect changes the entering card, a static ability then applies to the changed card,
 * and that in turn makes another replacement effect apply (or not):
 * <pre>
 *   Clone (Copy layer) enters as a copy of a Saproling
 *     -> Life and Limb makes every Saproling a Forest land
 *     -> Root Maze (lands enter tapped) now applies to it
 * </pre>
 * The lookahead shortcuts must not stop that chain: at the first check the entering card is only a Clone, so no
 * static ability affects it, but after the copy has been applied the check must be redone for the copy.
 * Clone's copy is optional and chosen by the AI, so every test accepts "declined" (0/0 Clone dies) as a legal
 * outcome, and only requires the result to be consistent when the copy was made.
 *
 * These look at game results only, so they must pass on the original code and on the optimized code.
 */
public class StaticLookaheadChainTest extends SimulationTest {

    private Game setup(Player[] out) {
        Game game = initAndCreateGame();
        out[0] = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, out[0]);
        return game;
    }

    /** Resolves Saproling Migration in a simulated game, then puts the Clone from hand onto the battlefield there. */
    private Card[] makeSaprolingThenEnterClone(Game game, Player p, Game[] simOut) {
        // mana for {1}{G}: Moxen are not Forests, so Life and Limb does not make them creatures (no summoning sickness)
        for (String mox : new String[] { "Mox Emerald", "Mox Jet" }) {
            addCard(mox, p).setTapped(false);
        }
        Card migration = addCardToZone("Saproling Migration", p, ZoneType.Hand);
        addCardToZone("Clone", p, ZoneType.Hand);
        game.getAction().checkStateEffects(true);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        SpellAbility sa = migration.getSpellAbilities().get(0);
        GameSimulator sim = createSimulator(p);
        sim.simulateSpellAbility(sa);
        Game simGame = sim.getSimulatedGameState();
        simOut[0] = simGame;

        Player simP = simGame.getPlayers().get(1);
        Card simClone = null;
        for (Card c : simP.getCardsIn(ZoneType.Hand)) {
            if (c.getName().equals("Clone")) {
                simClone = c;
            }
        }
        AssertJUnit.assertNotNull("Clone is still in hand after the simulation", simClone);

        Card onTable = simGame.getAction().moveToPlay(simClone, null, AbilityKey.newMap());
        simGame.getAction().checkStateEffects(true);
        return new Card[] { onTable == null ? simClone : onTable };
    }

    private static int saprolingTokens(Game game) {
        int n = 0;
        for (Card c : game.getCardsIn(ZoneType.Battlefield)) {
            if (c.isToken() && c.getType().hasSubtype("Saproling")) {
                n++;
            }
        }
        return n;
    }

    @Test
    public void testCopyBecomesLandThroughLifeAndLimbAndThenEntersTapped() {
        Player[] p = new Player[1];
        Game game = setup(p);
        addCard("Life and Limb", p[0]);
        addCard("Root Maze", p[0]);
        game.getAction().checkStateEffects(true);

        Game[] sim = new Game[1];
        Card clone = makeSaprolingThenEnterClone(game, p[0], sim)[0];

        // the Saproling token itself is a land through Life and Limb, so Root Maze taps it as it enters
        for (Card c : sim[0].getCardsIn(ZoneType.Battlefield)) {
            if (c.isToken() && c.getType().hasSubtype("Saproling")) {
                AssertJUnit.assertTrue("Saproling is a land under Life and Limb", c.isLand());
                AssertJUnit.assertTrue("Saproling token entered tapped (Root Maze)", c.isTapped());
            }
        }
        AssertJUnit.assertEquals("Saproling Migration (unkicked) makes two tokens, was it cast?", 2, saprolingTokens(sim[0]));

        if (clone.isInZone(ZoneType.Battlefield)) {
            AssertJUnit.assertTrue("Clone copied a Saproling, was " + clone.getName(), clone.getType().hasSubtype("Saproling"));
            AssertJUnit.assertTrue("a copy of a Saproling is a Forest land under Life and Limb", clone.isLand());
            AssertJUnit.assertTrue(clone.getType().hasSubtype("Forest"));
            AssertJUnit.assertTrue("it became a land only after copying, Root Maze must still apply", clone.isTapped());
        } else {
            AssertJUnit.assertTrue("copy declined, 0/0 Clone died", clone.isInZone(ZoneType.Graveyard));
        }
    }

    @Test
    public void testCopyOfSaprolingStaysUntappedWithoutLifeAndLimb() {
        Player[] p = new Player[1];
        Game game = setup(p);
        addCard("Root Maze", p[0]);
        game.getAction().checkStateEffects(true);

        Game[] sim = new Game[1];
        Card clone = makeSaprolingThenEnterClone(game, p[0], sim)[0];
        AssertJUnit.assertEquals("Saproling Migration was cast", 2, saprolingTokens(sim[0]));

        if (clone.isInZone(ZoneType.Battlefield)) {
            AssertJUnit.assertTrue("Clone copied a Saproling, was " + clone.getName(), clone.getType().hasSubtype("Saproling"));
            AssertJUnit.assertFalse("without Life and Limb it is no land", clone.isLand());
            AssertJUnit.assertFalse("so Root Maze must not tap it", clone.isTapped());
        } else {
            AssertJUnit.assertTrue(clone.isInZone(ZoneType.Graveyard));
        }
    }

    @Test
    public void testCopyOfOtherCreatureIsNotAffectedByLifeAndLimb() {
        Player[] p = new Player[1];
        Game game = setup(p);
        addCard("Life and Limb", p[0]);
        addCard("Root Maze", p[0]);
        addCard("Runeclaw Bear", p[0]);
        game.getAction().checkStateEffects(true);

        Card clone = addCardToZone("Clone", p[0], ZoneType.Hand);
        Card onTable = game.getAction().moveToPlay(clone, null, AbilityKey.newMap());
        game.getAction().checkStateEffects(true);
        clone = onTable == null ? clone : onTable;

        if (clone.isInZone(ZoneType.Battlefield)) {
            // the only creature is the Bear, so a copy is a Bear: no Saproling, no land, no Root Maze
            AssertJUnit.assertEquals("Runeclaw Bear", clone.getName());
            AssertJUnit.assertFalse(clone.isLand());
            AssertJUnit.assertFalse(clone.isTapped());
        } else {
            AssertJUnit.assertTrue(clone.isInZone(ZoneType.Graveyard));
        }
    }

    @Test
    public void testPlainCloneEntersUntappedWhenItIsNotAffectedByAnything() {
        Player[] p = new Player[1];
        Game game = setup(p);
        addCard("Life and Limb", p[0]);
        addCard("Root Maze", p[0]);
        game.getAction().checkStateEffects(true);

        // no other creature: the copy has nothing to choose, so it is a 0/0 Clone that dies, and never a land
        Card clone = addCardToZone("Clone", p[0], ZoneType.Hand);
        Card onTable = game.getAction().moveToPlay(clone, null, AbilityKey.newMap());
        game.getAction().checkStateEffects(true);
        clone = onTable == null ? clone : onTable;
        if (clone.isInZone(ZoneType.Battlefield)) {
            AssertJUnit.assertFalse(clone.isLand());
            AssertJUnit.assertFalse(clone.isTapped());
        }
    }
}