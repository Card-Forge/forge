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
import java.util.List;

import com.google.common.collect.ImmutableList;

import forge.Forge;
import forge.Graphics;
import forge.StaticData;
import forge.deck.DeckRecognizer.TokenType;
import forge.game.GameType;
import forge.gui.FThreads;
import forge.gui.util.SOptionPane;
import forge.toolbox.FCheckBox;
import forge.toolbox.FComboBox;
import forge.toolbox.FDialog;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FOptionPane;
import forge.toolbox.FTextArea;
import forge.util.Callback;
import forge.util.Localizer;


public class FDeckImportDialog extends FDialog {
    private Callback<Deck> callback;

    private final FTextArea txtInput = add(new FTextArea(true));
    private final FCheckBox newEditionCheck = add(new FCheckBox(Localizer.getInstance().getMessage("lblImportLatestVersionCard"), false));
    private final FCheckBox dateTimeCheck = add(new FCheckBox(Localizer.getInstance().getMessage("lblUseOnlySetsReleasedBefore"), false));
    private final FCheckBox smartCardArtCheck = add(new FCheckBox(Localizer.getInstance().getMessage("lblUseSmartCardArt"), false));
    private final FCheckBox createNewDeckCheck = add(new FCheckBox(Localizer.getInstance().getMessage("lblNewDeckCheckbox"), false));
//    private final FCheckBox importInDeck = add(new FCheckBox()
    /*setting onlyCoreExpCheck to false allow the copied cards to pass the check of deck contents
      forge-core\src\main\java\forge\deck\Deck.javaDeck.java starting @ Line 320 which is called by
      forge-gui-mobile\src\forge\deck\FDeckEditor.java starting @ Line 373
      (as of latest commit: 8e6655e3ee67688cff66b422d4722c58392eaa7e)
    */
    private final FCheckBox onlyCoreExpCheck = add(new FCheckBox(Localizer.getInstance().getMessage("lblUseOnlyCoreAndExpansionSets"), false));

    private final FComboBox<String> monthDropdown = add(new FComboBox<>()); //don't need wrappers since skin can't change while this dialog is open
    private final FComboBox<Integer> yearDropdown = add(new FComboBox<>());

    private final boolean showOptions;
    private final boolean currentDeckIsEmpty;
    private boolean createNewDeckControl;
    private final DeckImportController controller;

    private final static ImmutableList<String> importOrCancel = ImmutableList.of(Localizer.getInstance().getMessage("lblImport"), Localizer.getInstance().getMessage("lblCancel"));

