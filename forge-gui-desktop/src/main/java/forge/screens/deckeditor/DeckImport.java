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
import java.util.*;
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
import forge.deck.DeckRecognizer.Token.TokenKey;
import forge.game.GameFormat;
import forge.game.GameType;
import forge.gui.CardPicturePanel;
import forge.item.PaperCard;
import forge.screens.deckeditor.controllers.CDeckEditor;
import forge.screens.deckeditor.controllers.CStatisticsImporter;
import forge.screens.deckeditor.views.VStatisticsImporter;
import forge.toolbox.*;
import forge.util.Localizer;
import forge.view.FDialog;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import static forge.deck.DeckRecognizer.TokenType.*;

/**
  *
 * Dialog for quick import of decks.
 *
 * @param <TModel>
 */
public class DeckImport<TModel extends DeckBase> extends FDialog {
    private static final long serialVersionUID = -5837776824284093004L;

    private final FTextArea txtInput = new FTextArea();
    // Memo: Background colour: #3e4f63
    // UN-USED COLOUR TO USE "#E1E35F;";

    public static final String OK_CARD_IMPORT_COLOUR = "#89DC9F;";
    public static final String WARN_MSG_COLOUR = "#FEC700;";
    public static final String KO_CARD_NO_IMPORT_COLOUR = "#FF977A;";

    public static final String OK_IMPORT_CLASS = "ok_import";
    public static final String WARN_MSG_CLASS = "warn_msg";
    public static final String KO_NOIMPORT_CLASS = "ko_noimport";
    public static final String COMMENT_CLASS = "comment";
    public static final String DECKNAME_CLASS = "deckname";
    public static final String SECTION_CLASS = "section";
    public static final String CARDTYPE_CLASS = "cardtype";
    public static final String CMC_CLASS = "cmc";
    public static final String RARITY_CLASS = "rarity";

    private static final String STYLESHEET = String.format("<style>"
            + "body, h1, h2, h3, a {font-weight: normal; line-height: 1.8px; "
            + " font-family: Arial; font-size: 10px;}"
            + " tr, td { padding: 0px 1px; margin: 0px !important; }"
            + " h3 {font-size: 13px; margin: 2px 0; padding: 0px 5px; font-weight: bold;}"
            + " ul {margin: 5px; padding: 2px; font-size: 8px; }"
            + " ul li {font-size: 10px;}"
            + " code {font-size: 9px;}"
            + " div {margin: 0; text-align: justify; padding: 1px 0 1px 8px; line-height: 1.8px;}"
            + " a:hover { text-decoration: none !important;}"
            + " a:link { text-decoration: none !important;}"
            + " a { text-decoration: none !important;}"
            + " a:active { text-decoration: none !important;}"
            + " table {margin: 5px 0;}"
            + " .bullet {color: #FEC700;}"
            // Card Matching Colours #4F6070
            + " .%s {color: %s !important; font-weight: bold;}"  // ok import
            + " .%s {color: %s !important; font-weight: bold;}"  // warn msg
            + " .%s {color: %s !important; font-weight: bold;}"  // ko no import
            + " .%s {font-style: italic}"  // comment
            + " .%s {background-color: #332200; color: #ffffff; }"  // Deck Name
            // Placeholders CardType colour to reuse: #0e0f21
            + " .%s {font-weight: bold; background-color: #DDDDDD; color: #000000;}"  // section
            + " .%s {font-weight: bold; background-color: #FFCC66; color: #000000;}"  // card type
            + " .%s {font-weight: bold; background-color: #CCCCCC; color: #000000;}"  // cmc
            + " .%s {font-weight: bold; background-color: #F1B27E; color: #000000;}"  // rarity
            + "</style>", OK_IMPORT_CLASS, OK_CARD_IMPORT_COLOUR,
            WARN_MSG_CLASS, WARN_MSG_COLOUR, KO_NOIMPORT_CLASS, KO_CARD_NO_IMPORT_COLOUR,
            COMMENT_CLASS, DECKNAME_CLASS, SECTION_CLASS, CARDTYPE_CLASS, CMC_CLASS, RARITY_CLASS) ;

