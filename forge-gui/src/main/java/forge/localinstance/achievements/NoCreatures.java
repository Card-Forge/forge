package forge.localinstance.achievements;

import java.util.Map.Entry;

import forge.deck.Deck;
import forge.item.PaperCard;
import forge.localinstance.achievements.ChallengeAchievements.DeckChallengeAchievement;
import forge.util.Localizer;

public class NoCreatures extends DeckChallengeAchievement {
    public NoCreatures() {
        super("NoCreatures", Localizer.getInstance().getMessage("lblNoCreatures"),
            Localizer.getInstance().getMessage("lblWithNoCreatures"), Localizer.getInstance().getMessage("lblIMNotReallyAnimalPerson")
        );
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
