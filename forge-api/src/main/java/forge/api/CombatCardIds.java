package forge.api;

import com.google.common.eventbus.Subscribe;
import forge.game.card.CardView;
import forge.game.event.GameEventCardChangeZone;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Public face-down permanents need distinct positions, without identifying their printed cards. */
final class CombatCardIds {
    private final PlayerView viewer;
    private final MatchActivity activity;
    private final Map<Integer, String> concealed = new ConcurrentHashMap<>();

    CombatCardIds(PlayerView viewer, MatchActivity activity) { this.viewer = viewer; this.activity = activity; }

    String id(CardView card) {
        if (card == null) return null;
        if (card.isFaceDown() && card.getZone() == ZoneType.Battlefield && card.canBeShownTo(viewer))
            return concealed.computeIfAbsent(card.getId(), ignored -> UUID.randomUUID().toString());
        concealed.remove(card.getId());
        return activity.visualId(card);
    }

    @Subscribe public void changedZone(GameEventCardChangeZone event) {
        // A card going through a hidden zone must never retain a correlatable handle.
        concealed.remove(event.card().getId());
    }
}