    private static final String COLOUR_CODED_TAGS = String.format(
            "<ul>" +
            "<li> <span class=\"%s\">%s</span></li>" +
            "<li> <span class=\"%s\">%s</span></li>" +
            "<li> <span class=\"%s\">%s</span></li></ul>",
            OK_IMPORT_CLASS, Localizer.getInstance().getMessage("lblGuideImportCard"),
            WARN_MSG_CLASS, Localizer.getInstance().getMessage("lblGuideWarnMessage"),
            KO_NOIMPORT_CLASS, Localizer.getInstance().getMessage("lblGuideNoImportCard")
            );
    private static final String TIPS_LIST = String.format(
            "<p>%s</p> <p>%s</p> <p>%s</p> <p>%s</p> <p>%s</p> <p>%s</p> <p>%s</p>",
            Localizer.getInstance().getMessage("lblGuideTipsCount",
                    String.format("<span class=\"bullet\">(A) %s</span>", Localizer.getInstance().getMessage("lblGuideTipsTitleCount")),
                    String.format("<code>%s</code>", "\"4 Giant Growth\""),
                    String.format("<code>%s</code>", "\"4x Giant Growth\"")),
            Localizer.getInstance().getMessage("lblGuideTipsSet",
                    String.format("<span class=\"bullet\">(B) %s</span>", Localizer.getInstance().getMessage("lblGuideTipsTitleSet"))),
            Localizer.getInstance().getMessage("lblGuideTipsFoil",
                    String.format("<span class=\"bullet\">(C) %s</span>", Localizer.getInstance().getMessage("lblGuideTipsTitleFoil")),
                    String.format("<code>%s</code>", "Forest+"), "<code>(F)</code>"),
            Localizer.getInstance().getMessage("lblGuideTipsPlaceholder",
                    String.format("<span class=\"bullet\">(D) %s</span>", Localizer.getInstance().getMessage("lblGuideTipsTitlePlaceholder")),
                    "<code>Lands, Creatures, Artifacts</code>", "<code>Common, Uncommon, Rare, Mythic, Special</code>",
                    "<code>CMC0, CC1</code>", "<code>Black, White|Green, Red Blue, Multicolor, Colorless</code>"),
            Localizer.getInstance().getMessage("lblGuideTipsDeckSection",
                    String.format("<span class=\"bullet\">(E) %s</span>", Localizer.getInstance().getMessage("lblGuideTipsTitleDeckSections")),
                    "<code>Main, Sideboard, Commander</code>", "<code>Main</code>"),
            Localizer.getInstance().getMessage("lblGuideTipsDeckName",
                    String.format("<span class=\"bullet\">(F) %s</span>", Localizer.getInstance().getMessage("lblGuideTipsTitleDeckName"))),
            Localizer.getInstance().getMessage("lblGuideTipsDeckFormats",
                    String.format("<span class=\"bullet\">(G) %s</span>", Localizer.getInstance().getMessage("lblGuideTipsTitleDeckFormat")))
    );

    private static final String EXAMPLES_LIST = String.format(
            "<p><code class=\"bullet\">%s</code>: %s</p>" +
            "<p><code class=\"bullet\">%s</code>: %s</p>" +
            "<p><code class=\"bullet\">%s</code>: %s</p>" +
            "<p><code class=\"bullet\">%s</code>: %s</p>" +
            "<p><code class=\"bullet\">%s</code>: %s</p>",
            Localizer.getInstance().getMessage("lblExample1"),
            Localizer.getInstance().getMessage("nlExample1"),
            Localizer.getInstance().getMessage("lblExample2"),
            Localizer.getInstance().getMessage("nlExample2"),
            Localizer.getInstance().getMessage("lblExample3"),
            Localizer.getInstance().getMessage("nlExample3"),
            Localizer.getInstance().getMessage("lblExample4"),
            Localizer.getInstance().getMessage("nlExample4"),
            Localizer.getInstance().getMessage("lblExample5"),
            Localizer.getInstance().getMessage("nlExample5")
    );

    private static final String HTML_WELCOME_TEXT = String.format(
            "<head>" + DeckImport.STYLESHEET + "</head><body>"
                    + "<h3>%s</h3><div>%s</div> "
                    + "<h3>%s</h3><div>%s</div> "
                    + "<br> "
                    + "<h3>%s</h3><div>%s</div> "
                    + "<br></body>",
                Localizer.getInstance().getMessage("nlGuideTitle"),
                Localizer.getInstance().getMessage("nlGuideQuickInstructions", COLOUR_CODED_TAGS),
                Localizer.getInstance().getMessage("nlGuideTipsTitle"),
                Localizer.getInstance().getMessage("nlGuideTipsText", TIPS_LIST),
                Localizer.getInstance().getMessage("nlGuideExamplesTitle"), EXAMPLES_LIST
    );
    private static final int PADDING_TOKEN_MSG_LENGTH = 45;

    private final FHtmlViewer htmlOutput = new FHtmlViewer();
    private final FScrollPane scrollInput = new FScrollPane(this.txtInput, false);
    private final FScrollPane scrollOutput = new FScrollPane(this.htmlOutput, false);
    private final CardPicturePanel cardImagePreview = new CardPicturePanel();
    private final FLabel cardPreviewLabel = new FLabel.Builder()
            .text(Localizer.getInstance().getMessage("lblCardPreview"))
            .fontSize(14).tooltip("").build();
    private final FButton cmdCancelButton = new FButton(Localizer.getInstance().getMessage("lblCancel"));

    private final FButton cmdAcceptButton;  // Not initialised as label will be adaptive.
    private final FCheckBox createNewDeckCheckbox = new FCheckBox(Localizer.getInstance()
            .getMessage("lblNewDeckCheckbox"), false);

    // Release Date
    private final FCheckBox dateTimeCheck = new FCheckBox(Localizer.getInstance()
            .getMessage("lblUseOnlySetsReleasedBefore"), false);
    private final FComboBox<String> monthDropdown = new FComboBox<>();
    private final FComboBox<Integer> yearDropdown = new FComboBox<>();

    // Card Art Preferences
    private final FLabel cardArtPrefsLabel = new FLabel.Builder()
            .text(Localizer.getInstance().getMessage("lblPreferredArt"))
            .fontSize(14).tooltip(Localizer.getInstance().getMessage("nlPreferredArt")).build();
    private FComboBox<String> cardArtPrefsComboBox;
    private final FCheckBox cardArtPrefHasFilterCheckBox = new FCheckBox(Localizer.getInstance()
            .getMessage("lblPrefArtExpansionOnly"), false);

    // Smart Card Art Optimisation
    private final FCheckBox smartCardArtCheckBox = new FCheckBox(Localizer.getInstance().getMessage("lblUseSmartCardArt"), false);

    // Format Filter
    private final FCheckBox includeBnRCheck = new FCheckBox(Localizer.getInstance().getMessage("lblIgnoreBnR"), false);

