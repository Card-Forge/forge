package forge.adventure.archipelago.events;

import forge.adventure.archipelago.ArchipelagoClient;
import forge.adventure.archipelago.ArchipelagoColors;
import forge.adventure.archipelago.ArchipelagoData;
import forge.adventure.archipelago.ArchipelagoRandomizer;
import io.github.archipelagomw.Print.APPrintPart;
import io.github.archipelagomw.events.ArchipelagoEventListener;
import io.github.archipelagomw.events.PrintJSONEvent;

public class PrintJsonListener {
    private final ArchipelagoClient APClient;

    public PrintJsonListener(ArchipelagoClient APClient) {
        this.APClient = APClient;
    }

    @ArchipelagoEventListener
    public void onPrintJson(PrintJSONEvent event) {
        ArchipelagoRandomizer randomizer = ArchipelagoRandomizer.getInstance();
        switch (event.type) {
            case Chat:
                // Don't print chat messages originating from ourselves.
                if (APClient != null && event.apPrint.slot == APClient.getSlot()) {
                    return;
                } else {
                    // Todo: parse chat message
                }
            case ItemSend:
                StringBuilder messageString = new StringBuilder();
                String itemColor = ArchipelagoColors.Cyan;
                switch (event.item.flags){
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

                for (APPrintPart part : event.apPrint.parts) {
                        switch (part.type) {
                            case playerID -> {
                                if (APClient != null && part.text.equals(APClient.getMyName())) {
                                    messageString.append(ArchipelagoColors.Magenta).append(part.text).append("{RESET}");
                                } else {
                                    messageString.append(part.text);
                                }
                            }
                            case text -> messageString.append(part.text);
                            case itemID -> messageString.append(itemColor).append(part.text).append("{RESET}");
                            case locationID -> messageString.append(ArchipelagoColors.Green).append(part.text).append("{RESET}");
                        }
                    }
                ArchipelagoData APData = ArchipelagoData.getInstance();
                APData.generateGameNotification(messageString.toString());
            case Hint:
                // Todo: parse hint
        }
    }
}
