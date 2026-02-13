package forge.game.ability.effects;

import java.util.Map;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.CardCollection;
import forge.game.card.CardZoneTable;
import forge.game.spellability.SpellAbility;

public class EndCombatPhaseEffect extends SpellAbilityEffect {

    /*
     * (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#resolve(forge.game.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Game game = sa.getActivatingPlayer().getGame();

        // CR 723.2g If an effect attempts to end the combat phase at any time that’s not a combat phase, nothing happens
        if (game.getCombat() == null) {
            return;
        }

        // CR 721.2a
        game.getTriggerHandler().clearWaitingTriggers(); 

        // 1) All spells and abilities on the stack are exiled.
        Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
        CardZoneTable zoneMovements = AbilityKey.addCardZoneTableParams(moveParams, sa);

        game.getAction().exile(new CardCollection(game.getStackZone().getCards()), sa, moveParams);

        zoneMovements.triggerChangesZoneAll(game, sa);

        game.getStack().clear();
        game.getStack().clearSimultaneousStack();

        // 2) State-based actions are checked. No player gets priority, and no
        // triggered abilities are put onto the stack.
        game.getAction().checkStateEffects(true);

        // 3) The current phase and step ends. The game skips straight to the postcombat main phase. As this happens,
        // all attacking and blocking creatures are removed from combat and effects that last “until end of combat” expire.
        game.getPhaseHandler().endCombatPhaseByEffect();
    }

    /*
     * (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#getStackDescription(forge.game.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        return "End the combat phase.";
    }

}
