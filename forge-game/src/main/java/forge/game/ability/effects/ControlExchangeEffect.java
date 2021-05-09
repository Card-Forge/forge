package forge.game.ability.effects;

import java.util.List;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.CardTranslation;
import forge.util.Localizer;


public class ControlExchangeEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        Card object1 = null;
        Card object2 = null;
        List<Card> tgts = null;
        if (sa.usesTargeting()) {
            tgts = Lists.newArrayList(sa.getTargets().getTargetCards());
            if (tgts.size() > 0) {
                object1 = tgts.get(0);
            }
        }
        if (sa.hasParam("Defined")) {
            List<Card> cards = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa);
            object2 = cards.isEmpty() ? null : cards.get(0);
            if (cards.size() > 1 && !sa.usesTargeting()) {
                object1 = cards.get(1);
            }
        } else if (tgts.size() > 1) {
            object2 = tgts.get(1);
        }

        if (object1 == null || object2 == null) {
            return "";
        }

        return object1 + " exchanges controller with " + object2;
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        Card host = sa.getHostCard();
        Game game = host.getGame();
        Card object1 = null;
        Card object2 = null;

        List<Card> tgts = null;
        if (sa.usesTargeting()) {
            tgts = Lists.newArrayList(sa.getTargets().getTargetCards());
            if (tgts.size() > 0) {
                object1 = tgts.get(0);
            }
        }
        if (sa.hasParam("Defined")) {
            final List<Card> cards = AbilityUtils.getDefinedCards(host, sa.getParam("Defined"), sa);
            object2 = cards.isEmpty() ? null : cards.get(0);
            if (cards.size() > 1 && !sa.usesTargeting()) {
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

        if (sa.hasParam("Optional")) {
            if (!sa.getActivatingPlayer().getController().confirmAction(sa, null,
                    Localizer.getInstance().getMessage("lblExchangeControl",
                            CardTranslation.getTranslatedName(object1.getName()),
                            CardTranslation.getTranslatedName(object2.getName())), null)) {
                return;
            }
        }

        final long tStamp = game.getNextTimestamp();
        object2.setController(player1, tStamp);
        object1.setController(player2, tStamp);
        if (sa.hasParam("RememberExchanged")) {
            sa.getHostCard().addRemembered(object1);
            sa.getHostCard().addRemembered(object2);
        }
    }

}
