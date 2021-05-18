package forge.game.ability.effects;


import java.util.Map;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

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

                for (final Player p : game.getPlayers()) {
                    if (p.hasKeyword("Schemes can't be set in motion this turn.")) {
                        return;
                    }
                }

                game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
                game.getAction().moveTo(ZoneType.Command, controller.getActiveScheme(), null);
                game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);

                // Run triggers
                final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
                runParams.put(AbilityKey.Scheme, controller.getActiveScheme());
                game.getTriggerHandler().runTrigger(TriggerType.SetInMotion, runParams, false);
            } else {
                controller.setSchemeInMotion();
            }
        }
    }

}
