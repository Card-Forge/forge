package forge.ai;

import java.util.Arrays;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.StifleCombo;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Stiflenought (Phyrexian Dreadnought + Stifle). Kept in a class of its own: it shares no deck lists or helpers
 * with the other Flat Monte Carlo tests.
 */
public class FlatMonteCarloStifleTest extends AITest {

    private CardCollection handOf(final Player p, final String... names) {
        final CardCollection hand = new CardCollection();
        for (final String n : names) {
            hand.add(addCardToZone(n, p, ZoneType.Hand));
        }
        return hand;
    }

    @Test
    public void stiflenoughtPiecesAreReadFromTheCards() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        AssertJUnit.assertTrue(StifleCombo.isSelfSacrificeUnless(addCardToZone("Phyrexian Dreadnought", p, ZoneType.Hand)));
        AssertJUnit.assertFalse(StifleCombo.isSelfSacrificeUnless(addCardToZone("Swords to Plowshares", p, ZoneType.Hand)));
        AssertJUnit.assertTrue(StifleCombo.isAbilityCounter(addCardToZone("Stifle", p, ZoneType.Hand)));
        AssertJUnit.assertTrue(StifleCombo.isAbilityCounter(addCardToZone("Consign to Memory", p, ZoneType.Hand)));
        AssertJUnit.assertFalse(StifleCombo.isAbilityCounter(addCardToZone("Force of Will", p, ZoneType.Hand)));
        AssertJUnit.assertFalse(StifleCombo.isAbilityCounter(addCardToZone("Daze", p, ZoneType.Hand)));
        AssertJUnit.assertFalse(StifleCombo.isAbilityCounter(addCardToZone("Disallow", p, ZoneType.Hand)));
    }

    @Test
    public void aDreadnoughtWithAConsignAndTwoLandsIsAHandWorthKeeping() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        AssertJUnit.assertEquals(25, StifleCombo.handBonus(handOf(p, "Island", "Tundra", "Phyrexian Dreadnought",
                "Consign to Memory", "Brainstorm", "Swords to Plowshares", "Ponder")));
    }

    @Test
    public void theDreadnoughtIsCastWithAConsignInHand() {
        final Game game = initAndCreateGame();
        final Player p = activePlayer(game);
        addCard("Underground Sea", p);
        addCard("Tundra", p);
        final Card dread = addCardToZone("Phyrexian Dreadnought", p, ZoneType.Hand);
        addCardToZone("Consign to Memory", p, ZoneType.Hand);
        final SpellAbility cast = dread.getFirstSpellAbility();
        AssertJUnit.assertTrue(StifleCombo.explain(p, cast), StifleCombo.readyToCast(p, cast));
        AssertJUnit.assertSame(cast, StifleCombo.sureCast(p, Arrays.asList(cast)));
    }

    @Test
    public void theDreadnoughtWaitsForTheSecondLandWithAConsign() {
        final Game game = initAndCreateGame();
        final Player p = activePlayer(game);
        addCard("Underground Sea", p);
        final Card dread = addCardToZone("Phyrexian Dreadnought", p, ZoneType.Hand);
        final SpellAbility cast = dread.getFirstSpellAbility();
        addCardToZone("Consign to Memory", p, ZoneType.Hand);
        AssertJUnit.assertFalse(StifleCombo.explain(p, cast), StifleCombo.readyToCast(p, cast));
        addCard("Tundra", p);
        AssertJUnit.assertTrue(StifleCombo.explain(p, cast), StifleCombo.readyToCast(p, cast));
    }

    @Test
    public void theConsignIsHeldWhileTheDreadnoughtIsInHand() {
        final Game game = initAndCreateGame();
        final Player p = activePlayer(game);
        final SpellAbility consign = addCardToZone("Consign to Memory", p, ZoneType.Hand).getFirstSpellAbility();
        AssertJUnit.assertFalse(StifleCombo.keepsForDreadnought(p, consign));
        addCardToZone("Phyrexian Dreadnought", p, ZoneType.Hand);
        AssertJUnit.assertTrue(StifleCombo.keepsForDreadnought(p, consign));
    }

    @Test
    public void aDreadnoughtWithAStifleAndTwoLandsIsAHandWorthKeeping() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        AssertJUnit.assertEquals(25, StifleCombo.handBonus(handOf(p, "Island", "Tundra", "Phyrexian Dreadnought",
                "Stifle", "Brainstorm", "Swords to Plowshares", "Ponder")));
        // no Stifle
        AssertJUnit.assertEquals(0, StifleCombo.handBonus(handOf(p, "Island", "Tundra", "Phyrexian Dreadnought",
                "Brainstorm", "Swords to Plowshares", "Ponder", "Daze")));
        // one land only
        AssertJUnit.assertEquals(0, StifleCombo.handBonus(handOf(p, "Island", "Phyrexian Dreadnought", "Stifle",
                "Brainstorm", "Swords to Plowshares", "Ponder", "Daze")));
    }

    /** The player whose turn it is, in the first main phase, so that a sorcery speed spell can be cast. */
    private Player activePlayer(final Game game) {
        final Player p = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        return p;
    }

    @Test
    public void theDreadnoughtIsCastOnceItAndTheStifleCanBothBePaidFor() {
        final Game game = initAndCreateGame();
        final Player p = activePlayer(game);
        addCard("Underground Sea", p);
        addCard("Tundra", p);
        final Card dread = addCardToZone("Phyrexian Dreadnought", p, ZoneType.Hand);
        addCardToZone("Stifle", p, ZoneType.Hand);
        final SpellAbility cast = dread.getFirstSpellAbility();
        AssertJUnit.assertTrue(StifleCombo.explain(p, cast), StifleCombo.readyToCast(p, cast));
        AssertJUnit.assertSame(cast, StifleCombo.sureCast(p, Arrays.asList(cast)));
    }

    @Test
    public void theDreadnoughtWaitsForTheSecondLandAndForTheStifle() {
        final Game game = initAndCreateGame();
        final Player p = activePlayer(game);
        addCard("Underground Sea", p);
        final Card dread = addCardToZone("Phyrexian Dreadnought", p, ZoneType.Hand);
        final SpellAbility cast = dread.getFirstSpellAbility();
        addCardToZone("Stifle", p, ZoneType.Hand);
        // one land: the Dreadnought and the Stifle together cost two
        AssertJUnit.assertFalse(StifleCombo.explain(p, cast), StifleCombo.readyToCast(p, cast));
        addCard("Tundra", p);
        AssertJUnit.assertTrue(StifleCombo.explain(p, cast), StifleCombo.readyToCast(p, cast));
    }

    @Test
    public void theStifleIsHeldWhileTheDreadnoughtIsInHand() {
        final Game game = initAndCreateGame();
        final Player p = activePlayer(game);
        final SpellAbility stifle = addCardToZone("Stifle", p, ZoneType.Hand).getFirstSpellAbility();
        AssertJUnit.assertFalse(StifleCombo.keepsForDreadnought(p, stifle));
        addCardToZone("Phyrexian Dreadnought", p, ZoneType.Hand);
        AssertJUnit.assertTrue(StifleCombo.keepsForDreadnought(p, stifle));
    }
}
