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
package forge.screens.deckeditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import forge.deck.Deck;
import forge.deck.DeckBase;
import forge.deck.DeckImportController;
import forge.deck.DeckRecognizer;
import forge.deck.DeckRecognizer.TokenType;
import forge.item.InventoryItem;
import forge.screens.deckeditor.controllers.ACEditorBase;
import forge.toolbox.FButton;
import forge.toolbox.FCheckBox;
import forge.toolbox.FComboBox;
import forge.toolbox.FHtmlViewer;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FTextArea;
import forge.view.FDialog;

/**
 *
 * Dialog for quick import of decks.
 *
 * @param <TItem>
 * @param <TModel>
 */
public class DeckImport<TItem extends InventoryItem, TModel extends DeckBase> extends FDialog {
    private static final long serialVersionUID = -5837776824284093004L;

    private final FTextArea txtInput = new FTextArea();
    private static final String STYLESHEET = "<style>"
            + "body, h1, h2, h3, h4, h5, h6, table, tr, td, p {margin: 3px 1px; padding: 0; font-weight: "
            + "normal; font-style: normal; text-decoration: none; font-family: Arial; font-size: 10px; background-color: white;} "
            +
            // "h1 {border-bottom: solid 1px black; color: blue; font-size: 12px; margin: 3px 0 9px 0; } "
            // +
            ".comment {color: #666666;} " + ".knowncard {color: #009900;} " + ".unknowncard {color: #990000;} "
            + ".section {padding: 3px 10px; margin: 3px 0; font-weight: 700; background-color: #DDDDDD; } "
            + "</style>";
    private static final String HTML_WELCOME_TEXT = "<html>"
            + DeckImport.STYLESHEET
            + "<h3>You'll see recognized cards here</h3>"
            + "<div class='section'>Legend</div>"
            + "<ul>"
            + "<li class='knowncard'>Recognized cards will be shown in green. These cards will be auto-imported into a new deck<BR></li>"
            + "<li class='unknowncard'>Lines which seem to be cards but are either misspelled or unsupported by Forge, are shown in dark-red<BR></li>"
            + "<li class='comment'>Lines that appear unsignificant will be shown in gray<BR><BR></li>" + "</ul>"
            + "<div class='section'>Choosing source</div>"
            + "<p>In most cases when you paste from clipboard a carefully selected area of a webpage, it works perfectly.</p>"
            + "<p>Sometimes to filter out unneeded data you may have to export deck in MTGO format, and paste here downloaded file contents.</p>"
            + "<p>Sideboard recognition is supported. Make sure that the sideboard cards are listed after a line that contains the word 'Sideboard'</p>"
            + "</html>";

    private final FHtmlViewer htmlOutput = new FHtmlViewer(DeckImport.HTML_WELCOME_TEXT);
    private final FScrollPane scrollInput = new FScrollPane(this.txtInput, false);
    private final FScrollPane scrollOutput = new FScrollPane(this.htmlOutput, false);
    private final FLabel summaryMain = new FLabel.Builder().text("Imported deck summary will appear here").build();
    private final FLabel summarySide = new FLabel.Builder().text("Line for sideboard summary").build();
    private final FButton cmdAccept = new FButton("Import Deck");
    private final FButton cmdCancel = new FButton("Cancel");
    private final FCheckBox newEditionCheck = new FCheckBox("Import latest version of card", true);
    private final FCheckBox dateTimeCheck = new FCheckBox("Use only sets released before:", false);
    private final FCheckBox onlyCoreExpCheck = new FCheckBox("Use only core and expansion sets", true);

    private final FComboBox<String> monthDropdown = new FComboBox<String>(); //don't need wrappers since skin can't change while this dialog is open
    private final FComboBox<Integer> yearDropdown = new FComboBox<Integer>();

    private final DeckImportController controller;
    private final ACEditorBase<TItem, TModel> host;


