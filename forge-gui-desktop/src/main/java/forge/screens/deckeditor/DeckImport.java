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

import forge.StaticData;
import forge.card.CardEdition;
import forge.deck.*;
import forge.deck.DeckRecognizer.TokenType;
import forge.game.GameFormat;
import forge.game.GameType;
import forge.item.InventoryItem;
import forge.model.FModel;
import forge.screens.deckeditor.controllers.ACEditorBase;
import forge.toolbox.FButton;
import forge.toolbox.FCheckBox;
import forge.toolbox.FComboBox;
import forge.toolbox.FHtmlViewer;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FTextArea;
import forge.util.Localizer;
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
            + "body, h1, h2, h3, h4, h5, h6, table, tr, td, p {padding: 0; font-weight: normal; "
            + "text-decoration: none; font-family: Arial; font-size: 10px; color: #000000; background-color: white;} "
            + " h3 {font-size: 12px; margin: 2px 0; padding: 0px 5px; } "
            + " h4 {font-size: 11px; } "
            + " code {font-size: 10px; color: #000000; background-color: white; } "
            + "ul, ol, ul li, ol li {padding: 1px; margin 0; } "
            + "p {padding: 2px 5px; margin: 2px 0; font-weight: 400 !important; color: #000000; text-align: justify } "
            + ".unknowncard {color: #666666;} " + ".knowncard {color: #009900;} " + ".illegalcard {color: #990000;} "
            + ".section {padding: 2px 5px; margin: 2px 0; font-weight: 700; background-color: #DDDDDD; color: #000000 } "
            + ".cardtype {padding: 2px 20px; margin: 3px 0; font-weight: 400; background-color: #FFCC66; color: #000000 } "
            + ".deckname {padding: 2px 20px; margin: 3px 0; font-weight: 400; background-color: #332200; color: #FFFFFF }"
            + "</style>";
    //    TODO: Add localisation support
    private static final String HTML_WELCOME_TEXT = "<html>"
            + "<head>"
            + DeckImport.STYLESHEET
            + "</head>"
            + "<body>"
            + "<h3 id='how-to-use-the-deck-importer'>How to use the Deck Importer</h3>" +
            "<p><strong>Quick Instructions</strong>:\n" +
            "Using the Deck Importer is <strong>very simple</strong>: " +
            "just type or paste the names of the cards you want (one per line in the <em>Card List</em>), " +
            "and the Importer will automatically create the <em>Decklist</em> with M:TG cards in Forge." +
            "You could also specify how many copies of each card you want (default: <code>1</code>), " +
            "and their corresponding Edition.\nIf No Edition is specified, the card print will be " +
            "selected automatically according to the <em>Card Art Preference</em> option in Game Settings.\n\n" +
            "For example: <code>\"4 Power Sink TMP\"</code> will import:" +
            "<ul><li><code>4</code> copies of <code>Power Sink</code> from <code>Tempest</code>.</li></ul>" +
            "Each line in the list will be processed on-the-fly, and rendered in the Decklist with the following " +
            "color-codes:" +
            "<ul>" +
            "<li> <span class=\"knowncard\">Card Recognized: Successful match in the database.</span></li>" +
            "<li> <span class=\"unknowncard\">Card Not Found, or Not Supported yet in Forge.</span></li>" +
            "<li> <span class=\"comment\">General text or Comment: Simply ignored by the Importer.</span></li>" +
            "</ul></p>" +
            "<p><b>Additional Options</b>:" +
            "<ol>" +
            "<li><strong>Deck Name</strong>: you can specify a name for your deck . Just type " +
            "<code>Name: &lt;NAME OF YOUR DECK&gt;</code> in the Card List;</li>" +
            "<li><strong>Card types</strong>: you can organise your list by types, e.g. " +
            "<code>Creature</code>, <code>Instant</code>, <code>Land</code>, <code>Sorcery</code>;</li>" +
            "<li><strong>Deck Section</strong>: Similarly, you can organise your list by Deck Sections, " +
            "e.g. <code>Main</code>, <code>Sideboard</code>;</li>" +
            "<li><strong>Collector Number</strong>: You can identify a specific Card Print by including its CollNr., " +
            "e.g. <code>20 Island M21 265</code></li>" +
            "</ol></p>" +
            "<p><strong>Deck Formats</strong>:" +
            "Card Lists in the following formats are supported : MTG Arena (<code>.MTGA</code>); " +
            "MTG Goldfish (<code>.MTGO</code>); TappedOut; DeckStats.net; <code>.dec</code> files.</p>"
            + "</body>"
            + "</html>";

    private final FHtmlViewer htmlOutput = new FHtmlViewer(DeckImport.HTML_WELCOME_TEXT);
    private final FScrollPane scrollInput = new FScrollPane(this.txtInput, false);
    private final FScrollPane scrollOutput = new FScrollPane(this.htmlOutput, false);
    private final FLabel summaryMain = new FLabel.Builder().text(Localizer.getInstance().getMessage("lblImportedDeckSummay")).build();
    private final FLabel summarySide = new FLabel.Builder().text(Localizer.getInstance().getMessage("lblSideboardSummayLine")).build();
    private final FButton cmdCancel = new FButton(Localizer.getInstance().getMessage("lblCancel"));

    private FButton cmdAccept;  // Not initialised as label will be adaptive.
    private final FCheckBox dateTimeCheck = new FCheckBox(Localizer.getInstance().getMessage("lblUseOnlySetsReleasedBefore"), false);
    private final FCheckBox replaceDeckCheckbox = new FCheckBox(Localizer.getInstance().getMessage("lblReplaceDeck"), false);
    private final DeckFormat deckFormat;

    //don't need wrappers since skin can't change while this dialog is open
    private final FComboBox<String> monthDropdown = new FComboBox<>();
    private final FComboBox<Integer> yearDropdown = new FComboBox<>();

    private final DeckImportController controller;
    private final ACEditorBase<TItem, TModel> host;

    private final String IMPORT_CARDS_CMD_LABEL = Localizer.getInstance().getMessage("lblImportCardsCmd");
    private final String IMPORT_DECK_CMD_LABEL = Localizer.getInstance().getMessage("lblImportDeckCmd");
    private final String REPLACE_CARDS_CMD_LABEL = Localizer.getInstance().getMessage("lblReplaceCardsCmd");


    public DeckImport(final ACEditorBase<TItem, TModel> g) {
        boolean currentDeckIsNotEmpty = !(g.getDeckController().isEmpty());
        DeckFormat currentDeckFormat = g.getGameType().getDeckFormat();
        this.deckFormat = currentDeckFormat;
        GameType currentGameType = g.getGameType();
        // get the game format with the same name of current game type (if any)
        GameFormat currentGameFormat = FModel.getFormats().get(currentGameType.name());
        if (currentGameFormat == null)
            currentGameFormat = FModel.getFormats().get("Vintage");  // default for constructed
        List<String> allowedSetCodes = currentGameFormat.getAllowedSetCodes();
        this.controller = new DeckImportController(dateTimeCheck, monthDropdown, yearDropdown,
                currentDeckIsNotEmpty, allowedSetCodes, currentDeckFormat);
        String cmdAcceptLabel = currentDeckIsNotEmpty ? IMPORT_CARDS_CMD_LABEL : IMPORT_DECK_CMD_LABEL;
        this.cmdAccept = new FButton(cmdAcceptLabel);

        this.host = g;

        final int wWidth = 900;
        final int wHeight = 900;

        this.setPreferredSize(new java.awt.Dimension(wWidth, wHeight));
        this.setSize(wWidth, wHeight);
        String gameTypeName = String.format(" %s", currentGameType.name());
        this.setTitle(Localizer.getInstance().getMessage("lblDeckImporter") + gameTypeName);

        txtInput.setFocusable(true);
        txtInput.setEditable(true);

        final FSkin.SkinColor foreColor = FSkin.getColor(FSkin.Colors.CLR_TEXT);
        this.scrollInput.setBorder(new FSkin.TitledSkinBorder(BorderFactory.createEtchedBorder(), Localizer.getInstance().getMessage("lblPasteTypeDecklist"), foreColor));
        this.scrollOutput.setBorder(new FSkin.TitledSkinBorder(BorderFactory.createEtchedBorder(), Localizer.getInstance().getMessage("lblExpectRecognizedLines"), foreColor));
        this.scrollInput.setViewportBorder(BorderFactory.createLoweredBevelBorder());
        this.scrollOutput.setViewportBorder(BorderFactory.createLoweredBevelBorder());

        this.add(this.scrollInput, "cell 0 0, w 50%, growy, pushy");
        this.add(this.dateTimeCheck, "cell 0 2, w 50%, ax c");

        this.add(monthDropdown, "cell 0 3, w 20%, ax left, split 2, pad 0 4 0 0");
        this.add(yearDropdown, "w 15%");

        this.add(this.scrollOutput, "cell 1 0, w 50%, growy, pushy");
        this.add(this.summaryMain, "cell 1 1, label");
        this.add(this.summarySide, "cell 1 2, label");

        if (currentDeckIsNotEmpty){
            // Disabled Default behaviour to replace current deck: bulk import cards into the existing deck!
            //this.replaceDeckCheckbox.setSelected(currentDeckIsNotEmpty);
            this.add(this.replaceDeckCheckbox, "cell 1 3, align r, ax r");
        }

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
                String currentDeckName = g.getDeckController().getModelName();
                final Deck deck = controller.accept(currentDeckName);
                if (deck == null) { return; }
                // If the soon-to-import card list hasn't got any name specified in the list
                // we set it to the current one (if any) or set a new one.
                // In this way, if this deck will replace the current one, the name will be kept the same!
                if (!deck.hasName()){
                    if (currentDeckName.equals(""))
                        deck.setName("New Deck");  // TODO: try to generate a deck name?
                    else
                        deck.setName(currentDeckName);
                }

                DeckImport.this.host.getDeckController().loadDeck(deck, controller.getReplacingDeck());
                DeckImport.this.processWindowEvent(new WindowEvent(DeckImport.this, WindowEvent.WINDOW_CLOSING));
            }
        });

        final ActionListener updateDateCheck = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
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

        final ActionListener toggleDeckReplace = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { toggleDeckReplaceOption(); }
        };

        this.yearDropdown.addActionListener(reparse);
        this.monthDropdown.addActionListener(reparse);
        updateDateCheck.actionPerformed(null); // update actual state

        this.txtInput.getDocument().addDocumentListener(new OnChangeTextUpdate());
        this.cmdAccept.setEnabled(false);


        if (currentDeckIsNotEmpty){
            this.replaceDeckCheckbox.setSelected(false);
            this.replaceDeckCheckbox.addActionListener(toggleDeckReplace);
        }
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

    private void toggleDeckReplaceOption(){
        boolean replaceDeckStatus = this.replaceDeckCheckbox.isSelected();
        this.controller.setReplacingDeck(replaceDeckStatus);
        String cmdAcceptLabel = replaceDeckStatus ? this.REPLACE_CARDS_CMD_LABEL : this.IMPORT_CARDS_CMD_LABEL;
        this.cmdAccept.setText(cmdAcceptLabel);
    }

    private void displayTokens(final List<DeckRecognizer.Token> tokens) {
        if (tokens.isEmpty() || hasOnlyComment(tokens)) {
            htmlOutput.setText(HTML_WELCOME_TEXT);
        } else {
            final StringBuilder sbOut = new StringBuilder("<html>");
            sbOut.append(DeckImport.STYLESHEET);
            for (final DeckRecognizer.Token t : tokens) {
                sbOut.append(makeHtmlViewOfToken(t));
            }
            sbOut.append("</html>");
            htmlOutput.setText(sbOut.toString());
        }
    }

    private boolean hasOnlyComment(final List<DeckRecognizer.Token> tokens) {
        for (DeckRecognizer.Token token : tokens) {
            if (token.getType() != TokenType.Comment && token.getType() != TokenType.UnknownText)
                return false;
        }
        return true;
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
            if ((t.getType() == TokenType.DeckSectionName) && t.getText().toLowerCase().contains("side")) {
                idx = 1;
            }
        }
        summaryMain.setText(Localizer.getInstance().getMessage("lblDeckImporterSummaryOfMain", String.valueOf(cardsOk[0]), String.valueOf(cardsUnknown[0])));
        summarySide.setText(Localizer.getInstance().getMessage("lblDeckImporterSummaryOfSideboard", String.valueOf(cardsOk[1]), String.valueOf(cardsUnknown[1])));
        cmdAccept.setEnabled(cardsOk[0] > 0);
    }

