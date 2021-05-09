package forge.game.ability.effects;

import java.util.List;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.util.Lang;
import forge.util.Localizer;

public class DrawEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Player> tgtPlayers = getDefinedPlayersOrTargeted(sa);

        if (!tgtPlayers.isEmpty()) {

            int numCards = sa.hasParam("NumCards") ? AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumCards"), sa) : 1;
            
            sb.append(Lang.joinHomogenous(tgtPlayers));

            if (tgtPlayers.size() > 1) {
                sb.append(" each");
            }
            sb.append(Lang.joinVerb(tgtPlayers, " draw")).append(" ");
            sb.append(Lang.nounWithAmount(numCards, "card"));
            sb.append(".");
        }

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();
        final int numCards = sa.hasParam("NumCards") ? AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumCards"), sa) : 1;
        

        final TargetRestrictions tgt = sa.getTargetRestrictions();

        final boolean optional = sa.hasParam("OptionalDecider");
        final boolean upto = sa.hasParam("Upto");


        for (final Player p : getDefinedPlayersOrTargeted(sa)) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) 
                if (optional && !p.getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoYouWantDrawCards", Lang.nounWithAmount(numCards, " card")), null))
                    continue;

                int actualNum = numCards; 
                if (upto) {
                    actualNum = p.getController().chooseNumber(sa, Localizer.getInstance().getMessage("lblHowManyCardDoYouWantDraw"),0, numCards);
                }

                final CardCollectionView drawn = p.drawCards(actualNum, sa);
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

