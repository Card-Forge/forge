package forge.achievement;

import java.util.HashSet;
import java.util.Set;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class Domain extends ProgressiveAchievement {
    public Domain() {
        super("Domain", "Domain", "Win a game with one of each basic land on the battlefield", "It's nice being able to cast anything you want.");
    }

    @Override
    protected boolean eval(Player player, Game game) {
        if (player.getOutcome().hasWon()) {
            Set<String> basicLands = new HashSet<String>();
            for (Card c : player.getCardsIn(ZoneType.Battlefield)) {
                if (c.isBasicLand()) {
                    basicLands.add(c.getName());
                }
            }
            return basicLands.size() == 5;
        }
        return false;
    }

    @Override
    protected String getNoun() {
        return "Win";
    }
}
