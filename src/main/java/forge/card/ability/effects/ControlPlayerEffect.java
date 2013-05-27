package forge.card.ability.effects;

import java.util.List;

import forge.Command;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.game.GameState;
import forge.game.player.Player;
import forge.util.Lang;

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
        final GameState game = activator.getGame();

        List<Player> tgtPlayers = getTargetPlayers(sa);

        for(final Player pTarget: tgtPlayers) {
            
            // on next untap gain control
            game.getUntap().addUntil(pTarget, new Command() {
                @Override
                public void run() {
                    pTarget.obeyNewMaster(activator.getLobbyPlayer().createControllerFor(pTarget));
                    
                    // on following cleanup release control
                    game.getCleanup().addUntil(pTarget, new Command() {
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
