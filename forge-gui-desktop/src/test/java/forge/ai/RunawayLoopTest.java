package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Felidar Guardian blinks Saheeli Rai, which comes back as a new planeswalker with a fresh count of activations, so the
 * AI copied the Guardian for ever until the heap was full. The count is kept by the card's name and the ability.
 */
public class RunawayLoopTest extends AITest {
    @Test
    public void anAbilityThatKeepsComingBackIsStoppedAfterTheLimit() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card saheeli = addCardToZone("Saheeli Rai", ai, ZoneType.Battlefield);
        SpellAbility copy = null;
        for (final SpellAbility sa : saheeli.getSpellAbilities()) {
            if (sa.isActivatedAbility() && sa.getApi() == forge.game.ability.ApiType.CopyPermanent) {
                copy = sa;
            }
        }
        AssertJUnit.assertNotNull("Saheeli's copy ability", copy);
        copy.setActivatingPlayer(ai);
        for (int i = 0; i < RunawayLoop.LIMIT - 1; i++) {
            RunawayLoop.note(ai, copy);
        }
        AssertJUnit.assertFalse("one activation short of the limit", RunawayLoop.exceeded(copy));
        RunawayLoop.note(ai, copy);
        AssertJUnit.assertTrue("at the limit", RunawayLoop.exceeded(copy));
        AssertJUnit.assertTrue("and the AI is told to stop", ComputerUtil.preventRunAwayActivations(copy));
    }
}
