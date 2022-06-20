package forge.game.ability.effects;

import forge.GameCommand;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class GoadEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Player player = sa.getActivatingPlayer();
        final Game game = player.getGame();
        final long timestamp = game.getNextTimestamp();
        final boolean remember = sa.hasParam("RememberGoaded");

        for (final Card tgtC : getDefinedCardsOrTargeted(sa)) {
            // only goad things on the battlefield
            if (!tgtC.isInPlay()) {
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
            if (!sa.hasParam("Duration")) {
                final GameCommand untilEOT = new GameCommand() {
                    private static final long serialVersionUID = -1731759226844770852L;

                    @Override
                    public void run() {
                        gameCard.removeGoad(timestamp);
                    }
                };

                game.getCleanup().addUntil(player, untilEOT);
            }

            if (remember && gameCard.isGoaded()) {
                sa.getHostCard().addRemembered(tgtC);
            }
        }
    }

}
