package forge.control.home;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JOptionPane;

import forge.AllZone;
import forge.Constant;
import forge.Singletons;
import forge.control.FControl;
import forge.deck.Deck;
import forge.game.GameType;
import forge.game.limited.BoosterDraft;
import forge.game.limited.CardPoolLimitation;
import forge.gui.GuiUtils;
import forge.gui.deckeditor.DeckEditorDraft;
import forge.view.GuiTopLevel;
import forge.view.home.ViewDraft;
import forge.view.toolbox.FSkin;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ControlDraft {
    private final ViewDraft view;
    private final MouseAdapter madDirections;

    /** @param v0 &emsp; ViewDraft */
    public ControlDraft(ViewDraft v0) {
        this.view = v0;
        updateHumanDecks();

        madDirections = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                view.showDirections();
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                view.getLblDirections().setForeground(Singletons.getView().getSkin().getColor(FSkin.SkinProp.CLR_HOVER));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                view.getLblDirections().setForeground(Singletons.getView().getSkin().getColor(FSkin.SkinProp.CLR_TEXT));
            }
        };

        addListeners();
    }

    private void addListeners() {
        view.getLblDirections().addMouseListener(madDirections);
    }

    /** */
    public void setupDraft() {
        final DeckEditorDraft draft = new DeckEditorDraft();

        // Determine what kind of booster draft to run
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
            throw new IllegalStateException("startButton() error - computer deck is null");
        }

        Constant.Runtime.setGameType(GameType.Draft);

        GuiTopLevel g = (GuiTopLevel) AllZone.getDisplay();
        g.getController().changeState(FControl.MATCH_SCREEN);
        g.getController().getMatchController().initMatch();
        AllZone.getGameAction().newGame(Constant.Runtime.HUMAN_DECK[0], Constant.Runtime.COMPUTER_DECK[0]);
    }

    /** Updates deck list in view. */
    public void updateHumanDecks() {
        Collection<Deck[]> temp = AllZone.getDeckManager().getDraftDecks().values();
        List<Deck> human = new ArrayList<Deck>();
        for (Deck[] d : temp) { human.add(d[0]); }
        view.getLstHumanDecks().setDecks(human.toArray(new Deck[0]));
    }
}
