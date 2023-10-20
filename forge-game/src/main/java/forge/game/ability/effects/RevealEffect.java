package forge.game.ability.effects;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.Lang;

public class RevealEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final boolean anyNumber = sa.hasParam("AnyNumber");
        final boolean optional = sa.hasParam("Optional");
        int cnt = sa.hasParam("NumCards") ? AbilityUtils.calculateAmount(host, sa.getParam("NumCards"), sa) : 1;

        for (final Player p : getTargetPlayers(sa)) {
            if (!p.isInGame()) {
                continue;
            }
            final CardCollectionView cardsInHand = p.getCardsIn(ZoneType.Hand);
            if (cardsInHand.isEmpty()) {
                continue;
            }
            final CardCollection revealed = new CardCollection();
            if (sa.hasParam("Random")) {
                CardCollection valid = new CardCollection(cardsInHand);

                if (sa.hasParam("RevealValid")) {
                    valid = CardLists.getValidCards(valid, sa.getParam("RevealValid"), p, host, sa);
                }

                if (valid.isEmpty())
                    continue;

                final int revealnum = Math.min(valid.size(), cnt);
                revealed.addAll(Aggregates.random(valid, revealnum));
            } else if (sa.hasParam("RevealDefined")) {
                revealed.addAll(AbilityUtils.getDefinedCards(host, sa.getParam("RevealDefined"), sa));
            } else if (sa.hasParam("RevealAllValid")) {
                revealed.addAll(CardLists.getValidCards(cardsInHand, sa.getParam("RevealAllValid"), p, host, sa));
            } else {
                CardCollection valid = new CardCollection(cardsInHand);

                if (sa.hasParam("RevealValid")) {
                    valid = CardLists.getValidCards(valid, sa.getParam("RevealValid"), p, host, sa);
                }

                if (valid.isEmpty())
                    continue;

                if (cnt > valid.size())
                    cnt = valid.size();

                int min = cnt;
                if (anyNumber) {
                    cnt = valid.size();
                    min = 0;
                } else if (optional) {
                    min = 0;
                }

                revealed.addAll(p.getController().chooseCardsToRevealFromHand(min, cnt, valid));
            }

            if (sa.hasParam("RevealToAll") || sa.hasParam("Random")) {
                game.getAction().reveal(revealed, p, false, sa.getParamOrDefault("RevealTitle", ""));
            } else {
                game.getAction().reveal(revealed, p);
            }
            for (final Card c : revealed) {
                if (sa.hasParam("RememberRevealed")) {
                    host.addRemembered(c);
                }
            }
        }
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Player> tgtPlayers = getTargetPlayers(sa);

        if (tgtPlayers.size() > 0) {
            sb.append(Lang.joinHomogenous(tgtPlayers)).append(" reveals ");
            if (sa.hasParam("AnyNumber")) {
                sb.append("any number of cards ");
            } else if (sa.hasParam("NumCards")) {
                int numCards = sa.getHostCard() != null ?
                        AbilityUtils.calculateAmount(sa.getHostCard(), "NumCards", sa)
                        : StringUtils.isNumeric(sa.getParam("NumCards")) ? Integer.parseInt(sa.getParam("NumCards")) : 0;
                sb.append(numCards > 1 ? numCards + " cards " : "a card ");
            } else {
                sb.append("a card ");
            }
            if (sa.hasParam("Random")) {
                sb.append("at random ");
            }
            sb.append("from their hand.");
        } else {
            sb.append("Error - no target players for RevealHand. ");
        }

        return sb.toString();
    }

}
