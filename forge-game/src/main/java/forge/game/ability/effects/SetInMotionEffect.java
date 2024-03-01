package forge.game.ability.effects;


import java.util.Map;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityCantSetSchemesInMotion;
import forge.game.trigger.TriggerType;

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
                // Set the current scheme in motion again
                Game game = controller.getGame();

                if (StaticAbilityCantSetSchemesInMotion.any(game)) {
                    return;
                }

                game.getAction().moveToCommand(controller.getActiveScheme(), sa);

                // Run triggers
                final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
                runParams.put(AbilityKey.Scheme, controller.getActiveScheme());
                game.getTriggerHandler().runTrigger(TriggerType.SetInMotion, runParams, false);
            } else {
                controller.setSchemeInMotion(sa);
            }
        }
    }

}
