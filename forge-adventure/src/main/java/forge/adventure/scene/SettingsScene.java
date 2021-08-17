package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import forge.adventure.AdventureApplicationAdapter;
import forge.adventure.util.Controls;
import forge.localinstance.properties.ForgePreferences;
import forge.util.Localizer;


public class SettingsScene extends Scene {


    static public ForgePreferences Preference;
    Stage stage;
    Texture Background;
    private Table settingGroup;
    private ScrollPane scrollPane;

    @Override
    public void Enter() {
        Gdx.input.setInputProcessor(stage); //Start taking input from the ui
    }

    @Override
    public void dispose() {
        if (stage != null)
            stage.dispose();
    }

    @Override
    public void render() {


        Gdx.gl.glClearColor(1, 0, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.getBatch().begin();
        stage.getBatch().disableBlending();
        stage.getBatch().draw(Background, 0, 0, GetIntendedWidth(), GetIntendedHeight());
        stage.getBatch().enableBlending();
        stage.getBatch().end();
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();

    }

    public boolean Back() {
        AdventureApplicationAdapter.CurrentAdapter.SwitchScene(AdventureApplicationAdapter.CurrentAdapter.GetLastScene());
        return true;
    }

    private void AddSettingButton(String name, Class type, ForgePreferences.FPref pref, Object[] para) {


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
        control.setHeight(40);
        Label label = Controls.newLabel(name);
        //nameGroup.addActor(label);
        settingGroup.row();
        settingGroup.add(label).align(Align.left);
        settingGroup.add(control);
    }

    @Override
    public void ResLoaded() {
        stage = new Stage(new StretchViewport(GetIntendedWidth(), GetIntendedHeight()));
        Background = new Texture(forge.adventure.AdventureApplicationAdapter.CurrentAdapter.GetRes().GetFile("ui/title_bg.png"));
        settingGroup = new Table();
        if (Preference == null) {
            Preference = new ForgePreferences();
        }
        Localizer localizer = Localizer.getInstance();

        AddSettingButton(localizer.getMessage("lblCardName"), boolean.class, ForgePreferences.FPref.UI_OVERLAY_CARD_NAME, new Object[]{});
        AddSettingButton(localizer.getMessage("cbAdjustMusicVolume"), int.class, ForgePreferences.FPref.UI_VOL_MUSIC, new Object[]{0, 100});
        AddSettingButton(localizer.getMessage("cbAdjustSoundsVolume"), int.class, ForgePreferences.FPref.UI_VOL_SOUNDS, new Object[]{0, 100});
        AddSettingButton(localizer.getMessage("lblManaCost"), boolean.class, ForgePreferences.FPref.UI_OVERLAY_CARD_MANA_COST, new Object[]{});
        AddSettingButton(localizer.getMessage("lblPowerOrToughness"), boolean.class, ForgePreferences.FPref.UI_OVERLAY_CARD_POWER, new Object[]{});
        AddSettingButton(localizer.getMessage("lblCardID"), boolean.class, ForgePreferences.FPref.UI_OVERLAY_CARD_ID, new Object[]{});
        AddSettingButton(localizer.getMessage("lblAbilityIcon"), boolean.class, ForgePreferences.FPref.UI_OVERLAY_ABILITY_ICONS, new Object[]{});
        AddSettingButton(localizer.getMessage("nlImageFetcher"), boolean.class, ForgePreferences.FPref.UI_ENABLE_ONLINE_IMAGE_FETCHER, new Object[]{});


        settingGroup.row();
        settingGroup.add(Controls.newTextButton("Return", () -> Back()));

        scrollPane = new ScrollPane(settingGroup, Controls.GetSkin());
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setFillParent(true);
        stage.addActor(scrollPane);
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
