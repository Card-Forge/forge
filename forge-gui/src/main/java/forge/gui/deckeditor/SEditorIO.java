package forge.gui.deckeditor;

import org.apache.commons.lang3.StringUtils;

import forge.Singletons;
import forge.gui.deckeditor.controllers.DeckController;
import forge.gui.deckeditor.views.VCurrentDeck;
import forge.gui.framework.FScreen;
import forge.gui.toolbox.FOptionPane;

/** 
 * Handles editor preferences saving and loading.
 * 
 * <br><br><i>(S at beginning of class name denotes a static factory.)</i>
 */
public class SEditorIO {

    /**
     * Saves the current deck, with various prompts depending on the
     * current save environment.
     * 
     * @return boolean, true if success
     */
    public static boolean saveDeck() {
        final DeckController<?> controller = CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().getDeckController();
        final String name = VCurrentDeck.SINGLETON_INSTANCE.getTxfTitle().getText();

        // Warn if no name
        if (name == null || name.isEmpty()) {
            FOptionPane.showMessageDialog("Please name your deck using the 'Title' box.",
                    "Save Error!", FOptionPane.ERROR_ICON);
            return false;
        }
        // Confirm if overwrite
        else if (controller.fileExists(name)) {
            boolean confirmResult = true;
            if ( !StringUtils.equals(name, controller.getModelName()) ) { // prompt only if name was changed
                confirmResult = FOptionPane.showConfirmDialog(
                    "There is already a deck named '" + name + "'. Overwrite?",
                    "Overwrite Deck?");
            }

            if (confirmResult) { controller.save(); }
        }
        // Confirm if a new deck will be created
        else if (FOptionPane.showConfirmDialog("This will create a new deck named '" +
                name + "'. Continue?", "Create Deck?")) {
            controller.saveAs(name);
        }

        return true;
    }

    /**
     * Prompts to save changes if necessary.
     * 
     * @return boolean, true if success
     */
    public static boolean confirmSaveChanges(FScreen screen) {
        if (CDeckEditorUI.SINGLETON_INSTANCE.hasChanges()) {
            Singletons.getControl().ensureScreenActive(screen); //ensure Deck Editor is active before showing dialog
            final int choice = FOptionPane.showOptionDialog("Save changes to current deck?", "Save Changes?",
                    FOptionPane.QUESTION_ICON, new String[] {"Save", "Don't Save", "Cancel"});

            if (choice == -1 || choice == 2) { return false; }

            if (choice == 0 && !saveDeck()) { return false; }
        }

        return true;
    }
}
