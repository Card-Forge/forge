package forge.card.abilityfactory.effects;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import forge.Card;
import forge.Singletons;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;

public class RemoveFromCombatEffect extends SpellEffect {

    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
    
        final List<Card> tgtCards = getTargetCards(sa, params);
    
        sb.append("Remove ");
        sb.append(StringUtils.join(tgtCards, ", "));
        sb.append(" from combat.");
    
        return sb.toString();
    }

    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {

        final Target tgt = sa.getTarget();
        for (final Card c : getTargetCards(sa, params)) {
            if ((tgt == null) || c.canBeTargetedBy(sa)) {
                Singletons.getModel().getGame().getCombat().removeFromCombat(c);
            }
        }

    } 
}