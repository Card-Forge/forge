package forge.adventure.archipelago.events;

import com.github.tommyettinger.textra.TypingLabel;
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
                APRandomizer.setLastIp(ArchipelagoSettingsScene.instance().getIpTextField());
                APRandomizer.setLastPort(ArchipelagoSettingsScene.instance().getPortTextField());
                APRandomizer.setLastSlotName(ArchipelagoSettingsScene.instance().getSlotNameTextField());
                APRandomizer.setLastPassword(ArchipelagoSettingsScene.instance().getPasswordTextField());

                connectStatusLabel.setText("");
                connectStatusLabel.setText("{FADE=GREEN;GREEN;0.1}Connected!");
                APClient.setSlotData(event.getSlotData(SlotData.class));
                if (APRandomizer.getSlotData() == null){
                    // Initial connection setup
                    ArrayList<Long> locations = new ArrayList<>(APClient.getLocationManager().getMissingLocations());
                    locations.addAll(APClient.getLocationManager().getCheckedLocations());
                    APClient.scoutLocations(locations);
//                    APClient.scoutLocations(new ArrayList<Long>(Arrays.asList(1000L, 1001L, 1002L, 1003L, 1004L, 1005L,
//                            1100L, 1101L, 1102L, 1103L, 1104L, 1105L,
//                            1106L, 1107L, 1108L, 1109L, 1110L, 1111L, 1112L, 1113L,
//                            1200L, 1201L, 1202L, 1203L, 1204L, 1205L,
//                            1206L, 1207L, 1208L, 1209L, 1210L, 1211L, 1212L, 1213L,
//                            1300L, 1301L, 1302L, 1303L, 1304L, 1305L,
//                            1306L, 1307L, 1308L, 1309L, 1310L, 1311L, 1312L, 1313L,
//                            1400L, 1401L, 1402L, 1403L, 1404L, 1405L,
//                            1406L, 1407L, 1408L, 1409L, 1410L, 1411L, 1412L, 1413L,
//                            1500L, 1501L, 1502L, 1503L, 1504L, 1505L,
//                            1506L, 1507L, 1508L, 1509L, 1510L, 1511L, 1512L, 1513L)));
                    APRandomizer.setSlotData(APClient.slotData);
                }

                APRandomizer.sendQueuedLocations();


//                ArchipelagoData.getInstance().setTotalAmountOfSetUnlockChecks(APClient.slotData.SetUnlocks);
//                APClient.slotData.parseStartingItems(registries);
            } catch (Exception e) {
                e.printStackTrace();
                if (APClient.getSlotData() != null) {

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