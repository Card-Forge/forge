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

import java.util.List;

import forge.FThreads;
import forge.Forge;
import forge.deck.DeckRecognizer.TokenType;
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
    private final FCheckBox newEditionCheck = add(new FCheckBox("Import latest version of card", true));
    private final FCheckBox dateTimeCheck = add(new FCheckBox("Use only sets released before:", false));
    private final FCheckBox onlyCoreExpCheck = add(new FCheckBox("Use only core and expansion sets", true));

    private final FComboBox<String> monthDropdown = add(new FComboBox<String>()); //don't need wrappers since skin can't change while this dialog is open
    private final FComboBox<Integer> yearDropdown = add(new FComboBox<Integer>());

    private final DeckImportController controller;

    public FDeckImportDialog(final boolean replacingDeck, final Callback<Deck> callback0) {
        super("Import from Clipboard", 2);

        callback = callback0;
        controller = new DeckImportController(replacingDeck, newEditionCheck, dateTimeCheck, onlyCoreExpCheck, monthDropdown, yearDropdown);
        txtInput.setText(Forge.getClipboard().getContents()); //just pull import directly off the clipboard

        initButton(0, "Import", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                FThreads.invokeInBackgroundThread(new Runnable() {
                    @Override
                    public void run() {
                        final Deck deck = controller.accept(); //must accept in background thread in case a dialog is shown
                        if (deck == null) { return; }

                        FThreads.invokeInEdtLater(new Runnable() {
                            @Override
                            public void run() {
                                hide();
                                callback.run(deck);
                            }
                        });
                    }
                });
            }
        });
        initButton(1, "Cancel", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                hide();
            }
        });

        List<DeckRecognizer.Token> tokens = controller.parseInput(txtInput.getText());

        //ensure at least one known card found on clipboard
        for (DeckRecognizer.Token token : tokens) {
            if (token.getType() == TokenType.KnownCard) {
                return;
            }
        }

        setButtonEnabled(0, false);
        txtInput.setText("No known cards found on clipboard.\n\nCopy the decklist to the clipboard, then reopen this dialog.");
    }

    @Override
    protected float layoutAndGetHeight(float width, float maxHeight) {
        float padding = FOptionPane.PADDING;
        float w = width - 2 * padding;
        float h = txtInput.getPreferredHeight(w);
        float maxTextBoxHeight = maxHeight - 2 * padding;
        if (h > maxTextBoxHeight) {
            h = maxTextBoxHeight;
        }
        txtInput.setBounds(padding, padding, w, h);
        return h + 2 * padding;
    }
}
