package forge.achievement;

import forge.game.Game;
import forge.game.GameType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.Localizer;

public class ManaFlooded extends Achievement {
    public ManaFlooded(int bronze0, int silver0, int gold0, int mythic0) {
        super("ManaFlooded", Localizer.getInstance().getMessage("lblManaFlooded"),
            Localizer.getInstance().getMessage("lblWinGameWithLeast"), 0,
            Localizer.getInstance().getMessage("lblNLandOnTheBattlefield", String.valueOf(bronze0)), bronze0,
            Localizer.getInstance().getMessage("lblNLandOnTheBattlefield", String.valueOf(silver0)), silver0,
            Localizer.getInstance().getMessage("lblNLandOnTheBattlefield", String.valueOf(gold0)), gold0,
            Localizer.getInstance().getMessage("lblNLandOnTheBattlefield", String.valueOf(mythic0)), mythic0
        );
    }

    @Override
    protected int evaluate(Player player, Game game) {
        if (game.getRules().hasAppliedVariant(GameType.MomirBasic) || game.getRules().hasAppliedVariant(GameType.MoJhoSto)) {
            return 0; // in Momir Basic, getting a lot of lands out is not an achievement:w
        }
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
        return Localizer.getInstance().getMessage("lblLand");
    }
}
