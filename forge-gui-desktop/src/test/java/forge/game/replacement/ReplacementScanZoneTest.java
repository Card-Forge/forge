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

    /**
     * A replacement that declares no ActiveZones$ is active in every zone, library included, so
     * before this change an untap scan reached one buried in a library. Nothing in the pool relies
     * on that - the 49 undeclared Untap effects are all "doesn't untap during your untap step" on a
     * permanent - and this is the narrowing the skip actually makes.
     */
    @Test
    public void anUndeclaredReplacementNoLongerReachesOutOfTheLibrary() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, me);

        Card looter = addCard("Merfolk Looter", me);
        game.getAction().checkStateEffects(true);
        assertTrue("nothing is stopping it untapping yet", looter.canUntap(me, true));

        Card buried = addCardToZone("Ornithopter", me, ZoneType.Library);
        buried.addReplacementEffect(ReplacementHandler.parseReplacement(
                "Event$ Untap | ValidCard$ Creature"
                        + " | ValidStepTurnToController$ You | Layer$ CantHappen", buried, true));
        game.getAction().checkStateEffects(true);

        assertTrue("a library card is not scanned for untap replacements",
                looter.canUntap(me, true));
    }
}
