package forge.localinstance.achievements;

import java.util.Map.Entry;

import forge.card.CardType;
import forge.deck.Deck;
import forge.item.PaperCard;
import forge.localinstance.achievements.ChallengeAchievements.DeckChallengeAchievement;
import forge.util.Localizer;

public class NoSpells extends DeckChallengeAchievement {
    public NoSpells() {
        super("NoSpells", Localizer.getInstance().getMessage("lblNoSpells"),
            Localizer.getInstance().getMessage("lblWithOnlyCreaturesAndLands"),
            Localizer.getInstance().getMessage("lblILetMyArmyTalking")
        );
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
