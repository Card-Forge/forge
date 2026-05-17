package forge.adventure.archipelago.events;

import forge.adventure.archipelago.ItemRegistry;
import forge.adventure.data.ArchipelagoData;
import io.github.archipelagomw.events.ArchipelagoEventListener;
import io.github.archipelagomw.events.ReceiveItemEvent;
import io.github.archipelagomw.parts.NetworkItem;

public class ReceiveItem {
    @ArchipelagoEventListener
    public void onReceiveItem(ReceiveItemEvent event) {
        ArchipelagoData APData = ArchipelagoData.getInstance();
        NetworkItem item = event.getItem();

        switch ((int)item.itemID){
            case 1:
                APData.addItem(ItemRegistry.getItem(item.itemID));
                break;
            case 2:
                break;
            case 3:
                break;
            case 4:
                break;
            case 5:
                break;
            case 6:
                break;
            case 7:
                break;
            case 8:
                break;
            case 9:
                break;
            case 10:
                break;
            case 11:
                break;
        }
    }
}