    // These two components will only be used only when there is no pre-defined Game Format (e.g. Constructed)
    private final FCheckBox formatSelectionCheck = new FCheckBox(Localizer.getInstance()
            .getMessage("lblUseFormatFilter"), false);
    private final FComboBox<GameFormat> formatDropdown = new FComboBox<>();

    private final DeckImportController controller;
    private final CDeckEditor<TModel> host;

    private final String IMPORT_CARDS_CMD_LABEL = Localizer.getInstance().getMessage("lblImportCardsCmd");
    private final String CREATE_NEW_DECK_CMD_LABEL = Localizer.getInstance().getMessage("lblCreateNewCmd");
    private final String SMART_CARDART_TT_NO_DECK = Localizer.getInstance().getMessage("ttUseSmartCardArtNoDeck");
    private final String SMART_CARDART_TT_WITH_DECK = Localizer.getInstance().getMessage("ttUseSmartCardArtWithDeck");
    private final String currentGameType;
    private final VStatisticsImporter statsView;
    private final CStatisticsImporter cStatsView;

    public DeckImport(final CDeckEditor<TModel> g) {
        this.host = g;
        boolean currentDeckIsNotEmpty = !(g.getDeckController().isEmpty());
        GameType currentGameType = g.getGameType();
        this.controller = new DeckImportController(dateTimeCheck, monthDropdown, yearDropdown, currentDeckIsNotEmpty);
        this.controller.setGameFormat(currentGameType);
        if (currentDeckIsNotEmpty)
            this.controller.setCurrentDeckInEditor(this.host.getDeckController().getCurrentDeckInEditor());
        // Get the list of allowed Sections
        List<DeckSection> supportedSections = new ArrayList<>();
        for (DeckSection section : EnumSet.allOf(DeckSection.class)) {
            if (this.host.isSectionImportable(section))
                supportedSections.add(section);
        }
        this.currentGameType = currentGameType.name();
        this.controller.setAllowedSections(supportedSections);

        String cmdBtnLabel = currentDeckIsNotEmpty ? IMPORT_CARDS_CMD_LABEL : CREATE_NEW_DECK_CMD_LABEL;
        this.cmdAcceptButton = new FButton(cmdBtnLabel);
        this.statsView = new VStatisticsImporter(this.controller.currentGameFormatAllowsCommander());
        this.cStatsView = new CStatisticsImporter(this.statsView);
        initUIComponents(g, currentDeckIsNotEmpty);
    }

