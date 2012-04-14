package forge.gui.home.utilities;

import forge.Command;
import forge.Singletons;
import forge.deck.DeckBase;
import forge.gui.deckeditor.DeckEditorBase;
import forge.gui.deckeditor.DeckEditorConstructed;
import forge.gui.home.ICSubmenu;

/** 
 * Controls the deck editor submenu option in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CSubmenuDeckEditor implements ICSubmenu {
    /** */
    SINGLETON_INSTANCE;

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void initialize() {
        VSubmenuDeckEditor.SINGLETON_INSTANCE.populate();

    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#getCommand()
     */
    @SuppressWarnings("serial")
    @Override
    public Command getMenuCommand() {
        return new Command() { @Override
            public void execute() { showDeckEditor(); } };
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() { }

    /**
     * Shows constructed mode editor.
     * @param <T> extends DeckBase
     */
    @SuppressWarnings("unchecked")
    private <T extends DeckBase> void showDeckEditor() {
        DeckEditorBase<?, T> editor =
                (DeckEditorBase<?, T>) new DeckEditorConstructed(Singletons.getView().getFrame());
        editor.show(null);
        editor.setVisible(true);
    }
}
