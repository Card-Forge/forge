package forge.card.ability.effects;

import java.util.List;

import forge.Card;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.util.Lang;

public class DrawEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Player> tgtPlayers = getDefinedPlayersBeforeTargetOnes(sa);

        if (!tgtPlayers.isEmpty()) {

            int numCards = sa.hasParam("NumCards") ? AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("NumCards"), sa) : 1;
            
            sb.append(Lang.joinHomogenous(tgtPlayers));

            if (tgtPlayers.size() > 1) {
                sb.append(" each");
            }
            sb.append(Lang.joinVerb(tgtPlayers, " draw")).append(" ");
            sb.append(Lang.nounWithAmount(numCards, " card"));
            sb.append(".");
        }

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getSourceCard();
        int numCards = sa.hasParam("NumCards") ? AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("NumCards"), sa) : 1;
        

        final Target tgt = sa.getTarget();

        final boolean optional = sa.hasParam("OptionalDecider");


        for (final Player p : getDefinedPlayersBeforeTargetOnes(sa)) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) 
                if (optional && !p.getController().confirmAction(sa, null, "Do you want to draw " + Lang.nounWithAmount(numCards, " card") + "?"))
                    continue;

                //TODO: remove this deprecation exception
                final List<Card> drawn = p.drawCards(numCards);
                if (sa.hasParam("Reveal")) {
                    p.getGame().getAction().reveal(drawn, p);
                }
                if (sa.hasParam("RememberDrawn")) {
                    for (final Card c : drawn) {
                        source.addRemembered(c);
                    }
                }
            }
        }
    } // drawResolve()

