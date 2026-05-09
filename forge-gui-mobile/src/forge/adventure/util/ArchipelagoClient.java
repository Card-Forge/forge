package forge.adventure.util;

import com.github.tommyettinger.textra.TypingLabel;
import forge.StaticData;
import forge.adventure.archipelago.SlotData;
import forge.adventure.archipelago.events.ConnectResult;
import forge.adventure.archipelago.events.LocationChecked;
import forge.adventure.archipelago.events.OnDeathLink;
import forge.adventure.archipelago.events.ReceiveItem;
import forge.adventure.data.ArchipelagoData;
import forge.card.CardEdition;
import io.github.archipelagomw.Client;

import javax.annotation.Nullable;
import java.util.*;

public class ArchipelagoClient extends Client {

    @Nullable
    public SlotData slotData;

    public ArchipelagoClient(TypingLabel connectStatusLabel) {
        super();

        this.setGame("ForgeAP");
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

    }

    @Override
    public void onClose(String s, int i) {

    }
}
