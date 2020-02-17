package forge.achievement;

import forge.game.Game;
import forge.game.GameType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import forge.util.Localizer;

public class Domain extends ProgressiveAchievement {
    public Domain() {
        super("Domain", Localizer.getInstance().getMessage("lblDomain"),
            Localizer.getInstance().getMessage("lblWinGameWithOneOfEachBasicLandOnTheBattlefield"),
            Localizer.getInstance().getMessage("lblAbleToCastAnytingNiceBeing")
        );
    }

    private HashMap<String, String> basicLandMap = new HashMap<String, String>() {
        {
            put("Plains", "Plains");
            put("Snow-Covered Plains", "Plains");
            put("Island", "Island");
            put("Snow-Covered Island", "Island");
            put("Forest", "Forest");
            put("Snow-Covered Forest", "Forest");
            put("Mountain", "Mountain");
            put("Snow-Covered Mountain", "Mountain");
            put("Swamp", "Swamp");
            put("Snow-Covered Swamp", "Swamp");
        }
    };

    @Override
    protected boolean eval(Player player, Game game) {
        if (game.getRules().hasAppliedVariant(GameType.MomirBasic) || game.getRules().hasAppliedVariant(GameType.MoJhoSto)) {
            // Not an achievement in Momir Basic (easy to get due to predefined deck contents)
            return false;
        }
        if (player.getOutcome().hasWon()) {
            Set<String> basicLands = new HashSet<>();
            for (Card c : player.getCardsIn(ZoneType.Battlefield)) {
                String name = c.getName();
                if (c.isBasicLand() && basicLandMap.containsKey(name)) {
                    basicLands.add(basicLandMap.get(name));
                }
            }
            return basicLands.size() == 5;
        }
        return false;
    }

    @Override
    protected String getNoun() {
        return Localizer.getInstance().getMessage("lblWin");
    }
}
