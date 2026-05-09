package forge.adventure.archipelago.events;

import io.github.archipelagomw.events.ArchipelagoEventListener;
import io.github.archipelagomw.events.ReceiveItemEvent;
import io.github.archipelagomw.parts.NetworkItem;

public class ReceiveItem {
    @ArchipelagoEventListener
    public void onReceiveItem(ReceiveItemEvent event) {
        NetworkItem item = event.getItem();


    }
}