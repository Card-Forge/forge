package forge.card.ability.effects;


import forge.Card;
import forge.Singletons;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.game.GameState;
import forge.game.phase.PhaseType;
import forge.game.player.Player;

public class EndTurnEffect extends SpellAbilityEffect {

    // *************************************************************************
    // ************************* END TURN **************************************
    // *************************************************************************

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {

        GameState game = Singletons.getModel().getGame();
        // Steps taken from gatherer's rulings on Time Stop.
        // 1) All spells and abilities on the stack are exiled. This includes
        // Time Stop, though it will continue to resolve. It also includes
        // spells and abilities that can't be countered.
        for (final Card c : game.getStackZone().getCards()) {
            game.getAction().exile(c);
        }
        game.getStack().getStack().clear();

        // 2) All attacking and blocking creatures are removed from combat.
        game.getCombat().reset();

        // 3) State-based actions are checked. No player gets priority, and no
        // triggered abilities are put onto the stack.
        game.getAction().checkStateEffects();

        // 4) The current phase and/or step ends. The game skips straight to the
        // cleanup step. The cleanup step happens in its entirety.
        game.getPhaseHandler().setPhaseState(PhaseType.CLEANUP);

        // Update observers
        game.getStack().updateObservers();
        for (Player p : game.getPlayers()) {
            p.updateObservers();
            p.updateLabelObservers();
        }
    }


    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return "End the turn.";
    }

}
