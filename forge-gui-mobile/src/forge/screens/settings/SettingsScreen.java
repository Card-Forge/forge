package forge.screens.settings;

import forge.Forge;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinColor.Colors;
import forge.screens.FScreen;
import forge.util.Utils;

public class SettingsScreen extends FScreen {
    public static final float INSETS_FACTOR = 0.025f;
    public static final FSkinFont DESC_FONT = FSkinFont.get(11);
    public static final FSkinColor DESC_COLOR = FSkinColor.get(Colors.CLR_TEXT).alphaColor(0.5f);
    protected static final float SETTING_HEIGHT = Utils.AVG_FINGER_HEIGHT + Utils.scaleY(12);
    protected static final float SETTING_PADDING = Utils.scaleY(5);

    private final SettingsPage page = add(new SettingsPage());
    public SettingsScreen() {
        super("Settings");
        /*super(new TabPage[] {
                new SettingsPage(),
                new FilesPage(),
                new HelpPage()
        });*/
    }

    @Override
    public void showMenu() {
        Forge.back(); //hide settings screen when menu button pressed
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        page.setBounds(0, startY, width, height - startY);
    }
}
