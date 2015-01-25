package forge.deckchooser;

import forge.deck.DeckType;
import forge.gui.MouseUtil;
import forge.toolbox.FComboBoxWrapper;
import forge.toolbox.FSkin;
import forge.toolbox.FComboBox.TextAlignment;

import javax.swing.*;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class DecksComboBox extends FComboBoxWrapper<DeckType> {
    private List<IDecksComboBoxListener> _listeners = new ArrayList<>();
    private DeckType selectedDeckType = null;

    public DecksComboBox() {
        setSkinFont(FSkin.getBoldFont(14));
        setTextAlignment(TextAlignment.CENTER);
        addActionListener(getDeckTypeComboListener());
    }

    public void refresh(DeckType deckType) {
        setModel(new DefaultComboBoxModel<DeckType>(DeckType.values()));
        setSelectedItem(deckType);
    }

    private ActionListener getDeckTypeComboListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object selectedItem = getSelectedItem();
                if (selectedItem instanceof DeckType) {
                    MouseUtil.setCursor(Cursor.WAIT_CURSOR);
                    DeckType newDeckType = (DeckType)selectedItem;
                    if (newDeckType != selectedDeckType) {
                        selectedDeckType = newDeckType;
                        notifyDeckTypeSelected(newDeckType);
                    }
                    MouseUtil.resetCursor();
                }
            }
        };
    }

    public synchronized void addListener(IDecksComboBoxListener obj) {
        _listeners.add(obj);
    }

    public synchronized void removeListener(IDecksComboBoxListener obj) {
        _listeners.remove(obj);
    }

    private synchronized void notifyDeckTypeSelected(DeckType deckType) {
        if (deckType != null) {
            for (IDecksComboBoxListener listener : _listeners) {
                listener.deckTypeSelected(new DecksComboBoxEvent(this, deckType));
            }
        }
    }

    public DeckType getDeckType() {
        return selectedDeckType;
    }

    public void setDeckType(DeckType valueOf) {
        selectedDeckType = valueOf;
        setSelectedItem(selectedDeckType);
    }

    @Override
    public void setText(String text0) {
        selectedDeckType = null; //ensure selecting current deck type again raises event
        super.setText(text0);
    }
}
