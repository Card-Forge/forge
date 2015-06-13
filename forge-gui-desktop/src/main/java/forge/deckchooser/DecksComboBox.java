package forge.deckchooser;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.DefaultComboBoxModel;

import com.google.common.collect.Lists;

import forge.deck.DeckType;
import forge.gui.MouseUtil;
import forge.toolbox.FComboBox.TextAlignment;
import forge.toolbox.FComboBoxWrapper;
import forge.toolbox.FSkin;

public class DecksComboBox extends FComboBoxWrapper<DeckType> {
    private final List<IDecksComboBoxListener> _listeners = Lists.newArrayList();
    private DeckType selectedDeckType = null;

    public DecksComboBox() {
        setSkinFont(FSkin.getBoldFont(14));
        setTextAlignment(TextAlignment.CENTER);
        addActionListener(getDeckTypeComboListener());
    }

    public void refresh(final DeckType deckType) {
        setModel(new DefaultComboBoxModel<DeckType>(DeckType.ConstructedOptions));
        setSelectedItem(deckType);
    }

    private ActionListener getDeckTypeComboListener() {
        return new ActionListener() {
            @Override public void actionPerformed(final ActionEvent e) {
                final Object selectedItem = getSelectedItem();
                if (selectedItem instanceof DeckType) {
                    MouseUtil.setCursor(Cursor.WAIT_CURSOR);
                    final DeckType newDeckType = (DeckType)selectedItem;
                    if (newDeckType != selectedDeckType) {
                        selectedDeckType = newDeckType;
                        notifyDeckTypeSelected(newDeckType);
                    }
                    MouseUtil.resetCursor();
                }
            }
        };
    }

    public synchronized void addListener(final IDecksComboBoxListener obj) {
        _listeners.add(obj);
    }

    private synchronized void notifyDeckTypeSelected(final DeckType deckType) {
        if (deckType != null) {
            for (final IDecksComboBoxListener listener : _listeners) {
                listener.deckTypeSelected(new DecksComboBoxEvent(this, deckType));
            }
        }
    }

    public DeckType getDeckType() {
        return selectedDeckType;
    }

    public void setDeckType(final DeckType valueOf) {
        selectedDeckType = valueOf;
        setSelectedItem(selectedDeckType);
    }

    @Override
    public void setText(final String text0) {
        selectedDeckType = null; //ensure selecting current deck type again raises event
        super.setText(text0);
    }
}
