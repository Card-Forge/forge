package forge.screens.workshop.controllers;

import forge.UiCommand;
import forge.Singletons;
import forge.card.CardDb;
import forge.card.CardRules;
import forge.card.CardScriptInfo;
import forge.game.card.Card;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.screens.match.controllers.CDetail;
import forge.screens.match.controllers.CPicture;
import forge.screens.workshop.menus.WorkshopFileMenu;
import forge.screens.workshop.views.VCardDesigner;
import forge.screens.workshop.views.VCardScript;
import forge.screens.workshop.views.VWorkshopCatalog;
import forge.toolbox.FOptionPane;
import forge.toolbox.FTextEditor;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.util.Arrays;


/** 
 * Controls the "card script" panel in the workshop UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CCardScript implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private PaperCard currentCard;
    private CardScriptInfo currentScriptInfo;
    private boolean isTextDirty;
    private boolean switchInProgress;

    private CCardScript() {
        VCardScript.SINGLETON_INSTANCE.getTxtScript().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent arg0) {
                updateDirtyFlag();
            }

            @Override
            public void insertUpdate(DocumentEvent arg0) {
                updateDirtyFlag();
            }

            @Override
            public void changedUpdate(DocumentEvent arg0) {
                 //Plain text components do not fire these events
            }
        });
    }

    private void updateDirtyFlag() {
        boolean isTextNowDirty = currentScriptInfo != null && !VCardScript.SINGLETON_INSTANCE.getTxtScript().getText().equals(currentScriptInfo.getText());
        if (isTextDirty == isTextNowDirty) { return; }
        isTextDirty = isTextNowDirty;
        VCardDesigner.SINGLETON_INSTANCE.getBtnSaveCard().setEnabled(isTextNowDirty);
        VCardScript.SINGLETON_INSTANCE.getTabLabel().setText((isTextNowDirty ? "*" : "") + "Card Script");
        WorkshopFileMenu.updateSaveEnabled();
    }

    public PaperCard getCurrentCard() {
        return currentCard;
    }

    public void showCard(PaperCard card) {
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
        FTextEditor txtScript = VCardScript.SINGLETON_INSTANCE.getTxtScript();
        txtScript.setText(currentScriptInfo != null ? currentScriptInfo.getText() : "");
        txtScript.setEditable(currentScriptInfo != null ? currentScriptInfo.canEdit() : false);
        txtScript.setCaretPosition(0); //keep scrolled to top
    }

    public boolean hasChanges() {
        return (currentScriptInfo != null && isTextDirty);
    }

    public boolean canSwitchAway(boolean isCardChanging) {
        if (switchInProgress) { return false; }
        if (!hasChanges()) { return true; }

        switchInProgress = true;
        Singletons.getControl().ensureScreenActive(FScreen.WORKSHOP_SCREEN); //ensure Workshop is active before showing dialog
        final int choice = FOptionPane.showOptionDialog(
                "Save changes to " + currentCard + "?",
                "Save Changes?",
                FOptionPane.QUESTION_ICON,
                new String[] {"Save", "Don't Save", "Cancel"});
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

        String text = VCardScript.SINGLETON_INSTANCE.getTxtScript().getText();
        if (!currentScriptInfo.trySetText(text)) {
            return false;
        }

        updateDirtyFlag();

        String oldName = currentCard.getName();

        CardRules newRules = CardRules.fromScript(Arrays.asList(text.split("\n")));
        CardDb cardDb = newRules.isVariant() ? FModel.getMagicDb().getVariantCards() :
            FModel.getMagicDb().getCommonCards();

        cardDb.getEditor().putCard(newRules);
        if (newRules.getName().equals(oldName)) {
            Card.updateCard(currentCard);
        }
        else {
            currentCard = cardDb.getCard(newRules.getName());
        }

        VWorkshopCatalog.SINGLETON_INSTANCE.getCardManager().repaint();
        CDetail.SINGLETON_INSTANCE.showCard(currentCard);
        CPicture.SINGLETON_INSTANCE.showImage(currentCard);
        return true;
    }

    //========== Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public UiCommand getCommandOnSelect() {
        return null;
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
