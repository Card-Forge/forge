package forge.screens.settings;

import java.util.ArrayList;
import java.util.List;

import forge.assets.FSkin;
import forge.screens.FScreen;
import forge.toolbox.FComboBox;
import forge.toolbox.FContainer;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;

public class SettingsScreen extends FScreen {
    private static final float INSETS_FACTOR = 0.025f;
    private static final float GAP_Y_FACTOR = 0.01f;

    private final FScrollPane scroller = add(new FScrollPane());
    private final List<SettingPanel> settingPanels = new ArrayList<SettingPanel>();

    public SettingsScreen() {
        super(true, "Settings", false);

        addPanel(new ComboBoxPanel<String>("Theme:", FSkin.getAllSkins(), FSkin.getName()));
    }

    private void addPanel(SettingPanel panel) {
        scroller.add(panel);
        settingPanels.add(panel);
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        float dy = height * GAP_Y_FACTOR;
        scroller.setBounds(0, startY + dy, width, height - startY - dy);

        float x = width * INSETS_FACTOR;
        float y = 0;
        float panelWidth = width - 2 * x;
        float panelHeight;

        for (SettingPanel panel : settingPanels) {
            panelHeight = panel.getPreferredHeight();
            panel.setBounds(x, y, panelWidth, panelHeight);
            y += panelHeight + dy;
        }
    }

    private abstract class SettingPanel extends FContainer {
        public abstract float getPreferredHeight();
    }

    public class ComboBoxPanel<E> extends SettingPanel {
        private final FLabel label;
        private final FComboBox<E> comboBox;

        public ComboBoxPanel(String labelText, E[] items, E selectedItem) {
            this(labelText, new FComboBox<E>(items), selectedItem);
        }
        public ComboBoxPanel(String labelText, Iterable<E> items, E selectedItem) {
            this(labelText, new FComboBox<E>(items), selectedItem);
        }

        private ComboBoxPanel(String labelText, FComboBox<E> comboBox0, E selectedItem) {
            label = add(new FLabel.Builder().text(labelText).build());
            comboBox = add(comboBox0);
            label.setHeight(FComboBox.PREFERRED_HEIGHT - 6);
            comboBox.setHeight(FComboBox.PREFERRED_HEIGHT);
            comboBox.setSelectedItem(selectedItem);
        }

        @Override
        protected void doLayout(float width, float height) {
            label.setBounds(0, 0, width, label.getHeight());
            comboBox.setBounds(0, label.getHeight(), width, comboBox.getHeight());
        }

        @Override
        public float getPreferredHeight() {
            return label.getHeight() + comboBox.getHeight();
        }
    }
}
