package forge.game.ability.effects;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Sets;

import forge.game.ability.AbilityKey;
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

        if (sa.hasParam("IfDesc")) {
            if (sa.getParam("IfDesc").equals("True") && sa.hasParam("SpellDescription")) {
                String ifDesc = sa.getParam("SpellDescription");
                sb.append(ifDesc, 0, ifDesc.indexOf(",") + 1);
            } else {
                sb.append(sa.getParam("IfDesc"));
            }
            sb.append(" ");
        }

        final List<Player> tgtPlayers = getDefinedPlayersOrTargeted(sa);

        if (!tgtPlayers.isEmpty()) {
            int numCards = sa.hasParam("NumCards") ? AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumCards"), sa) : 1;

            sb.append(Lang.joinHomogenous(tgtPlayers));

            if (tgtPlayers.size() > 1) {
                sb.append(" each");
            }
            sb.append(Lang.joinVerb(tgtPlayers, " draw")).append(" ");
            //if NumCards calculation could change between getStackDescription and resolve, use NumCardsDesc to avoid
            //a "wrong" stack description
            sb.append(sa.hasParam("NumCardsDesc") ? sa.getParam("NumCardsDesc") : numCards == 1 ? "a card" :
                    (Lang.getNumeral(numCards) + " cards"));
            sb.append(".");
        }

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();

        final boolean upto = sa.hasParam("Upto");
        final boolean optional = sa.hasParam("OptionalDecider") || upto;
        Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
        moveParams.put(AbilityKey.LastStateBattlefield, sa.getLastStateBattlefield());
        moveParams.put(AbilityKey.LastStateGraveyard, sa.getLastStateGraveyard());

        final List<Player> tgts = getTargetPlayersWithDuplicates(true, "Defined", sa);

        for (final Player p : Sets.newHashSet(tgts)) {
            if (!p.isInGame()) {
                continue;
            }

            int numCards = sa.hasParam("NumCards") ? AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumCards"), sa) : 1;
            numCards *= Collections.frequency(tgts, p);

            // it is optional, not upto and player can't choose to draw that many cards
            if (optional && !upto && !p.canDrawAmount(numCards)) {
                continue;
            }

            if (optional && !p.getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoYouWantDrawCards", Lang.nounWithAmount(numCards, " card")), null)) {
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

            final CardCollectionView drawn = p.drawCards(actualNum, sa, moveParams);
            if (sa.hasParam("Reveal")) {
                p.getGame().getAction().reveal(drawn, p, !sa.getParam("Reveal").equals("All"));
            }
            if (sa.hasParam("RememberDrawn")) {
                source.addRemembered(drawn);
            }
            sa.setSVar("AFNotDrawnNum_" + p.getId(), "Number$" + drawn.size());
        }
    }
}
