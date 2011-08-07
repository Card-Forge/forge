package forge.gui.input;

import forge.*;

/**
 * <p>Input_Mulligan class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class Input_Mulligan extends Input {
    /** Constant <code>serialVersionUID=-8112954303001155622L</code> */
    private static final long serialVersionUID = -8112954303001155622L;

    /** {@inheritDoc} */
    @Override
    public void showMessage() {
        ButtonUtil.enableAll();
        AllZone.getDisplay().getButtonOK().setText("No");
        AllZone.getDisplay().getButtonCancel().setText("Yes");
        AllZone.getDisplay().showMessage("Do you want to Mulligan?");
    }

    /** {@inheritDoc} */
    @Override
    public void selectButtonOK() {
        end();
    }

    /** {@inheritDoc} */
    @Override
    public void selectButtonCancel() {
        AllZone.getGameInfo().setHumanMulliganedToZero(false);

        CardList hand = AllZoneUtil.getPlayerHand(AllZone.getHumanPlayer());
        for (Card c : hand)
            AllZone.getGameAction().moveToLibrary(c);

        for (int i = 0; i < 100; i++)
            AllZone.getHumanPlayer().shuffle();

        int newHand = hand.size() - 1;

        AllZone.getGameInfo().addHumanNumberOfTimesMulliganed(1);

        if (AllZone.getQuestData() != null) {
            if (AllZone.getQuestData().getInventory().hasItem("Sleight") && AllZone.getGameInfo().getHumanNumberOfTimesMulliganed() == 1)
                newHand++;
        }
        for (int i = 0; i < newHand; i++)
            AllZone.getHumanPlayer().drawCard();

        if (newHand == 0) {
            AllZone.getGameInfo().setHumanMulliganedToZero(true);
            end();
        }
    }//selectButtonOK()

    /**
     * <p>end.</p>
     */
    void end() {
        //Computer mulligan
        CardList CHandList = AllZoneUtil.getPlayerHand(AllZone.getComputerPlayer());

        Card dummy = CHandList.get(0);
        //Computer mulligans if there are no cards with converted mana cost of 0 in its hand
        while (CHandList.size() > 5 && CHandList.getValidCards("Card.cmcEQ0", AllZone.getComputerPlayer(), dummy).size() < 2) {
            for (Card c : CHandList)
                AllZone.getGameAction().moveToLibrary(c);

            for (int i = 0; i < 100; i++)
                AllZone.getComputerPlayer().shuffle();

            int newHand = CHandList.size() - 1;
            for (int i = 0; i < newHand; i++)
                AllZone.getComputerPlayer().drawCard();

            CHandList = AllZoneUtil.getPlayerHand(AllZone.getComputerPlayer());
        }

        //Human Leylines
        ButtonUtil.reset();
        CardList HHandList = AllZoneUtil.getPlayerHand(AllZone.getHumanPlayer());

        for (Card c : HHandList) {
            if (c.getName().startsWith("Leyline")) {
                if (GameActionUtil.showYesNoDialog(c, "Put onto Battlefield?"))
                    AllZone.getGameAction().moveToPlay(c);
            }
        }

        //Computer Leylines
        for (Card c : CHandList) {
            if (c.getName().startsWith("Leyline") && !(c.getName().startsWith("Leyline of Singularity")
                    && AllZoneUtil.getCardsInPlay("Leyline of Singularity").size() > 0)) {
                AllZone.getGameAction().moveToPlay(c);
                AllZone.getGameAction().checkStateEffects();
            }

        }
        if (AllZone.getGameAction().isStartCut() && !(HHandList.contains(AllZone.getGameAction().getHumanCut())
                || CHandList.contains(AllZone.getGameAction().getComputerCut()))) {
            AllZone.getGameAction().moveTo(AllZone.getZone(Constant.Zone.Library, AllZone.getHumanPlayer()), AllZone.getGameAction().getHumanCut());
            AllZone.getGameAction().moveTo(AllZone.getZone(Constant.Zone.Library, AllZone.getComputerPlayer()), AllZone.getGameAction().getComputerCut());
        }
        AllZone.getGameAction().checkStateEffects();
        Phase.setGameBegins(1);
        AllZone.getPhase().setNeedToNextPhase(false);
        stop();
    }
}
