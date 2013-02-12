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
package forge.gui.deckeditor;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JCheckBox;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;

import net.miginfocom.swing.MigLayout;
import forge.deck.Deck;
import forge.deck.DeckBase;
import forge.deck.DeckRecognizer;
import forge.deck.DeckSection;
import forge.deck.DeckRecognizer.TokenType;
import forge.gui.GuiUtils;
import forge.gui.deckeditor.controllers.ACEditorBase;
import forge.item.CardPrinted;
import forge.item.InventoryItem;

/**
 * 
 * Dialog for quick import of decks.
 *
 * @param <TItem>
 * @param <TModel>
 */
public class DeckImport<TItem extends InventoryItem, TModel extends DeckBase> extends JDialog {
    private static final long serialVersionUID = -5837776824284093004L;

    private final JTextArea txtInput = new JTextArea();
    private static final String STYLESHEET = "<style>"
            + "body, h1, h2, h3, h4, h5, h6, table, tr, td, p {margin: 1px; padding: 0; font-weight: "
            + "normal; font-style: normal; text-decoration: none; font-family: Arial; font-size: 10px;} "
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
            + "</html>";

    private final JEditorPane htmlOutput = new JEditorPane("text/html", DeckImport.HTML_WELCOME_TEXT);
    private final JScrollPane scrollInput = new JScrollPane(this.txtInput);
    private final JScrollPane scrollOutput = new JScrollPane(this.htmlOutput);
    private final JLabel summaryMain = new JLabel("Imported deck summary will appear here");
    private final JLabel summarySide = new JLabel("This is second line");
    private final JButton cmdAccept = new JButton("Import Deck");
    private final JButton cmdCancel = new JButton("Cancel");
    private final JCheckBox newEditionCheck = new JCheckBox("Import latest version of card", true);

    /** The tokens. */
    private final List<DeckRecognizer.Token> tokens = new ArrayList<DeckRecognizer.Token>();

    private final ACEditorBase<TItem, TModel> host;

