package forge.adventure.archipelago.events;

import forge.adventure.archipelago.ArchipelagoColors;
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
        if (event.getIndex() > APData.getLastArchipelagoRewardIndex()) {
            APData.unlockItemReward(ItemRegistry.getItem(item.itemID));
            //TODO: Make this fancy
            APData.generateGameNotification(String.format("%s sent you %s%s{RESET} (%s%s{RESET})", item.playerName, ArchipelagoColors.Blue, item.itemName, ArchipelagoColors.Green, item.locationName));
            APData.incrementLastArchipelagoRewardIndex();
        }
    }
}