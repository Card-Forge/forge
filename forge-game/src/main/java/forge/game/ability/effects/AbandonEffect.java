package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

public class AbandonEffect extends SpellAbilityEffect {


    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        Card source = sa.getHostCard();
        Player controller = source.getController();

        final Game game = controller.getGame();
        
        game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        controller.getZone(ZoneType.Command).remove(source);
        game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);

        controller.getZone(ZoneType.SchemeDeck).add(source);
    }

}
