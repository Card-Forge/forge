package forge.card.abilityfactory.effects;

import java.util.List;

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
    public void resolve(SpellAbility sa) {
        final Card card = sa.getSourceCard();
        final boolean remTapped = sa.hasParam("RememberTapped");
        if (remTapped) {
            card.clearRemembered();
        }

        final Target tgt = sa.getTarget();
        final List<Card> tgtCards = getTargetCards(sa);

        for (final Card tgtC : tgtCards) {
            if (tgt != null && !tgtC.canBeTargetedBy(sa)) {
                continue;
            }
            
            if (sa.hasParam("ETB") || tgtC.isInPlay()) {
                if (tgtC.isUntapped() && (remTapped)) {
                    card.addRemembered(tgtC);
                }
                tgtC.tap();
            }
        }
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
    
        sb.append("Tap ");
        final List<Card> tgtCards = getTargetCards(sa);
        sb.append(StringUtils.join(tgtCards, ", "));
        sb.append(".");
        return sb.toString();
    }

}