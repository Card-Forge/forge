package forge.achievement;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class ManaFlooded extends Achievement {
    public ManaFlooded(int bronze0, int silver0, int gold0, int mythic0) {
        super("ManaFlooded", "Mana Flooded", "Win a game with at least", 0,
            String.format("%d lands on the battlefield", bronze0), bronze0,
            String.format("%d lands on the battlefield", silver0), silver0,
            String.format("%d lands on the battlefield", gold0), gold0,
            String.format("%d lands on the battlefield", mythic0), mythic0);
    }

    @Override
    protected int evaluate(Player player, Game game) {
        if (player.getOutcome().hasWon()) {
            int landCount = 0;
            for (Card c : player.getZone(ZoneType.Battlefield).getCards()) {
                if (c.isLand()) {
                    landCount++;
                }
            }
            return landCount;
        }
        return 0; //indicate that player didn't win
    }

    @Override
    protected String getNoun() {
        return "Land";
    }
}
