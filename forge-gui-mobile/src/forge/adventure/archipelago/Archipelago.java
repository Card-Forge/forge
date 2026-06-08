package forge.adventure.archipelago;

import com.github.tommyettinger.textra.TypingLabel;
import io.github.archipelagomw.ClientStatus;

import java.net.URISyntaxException;
import java.util.*;

public class Archipelago {
    private final Random random = new Random();

    public static Archipelago instance = null;

    static private ArchipelagoClient APClient;

    public Archipelago() {
        instance = this;
    }

    public static Archipelago getInstance() {
        return instance == null ? instance = new Archipelago() : instance;
    }

    public void connect(String ip, String port, String slotName, String password, TypingLabel connectStatusLabel) {
        if (APClient != null && APClient.isConnected()) APClient.disconnect();

        APClient = new ArchipelagoClient(connectStatusLabel);
        APClient.setName(slotName);
        APClient.setPassword(password);

        String address = ip;
        if (port != null && !port.isEmpty() && !address.contains(":")) address += ":" + port;

        try {
            APClient.connect(address);
        } catch (URISyntaxException e) {
            connectStatusLabel.setText("");
            connectStatusLabel.setText("{FADE=RED;RED;0.1}Invalid IP");
        }
    }

    public void disconnect() {
        if (APClient != null && APClient.isConnected()) APClient.disconnect();
    }

    public boolean isConnected() {
        return APClient != null && APClient.isConnected();
    }

    public void checkLocation(Long id) {
        if (APClient == null || !APClient.checkLocation(id)) {
            ArchipelagoRandomizer.getInstance().locationQueue.add(id);
        }
    }

    public boolean checkLocations(List<Long> ids) {
        return APClient.checkLocations(ids);
    }

    public void goal(Long id) {
        APClient.setGameState(ClientStatus.CLIENT_GOAL);
    }

    public int getRandomItemPrice() {
        int min = 500;
        int max = 1000;
        SlotData slotData = APClient.getSlotData();
        if (slotData != null) {
            min = slotData.MinShopPrice;
            max = slotData.MaxShopPrice;
        }
        return random.nextInt(min, max + 1);
    }
}
