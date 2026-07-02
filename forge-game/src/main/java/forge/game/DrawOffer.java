package forge.game;

import java.util.LinkedHashMap;
import java.util.Map;

import forge.game.player.Player;

/**
 * Transient state of an in-flight draw offer: the offerer (who accepts implicitly)
 * and each responder's vote. Pure data — no GUI or network references. Orchestration
 * lives in forge-gui's DrawOfferCoordinator.
 */
public class DrawOffer {
    public enum Vote { PENDING, ACCEPTED, DECLINED }

    private final Player offerer;
    private final Map<Player, Vote> votes = new LinkedHashMap<>();

    public DrawOffer(final Player offerer, final Iterable<Player> responders) {
        this.offerer = offerer;
        for (final Player p : responders) {
            votes.put(p, Vote.PENDING);
        }
    }

    public Player getOfferer() { return offerer; }
    public Map<Player, Vote> getVotes() { return votes; }

    public void record(final Player responder, final boolean accepted) {
        if (votes.containsKey(responder)) {
            votes.put(responder, accepted ? Vote.ACCEPTED : Vote.DECLINED);
        }
    }

    public boolean isDeclined() {
        return votes.containsValue(Vote.DECLINED);
    }

    public boolean isUnanimousAccept() {
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

    public boolean isSettled() {
        return isDeclined() || isUnanimousAccept();
    }
}
