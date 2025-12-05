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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;

import forge.Forge;
import forge.Graphics;
import forge.StaticData;
import forge.deck.DeckRecognizer.TokenType;
import forge.game.GameType;
import forge.gui.FThreads;
import forge.gui.util.SOptionPane;
import forge.item.PaperCard;
import forge.toolbox.FCheckBox;
import forge.toolbox.FComboBox;
import forge.toolbox.FDialog;
import forge.toolbox.FOptionPane;
import forge.toolbox.FTextArea;
import forge.util.ItemPool;
import forge.util.Localizer;
import org.apache.commons.lang3.StringUtils;


public class FDeckImportDialog extends FDialog {
    private Consumer<Deck> callback;

    private final FTextArea txtInput = add(new FTextArea(true));
    private final FCheckBox newEditionCheck = add(new FCheckBox(Forge.getLocalizer().getMessage("lblImportLatestVersionCard"), false));
    private final FCheckBox dateTimeCheck = add(new FCheckBox(Forge.getLocalizer().getMessage("lblUseOnlySetsReleasedBefore"), false));
    private final FCheckBox smartCardArtCheck = add(new FCheckBox(Forge.getLocalizer().getMessage("lblUseSmartCardArt"), false));
    private final FCheckBox createNewDeckCheck = add(new FCheckBox(Forge.getLocalizer().getMessage("lblReplaceDeckCheckbox"), false));
//    private final FCheckBox importInDeck = add(new FCheckBox()
    /*setting onlyCoreExpCheck to false allow the copied cards to pass the check of deck contents
      forge-core\src\main\java\forge\deck\Deck.javaDeck.java starting @ Line 320 which is called by
      forge-gui-mobile\src\forge\deck\FDeckEditor.java starting @ Line 373
      (as of latest commit: 8e6655e3ee67688cff66b422d4722c58392eaa7e)
    */
    private final FCheckBox onlyCoreExpCheck = add(new FCheckBox(Forge.getLocalizer().getMessage("lblUseOnlyCoreAndExpansionSets"), false));

    private final FComboBox<String> monthDropdown = add(new FComboBox<>()); //don't need wrappers since skin can't change while this dialog is open
    private final FComboBox<Integer> yearDropdown = add(new FComboBox<>());

    private boolean showOptions;
    private final Deck currentDeck;
    private boolean createNewDeckControl;
    private final DeckImportController controller;
    private final FDeckEditor.DeckEditorConfig editorConfig;

    private final static ImmutableList<String> importOrCancel = ImmutableList.of(Forge.getLocalizer().getMessage("lblImport"), Forge.getLocalizer().getMessage("lblCancel"));

