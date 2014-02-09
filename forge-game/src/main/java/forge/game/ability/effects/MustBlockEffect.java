package forge.game.ability.effects;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;

import java.util.ArrayList;
import java.util.List;

public class MustBlockEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();

        List<Card> tgtCards = getTargetCards(sa);
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final boolean mustBlockAll = sa.hasParam("BlockAllDefined");

        List<Card> cards;
        if (sa.hasParam("DefinedAttacker")) {
            cards = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("DefinedAttacker"), sa);
        } else {
            cards = new ArrayList<Card>();
            cards.add(host);
        }

        for (final Card c : tgtCards) {
            if ((tgt == null) || c.canBeTargetedBy(sa)) {
                if (mustBlockAll) {
                    c.addMustBlockCards(cards);
                } else {
                    final Card attacker = cards.get(0);
                    c.addMustBlockCard(attacker);
                    System.out.println(c + " is adding " + attacker + " to mustBlockCards: " + c.getMustBlockCards());
                }
            }
        }

    } // mustBlockResolve()

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final StringBuilder sb = new StringBuilder();

        // end standard pre-

        final List<Card> tgtCards = getTargetCards(sa);

        String attacker = null;
        if (sa.hasParam("DefinedAttacker")) {
            final List<Card> cards = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("DefinedAttacker"), sa);
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
