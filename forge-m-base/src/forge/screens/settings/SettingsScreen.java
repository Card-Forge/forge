package forge.screens.settings;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge;
import forge.Forge.Graphics;
import forge.assets.FSkin;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinColor.Colors;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.toolbox.FList;
import forge.utils.ForgePreferences.FPref;
import forge.utils.Utils;

public class SettingsScreen extends FScreen {
    private static final float INSETS_FACTOR = 0.025f;
    private static final float GAP_Y_FACTOR = 0.01f;
    private static final FSkinFont DESC_FONT = FSkinFont.get(11);
    private static final FSkinColor DESC_COLOR = FSkinColor.get(Colors.CLR_TEXT).alphaColor(0.5f);

    private final FList<Setting> lstSettings = add(new FList<Setting>());

    public SettingsScreen() {
        super(true, "Settings", false);
        lstSettings.setListItemRenderer(new SettingRenderer());

        lstSettings.addGroup("General Settings");
        lstSettings.addGroup("Gameplay Options");
        lstSettings.addGroup("Random Deck Generation");
        lstSettings.addGroup("Advanced Settings");
        lstSettings.addGroup("Graphic Options");
        lstSettings.addGroup("Sound Options");

        lstSettings.addItem(new CustomSelectSetting(FPref.UI_SKIN, "Theme",
                "Sets the theme that determines how display components are skinned.",
                FSkin.getAllSkins()) {
            @Override
            public void valueChanged(String newValue) {
                FSkin.changeSkin(newValue);
            }
        }, 0);
        lstSettings.addItem(new BooleanSetting(FPref.DEV_MODE_ENABLED, "Developer Mode",
                "Enables menu with functions for testing during development."), 3);
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        float dy = height * GAP_Y_FACTOR;
        lstSettings.setBounds(0, startY + dy, width, height - startY - dy);
    }

    private abstract class Setting {
        protected String label;
        protected String description;
        protected FPref pref;

        public Setting(FPref pref0, String label0, String description0) {
            label = label0;
            description = description0;
            pref = pref0;
        }

        public abstract void select();
        public abstract void drawPrefValue(Graphics g, FSkinFont font, FSkinColor color, float x, float y, float width, float height);
    }

    private class BooleanSetting extends Setting {
        public BooleanSetting(FPref pref0, String label0, String description0) {
            super(pref0, label0, description0);
        }

        @Override
        public void select() {
            FModel.getPreferences().setPref(pref, !FModel.getPreferences().getPrefBoolean(pref));
        }

        @Override
        public void drawPrefValue(Graphics g, FSkinFont font, FSkinColor color, float x, float y, float w, float h) {
            x += w - h;
            w = h;
            g.drawRect(1, DESC_COLOR, x, y, w, h);
            if (FModel.getPreferences().getPrefBoolean(pref)) {
                //draw check mark
                x += 3;
                y++;
                w -= 6;
                h -= 3;
                g.drawLine(2, color, x, y + h / 2, x + w / 2, y + h);
                g.drawLine(2, color, x + w / 2, y + h, x + w, y);
            }
        }
    }

    private class CustomSelectSetting extends Setting {
        private final List<String> options = new ArrayList<String>();

        public CustomSelectSetting(FPref pref0, String label0, String description0, String[] options0) {
            super(pref0, label0 + ":", description0);

            for (String option : options0) {
                options.add(option);
            }
        }
        public CustomSelectSetting(FPref pref0, String label0, String description0, Iterable<String> options0) {
            super(pref0, label0 + ":", description0);

            for (String option : options0) {
                options.add(option);
            }
        }

        public void valueChanged(String newValue) {
            FModel.getPreferences().setPref(pref, newValue);
            FModel.getPreferences().save();
        }

        @Override
        public void select() {
            Forge.openScreen(new CustomSelectScreen());
        }

        private class CustomSelectScreen extends FScreen {
            private final FList<String> lstOptions;
            private final String currentValue = FModel.getPreferences().getPref(pref);

            private CustomSelectScreen() {
                super(true, "Select " + label.substring(0, label.length() - 1), false);
                lstOptions = add(new FList<String>(options));
                lstOptions.setListItemRenderer(new FList.DefaultListItemRenderer<String>() {
                    @Override
                    public boolean tap(String value, float x, float y, int count) {
                        if (!value.equals(currentValue)) {
                            valueChanged(value);
                        }
                        Forge.back();
                        return true;
                    }

                    @Override
                    public void drawValue(Graphics g, String value, FSkinFont font, FSkinColor foreColor, float width, float height) {
                        float x = width * INSETS_FACTOR;
                        float y = 0;
                        width -= 2 * x;

                        g.drawText(value, font, foreColor, x, y, width, height, false, HAlignment.LEFT, true);

                        float radius = height / 5;
                        x += width - radius;
                        y = height / 2;
                        g.drawCircle(1, DESC_COLOR, x, y, radius);
                        if (value.equals(currentValue)) {
                            g.fillCircle(foreColor, x, y, radius / 2);
                        }
                    }
                });
            }

            @Override
            protected void doLayout(float startY, float width, float height) {
                float dy = height * GAP_Y_FACTOR;
                lstOptions.setBounds(0, startY + dy, width, height - startY - dy);
            }
        }

        @Override
        public void drawPrefValue(Graphics g, FSkinFont font, FSkinColor color, float x, float y, float w, float h) {
            g.drawText(FModel.getPreferences().getPref(pref), font, color, x, y, w, h, false, HAlignment.RIGHT, false);
        }
    }

    private class SettingRenderer extends FList.ListItemRenderer<Setting> {
        @Override
        public float getItemHeight() {
            return Utils.AVG_FINGER_HEIGHT + 12;
        }

        @Override
        public boolean tap(Setting value, float x, float y, int count) {
            value.select();
            return true;
        }

        @Override
        public void drawValue(Graphics g, Setting value, FSkinFont font, FSkinColor color, float width, float height) {
            float x = width * INSETS_FACTOR;
            float y = x;
            float w = width - 2 * x;
            float h = font.getFont().getMultiLineBounds(value.label).height + 5;

            g.drawText(value.label, font, color, x, y, w, h, false, HAlignment.LEFT, false);
            value.drawPrefValue(g, font, color, x, y, w, h);
            h += 5;
            g.drawText(value.description, DESC_FONT, DESC_COLOR, x, y + h, w, height - h - y, true, HAlignment.LEFT, false);            
        }
    }
}