    public DeckImport(final ACEditorBase<TItem, TModel> g, final boolean allowCardsFromAllSets) {
        this.controller = new DeckImportController(!g.getDeckController().isEmpty(),
                newEditionCheck, dateTimeCheck, onlyCoreExpCheck, monthDropdown, yearDropdown);
        this.host = g;

        final int wWidth = 700;
        final int wHeight = 600;

        this.setPreferredSize(new java.awt.Dimension(wWidth, wHeight));
        this.setSize(wWidth, wHeight);
        this.setTitle("Deck Importer");

        txtInput.setFocusable(true);
        txtInput.setEditable(true);

        final FSkin.SkinColor foreColor = FSkin.getColor(FSkin.Colors.CLR_TEXT);
        this.scrollInput.setBorder(new FSkin.TitledSkinBorder(BorderFactory.createEtchedBorder(), "Paste or type a decklist", foreColor));
        this.scrollOutput.setBorder(new FSkin.TitledSkinBorder(BorderFactory.createEtchedBorder(), "Expect the recognized lines to appear", foreColor));
        this.scrollInput.setViewportBorder(BorderFactory.createLoweredBevelBorder());
        this.scrollOutput.setViewportBorder(BorderFactory.createLoweredBevelBorder());

        this.add(this.scrollInput, "cell 0 0, w 50%, growy, pushy");
        this.add(this.newEditionCheck, "cell 0 1, w 50%, ax c");
        this.add(this.dateTimeCheck, "cell 0 2, w 50%, ax c");

        this.add(monthDropdown, "cell 0 3, w 20%, ax left, split 2, pad 0 4 0 0");
        this.add(yearDropdown, "w 15%");

        this.onlyCoreExpCheck.setSelected(!allowCardsFromAllSets);
        this.add(this.onlyCoreExpCheck, "cell 0 4, w 50%, ax c");

        this.add(this.scrollOutput, "cell 1 0, w 50%, growy, pushy");
        this.add(this.summaryMain, "cell 1 1, label");
        this.add(this.summarySide, "cell 1 2, label");

        this.add(this.cmdAccept, "cell 1 4, split 2, w 150, align r, h 26");
        this.add(this.cmdCancel, "w 150, h 26");

        this.cmdCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                DeckImport.this.processWindowEvent(new WindowEvent(DeckImport.this, WindowEvent.WINDOW_CLOSING));
            }
        });

        this.cmdAccept.addActionListener(new ActionListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void actionPerformed(final ActionEvent e) {
                final Deck toSet = controller.accept();
                if (toSet == null) { return; }

                DeckImport.this.host.getDeckController().setModel((TModel) toSet);
                DeckImport.this.processWindowEvent(new WindowEvent(DeckImport.this, WindowEvent.WINDOW_CLOSING));
            }
        });

        final ActionListener updateDateCheck = new ActionListener() {
            @Override public void actionPerformed(final ActionEvent e) {
                final boolean isSel = dateTimeCheck.isSelected();
                monthDropdown.setEnabled(isSel);
                yearDropdown.setEnabled(isSel);
                parseAndDisplay();
            }
        };
        this.dateTimeCheck.addActionListener(updateDateCheck);

        final ActionListener reparse = new ActionListener() {
            @Override public void actionPerformed(final ActionEvent e) {
                parseAndDisplay();
            }
        };
        this.newEditionCheck.addActionListener(reparse);
        this.onlyCoreExpCheck.addActionListener(reparse);
        this.yearDropdown.addActionListener(reparse);
        this.monthDropdown.addActionListener(reparse);
        updateDateCheck.actionPerformed(null); // update actual state

        this.txtInput.getDocument().addDocumentListener(new OnChangeTextUpdate());
        this.cmdAccept.setEnabled(false);
    }

    /**
     * The Class OnChangeTextUpdate.
     */
    protected class OnChangeTextUpdate implements DocumentListener {
        private void onChange() {
            parseAndDisplay();
        }

        @Override
        public final void insertUpdate(final DocumentEvent e) {
            this.onChange();
        }

        @Override
        public final void removeUpdate(final DocumentEvent e) {
            this.onChange();
        }

        @Override
        public void changedUpdate(final DocumentEvent e) {
        } // Happend only on ENTER pressed
    }

    private void parseAndDisplay() {
        final List<DeckRecognizer.Token> tokens = controller.parseInput(txtInput.getText());
        displayTokens(tokens);
        updateSummaries(tokens);
    }

    private void displayTokens(final List<DeckRecognizer.Token> tokens) {
        if (tokens.isEmpty()) {
            htmlOutput.setText(HTML_WELCOME_TEXT);
        }
        else {
            final StringBuilder sbOut = new StringBuilder("<html>");
            sbOut.append(DeckImport.STYLESHEET);
            for (final DeckRecognizer.Token t : tokens) {
                sbOut.append(makeHtmlViewOfToken(t));
            }
            sbOut.append("</html>");
            htmlOutput.setText(sbOut.toString());
        }
    }

    private void updateSummaries(final List<DeckRecognizer.Token> tokens) {
        final int[] cardsOk = new int[2];
        final int[] cardsUnknown = new int[2];
        int idx = 0;
        for (final DeckRecognizer.Token t : tokens) {
            if (t.getType() == TokenType.KnownCard) {
                cardsOk[idx] += t.getNumber();
            }
            if (t.getType() == TokenType.UnknownCard) {
                cardsUnknown[idx] += t.getNumber();
            }
            if ((t.getType() == TokenType.SectionName) && t.getText().toLowerCase().contains("side")) {
                idx = 1;
            }
        }
        summaryMain.setText(String.format("Main: %d cards recognized, %d unknown cards", cardsOk[0], cardsUnknown[0]));
        summarySide.setText(String.format("Sideboard: %d cards recognized, %d unknown cards", cardsOk[1], cardsUnknown[1]));
        cmdAccept.setEnabled(cardsOk[0] > 0);
    }

    private static String makeHtmlViewOfToken(final DeckRecognizer.Token token) {
        switch (token.getType()) {
        case KnownCard:
            return String.format("<div class='knowncard'>%s * %s [%s] %s</div>", token.getNumber(), token.getCard()
                    .getName(), token.getCard().getEdition(), token.getCard().isFoil() ? "<i>foil</i>" : "");
        case UnknownCard:
            return String.format("<div class='unknowncard'>%s * %s</div>", token.getNumber(), token.getText());
        case SectionName:
            return String.format("<div class='section'>%s</div>", token.getText());
        case UnknownText:
        case Comment:
            return String.format("<div class='comment'>%s</div>", token.getText());
        default:
            return "";
        }
    }
}
