package forge.game.ability.effects;

import com.google.common.collect.Maps;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

import java.util.Map;

public class AbandonEffect extends SpellAbilityEffect {


    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        Card source = sa.getHostCard();
        Player controller = source.getController();

        boolean isOptional = sa.hasParam("Optional");
        if (isOptional && !controller.getController().confirmAction(sa, null, "Would you like to abandon the scheme " + source + "?")) {
            return;
        }

        final Game game = controller.getGame();

        if (sa.hasParam("RememberAbandoned")) {
            source.addRemembered(source);
        }
        
        game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        controller.getZone(ZoneType.Command).remove(source);
        game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);

        controller.getZone(ZoneType.SchemeDeck).add(source);

        // Run triggers
        final Map<String, Object> runParams = Maps.newHashMap();
        runParams.put("Scheme", source);
        game.getTriggerHandler().runTrigger(TriggerType.Abandoned, runParams, false);
    }

}
