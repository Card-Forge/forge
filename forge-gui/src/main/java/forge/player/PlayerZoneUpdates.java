package forge.player;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

import com.google.common.collect.Maps;

import forge.game.player.PlayerView;

public class PlayerZoneUpdates implements Iterable<PlayerZoneUpdate>, Serializable {
    private static final long serialVersionUID = 7023549243041119023L;

    private final Map<PlayerView, PlayerZoneUpdate> updates = Maps.newHashMap();

    public PlayerZoneUpdates() {
    }

    @Override
    public Iterator<PlayerZoneUpdate> iterator() {
        return updates.values().iterator();
    }

    public void add(final PlayerZoneUpdate update) {
        final PlayerView player = update.getPlayer();
        final PlayerZoneUpdate oldUpdate = updates.get(player);
        if (oldUpdate == null) {
            updates.put(player, update);
        } else {
            oldUpdate.add(update);
        }
    }

    public boolean isEmpty() {
        return updates.isEmpty();
    }

    public void clear() {
        updates.clear();
    }
}
