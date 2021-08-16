package forge.gamesimulationtests.util;

import forge.CardStorageReader;
import forge.StaticData;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;

public class CardDatabaseHelper {
    private static StaticData staticData;

    public static PaperCard getCard(String name) {
        initializeIfNeeded(false);

        PaperCard result = staticData.getCommonCards().getCard(name);
        if (result == null) {
            throw new IllegalArgumentException("Failed to get card with name " + name);
        }
        return result;
    }

    private static void initializeIfNeeded(boolean lazyLoad) {
        if (hasBeenInitialized()) {
            return;
        }
        initialize(lazyLoad);
    }

    private static void initialize(boolean loadCardsLazily) {
        final CardStorageReader reader = new CardStorageReader(ForgeConstants.CARD_DATA_DIR,
                null, loadCardsLazily);
        CardStorageReader customReader;
        try {
            customReader  = new CardStorageReader(ForgeConstants.USER_CUSTOM_CARDS_DIR,
                    null, loadCardsLazily);
        } catch (Exception e) {
            customReader = null;
        }
        staticData = new StaticData(reader, customReader, ForgeConstants.EDITIONS_DIR,
                ForgeConstants.USER_CUSTOM_EDITIONS_DIR ,ForgeConstants.BLOCK_DATA_DIR,
                "Latest Art All Editions",
                true,
                false);
    }

    private static boolean hasBeenInitialized() {
        return staticData != null;
    }

    public static StaticData getStaticDataToPopulateOtherMocks() {
        initializeIfNeeded(false);
        return staticData;
    }

    public static StaticData getStaticDataToPopulateOtherMocks(boolean lazyLoad) {
        initializeIfNeeded(lazyLoad);
        return staticData;
    }
}
