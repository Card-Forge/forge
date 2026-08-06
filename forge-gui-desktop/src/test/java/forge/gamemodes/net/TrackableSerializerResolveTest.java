package forge.gamemodes.net;

import forge.game.card.CardView;
import forge.trackable.TrackableTypes;
import forge.trackable.Tracker;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Pins the resolution contract of {@link TrackableSerializer#resolve}: a
 * resolvable {@link IdRef} returns the tracked object, an unresolvable one
 * must not return null (which would silently drop a selection and soft-lock
 * the waiting input). Regression guard for issue #82.
 */
public class TrackableSerializerResolveTest {

    @Test
    public void testResolvableIdRefReturnsTrackedObject() {
        final Tracker tracker = new Tracker();
        final CardView card = new CardView(5, tracker);
        tracker.putObj(TrackableTypes.CardViewType, 5, card);

        final Object resolved = TrackableSerializer.resolve(
                new TrackableSerializer.IdRef(TrackableSerializer.TYPE_CARD_VIEW, 5), tracker);

        Assert.assertSame(resolved, card, "A resolvable IdRef must return the tracked object");
    }

    @Test
    public void testUnresolvableCardViewIdRefFallsBackToDetachedCardView() {
        final Tracker tracker = new Tracker();

        final Object resolved = TrackableSerializer.resolve(
                new TrackableSerializer.IdRef(TrackableSerializer.TYPE_CARD_VIEW, 368), tracker);

        Assert.assertNotNull(resolved, "An unresolvable CardView IdRef must not resolve to null");
        Assert.assertTrue(resolved instanceof CardView, "Fallback must be a CardView");
        Assert.assertEquals(((CardView) resolved).getId(), 368,
                "The detached CardView must carry the original id for findByView");
    }

    @Test
    public void testUnresolvablePlayerViewIdRefStillReturnsNull() {
        final Tracker tracker = new Tracker();

        final Object resolved = TrackableSerializer.resolve(
                new TrackableSerializer.IdRef(TrackableSerializer.TYPE_PLAYER_VIEW, 2), tracker);

        Assert.assertNull(resolved, "Unresolvable PlayerView keeps the historical null contract");
    }
}
