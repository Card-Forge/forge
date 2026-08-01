package forge.game.replacement;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * The tap/untap/produce mana scan only visits the zones one of those replacements can be active
 * from, so a card that declares ActiveZones$ outside the battlefield is still found there.
 */
public class ReplacementScanZoneTest extends AITest {

    @Test
    public void aReplacementCanAskForAnotherZone() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, me);

        Card looter = addCard("Merfolk Looter", me);
        game.getAction().checkStateEffects(true);
        assertTrue("nothing is stopping it untapping yet", looter.canUntap(me, true));

        Card ghost = addCardToZone("Ornithopter", me, ZoneType.Graveyard);
        ghost.addReplacementEffect(ReplacementHandler.parseReplacement(
                "Event$ Untap | ActiveZones$ Graveyard | ValidCard$ Creature"
                        + " | ValidStepTurnToController$ You | Layer$ CantHappen", ghost, true));
        game.getAction().checkStateEffects(true);

        assertFalse("a graveyard declaration is honoured", looter.canUntap(me, true));
    }
}
