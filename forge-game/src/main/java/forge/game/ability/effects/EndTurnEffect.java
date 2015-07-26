package forge.game.ability.effects;


import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

public class EndTurnEffect extends SpellAbilityEffect {

    // *************************************************************************
    // ************************* END TURN **************************************
    // *************************************************************************

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        Game game = sa.getActivatingPlayer().getGame();
        // Steps taken from gatherer's rulings on Time Stop.
        // 1) All spells and abilities on the stack are exiled. This includes
        // Time Stop, though it will continue to resolve. It also includes
        // spells and abilities that can't be countered.
        for (final Card c : game.getStackZone().getCards()) {
            game.getAction().exile(c);
        }
        game.getStack().clear();
        game.getStack().clearSimultaneousStack();
        game.getTriggerHandler().clearWaitingTriggers();

        // 2) All attacking and blocking creatures are removed from combat.
        game.getPhaseHandler().endCombat();

        // 3) State-based actions are checked. No player gets priority, and no
        // triggered abilities are put onto the stack.
        game.getAction().checkStateEffects(true);

        // 4) The current phase and/or step ends. The game skips straight to the
        // cleanup step. The cleanup step happens in its entirety.
        game.getPhaseHandler().endTurnByEffect();
    }


    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return "End the turn.";
    }

}
