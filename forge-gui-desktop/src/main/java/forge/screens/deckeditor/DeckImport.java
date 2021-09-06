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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import forge.StaticData;
import forge.card.CardEdition;
import forge.card.CardRarity;
import forge.card.CardType;
import forge.card.ColorSet;
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
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FTextArea;
import forge.util.Localizer;
import forge.view.FDialog;
import org.apache.commons.math3.stat.inference.BinomialTest;

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
            + "body, h1, h2, h3, h4, h5, h6, table, tr, td {font-weight: normal; line-height: 1.4; "
            + "text-decoration: none; font-family: Arial; font-size: 10px; color: #000000; background-color: white;} "
            + " h3 {font-size: 13px; margin: 2px 0; padding: 0px 5px; } "
            + " h4 {font-size: 11px; margin: 2px 0; padding: 0px 5px; font-weight: 600;} "
            + " h5 {font-size: 11px; margin: 2px 0; padding: 0px 5px; font-weight: 600;} "
            + " code {font-size: 10px; color: #000000; background-color: white; } "
            + " ul li {padding: 5px 1px 1px 1px !important; margin: 0 1px !important} "
            + " p {margin: 2px; text-align: justify; padding: 2px 5px;} "
            + " p.example {margin: 0 2px !important; padding: 0 5px !important;} "
            + ".unknowncard {color: #666666;} " + ".knowncard {color: #009900;} "
            + ".illegalcard {color: #990000;} " + ".invalidcard {color: #000099;} "
            + ".comment {font-style: italic} "
            + ".section {padding: 2px 5px; margin: 2px 0; font-weight: 700; background-color: #DDDDDD; color: #000000 } "
            + ".editioncode {font-weight: 700; color: #5a8276 !important;} "
            + ".cardtype {padding: 2px 20px; margin: 3px 0; font-weight: 400; background-color: #FFCC66; color: #000000 } "
            + ".deckname {padding: 2px 20px; margin: 3px 0; font-weight: 400; background-color: #332200; color: #FFFFFF }"
            + "</style>";
    //    TODO: Add localisation support
    private static final String COLOUR_CODED_TAGS = String.format(
            "<ul>" +
            "<li> <span class=\"knowncard\">%s</span></li>" +
            "<li> <span class=\"unknowncard\">%s</span></li>" +
            "<li> <span class=\"illegalcard\">%s</span></li>" +
            "<li> <span class=\"invalidcard\">%s</span></li></ul>",
            Localizer.getInstance().getMessage("lblGuideKnownCard"),
            Localizer.getInstance().getMessage("lblGuideUnknownCard"),
            Localizer.getInstance().getMessage("lblGuideIllegalCard"),
            Localizer.getInstance().getMessage("lblGuideInvalidCard")
            );
    private static final String TIPS_LIST = String.format(
            "<ul><li>%s</li><li>%s</li><li>%s</li><li>%s</li><li>%s</li><li>%s</li></ul>",
            Localizer.getInstance().getMessage("lblGuideTipsCount",
                    String.format("<b>%s</b>", Localizer.getInstance().getMessage("lblGuideTipsTitleCount")),
                    String.format("<code>%s</code>", "\"4 Power Sink\""),
                    String.format("<code>%s</code>", "\"4x Power Sink\"")),
            Localizer.getInstance().getMessage("lblGuideTipsSet",
                    String.format("<b>%s</b>", Localizer.getInstance().getMessage("lblGuideTipsTitleSet"))),
            Localizer.getInstance().getMessage("lblGuideTipsCardType",
                    String.format("<b>%s</b>", Localizer.getInstance().getMessage("lblGuideTipsTitleCardType"))),
            Localizer.getInstance().getMessage("lblGuideTipsDeckSection",
                    String.format("<b>%s</b>", Localizer.getInstance().getMessage("lblGuideTipsTitleDeckSections"))),
            Localizer.getInstance().getMessage("lblGuideTipsDeckName",
                    String.format("<b>%s</b>", Localizer.getInstance().getMessage("lblGuideTipsTitleDeckName"))),
            Localizer.getInstance().getMessage("lblGuideTipsDeckFormats",
                    String.format("<b>%s</b>", Localizer.getInstance().getMessage("lblGuideTipsTitleDeckFormat")))
    );

    private static final String EXAMPLES_LIST = String.format(
            "<ul><li><code>%s</code></li></ul>" +
            "<p class=\"example\">%s</p>" +
            "<ul><li><code>%s</code></li></ul>" +
            "<p class=\"example\">%s</p>" +
            "<ul><li><code>%s</code></li></ul>" +
            "<p class=\"example\">%s</p>" +
            "<ul><li><code>%s</code></li></ul>" +
            "<p class=\"example\">%s</p>",
            Localizer.getInstance().getMessage("lblExample1"),
            Localizer.getInstance().getMessage("nlExample1"),
            Localizer.getInstance().getMessage("lblExample2"),
            Localizer.getInstance().getMessage("nlExample2"),
            Localizer.getInstance().getMessage("lblExample3"),
            Localizer.getInstance().getMessage("nlExample3"),
            Localizer.getInstance().getMessage("lblExample4"),
            Localizer.getInstance().getMessage("nlExample4")
    );

    private static final String HTML_WELCOME_TEXT = String.format("<html>"
            + "<head>"
            + DeckImport.STYLESHEET
            + "</head>"
            + "<body>"
            + "<h3 id='how-to-use-the-deck-importer'>%s</h3>"
            + "<p>%s</p> "
            + "<h4>%s</h4> "
            + "<p>%s</p> "
            + "<h4>%s</h4> "
            + "<p>%s</p> "
            + "</body>"
            + "</html>",
            Localizer.getInstance().getMessage("nlGuideTitle"),
            Localizer.getInstance().getMessage("nlGuideQuickInstructions", COLOUR_CODED_TAGS),
            Localizer.getInstance().getMessage("nlGuideTipsTitle"),
            Localizer.getInstance().getMessage("nlGuideTipsText", TIPS_LIST),
            Localizer.getInstance().getMessage("nlGuideExamplesTitle"),
            Localizer.getInstance().getMessage("nlGuideExamplesText", EXAMPLES_LIST)
            );

    private final FHtmlViewer htmlOutput = new FHtmlViewer(DeckImport.HTML_WELCOME_TEXT);
    private final FHtmlViewer decklistStats = new FHtmlViewer();
    private final FScrollPane scrollInput = new FScrollPane(this.txtInput, false);
    private final FScrollPane scrollOutput = new FScrollPane(this.htmlOutput, false);
    private final FScrollPane scrollStats = new FScrollPane(this.decklistStats, false);

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
        String import_deck_cmd_label = Localizer.getInstance().getMessage("lblImportDeckCmd");
        String cmdAcceptLabel = currentDeckIsNotEmpty ? IMPORT_CARDS_CMD_LABEL : import_deck_cmd_label;
        this.cmdAccept = new FButton(cmdAcceptLabel);

        this.host = g;

        initMainPanel(g, currentDeckIsNotEmpty, currentGameType);
    }

    private void initMainPanel(ACEditorBase<TItem, TModel> g, boolean currentDeckIsNotEmpty, GameType currentGameType) {
        GraphicsDevice gd = this.getGraphicsConfiguration().getDevice();
        final int wWidth = (int)(gd.getDisplayMode().getWidth() * 0.7);
        final int wHeight = (int)(gd.getDisplayMode().getHeight() * 0.8);
        this.setPreferredSize(new Dimension(wWidth, wHeight));
        this.setSize(wWidth, wHeight);

        String gameTypeName = String.format(" %s", currentGameType.name());
        this.setTitle(Localizer.getInstance().getMessage("lblDeckImporter") + gameTypeName);

        txtInput.setFocusable(true);
        txtInput.setEditable(true);

        final FSkin.SkinColor foreColor = FSkin.getColor(FSkin.Colors.CLR_TEXT);
        this.scrollInput.setBorder(new FSkin.TitledSkinBorder(BorderFactory.createEtchedBorder(),
                Localizer.getInstance().getMessage("lblPasteTypeDecklist"), foreColor));
        this.scrollInput.setViewportBorder(BorderFactory.createLoweredBevelBorder());

        this.scrollOutput.setBorder(new FSkin.TitledSkinBorder(BorderFactory.createEtchedBorder(),
                Localizer.getInstance().getMessage("lblExpectRecognizedLines"), foreColor));
        this.scrollOutput.setViewportBorder(BorderFactory.createLoweredBevelBorder());

        this.scrollStats.setBorder(new FSkin.TitledSkinBorder(BorderFactory.createEtchedBorder(),
                Localizer.getInstance().getMessage("lblSummaryStats"), foreColor));
        this.scrollStats.setViewportBorder(BorderFactory.createLoweredBevelBorder());

        this.add(this.scrollInput, "cell 0 0, w 40%, growy, pushy");
        this.add(this.scrollOutput, "cell 1 0, w 55%, growx, growy, pushx, pushy");
        this.add(this.scrollStats, "cell 2 0, w 50%, wrap, growy, pushy, pushx");

        this.add(this.dateTimeCheck, "cell 0 1, w 50%, ax c");
        this.add(monthDropdown, "cell 0 2, w 20%, ax left, split 2, pad 0 4 0 0");
        this.add(yearDropdown, "cell 0 2, w 20%, ax left, split 2, pad 0 4 0 0");

        if (currentDeckIsNotEmpty)
            this.add(this.replaceDeckCheckbox, "cell 2 1, w 50%, ax c");
        this.add(this.cmdAccept, "cell 2 2, w 150, align r, h 26");
        this.add(this.cmdCancel, "cell 2 2, w 150, align r, h 26");

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
                        deck.setName(Localizer.getInstance().getMessage("lblNewDeckName"));  // TODO: try to generate a deck name?
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
            sbOut.append(String.format("<h3>%s</h3>", Localizer.getInstance().getMessage("lblCurrentDecklist")));
            for (final DeckRecognizer.Token t : tokens) {
                sbOut.append(makeHtmlViewOfToken(t));
            }
            sbOut.append("</html>");
            htmlOutput.setText(sbOut.toString());
        }
    }

    private boolean hasOnlyComment(final List<DeckRecognizer.Token> tokens) {
        for (DeckRecognizer.Token token : tokens) {
            if (token.getType() != TokenType.COMMENT && token.getType() != TokenType.UNKNOWN_TEXT)
                return false;
        }
        return true;
    }

    private void updateSummaries(final List<DeckRecognizer.Token> tokens) {

        String head = "<style>"
                + "body, h1, h2, h3, h4, h5, h6, table, tr, td, p {padding: 0; font-weight: normal; "
                + "text-decoration: none; font-family: Arial; font-size: 10px; "
                + "color: white; background-color: #ffffff; color: #000000;} "
                + " h3 {font-size: 13px; margin: 2px 0; padding: 0px 5px; } "
                + " h4 {font-size: 10px; padding: 1px 20px; margin: 3px 0 0 0;} "
                + " div {margin: 0; ext-align: justify; padding 1px 10px;}"
                + " p {margin: 2px; text-align: justify; padding: 1px 5px;} "
                + ".unknowncard {color: #666666;} " + ".knowncard {color: #009900;} "
                + ".illegalcard {color: #990000;} " + ".invalidcard {color: #000099;} "
                + ".section {font-weight: 700; background-color: #DDDDDD; color: #000000 } "
                + ".mana {font-weight: 700; background-color: #c7bcba; color: #ffffff } "
                + ".rarity {font-weight: 700; background-color: #df8030; color: #ffffff } "
                + ".edition {font-weight: 700; background-color: #5a8276; color: #ffffff } "
                + ".editioncode {font-weight: 700; color: #5a8276; font-style: normal !important;} "
                + ".cardtype {font-weight: 700; background-color: #FFCC66; color: #000000} "
                + ".decksection {padding-left: 20px; font-weight: 700;}"
                + " ul li {padding: 5px 1px 1px 1px !important; margin: 0 1px !important} "
                + "</style>";

        int unknownCardsCount = 0;
        int illegalCardsCount = 0;
        int legalCardsCount = 0;
        int invalidCardsCount = 0;

        String summaryMsgTemplate = "<html><head>%s</head><body><h3>%s</h3>%s</body></html>";
        String deckListName = Localizer.getInstance().getMessage("lblDeckListDefaultName");

        if (hasOnlyComment(tokens)) {
            String statsSummaryList = String.format(
                    "<ul><li>%s</li><li>%s</li><li>%s</li><li>%s</li><li>%s</li><li>%s</li></ul>",
                    Localizer.getInstance().getMessage("lblStatsSummaryCount"),
                    Localizer.getInstance().getMessage("lblStatsSummarySection"),
                    Localizer.getInstance().getMessage("lblStatsSummaryCardType"),
                    Localizer.getInstance().getMessage("lblStatsSummaryCardSet"),
                    Localizer.getInstance().getMessage("lblStatsSummaryRarity"),
                    Localizer.getInstance().getMessage("lblStatsSummaryCMC")
            );
            this.decklistStats.setText(String.format(summaryMsgTemplate, head,
                    Localizer.getInstance().getMessage("lblSummaryHeadMsg", deckListName),
                    String.format("<p>%s</p>",
                            Localizer.getInstance().getMessage("lblImportedDeckSummary", statsSummaryList))));
        }
        else {
            Map<String, Integer> deckSectionsStats = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            deckSectionsStats.put(DeckSection.Main.name(), 0);
            String currentKeySection = DeckSection.Main.name();

            DeckStats cardTypeStats = new CardTypeDeckStats(Localizer.getInstance().getMessage("lblSummaryCardTypeStats"), "cardtype");
            DeckStats editionsStats = new EditionDeckStats(Localizer.getInstance().getMessage("lblSummaryEditionStats"), "edition");
            DeckStats manaStats = new DeckStats(Localizer.getInstance().getMessage("lblSummaryManaStats"), "mana");
            DeckStats rarityStats = new RarityDeckStats(Localizer.getInstance().getMessage("lblSummaryRarityStats"), "rarity");

            for (final DeckRecognizer.Token t : tokens) {
                if (t.getType() == TokenType.UNKNOWN_CARD_REQUEST)
                    unknownCardsCount += t.getNumber();
                else if (t.getType() == TokenType.ILLEGAL_CARD_REQUEST)
                    illegalCardsCount += t.getNumber();
                else if (t.getType() == TokenType.INVALID_CARD_REQUEST)
                    invalidCardsCount += t.getNumber();
                else if (t.getType() == TokenType.DECK_SECTION_NAME) {
                    if (!t.getText().equalsIgnoreCase(currentKeySection))
                        currentKeySection = t.getText();
                }
                else if (t.getType() == TokenType.DECK_NAME)
                    deckListName = String.format("\"%s\"", t.getText());
                else if (t.getType() == TokenType.LEGAL_CARD_REQUEST) {
                    int tokenNumber = t.getNumber();
                    legalCardsCount += tokenNumber;

                    // update deck section stats
                    int sectionCount = deckSectionsStats.getOrDefault(currentKeySection, 0);
                    sectionCount += tokenNumber;
                    deckSectionsStats.put(currentKeySection, sectionCount);

                    // update card edition stats
                    String setCode = t.getCard().getEdition();
                    editionsStats.add(currentKeySection, setCode, tokenNumber);
                    // update card type stats
                    for (CardType.CoreType coreType : t.getCard().getRules().getType().getCoreTypes()) {
                        String coreTypeName = coreType.name();
                        cardTypeStats.add(currentKeySection, coreTypeName, tokenNumber);
                    }
                    // update rarity stats
                    if (!t.getCard().isVeryBasicLand())
                        rarityStats.add(currentKeySection, t.getCard().getRarity().name(), tokenNumber);
                    // update colour stats
                    if (!t.getCard().getRules().getType().isLand()) {
                        String manaCost = String.format("CMC %d", t.getCard().getRules().getManaCost().getCMC());
                        manaStats.add(currentKeySection, manaCost, tokenNumber);
                    }
                }
            }

            String cardStatsReport = createCardsStatsReport(unknownCardsCount, illegalCardsCount,
                    invalidCardsCount,
                    legalCardsCount, deckSectionsStats);

            String deckListSummaryHtml = String.format("<h4 class=\"section\">%s</h4>%s %s %s %s %s",
                    Localizer.getInstance().getMessage("lblSummaryDeckStats"), cardStatsReport,
                    editionsStats.toHTML(), cardTypeStats.toHTML(), rarityStats.toHTML(), manaStats.toHTML());

            this.decklistStats.setText(String.format(summaryMsgTemplate, head,
                    Localizer.getInstance().getMessage("lblSummaryHeadMsg", deckListName),
                    deckListSummaryHtml));
        }
        cmdAccept.setEnabled(legalCardsCount > 0);
    }

    private String createCardsStatsReport(int unknownCardsCount, int illegalCardsCount, int invalidCardsCount,
                                          int legalCardsCount, Map<String, Integer> deckSectionsStats) {
        StringBuilder sb = new StringBuilder();
        if (legalCardsCount > 0){
            sb.append(String.format("<div class=\"knowncard\">%s: %d</div>",
                    Localizer.getInstance().getMessage("lblLegalCardsCount"), legalCardsCount));
        }
        if (illegalCardsCount > 0){
            sb.append(String.format("<div class=\"illegalcard\">%s: %d</div>",
                    Localizer.getInstance().getMessage("lblIllegalCardsCount"), illegalCardsCount));
        }
        if (invalidCardsCount > 0){
            sb.append(String.format("<div class=\"invalidcard\">%s: %d</div>",
                    Localizer.getInstance().getMessage("lblInvalidCardsCount"), invalidCardsCount));
        }
        if (unknownCardsCount > 0){
            sb.append(String.format("<div class=\"unknowncard\">%s: %d</div>",
                    Localizer.getInstance().getMessage("lblUnknownCardsCount"), unknownCardsCount));
        }
        String cardStatsReport = sb.toString();
        cardStatsReport += createDeckStatsReport(deckSectionsStats);
        return cardStatsReport;
    }

    private String createDeckStatsReport(Map<String, Integer> deckSectionsStats) {
        StringBuilder deckSectionsReport = new StringBuilder("<br />");
        for (String sectionName : deckSectionsStats.keySet()){
            String sectionNameLabel = Localizer.getInstance().getMessage(String.format("lbl%s", sectionName));
            String tag = String.format("<div>%s : %d</div>",
                    Localizer.getInstance().getMessage("lblDeckSectionStats",
                            String.format("<b>%s</b>", sectionNameLabel)),
                            deckSectionsStats.get(sectionName));
            deckSectionsReport.append(tag);
        }
        return deckSectionsReport.toString();
    }

    private String makeHtmlViewOfToken(final DeckRecognizer.Token token) {
        if (token == null)
            return "";

        switch (token.getType()) {
            case LEGAL_CARD_REQUEST:
                // TODO: String Padding for alignment
                return String.format("<div class=\"knowncard\">%s x %s " +
                                "<span class=\"editioncode\">(%s)</span> %s %s</div>",
                        token.getNumber(), token.getCard().getName(),
                        token.getCard().getEdition(),
                        token.getCard().getCollectorNumber(),
                        token.getCard().isFoil() ? "<i>(FOIL)</i>" : "");
            case UNKNOWN_CARD_REQUEST:
                return String.format("<div class=\"unknowncard\">%s x %s (%s)</div>",
                        token.getNumber(), token.getText(),
                        Localizer.getInstance().getMessage("lblUnknownCardMsg"));
            case ILLEGAL_CARD_REQUEST:
                return String.format("<div class=\"illegalcard\">%s x %s (%s %s)</div>",
                        token.getNumber(), token.getText(),
                        Localizer.getInstance().getMessage("lblIllegalCardMsg"),
                        this.deckFormat.name());
            case INVALID_CARD_REQUEST:
                return String.format("<div class=\"invalidcard\">%s x %s (%s)</div>",
                        token.getNumber(), token.getText(),
                        Localizer.getInstance().getMessage("lblInvalidCardMsg"));
            case DECK_SECTION_NAME:
                return String.format("<div class=\"section\">%s</div>", token.getText());
            case CARD_TYPE:
                return String.format("<div class=\"cardtype\">%s</div>", token.getText());
            case DECK_NAME:
                return String.format("<div class=\"deckname\">%s: %s</div>",
                        Localizer.getInstance().getMessage("lblDeckName"),
                        token.getText());
            case UNKNOWN_TEXT:
            case COMMENT:
            default:
                return "";
//             return String.format("<div class=\"comment\">%s</div>", token.getText());
        }
    }
}

