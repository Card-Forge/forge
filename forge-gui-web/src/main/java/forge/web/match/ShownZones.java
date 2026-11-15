package forge.web.match;

import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.player.PlayerZoneUpdate;
import forge.web.protocol.ToBrowser.Ref;
import forge.web.protocol.ToBrowser.ShownZone;
import forge.web.protocol.ToBrowser.Zones;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** The zones the browser opens beside the board. The dispatch thread shows and hides them. The socket thread resends them to a reloading browser. */
final class ShownZones {
    private final Consumer<Zones> send;
    private final Predicate<PlayerView> isLocal;
    private final Map<String, ShownZone> shown = new LinkedHashMap<>();
    /** Zones opened for the current choice and for the cards revealed with the current prompt. They close when those end. */
    private final List<PlayerZoneUpdate> selectionZonesShown = new ArrayList<>();
    private final List<PlayerZoneUpdate> revealZonesShown = new ArrayList<>();
    /** Whether zones have closed since the browser was last told. The browser is told with the next prompt or question. */
    private boolean closedUnsent;

    ShownZones(final Consumer<Zones> send, final Predicate<PlayerView> isLocal) {
        this.send = send;
        this.isLocal = isLocal;
    }

    /** Closes the zones of the last choice and opens these for the new one. */
    synchronized void showForSelection(final Iterable<PlayerZoneUpdate> zones) {
        hideSelection();
        selectionZonesShown.addAll(showZones(zones));
    }

    synchronized void hideSelection() {
        hideZones(selectionZonesShown);
        selectionZonesShown.clear();
    }

    synchronized void showRevealed(final Iterable<PlayerZoneUpdate> zones) {
        revealZonesShown.addAll(showZones(zones));
    }

    synchronized void hideRevealed() {
        hideZones(revealZonesShown);
        revealZonesShown.clear();
    }

    /** Tells the browser of the zones that closed since it was last told. */
    synchronized void flush() {
        if (closedUnsent) {
            closedUnsent = false;
            send.accept(zonesMessage());
        }
    }

    synchronized void sendTo(final Consumer<Zones> to) {
        closedUnsent = false;
        to.accept(zonesMessage());
    }

    private Zones zonesMessage() {
        return new Zones(List.copyOf(shown.values()));
    }

    private static String zoneKey(final PlayerView player, final ZoneType zone) {
        return player.getId() + "/" + zone.name();
    }

    /** Returns only the zones this call opened, so closing them leaves open the zones something else opened. */
    private List<PlayerZoneUpdate> showZones(final Iterable<PlayerZoneUpdate> zones) {
        final List<PlayerZoneUpdate> opened = new ArrayList<>();
        for (final PlayerZoneUpdate update : zones) {
            for (final ZoneType zone : update.getZones()) {
                // The browser always shows the battlefield and the viewer's own hand
                if (zone == ZoneType.Battlefield || (zone == ZoneType.Hand && isLocal.test(update.getPlayer()))) {
                    continue;
                }
                final String key = zoneKey(update.getPlayer(), zone);
                if (!shown.containsKey(key)) {
                    shown.put(key, new ShownZone(Ref.player(update.getPlayer().getId()), zone));
                    opened.add(new PlayerZoneUpdate(update.getPlayer(), zone));
                }
            }
        }
        if (!opened.isEmpty() || closedUnsent) {
            closedUnsent = false;
            send.accept(zonesMessage());
        }
        return opened;
    }

    /**
     * Closes zones that showZones opened. The browser is told with the next prompt or question, so an effect that reopens
     * the zone for its next step, such as Telling Time, does not show it close and open again.
     */
    private void hideZones(final List<PlayerZoneUpdate> zones) {
        if (zones.isEmpty()) {
            return;
        }
        for (final PlayerZoneUpdate update : zones) {
            for (final ZoneType zone : update.getZones()) {
                shown.remove(zoneKey(update.getPlayer(), zone));
            }
        }
        closedUnsent = true;
    }
}
