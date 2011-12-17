package forge.control.home;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
    private boolean directionsExpanded = false;

    /** @param v0 &emsp; ViewDraft */
    public ControlDraft(ViewDraft v0) {
        this.view = v0;
        updateHumanDecks();

        this.view.getTpnDirections().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (directionsExpanded) {
                    hideDirections();
                    directionsExpanded = false;
                }
                else {
                    showDirections();
                    directionsExpanded = true;
                }
            }
        });
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
        Deck human = view.getLstHumanDecks().getSelectedDeck();
        int aiIndex = view.getLstAIDecks().getSelectedIndex();

        if (human == null) {
            JOptionPane.showMessageDialog(null,
                    "No deck selected for human!\r\n(You may need to build a new deck.)",
                    "No deck", JOptionPane.ERROR_MESSAGE);
            return;
        }
        else if (human.getMain().countAll() < 40) {
            JOptionPane.showMessageDialog(null,
                    "The selected deck doesn't have enough cards to play (minimum 40)."
                    + "\r\nUse the deck editor to choose the cards you want before starting.",
                    "No deck", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Deck[] opponentDecks = AllZone.getDeckManager().getDraftDeck(human.getName());

        Constant.Runtime.HUMAN_DECK[0] = human;
        Constant.Runtime.COMPUTER_DECK[0] = opponentDecks[aiIndex];

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
        Collection<Deck[]> temp = AllZone.getDeckManager().getDraftDecks().values();
        List<Deck> human = new ArrayList<Deck>();
        for (Deck[] d : temp) { human.add(d[0]); }
        view.getLstHumanDecks().setDecks(human.toArray(new Deck[0]));
        view.getLstHumanDecks().setSelectedIndex(0);
    }

    private void showDirections() {
        view.getTpnDirections().setText(
                "Booster Draft Mode Instructions (Click to collapse)"
                + "\r\n\r\n"
                + "In a booster draft, several players (usually eight) are seated"
                + "around a table and each player is given three booster packs."
                + "\r\n\r\n"
                + "Each player opens a pack, selects a card from it and passes the remaining"
                + "cards to his or her left. Each player then selects one of the 14 remaining"
                + "cards from the pack that was just passed to him or her, and passes the"
                + "remaining cards to the left again. This continues until all of the cards"
                + "are depleted. The process is repeated with the second and third packs,"
                + "except that the cards are passed to the right in the second pack."
                + "\r\n\r\n"
                + "Players then build decks out of any of the cards that they selected"
                + "during the drafting and add as many basic lands as they want."
                + "\r\n\r\n"
                + "(Credit: Wikipedia <http://en.wikipedia.org/wiki/Magic:_The_Gathering_formats#Booster_Draft>)"
       );
    }

    private void hideDirections() {
        view.getTpnDirections().setText("Click here for draft mode instructions.");
    }
}
