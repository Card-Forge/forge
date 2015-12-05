package forge.screens.limited;

import forge.FThreads;
import forge.Forge;
import forge.assets.FSkinFont;
import forge.deck.DeckGroup;
import forge.deck.FDeckEditor;
import forge.deck.FDeckEditor.EditorType;
import forge.deck.io.DeckPreferences;
import forge.limited.SealedCardPoolGenerator;
import forge.screens.LaunchScreen;
import forge.screens.home.NewGameMenu;
import forge.toolbox.FLabel;
import forge.toolbox.FTextArea;
import forge.util.ThreadUtil;
import forge.util.Utils;

public class NewSealedScreen extends LaunchScreen {
    private static final float PADDING = Utils.scale(10);

    private final FTextArea lblDesc = add(new FTextArea(false,
            "In Sealed mode, you build a deck from booster packs (maximum 10).\n\n" +
            "Build a deck from the cards you receive. A number of AI opponents will do the same.\n\n" +
            "Then, play against each of the AI opponents."));

    public NewSealedScreen() {
        super(null, NewGameMenu.getMenu());

        lblDesc.setFont(FSkinFont.get(12));
        lblDesc.setTextColor(FLabel.INLINE_LABEL_COLOR);
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        float x = PADDING;
        float y = startY + PADDING;
        float w = width - 2 * PADDING;
        float h = height - y - PADDING;
        lblDesc.setBounds(x, y, w, h);
    }

    @Override
    protected void startMatch() {
        ThreadUtil.invokeInGameThread(new Runnable() { //must run in game thread to prevent blocking UI thread
            @Override
            public void run() {
                final DeckGroup sealed = SealedCardPoolGenerator.generateSealedDeck(false);
                if (sealed == null) { return; }

                FThreads.invokeInEdtLater(new Runnable() {
                    @Override
                    public void run() {
                        DeckPreferences.setSealedDeck(sealed.getName());
                        Forge.openScreen(new FDeckEditor(EditorType.Sealed, sealed.getName(), false));
                        Forge.setBackScreen(new LoadSealedScreen(), false); //ensure pressing back goes to load sealed screen
                    }
                });
            }
        });
    }
}
