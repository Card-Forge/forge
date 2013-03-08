package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.card.ability.AbilityUtils;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.util.Aggregates;

public class RevealEffect extends RevealEffectBase {

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getSourceCard();
        final boolean anyNumber = sa.hasParam("AnyNumber");

        final Target tgt = sa.getTarget();

        for (final Player p : getTargetPlayers(sa)) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                final List<Card> handChoices = p.getCardsIn(ZoneType.Hand);
                if (handChoices.size() > 0) {
                    final List<Card> revealed = new ArrayList<Card>();
                    if (sa.hasParam("Random")) {
                        if (sa.hasParam("NumCards")) {
                            final int num = AbilityUtils.calculateAmount(host, sa.getParam("NumCards"), sa);
                            final int revealnum = Math.min(handChoices.size(), num);
                            final List<Card> hand = new ArrayList<Card>(handChoices);
                            for (int i = 0; i < revealnum; i++) {
                                final Card random = Aggregates.random(hand);
                                revealed.add(random);
                                hand.remove(random);
                            }
                        } else {
                            revealed.add(Aggregates.random(handChoices));
                        }
                        GuiChoose.oneOrNone("Revealed card(s)", revealed);
                    } else {
                        List<Card> valid = new ArrayList<Card>(handChoices);
                        int max = 1;
                        if (sa.hasParam("RevealValid")) {
                            valid = CardLists.getValidCards(valid, sa.getParam("RevealValid"), p, host);
                        }
                        if (anyNumber) {
                            max = valid.size();
                        }
                        else if (sa.hasParam("NumCards")) {
                            max = Math.min(valid.size(), AbilityUtils.calculateAmount(host, sa.getParam("NumCards"), sa));
                        }
                        //revealed.addAll(getRevealedList(sa.getActivatingPlayer(), valid, max, anyNumber));
                        revealed.addAll(getRevealedList(p, valid, max, anyNumber));
                        //if (sa.getActivatingPlayer().isComputer()) {
                        if (p.isComputer()) {
                            GuiChoose.oneOrNone("Revealed card(s)", revealed);
                        }
                    }

                    if (sa.hasParam("RememberRevealed")) {
                        for (final Card rem : revealed) {
                            host.addRemembered(rem);
                        }
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
