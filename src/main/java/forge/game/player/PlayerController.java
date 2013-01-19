package forge.game.player;

import java.util.List;

import forge.Singletons;
import forge.card.spellability.SpellAbility;
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
    private final Player player;

    private PhaseType autoPassUntil = null;

    private ComputerAIInput aiInput;

    public final ComputerAIInput getAiInput() {
        return aiInput;
    }

    public PlayerController(Player player0) {
        player = player0;
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
        return player.equals(Singletons.getControl().getPlayer()) && !CMatchUI.SINGLETON_INSTANCE.stopAtPhase(turn, phase);
    }

    /**
     * TODO: Write javadoc for this method.
     * @param computerAIInput
     */
    public void setAiInput(ComputerAIInput computerAIInput) {
        aiInput = computerAIInput;
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

}
