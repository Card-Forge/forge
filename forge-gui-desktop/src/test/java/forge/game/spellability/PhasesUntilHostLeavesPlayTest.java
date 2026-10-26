package forge.game.spellability;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class PhasesUntilHostLeavesPlayTest extends AITest {

    @Test
    public void outOfTimePhasesBackInWhenControllerLeavesMultiplayerGame() {
        Game game = initAndCreateThreePlayerGame();
        Player p0 = game.getPlayers().get(0);
        Player p1 = game.getPlayers().get(1);
        Player p2 = game.getPlayers().get(2);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p1);

        Card bears = addCard("Grizzly Bears", p0);
        Card lions = addCard("Savannah Lions", p2);
        settle(game);

        Card outOfTime = addCardToZone("Out of Time", p1, ZoneType.Hand);
        game.getAction().moveToPlay(outOfTime, null, AbilityKey.newMap());
        settle(game);
        playUntilStackClear(game);

        assertTrue(bears.isPhasedOut(), "Grizzly Bears should be phased out by Out of Time");
        assertTrue(lions.isPhasedOut(), "Savannah Lions should be phased out by Out of Time");

        // Player 1 leaves the game
        game.onPlayerLost(p1);
        settle(game);

        assertFalse(bears.isPhasedOut(), "Grizzly Bears must phase back in when Out of Time's controller leaves the game");
        assertFalse(lions.isPhasedOut(), "Savannah Lions must phase back in when Out of Time's controller leaves the game");
    }

    @Test
    public void outOfTimePhasesBackInWhenDestroyed() {
        Game game = initAndCreateGame();
        Player p0 = game.getPlayers().get(0);
        Player p1 = game.getPlayers().get(1);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p0);

        Card bears = addCard("Grizzly Bears", p1);
        settle(game);

        Card outOfTime = addCardToZone("Out of Time", p0, ZoneType.Hand);
        game.getAction().moveToPlay(outOfTime, null, AbilityKey.newMap());
        settle(game);
        playUntilStackClear(game);

        assertTrue(bears.isPhasedOut(), "Grizzly Bears should be phased out");

        game.getAction().destroy(outOfTime, null, false, null);
        settle(game);
        playUntilStackClear(game);

        assertFalse(bears.isPhasedOut(), "Grizzly Bears must phase back in when Out of Time leaves the battlefield");
    }

    @Test
    public void oubliettePhasesBackInTappedWhenHostLeavesPlay() {
        Game game = initAndCreateGame();
        Player p0 = game.getPlayers().get(0);
        Player p1 = game.getPlayers().get(1);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p0);

        Card bears = addCard("Grizzly Bears", p1);
        bears.setTapped(false);
        settle(game);

        Card oubliette = addCardToZone("Oubliette", p0, ZoneType.Hand);
        game.getAction().moveToPlay(oubliette, null, AbilityKey.newMap());
        settle(game);
        playUntilStackClear(game);

        assertTrue(bears.isPhasedOut(), "Grizzly Bears should be phased out by Oubliette");

        game.getAction().destroy(oubliette, null, false, null);
        settle(game);
        playUntilStackClear(game);

        assertFalse(bears.isPhasedOut(), "Grizzly Bears must phase back in when Oubliette leaves the battlefield");
        assertTrue(bears.isTapped(), "Grizzly Bears must be tapped as it phases in via Oubliette");
    }

    @Test
    public void theMomentPhasesBackInWhenHostLeavesPlay() {
        Game game = initAndCreateGame();
        Player p0 = game.getPlayers().get(0);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p0);

        Card bears = addCard("Grizzly Bears", p0);
        Card moment = addCard("The Moment", p0);
        settle(game);

        SpellAbility sa = findSAWithPrefix(moment, "{2}, {T}:");
        sa.setActivatingPlayer(p0);
        sa.resetTargets();
        sa.getTargets().add(bears);
        AbilityUtils.resolve(sa);
        settle(game);

        assertTrue(bears.isPhasedOut(), "Grizzly Bears should be phased out by The Moment");

        game.getAction().destroy(moment, null, false, null);
        settle(game);

        assertFalse(bears.isPhasedOut(), "Grizzly Bears must phase back in when The Moment leaves the battlefield");
    }

    @Test
    public void wontPhaseInNormalIsClearedEvenIfPhaseInPreventedByExternalEffect() {
        Game game = initAndCreateGame();
        Player p0 = game.getPlayers().get(0);
        Player p1 = game.getPlayers().get(1);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p0);

        Card bears = addCard("Grizzly Bears", p1);
        settle(game);

        Card outOfTime = addCardToZone("Out of Time", p0, ZoneType.Hand);
        game.getAction().moveToPlay(outOfTime, null, AbilityKey.newMap());
        settle(game);
        playUntilStackClear(game);

        assertTrue(bears.isPhasedOut(), "Grizzly Bears should be phased out");
        assertTrue(bears.isWontPhaseInNormal(), "Grizzly Bears should have wontPhaseInNormal = true while Out of Time is active");

        // Add Disciple of Caelus Nin ("Permanents can't phase in")
        Card disciple = addCard("Disciple of Caelus Nin", p0);
        settle(game);

        // Destroy Out of Time while Disciple of Caelus Nin prevents phasing in
        game.getAction().destroy(outOfTime, null, false, null);
        settle(game);
        playUntilStackClear(game);

        // Bears should still be phased out because of Disciple of Caelus Nin
        assertTrue(bears.isPhasedOut(), "Grizzly Bears cannot phase in yet due to Disciple of Caelus Nin");
        // BUT wontPhaseInNormal must have been reset to false so that it can phase in during future untap steps
        assertFalse(bears.isWontPhaseInNormal(), "wontPhaseInNormal must be reset to false when Out of Time leaves");

        // Remove Disciple of Caelus Nin
        game.getAction().destroy(disciple, null, false, null);
        settle(game);

        // Now during untap step, it phases in normally
        bears.phase(true);
        assertFalse(bears.isPhasedOut(), "Grizzly Bears should phase in once anti-phasing restriction is gone");
    }

    private void settle(Game game) {
        game.getAction().checkStateEffects(true);
    }
}
