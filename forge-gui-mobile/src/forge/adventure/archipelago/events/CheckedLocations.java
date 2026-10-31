package forge.adventure.archipelago.events;

import forge.adventure.archipelago.ArchipelagoRandomizer;
import io.github.archipelagomw.events.ArchipelagoEventListener;
import io.github.archipelagomw.events.CheckedLocationsEvent;

/**
 * Keeps our slot's server-side checked locations in sync while connected. The server sends these on room updates:
 * for checks we send ourselves, checks made from another save/device on the same slot, and everything that gets
 * marked as checked when the slot goals/releases.
 */
public class CheckedLocations {

    @ArchipelagoEventListener
    public void onCheckedLocations(CheckedLocationsEvent event) {
        ArchipelagoRandomizer.getInstance().applyServerCheckedLocations(event.checkedLocations);
    }
}
