package forge.screens.home;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
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
import forge.gamemodes.replay.ReplayLauncher;
import forge.gamemodes.tutorial.HowToPlay;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.assets.FSkinFont;
import forge.screens.FScreen;
import forge.screens.achievements.AchievementsScreen;
import forge.screens.match.arena.ArenaMenus;
import forge.screens.online.OnlineMenu.OnlineScreen;
import forge.screens.planarconquest.ConquestMenu;
import forge.screens.quest.QuestMenu;
import forge.screens.settings.SettingsScreen;
import forge.toolbox.FButton;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.toolbox.Frost;
import forge.util.Utils;

public class HomeScreen extends FScreen {
    private static final float PADDING = Utils.scale(5);
    private static final FSkinColor clrTheme = FSkinColor.get(Colors.CLR_THEME);
    private static final FSkinColor l00 = clrTheme.stepColor(0);
    private static final FSkinColor d80 = clrTheme.stepColor(-80);
    public static final float MAIN_MENU_WIDTH_FACTOR = 0.35f;

    /**
     * Width of the landscape menu column: 35% of the window height on a phone, and on desktop as wide as that would be
     * at the Interface Size, so a big window does not get a column of giant buttons.
     */
    public static float getMainMenuWidth(float screenHeight) {
        if (GuiBase.isMobile()) {
            return screenHeight * MAIN_MENU_WIDTH_FACTOR;
        }
        return Math.min(screenHeight * MAIN_MENU_WIDTH_FACTOR, Utils.scale(Utils.BASE_HEIGHT * MAIN_MENU_WIDTH_FACTOR));
    }

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
                    if (FSkin.getLogo() == null)
                        FSkinImage.LOGO.draw(g, x, y, w, h);
                    else
                        g.drawImage(FSkin.getLogo(), x, y, w, h);
                }
            }
    ).iconInBackground().iconScaleFactor(1).build());
    private final ButtonScroller buttonScroller = add(new ButtonScroller());
    //desktop: the way out at the foot of the menu column, and which Forge this is
    private final MenuButton btnExit = add(new MenuButton(Forge.getLocalizer().getMessage("lblExit"), e -> Forge.exit(false)));
    private final FLabel lblVersion = add(new FLabel.Builder().text("Forge " + Forge.getDeviceAdapter().getVersionString())
            .font(FSkinFont.get(10)).textColor(FSkinColor.get(Colors.CLR_TEXT).alphaColor(0.45f)).align(Align.left).build());
    private final List<MenuButton> buttons = new ArrayList<>();
    private int activeButtonIndex, baseButtonCount;
    private FDeckChooser deckManager;
    private boolean QuestCommander = false;
    private String QuestWorld = "";

    private HomeScreen() {
        super((Header)null);

        addButton(Forge.getLocalizer().getMessage("lblNewGame"), e -> {
            activeButtonIndex = 0;
            Forge.lastButtonIndex = activeButtonIndex;
            NewGameMenu.getPreferredScreen().open();
        });
        addButton(Forge.getLocalizer().getMessage("lblLoadGame"), e -> {
            activeButtonIndex = 1;
            Forge.lastButtonIndex = activeButtonIndex;
            LoadGameMenu.getPreferredScreen().open();
        });
        addButton(Forge.getLocalizer().getMessage("lblPlayOnline"), e -> {
            activeButtonIndex = 2;
            Forge.lastButtonIndex = activeButtonIndex;
            OnlineScreen.Lobby.open();
        });
        addButton(Forge.getLocalizer().getMessage("lblDeckManager"), e -> {
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
                deckManager.setHeaderCaption(Forge.getLocalizer().getMessage("lblDeckManager"));
            }
            Forge.openScreen(deckManager);
        });
        addButton(Forge.getLocalizer().getMessage("lblAchievements"), e -> {
            activeButtonIndex = 4;
            Forge.lastButtonIndex = activeButtonIndex;
            AchievementsScreen.show();
        });
        addButton(Forge.getLocalizer().getMessage("lblSettings"), e -> {
            activeButtonIndex = 5;
            Forge.lastButtonIndex = activeButtonIndex;
            SettingsScreen.show(true);
        });
        addButton(Forge.getLocalizer().getMessage("lblHowToPlay"), e -> HowToPlay.chooseAndStart());
        addButton("Replays", e -> ReplayLauncher.chooseAndPlay());
        addButton(Forge.getLocalizer().getMessage("lblHelp"), e -> FThreads.invokeInEdtLater(() -> {
            try {
                if (Forge.getDeviceAdapter().isConnectedToInternet()) {
                    FOptionPane.showOptionDialog("Join Discord option will open the invite link to join Forge Discord server. Forge Support option will open the Forge Support Channel.", "Choose option", FOptionPane.INFORMATION_ICON, List.of("Join Discord", "Forge Support"), -1, result -> {
                        switch (result) {
                            case 0:
                                Gdx.net.openURI("https://discord.gg/3v9JCVr");
                                break;
                            case 1:
                                Gdx.net.openURI("https://discord.com/channels/267367946135928833/692000787856883752");
                                break;
                            default:
                                break;
                        }
                    });
                } else {
                    FOptionPane.showErrorDialog("Internet Connection required to open Forge Discord server", "No Internet");
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }));
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
                buttons.get(i).setCommand(e -> {
                    activeButtonIndex = index;
                    Forge.lastButtonIndex = activeButtonIndex;
                    command.handleEvent(e);
                });
                activeButtonIndex = i;
                return;
            }
        }
        final int index = buttons.size();
        activeButtonIndex = index;
        addButton(caption, e -> {
            activeButtonIndex = index;
            Forge.lastButtonIndex = activeButtonIndex;
            command.handleEvent(e);
        });
        revalidate();
        buttonScroller.scrollIntoView(buttons.get(index));
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        btnExit.setVisible(false);
        lblVersion.setVisible(false);
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
        float mainMenuWidth = getMainMenuWidth(height);
        float logoSize = mainMenuWidth - 2 * PADDING;
        float buttonHeight = Utils.AVG_FINGER_HEIGHT * 0.9f;
        boolean desktop = !GuiBase.isMobile();
        if (desktop) { //a shorter window: a smaller logo rather than menu entries scrolled out of sight under Exit
            float versionHeight = Math.round(lblVersion.getFont().getCapHeight() * 4);
            float menuHeight = (baseButtonCount + 1) * buttonHeight + versionHeight + 3 * PADDING;
            logoSize = Math.max(mainMenuWidth * 0.4f, Math.min(logoSize, height - menuHeight));
        }
        lblLogo.setBounds((mainMenuWidth - logoSize) / 2, PADDING, logoSize, logoSize);

        float x = 2 * PADDING;
        float y = lblLogo.getBottom() + PADDING;
        float buttonWidth = mainMenuWidth - x;

        float bottom = height;
        btnExit.setVisible(desktop);
        lblVersion.setVisible(desktop);
        if (desktop) { //Exit and the version at the foot of the column, under the menu
            float versionHeight = Math.round(lblVersion.getFont().getCapHeight() * 4);
            lblVersion.setBounds(x + PADDING * 3, height - versionHeight, buttonWidth - PADDING * 3, versionHeight);
            btnExit.setBounds(x, lblVersion.getTop() - buttonHeight, buttonWidth, buttonHeight);
            bottom = btnExit.getTop() - PADDING;
        }

        buttonScroller.buttonHeight = buttonHeight;
        buttonScroller.padding = 0;
        buttonScroller.setBounds(x, y, buttonWidth, bottom - y);

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
        //a desktop window keeps the landscape layout whatever shape it is dragged to (see FScreen.doLayout)
        if (w > h || (Forge.isLandscapeMode() && Forge.getDeviceAdapter().hasWindowControls())) {
            w = getMainMenuWidth(h);

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

            if (ArenaMenus.column(g, w, h, true)) { //Arena look: a glass column, the active entry washed in gold with a bright bar
                drawHoverBand(g, null, w);
                if (activeButtonIndex != -1) {
                    final MenuButton active = buttons.get(activeButtonIndex);
                    final float top = Math.max(buttonScroller.getTop() + active.getTop(), buttonScroller.getTop());
                    final float bottom = Math.min(buttonScroller.getTop() + active.getBottom(), h);
                    if (bottom > top) {
                        ArenaMenus.activeRow(g, 0, top, w, bottom - top, true, false);
                    }
                }
                return;
            }
            if (Frost.isOn()) { //frosted navigation column: slate glass, the active entry washed with the accent and a bar on the left
                g.fillGradientRect(Frost.slate(0.90f), Frost.slate(0.70f), false, 0, 0, w, h);
                g.drawLine(1, Frost.EDGE, w, 0, w, h);
                drawHoverBand(g, Frost.HOVER, w);
                if (activeButtonIndex != -1) {
                    final MenuButton active = buttons.get(activeButtonIndex);
                    final float top = Math.max(buttonScroller.getTop() + active.getTop(), buttonScroller.getTop());
                    final float bottom = Math.min(buttonScroller.getTop() + active.getBottom(), h);
                    if (bottom > top) {
                        g.fillGradientRect(Frost.accent(0.32f), Frost.accent(0.04f), false, 0, top, w, bottom - top);
                        g.fillRect(Frost.accent(1f), 0, top, Math.max(2f, Utils.scale(3)), bottom - top);
                    }
                }
                return;
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
            drawHoverBand(g, HOVER_FILL.getColor(), w);
        }
    }

    /** Desktop: a faint band across the whole menu column behind the entry under the mouse. */
    private void drawHoverBand(Graphics g, com.badlogic.gdx.graphics.Color color, float columnWidth) {
        MenuButton pointed = btnExit.isPointedAt() ? btnExit : null;
        float top = 0, bottom = 0;
        if (pointed != null) {
            top = btnExit.getTop();
            bottom = btnExit.getBottom();
        } else {
            for (int i = 0; i < buttons.size(); i++) {
                MenuButton b = buttons.get(i);
                if (i != activeButtonIndex && b.isPointedAt()) {
                    //clipped to the visible part of the scrolling list
                    top = Math.max(buttonScroller.getTop() + b.getTop(), buttonScroller.getTop());
                    bottom = Math.min(buttonScroller.getTop() + b.getBottom(), buttonScroller.getBottom());
                    pointed = b;
                    break;
                }
            }
        }
        if (pointed != null && bottom > top) {
            if (color == null) { //the Arena look's faint gold
                ArenaMenus.activeRow(g, 0, top, columnWidth, bottom - top, true, true);
            } else {
                g.fillRect(color, 0, top, columnWidth, bottom - top);
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

    private static final FSkinColor HOVER_FILL = FSkinColor.get(Colors.CLR_TEXT).alphaColor(0.07f);

    private class MenuButton extends FButton {
        public MenuButton(String caption, FEventHandler command) {
            super(caption, command);
        }

        /** Under the mouse now (hover flags can be left over after the list scrolls under a still pointer). */
        boolean isPointedAt() {
            return isVisible() && isHovered() && screenPos.contains(Forge.mouseMovedX, Forge.mouseMovedY);
        }

        @Override
        public void draw(Graphics g) {
            if (Forge.isLandscapeMode()) {
                //draw text only for Landscape mode
                final boolean active = buttons.indexOf(this) == activeButtonIndex;
                //the row under the mouse is lit across the column (desktop); the text keeps its size, so nothing jumps
                //(the band behind it is drawn with the column, see drawHoverBand)
                final boolean hovered = !active && isPointedAt();
                if (ArenaMenus.on()) { //Arena look: gold for the active entry, cream under the pointer, indented from the bar
                    final float indent = PADDING * 4;
                    g.drawText(getText(), getFont(), ArenaMenus.entryText(active, hovered), indent, 0,
                            getWidth() - indent, getHeight(), false, Align.left, true);
                    return;
                }
                if (Frost.isOn()) { //frosted: white text, brighter for the active entry, indented from the accent bar
                    final float indent = PADDING * 3;
                    g.drawText(getText(), getFont(), active || hovered ? Frost.TEXT : Frost.TEXT_DIM, indent, 0, getWidth() - indent, getHeight(), false, Align.left, true);
                    return;
                }
                g.drawText(getText(), getFont(), getForeColor(), 0, 0, getWidth(), getHeight(), false, Align.left, true);
            }
            else { //draw buttons normally for portrait mode
                super.draw(g);
            }
        }
    }
}
