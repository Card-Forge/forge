package forge.game.ability.effects;

import forge.GameCommand;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;

import java.util.List;

public class GoadEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final Player player = sa.getActivatingPlayer();
        List<Card> tgt = getDefinedCardsOrTargeted(sa, "Defined");
        String tgtString = sa.getParamOrDefault("DefinedDesc", Lang.joinHomogenous(tgt));
        if (tgtString.isEmpty()) {
            return "";
        } else {
            final StringBuilder sb = new StringBuilder();
            sb.append(player).append(" goads ").append(tgtString).append(".");
            return sb.toString();
        }
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Player player = sa.getActivatingPlayer();
        final Game game = player.getGame();
        final long timestamp = game.getNextTimestamp();
        final boolean remember = sa.hasParam("RememberGoaded");
        final boolean ungoad = sa.hasParam("NoLonger");
        final String duration = sa.getParamOrDefault("Duration", "UntilYourNextTurn");

        for (final Card tgtC : getDefinedCardsOrTargeted(sa)) {
            // only goad things on the battlefield
            if (!tgtC.isInPlay()) {
                continue;
            }

            if (ungoad) {
                tgtC.unGoad();
                continue;
            }

            // check if the object is still in game or if it was moved
            Card gameCard = game.getCardState(tgtC, null);
            // gameCard is LKI in that case, the card is not in game anymore
            // or the timestamp did change
            // this should check Self too
            if (gameCard == null || !tgtC.equalsWithGameTimestamp(gameCard)) {
                continue;
            }

            // 701.38d is handled by getGoaded
            gameCard.addGoad(timestamp, player);

            // currently, only Life of the Party uses Duration$ – Duration$ Permanent
            if (!duration.equals("Permanent")) {
                final GameCommand until = new GameCommand() {
                    private static final long serialVersionUID = -1731759226844770852L;

                    @Override
                    public void run() {
                        gameCard.removeGoad(timestamp);
                    }
                };

                addUntilCommand(sa, until, duration, player);
            }

            if (remember && gameCard.isGoaded()) {
                sa.getHostCard().addRemembered(tgtC);
            }
        }
    }

}
