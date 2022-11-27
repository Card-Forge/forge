package forge.game.ability.effects;

import forge.GameCommand;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
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

        for (final Card tgtC : getDefinedCardsOrTargeted(sa)) {
            // only goad things on the battlefield
            if (!game.getCardsIn(ZoneType.Battlefield).contains(tgtC)) {
                continue;
            }

            // make sure we can still target now if using targeting
            if (sa.usesTargeting() && !sa.getTargetRestrictions().canTgtPlayer() && !tgtC.canBeTargetedBy(sa)) {
                continue;
            }

            // 701.38d is handled by getGoaded
            tgtC.addGoad(timestamp, player);

            // currently, only Life of the Party uses Duration$ – Duration$ Permanent
            if (!sa.hasParam("Duration")) {
                final GameCommand untilEOT = new GameCommand() {
                    private static final long serialVersionUID = -1731759226844770852L;

                    @Override
                    public void run() {
                        tgtC.removeGoad(timestamp);
                    }
                };

                game.getCleanup().addUntil(player, untilEOT);
            }

            if (remember && tgtC.isGoaded()) {
                sa.getHostCard().addRemembered(tgtC);
            }
        }
    }

}
