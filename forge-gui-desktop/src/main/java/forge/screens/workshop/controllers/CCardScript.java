package forge.screens.workshop.controllers;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Arrays;
import java.util.Map.Entry;

import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Style;
import javax.swing.text.StyledDocument;

import com.google.common.collect.ImmutableList;

import forge.Singletons;
import forge.card.CardDb;
import forge.card.CardRules;
import forge.card.CardScriptInfo;
import forge.card.CardScriptParser;
import forge.game.card.Card;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.screens.workshop.menus.WorkshopFileMenu;
import forge.screens.workshop.views.VCardDesigner;
import forge.screens.workshop.views.VCardScript;
import forge.screens.workshop.views.VWorkshopCatalog;
import forge.toolbox.FOptionPane;

/**
 * Controls the "card script" panel in the workshop UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CCardScript implements ICDoc {
    SINGLETON_INSTANCE;

    private PaperCard currentCard;
    private CardScriptInfo currentScriptInfo;
    private boolean isTextDirty;
    private boolean switchInProgress;
    private boolean refreshing;

    private CCardScript() {
        VCardScript.SINGLETON_INSTANCE.getTxtScript().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(final DocumentEvent arg0) {
                updateDirtyFlag();
            }
            @Override
            public void insertUpdate(final DocumentEvent arg0) {
                updateDirtyFlag();
            }
            @Override
            public void changedUpdate(final DocumentEvent arg0) {
                //Plain text components do not fire these events
            }
        });
        VCardScript.SINGLETON_INSTANCE.getTxtScript().addFocusListener(new FocusListener() {
            @Override
            public void focusLost(final FocusEvent e) {
                refresh();
            }
            @Override
            public void focusGained(final FocusEvent e) {
            }
        });
    }

    private void updateDirtyFlag() {
        final boolean isTextNowDirty = !refreshing && currentScriptInfo != null && !VCardScript.SINGLETON_INSTANCE.getTxtScript().getText().equals(currentScriptInfo.getText());
        if (isTextDirty == isTextNowDirty) { return; }
        isTextDirty = isTextNowDirty;
        VCardDesigner.SINGLETON_INSTANCE.getBtnSaveCard().setEnabled(isTextNowDirty);
        VCardScript.SINGLETON_INSTANCE.getTabLabel().setText((isTextNowDirty ? "*" : "") + "Card Script");
        WorkshopFileMenu.updateSaveEnabled();
    }

    public PaperCard getCurrentCard() {
        return currentCard;
    }

    public void showCard(final PaperCard card) {
        if (currentCard == card || switchInProgress) { return; }

        if (!canSwitchAway(true)) { //ensure current card saved before changing to a different card
            VWorkshopCatalog.SINGLETON_INSTANCE.getCardManager().setSelectedItem(currentCard); //return selection to current card //TODO: fix so clicking away again doesn't cause weird selection problems
            return;
        }

        currentCard = card;
        currentScriptInfo = card != null ? CardScriptInfo.getScriptFor(currentCard.getRules().getName()) : null;
        refresh();
    }

    public void refresh() {
        if (refreshing) { return; }
        refreshing = true;
        final JTextPane txtScript = VCardScript.SINGLETON_INSTANCE.getTxtScript();
        txtScript.setText(currentScriptInfo != null ? currentScriptInfo.getText() : "");
        txtScript.setEditable(currentScriptInfo != null ? currentScriptInfo.canEdit() : false);
        txtScript.setCaretPosition(0); //keep scrolled to top

        final StyledDocument doc = VCardScript.SINGLETON_INSTANCE.getDoc();
        final Style error = VCardScript.SINGLETON_INSTANCE.getErrorStyle();
        if (FModel.getPreferences().getPrefBoolean(FPref.DEV_WORKSHOP_SYNTAX) && currentScriptInfo != null) {
            for (final Entry<Integer, Integer> region : new CardScriptParser(currentScriptInfo.getText()).getErrorRegions().entrySet()) {
                doc.setCharacterAttributes(region.getKey(), region.getValue(), error, true);
            }
        }
        refreshing = false;
    }

    public boolean hasChanges() {
        return (currentScriptInfo != null && isTextDirty);
    }

    private static final ImmutableList<String> switchAwayOptions = ImmutableList.of("Save", "Don't Save", "Cancel");
    public boolean canSwitchAway(final boolean isCardChanging) {
        if (switchInProgress) { return false; }
        if (!hasChanges()) { return true; }

        switchInProgress = true;
        Singletons.getControl().ensureScreenActive(FScreen.WORKSHOP_SCREEN); //ensure Workshop is active before showing dialog
        final int choice = FOptionPane.showOptionDialog(
                String.format("Save changes to %s?", currentCard),
                "Save Changes?",
                FOptionPane.QUESTION_ICON,
                switchAwayOptions);
        switchInProgress = false;

        if (choice == -1 || choice == 2) { return false; }

        if (choice == 0 && !saveChanges()) { return false; }

        if (!isCardChanging) {
            refresh(); //refresh if current card isn't changing to restore script text from file
        }
        return true;
    }

    public boolean saveChanges() {
        if (!hasChanges()) { return true; } //not need if text hasn't been changed

        final String text = VCardScript.SINGLETON_INSTANCE.getTxtScript().getText();
        if (!currentScriptInfo.trySetText(text)) {
            return false;
        }

        updateDirtyFlag();

        final String oldName = currentCard.getName();

        final CardRules newRules = CardRules.fromScript(Arrays.asList(text.split("\n")));
        final CardDb cardDb = newRules.isVariant() ? FModel.getMagicDb().getVariantCards() :
            FModel.getMagicDb().getCommonCards();

        cardDb.getEditor().putCard(newRules);
        if (newRules.getName().equals(oldName)) {
            Card.updateCard(currentCard);
        } else {
            currentCard = cardDb.getCard(newRules.getName());
        }

        VWorkshopCatalog.SINGLETON_INSTANCE.getCardManager().repaint();
        VWorkshopCatalog.SINGLETON_INSTANCE.getCDetailPicture().showItem(currentCard);
        refresh();
        return true;
    }

    //========== Overridden methods

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    public void initialize() {
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
    }
}
