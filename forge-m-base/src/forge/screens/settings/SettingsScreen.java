package forge.screens.settings;

import forge.assets.FSkin;
import forge.screens.FScreen;
import forge.toolbox.FComboBox;

public class SettingsScreen extends FScreen {
    private FComboBox<String> cmbTheme;

    public SettingsScreen() {
        super(true, "Settings", true);
        
        cmbTheme = new FComboBox<>(FSkin.getAllSkins());
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        cmbTheme.setBounds(20, 20, width, 25);
    }
}
