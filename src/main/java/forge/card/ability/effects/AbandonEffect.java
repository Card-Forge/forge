package forge.card.ability.effects;


import forge.Card;
import forge.Singletons;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.card.trigger.TriggerType;
import forge.game.GameState;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class AbandonEffect extends SpellAbilityEffect {

    private GameState game = Singletons.getModel().getGame();

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        Card source = sa.getSourceCard();
        Player controller = source.getController();

        game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        controller.getZone(ZoneType.Command).remove(source);
        game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);

        controller.getSchemeDeck().add(source);
    }

}
