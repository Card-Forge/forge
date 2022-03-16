package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import forge.Forge;
import forge.adventure.util.Config;
import forge.adventure.util.Controls;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;

import java.util.function.Function;

/**
 * Scene to handle settings of the base forge and adventure mode
 */
public class SettingsScene extends UIScene {
    static public ForgePreferences Preference;
    Stage stage;
    Texture Background;
    private Table settingGroup;
    TextButton back;
    boolean init;

    public SettingsScene() {
        super(Forge.isLandscapeMode() ? "ui/settings_mobile.json" : "ui/settings.json");
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
        stage.getBatch().draw(Background, 0, 0, GetIntendedWidth(), GetIntendedHeight());
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
        if (GuiBase.isAndroid()||!Forge.isLandscapeMode()) {
            box.getImage().setScaling(Scaling.stretch);
            float width = !Forge.isLandscapeMode() ? 24 : 12;
            box.getImageCell().size(width, 12);
            box.getImageCell().pad(2, 2, 2, 10);
        }
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
        if (GuiBase.isAndroid()||!Forge.isLandscapeMode()) {
            box.getImage().setScaling(Scaling.stretch);
            float width = !Forge.isLandscapeMode() ? 24 : 12;
            box.getImageCell().size(width, 12);
            box.getImageCell().pad(2, 2, 2, 10);
        }
        box.setChecked(value);
        box.addListener(change);
        addLabel(name);
        settingGroup.add(box).align(Align.right);
    }

    private void addSettingField(String name, int value, ChangeListener change) {
        TextField text = Controls.newTextField(String.valueOf(value));
        text.setTextFieldFilter(new TextField.TextFieldFilter() {
            @Override
            public boolean acceptChar(TextField textField, char c) {
                return Character.isDigit(c);
            }
        });
        text.addListener(change);
        addLabel(name);
        settingGroup.add(text).align(Align.right);
    }

    void addLabel(String name) {
        Label label = new Label(name, Controls.GetSkin().get("white", Label.LabelStyle.class));
        label.setWrap(true);
        if (!Forge.isLandscapeMode()) {
            label.setFontScaleX(2);
        }
        settingGroup.row().space(5);
        settingGroup.add(label).align(Align.left).pad(2, 2, 2, 5).expand();
    }

    @Override
    public void resLoaded() {
        super.resLoaded();
        if(!this.init) {
            settingGroup = new Table();
            if (Preference == null) {
                Preference = new ForgePreferences();
            }

            SelectBox plane = Controls.newComboBox(Config.instance().getAllAdventures(), Config.instance().getSettingData().plane, new Function<Object, Void>() {
                @Override
                public Void apply(Object o) {
                    Config.instance().getSettingData().plane = (String) o;
                    Config.instance().saveSettings();
                    return null;
                }
            });
            addLabel(Forge.getLocalizer().getMessage("lblWorld"));
            if (!Forge.isLandscapeMode()) {
                plane.getStyle().listStyle.font.getData().setScale(2, 1);
            }
            settingGroup.add(plane).align(Align.right).pad(2);

            if (!GuiBase.isAndroid()) {
                SelectBox videomode = Controls.newComboBox(new String[]{"720p", "768p", "900p", "1080p"}, Config.instance().getSettingData().videomode, new Function<Object, Void>() {
                    @Override
                    public Void apply(Object o) {
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
                    }
                });
                addLabel(Forge.getLocalizer().getMessage("lblVideoMode"));
                settingGroup.add(videomode).align(Align.right).pad(2);
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
            ui.onButtonPress("return", new Runnable() {
                @Override
                public void run() {
                    SettingsScene.this.back();
                }
            });

            ScrollPane scrollPane = ui.findActor("settings");
            scrollPane.setActor(settingGroup);

            if (!Forge.isLandscapeMode()) {
                float w = Scene.GetIntendedWidth();
                float bW = w/2;
                float oX = w/2 - bW/2;
                back.getLabel().setFontScaleX(2);
                back.setWidth(bW);
                back.setHeight(20);
                float bH = back.getHeight();
                back.setX(oX);
                scrollPane.setX(w/2-scrollPane.getWidth()/2);
            }
            this.init = true;
        }
    }

    @Override
    public void create() {

    }
}
