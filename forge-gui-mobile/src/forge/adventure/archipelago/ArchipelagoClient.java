package forge.adventure.archipelago;

import com.github.tommyettinger.textra.TypingLabel;
import forge.adventure.archipelago.events.ConnectResult;
import forge.adventure.archipelago.events.LocationChecked;
import forge.adventure.archipelago.events.OnDeathLink;
import forge.adventure.archipelago.events.ReceiveItem;
import io.github.archipelagomw.Client;
import io.github.archipelagomw.flags.ItemsHandling;

import javax.annotation.Nullable;

public class ArchipelagoClient extends Client {

    @Nullable
    public SlotData slotData;

    public ArchipelagoClient(TypingLabel connectStatusLabel) {
        super();

        this.setGame("ForgeAP");
        this.setItemsHandlingFlags(ItemsHandling.SEND_ITEMS | ItemsHandling.SEND_OWN_ITEMS | ItemsHandling.SEND_STARTING_INVENTORY);
        this.getEventManager().registerListener(new ConnectResult(this, connectStatusLabel));
        this.getEventManager().registerListener(new ReceiveItem());
        this.getEventManager().registerListener(new LocationChecked());
        this.getEventManager().registerListener(new OnDeathLink());
    }

    @Nullable
    public SlotData getSlotData() {
        return slotData;
    }

    @Override
    public void onError(Exception ex) {
        ex.getLocalizedMessage();
    }

    @Override
    public void onClose(String s, int i) {

    }
}
