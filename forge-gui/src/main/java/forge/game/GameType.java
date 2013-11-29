package forge.game;

import forge.deck.DeckFormat;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum GameType {

    //            deck composition rules, isPoolRestricted, can sideboard between matches
    Sealed      ( DeckFormat.Limited, true, true ),
    Draft       ( DeckFormat.Limited, true, true ),
    Gauntlet    ( DeckFormat.Limited, true, true ),
    Quest       ( DeckFormat.QuestDeck, true, true ),
    Constructed ( DeckFormat.Constructed, false, true ), 
    Archenemy   ( DeckFormat.Archenemy, false, false ),
    Planechase  ( DeckFormat.Planechase, false, false ),
    Vanguard    ( DeckFormat.Vanguard, true, true ), 
    Commander   ( DeckFormat.Commander, false, false);

    private final DeckFormat decksFormat;
    private final boolean bCardpoolLimited;
    private final boolean canSideboard;
    
    GameType(DeckFormat formatType, boolean isDeckBuilderLimited, boolean sideboardingAllowed ) {
        bCardpoolLimited = isDeckBuilderLimited;
        decksFormat = formatType;
        canSideboard = sideboardingAllowed;
    }

    /**
     * @return the decksFormat
     */
    public DeckFormat getDecksFormat() {
        return decksFormat;
    }

    /**
     * @return the isCardpoolLimited
     */
    public boolean isCardpoolLimited() {
        return bCardpoolLimited;
    }

    /**
     * @return the canSideboard
     */
    public boolean isSideboardingAllowed() {
        return canSideboard;
    }

    public boolean isCommandZoneNeeded() {
    	return true; //TODO: Figure out way to move command zone into field so it can be hidden when empty
        /*switch (this) {
        case Archenemy:
        case Commander:
        case Planechase:
        case Vanguard:
            return true;
        default:
            return false;
        }*/
    }
}