class DeckStats {

    protected Map<String, Map<String, Integer>> deckSectionMap;
    protected String label;
    protected String cssClass;

    public DeckStats(final String name, final String cssClass0){
        this.deckSectionMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.label = name;
        this.cssClass = cssClass0;
    }

    public void add(String sectionName, String key, int amount){
        Map<String, Integer> sectionMap = getOrInitStatMap(sectionName);
        int currentCount = sectionMap.getOrDefault(key, 0);
        currentCount += amount;
        sectionMap.put(key, currentCount);
        deckSectionMap.put(sectionName, sectionMap);
    }

    protected Map<String, Integer> getOrInitStatMap(String sectionName){
        return this.deckSectionMap.getOrDefault(sectionName, new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
    }

    public String toHTML(){
        StringBuilder report = new StringBuilder("");
        report.append(String.format("<h4 class=\"%s\">%s</h4>", this.cssClass, this.label));
        for (String deckSection: this.deckSectionMap.keySet()) {
            Map<String, Integer> sectionStat = this.deckSectionMap.get(deckSection);
            report.append(String.format("<div class=\"decksection\">%s</div>", deckSection));
            int itemCounter = 0;
            for (String propertyLabel : sectionStat.keySet()) {
                int count = sectionStat.getOrDefault(propertyLabel, 0);
                if (count == 0)
                    continue;
                String tag = this.propertyTag(propertyLabel, count);
                report.append(tag);
                itemCounter += 1;
                if (itemCounter == 3) {
                    report.append("<br />");
                    itemCounter = 0;  // reset
                }
            }
        }
        return report.toString();
    }

    protected String propertyTag(String propertyLabel, int count){
        return String.format("<span>%s : %d<;&nbsp;&nbsp;</span>", propertyLabel, count);
    }
}

class EditionDeckStats extends DeckStats {

