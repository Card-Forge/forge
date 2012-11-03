package forge.card.abilityfactory.effects;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import forge.Card;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.gui.GuiChoose;

public class TapOrUntapEffect extends SpellEffect {
    
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(Map<String, String> params, SpellAbility sa) {
        // when getStackDesc is called, just build exactly what is happening
        final StringBuilder sb = new StringBuilder();

    
        sb.append("Tap or untap ");
    
        final List<Card> tgtCards = getTargetCards(sa, params);
        sb.append(StringUtils.join(tgtCards, ", "));
        sb.append(".");
        return sb.toString();
    }

    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final List<Card> tgtCards = getTargetCards(sa, params);

        final Target tgt = sa.getTarget();

        for (final Card tgtC : tgtCards) {
            if (tgtC.isInPlay() && ((tgt == null) || tgtC.canBeTargetedBy(sa))) {
                if (sa.getActivatingPlayer().isHuman()) {
                    final String[] tapOrUntap = new String[] { "Tap", "Untap" };
                    final Object z = GuiChoose.oneOrNone("Tap or Untap " + tgtC + "?", tapOrUntap);
                    if (null == z) {
                        continue;
                    }
                    final boolean tap = (z.equals("Tap")) ? true : false;

                    if (tap) {
                        tgtC.tap();
                    } else {
                        tgtC.untap();
                    }
                } else {
                    // computer
                    tgtC.tap();
                }
            }
        }
    }

}