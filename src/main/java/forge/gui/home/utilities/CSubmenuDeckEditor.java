package forge.gui.home.utilities;

import forge.Command;
import forge.deck.DeckBase;
import forge.gui.deckeditor.DeckEditorBase;
import forge.gui.deckeditor.DeckEditorConstructed;
import forge.gui.home.ICSubmenu;

/** 
 * TODO: Write javadoc for this type.
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
        DeckEditorBase<?, T> editor = (DeckEditorBase<?, T>) new DeckEditorConstructed();
        editor.show(null);
        editor.setAlwaysOnTop(true);
        editor.setVisible(true);
    }
}
