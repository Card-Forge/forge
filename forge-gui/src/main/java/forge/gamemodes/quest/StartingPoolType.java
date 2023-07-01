package forge.gamemodes.quest;

import forge.util.Localizer;

public enum StartingPoolType {
    Complete("lblUnrestricted"),
    Sanctioned("lblSanctionedFormat"),
    Casual("lblCasualOrArchivedFormat"),
    CustomFormat("lblCustomFormat"),
    Precon("lblEventOrStartDeck"),
    SealedDeck("lblMySealedDeck"),
    DraftDeck("lblMyDraftDeck"),
    Cube("lblPredefinedCube");

    private final String caption;

    StartingPoolType(String caption0) {
        caption = Localizer.getInstance().getMessage(caption0);
    }

    /* (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return caption;
    }
}
