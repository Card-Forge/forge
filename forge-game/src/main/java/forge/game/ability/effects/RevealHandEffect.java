package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

import java.util.List;

public class RevealHandEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Player> tgtPlayers = getTargetPlayers(sa);

        sb.append(sa.getActivatingPlayer()).append(" looks at ");

        if (tgtPlayers.size() > 0) {
            for (final Player p : tgtPlayers) {
                sb.append(p.toString()).append("'s ");
            }
        } else {
            sb.append("Error - no target players for RevealHand. ");
        }
        sb.append("hand.");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final boolean optional = sa.hasParam("Optional");

        for (final Player p : getTargetPlayers(sa)) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                if (optional && !p.getController().confirmAction(sa, null, "Do you want to reveal your hand?")) {
                    continue;
                }
                final CardCollectionView hand = p.getCardsIn(ZoneType.Hand);
                sa.getActivatingPlayer().getController().reveal(hand, ZoneType.Hand, p);
                if (sa.hasParam("RememberRevealed")) {
                    for (final Card c : hand) {
                        host.addRemembered(c);
                    }
                }
                if (sa.hasParam("RememberRevealedPlayer")) {
                    host.addRemembered(p);
                }
            }
        }
    }
}
