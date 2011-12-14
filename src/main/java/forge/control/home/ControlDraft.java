package forge.control.home;

import java.util.ArrayList;

import javax.swing.JOptionPane;

import forge.AllZone;
import forge.Constant;
import forge.deck.Deck;
import forge.game.limited.BoosterDraft;
import forge.game.limited.CardPoolLimitation;
import forge.gui.GuiUtils;
import forge.gui.deckeditor.DeckEditorDraft;
import forge.view.GuiTopLevel;
import forge.view.home.ViewDraft;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ControlDraft {
    private ViewDraft view;

    /** @param v0 &emsp; ViewDraft */
    public ControlDraft(ViewDraft v0) {
        this.view = v0;
        updateHumanDecks();
    }

    /** */
    public void setupDraft() {
        final DeckEditorDraft draft = new DeckEditorDraft();

        // determine what kind of booster draft to run
        final ArrayList<String> draftTypes = new ArrayList<String>();
        draftTypes.add("Full Cardpool");
        draftTypes.add("Block / Set");
        draftTypes.add("Custom");

        final String prompt = "Choose Draft Format:";
        final Object o = GuiUtils.getChoice(prompt, draftTypes.toArray());

        if (o.toString().equals(draftTypes.get(0))) {
            draft.showGui(new BoosterDraft(CardPoolLimitation.Full));
        }

        else if (o.toString().equals(draftTypes.get(1))) {
            draft.showGui(new BoosterDraft(CardPoolLimitation.Block));
        }

        else if (o.toString().equals(draftTypes.get(2))) {
            draft.showGui(new BoosterDraft(CardPoolLimitation.Custom));
        }

    }

    /** */
    public void start() {
        String human = view.getLstHumanDecks().getSelectedValue().toString();
        int ai = view.getLstAIDecks().getSelectedIndex();

        if (human == null) {
            JOptionPane.showMessageDialog(null,
                    "Draft a new deck, save, and select before starting.",
                    "No deck", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // load old draft
        Deck[] deck = AllZone.getDeckManager().getDraftDeck(human);

        Constant.Runtime.HUMAN_DECK[0] = deck[0];
        Constant.Runtime.COMPUTER_DECK[0] = deck[ai];

        if (Constant.Runtime.COMPUTER_DECK[0] == null) {
            throw new IllegalStateException("OldGuiNewGame : startButton() error - computer deck is null");
        }

        GuiTopLevel g = (GuiTopLevel) AllZone.getDisplay();
        g.getController().changeState(1);
        g.getController().getMatchController().initMatch();
        AllZone.getGameAction().newGame(Constant.Runtime.HUMAN_DECK[0], Constant.Runtime.COMPUTER_DECK[0]);
    }

    /** Updates deck list in view. */
    public void updateHumanDecks() {
        view.getLstHumanDecks().setListData(AllZone.getDeckManager().getDraftDecks().keySet().toArray());
        view.getLstHumanDecks().setSelectedIndex(0);
    }
}
