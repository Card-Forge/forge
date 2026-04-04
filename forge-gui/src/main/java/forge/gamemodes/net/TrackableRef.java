package forge.gamemodes.net;

import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableTypes;
import forge.trackable.TrackableTypes.TrackableType;
import forge.trackable.Tracker;

import org.tinylog.Logger;

import java.io.Serializable;

/**
 * Shared primitives for replacing CardView/PlayerView references with
 * lightweight {@link IdRef} markers during network serialization.
 * Used by both {@link GameEventProxy} (game events) and the Netty
 * encoder/decoder (protocol method args).
 */
final class TrackableRef {
    static final byte TYPE_CARD_VIEW = 0;
    static final byte TYPE_PLAYER_VIEW = 1;

    static final class IdRef implements Serializable {
        private static final long serialVersionUID = 1L;
        final byte typeTag;
        final int id;

        IdRef(byte typeTag, int id) {
            this.typeTag = typeTag;
            this.id = id;
        }
    }

    static byte typeTagFor(TrackableObject obj) {
        if (obj instanceof CardView) return TYPE_CARD_VIEW;
        if (obj instanceof PlayerView) return TYPE_PLAYER_VIEW;
        return -1;
    }

    static TrackableType<?> trackableTypeFor(byte typeTag) {
        switch (typeTag) {
            case TYPE_CARD_VIEW: return TrackableTypes.CardViewType;
            case TYPE_PLAYER_VIEW: return TrackableTypes.PlayerViewType;
            default: return null;
        }
    }

    static Object replace(Object obj) {
        if (obj instanceof TrackableObject trackable) {
            byte tag = typeTagFor(trackable);
            if (tag >= 0) {
                return new IdRef(tag, trackable.getId());
            }
        }
        return obj;
    }

    static Object resolve(Object obj, Tracker tracker) {
        if (obj instanceof IdRef ref) {
            TrackableType<?> type = trackableTypeFor(ref.typeTag);
            if (type != null) {
                Object resolved = tracker.getObj(type, ref.id);
                if (resolved == null) {
                    Logger.warn("Could not resolve IdRef(tag={}, id={}) from Tracker", ref.typeTag, ref.id);
                }
                return resolved;
            }
        }
        return obj;
    }

    private TrackableRef() {}
}
