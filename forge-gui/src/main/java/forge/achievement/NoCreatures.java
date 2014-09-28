package forge.achievement;

import java.util.Map.Entry;

import forge.achievement.ChallengeAchievements.DeckChallengeAchievement;
import forge.deck.Deck;
import forge.item.PaperCard;

public class NoCreatures extends DeckChallengeAchievement {
    public NoCreatures() {
        super("NoCreatures", "No Creatures", "with no creatures", "I'm not really an animal person.");
    }

    @Override
    protected boolean eval(Deck deck) {
        for (Entry<PaperCard, Integer> card : deck.getMain()) {
            if (card.getKey().getRules().getType().isCreature()) {
                return false;
            }
        }
        return true;
    }
}
