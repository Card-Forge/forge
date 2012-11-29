package forge.card.abilityfactory.effects;

import java.util.ArrayList;

import forge.Card;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;

public class FightEffect extends SpellEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        ArrayList<Card> fighters = getFighters(sa);

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }

        if (fighters.size() > 1) {
            sb.append(fighters.get(0) + " fights " + fighters.get(1));
        }
        else {
            sb.append(fighters.get(0) + " fights unknown");
        }
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        ArrayList<Card> fighters = getFighters(sa);

        if (fighters.size() < 2 || !fighters.get(0).isInPlay()
                || !fighters.get(1).isInPlay()) {
            return;
        }

        int dmg2 = fighters.get(1).getNetAttack();
        fighters.get(1).addDamage(fighters.get(0).getNetAttack(), fighters.get(0));
        fighters.get(0).addDamage(dmg2, fighters.get(1));
    }

    private static ArrayList<Card> getFighters(SpellAbility sa) {
        final ArrayList<Card> fighterList = new ArrayList<Card>();

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
        if (sa.hasParam("Defined")) {
            ArrayList<Card> defined = AbilityFactory.getDefinedCards(sa.getSourceCard(), sa.getParam("Defined"), sa);
            // Allow both fighters to come from defined list if first fighter not already found
            if (!defined.isEmpty()) {
                if (defined.size() > 1 && fighter1 == null) {
                    fighter1 = defined.get(0);
                    fighter2 = defined.get(1);
                }
                else {
                    fighter2 = defined.get(0);
                }
            }
        } else if (tgts.size() > 1) {
            fighter2 = tgts.get(1);
        }

        if (fighter1 != null) fighterList.add(fighter1);
        if (fighter2 != null) fighterList.add(fighter2);

        return fighterList;
    }

}
