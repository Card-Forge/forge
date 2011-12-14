package forge.control;

import forge.Command;
import forge.deck.Deck;
import forge.game.GameType;
import forge.gui.deckeditor.DeckEditorCommon;
import forge.view.home.HomeTopLevel;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ControlHomeUI {
    private HomeTopLevel view;

    /** @param v0 &emsp; HomeTopLevel */
    public ControlHomeUI(HomeTopLevel v0) {
        view = v0;
    }

    /** @param d0 &emsp; Deck*/
    public void showDeckEditor(Deck d0) {
        GameType gt = GameType.Constructed;
        DeckEditorCommon editor = new DeckEditorCommon(gt);

        final Command exit = new Command() {
            private static final long serialVersionUID = -9133358399503226853L;

            @Override
            public void execute() {
                view.getConstructedController().updateDeckNames();
            }
        };

        editor.show(exit);

        if (d0 != null) {
            editor.getCustomMenu().showDeck(d0, gt);
        }

        editor.setVisible(true);
    }

    /** */
    public void exit() {
        System.exit(0);
    }
}
