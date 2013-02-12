package forge.card.ability.effects;

import forge.Singletons;
import forge.card.ability.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.game.GameState;
import forge.game.player.Player;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class PlaneswalkEffect extends SpellEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        GameState game = Singletons.getModel().getGame();
        
        System.out.println("AF Planeswalking!");
        
        for(Player p : game.getPlayers())
        {
            p.leaveCurrentPlane();
        }
        sa.getActivatingPlayer().planeswalk();
    }

}
