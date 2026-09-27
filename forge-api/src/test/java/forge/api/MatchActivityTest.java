package forge.api;

import forge.game.card.CardView;
import forge.game.event.*;
import forge.game.player.PlayerView;
import forge.game.phase.PhaseType;
import forge.game.zone.ZoneType;
import forge.game.zone.ZoneView;
import forge.trackable.TrackableProperty;
import forge.util.Localizer;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class MatchActivityTest {
    @BeforeClass public void language() { Localizer.getInstance().initialize("en-US", "../forge-gui/res/languages"); }

    @Test public void copiesEventsWithoutLeakingHiddenCardsAndResetsTheirHandles() {
        var viewer = new PlayerView(1, null);
        var opponent = new PlayerView(2, null);
        opponent.set(TrackableProperty.Name, "Verdant");
        var card = new CardView(99, null, "Secret identity");
        card.set(TrackableProperty.Controller, opponent);
        card.set(TrackableProperty.Zone, ZoneType.Hand);
        var activity = new MatchActivity(viewer);
        activity.onGameEvent(new GameEventTurnBegan(opponent, 2));
        activity.onGameEvent(new GameEventCardChangeZone(card, new ZoneView(opponent, ZoneType.Library), new ZoneView(opponent, ZoneType.Hand)));
        assertFalse(activity.snapshot().toString().contains("Secret identity"));
        assertNull(activity.visualId(card));
        card.set(TrackableProperty.Zone, ZoneType.Battlefield);
        activity.onGameEvent(new GameEventLandPlayed(opponent, card));
        var snapshot = activity.snapshot();
        var entry = snapshot.get(snapshot.size() - 1);
        assertEquals(entry.message(), "Verdant played Secret identity.");
        assertEquals(entry.turn(), 2);
        assertEquals(entry.cardId(), activity.visualId(card));
        assertNotEquals(entry.cardId(), "99");
        // Events can arrive before a frozen view updates its zone.
        activity.onGameEvent(new GameEventCardChangeZone(card, new ZoneView(opponent, ZoneType.Battlefield), new ZoneView(opponent, ZoneType.Library)));
        var movement = activity.snapshot().get(activity.snapshot().size() - 1);
        assertNull(movement.cardId());
        assertNull(movement.cardName());
        assertFalse(movement.message().contains("Secret identity"));
        card.set(TrackableProperty.Zone, ZoneType.Library);
        assertNull(activity.visualId(card));
        card.set(TrackableProperty.Zone, ZoneType.Battlefield);
        assertNotEquals(activity.visualId(card), entry.cardId(), "Hidden-zone transitions must break identity correlation");
        card.set(TrackableProperty.Facedown, true);
        activity.onGameEvent(new GameEventLandPlayed(opponent, card));
        var faceDown = activity.snapshot().get(activity.snapshot().size() - 1);
        assertNull(faceDown.cardId());
        assertNull(faceDown.cardName());
        assertFalse(faceDown.message().contains("Secret identity"));
        assertEquals(snapshot.size(), 3, "Published history must be immutable and detached");
    }

    @Test public void retainsOrderedHistoryAcrossPollsWithBoundedMemory() {
        var viewer = new PlayerView(1, null);
        var activity = new MatchActivity(viewer);
        activity.onGameEvent(new GameEventTurnBegan(viewer, 1));
        for (int i = 0; i < 150; i++) activity.onGameEvent(new GameEventPlayerLivesChanged(viewer, 20, 19));
        var history = activity.snapshot();
        assertEquals(history.size(), 120);
        assertEquals(history.get(119).id(), 151L);
        assertEquals(history.get(0).id(), 32L);
        assertEquals(history.get(119).message(), "You lost 1 life (19 remaining).");
        assertEquals(history, activity.snapshot());
    }

    @Test public void copiesPhaseAndTurnWithoutAddingNoisyHistoryEntries() {
        var viewer = new PlayerView(1, null);
        var activity = new MatchActivity(viewer);
        activity.onGameEvent(new GameEventTurnBegan(viewer, 3));
        var before = activity.frame();
        activity.onGameEvent(new GameEventTurnPhase(viewer, PhaseType.COMBAT_DECLARE_BLOCKERS, ""));
        var after = activity.frame();
        assertEquals(after.turn(), 3);
        assertEquals(after.activePlayerId(), Integer.valueOf(1));
        assertEquals(after.phaseKey(), "COMBAT_DECLARE_BLOCKERS");
        assertTrue(after.revision() > before.revision());
        assertEquals(after.entries(), before.entries());
        assertEquals(before.phaseKey(), "UNTAP", "Earlier frames must remain detached");
    }
}
