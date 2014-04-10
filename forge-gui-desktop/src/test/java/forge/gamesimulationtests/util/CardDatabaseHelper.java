package forge.gamesimulationtests.util;

import forge.CardStorageReader;
import forge.StaticData;
import forge.item.PaperCard;
import forge.properties.ForgeConstants;

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
		final CardStorageReader reader = new CardStorageReader(ForgeConstants.CARD_DATA_DIR, null, null);
        staticData = new StaticData(reader, ForgeConstants.EDITIONS_DIR, ForgeConstants.BLOCK_DATA_DIR);
	}
	
	private static boolean hasBeenInitialized() {
		return staticData != null;
	}
	
	public static StaticData getStaticDataToPopulateOtherMocks() {
		initializeIfNeeded();
		return staticData;
	}
}