//    private static String makeHtmlViewOfToken(final DeckRecognizer.Token token) {
//        switch (token.getType()) {
//            case KnownCard:
//                return String.format("<div class='knowncard'>%s * %s [%s] %s</div>", token.getNumber(), token.getCard()
//                        .getName(), token.getCard().getEdition(), token.getCard().isFoil() ? "<i>foil</i>" : "");
//            case UnknownCard:
//                return String.format("<div class='unknowncard'>%s * %s</div>", token.getNumber(), token.getText());
//            case SectionName:
//                return String.format("<div class='section'>%s</div>", token.getText());
//            case UnknownText:
//            case Comment:
//                return String.format("<div class='comment'>%s</div>", token.getText());
//            default:
//                return "";
//        }
//    }

    private String makeHtmlViewOfToken(final DeckRecognizer.Token token) {
        switch (token.getType()) {
            case KnownCard:
                CardEdition edition = StaticData.instance().getEditions().get(token.getCard().getEdition());
                String editionName;
                if (edition == null)
                    editionName = "Unknown Edition";
                else
                    editionName = edition.getName();
                // TODO: String Padding for alignment
                return String.format("<div class='knowncard'>%s x %s from %s (%s) %s</div>",
                        token.getNumber(), token.getCard().getName(),
                        editionName, token.getCard().getEdition(),
                        token.getCard().isFoil() ? "<i>foil</i>" : "");
            case UnknownCard:
                return String.format("<div class='unknowncard'>%s x %s (%s)</div>",
                        token.getNumber(), token.getText(),
                        Localizer.getInstance().getMessage("lblUnknownCard"));
            case IllegalCard:
                return String.format("<div class='illegalcard'>%s x %s (%s %s)</div>",
                        token.getNumber(), token.getText(),
                        Localizer.getInstance().getMessage("lblIllegalCard"),
                        this.deckFormat.name());
            case DeckSectionName:
                return String.format("<div class='section'>%s</div>", token.getText());
            case CardType:
                return String.format("<div class='cardtype'>%s</div>", token.getText());
            case DeckName:
                return String.format("<div class='deckname'>Deck Name: %s</div>", token.getText());
            case UnknownText:
            case Comment:
            default:
                return "";
            // return String.format("<div class='comment'>%s</div>", token.getText());
        }
    }
}
