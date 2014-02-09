package forge.game.ability.effects;

import forge.Command;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;

import java.util.List;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ControlPlayerEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        
        List<Player> tgtPlayers = getTargetPlayers(sa);
        return String.format("%s controls %s during their next turn", sa.getActivatingPlayer(), Lang.joinHomogenous(tgtPlayers));
    }
    
    @SuppressWarnings("serial")
    @Override
    public void resolve(SpellAbility sa) {
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();

        List<Player> tgtPlayers = getTargetPlayers(sa);

        for(final Player pTarget: tgtPlayers) {
            
            // on next untap gain control
            game.getUntap().addUntil(pTarget, new Command() {
                @Override
                public void run() {
                    pTarget.setControllingPlayerController(activator.getLobbyPlayer().createControllerFor(pTarget));
                    
                    // on following cleanup release control
                    game.getEndOfTurn().addUntil(new Command() {
                        @Override
                        public void run() {
                            pTarget.releaseControl();
                        }
                    });
                }
            });
            
        }
    }
}
