package forge.adventure.archipelago;

import com.github.tommyettinger.textra.TypingLabel;
import io.github.archipelagomw.ClientStatus;
import io.github.archipelagomw.network.client.BouncePacket;

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
        if (APClient == null || !APClient.isConnected() || !APClient.checkLocation(id)) {
            ArchipelagoRandomizer.getInstance().locationQueue.add(id);
        }
    }

    public void checkLocations(List<Long> ids) {
        if (APClient == null || !APClient.isConnected() || !APClient.checkLocations(ids)) {
            ArchipelagoRandomizer.getInstance().locationQueue.addAll(ids);
        }
    }

    public boolean sendQueuedLocations(List<Long> ids) {
        return APClient != null && APClient.isConnected() && APClient.checkLocations(ids);
    }

    public void goal() {
        if (APClient != null) APClient.setGameState(ClientStatus.CLIENT_GOAL);
    }

    // Not queued: a death while disconnected isn't sent.
    public void sendDeathLink(String cause) {
        if (APClient != null && APClient.isConnected() && APClient.getSlotData() != null && APClient.getSlotData().DeathLink == 1) {
            HashMap<String, Object> data = new HashMap<>();
            data.put("time", System.currentTimeMillis() / 1000.0);
            data.put("source", APClient.getMyName());
            data.put("cause", cause);
            BouncePacket deathLink = new BouncePacket();
            deathLink.tags = new String[]{"DeathLink"};
            deathLink.setData(data);
            APClient.sendBounce(deathLink);
        }
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
