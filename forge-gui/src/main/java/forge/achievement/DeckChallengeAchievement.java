package forge.achievement;

import forge.deck.Deck;
import forge.game.Game;
import forge.game.player.Player;

public abstract class DeckChallengeAchievement extends ProgressiveAchievement {
    protected DeckChallengeAchievement(String key0, String displayName0, String condition0, String flavorText0) {
        super(key0, displayName0, "Win a game using a deck " + condition0, flavorText0);
    }

    @Override
    protected String getNoun() {
        return "Win";
    }

    @Override
    protected final boolean eval(Player player, Game game) {
        if (player.getOutcome().hasWon()) {
            return eval(player.getRegisteredPlayer().getDeck());
        }
        return false;
    }

    protected abstract boolean eval(Deck deck);
}
