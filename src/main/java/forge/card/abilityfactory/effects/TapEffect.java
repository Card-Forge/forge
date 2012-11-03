package forge.card.abilityfactory.effects;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import forge.Card;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;

public class TapEffect extends SpellEffect {
    
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(Map<String, String> params, SpellAbility sa) {
        final Card card = sa.getSourceCard();
        final boolean remTapped = params.containsKey("RememberTapped");
        if (remTapped) {
            card.clearRemembered();
        }

        final Target tgt = sa.getTarget();
        final List<Card> tgtCards = getTargetCards(sa, params);

        for (final Card tgtC : tgtCards) {
            if (tgt != null && !tgtC.canBeTargetedBy(sa)) {
                continue;
            }
            
            if (params.containsKey("ETB") || tgtC.isInPlay()) {
                if (tgtC.isUntapped() && (remTapped)) {
                    card.addRemembered(tgtC);
                }
                tgtC.tap();
            }
        }
    }

    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
    
        sb.append("Tap ");
        final List<Card> tgtCards = getTargetCards(sa, params);
        sb.append(StringUtils.join(tgtCards, ", "));
        sb.append(".");
        return sb.toString();
    }

}