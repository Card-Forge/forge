package forge.game.ability.effects;

import com.google.common.collect.Lists;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;

import java.util.ArrayList;
import java.util.List;


public class ControlExchangeEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        Card object1 = null;
        Card object2 = null;
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        List<Card> tgts = tgt == null ? new ArrayList<Card>() : Lists.newArrayList(sa.getTargets().getTargetCards());
        if (tgts.size() > 0) {
            object1 = tgts.get(0);
        }
        if (sa.hasParam("Defined")) {
            List<Card> cards = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa);
            object2 = cards.isEmpty() ? null : cards.get(0);
            if (cards.size() > 1 && sa.hasParam("BothDefined")) {
                object1 = cards.get(1);
            }
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
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        List<Card> tgts = tgt == null ? new ArrayList<Card>() : Lists.newArrayList(sa.getTargets().getTargetCards());
        if (tgts.size() > 0) {
            object1 = tgts.get(0);
        }
        if (sa.hasParam("Defined")) {
            final List<Card> cards = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa);
            object2 = cards.isEmpty() ? null : cards.get(0);
            if (cards.size() > 1 && sa.hasParam("BothDefined")) {
                object1 = cards.get(1);
            }
        } else if (tgts.size() > 1) {
            object2 = tgts.get(1);
        }

        if (object1 == null || object2 == null || !object1.isInPlay()
                || !object2.isInPlay()) {
            return;
        }

        final Player player1 = object1.getController();
        final Player player2 = object2.getController();

        if (!object2.canBeControlledBy(player1) || !object1.canBeControlledBy(player2)) {
            return;
        }

        final long tStamp = sa.getActivatingPlayer().getGame().getNextTimestamp();
        object2.setController(player1, tStamp);
        object1.setController(player2, tStamp);
        if (sa.hasParam("RememberExchanged")) {
            sa.getHostCard().addRemembered(object1);
            sa.getHostCard().addRemembered(object2);
        }
    }

}
