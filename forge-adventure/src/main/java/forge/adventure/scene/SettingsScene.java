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
import forge.adventure.AdventureApplicationAdapter;
import forge.adventure.util.Config;
import forge.adventure.util.Controls;
import forge.localinstance.properties.ForgePreferences;
import forge.util.Localizer;

import java.util.function.Function;

/**
 * Scene to handle settings of the base forge and adventure mode
 */
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

    @Override
    public boolean keyPressed(int keycode)
    {
        if (keycode == Input.Keys.ESCAPE)
        {
            back();
        }
        return true;
    }
    public boolean back() {
        AdventureApplicationAdapter.instance.switchToLast();
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
    private void addSettingSlider(String name,  ForgePreferences.FPref pref, int min,int max) {

        Slider slide = Controls.newSlider(min,max, 1, false);
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



        addSettingField("Fullscreen", Config.instance().getSettingData().fullScreen, new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Config.instance().getSettingData().fullScreen=((CheckBox) actor).isChecked();
                Config.instance().saveSettings();
            }
        });
        addSettingField("Screen width", Config.instance().getSettingData().width, new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String text=((TextField) actor).getText();
                Config.instance().getSettingData().width=text==null||text.isEmpty()?0:Integer.valueOf(text);
                Config.instance().saveSettings();
            }
        });
        addSettingField("Screen height", Config.instance().getSettingData().height, new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String text=((TextField) actor).getText();
                Config.instance().getSettingData().height=text==null||text.isEmpty()?0:Integer.valueOf(text);
                Config.instance().saveSettings();
            }
        });
        addCheckBox(localizer.getMessage("lblCardName"), ForgePreferences.FPref.UI_OVERLAY_CARD_NAME);
        addSettingSlider(localizer.getMessage("cbAdjustMusicVolume"),  ForgePreferences.FPref.UI_VOL_MUSIC,0,100);
        addSettingSlider(localizer.getMessage("cbAdjustSoundsVolume"),  ForgePreferences.FPref.UI_VOL_SOUNDS, 0,100);
        addCheckBox(localizer.getMessage("lblManaCost"), ForgePreferences.FPref.UI_OVERLAY_CARD_MANA_COST);
        addCheckBox(localizer.getMessage("lblPowerOrToughness"),  ForgePreferences.FPref.UI_OVERLAY_CARD_POWER);
        addCheckBox(localizer.getMessage("lblCardID"), ForgePreferences.FPref.UI_OVERLAY_CARD_ID);
        addCheckBox(localizer.getMessage("lblAbilityIcon"), ForgePreferences.FPref.UI_OVERLAY_ABILITY_ICONS);
        addCheckBox(localizer.getMessage("cbImageFetcher"), ForgePreferences.FPref.UI_ENABLE_ONLINE_IMAGE_FETCHER);


        addCheckBox(localizer.getMessage("lblBattlefieldTextureFiltering"), ForgePreferences.FPref.UI_LIBGDX_TEXTURE_FILTERING);
        addCheckBox(localizer.getMessage("lblAltZoneTabs"), ForgePreferences.FPref.UI_ALT_PLAYERZONETABS);
        addCheckBox(localizer.getMessage("lblAnimatedCardTapUntap"), ForgePreferences.FPref.UI_ANIMATED_CARD_TAPUNTAP);
        addCheckBox(localizer.getMessage("lblBorderMaskOption"), ForgePreferences.FPref.UI_ENABLE_BORDER_MASKING);
        addCheckBox(localizer.getMessage("lblPreloadExtendedArtCards"), ForgePreferences.FPref.UI_ENABLE_PRELOAD_EXTENDED_ART);
        addCheckBox(localizer.getMessage("lblAutoCacheSize"), ForgePreferences.FPref.UI_AUTO_CACHE_SIZE);
        addCheckBox(localizer.getMessage("lblDisposeTextures"), ForgePreferences.FPref.UI_ENABLE_DISPOSE_TEXTURES);
        addInputField(localizer.getMessage("lblDisposeTextures"), ForgePreferences.FPref.UI_LANGUAGE);


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
