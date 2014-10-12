package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;

import java.util.List;

public class RevealEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final boolean anyNumber = sa.hasParam("AnyNumber");
        int cnt = sa.hasParam("NumCards") ? AbilityUtils.calculateAmount(host, sa.getParam("NumCards"), sa) : 1;

        final TargetRestrictions tgt = sa.getTargetRestrictions();

        for (final Player p : getTargetPlayers(sa)) {
            final Game game = p.getGame();
            if (tgt == null || p.canBeTargetedBy(sa)) {
                final CardCollectionView cardsInHand = p.getZone(ZoneType.Hand).getCards();
                if (cardsInHand.isEmpty()) {
                    continue; 
                }
                final CardCollection revealed = new CardCollection();
                if (sa.hasParam("Random")) {
                    if (sa.hasParam("NumCards")) {
                        final int revealnum = Math.min(cardsInHand.size(), cnt);
                        final CardCollection hand = new CardCollection(cardsInHand);
                        for (int i = 0; i < revealnum; i++) {
                            final Card random = Aggregates.random(hand);
                            revealed.add(random);
                            hand.remove(random);
                        }
                    } else {
                        revealed.add(Aggregates.random(cardsInHand));
                    }
                    
                } else if (sa.hasParam("RevealDefined")) {
                    revealed.addAll(AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("RevealDefined"), sa));
                } else {
                    CardCollection valid = new CardCollection(cardsInHand);

                    if (sa.hasParam("RevealValid")) {
                        valid = CardLists.getValidCards(valid, sa.getParam("RevealValid"), p, host);
                    }
                    
                    if (valid.isEmpty())
                        continue;
                    
                    if( cnt > valid.size() )
                        cnt = valid.size();

                    int min = cnt;
                    if (anyNumber) {
                        cnt = valid.size();
                        min = 0;
                    }
                    
                    revealed.addAll(p.getController().chooseCardsToRevealFromHand(min, cnt, valid));
                }

                game.getAction().reveal(revealed, p);

                if (sa.hasParam("RememberRevealed")) {
                    for (final Card rem : revealed) {
                        host.addRemembered(rem);
                    }
                }
            }
        }
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Player> tgtPlayers = getTargetPlayers(sa);

        if (tgtPlayers.size() > 0) {
            sb.append(tgtPlayers.get(0)).append(" reveals ");
            if (sa.hasParam("AnyNumber")) {
                sb.append("any number of cards ");
            } else {
                sb.append("a card ");
            }
            if (sa.hasParam("Random")) {
                sb.append("at random ");
            }
            sb.append("from his or her hand.");
        } else {
            sb.append("Error - no target players for RevealHand. ");
        }

        return sb.toString();
    }

}
