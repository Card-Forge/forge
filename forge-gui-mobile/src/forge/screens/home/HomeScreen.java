package forge.screens.home;

import java.util.ArrayList;

import forge.screens.FScreen;
import forge.assets.FSkinImage;
import forge.deck.FDeckChooser;
import forge.game.GameType;
import forge.screens.achievements.AchievementsScreen;
import forge.screens.settings.SettingsScreen;
import forge.toolbox.FButton;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.util.Utils;

public class HomeScreen extends FScreen {
    private static final float PADDING = Utils.scale(5);

    private final FLabel lblLogo = add(new FLabel.Builder().icon(FSkinImage.LOGO).iconInBackground().iconScaleFactor(1).build());
    private final ArrayList<FButton> buttons = new ArrayList<FButton>();

    public HomeScreen() {
        super((Header)null);

        addButton("New Game", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                NewGameMenu.getPreferredScreen().open();
            }
        });
        addButton("Load Game", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                LoadGameMenu.getPreferredScreen().open();
            }
        });
        addButton("Play Online", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
            }
        });
        buttons.get(buttons.size() - 1).setEnabled(false);
        addButton("Deck Manager", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                FDeckChooser.promptForDeck("Deck Manager", GameType.DeckManager, false, null);
            }
        });
        addButton("Achievements", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                AchievementsScreen.show();
            }
        });
        addButton("Settings", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                SettingsScreen.show();
            }
        });
    }

    private void addButton(String caption, FEventHandler command) {
        buttons.add(add(new FButton(caption, command)));
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        float buttonWidth = width - 2 * PADDING;
        float buttonHeight = buttons.get(0).getFont().getCapHeight() * 3.5f;
        float x = PADDING;
        float y = height;
        float dy = buttonHeight + PADDING;

        for (int i = buttons.size() - 1; i >= 0; i--) {
            y -= dy;
            buttons.get(i).setBounds(x, y, buttonWidth, buttonHeight);
        }

        float logoSize = y - 2 * PADDING;
        y = PADDING;
        if (logoSize > buttonWidth) {
            y += (logoSize - buttonWidth) / 2;
            logoSize = buttonWidth;
        }
        x = (width - logoSize) / 2;
        lblLogo.setBounds(x, y, logoSize, logoSize);
    }
}
