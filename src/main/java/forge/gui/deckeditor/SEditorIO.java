package forge.gui.deckeditor;

import javax.swing.JOptionPane;

import org.apache.commons.lang.StringUtils;

import forge.deck.DeckBase;
import forge.gui.deckeditor.controllers.DeckController;
import forge.gui.deckeditor.views.VCurrentDeck;

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
        return saveDeck(false);
    }

    /**
     * Saves the current deck, with various prompts depending on the
     * current save environment.
     * 
     * @param limitedDeckMode if true, the editor is in limited deck mode,
     * so the overwrite prompt should be adjusted accordingly.
     * 
     * @return boolean, true if success
     */
    @SuppressWarnings("unchecked")
    public static boolean saveDeck(boolean limitedDeckMode) {
        final DeckController<DeckBase> controller = (DeckController<DeckBase>) CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().getDeckController();
        final String name = VCurrentDeck.SINGLETON_INSTANCE.getTxfTitle().getText();

        // Warn if no name
        if (name == null || name.isEmpty()) {
            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(),
                    "Please name your deck using the 'Title' box.",
                    "Save Error!",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        // Confirm if overwrite
        else if (controller.fileExists(name)) {
            int confirmResult = JOptionPane.YES_OPTION;
            if ( !StringUtils.equals(name, controller.getModelName()) ) { // prompt only if name was changed
                confirmResult = JOptionPane.showConfirmDialog(null,
                    limitedDeckMode ? "Would you like to save changes to your deck?"
                    : "There is already a deck named '" + name + "'. Overwrite?",
                    limitedDeckMode ? "Save changes?" : "Overwrite Deck?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            }

            if (confirmResult == JOptionPane.YES_OPTION) { controller.save(); }
        }
        // Confirm if a new deck will be created
        else {
            final int m = JOptionPane.showConfirmDialog(null,
                    "This will create a new deck named '" + name + "'. Continue?",
                    "Create Deck?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (m == JOptionPane.YES_OPTION) { controller.saveAs(name); }
        }

        return true;
    }

    /**
     * Prompts to save changes if necessary.
     * 
     * @return boolean, true if success
     */
    @SuppressWarnings("unchecked")
    public static boolean confirmSaveChanges() {
        if (!((DeckController<DeckBase>) CDeckEditorUI
                .SINGLETON_INSTANCE.getCurrentEditorController().getDeckController()).isSaved()) {
            final int choice = JOptionPane.showConfirmDialog(JOptionPane.getRootFrame(),
                    "Save changes to current deck?",
                    "Save Changes?",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (choice == JOptionPane.CANCEL_OPTION) { return false; }

            if (choice == JOptionPane.YES_OPTION && !saveDeck()) { return false; }
        }

        return true;
    }
}
