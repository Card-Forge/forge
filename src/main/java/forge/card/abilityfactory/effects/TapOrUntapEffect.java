package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import forge.Card;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
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
    
        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }
    
        sb.append("Tap or untap ");
    
        ArrayList<Card> tgtCards;
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
        }
    
        final Iterator<Card> it = tgtCards.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(".");
        return sb.toString();
    }

    /**
     * <p>
     * tapOrUntapResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final Card card = sa.getSourceCard();

        ArrayList<Card> tgtCards;
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = AbilityFactory.getDefinedCards(card, params.get("Defined"), sa);
        }

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

    // ******************************************
    // ************** Phases ********************
    // ******************************************
    // Phases generally Phase Out. Time and Tide is the only card that can force
    // Phased Out cards in.


    /**
     * <p>
     * phasesCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
}