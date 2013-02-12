package forge.card.abilityfactory.effects;

import java.util.ArrayList;

import forge.Card;
import forge.card.abilityfactory.AbilityUtils;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;


public class ControlExchangeEffect extends SpellEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        Card object1 = null;
        Card object2 = null;
        final Target tgt = sa.getTarget();
        ArrayList<Card> tgts = tgt.getTargetCards();
        if (tgts.size() > 0) {
            object1 = tgts.get(0);
        }
        if (sa.hasParam("Defined")) {
            object2 = AbilityUtils.getDefinedCards(sa.getSourceCard(), sa.getParam("Defined"), sa).get(0);
        } else if (tgts.size() > 1) {
            object2 = tgts.get(1);
        }

        return object1 + " exchanges controller with " + object2;
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        Card object1 = null;
        Card object2 = null;
        final Target tgt = sa.getTarget();
        ArrayList<Card> tgts = tgt.getTargetCards();
        if (tgts.size() > 0) {
            object1 = tgts.get(0);
        }
        if (sa.hasParam("Defined")) {
            object2 = AbilityUtils.getDefinedCards(sa.getSourceCard(), sa.getParam("Defined"), sa).get(0);
        } else if (tgts.size() > 1) {
            object2 = tgts.get(1);
        }

        if (object1 == null || object2 == null || !object1.isInPlay()
                || !object2.isInPlay()) {
            return;
        }

        Player player2 = object2.getController();
        object2.addController(object1.getController());
        object1.addController(player2);
    }

}
