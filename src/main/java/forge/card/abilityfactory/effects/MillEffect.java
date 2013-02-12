package forge.card.abilityfactory.effects;

import java.util.List;

import forge.Card;
import forge.card.abilityfactory.AbilityUtils;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class MillEffect extends SpellEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getSourceCard();
        final int numCards = AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("NumCards"), sa);
        final boolean bottom = sa.hasParam("FromBottom");

        if (sa.hasParam("ForgetOtherRemembered")) {
            source.clearRemembered();
        }

        final Target tgt = sa.getTarget();

        ZoneType destination = ZoneType.smartValueOf(sa.getParam("Destination"));
        if (destination == null) {
            destination = ZoneType.Graveyard;
        }

        for (final Player p : getTargetPlayers(sa)) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                final List<Card> milled = p.mill(numCards, destination, bottom);
                if (sa.hasParam("RememberMilled")) {
                    for (final Card c : milled) {
                        source.addRemembered(c);
                    }
                }
                if (sa.hasParam("Imprint")) {
                    for (final Card c : milled) {
                        source.addImprinted(c);
                    }
                }
            }
        }
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final int numCards = AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("NumCards"), sa);

        final List<Player> tgtPlayers = getTargetPlayers(sa);
        for (final Player p : tgtPlayers) {
            sb.append(p.toString()).append(" ");
        }

        final ZoneType dest = ZoneType.smartValueOf(sa.getParam("Destination"));
        if ((dest == null) || dest.equals(ZoneType.Graveyard)) {
            sb.append("mills ");
        } else if (dest.equals(ZoneType.Exile)) {
            sb.append("exiles ");
        } else if (dest.equals(ZoneType.Ante)) {
            sb.append("antes ");
        }
        sb.append(numCards);
        sb.append(" card");
        if (numCards != 1) {
            sb.append("s");
        }
        final String millPosition = sa.hasParam("FromBottom") ? "bottom" : "top";
        sb.append(" from the " + millPosition + " of his or her library.");


        return sb.toString();
    }
}
