package forge.game.ability.effects;

import forge.card.CardStateName;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

import java.util.List;

public class MillEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();
        final int numCards = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumCards"), sa);
        final boolean bottom = sa.hasParam("FromBottom");
        final boolean facedown = sa.hasParam("ExileFaceDown");
        final boolean reveal = !sa.hasParam("NoReveal");

        if (sa.hasParam("ForgetOtherRemembered")) {
            source.clearRemembered();
        }

        final TargetRestrictions tgt = sa.getTargetRestrictions();

        ZoneType destination = ZoneType.smartValueOf(sa.getParam("Destination"));
        if (destination == null) {
            destination = ZoneType.Graveyard;
        }

        for (final Player p : getTargetPlayers(sa)) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                if (sa.hasParam("Optional")) {
                    final String prompt = String.format("Do you want to put card(s) from library to %s?", destination);
                    if (!p.getController().confirmAction(sa, null, prompt)) {
                        continue;
                    }
                }
                final CardCollectionView milled = p.mill(numCards, destination, bottom);
                // Reveal the milled cards, so players don't have to manually inspect the
                // graveyard to figure out which ones were milled.
                if (!facedown && reveal) { // do not reveal when exiling face down
                    //p.getGame().getAction().reveal(milled, p, false);
                    StringBuilder sb = new StringBuilder();
                    sb.append(p).append(" milled ").append(milled).append(" to ").append(destination);
                    p.getGame().getGameLog().add(GameLogEntryType.ZONE_CHANGE, sb.toString());
                }
                if (destination.equals(ZoneType.Exile)) {
                    Card host = sa.getOriginalHost();
                    if (host == null) {
                        host = sa.getHostCard();
                    }
                    for (final Card c : milled) {
                        c.setExiledWith(host);
                    	if (facedown) {
                            c.setState(CardStateName.FaceDown, true);
                        }
                    }
                }
                if (sa.hasParam("RememberMilled")) {
                    for (final Card c : milled) {
                        source.addRemembered(c);
                    }
                }
                if (sa.hasParam("Imprint")) {
                    for (final Card c : milled) {
                        source.addImprintedCard(c);
                    }
                }
            }
        }
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final int numCards = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumCards"), sa);

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
