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
    MomirBasic      (DeckFormat.MomirBasic, false, false, false, "Momir Basic"),
    Commander       (DeckFormat.Commander, false, false, false, "Commander"),
    Planechase      (DeckFormat.Planechase, false, false, true, "Planechase"),
    Archenemy       (DeckFormat.Archenemy, false, false, true, "Archenemy"),
    ArchenemyRumble (DeckFormat.Archenemy, false, false, true, "Archenemy Rumble");

    private final DeckFormat deckFormat;
    private final boolean isCardPoolLimited, canSideboard, addWonCardsMidGame;
    private final String name;

    GameType(DeckFormat deckFormat0, boolean isCardPoolLimited0, boolean canSideboard0, boolean addWonCardsMidgame0, String name0) {
        deckFormat = deckFormat0;
        isCardPoolLimited = isCardPoolLimited0;
        canSideboard = canSideboard0;
        addWonCardsMidGame = addWonCardsMidgame0;
        name = name0;
    }

    /**
     * @return the decksFormat
     */
    public DeckFormat getDecksFormat() {
        return deckFormat;
    }

    /**
     * @return the isCardpoolLimited
     */
    public boolean isCardPoolLimited() {
        return isCardPoolLimited;
    }

    /**
     * @return the canSideboard
     */
    public boolean isSideboardingAllowed() {
        return canSideboard;
    }

    public boolean canAddWonCardsMidGame() {
        return addWonCardsMidGame;
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

    public String toString() {
        return name;
    }
}
