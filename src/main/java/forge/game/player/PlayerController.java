package forge.game.player;

import java.util.List;

import forge.Singletons;
import forge.card.spellability.SpellAbility;
import forge.control.input.Input;
import forge.game.phase.PhaseType;
import forge.gui.GuiChoose;
import forge.gui.match.CMatchUI;


/** 
 * A prototype for player controller class
 * 
 * Handles phase skips for now.
 */
public class PlayerController {

    // Should keep some 'Model' of player here.
    // Yet I have little idea of what is model now.
    private Player player;

    private PhaseType autoPassUntil = null;

    private Input defaultInput;
    private Input blockInput;

    public final Input getDefaultInput() {
        return defaultInput;
    }

    public PlayerController() {}
    void setPlayer(Player p) {
        player = p;
    }

    /**
     * TODO: Write javadoc for this method.
     * @param cleanup
     */
    public void autoPassTo(PhaseType cleanup) {
        autoPassUntil = cleanup;
    }
    public void autoPassCancel() {
        autoPassUntil = null;
    }


    public boolean mayAutoPass(PhaseType phase) {

        return phase.isBefore(autoPassUntil);
    }


    public boolean isUiSetToSkipPhase(final Player turn, final PhaseType phase) {
        boolean isLocalPlayer = player.equals(Singletons.getControl().getPlayer());
        return isLocalPlayer && !CMatchUI.SINGLETON_INSTANCE.stopAtPhase(turn, phase);
    }

    void setDefaultInput(Input input) {
        defaultInput = input;
    }

    /**
     * Uses GUI to learn which spell the player (human in our case) would like to play
     */
    public SpellAbility getAbilityToPlay(List<SpellAbility> abilities) {
        if (abilities.size() == 0) {
            return null;
        } else if (abilities.size() == 1) {
            return abilities.get(0);
        } else {
            return GuiChoose.oneOrNone("Choose", abilities); // some day network interaction will be here
        }
    }

    /** Input to use when player has to declare blockers */
    public Input getBlockInput() {
        return blockInput;
    }

    void setBlockInput(Input blockInput0) {
        this.blockInput = blockInput0; 
    }

}
