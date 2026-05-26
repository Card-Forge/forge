package forge.adventure.archipelago.events;

import com.github.tommyettinger.textra.TypingLabel;
import forge.adventure.archipelago.ArchipelagoData;
import forge.adventure.archipelago.SlotData;
import forge.adventure.archipelago.ArchipelagoClient;
import io.github.archipelagomw.events.ArchipelagoEventListener;
import io.github.archipelagomw.events.ConnectionResultEvent;
import io.github.archipelagomw.network.ConnectionResult;

public class ConnectResult {

    private final ArchipelagoClient APClient;
    private TypingLabel connectStatusLabel;

    public ConnectResult(ArchipelagoClient APClient, TypingLabel connectStatusLabel) {
        this.APClient = APClient;
        this.connectStatusLabel = connectStatusLabel;
    }

    @ArchipelagoEventListener
    public void onConnectResult(ConnectionResultEvent event) {
        if (event.getResult() == ConnectionResult.Success) {
            try {
                connectStatusLabel.setText("");
                connectStatusLabel.setText("{FADE=GREEN;GREEN;0.1}Connected!");
                APClient.slotData = event.getSlotData(SlotData.class);
//                ArchipelagoData.getInstance().setTotalAmountOfSetUnlockChecks(APClient.slotData.SetUnlocks);
//                APClient.slotData.parseStartingItems(registries);
            } catch (Exception e) {
                if (APClient.slotData != null) {

                } else {

                }
            }

//            if (APClient.slotData.deathlink) {
//                APClient.setDeathLinkEnabled(true);
//            }


            // ensure server is synced
        } else if (event.getResult() == ConnectionResult.InvalidPassword) {
            connectStatusLabel.setText("");
            connectStatusLabel.setText("{FADE=RED;RED;0.1}Invalid Password");
        } else if (event.getResult() == ConnectionResult.IncompatibleVersion) {
            connectStatusLabel.setText("");
            connectStatusLabel.setText("{FADE=RED;RED;0.1}Incompatible Version");
        } else if (event.getResult() == ConnectionResult.InvalidSlot) {
            connectStatusLabel.setText("");
            connectStatusLabel.setText("{FADE=RED;RED;0.1}Invalid Slot");
        } else if (event.getResult() == ConnectionResult.SlotAlreadyTaken) {
            connectStatusLabel.setText("");
            connectStatusLabel.setText("{FADE=RED;RED;0.1}Slot Taken");
        }
    }
}