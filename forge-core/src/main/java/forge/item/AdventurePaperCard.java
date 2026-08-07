package forge.item;

import forge.card.CardRarity;
import forge.card.CardRules;

/// Extension of the PaperCard class with features specific to Adventure mode so as not to pollute the model used for the classic game mode.
public class AdventurePaperCard extends PaperCard {
    private transient boolean isLocked;

    public AdventurePaperCard(CardRules rules0, String edition0, CardRarity rarity0) {
        super(rules0, edition0, rarity0);
    }

    public AdventurePaperCard(PaperCard copyFrom, PaperCardFlags flags) {
        super(copyFrom, flags);
    }

    public AdventurePaperCard(CardRules rules0, String edition0, CardRarity rarity0, int artIndex0, boolean foil0, String collectorNumber0, String artist0, String functionalVariant) {
        super(rules0, edition0, rarity0, artIndex0, foil0, collectorNumber0, artist0, functionalVariant);
    }

    protected AdventurePaperCard(CardRules rules, String edition, CardRarity rarity, int artIndex, boolean foil, String collectorNumber, String artist, String functionalVariant, PaperCardFlags flags) {
        super(rules, edition, rarity, artIndex, foil, collectorNumber, artist, functionalVariant, flags);
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }
}
