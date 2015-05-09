package forge.screens.home;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge;
import forge.Graphics;
import forge.screens.FScreen;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinImage;
import forge.deck.FDeckChooser;
import forge.game.GameType;
import forge.screens.achievements.AchievementsScreen;
import forge.screens.online.OnlineMenu;
import forge.screens.settings.SettingsScreen;
import forge.toolbox.FButton;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.util.Utils;

public class HomeScreen extends FScreen {
    private static final float PADDING = Utils.scale(5);
    private static final FSkinColor clrTheme = FSkinColor.get(Colors.CLR_THEME);
    private static final FSkinColor l00 = clrTheme.stepColor(0);
    private static final FSkinColor d80 = clrTheme.stepColor(-80);
    private static final float MAIN_MENU_WIDTH_FACTOR = 0.35f;

    public static final HomeScreen instance = new HomeScreen();

    private final FLabel lblLogo = add(new FLabel.Builder().icon(FSkinImage.LOGO).iconInBackground().iconScaleFactor(1).build());
    private final ArrayList<MenuButton> buttons = new ArrayList<MenuButton>();
    private FDeckChooser deckManager;

    private HomeScreen() {
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
                OnlineMenu.getPreferredScreen().open();
            }
        });
        addButton("Deck Manager", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                if (deckManager == null) {
                    deckManager = new FDeckChooser(GameType.DeckManager, false, null) {
                        @Override
                        public FScreen getLandscapeBackdropScreen() {
                            return HomeScreen.instance; //use home screen as backdrop when in landscape mode
                        }
                    };
                    deckManager.setHeaderCaption("Deck Manager");
                }
                Forge.openScreen(deckManager);
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
        buttons.add(add(new MenuButton(caption, command)));
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

    @Override
    protected float doLandscapeLayout(float width, float height) {
        float mainMenuWidth = height * MAIN_MENU_WIDTH_FACTOR;
        float logoSize = mainMenuWidth - 2 * PADDING;
        lblLogo.setBounds(PADDING, PADDING, logoSize, logoSize);

        float x = 2 * PADDING;
        float y = lblLogo.getBottom() + PADDING;
        float buttonWidth = mainMenuWidth - x;
        float buttonHeight = Utils.AVG_FINGER_HEIGHT * 0.9f;

        for (MenuButton button : buttons) {
            button.setBounds(x, y, buttonWidth, buttonHeight);
            y += buttonHeight;
        }
        return width - mainMenuWidth; //move hosted screens to the right of menu
    }

    @Override
    protected void drawBackground(Graphics g) {
        super.drawBackground(g);

        //handle drawing main menu background for Landscape mode
        float w = getWidth();
        float h = getHeight();
        if (w > h) {
            w = h * MAIN_MENU_WIDTH_FACTOR;

            MenuButton activeButton = null;
            if (NewGameMenu.getPreferredScreen().isCurrentScreen()) {
                activeButton = buttons.get(0);
            }
            else if (LoadGameMenu.getPreferredScreen().isCurrentScreen()) {
                activeButton = buttons.get(1);
            }
            else if (OnlineMenu.getPreferredScreen().isCurrentScreen()) {
                activeButton = buttons.get(2);
            }
            else if (Forge.getCurrentScreen() == deckManager) {
                activeButton = buttons.get(3);
            }
            else if (AchievementsScreen.isCurrentScreen()) {
                activeButton = buttons.get(4);
            }
            else if (SettingsScreen.isCurrentScreen()) {
                activeButton = buttons.get(5);
            }

            float y1 = 0;
            float h1 = h;
            float y2 = 0;
            float h2 = 0;
            if (activeButton != null) {
                h1 = activeButton.getTop();
                y2 = activeButton.getBottom();
                h2 = h - y2;
            }

            float w1 = w * 0.66f;
            float w2 = w - w1;
            g.fillRect(l00, 0, y1, w1, h1);
            if (h2 > 0) {
                g.fillRect(l00, 0, y2, w1, h2);
            }
            g.fillGradientRect(l00, d80, false, w1, y1, w2, h1);
            if (h2 > 0) {
                g.fillGradientRect(l00, d80, false, w1, y2, w2, h2);
            }
        }
    }

    private class MenuButton extends FButton {
        public MenuButton(String caption, FEventHandler command) {
            super(caption, command);
        }

        @Override
        public void draw(Graphics g) {
            if (Forge.isLandscapeMode()) {
                //draw text only for Landscape mode
                g.drawText(getText(), getFont(), getForeColor(), 0, 0, getWidth(), getHeight(), false, HAlignment.LEFT, true);
            }
            else { //draw buttons normally for portrait mode
                super.draw(g);
            }
        }
    }
}
