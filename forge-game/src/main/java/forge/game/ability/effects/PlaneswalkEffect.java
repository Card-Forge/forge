package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import java.util.List;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class PlaneswalkEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        Game game = sa.getActivatingPlayer().getGame();
        
        for(Player p : game.getPlayers())
        {
            p.leaveCurrentPlane();
        }
        if(sa.hasParam("Defined")) {
            List<Card> destinations = AbilityUtils.getDefinedCards(sa.getSourceCard(), sa.getParam("Defined"), sa);
            sa.getActivatingPlayer().planeswalkTo(destinations);
        }
        else
        {
            sa.getActivatingPlayer().planeswalk();
        }
        
    }

}
