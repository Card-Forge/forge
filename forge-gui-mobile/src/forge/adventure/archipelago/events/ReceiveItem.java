package forge.adventure.archipelago.events;

import forge.adventure.archipelago.ArchipelagoColors;
import forge.adventure.archipelago.ArchipelagoRandomizer;
import forge.adventure.archipelago.ItemRegistry;
import forge.adventure.archipelago.ArchipelagoData;
import io.github.archipelagomw.events.ArchipelagoEventListener;
import io.github.archipelagomw.events.ReceiveItemEvent;
import io.github.archipelagomw.parts.NetworkItem;

public class ReceiveItem {

    @ArchipelagoEventListener
    public void onReceiveItem(ReceiveItemEvent event) {
        ArchipelagoRandomizer APRandomizer = ArchipelagoRandomizer.getInstance();
        ArchipelagoData APData = ArchipelagoData.getInstance();
        NetworkItem item = event.getItem();
        if (event.getIndex() > APRandomizer.getLastArchipelagoRewardIndex()) {
            APRandomizer.unlockItemReward(ItemRegistry.getItem(item.itemID));
            //TODO: Make this fancy
            APData.generateGameNotification(String.format("%s sent you %s%s{RESET} (%s%s{RESET})", item.playerName, ArchipelagoColors.Blue, item.itemName, ArchipelagoColors.Green, item.locationName));
            APRandomizer.incrementLastArchipelagoRewardIndex();
        }
    }
}