package forge.gamesimulationtests.util;

import forge.CardStorageReader;
import forge.StaticData;
import forge.item.PaperCard;
import forge.properties.NewConstants;

public class CardDatabaseHelper {
	private static StaticData staticData;
	
	public static PaperCard getCard( String name ) {
		initializeIfNeeded();
		
		PaperCard result = staticData.getCommonCards().getCard( name );
		if( result == null ) {
			throw new IllegalArgumentException( "Failed to get card with name " + name );
		}
		return result;
	}
	
	private static void initializeIfNeeded() {
		if( hasBeenInitialized() ) {
			return;
		}
		initialize();
	}
	
	private static void initialize() {
		final CardStorageReader reader = new CardStorageReader( NewConstants.CARD_DATA_DIR, null, null );
        staticData = new StaticData( reader, NewConstants._RES_ROOT+"editions", NewConstants._RES_ROOT+"blockdata" );
	}
	
	private static boolean hasBeenInitialized() {
		return staticData != null;
	}
	
	public static StaticData getStaticDataToPopulateOtherMocks() {
		initializeIfNeeded();
		return staticData;
	}
}
