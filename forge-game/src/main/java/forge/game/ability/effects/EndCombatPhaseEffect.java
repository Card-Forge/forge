package forge.game.ability.effects;


import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.CardCollection;
import forge.game.spellability.SpellAbility;

public class EndCombatPhaseEffect extends SpellAbilityEffect {

    /*
     * (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#resolve(forge.game.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Game game = sa.getActivatingPlayer().getGame();

        // If an effect attempts to end the combat phase at any time that’s not a combat phase, nothing happens
        if (game.getCombat() == null) {
            return;
        }

        // 1) All spells and abilities on the stack are exiled.
        game.getAction().exile(new CardCollection(game.getStackZone().getCards()), sa, null);
        game.getStack().clear();
        game.getStack().clearSimultaneousStack();
        game.getTriggerHandler().clearWaitingTriggers();

        // 2) State-based actions are checked. No player gets priority, and no
        // triggered abilities are put onto the stack.
        game.getAction().checkStateEffects(true);

        // 3) The current phase and step ends. The game skips straight to the postcombat main phase. As this happens,
        // all attacking and blocking creatures are removed from combat and effects that last “until end of combat” expire.
        game.getPhaseHandler().endCombat();
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
