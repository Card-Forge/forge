package forge.adventure.archipelago.events;

import com.badlogic.gdx.Gdx;
import forge.adventure.archipelago.ArchipelagoClient;
import forge.adventure.archipelago.ArchipelagoData;
import forge.adventure.archipelago.SlotData;
import io.github.archipelagomw.events.ArchipelagoEventListener;
import io.github.archipelagomw.events.DeathLinkEvent;

public class OnDeathLink {
    private final ArchipelagoClient APClient;

    public OnDeathLink(ArchipelagoClient APClient) {
        this.APClient = APClient;
    }

    // The client already ignores the death links it sends itself.
    @ArchipelagoEventListener
    public void onDeath(DeathLinkEvent event) {
        SlotData slotData = APClient.getSlotData();
        if (APClient.isConnected() && slotData != null && slotData.DeathLink == 1) {
            // Called from the client thread, the penalty changes the player so it's applied on the game thread.
            Gdx.app.postRunnable(() -> ArchipelagoData.getInstance().applyDeathLink(event.source, event.cause));
        }
    }
}