    public FDeckImportDialog(final Deck currentDeck, final FDeckEditor.DeckEditorConfig editorConfig) {
        super(Forge.getLocalizer().getMessage("lblImportFromClipboard"), 2);
        boolean usingInventory = editorConfig.usePlayerInventory();
        boolean replacingDeck = !currentDeck.isEmpty() || usingInventory;
        this.currentDeck = currentDeck;
        this.editorConfig = editorConfig;
        ItemPool<PaperCard> cardPool = editorConfig.getCardPool();
        controller = new DeckImportController(dateTimeCheck, monthDropdown, yearDropdown, replacingDeck);
        String contents = Forge.getClipboard().getContents();
        if (contents == null)
            contents = ""; //prevent NPE
        txtInput.setText(contents);

        GameType gameType = editorConfig.getGameType();
        controller.setGameFormat(gameType);
        List<DeckSection> supportedSections = new ArrayList<>();
        supportedSections.addAll(List.of(editorConfig.getPrimarySections()));
        supportedSections.addAll(List.of(editorConfig.getExtraSections()));
        controller.setAllowedSections(supportedSections);
        controller.setCurrentDeckInEditor(currentDeck);
        if(usingInventory)
            controller.setPlayerInventory(cardPool);

        onlyCoreExpCheck.setSelected(StaticData.instance().isCoreExpansionOnlyFilterSet());
        newEditionCheck.setSelected(StaticData.instance().cardArtPreferenceIsLatest());
        smartCardArtCheck.setSelected(StaticData.instance().isEnabledCardArtSmartSelection());
        createNewDeckCheck.setSelected(replacingDeck);
        this.createNewDeckControl = replacingDeck;

        if(usingInventory)
            controller.setImportBehavior(DeckImportController.ImportBehavior.REPLACE_CURRENT);
        else
            controller.setImportBehavior(createNewDeckControl ? DeckImportController.ImportBehavior.CREATE_NEW : DeckImportController.ImportBehavior.MERGE);

        initButton(0, Forge.getLocalizer().getMessage("lblImport"), e -> FThreads.invokeInBackgroundThread(this::performImport));
        initButton(1, Forge.getLocalizer().getMessage("lblCancel"), e -> hide());

        dateTimeCheck.setCommand(e -> updateDropDownEnabled());
        newEditionCheck.setCommand(e -> setArtPreferenceInController());
        onlyCoreExpCheck.setCommand(e -> setArtPreferenceInController());
        smartCardArtCheck.setCommand(e -> controller.setSmartCardArtOptimisation(smartCardArtCheck.isSelected()));
        createNewDeckCheck.setCommand(e -> {
            createNewDeckControl = createNewDeckCheck.isSelected();
            controller.setImportBehavior(createNewDeckControl ? DeckImportController.ImportBehavior.CREATE_NEW : DeckImportController.ImportBehavior.MERGE);
        });
        setShowOptions(false);
    }

    private void setArtPreferenceInController() {
        boolean isLatest = newEditionCheck.isSelected();
        boolean coreFilter = onlyCoreExpCheck.isSelected();
        controller.setCardArtPreference(isLatest, coreFilter);
    }

    private void updateDropDownEnabled() {
        boolean enabled = dateTimeCheck.isSelected() && this.showOptions;
        monthDropdown.setEnabled(enabled);
        yearDropdown.setEnabled(enabled);
    }

    private void setShowOptions(boolean showOptions) {
        this.showOptions = showOptions;
        dateTimeCheck.setEnabled(showOptions);
        newEditionCheck.setEnabled(showOptions);
        onlyCoreExpCheck.setEnabled(showOptions);
        newEditionCheck.setEnabled(showOptions);
        smartCardArtCheck.setEnabled(showOptions);
        createNewDeckCheck.setEnabled(showOptions);
        updateDropDownEnabled();
    }

    public void setCallback(Consumer<Deck> callback0){
        callback = callback0;
    }

    public void setFreePrintConverter(Function<PaperCard, PaperCard> freePrintConverter) {
        this.controller.setFreePrintConverter(freePrintConverter);
    }

    public DeckImportController.ImportBehavior getImportBehavior() {
        return controller.getImportBehavior();
    }

    public void setImportBannedCards(boolean importBannedCards) {
        controller.importBannedAndRestrictedCards(importBannedCards);
    }

    public void initParse() {
        boolean usingInventory = editorConfig.usePlayerInventory();
        List<DeckRecognizer.Token> tokens = controller.parseInput(txtInput.getText());
        if (usingInventory)
            tokens = controller.constrainTokensToInventory();
        else if (controller.isSmartCardArtEnabled())
            tokens = controller.optimiseCardArtInTokens();
        //ensure at least one known card found on clipboard
        for (DeckRecognizer.Token token : tokens) {
            if (token.getType() == TokenType.LEGAL_CARD || token.getType() == TokenType.FREE_CARD_NOT_IN_INVENTORY) {

                if(usingInventory) {
                    //Settings aren't compatible with player inventories.
                    setShowOptions(false);
                    return;
                }

                setShowOptions(true);

                updateDropDownEnabled();
                setArtPreferenceInController();
                return;
            }
        }

        setButtonEnabled(0, false);
        txtInput.setText(Forge.getLocalizer().getMessage("lblNoKnownCardsOnClipboard"));
    }

