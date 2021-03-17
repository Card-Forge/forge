package forge.gamesimulationtests.util;

import forge.CardStorageReader;
import forge.StaticData;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;

public class CardDatabaseHelper {
    private static StaticData staticData;

    public static PaperCard getCard(String name) {
        initializeIfNeeded();

        PaperCard result = staticData.getCommonCards().getCard(name);
        if (result == null) {
            throw new IllegalArgumentException("Failed to get card with name " + name);
        }
        return result;
    }

    private static void initializeIfNeeded() {
        if (hasBeenInitialized()) {
            return;
        }
        initialize();
    }

    private static void initialize() {
        final CardStorageReader reader = new CardStorageReader(ForgeConstants.CARD_DATA_DIR, null, FModel.getPreferences().getPrefBoolean(FPref.LOAD_CARD_SCRIPTS_LAZILY));
        staticData = new StaticData(reader, ForgeConstants.EDITIONS_DIR, ForgeConstants.BLOCK_DATA_DIR, FModel.getPreferences().getPrefBoolean(FPref.UI_LOAD_UNKNOWN_CARDS), FModel.getPreferences().getPrefBoolean(FPref.UI_LOAD_NONLEGAL_CARDS));
    }

    private static boolean hasBeenInitialized() {
        return staticData != null;
    }

    public static StaticData getStaticDataToPopulateOtherMocks() {
        initializeIfNeeded();
        return staticData;
    }
}
