package forge.screens.home;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkin;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinImage;
import forge.deck.FDeckChooser;
import forge.game.GameType;
import forge.screens.FScreen;
import forge.screens.achievements.AchievementsScreen;
import forge.screens.online.OnlineMenu.OnlineScreen;
import forge.screens.planarconquest.ConquestMenu;
import forge.screens.quest.QuestMenu;
import forge.screens.settings.SettingsScreen;
import forge.toolbox.FButton;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.util.Localizer;
import forge.util.Utils;

public class HomeScreen extends FScreen {
    private static final float PADDING = Utils.scale(5);
    private static final FSkinColor clrTheme = FSkinColor.get(Colors.CLR_THEME);
    private static final FSkinColor l00 = clrTheme.stepColor(0);
    private static final FSkinColor d80 = clrTheme.stepColor(-80);
    public static final float MAIN_MENU_WIDTH_FACTOR = 0.35f;

    public static final HomeScreen instance = new HomeScreen();

    private final FLabel lblLogo = add(new FLabel.Builder().icon(
            new FImage() {
                final float size = Forge.getScreenWidth() * 0.6f;
                @Override
                public float getWidth() {
                    return size;
                }
                @Override
                public float getHeight() {
                    return size;
                }
                @Override
                public void draw(Graphics g, float x, float y, float w, float h) {
                    if (FSkin.hdLogo == null)
                        FSkinImage.LOGO.draw(g, x, y, w, h);
                    else
                        g.drawImage(FSkin.hdLogo, x, y, w, h);
                }
            }
    ).iconInBackground().iconScaleFactor(1).build());
    private final ButtonScroller buttonScroller = add(new ButtonScroller());
    private final List<MenuButton> buttons = new ArrayList<>();
    private int activeButtonIndex, baseButtonCount;
    private FDeckChooser deckManager;
    private boolean QuestCommander = false;
    private String QuestWorld = "";

