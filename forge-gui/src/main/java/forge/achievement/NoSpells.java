package forge.achievement;

import java.util.Map.Entry;

import forge.achievement.ChallengeAchievements.DeckChallengeAchievement;
import forge.card.CardType;
import forge.deck.Deck;
import forge.item.PaperCard;

public class NoSpells extends DeckChallengeAchievement {
    public NoSpells() {
        super("NoSpells", "No Spells", "with only creatures and lands", "I let my army do the talking.");
    }

    @Override
    protected boolean eval(Deck deck) {
        for (Entry<PaperCard, Integer> card : deck.getMain()) {
            CardType type = card.getKey().getRules().getType();
            if (!type.isCreature() && !type.isLand()) {
                return false;
            }
        }
        return true;
    }
}
