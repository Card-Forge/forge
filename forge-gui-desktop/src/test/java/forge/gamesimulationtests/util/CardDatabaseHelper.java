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
        staticData = createStaticData(lazyLoad);
    }

    /**
     * Builds a {@link StaticData} that shares nothing with the process-wide instance.
     *
     * <p>
     * Two kinds of test need this. One is a test that loads the database differently, such
     * as lazily: the shared instance is whatever the first test class to ask for it built.
     * The other is a test that expects a different answer from
     * {@code ImageKeys.hasImage}, because {@link PaperCard} caches that answer per card
     * and the shared cards keep whatever a previously-run class recorded. PowerMock used
     * to hide both problems behind a per-class classloader.
     * </p>
     */
    private static final java.util.Map<String, StaticData> isolatedInstances = new java.util.HashMap<>();

    /**
     * As {@link #createStaticData(boolean)}, but built once per {@code key} and reused.
     *
     * <p>
     * Test classes call their initialisation from {@code @BeforeMethod}, so an uncached
     * call rebuilds the whole 33,682-script database for every single test method. Keying
     * on the test class name restores what PowerMock's per-class classloader used to give:
     * one database per class, isolated from every other class.
     * </p>
     */
    public static StaticData createStaticData(String key, boolean loadCardsLazily) {
        return isolatedInstances.computeIfAbsent(key, k -> createStaticData(loadCardsLazily));
    }

    public static StaticData createStaticData(boolean loadCardsLazily) {
        final CardStorageReader reader = new CardStorageReader(ForgeConstants.CARD_DATA_DIR,
                null, loadCardsLazily);
        CardStorageReader customReader;
        try {
            customReader  = new CardStorageReader(ForgeConstants.USER_CUSTOM_CARDS_DIR,
                    null, loadCardsLazily);
        } catch (Exception e) {
            customReader = null;
        }
        return new StaticData(reader, customReader, ForgeConstants.EDITIONS_DIR,
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
