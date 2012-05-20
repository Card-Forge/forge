package forge.gui.deckeditor.controllers;

import java.awt.Dialog.ModalityType;

import forge.Command;
import forge.Singletons;
import forge.deck.DeckBase;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.DeckImport;
import forge.gui.deckeditor.views.VAllDecks;
import forge.gui.framework.ICDoc;
import forge.gui.toolbox.FLabel;
import forge.item.InventoryItem;

/** 
 * Controls the "all decks" panel in the deck editor UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CAllDecks implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    //========== Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @SuppressWarnings("serial")
    @Override
    public void initialize() {
        VAllDecks.SINGLETON_INSTANCE.getLstDecks().setDecks(
                Singletons.getModel().getDecks().getConstructed());

        ((FLabel) VAllDecks.SINGLETON_INSTANCE.getBtnImport())
            .setCommand(new Command() { @Override
                public void execute() { importDeck(); } });
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
    }

    /**
     * Opens dialog for importing a deck from a different MTG software.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <TItem extends InventoryItem, TModel extends DeckBase> void importDeck() {
        final ACEditorBase<TItem, TModel> ed = (ACEditorBase<TItem, TModel>)
                CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController();

        final DeckImport dImport = new DeckImport(ed);
        dImport.setModalityType(ModalityType.APPLICATION_MODAL);
        dImport.setVisible(true);
    }
}
