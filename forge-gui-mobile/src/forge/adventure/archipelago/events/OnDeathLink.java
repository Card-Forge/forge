package forge.adventure.archipelago.events;

import forge.adventure.archipelago.ArchipelagoClient;
import forge.adventure.archipelago.SlotData;
import io.github.archipelagomw.events.ArchipelagoEventListener;
import io.github.archipelagomw.events.DeathLinkEvent;

public class OnDeathLink {
    private final ArchipelagoClient APClient;

    public OnDeathLink(ArchipelagoClient APClient) {
        this.APClient = APClient;
    }

    @ArchipelagoEventListener
    public void onDeath(DeathLinkEvent event) {
        SlotData slotData = APClient.getSlotData();
        if (APClient.isConnected() && slotData != null && slotData.DeathLink == 1) {
            // Todo: Handle deathlink
        }
    }
}
