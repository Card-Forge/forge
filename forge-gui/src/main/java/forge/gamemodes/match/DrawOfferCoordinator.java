package forge.gamemodes.match;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import forge.ai.PlayerControllerAi;
import forge.game.DrawOffer;
import forge.game.Game;
import forge.game.GameEndReason;
import forge.game.player.Player;
import forge.gui.interfaces.IGuiGame;
import forge.player.PlayerControllerHuman;

/** Orchestrates an in-flight draw offer: build, broadcast tally, collect votes, resolve. */
public final class DrawOfferCoordinator {
    private DrawOfferCoordinator() {}

    /** Called when a player activates Offer Draw. Ignored if one is already in flight. */
    public static synchronized void offer(final Game game, final Player offerer) {
        if (game.isGameOver() || offerer == null) {
            return;
        }
        final DrawOffer existing = game.getDrawOffer();
        if (existing != null) {
            final boolean offererPresent = game.getPlayers().contains(existing.getOfferer());
            final boolean anyResponderPresent = existing.getVotes().keySet().stream().anyMatch(game.getPlayers()::contains);
            if (offererPresent && anyResponderPresent) {
                return; // a viable offer is already in flight
            }
            game.setDrawOffer(null); // stale (offerer/responders left) — supersede it
        }
        final List<Player> responders = new ArrayList<>(game.getPlayers());
        responders.remove(offerer);
        if (responders.isEmpty()) {
            return;
        }
        final DrawOffer offer = new DrawOffer(offerer, responders);
        game.setDrawOffer(offer);

        for (final Player p : responders) {
            if (p.getController() instanceof PlayerControllerAi ai) {
                offer.record(p, ai.acceptsDrawOffer());
            }
        }

        broadcast(game, offer);
        if (offer.isSettled()) {
            resolve(game, offer);
        }
    }

    /** Called when a human responder votes. */
    public static synchronized void respond(final Game game, final Player responder, final boolean accept) {
        final DrawOffer offer = game.getDrawOffer();
        if (offer == null || game.isGameOver()) {
            return; // stale / already settled
        }
        offer.record(responder, accept);
        broadcast(game, offer);
        if (offer.isSettled()) {
            resolve(game, offer);
        }
    }

    private static void broadcast(final Game game, final DrawOffer offer) {
        broadcast(game, offer, null);
    }
    private static void broadcast(final Game game, final DrawOffer offer, final DrawOfferMessage.Result result) {
        // Drop any responder who has left the game since the offer was made.
        offer.getVotes().keySet().removeIf(p -> !game.getPlayers().contains(p));

        final List<DrawOfferMessage.Entry> entries = new ArrayList<>();
        for (final Map.Entry<Player, DrawOffer.Vote> e : offer.getVotes().entrySet()) {
            entries.add(new DrawOfferMessage.Entry(e.getKey().getView(), e.getValue()));
        }
        final DrawOfferMessage.Status update = new DrawOfferMessage.Status(offer.getOfferer().getView(), entries, result);
        for (final Player p : game.getRegisteredPlayers()) {
            if (p.getController() instanceof PlayerControllerHuman pch) {
                final IGuiGame gui = pch.getGui();
                if (gui != null) {
                    gui.updateDrawOffer(update);
                }
            }
        }
    }

    private static void resolve(final Game game, final DrawOffer offer) {
        if (game.isGameOver()) {
            game.setDrawOffer(null);
            return;
        }
        final boolean accepted = offer.isUnanimousAccept();
        broadcast(game, offer, accepted ? DrawOfferMessage.Result.ACCEPTED : DrawOfferMessage.Result.DECLINED);
        game.setDrawOffer(null);
        if (!accepted) {
            return;
        }
        // Atomic: only now do we touch outcomes. Apply to every alive player, then end directly.
        for (final Player p : game.getPlayers()) {
            p.intentionalDraw();
        }
        game.setGameOver(GameEndReason.Draw);
        releaseInputQueues(game);
    }

    /** Mirror concede: remote-client controllers have no event handler, so release their input queues. */
    private static void releaseInputQueues(final Game game) {
        if (!game.isGameOver()) {
            return;
        }
        for (final Player p : game.getRegisteredPlayers()) {
            if (p.getController() instanceof PlayerControllerHuman pch) {
                pch.getInputQueue().onGameOver(true);
            }
        }
    }
}
