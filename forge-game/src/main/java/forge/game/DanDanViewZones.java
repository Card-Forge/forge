package forge.game;

import java.util.HashSet;

import forge.card.CardType;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.trackable.TrackableProperty;
import forge.util.collect.FCollectionView;
import org.tinylog.Logger;

/**
 * Canonical {@link PlayerView} source for DanDan shared {@link ZoneType#Library} and
 * {@link ZoneType#Graveyard}: each seat may carry its own trackable copy, but UI and verbose tooling
 * should use the first registered player's lists so order and counts match the engine and sim.
 */
public final class DanDanViewZones {

    private static final boolean DEBUG_SYNC = Boolean.getBoolean("forge.debug.dandan.sync");

    private DanDanViewZones() {
    }

    /** Prefer live {@link Game} rules when present so UI matches sim even if trackable {@link GameType} lags. */
    public static boolean isDanDan(final GameView gameView) {
        if (gameView == null) {
            return false;
        }
        final Game g = gameView.getGame();
        if (g != null) {
            final GameRules rules = g.getRules();
            if (rules != null && (rules.getGameType() == GameType.DanDan || rules.hasAppliedVariant(GameType.DanDan))) {
                return true;
            }
        }
        final Match match = gameView.getMatch();
        if (match != null) {
            final GameRules rules = match.getRules();
            if (rules != null && (rules.getGameType() == GameType.DanDan || rules.hasAppliedVariant(GameType.DanDan))) {
                return true;
            }
        }
        return gameView.getGameType() == GameType.DanDan;
    }

    /**
     * Card list to show for {@code player} in {@code zone}. For DanDan library/graveyard, returns the
     * first player's list.
     */
    public static FCollectionView<CardView> cardsForZoneDisplay(final GameView gameView, final PlayerView player,
            final ZoneType zone) {
        if (gameView != null && isDanDan(gameView)
                && (zone == ZoneType.Library || zone == ZoneType.Graveyard)) {
            // Prefer live model zone order when available (desktop local games).
            final Game g = gameView.getGame();
            if (g != null && !g.getPlayers().isEmpty()) {
                final Player canonical = g.getPlayers().get(0);
                final PlayerZone sharedZone = canonical.getZone(zone);
                if (sharedZone != null) {
                    final Iterable<Card> liveCards = sharedZone.getCards(false);
                    return CardView.getCollection(liveCards);
                }
            }

            final FCollectionView<PlayerView> players = gameView.getPlayers();
            if (players != null && !players.isEmpty()) {
                final FCollectionView<CardView> shared = players.get(0).getCards(zone);
                if (DEBUG_SYNC) {
                    final FCollectionView<CardView> local = player.getCards(zone);
                    final String sharedHash = shortIdHash(shared);
                    final String localHash = shortIdHash(local);
                    if (!sharedHash.equals(localHash)) {
                        Logger.debug("DanDan view mismatch {} {} shared={} local={} player={}",
                                zone, gameView.getTurn(), sharedHash, localHash, player.getName());
                    }
                }
                if (shared != null) {
                    return shared;
                }
            }
        }
        return player == null ? null : player.getCards(zone);
    }

    /** Count aligned with {@link #cardsForZoneDisplay}. */
    public static int zoneCountForDisplay(final GameView gameView, final PlayerView player, final ZoneType zone) {
        final FCollectionView<CardView> cards = cardsForZoneDisplay(gameView, player, zone);
        return cards == null ? 0 : cards.size();
    }

    /**
     * Distinct card types in graveyard for UI (e.g. tooltips, delirium tint); uses canonical list in DanDan.
     */
    public static int graveyardTypeCountForDisplay(final GameView gameView, final PlayerView player) {
        if (gameView != null && isDanDan(gameView)) {
            final FCollectionView<CardView> cards = cardsForZoneDisplay(gameView, player, ZoneType.Graveyard);
            if (cards == null) {
                return 0;
            }
            final HashSet<CardType.CoreType> types = new HashSet<>();
            for (final CardView c : cards) {
                types.addAll(c.getCurrentState().getType().getCoreTypes());
            }
            return types.size();
        }
        return player == null ? 0 : player.getZoneTypes(TrackableProperty.Graveyard);
    }

    /** Delirium highlight in zone tabs; uses canonical graveyard in DanDan. */
    public static boolean hasDeliriumForDisplay(final GameView gameView, final PlayerView player) {
        if (player == null) {
            return false;
        }
        return graveyardTypeCountForDisplay(gameView, player) >= 4;
    }

    private static String shortIdHash(final Iterable<CardView> cards) {
        if (cards == null) {
            return "null";
        }
        int count = 0;
        int hash = 1;
        for (final CardView c : cards) {
            count++;
            hash = 31 * hash + (c == null ? 0 : c.getId());
        }
        return count + ":" + Integer.toHexString(hash);
    }
}
