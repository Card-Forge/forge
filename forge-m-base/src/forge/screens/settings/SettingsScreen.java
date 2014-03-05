package forge.screens.settings;

import forge.screens.FScreen;
import forge.toolbox.FList;

public class SettingsScreen extends FScreen {
    private static final float GAP_Y_FACTOR = 0.01f;

    private final FList<Object> lstSettings = add(new FList<Object>());

    public SettingsScreen() {
        super(true, "Settings", false);

        lstSettings.addItem("Theme");
        lstSettings.addItem("Theme4");
        lstSettings.addItem("Theme8");
        //addSetting(new ComboBoxPanel<String>("Theme:", FSkin.getAllSkins(), FSkin.getName()));
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        float dy = height * GAP_Y_FACTOR;
        lstSettings.setBounds(0, startY + dy, width, height - startY - dy);
    }
}