    public EditionDeckStats(final String name, final String cssClass0) {
        super(name, cssClass0);
    }

    @Override
    public String toHTML(){
        StringBuilder report = new StringBuilder("");
        report.append(String.format("<h4 class=\"%s\">%s</h4>", this.cssClass, this.label));
        for (String deckSection: this.deckSectionMap.keySet()) {
            Map<String, Integer> sectionStat = this.deckSectionMap.get(deckSection);
            report.append(String.format("<div class=\"decksection\">%s</div>", deckSection));
            for (String propertyLabel : sectionStat.keySet()) {
                int count = sectionStat.getOrDefault(propertyLabel, 0);
                if (count == 0)
                    continue;
                String tag = this.propertyTag(propertyLabel, count);
                report.append(tag);
            }
        }
        return report.toString();
    }

    @Override
    protected String propertyTag(String setCode, int count){
        CardEdition edition = StaticData.instance().getCardEdition(setCode);
        if (edition == null)
            edition = CardEdition.UNKNOWN;
        return String.format("<div><em class=\"editioncode\">[%s]</em> %s : %d</div>",
                setCode, edition.getName(), count);
    }
}

class CardTypeDeckStats extends DeckStats {
    public CardTypeDeckStats(final String name, final String cssClass0) {
        super(name, cssClass0);
    }

    @Override
    protected String propertyTag(String typeLabel, int count){
        String typeLocalLab = Localizer.getInstance().getMessage(String.format("lbl%s", typeLabel));
        return String.format("<span>%s : %d;&nbsp;&nbsp;</span>", typeLocalLab, count);
    }
}

class RarityDeckStats extends DeckStats {

    public RarityDeckStats(final String name, final String cssClass0) {
        super(name, cssClass0);
    }

    @Override
    protected Map<String, Integer> getOrInitStatMap(String sectionName){
        Map<String, Integer> statMap = this.deckSectionMap.getOrDefault(sectionName, null);
        if (statMap == null){
            Map<String, Integer> defaultRarityMap = new LinkedHashMap<>();  // I want card rarity to be shown in this exact order!
            defaultRarityMap.put(CardRarity.Common.name(), 0);
            defaultRarityMap.put(CardRarity.Uncommon.name(), 0);
            defaultRarityMap.put(CardRarity.Rare.name(), 0);
            defaultRarityMap.put(CardRarity.MythicRare.name(), 0);
            defaultRarityMap.put(CardRarity.Special.name(), 0);
            return defaultRarityMap;
        }
        return statMap;
    }

}
