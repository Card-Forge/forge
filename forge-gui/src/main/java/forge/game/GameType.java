package forge.game;

import forge.deck.DeckFormat;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum GameType {

    //            deck composition rules, isPoolRestricted, can sideboard between matches
    Sealed      ( DeckFormat.Limited, true, true, true ),
    Draft       ( DeckFormat.Limited, true, true, true ),
    Gauntlet    ( DeckFormat.Limited, true, true, true ),
    Quest       ( DeckFormat.QuestDeck, true, true, false ),
    Constructed ( DeckFormat.Constructed, false, true, true ),
    Archenemy   ( DeckFormat.Archenemy, false, false, true ),
    Planechase  ( DeckFormat.Planechase, false, false, true ),
    Vanguard    ( DeckFormat.Vanguard, true, true, true ),
    Commander   ( DeckFormat.Commander, false, false, false);

    private final DeckFormat decksFormat;
    private final boolean bCardpoolLimited;
    private final boolean canSideboard;
    private final boolean addWonCardsMidgame;

    GameType(DeckFormat formatType, boolean isDeckBuilderLimited, boolean sideboardingAllowed, boolean addAnteMidGame ) {
        bCardpoolLimited = isDeckBuilderLimited;
        decksFormat = formatType;
        canSideboard = sideboardingAllowed;
        addWonCardsMidgame = addAnteMidGame;
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

    public boolean canAddWonCardsMidgame() { return addWonCardsMidgame; }

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
