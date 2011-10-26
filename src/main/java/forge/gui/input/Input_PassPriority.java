package forge.gui.input;

import forge.AllZone;
import forge.ButtonUtil;
import forge.Card;
import forge.GuiDisplayUtil;
import forge.Player;
import forge.PlayerZone;

/**
 * <p>
 * Input_PassPriority class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Input_PassPriority extends Input implements java.io.Serializable {
    /** Constant <code>serialVersionUID=-581477682214137181L</code>. */
    private static final long serialVersionUID = -581477682214137181L;

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        GuiDisplayUtil.updateGUI();
        ButtonUtil.enableOnlyOK();

        String phase = AllZone.getPhase().getPhase();
        Player player = AllZone.getPhase().getPriorityPlayer();

        if (player.isComputer()) {
            System.out.println(phase + ": Computer in passpriority");
        }

        StringBuilder sb = new StringBuilder();

        sb.append("Turn : ").append(AllZone.getPhase().getPlayerTurn()).append("\n");
        sb.append("Phase: ").append(phase).append("\n");
        sb.append("Stack: ");
        if (AllZone.getStack().size() != 0) {
            sb.append(AllZone.getStack().size()).append(" to Resolve.");
        } else {
            sb.append("Empty");
        }
        sb.append("\n");
        sb.append("Priority: ").append(player);

        AllZone.getDisplay().showMessage(sb.toString());
    }

    /** {@inheritDoc} */
    @Override
    public final void selectButtonOK() {
        AllZone.getPhase().passPriority();
        GuiDisplayUtil.updateGUI();
        Input in = AllZone.getInputControl().getInput();
        if (in == this || in == null) {
            AllZone.getInputControl().resetInput();
        // Clear out PassPriority after clicking button
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void selectCard(final Card card, final PlayerZone zone) {
        if (AllZone.getGameAction().playCard(card)) {
            AllZone.getPhase().setPriority(AllZone.getHumanPlayer());
        }
    } // selectCard()
}
