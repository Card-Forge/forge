package forge.gui.input;

import forge.*;

/**
 * <p>Input_Cleanup class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class Input_Cleanup extends Input {
    /** Constant <code>serialVersionUID=-4164275418971547948L</code> */
    private static final long serialVersionUID = -4164275418971547948L;

    /** {@inheritDoc} */
    @Override
    public void showMessage() {
        if (AllZone.getPhase().getPlayerTurn().isComputer()) {
            AI_CleanupDiscard();
            return;
        }

        ButtonUtil.disableAll();
        int n = AllZoneUtil.getPlayerHand(AllZone.getHumanPlayer()).size();

        //MUST showMessage() before stop() or it will overwrite the next Input's message
        StringBuffer sb = new StringBuffer();
        sb.append("Cleanup Phase: You can only have a maximum of ").append(AllZone.getHumanPlayer().getMaxHandSize());
        sb.append(" cards, you currently have ").append(n).append(" cards in your hand - select a card to discard");
        AllZone.getDisplay().showMessage(sb.toString());

        //goes to the next phase
        if (n <= AllZone.getHumanPlayer().getMaxHandSize() || AllZone.getHumanPlayer().getMaxHandSize() == -1) {
            CombatUtil.removeAllDamage();

            AllZone.getPhase().setNeedToNextPhase(true);
            AllZone.getPhase().nextPhase();    // TODO: keep an eye on this code, see if we can get rid of it.
        }
    }

    /** {@inheritDoc} */
    @Override
    public void selectCard(Card card, PlayerZone zone) {
        if (zone.is(Constant.Zone.Hand, AllZone.getHumanPlayer())) {
            card.getController().discard(card, null);
            if (AllZone.getStack().size() == 0)
                showMessage();
        }
    }//selectCard()


    /**
     * <p>AI_CleanupDiscard.</p>
     */
    public void AI_CleanupDiscard() {
        int size = AllZoneUtil.getPlayerHand(AllZone.getComputerPlayer()).size();

        if (AllZone.getComputerPlayer().getMaxHandSize() != -1) {
            int numDiscards = size - AllZone.getComputerPlayer().getMaxHandSize();
            AllZone.getComputerPlayer().discard(numDiscards, null, false);
        }
        CombatUtil.removeAllDamage();

        AllZone.getPhase().setNeedToNextPhase(true);
    }
}