    private void initUIComponents(CDeckEditor<TModel> g, boolean currentDeckIsNotEmpty) {
//        GraphicsDevice gd = this.getGraphicsConfiguration().getDevice();
//        final int wWidth = (int)(gd.getDisplayMode().getWidth() * 0.85);
//        final int wHeight = (int)(gd.getDisplayMode().getHeight() * 0.8);
        final int wWidth = (int)(Singletons.getView().getFrame().getSize().width * 0.95);
        final int wHeight = (int)(Singletons.getView().getFrame().getSize().height * 0.9);
        this.setPreferredSize(new Dimension(wWidth, wHeight));
        this.setSize(wWidth, wHeight);

        // Set Title
        this.setTitle(Localizer.getInstance().getMessage("lblDeckImporterPanelTitle", this.currentGameType));
        txtInput.setFocusable(true);
        txtInput.setEditable(true);
        showInstructions();

        final FSkin.SkinColor foreColor = FSkin.getColor(FSkin.Colors.CLR_TEXT);

        // === INIT UI COMPONENTS ===
        // --------------------------

        // === TOP COMPONENTS ===

        // == A. Scroll Input (Card List)
        this.scrollInput.setBorder(new FSkin.TitledSkinBorder(BorderFactory.createEtchedBorder(),
                Localizer.getInstance().getMessage("lblCardListTitle"), foreColor));
        this.scrollInput.setViewportBorder(BorderFactory.createLoweredBevelBorder());
        // Action Listeners
        // ----------------
        this.txtInput.getDocument().addDocumentListener(new OnChangeTextUpdate());

        // == B. Scroll Output (Decklist)
        this.scrollOutput.setBorder(new FSkin.TitledSkinBorder(BorderFactory.createEtchedBorder(),
                Localizer.getInstance().getMessage("lblDecklistTitle"), foreColor));
        this.scrollOutput.setViewportBorder(BorderFactory.createLoweredBevelBorder());
        // Action Listeners
        // ----------------
        this.htmlOutput.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                activateCardPreview(e);
            }
        });

        // == C.1 Stats Panel
        FPanel statsPanel = new FPanel(new BorderLayout());
        statsPanel.add(this.statsView.getMainPanel(), BorderLayout.CENTER);
        statsPanel.setOpaque(false);

        // == C.2 Card Preview Panel
        this.cardImagePreview.setOpaque(false);
        this.cardImagePreview.setBorder(new EmptyBorder(2, 5, 2, 5));
        this.cardImagePreview.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT).getColor());
        resetCardImagePreviewPanel();
        FPanel cardPreview = new FPanel(new MigLayout("fill"));
        cardPreview.add(this.cardPreviewLabel, "cell 0 0, align left, w 100%");
        cardPreview.add(this.cardImagePreview, "cell 0 1, w 70%, h 60%, growy, pushy, ax c");

        // === BOTTOM COMPONENTS ===

        // == A. (Closed) Option Panel
        // This component will be used as a Placeholder panel to simulate Show/Hide animation
        JPanel closedOptsPanel = new JPanel(new MigLayout("insets 10, gap 5, left, w 100%"));
        closedOptsPanel.setVisible(true);
        closedOptsPanel.setOpaque(false);
        final TitledBorder showOptsBorder = new TitledBorder(BorderFactory.createEtchedBorder(),
                String.format("\u25B6 %s", Localizer.getInstance().getMessage("lblExtraOptions")));
        showOptsBorder.setTitleColor(foreColor.getColor());
        closedOptsPanel.setBorder(showOptsBorder);
        closedOptsPanel.add(new JSeparator(JSeparator.HORIZONTAL), "w 100%, hidemode 2");

        // == B. (Actual) Options Panel
        JPanel optionsPanel = new JPanel(new MigLayout("insets 10, gap 5, left, h 130!"));
        final TitledBorder border = new TitledBorder(BorderFactory.createEtchedBorder(),
                String.format("\u25BC %s", Localizer.getInstance().getMessage("lblHideOptions")));
        border.setTitleColor(foreColor.getColor());
        optionsPanel.setBorder(border);
        optionsPanel.setVisible(false);
        optionsPanel.setOpaque(false);

        // Action Listeners
        // ----------------
        closedOptsPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                optionsPanel.setVisible(true);
                closedOptsPanel.setVisible(false);
            }
        });
        optionsPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                optionsPanel.setVisible(false);
                closedOptsPanel.setVisible(true);
            }
        });

        // OPTIONS PANEL COMPONENTS
        // ------------------------

        String optPanelsConstrains = "w 130!, h 120!, left, insets 0";

        // B1. Date filter
        this.monthDropdown.setEnabled(false);
        this.yearDropdown.setEnabled(false);
        this.dateTimeCheck.setToolTipText(Localizer.getInstance().getMessage("ttUseOnlySetsReleasedBefore"));
        // Info Label
        FSkin.SkinnedLabel dateFilterInfoLabel = new FSkin.SkinnedLabel(
                Localizer.getInstance().getMessage("nlUseOnlySetsReleasedBefore"));
        dateFilterInfoLabel.setFont(FSkin.getItalicFont());
        dateFilterInfoLabel.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));

        JPanel dateFilterPanel = new JPanel(new MigLayout(optPanelsConstrains));
        dateFilterPanel.setOpaque(false);
        dateFilterPanel.add(this.dateTimeCheck, "cell 0 0, w 90%!, ax left");
        dateFilterPanel.add(this.monthDropdown, "cell 0 1, w 10%, ax left, split 2, pad 0 2 0 0");
        dateFilterPanel.add(this.yearDropdown,  "cell 0 1, w 8%, ax left, split 2");
        dateFilterPanel.add(dateFilterInfoLabel, "cell 0 2, w 80%!, h 22px!, ax left, wrap");

        // Action Listeners
        // ----------------
        this.dateTimeCheck.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final boolean isSel = dateTimeCheck.isSelected();
                monthDropdown.setEnabled(isSel);
                yearDropdown.setEnabled(isSel);
                parseAndDisplay();
            }
        });
        final ActionListener reparseAction = new ActionListener() {
            @Override public void actionPerformed(final ActionEvent e) {
                parseAndDisplay();
            }
        };
        this.yearDropdown.addActionListener(reparseAction);
        this.monthDropdown.addActionListener(reparseAction);

        optionsPanel.add(dateFilterPanel, "cell 0 0, w 90%, left");

        // B2. Card Art Preference Filter

        final String latestOpt = Localizer.getInstance().getMessage("latestArtOpt");
        final String originalOpt = Localizer.getInstance().getMessage("originalArtOpt");
        final String [] choices = {latestOpt, originalOpt};
        this.cardArtPrefsComboBox = new FComboBox<>(choices);
        final String selectedItem = StaticData.instance().cardArtPreferenceIsLatest() ? latestOpt : originalOpt;
        this.cardArtPrefsComboBox.setSelectedItem(selectedItem);
        this.cardArtPrefsComboBox.setToolTipText(Localizer.getInstance().getMessage("nlPreferredArt"));
        // Info Label
        FSkin.SkinnedLabel artPrefInfoLabel = new FSkin.SkinnedLabel(
                Localizer.getInstance().getMessage("nlPreferredArt"));
        artPrefInfoLabel.setFont(FSkin.getItalicFont());
        artPrefInfoLabel.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));

        // Smart Art Checkbox
        this.smartCardArtCheckBox.setToolTipText(currentDeckIsNotEmpty ? SMART_CARDART_TT_WITH_DECK : SMART_CARDART_TT_NO_DECK);
        this.smartCardArtCheckBox.setSelected(StaticData.instance().isEnabledCardArtSmartSelection());

        this.cardArtPrefHasFilterCheckBox.setSelected(StaticData.instance().isCoreExpansionOnlyFilterSet());
        this.cardArtPrefHasFilterCheckBox.setToolTipText(Localizer.getInstance().getMessage("nlPrefArtExpansionOnly"));

        JPanel cardArtPanel = new JPanel(new MigLayout(optPanelsConstrains));
        cardArtPanel.setOpaque(false);
        cardArtPanel.add(this.cardArtPrefsLabel,    "cell 0 0, w 25%, left, split 2");
        cardArtPanel.add(this.cardArtPrefsComboBox, "cell 0 0, w 10%, left, split 2");
        cardArtPanel.add(this.cardArtPrefHasFilterCheckBox,    "cell 0 1, w 15%, left, gaptop 5");
        cardArtPanel.add(this.smartCardArtCheckBox,    "cell 0 2, w 15%, left, gaptop 5");
        cardArtPanel.add(artPrefInfoLabel, "cell 0 3, w 90%, left");

        // Action Listeners
        // ----------------
        ItemListener updateCardArtPreference = new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                String artPreference = cardArtPrefsComboBox.getSelectedItem();
                if (artPreference == null)
                    artPreference = latestOpt;  // default, just in case
                final boolean latestArt = artPreference.equalsIgnoreCase(latestOpt);
                final boolean coreExpFilter = cardArtPrefHasFilterCheckBox.isSelected();
                controller.setCardArtPreference(latestArt, coreExpFilter);
                parseAndDisplay();
            }
        };
        this.cardArtPrefsComboBox.addItemListener(updateCardArtPreference);
        this.cardArtPrefHasFilterCheckBox.addItemListener(updateCardArtPreference);

        this.smartCardArtCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean enableSmartCardArt = smartCardArtCheckBox.isSelected();
                controller.setSmartCardArtOptimisation(enableSmartCardArt);
                parseAndDisplay();
            }
        });

        optionsPanel.add(cardArtPanel,     "cell 1 0, w 100%, left");

        // B3. Block Filter
        this.formatDropdown.setEnabled(false);  // not enabled by default
        this.includeBnRCheck.setToolTipText(Localizer.getInstance().getMessage("ttIgnoreBnR"));
        // Info Label
        FSkin.SkinnedLabel bnrInfoLabel = new FSkin.SkinnedLabel(
                Localizer.getInstance().getMessage("nlIgnoreBnR"));
        bnrInfoLabel.setFont(FSkin.getItalicFont());
        bnrInfoLabel.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));

        JPanel blockFilterPanel = new JPanel(new MigLayout(optPanelsConstrains));
        blockFilterPanel.setOpaque(false);
        // This panel will customise depending on current Deck Editor / Game Format
        if (this.controller.hasNoDefaultGameFormat()) {
            this.controller.fillFormatDropdown(this.formatDropdown);
            this.formatDropdown.setRenderer(new GameFormatDropdownRenderer());
            this.formatDropdown.setSelectedIndex(0);
            this.formatDropdown.addActionListener(new GameFormatComboListener(this.formatDropdown));

            blockFilterPanel.add(this.formatSelectionCheck, "cell 0 0, w 55%, ax left");
            blockFilterPanel.add(this.formatDropdown, "cell 0 1, w 15%, ax left");
            blockFilterPanel.add(this.includeBnRCheck, "cell 0 2, w 45%, ax left, gaptop 5");
            blockFilterPanel.add(bnrInfoLabel, "cell 0 3, left, w 90%");
        } else {
            blockFilterPanel.add(this.includeBnRCheck, "cell 0 0, w 45%, ax left");
            blockFilterPanel.add(bnrInfoLabel, "cell 0 1, left, w 90%");
        }
        optionsPanel.add(blockFilterPanel, "cell 2 0, w 100%, left");

        // Action Listeners
        // ----------------
        if (controller.hasNoDefaultGameFormat()) {
            final ActionListener updateFormatSelectionCheck = new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    final boolean isSel = formatSelectionCheck.isSelected();
                    formatDropdown.setEnabled(isSel);
                    if (!isSel)
                        controller.setCurrentGameFormat(null);  // reset any game format
                    else {
                        GameFormat gameFormat = formatDropdown.getSelectedItem();
                        controller.setCurrentGameFormat(gameFormat);
                    }
                    parseAndDisplay();
                }
            };
            this.formatSelectionCheck.addActionListener(updateFormatSelectionCheck);
            this.formatDropdown.addActionListener(updateFormatSelectionCheck);
        }

        this.includeBnRCheck.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final boolean includeBnR = includeBnRCheck.isSelected();
                controller.importBannedAndRestrictedCards(includeBnR);
                parseAndDisplay();
            }
        });

        // == C Command buttons
        JPanel cmdPanel = new JPanel(new MigLayout("insets 10, gap 5, right, h 40!"));
        cmdPanel.setOpaque(false);
        this.cmdAcceptButton.setEnabled(false);
        if (currentDeckIsNotEmpty) {
            cmdPanel.add(this.createNewDeckCheckbox, "align l, split 3");
            cmdPanel.add(this.cmdAcceptButton, "w 150!, align r, h 26!, split 3");
            cmdPanel.add(this.cmdCancelButton, "w 150!, align r, h 26!, split 3");
        } else {
            cmdPanel.add(this.cmdAcceptButton, "w 150!, align r, h 26!, split 2");
            cmdPanel.add(this.cmdCancelButton, "w 150!, align r, h 26!, split 2");
        }

        // ActionListeners
        // ---------------
        this.cmdCancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                DeckImport.this.processWindowEvent(new WindowEvent(DeckImport.this, WindowEvent.WINDOW_CLOSING));
            }
        });

        this.cmdAcceptButton.addActionListener(new ActionListener() {
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
                host.getDeckController().loadDeck(deck, controller.getCreateNewDeck());
                processWindowEvent(new WindowEvent(DeckImport.this, WindowEvent.WINDOW_CLOSING));
            }
        });

        if (currentDeckIsNotEmpty){
            this.createNewDeckCheckbox.setSelected(false);
            this.createNewDeckCheckbox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    boolean createNewDeck = createNewDeckCheckbox.isSelected();
                    controller.setCreateNewDeck(createNewDeck);
                    String cmdAcceptLabel = createNewDeck ? CREATE_NEW_DECK_CMD_LABEL : IMPORT_CARDS_CMD_LABEL;
                    cmdAcceptButton.setText(cmdAcceptLabel);
                    String smartCardArtChboxTooltip = createNewDeck ? SMART_CARDART_TT_NO_DECK : SMART_CARDART_TT_WITH_DECK;
                    smartCardArtCheckBox.setToolTipText(smartCardArtChboxTooltip);
                    parseAndDisplay();
                }
            });
        }


        // === ASSEMBLING ALL PANELS TOGETHER
        // ==================================
        this.add(this.scrollInput, "cell 0 0, w 40%, growy, pushy, spany 2");
        this.add(this.scrollOutput, "cell 1 0, w 60%, growy, pushy, spany 2");
        this.add(statsPanel, "cell 2 0, w 480:510:550, growy, pushy, ax c");
        this.add(cardPreview, "cell 2 1, w 480:510:550, h 65%, growy, pushy, ax c");
        this.add(closedOptsPanel, "cell 0 2, left, w 100%, h 25!, spanx 3, hidemode 3");
        this.add(optionsPanel, "cell 0 2, left, w 100%, spanx 3, hidemode 3");
        this.add(cmdPanel, "cell 0 3, w 100%, spanx 3");
    }

    private void activateCardPreview(HyperlinkEvent e) {
        if(e.getEventType() == HyperlinkEvent.EventType.ENTERED ||
           e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            String keyString = e.getDescription();
            TokenKey tokenKey = TokenKey.fromString(keyString);
            if (tokenKey == null)
                return;
            PaperCard card = StaticData.instance().fetchCard(tokenKey.cardName, tokenKey.setCode,
                                                                    tokenKey.collectorNumber);
            if (card != null) {
                DeckRecognizer.Token mockToken = createMockTokenFromTokenKey(card, tokenKey);
                setupCardImagePreviewPanel(card, mockToken);
            }
        }
    }

    private DeckRecognizer.Token createMockTokenFromTokenKey(PaperCard card, TokenKey tokenKey){
        DeckRecognizer.Token mockToken;
        switch (tokenKey.tokenType) {
            case CARD_FROM_INVALID_SET:
                mockToken = DeckRecognizer.Token.CardInInvalidSet(card, 0, true);
                break;
            case CARD_FROM_NOT_ALLOWED_SET:
                mockToken = DeckRecognizer.Token.NotAllowedCard(card, 0, true);
                break;
            case LIMITED_CARD:
                mockToken = DeckRecognizer.Token.LimitedCard(card, 0, tokenKey.deckSection, tokenKey.limitedType, true);
                break;
            case LEGAL_CARD:
                mockToken = DeckRecognizer.Token.LegalCard(card, 0, tokenKey.deckSection, true);
                break;
            default:
                mockToken = null;
                break;
        }
        return mockToken;
    }

    private void setupCardImagePreviewPanel(PaperCard card, DeckRecognizer.Token token) {
        StaticData data = StaticData.instance();
        // no need to check for card that has Image because CardPicturePanel
        // has automatic integration with cardFetch
        StringBuilder statusLbl = new StringBuilder();
        if (token != null && token.isCardToken()) {
            String cssClass = getTokenCSSClass(token.getType());
            if (token.getType() == LIMITED_CARD)
                cssClass = WARN_MSG_CLASS;
            String statusMsg = String.format("<span class=\"%s\" style=\"font-size: 9px;\">%s</span>", cssClass,
                                                                                        getTokenStatusMessage(token));
            statusLbl.append(statusMsg);
        }

        CardEdition edition = data.getCardEdition(card.getEdition());
        String editionName = edition != null ? String.format("%s ", edition.getName()) : "";
        StringBuilder editionLbl = new StringBuilder("<span style=\"font-size: 9px;\">");
        editionLbl.append(String.format("<b>%s</b>: \"%s\" (<code>%s</code>) - Collector Nr. %s",
                Localizer.getInstance().getMessage("lblSet"), editionName, card.getEdition(), card.getCollectorNumber()));
        if ((token.getType() == LEGAL_CARD) || ((token.getType() == LIMITED_CARD) && this.controller.importBannedAndRestrictedCards())){
            editionLbl.append(String.format(" - <b class=\"%s\">%s: %s</b>", OK_IMPORT_CLASS,
                    Localizer.getInstance().getMessage("lblDeckSection"), token.getTokenSection()));
        }
        editionLbl.append("</span>");
        cardImagePreview.setItem(card);

        if (token.getType() == LEGAL_CARD || (token.getType() == LIMITED_CARD && this.controller.importBannedAndRestrictedCards()))
            cardImagePreview.showAsEnabled();
        else
            cardImagePreview.showAsDisabled();
        cardPreviewLabel.setText(String.format("<html>%s %s<br>%s</html>", STYLESHEET, editionLbl, statusLbl));

        // set tooltip
        String tooltip = String.format("%s [%s] #%s", card.getName(), card.getEdition(),
                card.getCollectorNumber());
        cardImagePreview.setToolTipText(tooltip);
    }

    private void resetCardImagePreviewPanel() {
        this.cardPreviewLabel.setText(Localizer.getInstance().getMessage("lblCardPreview"));
        this.cardImagePreview.setItem(ImageCache.getDefaultImage());
        this.cardImagePreview.showAsEnabled();  // reset alpha value
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
        }
    }

    private void parseAndDisplay() {
        List<DeckRecognizer.Token> tokens = controller.parseInput(txtInput.getText());
        if (controller.isSmartCardArtEnabled())
            tokens = controller.optimiseCardArtInTokens();
        displayTokens(tokens);
        updateSummaries(tokens);
    }

    private void displayTokens(final List<DeckRecognizer.Token> tokens) {
        if (tokens.isEmpty() || hasOnlyComment(tokens)) {
            showInstructions();
            resetCardImagePreviewPanel();
        } else {
            final StringBuilder sbOut = new StringBuilder();
            sbOut.append(String.format("<head>%s</head>", DeckImport.STYLESHEET));
            sbOut.append(String.format("<body><h3>%s</h3>",
                    Localizer.getInstance().getMessage("lblCurrentDecklist")));
            sbOut.append("<table>");
            for (final DeckRecognizer.Token t : tokens)
                sbOut.append(toHTML(t));
            sbOut.append("</table>");
            htmlOutput.setText(FSkin.encodeSymbols(sbOut.toString(), false));
        }
    }

    private void showInstructions() {
        htmlOutput.setText(FSkin.encodeSymbols(HTML_WELCOME_TEXT, false));
    }

    private boolean hasOnlyComment(final List<DeckRecognizer.Token> tokens) {
        for (DeckRecognizer.Token token : tokens) {
            if (token.getType() != COMMENT && token.getType() != UNKNOWN_TEXT)
                return false;
        }
        return true;
    }

    private void updateSummaries(final List<DeckRecognizer.Token> tokens) {
        this.cStatsView.updateStats(tokens, this.controller.importBannedAndRestrictedCards());
        cmdAcceptButton.setEnabled(this.cStatsView.getTotalCardsInDecklist() > 0);
        Object displayedCardInPanel = this.cardImagePreview.getDisplayed();
        if (!(displayedCardInPanel instanceof PaperCard))  // also accounts for any null
            this.resetCardImagePreviewPanel();
        else {
            PaperCard cardDisplayed = (PaperCard) displayedCardInPanel;
            // this will return either the same card instance or its [un]foiled version
            // null will be returned if not found in card list anymore
            PaperCard cardFromDecklist = this.controller.getCardFromDecklist(cardDisplayed);
            if (cardFromDecklist == null) {
                cardFromDecklist = this.controller.getCardFromDecklistByName(cardDisplayed.getName());
                if (cardFromDecklist == null)
                    this.resetCardImagePreviewPanel(); // current displayed card is not in decklist
                else {
                    DeckRecognizer.Token cardToken = controller.getTokenFromCardInDecklist(cardFromDecklist);
                    setupCardImagePreviewPanel(cardFromDecklist, cardToken);
                }
            }
            else {
                DeckRecognizer.Token cardToken = controller.getTokenFromCardInDecklist(cardFromDecklist);
                setupCardImagePreviewPanel(cardFromDecklist, cardToken);
            }
        }
    }

