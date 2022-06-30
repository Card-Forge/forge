package forge.game.ability.effects;

import java.util.List;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.*;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.Localizer;

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
                if (optional && !p.getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoYouWantRevealYourHand"), null)) {
                    continue;
                }
                CardCollectionView hand = p.getCardsIn(ZoneType.Hand);
                if (sa.hasParam("RevealType")) {
                    hand = CardLists.getType(hand, sa.getParam("RevealType"));
                }
                if (sa.hasParam("Look")) {
                    sa.getActivatingPlayer().getController().reveal(hand, ZoneType.Hand, p);
                } else {
                    host.getGame().getAction().reveal(hand, p);
                }
                if (sa.hasParam("RememberRevealed")) {
                    host.addRemembered(hand);
                }
                if (sa.hasParam("ImprintRevealed")) {
                    host.addImprintedCards(hand);
                }
                if (sa.hasParam("RememberRevealedPlayer")) {
                    host.addRemembered(p);
                }
            }
        }
    }
}
