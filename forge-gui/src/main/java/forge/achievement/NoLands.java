package forge.achievement;

import java.util.Map.Entry;

import forge.achievement.ChallengeAchievements.DeckChallengeAchievement;
import forge.deck.Deck;
import forge.item.PaperCard;

public class NoLands extends DeckChallengeAchievement {
    public NoLands() {
        super("NoLands", "No Lands", "with no lands", "I prefer mana from more artificial sources.");
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
