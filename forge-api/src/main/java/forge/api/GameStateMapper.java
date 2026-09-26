package forge.api;

import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Copies views on the engine's owning thread, at a stable UI update boundary. */
public final class GameStateMapper {
    private GameStateMapper() { }

    public record CardState(Integer id, String name, String manaCost, String type,
                            Integer power, Integer toughness, boolean tapped, boolean faceDown) { }
    /** Hidden cards have counts only: no identifiers or positions that survive a shuffle. */
    public record ZoneState(String zone, int count, List<CardState> visibleCards) {
        public ZoneState { visibleCards = List.copyOf(visibleCards); }
    }
    public record PlayerState(int id, String name, int life, boolean priority, List<ZoneState> zones) {
        public PlayerState { zones = List.copyOf(zones); }
    }
    public record State(int gameId, int viewerId, int turn, String phase, Integer activePlayerId,
                        boolean gameOver, List<PlayerState> players) {
        public State { players = List.copyOf(players); }
    }

    public static State snapshot(GameView game, PlayerView viewer) {
        Objects.requireNonNull(game);
        Objects.requireNonNull(viewer, "An explicit player perspective is required");
        if (game.getPlayers() == null || !game.getPlayers().contains(viewer)) {
            throw new IllegalArgumentException("Viewer does not belong to this game");
        }
        List<PlayerState> players = new ArrayList<>();
        for (PlayerView player : game.getPlayers()) {
            List<ZoneState> zones = new ArrayList<>();
            for (ZoneType zone : List.of(ZoneType.Battlefield, ZoneType.Hand, ZoneType.Library,
                    ZoneType.Graveyard, ZoneType.Exile, ZoneType.Command, ZoneType.Sideboard)) {
                zones.add(zone(player, zone, viewer));
            }
            players.add(new PlayerState(player.getId(), player.getName(), player.getLife(),
                    player.getHasPriority(), zones));
        }
        return new State(game.getId(), viewer.getId(), game.getTurn(),
                game.getPhase() == null ? null : game.getPhase().name(),
                game.getPlayerTurn() == null ? null : game.getPlayerTurn().getId(), game.isGameOver(), players);
    }

    static ZoneState zone(PlayerView player, ZoneType zone, PlayerView viewer) {
        Objects.requireNonNull(viewer);
        List<CardState> visible = new ArrayList<>();
        var cards = player.getCards(zone);
        if (cards != null) {
            for (CardView card : cards) {
                CardState projected = card(card, viewer);
                if (projected != null) { visible.add(projected); }
            }
        }
        return new ZoneState(zone.name(), player.getZoneSize(zone), visible);
    }

    static CardState card(CardView card, PlayerView viewer) {
        Objects.requireNonNull(viewer);
        if (!card.canBeShownTo(viewer)) { return null; }
        // Never serialize alternate faces, oracle text or image keys from raw views.
        // Do not leak the underlying ID: it could be correlated with an earlier visible face.
        if (card.isFaceDown()) {
            return new CardState(null, "Face-down card", null, null, null, null, card.isTapped(), true);
        }
        var state = card.getCurrentState();
        return new CardState(card.getId(), state.getName(), state.getManaCost().toString(),
                state.getType().toString(), state.getPower(), state.getToughness(), card.isTapped(), false);
    }
}
