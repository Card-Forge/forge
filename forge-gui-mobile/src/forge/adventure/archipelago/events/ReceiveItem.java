package forge.adventure.archipelago.events;

import forge.adventure.archipelago.ArchipelagoColors;
import forge.adventure.archipelago.ArchipelagoRandomizer;
import forge.adventure.archipelago.ItemRegistry;
import forge.adventure.archipelago.ArchipelagoData;
import forge.adventure.util.Current;
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
            switch ((int)item.itemID) {
                case 1000:
                    APRandomizer.unlockGoldReward(750);
                    break;
                case 1001:
                    APRandomizer.unlockGoldReward(1500);
                    break;
                case 1002:
                    APRandomizer.unlockGoldReward(3000);
                    break;
                case 1003:
                    APRandomizer.unlockManaCrystalReward(20);
                    break;
                case 1004:
                    APRandomizer.unlockManaCrystalReward(30);
                    break;
                case 1005:
                    APRandomizer.unlockManaCrystalReward(50);
                    break;
                case 1009:
                    APData.unlockRandomSet();
                    break;
                case 1010:
                    Current.player().addMaxLife(1);
                    break;
                case 1011:
                    Current.player().addMaxLife(2);
                    break;
                case 2000:
                case 2001:
                case 2002:
                case 2003:
                case 2004:
                    //TODO: implement color sanity
                    break;
                default:
                    APRandomizer.unlockItemReward(ItemRegistry.getItem(item.itemID));
            }

            String itemColor = ArchipelagoColors.Cyan;
            switch (item.flags){
                case 0b001:
                    itemColor = ArchipelagoColors.Plum;
                    break;
                case 0b010:
                    itemColor = ArchipelagoColors.SlateBlue;
                    break;
                case 0b100:
                    itemColor = ArchipelagoColors.Salmon;
                    break;
            }

            APData.generateGameNotification(String.format("%s sent you %s%s{RESET} (%s%s{RESET})", item.playerName, itemColor, item.itemName, ArchipelagoColors.Green, item.locationName));
            APRandomizer.incrementLastArchipelagoRewardIndex();
        }
    }
}