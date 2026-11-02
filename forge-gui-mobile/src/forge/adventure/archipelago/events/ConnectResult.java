package forge.adventure.archipelago.events;

import com.badlogic.gdx.Gdx;
import com.github.tommyettinger.textra.TypingLabel;
import forge.adventure.archipelago.Archipelago;
import forge.adventure.archipelago.ArchipelagoData;
import forge.adventure.archipelago.ArchipelagoRandomizer;
import forge.adventure.archipelago.SlotData;
import forge.adventure.archipelago.ArchipelagoClient;
import forge.adventure.scene.ArchipelagoSettingsScene;
import io.github.archipelagomw.events.ArchipelagoEventListener;
import io.github.archipelagomw.events.ConnectionResultEvent;
import io.github.archipelagomw.network.ConnectionResult;

import java.util.ArrayList;

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
                ArchipelagoRandomizer APRandomizer = ArchipelagoRandomizer.getInstance();

                if (APRandomizer.getSlotData() != null && !APRandomizer.getSlotData().Seed.equals(event.getSlotData(SlotData.class).Seed)) {
                    APClient.disconnect();
                    connectStatusLabel.setText("");
                    connectStatusLabel.setText("{FADE=PURPLE;PURPLE;0.1}Seed Mismatch");
                    return;
                }

                APRandomizer.setLastIp(ArchipelagoSettingsScene.instance().getIpTextField());
                APRandomizer.setLastPort(ArchipelagoSettingsScene.instance().getPortTextField());
                APRandomizer.setLastSlotName(ArchipelagoSettingsScene.instance().getSlotNameTextField());
                APRandomizer.setLastPassword(ArchipelagoSettingsScene.instance().getPasswordTextField());

                connectStatusLabel.setText("");
                connectStatusLabel.setText("{FADE=GREEN;GREEN;0.1}Connected!");
                APClient.setSlotData(event.getSlotData(SlotData.class));
                if (APRandomizer.getSlotData() == null){
                    // Initial connection setup
                    APRandomizer.setSlotData(APClient.slotData);
                    ArrayList<Long> locations = new ArrayList<>(APClient.getLocationManager().getMissingLocations());
                    locations.addAll(APClient.getLocationManager().getCheckedLocations());
                    APClient.scoutLocations(locations);
                    // The save was created before the slot options were known, so the starting inventory is given now.
                    SlotData slotData = APClient.slotData;
                    if (slotData != null) {
                        APRandomizer.applyStartingSets(slotData);
                        Gdx.app.postRunnable(() -> APRandomizer.applyStartingInventory(slotData));
                    }
                }
                // Must be known before the received items are processed, which happens right after this event: set unlocks
                // are split over this many checks. Also repairs saves whose set unlocks were split over the default of 100.
                if (APClient.slotData != null) {
                    ArchipelagoData.getInstance().setTotalAmountOfSetUnlockChecks(APClient.slotData.SetUnlockCount);
                }
                ArchipelagoData.getInstance().unlockRemainingSetsIfAllReceived();
                APRandomizer.sendQueuedLocations();
                // Take over what the server already knows was checked for our slot (other saves/devices, release on goal).
                APRandomizer.applyServerCheckedLocations(APClient.getLocationManager().getCheckedLocations());
                // The goal status is not queued like location checks, so re-send it in case it was reached while disconnected.
                if (ArchipelagoData.getInstance().isGoalReached()) {
                    Archipelago.getInstance().goal();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (APClient.slotData != null) {
                APClient.setDeathLinkEnabled(APClient.slotData.DeathLink == 1);
            }
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