package forge.gamemodes.match;

import java.io.Serializable;
import java.util.List;

import forge.game.DrawOffer;
import forge.game.player.PlayerView;

/** Draw-offer protocol payloads: the client {@link Action} command and the server {@link Status} snapshot. */
public final class DrawOfferMessage {
    private DrawOfferMessage() {}

    /** Client -> server command. */
    public enum Action { OFFER, ACCEPT, DECLINE }

    /** Terminal outcome of an offer (null while voting). */
    public enum Result { ACCEPTED, DECLINED }

    public record Entry(PlayerView player, DrawOffer.Vote vote) implements Serializable {}

    /** Server -> client snapshot of the offer: offerer, every responder's vote, and the result once settled. */
    public record Status(PlayerView offerer, List<Entry> entries, Result result) implements Serializable {
        public boolean isPending(final PlayerView player) {
            return player != null && entries.stream()
                    .anyMatch(e -> player.equals(e.player()) && e.vote() == DrawOffer.Vote.PENDING);
        }
    }
}
