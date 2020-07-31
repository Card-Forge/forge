package forge.achievement;

import java.util.Map.Entry;

import forge.achievement.ChallengeAchievements.DeckChallengeAchievement;
import forge.deck.Deck;
import forge.item.PaperCard;
import forge.util.Localizer;

public class NoLands extends DeckChallengeAchievement {
    public NoLands() {
        super("NoLands", Localizer.getInstance().getMessage("lblNoLands"),
            Localizer.getInstance().getMessage("lblWithNoLands"),
            Localizer.getInstance().getMessage("lblIMorePreferManaFromArtificial")
        );
    }

    @Override
    protected boolean eval(Deck deck) {
        for (Entry<PaperCard, Integer> card : deck.getMain()) {
            if (card.getKey().getRules().getType().isLand()) {
                return false;
            }
        }
        return true;
    }
}
