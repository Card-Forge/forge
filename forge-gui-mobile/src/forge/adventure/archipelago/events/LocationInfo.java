package forge.adventure.archipelago.events;

import forge.adventure.archipelago.ArchipelagoClient;
import forge.adventure.archipelago.ArchipelagoRandomizer;
import io.github.archipelagomw.events.ArchipelagoEventListener;
import io.github.archipelagomw.events.LocationInfoEvent;

public class LocationInfo {

    private final ArchipelagoClient APClient;

    public LocationInfo(ArchipelagoClient APClient) {
        this.APClient = APClient;
    }

    @ArchipelagoEventListener
    public void onLocationInfo(LocationInfoEvent event) {
        int locationCount = APClient.getLocationManager().getMissingLocations().size() + APClient.getLocationManager().getCheckedLocations().size();
        if (event.locations.size() == locationCount) {
            ArchipelagoRandomizer.getInstance().handleShopData(event.locations);
        } else {
            // Todo: Show hints in console, maybe handle this in PrintJsonListener later?
        }
    }
}
