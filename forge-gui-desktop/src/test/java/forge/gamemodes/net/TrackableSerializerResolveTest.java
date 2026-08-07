package forge.gamemodes.net;

import forge.game.card.CardView;
import forge.game.player.PlayerView;
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

    @Test
    public void testUnresolvableCardViewFallbackUsesSameTracker() {
        final Tracker tracker = new Tracker();

        final Object resolved = TrackableSerializer.resolve(
                new TrackableSerializer.IdRef(TrackableSerializer.TYPE_CARD_VIEW, 42), tracker);

        Assert.assertTrue(resolved instanceof CardView);
        Assert.assertSame(((CardView) resolved).getTracker(), tracker,
                "The detached fallback CardView must be wired to the same Tracker instance "
                        + "so later property lookups behave consistently");
    }

    @Test
    public void testUnresolvableCardViewFallbackCreatesFreshInstanceEachCall() {
        final Tracker tracker = new Tracker();
        final TrackableSerializer.IdRef ref =
                new TrackableSerializer.IdRef(TrackableSerializer.TYPE_CARD_VIEW, 7);

        final Object first = TrackableSerializer.resolve(ref, tracker);
        final Object second = TrackableSerializer.resolve(ref, tracker);

        Assert.assertNotSame(first, second,
                "Each unresolved lookup must produce its own detached CardView, not a cached singleton");
    }

    @Test
    public void testUnresolvableCardViewIdRefWithZeroIdFallsBack() {
        final Tracker tracker = new Tracker();

        final Object resolved = TrackableSerializer.resolve(
                new TrackableSerializer.IdRef(TrackableSerializer.TYPE_CARD_VIEW, 0), tracker);

        Assert.assertTrue(resolved instanceof CardView);
        Assert.assertEquals(((CardView) resolved).getId(), 0);
    }

    @Test
    public void testUnresolvableCardViewIdRefWithNegativeIdFallsBack() {
        final Tracker tracker = new Tracker();

        final Object resolved = TrackableSerializer.resolve(
                new TrackableSerializer.IdRef(TrackableSerializer.TYPE_CARD_VIEW, -1), tracker);

        Assert.assertTrue(resolved instanceof CardView, "Fallback must apply regardless of id sign");
        Assert.assertEquals(((CardView) resolved).getId(), -1);
    }

    @Test
    public void testUnknownTypeTagDoesNotTriggerCardViewFallback() {
        final Tracker tracker = new Tracker();
        final byte unknownTag = (byte) 99;

        final Object resolved = TrackableSerializer.resolve(
                new TrackableSerializer.IdRef(unknownTag, 5), tracker);

        Assert.assertNull(resolved,
                "The CardView fallback must be gated on TYPE_CARD_VIEW specifically, "
                        + "not applied to arbitrary/unknown type tags");
    }

    @Test
    public void testResolvablePlayerViewIdRefStillReturnsTrackedObject() {
        final Tracker tracker = new Tracker();
        final PlayerView player = new PlayerView(3, tracker);
        tracker.putObj(TrackableTypes.PlayerViewType, 3, player);

        final Object resolved = TrackableSerializer.resolve(
                new TrackableSerializer.IdRef(TrackableSerializer.TYPE_PLAYER_VIEW, 3), tracker);

        Assert.assertSame(resolved, player,
                "The new CardView fallback branch must not affect resolvable PlayerView lookups");
    }

    @Test
    public void testNonIdRefObjectPassesThroughUnchanged() {
        final Tracker tracker = new Tracker();
        final String plain = "not-an-idref";

        final Object resolved = TrackableSerializer.resolve(plain, tracker);

        Assert.assertSame(resolved, plain, "Objects that are not IdRef/EventCardRef must pass through unchanged");
    }
}