    private HomeScreen() {
        super((Header)null);

        final Localizer localizer = Localizer.getInstance();

        addButton(localizer.getMessage("lblNewGame"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                activeButtonIndex = 0;
                Forge.lastButtonIndex = activeButtonIndex;
                NewGameMenu.getPreferredScreen().open();
            }
        });
        addButton(localizer.getMessage("lblLoadGame"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                activeButtonIndex = 1;
                Forge.lastButtonIndex = activeButtonIndex;
                LoadGameMenu.getPreferredScreen().open();
            }
        });
        addButton(localizer.getMessage("lblPlayOnline"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                activeButtonIndex = 2;
                Forge.lastButtonIndex = activeButtonIndex;
                OnlineScreen.Lobby.open();
            }
        });
        addButton(localizer.getMessage("lblDeckManager"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                activeButtonIndex = 3;
                Forge.lastButtonIndex = activeButtonIndex;
                if (deckManager == null) {
                    deckManager = new FDeckChooser(GameType.DeckManager, false, null) {
                        @Override
                        protected float doLandscapeLayout(float width, float height) {
                            //don't show header in landscape mode
                            getHeader().setBounds(0, 0, 0, 0);
                            doLayout(0, width, height);
                            return 0;
                        }
                    };
                    deckManager.setHeaderCaption(localizer.getMessage("lblDeckManager"));
                }
                Forge.openScreen(deckManager);
            }
        });
        addButton(localizer.getMessage("lblAchievements"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                activeButtonIndex = 4;
                Forge.lastButtonIndex = activeButtonIndex;
                AchievementsScreen.show();
            }
        });
        addButton(localizer.getMessage("lblSettings"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                activeButtonIndex = 5;
                Forge.lastButtonIndex = activeButtonIndex;
                SettingsScreen.show(true);
            }
        });
        baseButtonCount = buttons.size();
    }

    private void addButton(String caption, FEventHandler command) {
        buttons.add(buttonScroller.add(new MenuButton(caption, command)));
    }

    public void updateQuestCommanderMode(boolean isCommander){
        QuestCommander = isCommander;
    }

    public void updateQuestWorld(String questWorld){
        QuestWorld = questWorld;
    }

    public void openMenu(int index){
        if (index < 0)
            return; //menu on startup
        if (index == 2)
            OnlineScreen.Lobby.open();
        else if (index < 6)
            NewGameMenu.getPreferredScreen().open();
        else if (index == 6)
            QuestMenu.launchQuestMode(QuestMenu.LaunchReason.StartQuestMode, HomeScreen.instance.getQuestCommanderMode());
        else if (index == 7)
            ConquestMenu.launchPlanarConquest(ConquestMenu.LaunchReason.StartPlanarConquest);
    }

    public boolean getQuestCommanderMode() {
        return QuestCommander;
    }

    public int getActiveButtonIndex() {
        return activeButtonIndex;
    }

    public String getQuestWorld() {
        return QuestWorld;
    }

    public void addButtonForMode(String caption, final FEventHandler command) {
        //ensure we don't add the same mode button more than once
        for (int i = baseButtonCount; i < buttons.size(); i++) {
            if (buttons.get(i).getText().equals(caption)) {
                final int index = i;
                buttons.get(i).setCommand(new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        activeButtonIndex = index;
                        Forge.lastButtonIndex = activeButtonIndex;
                        command.handleEvent(e);
                    }
                });
                activeButtonIndex = i;
                return;
            }
        }
        final int index = buttons.size();
        activeButtonIndex = index;
        addButton(caption, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                activeButtonIndex = index;
                Forge.lastButtonIndex = activeButtonIndex;
                command.handleEvent(e);
            }
        });
        revalidate();
        buttonScroller.scrollIntoView(buttons.get(index));
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        float buttonWidth = width - 2 * PADDING;
        float buttonHeight = buttons.get(0).getFont().getCapHeight() * 3.5f;
        float x = PADDING;
        float dy = buttonHeight + PADDING;
        float y = height - buttons.size() * dy;

        buttonScroller.buttonHeight = buttonHeight;
        buttonScroller.padding = PADDING;
        buttonScroller.setBounds(x, y, buttonWidth, height - y);

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

        buttonScroller.buttonHeight = buttonHeight;
        buttonScroller.padding = 0;
        buttonScroller.setBounds(x, y, buttonWidth, height - y);

        return width - mainMenuWidth; //move hosted screens to the right of menu
    }

    @Override
    public FScreen getLandscapeBackdropScreen() {
        return null;
    }

    @Override
    protected void drawBackground(Graphics g) {
        super.drawBackground(g);

        //handle drawing main menu background for Landscape mode
        float w = getWidth();
        float h = getHeight();
        if (w > h) {
            w = h * MAIN_MENU_WIDTH_FACTOR;

            float y1 = 0;
            float h1 = h;
            float y2 = 0;
            float h2 = 0;
            if (activeButtonIndex != -1) {
                MenuButton activeButton = buttons.get(activeButtonIndex);
                h1 = buttonScroller.getTop() + activeButton.getTop();
                y2 = h1 + activeButton.getHeight();
                h2 = h - y2;

                //prevent showing active button height behind logo
                float scrollerTop = buttonScroller.getTop();
                if (y2 < scrollerTop) { //if entire active button behind logo, only need to draw one rectangle
                    h1 = h;
                    y2 = 0;
                    h2 = 0;
                }
                else if (h1 < scrollerTop) {
                    h1 = scrollerTop;
                }
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

    private class ButtonScroller extends FScrollPane {
        private float buttonHeight, padding;

        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            float y = 0;
            float dy = buttonHeight + padding;
            for (MenuButton button : buttons) {
                button.setBounds(0, y, visibleWidth, buttonHeight);
                y += dy;
            }
            return new ScrollBounds(visibleWidth, y - padding);
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
                g.drawText(getText(), getFont(), getForeColor(), 0, 0, getWidth(), getHeight(), false, Align.left, true);
            }
            else { //draw buttons normally for portrait mode
                super.draw(g);
            }
        }
    }
}
