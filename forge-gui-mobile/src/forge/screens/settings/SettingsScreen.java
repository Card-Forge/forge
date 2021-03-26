package forge.screens.settings;

import forge.Forge;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.screens.FScreen;
import forge.screens.TabPageScreen;
import forge.screens.home.HomeScreen;
import forge.util.Utils;

public class SettingsScreen extends TabPageScreen<SettingsScreen> {
    public static final FSkinFont DESC_FONT = FSkinFont.get(11);
    public static final FSkinColor DESC_COLOR = FSkinColor.get(Colors.CLR_TEXT).alphaColor(0.5f);
    public static final float SETTING_HEIGHT = Utils.AVG_FINGER_HEIGHT + Utils.scale(12);
    public static final float SETTING_PADDING = Utils.scale(5);
    private static final float INSETS_FACTOR = 0.025f;
    private static final float MAX_INSETS = SETTING_HEIGHT * 0.15f;

    private static boolean fromHomeScreen;
    private static SettingsScreen settingsScreen; //keep settings screen around so scroll positions maintained
    private final SettingsPage settingsPage;

    public static void show(boolean fromHomeScreen0) {
        if (settingsScreen == null) {
            settingsScreen = new SettingsScreen();
        }
        fromHomeScreen = fromHomeScreen0;
        Forge.openScreen(settingsScreen);
    }

    public static boolean launchedFromHomeScreen() {
        return fromHomeScreen;
    }

    public static float getInsets(float itemWidth) {
        float insets = itemWidth * INSETS_FACTOR;
        if (insets > MAX_INSETS) {
            insets = MAX_INSETS;
        }
        return insets;
    }

    public SettingsPage getSettingsPage() {
        return settingsPage;
    }

    public static SettingsScreen getSettingsScreen() {
        return settingsScreen;
    }

    @SuppressWarnings("unchecked")
    private SettingsScreen() {
        super(new TabHeader<SettingsScreen>(new TabPage[] {
                new SettingsPage(),
                new FilesPage()
        }, true) {
            @Override
            protected boolean showBackButtonInLandscapeMode() {
                return !fromHomeScreen; //don't show back button if launched from home screen
            }
        });
        settingsPage = (SettingsPage) tabPages[0];
    }

    public FScreen getLandscapeBackdropScreen() {
        if (fromHomeScreen) {
            return HomeScreen.instance;
        }
        return null;
    }

    @Override
    public void showMenu() {
        Forge.back(); //hide settings screen when menu button pressed
    }
}
