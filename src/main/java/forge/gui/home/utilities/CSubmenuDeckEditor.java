package forge.gui.home.utilities;

import forge.Command;
import forge.Singletons;
import forge.deck.DeckBase;
import forge.game.GameType;
import forge.gui.deckeditor.DeckEditorBase;
import forge.gui.deckeditor.DeckEditorConstructed;
import forge.gui.deckeditor.DeckEditorLimited;
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
            public void execute() { showDeckEditor(GameType.Constructed, null); } };
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() { }

    /**
     * @param <T> &emsp;
     * @param gt0 &emsp; GameType
     * @param d0 &emsp; Deck
     */
    @SuppressWarnings("unchecked")
    public <T extends DeckBase> void showDeckEditor(GameType gt0, T d0) {

        DeckEditorBase<?, T> editor = null;
        if (gt0 == GameType.Constructed) {
            editor = (DeckEditorBase<?, T>) new DeckEditorConstructed();
        } else if (gt0 == GameType.Draft) {
            editor = (DeckEditorBase<?, T>) new DeckEditorLimited(Singletons.getModel().getDecks().getDraft());
        } else if (gt0 == GameType.Sealed) {
            editor = (DeckEditorBase<?, T>) new DeckEditorLimited(Singletons.getModel().getDecks().getSealed());
        }

        editor.show(null);

        if (d0 != null) {
            editor.getController().setModel(d0);
        }

        editor.setAlwaysOnTop(true);
        editor.setVisible(true);
    }
}
