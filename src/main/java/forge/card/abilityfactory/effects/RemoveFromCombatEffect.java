package forge.card.abilityfactory.effects;

import java.util.ArrayList;

import forge.Card;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;

public class RemoveFromCombatEffect extends SpellEffect {

    @Override
    public String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
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
    
        sb.append("Remove ");
    
        for (final Card c : tgtCards) {
            sb.append(c);
        }
    
        sb.append(" from combat.");
    
        // begin standard post-
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }
    
        return sb.toString();
    }

    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {

        ArrayList<Card> tgtCards;

        final Target tgt = sa.getTarget();
        if ((tgt != null) && !params.containsKey("Defined")) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Card c : tgtCards) {
            if ((tgt == null) || c.canBeTargetedBy(sa)) {
                Singletons.getModel().getGame().getCombat().removeFromCombat(c);
            }
        }

    } 
}