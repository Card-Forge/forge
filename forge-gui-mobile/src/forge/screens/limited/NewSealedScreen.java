package forge.screens.limited;

import forge.Forge;
import forge.assets.FSkinFont;
import forge.deck.DeckGroup;
import forge.deck.FDeckEditor;
import forge.deck.FDeckEditor.EditorType;
import forge.deck.io.DeckPreferences;
import forge.gamemodes.limited.SealedCardPoolGenerator;
import forge.gui.FThreads;
import forge.screens.LaunchScreen;
import forge.screens.home.NewGameMenu;
import forge.toolbox.FLabel;
import forge.toolbox.FTextArea;
import forge.util.ThreadUtil;
import forge.util.Utils;

public class NewSealedScreen extends LaunchScreen {
    private static final float PADDING = Utils.scale(10);

    private final FTextArea lblDesc = add(new FTextArea(false,
            Forge.getLocalizer().getMessage("lblSealedText2") + "\n\n" +
            Forge.getLocalizer().getMessage("lblSealedText3") + "\n\n" +
            Forge.getLocalizer().getMessage("lblSealedText4")));

    public NewSealedScreen() {
        super(null, NewGameMenu.getMenu());

        lblDesc.setFont(FSkinFont.get(12));
        lblDesc.setTextColor(FLabel.getInlineLabelColor());
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
        //must run in game thread to prevent blocking UI thread
        ThreadUtil.invokeInGameThread(() -> {
            final DeckGroup sealed = SealedCardPoolGenerator.generateSealedDeck(false);
            if (sealed == null) { return; }

            FThreads.invokeInEdtLater(() -> {
                DeckPreferences.setSealedDeck(sealed.getName());
                Forge.openScreen(new FDeckEditor(EditorType.Sealed, sealed.getName(), false));
                Forge.setBackScreen(new LoadSealedScreen(), false); //ensure pressing back goes to load sealed screen
            });
        });
    }
}
