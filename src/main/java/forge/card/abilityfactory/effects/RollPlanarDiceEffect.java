package forge.card.abilityfactory.effects;

import forge.Singletons;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.game.PlanarDice;
import forge.game.player.Player;
import forge.gui.GuiDialog;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class RollPlanarDiceEffect extends SpellEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        boolean countedTowardsCost = !sa.hasParam("NotCountedTowardsCost");
        
        Player roller = sa.getActivatingPlayer();
        if(countedTowardsCost)
        {
            
            Singletons.getModel().getGame().getPhaseHandler().incPlanarDiceRolledthisTurn();
        }
        PlanarDice result = PlanarDice.roll(roller);
        
        GuiDialog.message(roller.getName() + " rolled " + result.toString());

    }
}
