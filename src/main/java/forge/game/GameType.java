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
    Quest       ( DeckFormat.Constructed, true, true ),
    Constructed ( DeckFormat.Constructed, false, true ), 
    Archenemy   ( DeckFormat.Archenemy, false, false ),
    Planechase  ( DeckFormat.Planechase, false, false ),
    Vanguard    ( DeckFormat.Vanguard, true, true );

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
}
