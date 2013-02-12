package forge.card.ability.effects;

import java.util.List;

import javax.swing.JOptionPane;

import forge.Card;
import forge.card.ability.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

public class RevealHandEffect extends SpellEffect {

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
        final Card host = sa.getSourceCard();

        final Target tgt = sa.getTarget();

        for (final Player p : getTargetPlayers(sa)) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                final List<Card> hand = p.getCardsIn(ZoneType.Hand);
                if (sa.getActivatingPlayer().isHuman()) {
                    if (hand.size() > 0) {
                        GuiChoose.one(p + "'s hand", hand);
                    } else {
                        final StringBuilder sb = new StringBuilder();
                        sb.append(p).append("'s hand is empty!");
                        javax.swing.JOptionPane.showMessageDialog(null, sb.toString(), p + "'s hand",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                } else {
                    // reveal to Computer (when computer can keep track of seen
                    // cards...)
                }
                if (sa.hasParam("RememberRevealed")) {
                    for (final Card c : hand) {
                        host.addRemembered(c);
                    }
                }
            }
        }
    }
}
