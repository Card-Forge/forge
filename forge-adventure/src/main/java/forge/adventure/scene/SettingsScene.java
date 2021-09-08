package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import forge.adventure.AdventureApplicationAdapter;
import forge.adventure.util.Config;
import forge.adventure.util.Controls;
import forge.localinstance.properties.ForgePreferences;
import forge.util.Localizer;

import java.util.function.Function;


public class SettingsScene extends UIScene {


    static public ForgePreferences Preference;
    Stage stage;
    Texture Background;
    private Table settingGroup;

    public SettingsScene() {
        super("ui/settings.json");
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

    public boolean back() {
        AdventureApplicationAdapter.instance.switchToLast();
        return true;
    }

    private void addSettingButton(String name, Class type, ForgePreferences.FPref pref, Object[] para) {


        Actor control;
        if (boolean.class.equals(type)) {
            CheckBox box = Controls.newCheckBox("");
            control = box;
            box.setChecked(Preference.getPrefBoolean(pref));
            control.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    Preference.setPref(pref, ((CheckBox) actor).isChecked());
                    Preference.save();
                }
            });

        } else if (int.class.equals(type)) {
            Slider slide = Controls.newSlider((int) para[0], (int) para[1], 1, false);
            control = slide;
            slide.setValue(Preference.getPrefInt(pref));
            slide.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    Preference.setPref(pref, String.valueOf((int) ((Slider) actor).getValue()));
                    Preference.save();
                }
            });

        } else if (int.class.equals(type)) {
            TextField text = Controls.newTextField(Preference.getPref(pref));
            control = text;
            text.setTextFieldFilter(new TextField.TextFieldFilter() {
                @Override
                public boolean acceptChar(TextField textField, char c) {
                    return Character.isDigit(c);
                }
            });
            text.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    Preference.setPref(pref, String.valueOf((int) ((Slider) actor).getValue()));
                    Preference.save();
                }
            });

        } else {
            control = Controls.newLabel("");

        }
        addLabel(name);
        settingGroup.add(control).align(Align.right);
    }
    private void addSettingButton(String name,Object value, ChangeListener change) {

        Class type = value.getClass();
        Actor control;
        if (Boolean.class.equals(type)) {
            CheckBox box = Controls.newCheckBox("");
            control = box;
            box.setChecked((Boolean) value);
            control.addListener(change);

        } else if (Integer.class.equals(type)) {
            TextField text = Controls.newTextField((String) value.toString());
            control = text;
            text.setTextFieldFilter(new TextField.TextFieldFilter() {
                @Override
                public boolean acceptChar(TextField textField, char c) {
                    return Character.isDigit(c);
                }
            });
            text.addListener(change);

        } else {
            control = Controls.newLabel("");

        }
        addLabel(name);
        settingGroup.add(control).align(Align.right);
    }
    void addLabel( String name)
    {

        Label label = new Label(name, Controls.GetSkin().get("white",Label.LabelStyle.class));

        settingGroup.row().space(5);
        settingGroup.add(label).align(Align.left).fillX();
    }
    @Override
    public void resLoaded() {
        super.resLoaded();
        settingGroup = new Table();
        if (Preference == null) {
            Preference = new ForgePreferences();
        }
        Localizer localizer = Localizer.getInstance();

        SelectBox plane = Controls.newComboBox(Config.instance().getAllAdventures(), Config.instance().getSettingData().plane, new Function<Object, Void>() {
            @Override
            public Void apply(Object o) {
                Config.instance().getSettingData().plane= (String) o;
                Config.instance().saveSettings();
                return null;
            }
        });
        addLabel("Plane");
        settingGroup.add(plane).align(Align.right);



        addSettingButton("Fullscreen", Config.instance().getSettingData().fullScreen, new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Config.instance().getSettingData().fullScreen=((CheckBox) actor).isChecked();
                Config.instance().saveSettings();
            }
        });
        addSettingButton("Screen width", Config.instance().getSettingData().width, new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String text=((TextField) actor).getText();
                Config.instance().getSettingData().width=text==null||text.isEmpty()?0:Integer.valueOf(text);
                Config.instance().saveSettings();
            }
        });
        addSettingButton("Screen height", Config.instance().getSettingData().height, new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String text=((TextField) actor).getText();
                Config.instance().getSettingData().height=text==null||text.isEmpty()?0:Integer.valueOf(text);
                Config.instance().saveSettings();
            }
        });
        addSettingButton(localizer.getMessage("lblCardName"), boolean.class, ForgePreferences.FPref.UI_OVERLAY_CARD_NAME, new Object[]{});
        addSettingButton(localizer.getMessage("cbAdjustMusicVolume"), int.class, ForgePreferences.FPref.UI_VOL_MUSIC, new Object[]{0, 100});
        addSettingButton(localizer.getMessage("cbAdjustSoundsVolume"), int.class, ForgePreferences.FPref.UI_VOL_SOUNDS, new Object[]{0, 100});
        addSettingButton(localizer.getMessage("lblManaCost"), boolean.class, ForgePreferences.FPref.UI_OVERLAY_CARD_MANA_COST, new Object[]{});
        addSettingButton(localizer.getMessage("lblPowerOrToughness"), boolean.class, ForgePreferences.FPref.UI_OVERLAY_CARD_POWER, new Object[]{});
        addSettingButton(localizer.getMessage("lblCardID"), boolean.class, ForgePreferences.FPref.UI_OVERLAY_CARD_ID, new Object[]{});
        addSettingButton(localizer.getMessage("lblAbilityIcon"), boolean.class, ForgePreferences.FPref.UI_OVERLAY_ABILITY_ICONS, new Object[]{});
        addSettingButton(localizer.getMessage("cbImageFetcher"), boolean.class, ForgePreferences.FPref.UI_ENABLE_ONLINE_IMAGE_FETCHER, new Object[]{});


        settingGroup.row();
        ui.onButtonPress("return", () -> back());

        ScrollPane scrollPane = ui.findActor("settings");
        scrollPane.setActor(settingGroup);
    }

    @Override
    public void create() {

    }

    enum ControlTypes {
        CheckBox,
        Slider,
        Resolution
    }
}