    public FDeckImportDialog(final boolean replacingDeck, final FDeckEditor.EditorType editorType) {
        super(Localizer.getInstance().getMessage("lblImportFromClipboard"), 2);
        controller = new DeckImportController(dateTimeCheck, monthDropdown, yearDropdown, replacingDeck);
        String contents = Forge.getClipboard().getContents();
        if (contents == null)
            contents = ""; //prevent NPE
        txtInput.setText(contents);

        if (FDeckEditor.allowsReplacement(editorType)) {
            GameType gameType = GameType.valueOf(editorType.name());
            controller.setGameFormat(gameType);
            List<DeckSection> supportedSections = new ArrayList<>();
            supportedSections.add(DeckSection.Main);
            supportedSections.add(DeckSection.Sideboard);
            if (editorType != FDeckEditor.EditorType.Constructed)
                supportedSections.add(DeckSection.Commander);
            controller.setAllowedSections(supportedSections);
        }

        onlyCoreExpCheck.setSelected(StaticData.instance().isCoreExpansionOnlyFilterSet());
        newEditionCheck.setSelected(StaticData.instance().cardArtPreferenceIsLatest());
        smartCardArtCheck.setSelected(StaticData.instance().isEnabledCardArtSmartSelection());
        createNewDeckCheck.setSelected(replacingDeck);
        this.currentDeckIsEmpty = !replacingDeck;
        this.createNewDeckControl = replacingDeck;

        initButton(0, Localizer.getInstance().getMessage("lblImport"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                FThreads.invokeInBackgroundThread(new Runnable() {
                    @Override
                    public void run() {
                        List<DeckRecognizer.Token> tokens = controller.parseInput(txtInput.getText()); //ensure deck updated based on any changes to options

                        if (controller.isSmartCardArtEnabled())
                            tokens = controller.optimiseCardArtInTokens();

                        //if there are any cards that cannot be imported, let user know this and give them the option to cancel
                        StringBuilder sb = new StringBuilder();
                        for (DeckRecognizer.Token token : tokens) {
                            if (token.getType() == TokenType.CARD_FROM_NOT_ALLOWED_SET
                                    || token.getType() == TokenType.CARD_FROM_INVALID_SET
                                    || token.getType() == TokenType.UNKNOWN_CARD
                                    || token.getType() == TokenType.UNSUPPORTED_CARD) {
                                if (sb.length() > 0)
                                    sb.append("\n");
                                sb.append(token.getQuantity()).append(" ").append(token.getText());
                            }
                        }
                        if (sb.length() > 0) {
                            if (SOptionPane.showOptionDialog(Localizer.getInstance().getMessage("lblFollowingCardsCannotBeImported") + "\n\n" + sb, Localizer.getInstance().getMessage("lblImportRemainingCards"), SOptionPane.INFORMATION_ICON, importOrCancel) == 1) {
                                return;
                            }
                        }

                        final Deck deck = controller.accept(); //must accept in background thread in case a dialog is shown
                        if (deck == null) { return; }

                        FThreads.invokeInEdtLater(new Runnable() {
                            @Override
                            public void run() {
                                hide();
                                if (callback != null)
                                    callback.run(deck);
                            }
                        });
                    }
                });
            }
        });
        initButton(1, Localizer.getInstance().getMessage("lblCancel"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                hide();
            }
        });

        List<DeckRecognizer.Token> tokens = controller.parseInput(txtInput.getText());
        if (controller.isSmartCardArtEnabled())
            tokens = controller.optimiseCardArtInTokens();
        //ensure at least one known card found on clipboard
        for (DeckRecognizer.Token token : tokens) {
            if (token.getType() == TokenType.LEGAL_CARD) {
                showOptions = true;

                dateTimeCheck.setCommand(new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        updateDropDownEnabled();
                    }
                });
                newEditionCheck.setCommand(new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {setArtPreferenceInController();}
                });
                onlyCoreExpCheck.setCommand(new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {setArtPreferenceInController();}
                });
                smartCardArtCheck.setCommand(new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        controller.setSmartCardArtOptimisation(smartCardArtCheck.isSelected());
                    }
                });
                createNewDeckCheck.setCommand(new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        createNewDeckControl = createNewDeckCheck.isSelected();
                        controller.setCreateNewDeck(createNewDeckControl);
                    }
                });
                updateDropDownEnabled();
                setArtPreferenceInController();
                return;
            }
        }

        showOptions = false;
        setButtonEnabled(0, false);
        txtInput.setText(Localizer.getInstance().getMessage("lblNoKnownCardsOnClipboard"));
    }

    private void setArtPreferenceInController() {
        boolean isLatest = newEditionCheck.isSelected();
        boolean coreFilter = onlyCoreExpCheck.isSelected();
        controller.setCardArtPreference(isLatest, coreFilter);
    }

    private void updateDropDownEnabled() {
        boolean enabled = dateTimeCheck.isSelected();
        monthDropdown.setEnabled(enabled);
        yearDropdown.setEnabled(enabled);
    }

    public void setCallback(Callback<Deck> callback0){
        callback = callback0;
    }

    public boolean createNewDeck(){ return this.createNewDeckControl; }

    @Override
    public void drawOverlay(Graphics g) {
        super.drawOverlay(g);
        if (showOptions) {
            float y = txtInput.getTop() - FOptionPane.PADDING;
            g.drawLine(BORDER_THICKNESS, BORDER_COLOR, 0, y, getWidth(), y);
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

            if (!this.currentDeckIsEmpty){
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
}
