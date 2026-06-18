package forge.adventure.archipelago.events;

import com.github.tommyettinger.textra.TypingLabel;
import forge.adventure.archipelago.ArchipelagoClient;
import forge.adventure.archipelago.SlotData;
import io.github.archipelagomw.Print.APPrintJsonType;
import io.github.archipelagomw.events.ArchipelagoEventListener;
import io.github.archipelagomw.events.DeathLinkEvent;
import io.github.archipelagomw.events.PrintJSONEvent;



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
