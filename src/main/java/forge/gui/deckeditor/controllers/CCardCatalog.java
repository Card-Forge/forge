package forge.gui.deckeditor.controllers;

import forge.Command;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.views.VCardCatalog;
import forge.gui.framework.ICDoc;
import forge.gui.toolbox.FLabel;
import forge.item.InventoryItem;

/** 
 * Controls the "card catalog" panel in the deck editor UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CCardCatalog implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    // refresh analysis on add

    private CCardCatalog() {
    }

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
    @Override
    @SuppressWarnings("serial")
    public void initialize() {
        // Add/remove buttons
        ((FLabel) VCardCatalog.SINGLETON_INSTANCE.getBtnAdd()).setCommand(new Command() {
            @Override
            public void execute() {
                CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().addCard();
                CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().getTableCatalog().getTable().requestFocusInWindow();
                CStatistics.SINGLETON_INSTANCE.update();
                CProbabilities.SINGLETON_INSTANCE.update();
            }
        });

        ((FLabel) VCardCatalog.SINGLETON_INSTANCE.getBtnAdd4()).setCommand(new Command() {
            @Override
            public void execute() {
                final InventoryItem item = CDeckEditorUI.SINGLETON_INSTANCE
                        .getCurrentEditorController().getTableCatalog().getSelectedCard();

                for (int i = 0; i < 4; i++) {
                    if (item != null && item.equals(CDeckEditorUI.SINGLETON_INSTANCE
                            .getCurrentEditorController().getTableCatalog().getSelectedCard())) {
                        CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().addCard();
                    }
                }
                CStatistics.SINGLETON_INSTANCE.update();
                CProbabilities.SINGLETON_INSTANCE.update();
            }
        });
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
    }
}
