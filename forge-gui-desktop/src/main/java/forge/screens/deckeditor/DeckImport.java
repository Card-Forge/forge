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
import java.awt.event.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import forge.ImageCache;
import forge.Singletons;
import forge.StaticData;
import forge.card.CardEdition;
import forge.deck.*;
import forge.deck.DeckRecognizer.TokenType;
import forge.deck.DeckRecognizer.Token.TokenKey;
import forge.game.GameFormat;
import forge.game.GameType;
import forge.gui.CardPicturePanel;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.screens.deckeditor.controllers.ACEditorBase;
import forge.screens.deckeditor.controllers.CStatisticsImporter;
import forge.screens.deckeditor.views.VStatisticsImporter;
import forge.toolbox.FComboBox;
import forge.toolbox.*;
import forge.util.Localizer;
import forge.view.FDialog;
import net.miginfocom.swing.MigLayout;

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
    public static final String KNOWNCARD_COLOR = "#89DC9F;";
    public static final String UNKNOWN_CARD_COLOR = "#E1E35F;";
    public static final String ILLEGAL_CARD_COLOR = "#FF977A;";
    public static final String INVALID_CARD_COLOR = "#A9E5DD;";
    private static final String STYLESHEET = String.format("<style>"
            + "body, h1, h2, h3, h4, h5, h6, table, tr, td, a {font-weight: normal; line-height: 1.6; "
            + " font-family: Arial; font-size: 10px;}"
            + " h3 {font-size: 13px; margin: 2px 0; padding: 0px 5px;}"
            + " h4 {font-size: 11px; margin: 2px 0; padding: 0px 5px; font-weight: bold;}"
            + " h5 {font-size: 11px; margin: 0; text-align: justify; padding: 1px 0 1px 8px;}"
            + " ul li {padding: 5px 1px 1px 1px !important; margin: 0 1px !important}"
            + " code {font-size: 10px;}"
            + " p {margin: 2px; text-align: justify; padding: 2px 5px;}"
            + " div {margin: 0; text-align: justify; padding: 1px 0 1px 8px;}"
            + " a:hover { text-decoration: none !important;}"
            + " a:link { text-decoration: none !important;}"
            + " a { text-decoration: none !important;}"
            + " a:active { text-decoration: none !important;}"
            + " table {margin: 5px 0;}"
            // Card Matching Colours #4F6070
            + " .knowncard   {color: %s !important; font-weight: bold;}"
            + " .unknowncard {color: %s !important; font-weight: bold;}"
            + " .illegalcard {color: %s !important; font-weight: bold;}"
            + " .invalidcard {color: %s !important; font-weight: bold;}"
            + " .comment     {font-style: italic}"
            // Deck Name
            + " .deckname    {background-color: #332200; color: #ffffff; }"
            + " .sectionname {padding-left: 8px; font-weight: bold; }"
            // Placeholders
            + " .section     {font-weight: bold; background-color: #DDDDDD; color: #000000;}"
            + " .cardtype    {font-weight: bold; background-color: #FFCC66; color: #000000;}"
            + " .cmc         {font-weight: bold; background-color: #C6C7BA; color: #000000;}"
            + " .rarity      {font-weight: bold; background-color: #df8030; color: #ffffff;}"
            + " .mana        {font-weight: bold; background-color: #38221A; color: #ffffff;}"
            // Colours
            + " .colorless   {font-weight: bold; background-color: #544132; color: #ffffff;}"
            + " .blue        {font-weight: bold; background-color: #0D78BF; color: #ffffff;}"
            + " .red         {font-weight: bold; background-color: #ED0713; color: #ffffff;}"
            + " .white       {font-weight: bold; background-color: #FCFCB6; color: #000000;}"
            + " .black       {font-weight: bold; background-color: #787878; color: #000000;}"
            + " .green       {font-weight: bold; background-color: #26AB57; color: #000000;}"
            + " .multicolor  {font-weight: bold; background-color: #c4c26c; color: #000000;}"
            // Card Edition
            + " .editioncode {font-weight: bold; color: #ffffff;}"
            + "</style>",KNOWNCARD_COLOR, UNKNOWN_CARD_COLOR, ILLEGAL_CARD_COLOR, INVALID_CARD_COLOR) ;
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
            + "<h3 id='how-to-use-the-deck-importer'>%s</h3><div>%s</div> "
            + "<h4>%s</h4><div>%s</div> "
            + "<h4>%s</h4><div>%s</div> "
            + "</body></html>",
            Localizer.getInstance().getMessage("nlGuideTitle"),
            Localizer.getInstance().getMessage("nlGuideQuickInstructions", COLOUR_CODED_TAGS),
            Localizer.getInstance().getMessage("nlGuideTipsTitle"),
            Localizer.getInstance().getMessage("nlGuideTipsText", TIPS_LIST),
            Localizer.getInstance().getMessage("nlGuideExamplesTitle"),
            Localizer.getInstance().getMessage("nlGuideExamplesText", EXAMPLES_LIST)
    );

    private final FHtmlViewer htmlOutput = new FHtmlViewer(DeckImport.HTML_WELCOME_TEXT);
    private final FScrollPane scrollInput = new FScrollPane(this.txtInput, false);
    private final FScrollPane scrollOutput = new FScrollPane(this.htmlOutput, false);
    private final CardPicturePanel cardImagePreview = new CardPicturePanel();
    private final FLabel cardPreviewLabel = new FLabel.Builder()
            .text(Localizer.getInstance().getMessage("lblCardPreview"))
            .fontSize(14).tooltip("").build();
    private final FButton cmdCancelButton = new FButton(Localizer.getInstance().getMessage("lblCancel"));

    private final FButton cmdAcceptButton;  // Not initialised as label will be adaptive.
    private final FCheckBox createNewDeckCheckbox = new FCheckBox(Localizer.getInstance().getMessage("lblNewDeckCheckbox"), false);
    private final DeckFormat deckFormat;

    // Release Date
    private final FCheckBox dateTimeCheck = new FCheckBox(Localizer.getInstance().getMessage("lblUseOnlySetsReleasedBefore"), false);
    private final FComboBox<String> monthDropdown = new FComboBox<>();
    private final FComboBox<Integer> yearDropdown = new FComboBox<>();

    // Card Art Preferences
    private final FLabel cardArtPrefsLabel = new FLabel.Builder()
            .text(Localizer.getInstance().getMessage("lblPreferredArt"))
            .fontSize(14).build();
    private FComboBox<String> cardArtPrefsComboBox;
    private final FCheckBox cardArtPrefFilter = new FCheckBox(Localizer.getInstance().getMessage("lblPrefArtExpansionOnly"), false);

    // Block Format Filter
    private final FCheckBox blockCheck = new FCheckBox(Localizer.getInstance().getMessage("lblUseBlockFilters"), false);
    private final FComboBox<GameFormat> blocksDropdown = new FComboBox<>();

    private final DeckImportController controller;
    private final ACEditorBase<TItem, TModel> host;

    private final String IMPORT_CARDS_CMD_LABEL = Localizer.getInstance().getMessage("lblImportCardsCmd");
    private final String CREATE_NEW_DECK_CMD_LABEL = Localizer.getInstance().getMessage("lblCreateNewCmd");

    public DeckImport(final ACEditorBase<TItem, TModel> g) {
        boolean currentDeckIsNotEmpty = !(g.getDeckController().isEmpty());
        DeckFormat currentDeckFormat = g.getGameType().getDeckFormat();
        this.deckFormat = currentDeckFormat;
        GameType currentGameType = g.getGameType();
        // get the game format with the same name of current game type (if any)
        GameFormat currentGameFormat = FModel.getFormats().get(currentGameType.name());
        GameFormat vintageGameFormat = FModel.getFormats().get("Vintage");  // default for constructed
        if (currentGameFormat == null)
            currentGameFormat = vintageGameFormat;
        List<String> allowedSetCodes = currentGameFormat.getAllowedSetCodes();
        if (allowedSetCodes == null || allowedSetCodes.isEmpty())
            allowedSetCodes = vintageGameFormat.getAllowedSetCodes();
        this.controller = new DeckImportController(dateTimeCheck, monthDropdown, yearDropdown,
                currentDeckIsNotEmpty, allowedSetCodes, currentDeckFormat, blockCheck, blocksDropdown);
        this.cmdAcceptButton = new FButton(IMPORT_CARDS_CMD_LABEL);
        this.host = g;
        initMainPanel(g, currentDeckIsNotEmpty, currentGameType);
    }

    private void initMainPanel(ACEditorBase<TItem, TModel> g, boolean currentDeckIsNotEmpty, GameType currentGameType) {
//        GraphicsDevice gd = this.getGraphicsConfiguration().getDevice();
//        final int wWidth = (int)(gd.getDisplayMode().getWidth() * 0.85);
//        final int wHeight = (int)(gd.getDisplayMode().getHeight() * 0.8);
        final int wWidth = (int)(Singletons.getView().getFrame().getSize().width * 0.95);
        final int wHeight = (int)(Singletons.getView().getFrame().getSize().height * 0.9);
        this.setPreferredSize(new Dimension(wWidth, wHeight));
        this.setSize(wWidth, wHeight);

        // Set Title
        String gameTypeName = String.format(" %s", currentGameType.name());
        this.setTitle(Localizer.getInstance().getMessage("lblDeckImporterPanelTitle") + gameTypeName);

        txtInput.setFocusable(true);
        txtInput.setEditable(true);

        final FSkin.SkinColor foreColor = FSkin.getColor(FSkin.Colors.CLR_TEXT);

        // === INIT COMPONENTS

        // == Scroll Input (Card List)
        this.scrollInput.setBorder(new FSkin.TitledSkinBorder(BorderFactory.createEtchedBorder(),
                Localizer.getInstance().getMessage("lblCardListTitle"), foreColor));
        this.scrollInput.setViewportBorder(BorderFactory.createLoweredBevelBorder());
        // == Scroll Output (Decklist)
        this.scrollOutput.setBorder(new FSkin.TitledSkinBorder(BorderFactory.createEtchedBorder(),
                Localizer.getInstance().getMessage("lblDecklistTitle"), foreColor));
        this.scrollOutput.setViewportBorder(BorderFactory.createLoweredBevelBorder());
        // == Stats Panel
        FPanel statsPanel = new FPanel(new BorderLayout());
        statsPanel.add(VStatisticsImporter.instance().getMainPanel(), BorderLayout.CENTER);
        statsPanel.setOpaque(false);
        // == Card Preview
        this.cardImagePreview.setOpaque(false);
        this.cardImagePreview.setBorder(new EmptyBorder(2, 5, 2, 5));
        this.cardImagePreview.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT).getColor());
        resetCardImagePreviewPanel();
        FPanel cardPreview = new FPanel(new MigLayout("fill"));
        cardPreview.add(this.cardPreviewLabel, "cell 0 0, align left, w 100%");
        cardPreview.add(this.cardImagePreview, "cell 0 1, w 70%, h 60%, growy, pushy, ax c");
        // == Options Panel
        JPanel optionsPanel = new JPanel(new MigLayout("insets 10, gap 5, center, h 120!"));
        final TitledBorder border = new TitledBorder(BorderFactory.createEtchedBorder(), "Options");
        border.setTitleColor(foreColor.getColor());
        optionsPanel.setBorder(border);
        optionsPanel.setVisible(false);
        optionsPanel.setOpaque(false);

        FButton showOptionsButton = new FButton(Localizer.getInstance().getMessage("lblShowOptions"));
        showOptionsButton.setFont(FSkin.getBoldFont(12));
        FButton hideOptionsButton = new FButton(Localizer.getInstance().getMessage("lblHideOptions"));
        hideOptionsButton.setFont(FSkin.getBoldFont(12));

        String optPanelConstrains = "w 130!, h 80!, left, insets 0";

        // = (opt) Date filter
        JPanel dateFilterPanel = new JPanel(new MigLayout(optPanelConstrains));
        dateFilterPanel.setOpaque(false);
        dateFilterPanel.add(this.dateTimeCheck, "cell 0 0, w 40%, ax left");
        dateFilterPanel.add(this.monthDropdown, "cell 0 1, w 10%, ax left, split 2, pad 0 2 0 0");
        dateFilterPanel.add(this.yearDropdown,  "cell 0 1, w 8%, ax left, split 2");

        // (opt) Card Art Preference Filter

        final String latestOpt = Localizer.getInstance().getMessage("latestArtOpt");
        final String originalOpt = Localizer.getInstance().getMessage("originalArtOpt");
        final String [] choices = {latestOpt, originalOpt};
        this.cardArtPrefsComboBox = new FComboBox<>(choices);
        final String selectedItem = StaticData.instance().cardArtPreferenceIsLatest() ? latestOpt : originalOpt;
        this.cardArtPrefsComboBox.setSelectedItem(selectedItem);
        this.cardArtPrefFilter.setSelected(StaticData.instance().isCoreExpansionOnlyFilterSet());

        JPanel cardArtPanel = new JPanel(new MigLayout(optPanelConstrains));
        cardArtPanel.setOpaque(false);
        cardArtPanel.add(this.cardArtPrefsLabel,    "cell 0 0, w 25%, left, split 2");
        cardArtPanel.add(this.cardArtPrefsComboBox, "cell 0 0, w 10%, left, split 2");
        cardArtPanel.add(this.cardArtPrefFilter,    "cell 0 1, w 15%, left");

        // (opt) Block Filter
        this.blocksDropdown.setEnabled(false);  // not enabled by default
        JPanel blockFilterPanel = new JPanel(new MigLayout(optPanelConstrains));
        blockFilterPanel.setOpaque(false);
        blockFilterPanel.add(this.blockCheck,     "cell 0 0, w 40%, ax left");
        blockFilterPanel.add(this.blocksDropdown, "cell 0 1, w 15%, ax right");

        optionsPanel.add(hideOptionsButton, "w 150!, h 26!, cell 0 0, span 4, growx, left");
        optionsPanel.add(dateFilterPanel,      "cell 0 2, w 100%, left");

        if (this.controller.isBlockFormatsSupported()) {
            optionsPanel.add(cardArtPanel,     "cell 1 2, w 100%, left");
            optionsPanel.add(blockFilterPanel, "cell 2 2, w 100%, left");
        } else {
            optionsPanel.add(cardArtPanel, "cell 1 2, w 100%, left");
            JPanel placeHolderPanel = new JPanel(new MigLayout(optPanelConstrains));
            placeHolderPanel.setOpaque(false);
            optionsPanel.add(placeHolderPanel, "cell 2 2, w 100%, left");
        }

        // == Command buttons
        JPanel cmdPanel = new JPanel(new MigLayout("insets 10, gap 5, right, h 40!"));
        cmdPanel.setOpaque(false);
        if (currentDeckIsNotEmpty) {
            cmdPanel.add(this.createNewDeckCheckbox, "align l, split 3");
            cmdPanel.add(this.cmdAcceptButton, "w 150!, align r, h 26!, split 3");
            cmdPanel.add(this.cmdCancelButton, "w 150!, align r, h 26!, split 3");
        } else {
            cmdPanel.add(this.cmdAcceptButton, "w 150!, align r, h 26!, split 2");
            cmdPanel.add(this.cmdCancelButton, "w 150!, align r, h 26!, split 2");
        }

        // === Assembling main UI component
        this.add(this.scrollInput, "cell 0 0, w 40%, growy, pushy, spany 2");
        this.add(this.scrollOutput, "cell 1 0, w 60%, growy, pushy, spany 2");
        this.add(statsPanel, "cell 2 0, w 480:510:550, growy, pushy, ax c");
        this.add(cardPreview, "cell 2 1, w 480:510:550, h 65%, growy, pushy, ax c");
        this.add(showOptionsButton, "cell 0 2, w 150!, h 26!, left, spanx 3, hidemode 3");
        this.add(optionsPanel, "cell 0 2, center, w 100%, spanx 3, hidemode 3");
        this.add(cmdPanel, "cell 0 3, w 100%, spanx 3");


        // === ACTION LISTENERS
        showOptionsButton.addActionListener(actionEvent -> {
            optionsPanel.setVisible(true);
            showOptionsButton.setVisible(false);
        });
        hideOptionsButton.addActionListener(actionEvent -> {
            optionsPanel.setVisible(false);
            showOptionsButton.setVisible(true);
        });

        this.cmdCancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                DeckImport.this.processWindowEvent(new WindowEvent(DeckImport.this, WindowEvent.WINDOW_CLOSING));
            }
        });

        this.cmdAcceptButton.addActionListener(new ActionListener() {
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
                        deck.setName(Localizer.getInstance().getMessage("lblNewDeckName"));
                    else
                        deck.setName(currentDeckName);
                }

                DeckImport.this.host.getDeckController().loadDeck(deck, controller.getCreateNewDeck());
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

        this.cardArtPrefsComboBox.addItemListener(new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                String artPreference = cardArtPrefsComboBox.getSelectedItem();
                if (artPreference == null)
                    artPreference = latestOpt;  // default, just in case
                final boolean latestArt = artPreference.equalsIgnoreCase(latestOpt);
                final boolean coreExpFilter = StaticData.instance().isCoreExpansionOnlyFilterSet();
                controller.setCardArtPreference(latestArt, coreExpFilter);
                parseAndDisplay();
            }
        });

        cardArtPrefFilter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String artPreference = cardArtPrefsComboBox.getSelectedItem();
                if (artPreference == null)
                    artPreference = latestOpt;  // default, just in case
                final boolean latestArt = artPreference.equalsIgnoreCase(latestOpt);
                final boolean coreExpFilter = cardArtPrefFilter.isSelected();
                controller.setCardArtPreference(latestArt, coreExpFilter);
                parseAndDisplay();
            }
        });

        final ActionListener updateCardBlockCheck = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final boolean isSel = blockCheck.isSelected();
                blocksDropdown.setEnabled(isSel);
                parseAndDisplay();
            }
        };
        this.blockCheck.addActionListener(updateCardBlockCheck);

        final ActionListener reparse = new ActionListener() {
            @Override public void actionPerformed(final ActionEvent e) {
                parseAndDisplay();
            }
        };

        final ActionListener toggleNewDeck = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { toggleNewDeck(); }
        };

        this.yearDropdown.addActionListener(reparse);
        this.monthDropdown.addActionListener(reparse);
        this.blocksDropdown.addActionListener(reparse);
        updateDateCheck.actionPerformed(null); // update actual state

        this.txtInput.getDocument().addDocumentListener(new OnChangeTextUpdate());
        this.cmdAcceptButton.setEnabled(false);

        if (currentDeckIsNotEmpty){
            this.createNewDeckCheckbox.setSelected(false);
            this.createNewDeckCheckbox.addActionListener(toggleNewDeck);
        }

        this.htmlOutput.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                activateCardPreview(e);
            }
        });
    }

    private void activateCardPreview(HyperlinkEvent e) {
        // TODO: FOIL and Card Status
        if(e.getEventType() == HyperlinkEvent.EventType.ENTERED ||
           e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            String cardRef = e.getDescription();
            TokenKey tokenKey = DeckRecognizer.Token.parseTokenKey(cardRef);
            StaticData data = StaticData.instance();
            PaperCard card = data.fetchCard(tokenKey.cardName, tokenKey.setCode, tokenKey.collectorNumber);
            if (card != null){
                // no need to check for card that has Image because CardPicturePanel
                // has automatic integration with cardFetch
                String header;
                if (tokenKey.deckSection != null)
                        header = String.format("%s: %s",
                                Localizer.getInstance().getMessage("lblDeckSection"),
                                tokenKey.deckSection);
                else {
                    if (tokenKey.typeName.equals("illegal_card_request"))
                        header = String.format("%s",
                                Localizer.getInstance().getMessage("lblIllegalCardMsg", getGameFormatLabel()));
                    else
                        header = String.format("%s",
                                Localizer.getInstance().getMessage("lblInvalidCardMsg"));
                }
                CardEdition edition = data.getCardEdition(card.getEdition());
                String editionName = edition != null ? String.format("%s ", edition.getName()) : "";
                String cardLbl = String.format("%s, %s(%s), No. %s", tokenKey.cardName,
                        editionName, tokenKey.setCode, tokenKey.collectorNumber);
                cardImagePreview.setItem(card);
                if (!tokenKey.typeName.equals("legal_card_request"))
                    cardImagePreview.showAsDisabled();
                else
                    cardImagePreview.showAsEnabled();
                cardPreviewLabel.setText(String.format(
                        "<html><span style=\"font-size: 9px; color: %s;\">%s</span><br />" +
                                "<span style=\"font-size: 9px;\">%s</span></html>",
                        getTokenTypeColour(tokenKey.typeName), header, cardLbl));
                // set tooltip
                cardImagePreview.setToolTipText(cardLbl);
            }
        }
    }

    private String getTokenTypeColour(String typeName){
        switch (typeName.trim().toUpperCase()){
            case "LEGAL_CARD_REQUEST":
                return KNOWNCARD_COLOR;
            case "ILLEGAL_CARD_REQUEST":
                return ILLEGAL_CARD_COLOR;
            case "INVALID_CARD_REQUEST":
                return INVALID_CARD_COLOR;
            default:
                return "";
        }
    }

    private void resetCardImagePreviewPanel() {
        this.cardPreviewLabel.setText(Localizer.getInstance().getMessage("lblCardPreview"));
        this.cardImagePreview.setItem(ImageCache.getDefaultImage());
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

    private void toggleNewDeck(){
        boolean createNewDeck = this.createNewDeckCheckbox.isSelected();
        this.controller.setCreateNewDeck(createNewDeck);
        String cmdAcceptLabel = createNewDeck ? this.CREATE_NEW_DECK_CMD_LABEL : this.IMPORT_CARDS_CMD_LABEL;
        this.cmdAcceptButton.setText(cmdAcceptLabel);
    }

    private void displayTokens(final List<DeckRecognizer.Token> tokens) {
        if (tokens.isEmpty() || hasOnlyComment(tokens)) {
            htmlOutput.setText(HTML_WELCOME_TEXT);
            resetCardImagePreviewPanel();
        } else {
            final StringBuilder sbOut = new StringBuilder("<html>");
            sbOut.append(String.format("<head>%s</head>", DeckImport.STYLESHEET));
            sbOut.append(String.format("<body><h3>%s</h3>", Localizer.getInstance().getMessage("lblCurrentDecklist")));
            for (final DeckRecognizer.Token t : tokens)
                sbOut.append(toHTML(t));
            sbOut.append("</body></html>");
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
        CStatisticsImporter.instance().updateStats(tokens);
        cmdAcceptButton.setEnabled(CStatisticsImporter.instance().getTotalCardsInDecklist() > 0);
        this.resetCardImagePreviewPanel();
    }

    private String toHTML(final DeckRecognizer.Token token) {
        if (token == null)
            return "";

        switch (token.getType()) {
            case LEGAL_CARD_REQUEST:
                PaperCard tokenCard = token.getCard();
                return String.format("<div class=\"knowncard\"><a class=\"knowncard\" href=\"%s\">%s x %s " +
                                "<span class=\"editioncode\">(%s)</span> %s %s</a></div>",
                                token.getKey(), token.getNumber(), tokenCard.getName(),
                                tokenCard.getEdition(),
                                tokenCard.getCollectorNumber(), tokenCard.isFoil() ? " - <i>FOIL</i> -" : "");
            case UNKNOWN_CARD_REQUEST:
                return String.format("<div class=\"unknowncard\">%s x %s (%s)</div>",
                        token.getNumber(), token.getText(),
                        Localizer.getInstance().getMessage("lblUnknownCardMsg"));
            case ILLEGAL_CARD_REQUEST:
                return String.format("<div class=\"illegalcard\"><a class=\"illegalcard\" href=\"%s\">" +
                                "%s x %s %s (%s)</a></div>",
                        token.getKey(), token.getNumber(), token.getText(),
                        token.getCard().isFoil() ? " - <i>FOIL</i> -" : "",
                        Localizer.getInstance().getMessage("lblIllegalCardMsg", getGameFormatLabel()));
            case INVALID_CARD_REQUEST:
                return String.format("<div class=\"invalidcard\">" +
                                "<a class=\"invalidcard\" href=\"%s\">%s x %s %s (%s)</a></div>",
                        token.getKey(), token.getNumber(), token.getText(),
                        token.getCard().isFoil() ? " - <i>FOIL</i> -" : "",
                        Localizer.getInstance().getMessage("lblInvalidCardMsg"));
            case DECK_SECTION_NAME:
                return String.format("<div class=\"section\">%s</div>", token.getText());
            case CARD_TYPE:
                return String.format("<div class=\"cardtype\">%s</div>", token.getText());
            case CARD_RARITY:
                return String.format("<div class=\"rarity\">%s</div>", token.getText());
            case CARD_CMC:
                return String.format("<div class=\"cmc\">%s</div>", token.getText());
            case MANA_COLOUR:
                String cssColorClass = token.getText().toLowerCase().trim();
                return String.format("<div class=\"%s\">%s</div>", cssColorClass, token.getText());
            case DECK_NAME:
                return String.format("<div class=\"deckname\">%s: %s</div>",
                        Localizer.getInstance().getMessage("lblDeckName"),
                        token.getText());
            case COMMENT:
                return String.format("<div class=\"comment\">%s</div>", token.getText());
            case UNKNOWN_TEXT:
            default:
                return "";
        }
    }

    private String getGameFormatLabel() {
        if (this.blockCheck.isSelected() && this.blocksDropdown.getSelectedItem() != null)
           return this.blocksDropdown.getSelectedItem().getName();
        else
            return String.format("%s %s",
                    Localizer.getInstance().getMessage("lblFormat"), this.deckFormat.name());
    }
}
