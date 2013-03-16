package forge.card.ability.effects;

import java.util.ArrayList;

import forge.Card;
import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;


public class ControlExchangeEffect extends SpellAbilityEffect {

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

        final Player player2 = object2.getController();
        final long tStamp = Singletons.getModel().getGame().getNextTimestamp();
        object2.setController(object1.getController(), tStamp);
        object1.setController(player2, tStamp);
    }

}
