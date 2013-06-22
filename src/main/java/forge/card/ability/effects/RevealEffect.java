package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.TargetRestrictions;
import forge.game.Game;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;

public class RevealEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getSourceCard();
        final boolean anyNumber = sa.hasParam("AnyNumber");
        int cnt = sa.hasParam("NumCards") ? AbilityUtils.calculateAmount(host, sa.getParam("NumCards"), sa) : 1;

        
        final TargetRestrictions tgt = sa.getTargetRestrictions();

        for (final Player p : getTargetPlayers(sa)) {
            final Game game = p.getGame();
            if (tgt == null || p.canBeTargetedBy(sa)) {
                final List<Card> cardsInHand = p.getZone(ZoneType.Hand).getCards();
                if (cardsInHand.isEmpty())
                    continue; 
                
                final List<Card> revealed = new ArrayList<Card>();
                if (sa.hasParam("Random")) {
                    if (sa.hasParam("NumCards")) {
                        final int revealnum = Math.min(cardsInHand.size(), cnt);
                        final List<Card> hand = new ArrayList<Card>(cardsInHand);
                        for (int i = 0; i < revealnum; i++) {
                            final Card random = Aggregates.random(hand);
                            revealed.add(random);
                            hand.remove(random);
                        }
                    } else {
                        revealed.add(Aggregates.random(cardsInHand));
                    }
                    
                } else if (sa.hasParam("RevealDefined")) {
                    revealed.addAll(AbilityUtils.getDefinedCards(sa.getSourceCard(), sa.getParam("Defined"), sa));
                } else {
                    List<Card> valid = new ArrayList<Card>(cardsInHand);

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