//    private void setupCardImagePreview(PaperCard cardDisplayed) {
//        if (this.controller.isTokenInListLegal(cardDisplayed)) {
//            this.cardImagePreview.setItem(cardDisplayed);
//            this.cardImagePreview.showAsEnabled();
//        } else if (this.controller.isTokenInListLimited(cardDisplayed)) {
//            this.cardImagePreview.setItem(cardDisplayed);
//            if (this.includeBnRCheck.isSelected())
//                this.cardImagePreview.showAsEnabled();
//            else
//                this.cardImagePreview.showAsDisabled();
//        } else { // any other card token NOT legal nor limited
//            this.cardImagePreview.setItem(cardDisplayed);
//            this.cardImagePreview.showAsDisabled();
//        }
//    }

    private String toHTML(final DeckRecognizer.Token token) {
        if (token == null)
            return "";
        String tokenMsg = getTokenMessage(token);
        if (tokenMsg == null)
            return "";
        String tokenStatus = getTokenStatusMessage(token);
        String cssClass = getTokenCSSClass(token.getType());
        if (tokenStatus.length() == 0)
            tokenMsg = padEndWithHTMLSpaces(tokenMsg, 2*PADDING_TOKEN_MSG_LENGTH+10);
        else {
            tokenMsg = padEndWithHTMLSpaces(tokenMsg, PADDING_TOKEN_MSG_LENGTH);
            tokenStatus = padEndWithHTMLSpaces(tokenStatus, PADDING_TOKEN_MSG_LENGTH);
        }
        if (token.isCardToken())
            tokenMsg = String.format("<a class=\"%s\" href=\"%s\">%s</a>", cssClass,
                    token.getKey().toString(), tokenMsg);

        if (tokenStatus == null) {
            String tokenTag = String.format("<td colspan=\"2\" class=\"%s\">%s</td>", cssClass, tokenMsg);
            return String.format("<tr>%s</tr>", tokenTag);
        }

        String tokenTag = "<td class=\"%s\">%s</td>";
        String tokenMsgTag = String.format(tokenTag, cssClass, tokenMsg);
        String tokenStatusTag;
        if (token.getType() == LIMITED_CARD)
            cssClass = WARN_MSG_CLASS;
        tokenStatusTag = String.format(tokenTag, cssClass, tokenStatus);
        return String.format("<tr>%s %s</tr>", tokenMsgTag, tokenStatusTag);
    }

    private static String padEndWithHTMLSpaces(String targetMsg, int limit) {
        StringBuilder spacer = new StringBuilder();
        for (int i = targetMsg.length(); i < limit; i++)
            spacer.append("&nbsp;");
        return String.format("%s%s", targetMsg, spacer);
    }

    private String getTokenMessage(DeckRecognizer.Token token) {
        switch (token.getType()) {
            case LEGAL_CARD:
            case LIMITED_CARD:
            case CARD_FROM_NOT_ALLOWED_SET:
            case CARD_FROM_INVALID_SET:
                return String.format("%s x %s %s", token.getQuantity(), token.getText(), getTokenFoilLabel(token));
            // Card Warning Msgs
            case UNKNOWN_CARD:
            case UNSUPPORTED_CARD:
                return token.getQuantity() > 0 ? String.format("%s x %s", token.getQuantity(), token.getText())
                        : token.getText();

            case UNSUPPORTED_DECK_SECTION:
                return String.format("%s: %s", Localizer.getInstance().getMessage("lblWarningMsgPrefix"),
                                        Localizer.getInstance()
                                                .getMessage("lblWarnDeckSectionNotAllowedInEditor", token.getText(),
                                                        this.currentGameType));

            // Special Case of Card moved into another section (e.g. Commander from Sideboard)
            case WARNING_MESSAGE:
                return String.format("%s: %s", Localizer.getInstance()
                                .getMessage("lblWarningMsgPrefix"), token.getText());

            // Placeholders
            case DECK_SECTION_NAME:
                return String.format("%s: %s", Localizer.getInstance().getMessage("lblDeckSection"),
                                        token.getText());

            case CARD_RARITY:
                return String.format("%s: %s", Localizer.getInstance().getMessage("lblRarity"),
                                        token.getText());

            case CARD_TYPE:
            case CARD_CMC:
            case MANA_COLOUR:
            case COMMENT:
                return token.getText();

            case DECK_NAME:
                return String.format("%s: %s", Localizer.getInstance().getMessage("lblDeckName"),
                        token.getText());

            case UNKNOWN_TEXT:
            default:
                return null;

        }
    }

    private String getTokenStatusMessage(DeckRecognizer.Token token){
        if (token == null)
            return "";

        switch (token.getType()) {
            case LIMITED_CARD:
                return String.format("%s: %s", Localizer.getInstance().getMessage("lblWarningMsgPrefix"),
                        Localizer.getInstance().getMessage("lblWarnLimitedCard",
                        StringUtils.capitalize(token.getLimitedCardType().name()), getGameFormatLabel()));

            case CARD_FROM_NOT_ALLOWED_SET:
                return Localizer.getInstance().getMessage("lblErrNotAllowedCard", getGameFormatLabel());

            case CARD_FROM_INVALID_SET:
                return Localizer.getInstance().getMessage("lblErrCardEditionDate");

            case UNSUPPORTED_CARD:
                return Localizer.getInstance().getMessage("lblErrUnsupportedCard", this.currentGameType);

            case UNKNOWN_CARD:
                return String.format("%s: %s", Localizer.getInstance().getMessage("lblWarningMsgPrefix"),
                        Localizer.getInstance().getMessage("lblWarnUnknownCardMsg"));

            case UNSUPPORTED_DECK_SECTION:
            case WARNING_MESSAGE:
            case COMMENT:
            case CARD_CMC:
            case MANA_COLOUR:
            case CARD_TYPE:
            case DECK_SECTION_NAME:
            case CARD_RARITY:
            case DECK_NAME:
            case LEGAL_CARD:
            case UNKNOWN_TEXT:
            default:
                return "";

        }

    }

    private String getTokenCSSClass(DeckRecognizer.TokenType tokenType){
        switch (tokenType){
            case LEGAL_CARD:
                return OK_IMPORT_CLASS;
            case LIMITED_CARD:
                return this.controller.importBannedAndRestrictedCards() ? OK_IMPORT_CLASS : WARN_MSG_CLASS;
            case CARD_FROM_NOT_ALLOWED_SET:
            case CARD_FROM_INVALID_SET:
            case UNSUPPORTED_CARD:
                return KO_NOIMPORT_CLASS;
            case UNSUPPORTED_DECK_SECTION:
            case WARNING_MESSAGE:
            case UNKNOWN_CARD:
                return WARN_MSG_CLASS;
            case COMMENT:
                return COMMENT_CLASS;
            case DECK_NAME:
                return DECKNAME_CLASS;
            case DECK_SECTION_NAME:
                return SECTION_CLASS;
            case CARD_TYPE:
                return CARDTYPE_CLASS;
            case CARD_RARITY:
                return RARITY_CLASS;
            case CARD_CMC:
            case MANA_COLOUR:
                return CMC_CLASS;
            case UNKNOWN_TEXT:
            default:
                return "";
        }
    }

    private String getTokenFoilLabel(DeckRecognizer.Token token) {
        if (!token.isCardToken())
            return "";
        final String foilMarker = "- (Foil)";
        return token.getCard().isFoil() ? foilMarker : "";
    }

    private String getGameFormatLabel() {
         return String.format("\"%s\"", this.controller.getCurrentGameFormatName());
    }
}

class GameFormatDropdownRenderer extends JLabel implements ListCellRenderer<GameFormat> {
    JSeparator separator;

    public GameFormatDropdownRenderer() {
        setOpaque(true);
        setBorder(new EmptyBorder(1, 1, 1, 1));
        separator = new JSeparator(JSeparator.HORIZONTAL);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends GameFormat> list, GameFormat value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
        if (value == null || value.equals(GameFormat.NoFormat))
            return separator;

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }
        setFont(list.getFont());
        setText(value.getName());
        return this;
    }
}

class GameFormatComboListener implements ActionListener {
    FComboBox<GameFormat> combo;
    GameFormat separator = GameFormat.NoFormat;
    GameFormat currentItem;

    GameFormatComboListener(FComboBox<GameFormat> combo) {
        this.combo = combo;
        currentItem = combo.getSelectedItem();
    }

    public void actionPerformed(ActionEvent e) {
        GameFormat tempItem = combo.getSelectedItem();
        if (separator.equals(tempItem))
            combo.setSelectedItem(currentItem);
        else
            currentItem = tempItem;
    }
}
