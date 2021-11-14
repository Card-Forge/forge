package forge.game.ability.effects;

import java.util.List;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityCantDraw;
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
            sb.append(numCards == 1 ? "a card" : (Lang.getNumeral(numCards) + " cards"));
            sb.append(".");
        }

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();
        final int numCards = sa.hasParam("NumCards") ? AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumCards"), sa) : 1;

        final boolean upto = sa.hasParam("Upto");
        final boolean optional = sa.hasParam("OptionalDecider") || upto;

        for (final Player p : getDefinedPlayersOrTargeted(sa)) {
            // TODO can this be removed?
            if (sa.usesTargeting() && !p.canBeTargetedBy(sa)) {
                continue;
            }

            // it is optional, not upto and player can't choose to draw that many cards
            if (optional && !upto && !p.canDrawAmount(numCards)) {
                continue;
            }

            if (optional && !p.getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoYouWantDrawCards", Lang.nounWithAmount(numCards, " card")))) {
                continue;
            }

            int actualNum = numCards;

            if (upto) { // if it is upto, player can only choose how many cards they can draw
                actualNum = StaticAbilityCantDraw.canDrawAmount(p, actualNum);
            }
            if (actualNum <= 0) {
                continue;
            }
            if (upto) {
                actualNum = p.getController().chooseNumber(sa, Localizer.getInstance().getMessage("lblHowManyCardDoYouWantDraw"), 0, actualNum);
            }

            final CardCollectionView drawn = p.drawCards(actualNum, sa);
            if (sa.hasParam("Reveal")) {
                if (sa.getParam("Reveal").equals("All")) {
                    p.getGame().getAction().reveal(drawn, p, false);
                } else {
                    p.getGame().getAction().reveal(drawn, p);
                }
            }
            if (sa.hasParam("RememberDrawn")) {
                for (final Card c : drawn) {
                    source.addRemembered(c);
                }
            }
            sa.setSVar("AFNotDrawnNum_" + p.getId(), "Number$" + drawn.size());
        }
    }
}
