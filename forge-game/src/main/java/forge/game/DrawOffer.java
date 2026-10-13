package forge.game;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import forge.game.player.Player;

/**
 * Transient state of an in-flight draw offer: the offerer (who accepts implicitly),
 * or a reason when the game itself offers, and each responder's vote. No GUI or
 * network references. Votes arrive from other threads, so access is synchronized.
 */
public class DrawOffer {
    public enum Vote { PENDING, ACCEPTED, DECLINED }

    private final Player offerer;
    private final String reason;
    private final Map<Player, Vote> votes = new LinkedHashMap<>();

    public DrawOffer(final Player offerer, final Iterable<Player> responders) {
        this(offerer, null, responders);
    }

    /** An offer the game makes itself has no offerer and gives its reason instead. */
    public DrawOffer(final Player offerer, final String reason, final Iterable<Player> responders) {
        this.offerer = offerer;
        this.reason = reason;
        for (final Player p : responders) {
            votes.put(p, Vote.PENDING);
        }
    }

    public Player getOfferer() { return offerer; }
    public String getReason() { return reason; }
    public synchronized Map<Player, Vote> getVotes() { return new LinkedHashMap<>(votes); }

    public synchronized void record(final Player responder, final boolean accepted) {
        if (votes.containsKey(responder)) {
            votes.put(responder, accepted ? Vote.ACCEPTED : Vote.DECLINED);
        }
        notifyAll();
    }

    /** Waits up to {@code millis} for the last vote and returns whether the offer is settled. */
    public synchronized boolean awaitSettled(final long millis) throws InterruptedException {
        if (!isSettled()) {
            wait(millis);
        }
        return isSettled();
    }

    /** Drops the vote of anyone not in {@code present}. */
    public synchronized void retainResponders(final Collection<Player> present) {
        votes.keySet().retainAll(present);
        notifyAll();
    }

    public synchronized boolean isDeclined() {
        return votes.containsValue(Vote.DECLINED);
    }

    public synchronized boolean isUnanimousAccept() {
        if (votes.isEmpty()) {
            return false;
        }
        for (final Vote v : votes.values()) {
            if (v != Vote.ACCEPTED) {
                return false;
            }
        }
        return true;
    }

    public synchronized boolean isSettled() {
        return isDeclined() || isUnanimousAccept();
    }
}