    /**
     * Instantiates a new deck import.
     * 
     * @param g
     *            the g
     */
    public DeckImport(final ACEditorBase<TItem, TModel> g) {
        this.host = g;

        final int wWidth = 600;
        final int wHeight = 600;

        this.setPreferredSize(new java.awt.Dimension(wWidth, wHeight));
        this.setSize(wWidth, wHeight);
        GuiUtils.centerFrame(this);
        this.setResizable(false);
        this.setTitle("Deck Import (wip)");

        final Font fButtons = new java.awt.Font("Dialog", 0, 13);
        this.cmdAccept.setFont(fButtons);
        this.cmdCancel.setFont(fButtons);

        this.txtInput.setFont(fButtons);
        // htmlOutput.setFont(fButtons);

        this.htmlOutput.setEditable(false);

        this.scrollInput.setBorder(new TitledBorder(BorderFactory.createEtchedBorder(), "Paste or type a decklist"));
        this.scrollOutput.setBorder(new TitledBorder(BorderFactory.createEtchedBorder(),
                "Expect the recognized lines to appear"));
        this.scrollInput.setViewportBorder(BorderFactory.createLoweredBevelBorder());
        this.scrollOutput.setViewportBorder(BorderFactory.createLoweredBevelBorder());

        this.getContentPane().setLayout(new MigLayout("fill"));
        this.getContentPane().add(this.scrollInput, "cell 0 0, w 50%, growy, pushy");
        this.getContentPane().add(this.newEditionCheck, "cell 0 1, w 50%, align c");
        this.getContentPane().add(this.scrollOutput, "cell 1 0, w 50%, growy, pushy");
        this.getContentPane().add(this.summaryMain, "cell 1 1, label");
        this.getContentPane().add(this.summarySide, "cell 1 2, label");

        this.getContentPane().add(this.cmdAccept, "cell 1 3, split 2, w 100, align c");
        this.getContentPane().add(this.cmdCancel, "w 100");


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
                final String warning = "This will replace contents of your currently open deck with whatever you are importing. Proceed?";
                final int answer = JOptionPane.showConfirmDialog(DeckImport.this, warning, "Replacing old deck",
                        JOptionPane.YES_NO_OPTION);
                if (JOptionPane.NO_OPTION == answer) {
                    return;
                }
                final Deck toSet = DeckImport.this.buildDeck();
                DeckImport.this.host.getDeckController().setModel((TModel) toSet);
                DeckImport.this.processWindowEvent(new WindowEvent(DeckImport.this, WindowEvent.WINDOW_CLOSING));
            }
        });

        this.txtInput.getDocument().addDocumentListener(new OnChangeTextUpdate());
        this.cmdAccept.setEnabled(false);
    }

    private void readInput() {
        this.tokens.clear();
        final ElementIterator it = new ElementIterator(this.txtInput.getDocument().getDefaultRootElement());
        Element e;
        while ((e = it.next()) != null) {
            if (!e.isLeaf()) {
                continue;
            }
            final int rangeStart = e.getStartOffset();
            final int rangeEnd = e.getEndOffset();
            try {
                final String line = this.txtInput.getText(rangeStart, rangeEnd - rangeStart);
                this.tokens.add(DeckRecognizer.recognizeLine(line, newEditionCheck.isSelected()));
            } catch (final BadLocationException ex) {
            }
        }
    }

    private void displayTokens() {
        final StringBuilder sbOut = new StringBuilder("<html>");
        sbOut.append(DeckImport.STYLESHEET);
        for (final DeckRecognizer.Token t : this.tokens) {
            sbOut.append(this.makeHtmlViewOfToken(t));
        }
        sbOut.append("</html>");
        this.htmlOutput.setText(sbOut.toString());
    }

    private void updateSummaries() {
        final int[] cardsOk = new int[2];
        final int[] cardsUnknown = new int[2];
        int idx = 0;
        for (final DeckRecognizer.Token t : this.tokens) {
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
        this.summaryMain.setText(String.format("Main: %d cards recognized, %d unknown cards", cardsOk[0],
                cardsUnknown[0]));
        this.summarySide.setText(String.format("Sideboard: %d cards recognized, %d unknown cards", cardsOk[1],
                cardsUnknown[1]));
        this.cmdAccept.setEnabled(cardsOk[0] > 0);
    }

    private Deck buildDeck() {
        final Deck result = new Deck();
        boolean isMain = true;
        for (final DeckRecognizer.Token t : this.tokens) {
            final DeckRecognizer.TokenType type = t.getType();
            if ((type == DeckRecognizer.TokenType.SectionName) && t.getText().toLowerCase().contains("side")) {
                isMain = false;
            }
            if (type != DeckRecognizer.TokenType.KnownCard) {
                continue;
            }
            final CardPrinted crd = t.getCard();
            if (isMain) {
                result.getMain().add(crd, t.getNumber());
            } else {
                result.getOrCreate(DeckSection.Sideboard).add(crd, t.getNumber());
            }
        }
        return result;
    }

    /**
     * The Class OnChangeTextUpdate.
     */
    protected class OnChangeTextUpdate implements DocumentListener {
        private void onChange() {
            DeckImport.this.readInput();
            DeckImport.this.displayTokens();
            DeckImport.this.updateSummaries();
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * javax.swing.event.DocumentListener#insertUpdate(javax.swing.event
         * .DocumentEvent)
         */
        @Override
        public final void insertUpdate(final DocumentEvent e) {
            this.onChange();
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * javax.swing.event.DocumentListener#removeUpdate(javax.swing.event
         * .DocumentEvent)
         */
        @Override
        public final void removeUpdate(final DocumentEvent e) {
            this.onChange();
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * javax.swing.event.DocumentListener#changedUpdate(javax.swing.event
         * .DocumentEvent)
         */
        @Override
        public void changedUpdate(final DocumentEvent e) {
        } // Happend only on ENTER pressed
    }

    private String makeHtmlViewOfToken(final DeckRecognizer.Token token) {
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
            break;
        }
        return "";
    }

}
