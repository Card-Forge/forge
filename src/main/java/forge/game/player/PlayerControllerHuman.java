package forge.game.player;

import java.util.List;

import javax.swing.JOptionPane;

import forge.Card;
import forge.Singletons;
import forge.card.spellability.SpellAbility;
import forge.control.input.Input;
import forge.control.input.InputBlock;
import forge.control.input.InputCleanup;
import forge.control.input.InputPassPriority;
import forge.game.GameState;
import forge.game.phase.PhaseType;
import forge.gui.GuiChoose;
import forge.gui.match.CMatchUI;


/** 
 * A prototype for player controller class
 * 
 * Handles phase skips for now.
 */
public class PlayerControllerHuman extends PlayerController {


    private PhaseType autoPassUntil = null;

    private final Input defaultInput;
    private final Input blockInput;
    private final Input cleanupInput;

    
    public final Input getDefaultInput() {
        return defaultInput;
    }

    public PlayerControllerHuman(GameState game0, HumanPlayer p) {
        super(game0, p);
        
        defaultInput = new InputPassPriority();
        blockInput = new InputBlock(player);
        cleanupInput = new InputCleanup(game);
    }

    public boolean mayAutoPass(PhaseType phase) {

        return phase.isBefore(autoPassUntil);
    }


    public boolean isUiSetToSkipPhase(final Player turn, final PhaseType phase) {
        boolean isLocalPlayer = player.equals(Singletons.getControl().getPlayer());
        return isLocalPlayer && !CMatchUI.SINGLETON_INSTANCE.stopAtPhase(turn, phase);
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

    /**
     * @return the cleanupInput
     */
    public Input getCleanupInput() {
        return cleanupInput;
    }

    /**
     * TODO: Write javadoc for this method.
     * @param c
     */
    public void playFromSuspend(Card c) {
        c.setSuspend(true);
        game.getAction().playCardWithoutManaCost(c, c.getOwner());
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#playCascade(java.util.List, forge.Card)
     */
    @Override
    public boolean playCascade(Card cascadedCard, Card sourceCard) {

        final StringBuilder title = new StringBuilder();
        title.append(sourceCard.getName()).append(" - Cascade Ability");
        final StringBuilder question = new StringBuilder();
        question.append("Cast ").append(cascadedCard.getName());
        question.append(" without paying its mana cost?");

        final int answer = JOptionPane.showConfirmDialog(null, question.toString(),
                title.toString(), JOptionPane.YES_NO_OPTION);

        boolean result =  answer == JOptionPane.YES_OPTION;
        game.getAction().playCardWithoutManaCost(cascadedCard, player);
        return result;
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#mayPlaySpellAbilityForFree(forge.card.spellability.SpellAbility)
     */
    @Override
    public void mayPlaySpellAbilityForFree(SpellAbility copySA) {
        game.getAction().playSpellAbilityForFree(copySA);
    }



}
