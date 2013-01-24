package forge.game.player;

import java.util.List;

import forge.Card;
import forge.control.input.Input;
import forge.game.GameState;
import forge.game.phase.CombatUtil;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ComputerAiInputBlock extends Input {

    private final GameState game;
    /**
     * TODO: Write javadoc for Constructor.
     * @param game
     * @param player
     */
    public ComputerAiInputBlock(GameState game, Player player) {
        super();
        this.game = game;
        this.player = player;
    }

    private final Player player; 
    
    private static final long serialVersionUID = -2253562658069995572L;

    @Override
    public void showMessage() {
        // TODO Auto-generated method stub
        final List<Card> blockers = player.getCreaturesInPlay();
        game.setCombat(ComputerUtilBlock.getBlockers(player, game.getCombat(), blockers));
        CombatUtil.orderMultipleCombatants(game.getCombat());
        game.getPhaseHandler().setPlayersPriorityPermission(false);
        
        stop();
    }
}
