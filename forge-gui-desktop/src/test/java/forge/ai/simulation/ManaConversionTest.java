package forge.ai.simulation;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.MagicColor;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Signets and filter lands (Mystic Gate) make mana only when something else pays for them. The evaluator the
 * Flat Monte Carlo rollouts use must not count a lone one as a source, and must count one that works for what it
 * really adds: a mana for a Signet, only colors for a filter land.
 */
public class ManaConversionTest extends SimulationTest {
    private ManaConversion.Profile profile(final Player p) {
        return ManaConversion.analyze(p, true);
    }

    private static SpellAbility converterOf(final Card c) {
        for (final SpellAbility ma : c.getManaAbilities()) {
            if (ManaConversion.isConverter(ma)) {
                return ma;
            }
        }
        throw new AssertionError(c + " has no converter ability");
    }

    @Test
    public void aSignetAndAFilterLandAreConverters() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card signet = addCard("Azorius Signet", ai);
        final Card gate = addCard("Mystic Gate", ai);
        final Card plains = addCard("Plains", ai);

        AssertJUnit.assertNotNull(converterOf(signet));
        AssertJUnit.assertNotNull(converterOf(gate));
        for (final SpellAbility ma : plains.getManaAbilities()) {
            AssertJUnit.assertFalse("a basic land is not a converter", ManaConversion.isConverter(ma));
        }
    }

    @Test
    public void aLoneSignetMakesNoMana() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card signet = addCard("Azorius Signet", ai);

        final ManaConversion.Profile p = profile(ai);
        AssertJUnit.assertEquals("nothing pays the {1}", 0, p.mana);
        AssertJUnit.assertEquals("and so neither color can be made", 0, p.colors);
        AssertJUnit.assertTrue(p.isLocked(converterOf(signet)));
        AssertJUnit.assertTrue(p.isDead(signet));
    }

    @Test
    public void aSignetWithALandIsOneMoreManaAndTwoColors() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card signet = addCard("Azorius Signet", ai);
        addCard("Mountain", ai);

        final ManaConversion.Profile p = profile(ai);
        // the Mountain pays the {1}, the Signet makes {W}{U}
        AssertJUnit.assertEquals(2, p.mana);
        AssertJUnit.assertTrue(p.canMake(MagicColor.WHITE));
        AssertJUnit.assertTrue(p.canMake(MagicColor.BLUE));
        AssertJUnit.assertTrue(p.canMake(MagicColor.RED));
        AssertJUnit.assertFalse(p.isLocked(converterOf(signet)));
        AssertJUnit.assertFalse(p.isDead(signet));
    }

    @Test
    public void aLandCanStartAChainOfSignets() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Azorius Signet", ai);
        addCard("Dimir Signet", ai);
        addCard("Mountain", ai);

        final ManaConversion.Profile p = profile(ai);
        // 1 land + 2 Signets: each one pays itself with the mana of the one before and adds one
        AssertJUnit.assertEquals(3, p.mana);
        AssertJUnit.assertEquals(2, p.usable.size());
        AssertJUnit.assertTrue(p.locked.isEmpty());
    }

    @Test
    public void aSignetCannotPayForItself() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Azorius Signet", ai);
        addCard("Dimir Signet", ai);

        final ManaConversion.Profile p = profile(ai);
        AssertJUnit.assertEquals("two Signets and no land: they only wait for each other", 0, p.mana);
        AssertJUnit.assertEquals(2, p.locked.size());
    }

    @Test
    public void aFilterLandWorksWhenTheRightColorGoesIn() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card gate = addCard("Mystic Gate", ai);
        addCard("Island", ai);

        final ManaConversion.Profile p = profile(ai);
        AssertJUnit.assertFalse("the Island pays {W/U}", p.isLocked(converterOf(gate)));
        AssertJUnit.assertTrue(p.canMake(MagicColor.WHITE));
        AssertJUnit.assertTrue(p.canMake(MagicColor.BLUE));
        // a filter land changes mana, it does not add any: the Island's and the Gate's own {C}
        AssertJUnit.assertEquals(2, p.mana);
    }

    @Test
    public void aFilterLandWithTheWrongColorStaysLocked() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card gate = addCard("Mystic Gate", ai);
        addCard("Mountain", ai);

        final ManaConversion.Profile p = profile(ai);
        AssertJUnit.assertTrue("red can not pay {W/U}", p.isLocked(converterOf(gate)));
        AssertJUnit.assertFalse(p.canMake(MagicColor.WHITE));
        AssertJUnit.assertFalse(p.canMake(MagicColor.BLUE));
        AssertJUnit.assertTrue(p.canMake(MagicColor.RED));
        AssertJUnit.assertFalse("the Gate still taps for {C}, so it is not dead", p.isDead(gate));
        AssertJUnit.assertEquals(2, p.mana);
    }

    @Test
    public void aLoneFilterLandOnlyMakesColorless() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card gate = addCard("Mystic Gate", ai);

        final ManaConversion.Profile p = profile(ai);
        AssertJUnit.assertTrue(p.isLocked(converterOf(gate)));
        AssertJUnit.assertEquals(1, p.mana);
        AssertJUnit.assertEquals(0, p.colors);
    }

    @Test
    public void tappedSourcesCountOnlyInTheManaBaseView() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card signet = addCard("Azorius Signet", ai);
        final Card mountain = addCard("Mountain", ai);
        mountain.tap(true, null, null);

        AssertJUnit.assertEquals("judging the mana base, the land will untap", 2, ManaConversion.analyze(ai, true).mana);
        final ManaConversion.Profile now = ManaConversion.analyze(ai, false);
        AssertJUnit.assertEquals("right now nothing can pay for the Signet", 0, now.mana);
        AssertJUnit.assertTrue(now.isLocked(converterOf(signet)));
    }

    @Test
    public void usableManaIsWhatTheEvaluatorsRead() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Azorius Signet", ai);
        AssertJUnit.assertEquals(0, ManaConversion.usableMana(ai));
        addCard("Plains", ai);
        AssertJUnit.assertEquals(2, ManaConversion.usableMana(ai));
    }
}
