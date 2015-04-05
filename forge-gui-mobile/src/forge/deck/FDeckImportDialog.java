/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.deck;

import forge.toolbox.FCheckBox;
import forge.toolbox.FComboBox;
import forge.toolbox.FDialog;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FOptionPane;
import forge.toolbox.FTextArea;
import forge.util.Callback;


public class FDeckImportDialog extends FDialog {
    private final Callback<Deck> callback;

    private final FTextArea txtInput = add(new FTextArea(true));
    private final FCheckBox newEditionCheck = new FCheckBox("Import latest version of card", true);
    private final FCheckBox dateTimeCheck = new FCheckBox("Use only sets released before:", false);
    private final FCheckBox onlyCoreExpCheck = new FCheckBox("Use only core and expansion sets", true);

    private final FComboBox<String> monthDropdown = new FComboBox<String>(); //don't need wrappers since skin can't change while this dialog is open
    private final FComboBox<Integer> yearDropdown = new FComboBox<Integer>();

    private final DeckImportController controller;

    public FDeckImportDialog(final boolean replacingDeck, final Callback<Deck> callback0) {
        super("Import Deck", 2);

        callback = callback0;
        controller = new DeckImportController(replacingDeck, newEditionCheck, dateTimeCheck, onlyCoreExpCheck, monthDropdown, yearDropdown);

        initButton(0, "OK", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                controller.parseInput(txtInput.getText());
                Deck deck = controller.accept();
                if (deck == null) { return; }

                hide();
                callback.run(deck);
            }
        });
        initButton(1, "Cancel", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                hide();
            }
        });
    }

    @Override
    protected float layoutAndGetHeight(float width, float maxHeight) {
        float padding = FOptionPane.PADDING;
        float x = padding;
        float y = padding;
        float w = width - 2 * padding;
        float h = maxHeight - 2 * padding;

        txtInput.setBounds(x, y, w, h);

        return maxHeight;
    }
}
