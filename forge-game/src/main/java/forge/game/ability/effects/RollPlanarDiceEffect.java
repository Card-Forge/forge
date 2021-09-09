package forge.game.ability.effects;

import forge.game.Game;
import forge.game.PlanarDice;
import forge.game.ability.SpellAbilityEffect;
import forge.game.event.GameEventRollDie;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class RollPlanarDiceEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        boolean countedTowardsCost = !sa.hasParam("NotCountedTowardsCost");
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();

        if(countedTowardsCost) {
            game.getPhaseHandler().incPlanarDiceRolledthisTurn();
        }
        PlanarDice result = PlanarDice.roll(activator, null);
        // Play the die roll sound
        activator.getGame().fireEvent(new GameEventRollDie());
        String message = Localizer.getInstance().getMessage("lblPlayerRolledResult", activator.getName(), result.toString());
        game.getAction().notifyOfValue(sa, activator, message, null);

    }
}
