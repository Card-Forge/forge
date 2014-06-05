package forge.game;

import forge.deck.DeckFormat;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum GameType {

    //            deck composition rules, isPoolRestricted, can sideboard between matches
    Sealed          (DeckFormat.Limited, true, true, true, "Sealed"),
    Draft           (DeckFormat.Limited, true, true, true, "Draft"),
    Winston         (DeckFormat.Limited, true, true, true, "Winston"),
    Gauntlet        (DeckFormat.Limited, true, true, true, "Gauntlet"),
    Quest           (DeckFormat.QuestDeck, true, true, false, "Quest"),
    QuestDraft      (DeckFormat.Limited, true, true, true, "Quest Draft"),
    Constructed     (DeckFormat.Constructed, false, true, true, "Constructed"),
    Vanguard        (DeckFormat.Vanguard, true, true, true, "Vanguard"),
    Commander       (DeckFormat.Commander, false, false, false, "Commander"),
    Planechase      (DeckFormat.Planechase, false, false, true, "Planechase"),
    Archenemy       (DeckFormat.Archenemy, false, false, true, "Archenemy"),
    ArchenemyRumble (DeckFormat.Archenemy, false, false, true, "Archenemy Rumble");

    private final DeckFormat decksFormat;
    private final boolean bCardpoolLimited;
    private final boolean canSideboard;
    private final boolean addWonCardsMidgame;
    private final String name;

    GameType(DeckFormat formatType, boolean isDeckBuilderLimited, boolean sideboardingAllowed, boolean addAnteMidGame, String name0) {
        bCardpoolLimited = isDeckBuilderLimited;
        decksFormat = formatType;
        canSideboard = sideboardingAllowed;
        addWonCardsMidgame = addAnteMidGame;
        name = name0;
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

    public String toString() {
        return name;
    }
}