    @Override
    public void drawOverlay(Graphics g) {
        super.drawOverlay(g);
        if (showOptions) {
            float y = txtInput.getTop() - FOptionPane.PADDING;
            g.drawLine(BORDER_THICKNESS, getBorderColor(), 0, y, getWidth(), y);
        }
    }

    @Override
    protected float layoutAndGetHeight(float width, float maxHeight) {
        float padding = FOptionPane.PADDING;
        float x = padding;
        float y = padding;
        float w = width - 2 * padding;
        float h;
        if (showOptions) {
            h = monthDropdown.getHeight();
            float fieldPadding = padding / 2;

            newEditionCheck.setBounds(x, y, w / 2, h);
            onlyCoreExpCheck.setBounds(x + w/2, y, w/2, h);
            y += h + fieldPadding;
            dateTimeCheck.setBounds(x, y, w, h);
            y += h + fieldPadding;
            
            float dropDownWidth = (w - fieldPadding) / 2;
            monthDropdown.setBounds(x, y, dropDownWidth, h);
            yearDropdown.setBounds(x + dropDownWidth + fieldPadding, y, dropDownWidth, h);
            y += h + fieldPadding;

            if (!this.currentDeck.isEmpty()){
                smartCardArtCheck.setBounds(x, y, w/2, h);
                createNewDeckCheck.setBounds(x + w/2, y, w/2, h);
            } else
                smartCardArtCheck.setBounds(x, y, w, h);

            y += h + 2 * padding;
        }
        h = txtInput.getPreferredHeight(w);
        float maxTextBoxHeight = maxHeight - y - padding;
        if (h > maxTextBoxHeight) {
            h = maxTextBoxHeight;
        }
        txtInput.setBounds(x, y, w, h);
        y += h + padding;
        if (showOptions) {
            h = newEditionCheck.getHeight();
        }
        return y;
    }

    private static final EnumSet<TokenType> MISSING_TOKENS = EnumSet.of(TokenType.CARD_FROM_NOT_ALLOWED_SET,
            TokenType.CARD_FROM_INVALID_SET, TokenType.UNKNOWN_CARD, TokenType.UNSUPPORTED_CARD,
            TokenType.WARNING_MESSAGE, TokenType.CARD_NOT_IN_INVENTORY);

    private void performImport() {
        List<DeckRecognizer.Token> tokens = controller.parseInput(txtInput.getText()); //ensure deck updated based on any changes to options

        if (editorConfig.usePlayerInventory())
            tokens = controller.constrainTokensToInventory();
        else if (controller.isSmartCardArtEnabled())
            tokens = controller.optimiseCardArtInTokens();

        //if there are any cards that cannot be imported, let user know this and give them the option to cancel
        // Android API StringBuilder isEmpty() is unavailable. https://developer.android.com/reference/java/lang/StringBuilder
        StringBuilder sb = new StringBuilder();
        for (DeckRecognizer.Token token : tokens) {
            if (MISSING_TOKENS.contains(token.getType())) {
                if (sb.length() != 0)
                    sb.append("\n");
                String message = controller.getTokenMessage(token);
                String statusMessage = controller.getTokenStatusMessage(token);
                if(!StringUtils.isBlank(statusMessage))
                    sb.append(String.format("%s - (%s)", message, statusMessage));
                else
                    sb.append(statusMessage);
            }
        }
        if (sb.length() != 0) {
            Localizer localizer = Forge.getLocalizer();
            if (SOptionPane.showOptionDialog(localizer.getMessage("lblFollowingCardsCannotBeImported") + "\n\n" + sb, localizer.getMessage("lblImportRemainingCards"), SOptionPane.WARNING_ICON, importOrCancel) == 1) {
                return;
            }
        }

        final Deck deck = controller.accept(currentDeck.getName()); //must accept in background thread in case a dialog is shown
        if (deck == null) {
            return;
        }

        FThreads.invokeInEdtLater(() -> {
            hide();
            if (callback != null)
                callback.accept(deck);
        });
    }
}
