package forge.card.abilityfactory.effects;

import java.util.ArrayList;

import forge.Card;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;

public class MustBlockEffect extends SpellEffect {

    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final Card host = sa.getSourceCard();

        ArrayList<Card> tgtCards;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
        }

        ArrayList<Card> cards;
        if (params.containsKey("DefinedAttacker")) {
            cards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("DefinedAttacker"), sa);
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
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        final Card host = sa.getSourceCard();
        final StringBuilder sb = new StringBuilder();
    
        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }
    
        // end standard pre-
    
        ArrayList<Card> tgtCards;
    
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
        }
    
        String attacker = null;
        if (params.containsKey("DefinedAttacker")) {
            final ArrayList<Card> cards = AbilityFactory.getDefinedCards(sa.getSourceCard(),
                    params.get("DefinedAttacker"), sa);
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