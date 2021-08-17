package forge.adventure.libgdxgui.screens.limited;

import forge.adventure.libgdxgui.Forge;
import forge.adventure.libgdxgui.assets.FSkinFont;
import forge.deck.DeckGroup;
import forge.adventure.libgdxgui.deck.FDeckEditor;
import forge.adventure.libgdxgui.deck.FDeckEditor.EditorType;
import forge.deck.io.DeckPreferences;
import forge.gamemodes.limited.SealedCardPoolGenerator;
import forge.gui.FThreads;
import forge.adventure.libgdxgui.screens.LaunchScreen;
import forge.adventure.libgdxgui.screens.home.NewGameMenu;
import forge.adventure.libgdxgui.toolbox.FLabel;
import forge.adventure.libgdxgui.toolbox.FTextArea;
import forge.util.Localizer;
import forge.util.ThreadUtil;
import forge.adventure.libgdxgui.util.Utils;

public class NewSealedScreen extends LaunchScreen {
    private static final float PADDING = Utils.scale(10);

    private final FTextArea lblDesc = add(new FTextArea(false,
            Localizer.getInstance().getMessage("lblSealedText2") + "\n\n" +
            Localizer.getInstance().getMessage("lblSealedText3") + "\n\n" +
            Localizer.getInstance().getMessage("lblSealedText4")));

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
