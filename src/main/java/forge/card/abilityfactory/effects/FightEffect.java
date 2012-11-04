package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.Map;

import forge.Card;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;

public class FightEffect extends SpellEffect {
    
    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        Card fighter1 = null;
        Card fighter2 = null;
        final Target tgt = sa.getTarget();
        ArrayList<Card> tgts = null;
        if (tgt != null) {
            tgts = tgt.getTargetCards();
            if (tgts.size() > 0) {
                fighter1 = tgts.get(0);
            }
        }
        if (params.containsKey("Defined")) {
            ArrayList<Card> defined = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
            // Allow both fighters to come from defined list if first fighter not already found
            if (defined.size() > 1 && fighter1 == null) {
                fighter1 = defined.get(0);
                fighter2 = defined.get(1);
            }
            else {
                fighter2 = defined.get(0);
            }
        } else if (tgts.size() > 1) {
            fighter2 = tgts.get(1);
        }

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }

        sb.append(fighter1 + " fights " + fighter2);
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(Map<String, String> params, SpellAbility sa) {
        Card fighter1 = null;
        Card fighter2 = null;
        final Target tgt = sa.getTarget();
        ArrayList<Card> tgts = null;
        if (tgt != null) {
            tgts = tgt.getTargetCards();
            if (tgts.size() > 0) {
                fighter1 = tgts.get(0);
            }
        }
        if (params.containsKey("Defined")) {
            ArrayList<Card> defined = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
            // Allow both fighters to come from defined list if first fighter not already found
            if (defined.size() > 1 && fighter1 == null) {
                fighter1 = defined.get(0);
                fighter2 = defined.get(1);
            }
            else {
                fighter2 = defined.get(0);
            }
        } else if (tgts.size() > 1) {
            fighter2 = tgts.get(1);
        }

        if (fighter1 == null || fighter2 == null || !fighter1.isInPlay()
                || !fighter2.isInPlay()) {
            return;
        }

        int dmg2 = fighter2.getNetAttack();
        fighter2.addDamage(fighter1.getNetAttack(), fighter1);
        fighter1.addDamage(dmg2, fighter2);
    }

} 