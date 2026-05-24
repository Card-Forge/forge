package forge.adventure.archipelago;

import com.github.tommyettinger.textra.TypingLabel;
import io.github.archipelagomw.ClientStatus;

import java.net.URISyntaxException;

public class Archipelago {
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
            //TODO: Handle incorrect address
        }
    }

    public void CheckLocation(Long id) {
        APClient.checkLocation(id);
    }

    public void Goal(Long id) {
        APClient.setGameState(ClientStatus.CLIENT_GOAL);
    }
}
