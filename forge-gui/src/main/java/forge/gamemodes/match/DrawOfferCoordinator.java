package forge.gamemodes.match;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import forge.ai.PlayerControllerAi;
import forge.game.DrawOffer;
import forge.game.Game;
import forge.game.GameEndReason;
import forge.game.player.Player;
import forge.game.player.PlayerView;
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
            if (existing.getOfferer() == null) {
                return;
            }
            final boolean offererPresent = game.getPlayers().contains(existing.getOfferer());
            final boolean anyResponderPresent = existing.getVotes().keySet().stream().anyMatch(game.getPlayers()::contains);
            if (offererPresent && anyResponderPresent) {
                // a viable offer is already in flight: show it again to whoever still has to answer
                broadcast(game, existing);
                return;
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

    /**
     * Called on the game thread when the game may be in an endless loop. Every player votes and AI players accept.
     * Returns whether they all accepted, and leaves ending the game to the caller.
     */
    public static boolean offerForLoop(final Game game, final String reason) {
        final DrawOffer offer = new DrawOffer(null, reason, game.getPlayers());
        synchronized (DrawOfferCoordinator.class) {
            if (game.isGameOver() || game.getDrawOffer() != null) {
                return false;
            }
            game.setDrawOffer(offer);
            acceptForAi(offer);
            broadcast(game, offer);
        }
        try {
            while (game.getDrawOffer() == offer) {
                Thread.sleep(250);
                synchronized (DrawOfferCoordinator.class) {
                    // no vote arrives from a player who has left the game or whose seat the AI has taken over
                    dropAbsentResponders(game, offer);
                    acceptForAi(offer);
                    if (game.getDrawOffer() == offer && (game.isGameOver() || offer.isSettled())) {
                        resolve(game, offer);
                    }
                }
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            game.setDrawOffer(null);
        }
        return !game.isGameOver() && offer.isUnanimousAccept();
    }

    private static void acceptForAi(final DrawOffer offer) {
        for (final Player p : offer.getVotes().keySet()) {
            if (p.getController().isAI()) {
                offer.record(p, true);
            }
        }
    }

    private static void dropAbsentResponders(final Game game, final DrawOffer offer) {
        offer.getVotes().keySet().removeIf(p -> !game.getPlayers().contains(p));
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
        dropAbsentResponders(game, offer);

        final List<DrawOfferMessage.Entry> entries = new ArrayList<>();
        for (final Map.Entry<Player, DrawOffer.Vote> e : offer.getVotes().entrySet()) {
            entries.add(new DrawOfferMessage.Entry(e.getKey().getView(), e.getValue()));
        }
        final DrawOfferMessage.Status update = new DrawOfferMessage.Status(PlayerView.get(offer.getOfferer()), offer.getReason(), entries, result);
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
        if (!accepted || offer.getOfferer() == null) {
            // the game thread ends a game that it offered to draw itself
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
