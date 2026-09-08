package forge.game.ability.effects;

import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class SetInMotionEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        Card source = sa.getHostCard();
        Player controller = source.getController();
        boolean again = sa.hasParam("Again");

        int repeats = 1;
        if (sa.hasParam("RepeatNum")) {
            repeats = AbilityUtils.calculateAmount(source, sa.getParam("RepeatNum"), sa);
        }

        for (int i = 0; i < repeats; i++) {
            if (again) {
                Card scheme = null;

                Object triggeredScheme = sa.getRootAbility().getTriggeringObject(AbilityKey.Scheme);
                if (triggeredScheme instanceof Card) {
                    scheme = controller.getGame().getCardState((Card) triggeredScheme, null);
                }

                if (scheme == null) {
                    scheme = controller.getActiveScheme();
                }

                if (scheme != null) {
                    controller.setSchemeInMotion(sa, scheme);
                }
            } else {
                controller.setSchemeInMotion(sa);
            }
        }
    }
}
