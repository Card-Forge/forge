package forge.adventure.archipelago;
import forge.adventure.util.Current;
import java.util.*;

public class ArchipelagoRandomizer {
    private static final  ArchipelagoRandomizer archipelagoRandomizerInstance = new ArchipelagoRandomizer();
    private final ArchipelagoData archipelagoDataInstance;

    private ArchipelagoRandomizer() {
        archipelagoDataInstance = ArchipelagoData.getInstance();
    }

    public static ArchipelagoRandomizer getInstance() {
        return archipelagoRandomizerInstance;
    }

    /// Todo: Add custom pop-up message to be shown upon receiving a check.
    public void unlockManaCrystalReward(Integer amount) {
        Current.player().addShards(amount);
        archipelagoDataInstance.addShards(amount);
    }

    public void unlockGoldReward(int amount) {
        Current.player().giveGold(amount);
        archipelagoDataInstance.addGold(amount);
    }

    // Todo: Verify that this is actually what we want and it's working
    public void unlockMultipleItemReward(Map<String, Integer> itemNamesAndAmounts) {
        for (Map.Entry<String, Integer> item : itemNamesAndAmounts.entrySet()) {
            for (int i = 0; i < item.getValue(); i++) {
                Current.player().addItem(item.getKey());
                archipelagoDataInstance.addItem(item.getKey());
            }
        }
    }

    public void unlockItemReward(String itemName) {
        Current.player().addItem(itemName);
        archipelagoDataInstance.addItem(itemName);
    }

    // Todo: This should be called by the networked part of the AP implementation when we receive a reward.
    public void incrementLastArchipelagoRewardIndex() {
        archipelagoDataInstance.lastArchipelagoRewardIndex++;
    }

    public int getLastArchipelagoRewardIndex() {
        return archipelagoDataInstance.lastArchipelagoRewardIndex;
    }
}
