package forge.adventure.archipelago.events;

import com.badlogic.gdx.Gdx;
import forge.adventure.archipelago.ArchipelagoClient;
import forge.adventure.archipelago.ArchipelagoData;
import forge.adventure.archipelago.SlotData;
import io.github.archipelagomw.events.ArchipelagoEventListener;
import io.github.archipelagomw.events.DeathLinkEvent;

import java.util.Objects;

public class OnDeathLink {
    private final ArchipelagoClient APClient;

    public OnDeathLink(ArchipelagoClient APClient) {
        this.APClient = APClient;
    }

    @ArchipelagoEventListener
    public void onDeath(DeathLinkEvent event) {
        SlotData slotData = APClient.getSlotData();
        // Ignore death links sent by ourselves.
        if (Objects.equals(event.source, APClient.getMyName())) return;
        if (APClient.isConnected() && slotData != null && slotData.DeathLink == 1) {
            // Called from the client thread, the penalty changes the player so it's applied on the game thread.
            Gdx.app.postRunnable(() -> ArchipelagoData.getInstance().applyDeathLink(event.source, event.cause));
        }
    }
}
