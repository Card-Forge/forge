package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.adventure.util.Config;
import forge.adventure.util.Controls;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;

/**
 * Scene to handle settings of the base forge and adventure mode
 */
public class SettingsScene extends UIScene {
    static public ForgePreferences Preference;
    Stage stage;
    Texture Background;
    private Table settingGroup;
    TextButton back;

    public SettingsScene() {
        super(Forge.isLandscapeMode() ? "ui/settings.json" : "ui/settings_portrait.json");
    }


    @Override
    public void dispose() {
        if (stage != null)
            stage.dispose();
    }

    public void renderAct(float delta) {
        Gdx.gl.glClearColor(1, 0, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.getBatch().begin();
        stage.getBatch().disableBlending();
        stage.getBatch().draw(Background, 0, 0, getIntendedWidth(), getIntendedHeight());
        stage.getBatch().enableBlending();
        stage.getBatch().end();
        stage.act(delta);
        stage.draw();
    }

    @Override
    public boolean keyPressed(int keycode) {
        if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
            back();
        }
        return true;
    }

    public boolean back() {
        Forge.switchToLast();
        return true;
    }

    private void addInputField(String name, ForgePreferences.FPref pref) {
        TextField box = Controls.newTextField("");
        box.setText(Preference.getPref(pref));
        box.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Preference.setPref(pref, ((TextField) actor).getText());
                Preference.save();
            }
        });

        addLabel(name);
        settingGroup.add(box).align(Align.right);
    }

    private void addCheckBox(String name, ForgePreferences.FPref pref) {
        CheckBox box = Controls.newCheckBox("");
        box.setChecked(Preference.getPrefBoolean(pref));
        box.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Preference.setPref(pref, ((CheckBox) actor).isChecked());
                Preference.save();
            }
        });

        addLabel(name);
        settingGroup.add(box).align(Align.right);
    }

    private void addSettingSlider(String name, ForgePreferences.FPref pref, int min, int max) {
        Slider slide = Controls.newSlider(min, max, 1, false);
        slide.setValue(Preference.getPrefInt(pref));
        slide.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Preference.setPref(pref, String.valueOf((int) ((Slider) actor).getValue()));
                Preference.save();
            }
        });
        addLabel(name);
        settingGroup.add(slide).align(Align.right);
    }

    private void addSettingField(String name, boolean value, ChangeListener change) {
        CheckBox box = Controls.newCheckBox("");
        box.setChecked(value);
        box.addListener(change);
        addLabel(name);
        settingGroup.add(box).align(Align.right);
    }

    private void addSettingField(String name, int value, ChangeListener change) {
        TextField text = Controls.newTextField(String.valueOf(value));
        text.setTextFieldFilter((textField, c) -> Character.isDigit(c));
        text.addListener(change);
        addLabel(name);
        settingGroup.add(text).align(Align.right);
    }

    void addLabel(String name) {
        Label label = Controls.newLabel(name);
        label.setWrap(true);
        settingGroup.row().space(5);
        int w = Forge.isLandscapeMode() ? 160 : 80;
        settingGroup.add(label).align(Align.left).pad(2, 2, 2, 5).width(w).expand();
    }

    @Override
    public void resLoaded() {
        super.resLoaded();
        settingGroup = new Table();
        if (Preference == null) {
            Preference = new ForgePreferences();
        }

        SelectBox plane = Controls.newComboBox(Config.instance().getAllAdventures(), Config.instance().getSettingData().plane, o -> {
            Config.instance().getSettingData().plane = (String) o;
            Config.instance().saveSettings();
            return null;
        });
        addLabel(Forge.getLocalizer().getMessage("lblWorld"));
        settingGroup.add(plane).align(Align.right).pad(2);

        if (!GuiBase.isAndroid()) {
            SelectBox videomode = Controls.newComboBox(new String[]{"720p", "768p", "900p", "1080p"}, Config.instance().getSettingData().videomode, o -> {
                String mode = (String) o;
                if (mode == null)
                    mode = "720p";
                Config.instance().getSettingData().videomode = mode;
                if (mode.equalsIgnoreCase("768p")) {
                    Config.instance().getSettingData().width = 1366;
                    Config.instance().getSettingData().height = 768;
                } else if (mode.equalsIgnoreCase("900p")) {
                    Config.instance().getSettingData().width = 1600;
                    Config.instance().getSettingData().height = 900;
                } else if (mode.equalsIgnoreCase("1080p")) {
                    Config.instance().getSettingData().width = 1920;
                    Config.instance().getSettingData().height = 1080;
                } else {
                    Config.instance().getSettingData().width = 1280;
                    Config.instance().getSettingData().height = 720;
                }
                Config.instance().saveSettings();
                //update preference for classic mode if needed
                if (FModel.getPreferences().getPref(ForgePreferences.FPref.UI_VIDEO_MODE) != mode) {
                    FModel.getPreferences().setPref(ForgePreferences.FPref.UI_VIDEO_MODE, mode);
                    FModel.getPreferences().save();
                }
                return null;
            });
            addLabel(Forge.getLocalizer().getMessage("lblVideoMode"));
            settingGroup.add(videomode).align(Align.right).pad(2);
        }
        if (Forge.isLandscapeMode()) {
            //different adjustment to landscape
            SelectBox rewardCardAdjLandscape = Controls.newComboBox(new Float[]{0.6f, 0.65f, 0.7f, 0.75f, 0.8f, 0.85f, 0.9f, 1f, 1.05f, 1.1f, 1.15f, 1.2f, 1.25f, 1.3f, 1.35f, 1.4f, 1.45f, 1.5f, 1.55f, 1.6f}, Config.instance().getSettingData().rewardCardAdjLandscape, o -> {
                Float val = (Float) o;
                if (val == null || val == 0f)
                    val = 1f;
                Config.instance().getSettingData().rewardCardAdjLandscape = val;
                Config.instance().saveSettings();
                return null;
            });
            addLabel("Reward/Shop Card Display Ratio");
            settingGroup.add(rewardCardAdjLandscape).align(Align.right).pad(2);
            SelectBox tooltipAdjLandscape = Controls.newComboBox(new Float[]{0.6f, 0.65f, 0.7f, 0.75f, 0.8f, 0.85f, 0.9f, 1f, 1.05f, 1.1f, 1.15f, 1.2f, 1.25f, 1.3f, 1.35f, 1.4f, 1.45f, 1.5f, 1.55f, 1.6f}, Config.instance().getSettingData().cardTooltipAdjLandscape, o -> {
                Float val = (Float) o;
                if (val == null || val == 0f)
                    val = 1f;
                Config.instance().getSettingData().cardTooltipAdjLandscape = val;
                Config.instance().saveSettings();
                return null;
            });
            addLabel("Reward/Shop Card Tooltip Ratio");
            settingGroup.add(tooltipAdjLandscape).align(Align.right).pad(2);
        } else {
            //portrait adjustment
            SelectBox rewardCardAdj = Controls.newComboBox(new Float[]{0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f, 1.6f, 1.8f, 1.9f, 2f}, Config.instance().getSettingData().rewardCardAdj, o -> {
                Float val = (Float) o;
                if (val == null || val == 0f)
                    val = 1f;
                Config.instance().getSettingData().rewardCardAdj = val;
                Config.instance().saveSettings();
                return null;
            });
            addLabel("Reward/Shop Card Display Ratio");
            settingGroup.add(rewardCardAdj).align(Align.right).pad(2);
            SelectBox tooltipAdj = Controls.newComboBox(new Float[]{0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f, 1.6f, 1.8f, 1.9f, 2f}, Config.instance().getSettingData().cardTooltipAdj, o -> {
                Float val = (Float) o;
                if (val == null || val == 0f)
                    val = 1f;
                Config.instance().getSettingData().cardTooltipAdj = val;
                Config.instance().saveSettings();
                return null;
            });
            addLabel("Reward/Shop Card Tooltip Ratio");
            settingGroup.add(tooltipAdj).align(Align.right).pad(2);
        }
        if (!GuiBase.isAndroid()) {
            addSettingField(Forge.getLocalizer().getMessage("lblFullScreen"), Config.instance().getSettingData().fullScreen, new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    boolean value = ((CheckBox) actor).isChecked();
                    Config.instance().getSettingData().fullScreen = value;
                    Config.instance().saveSettings();
                    //update
                    if (FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_FULLSCREEN_MODE) != value) {
                        FModel.getPreferences().setPref(ForgePreferences.FPref.UI_LANDSCAPE_MODE, value);
                        FModel.getPreferences().save();
                    }
                }
            });
        }
        addCheckBox(Forge.getLocalizer().getMessage("lblCardName"), ForgePreferences.FPref.UI_OVERLAY_CARD_NAME);
        addSettingSlider(Forge.getLocalizer().getMessage("cbAdjustMusicVolume"), ForgePreferences.FPref.UI_VOL_MUSIC, 0, 100);
        addSettingSlider(Forge.getLocalizer().getMessage("cbAdjustSoundsVolume"), ForgePreferences.FPref.UI_VOL_SOUNDS, 0, 100);
        addCheckBox(Forge.getLocalizer().getMessage("lblManaCost"), ForgePreferences.FPref.UI_OVERLAY_CARD_MANA_COST);
        addCheckBox(Forge.getLocalizer().getMessage("lblPowerOrToughness"), ForgePreferences.FPref.UI_OVERLAY_CARD_POWER);
        addCheckBox(Forge.getLocalizer().getMessage("lblCardID"), ForgePreferences.FPref.UI_OVERLAY_CARD_ID);
        addCheckBox(Forge.getLocalizer().getMessage("lblAbilityIcon"), ForgePreferences.FPref.UI_OVERLAY_ABILITY_ICONS);
        addCheckBox(Forge.getLocalizer().getMessage("cbImageFetcher"), ForgePreferences.FPref.UI_ENABLE_ONLINE_IMAGE_FETCHER);


        if (!GuiBase.isAndroid()) {
            addCheckBox(Forge.getLocalizer().getMessage("lblBattlefieldTextureFiltering"), ForgePreferences.FPref.UI_LIBGDX_TEXTURE_FILTERING);
            addCheckBox(Forge.getLocalizer().getMessage("lblAltZoneTabs"), ForgePreferences.FPref.UI_ALT_PLAYERZONETABS);
        }

        addCheckBox(Forge.getLocalizer().getMessage("lblLandscapeMode"), ForgePreferences.FPref.UI_LANDSCAPE_MODE);
        addCheckBox(Forge.getLocalizer().getMessage("lblAnimatedCardTapUntap"), ForgePreferences.FPref.UI_ANIMATED_CARD_TAPUNTAP);
        if (!GuiBase.isAndroid()) {
            addCheckBox(Forge.getLocalizer().getMessage("lblBorderMaskOption"), ForgePreferences.FPref.UI_ENABLE_BORDER_MASKING);
            addCheckBox(Forge.getLocalizer().getMessage("lblPreloadExtendedArtCards"), ForgePreferences.FPref.UI_ENABLE_PRELOAD_EXTENDED_ART);
            addCheckBox(Forge.getLocalizer().getMessage("lblAutoCacheSize"), ForgePreferences.FPref.UI_AUTO_CACHE_SIZE);
            addCheckBox(Forge.getLocalizer().getMessage("lblDisposeTextures"), ForgePreferences.FPref.UI_ENABLE_DISPOSE_TEXTURES);
            //addInputField(Forge.getLocalizer().getMessage("lblDisposeTextures"), ForgePreferences.FPref.UI_LANGUAGE);
        }


        settingGroup.row();
        back = ui.findActor("return");
        back.getLabel().setText(Forge.getLocalizer().getMessage("lblBack"));
        ui.onButtonPress("return", () -> SettingsScene.this.back());

        ScrollPane scrollPane = ui.findActor("settings");
        scrollPane.setActor(settingGroup);

    }

    @Override
    public void create() {

    }
}
