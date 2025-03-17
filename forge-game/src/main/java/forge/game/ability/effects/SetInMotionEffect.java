package forge.game.ability.effects;

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
                controller.setSchemeInMotion(sa, controller.getActiveScheme());
            } else {
                controller.setSchemeInMotion(sa);
            }
        }
    }

}
