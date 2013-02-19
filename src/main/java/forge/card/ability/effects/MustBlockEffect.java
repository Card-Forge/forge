package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;

public class MustBlockEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getSourceCard();

        List<Card> tgtCards;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityUtils.getDefinedCards(sa.getSourceCard(), sa.getParam("Defined"), sa);
        }

        List<Card> cards;
        if (sa.hasParam("DefinedAttacker")) {
            cards = AbilityUtils.getDefinedCards(sa.getSourceCard(), sa.getParam("DefinedAttacker"), sa);
        } else {
            cards = new ArrayList<Card>();
            cards.add(host);
        }

        for (final Card c : tgtCards) {
            if ((tgt == null) || c.canBeTargetedBy(sa)) {
                final Card attacker = cards.get(0);
                c.addMustBlockCard(attacker);
                System.out.println(c + " is adding " + attacker + " to mustBlockCards: " + c.getMustBlockCards());
            }
        }

    } // mustBlockResolve()

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final Card host = sa.getSourceCard();
        final StringBuilder sb = new StringBuilder();

        // end standard pre-

        final List<Card> tgtCards = getTargetCards(sa);

        String attacker = null;
        if (sa.hasParam("DefinedAttacker")) {
            final List<Card> cards = AbilityUtils.getDefinedCards(sa.getSourceCard(), sa.getParam("DefinedAttacker"), sa);
            attacker = cards.get(0).toString();
        } else {
            attacker = host.toString();
        }

        for (final Card c : tgtCards) {
            sb.append(c).append(" must block ").append(attacker).append(" if able.");
        }

        return sb.toString();
    }

}
