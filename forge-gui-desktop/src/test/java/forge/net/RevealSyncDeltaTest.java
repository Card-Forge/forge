package forge.net;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gamemodes.net.DeltaPacket;
import forge.gamemodes.net.server.DeltaSyncManager;
import forge.trackable.TrackableProperty;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * Reusable harness for verifying that host-side game-state changes actually reach a
 * networked client through the real delta-sync path ({@link DeltaSyncManager}).
 *
 * <p>Pattern for tests of this type:
 * <ol>
 *   <li>Build an exact board with {@link AITest} helpers (deterministic — no AI, no sockets).</li>
 *   <li>Take an initial full sync with a fresh {@link DeltaSyncManager} (client is "caught up").</li>
 *   <li>Mutate the host state (the thing under test).</li>
 *   <li>Collect the incremental delta and assert the change is present in it — i.e. the client
 *       would receive it.</li>
 * </ol>
 *
 * <p>The first scenario here is the "own top-library card reveal" sync: a Future Sight controller
 * must be able to see the revealed top card of their own (hidden) library on their client. This runs
 * the SAME delta code the iOS/desktop network clients use (default {@code useDeltaSync=true}), but
 * deterministically and in-process.
 *
 * <p>Why a dedicated test: a delta regression that drops a hidden-zone reveal is otherwise
 * invisible — the periodic network checksums did not walk the Library zone.
 */
public class RevealSyncDeltaTest extends AITest {

    /** Find the delta entry (new-object or incremental) for a given trackable id, or null. */
    private static Map<TrackableProperty, Object> deltaFor(DeltaPacket packet, int cardViewId) {
        int key = DeltaPacket.makeDeltaKey(DeltaPacket.TYPE_CARD_VIEW, cardViewId);
        Map<TrackableProperty, Object> d = packet.getObjectDeltas().get(key);
        if (d == null) {
            d = packet.getNewObjects().get(key);
        }
        return d;
    }

    @Test
    public void topLibraryRevealReachesClientDelta() {
        Game game = initAndCreateGame();
        Player a = game.getPlayers().get(1); // active player; the "reveal owner"

        // One card in A's (hidden) library — this is the top card Future Sight reveals.
        addCardToZone("Island", a, ZoneType.Library);
        game.getAction().checkStateEffects(true);

        GameView gv = game.getView();
        // Realize the library CardView so it exists in the view graph before the initial sync.
        CardView topBefore = gv.getPlayers().get(1).getCards(ZoneType.Library).iterator().next();
        PlayerView aView = gv.getPlayers().get(1);
        int topId = topBefore.getId();

        Assert.assertFalse(topBefore.canBeShownTo(aView),
                "Precondition: with no reveal source, A must NOT see its own top library card");

        // 1. Initial full sync — the client is now caught up (no reveal yet).
        DeltaSyncManager sync = new DeltaSyncManager();
        sync.collectDeltas(gv);

        // 2. Host-side change under test: Future Sight enters A's battlefield and its continuous
        //    static grants A may-look on the top library card.
        addCard("Future Sight", a);
        game.getAction().checkStateEffects(true);

        // Host truth: A can now see its own top library card.
        Assert.assertTrue(topBefore.canBeShownTo(aView),
                "Host state: with Future Sight in play, A should see its own top library card");

        // 3. Collect the incremental delta the client would receive.
        sync.registerNewObjects(gv);
        DeltaPacket delta = sync.collectDeltas(gv);

        // 4. The decisive check: does the reveal (PlayerMayLook on the top library CardView)
        //    actually travel in the delta? If yes, the client syncs the reveal.
        Map<TrackableProperty, Object> cardDelta = deltaFor(delta, topId);
        boolean mayLookInDelta = cardDelta != null && cardDelta.containsKey(TrackableProperty.PlayerMayLook);

        System.out.println("[REVEALSYNC] top library CardView id=" + topId
                + " present-in-delta=" + (cardDelta != null)
                + " PlayerMayLook-in-delta=" + mayLookInDelta
                + (cardDelta != null ? " props=" + cardDelta.keySet() : ""));

        Assert.assertTrue(mayLookInDelta,
                "The top library card's PlayerMayLook reveal must be present in the client delta, "
                        + "otherwise the owning player never sees their own revealed top card. "
                        + "cardDelta=" + cardDelta);
    }
}
