package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import forge.adventure.AdventureApplicationAdapter;
import forge.adventure.util.Controls;
import forge.localinstance.properties.ForgePreferences;
import forge.util.Localizer;


public class SettingsScene extends  Scene {


    static public ForgePreferences Preference;
    private VerticalGroup settingGroup;
    private VerticalGroup nameGroup;

    @Override
    public void dispose() {
        if(Stage!=null)
            Stage.dispose();
    }
    Texture Background;
    @Override
    public void render() {


        Gdx.gl.glClearColor(1,0,1,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Stage.getBatch().begin();
        Stage.getBatch().disableBlending();
        Stage.getBatch().draw(Background,0,0,IntendedWidth,IntendedHeight);
        Stage.getBatch().enableBlending();
        Stage.getBatch().end();
        Stage.act(Gdx.graphics.getDeltaTime());
        Stage.draw();

    }

    public boolean Back()
    {
        AdventureApplicationAdapter.CurrentAdapter.SwitchScene(AdventureApplicationAdapter.CurrentAdapter.GetLastScene());
        return true;
    }
    private void AddSettingButton(String name, Class type, ForgePreferences.FPref pref, Object[] para)
    {


        Actor control;
        if (boolean.class.equals(type))
        {
            CheckBox box = Controls.newCheckBox("");
            control =box;
            box.setChecked(Preference.getPrefBoolean(pref));
            control.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor)
                {
                    Preference.setPref(pref, ((CheckBox)actor).isChecked());
                    Preference.save();
                }
            });

        }
        else if (int.class.equals(type))
        {
            Slider slide = Controls.newSlider((int)para[0],(int)para[1],1,false);
            control=slide;
            slide.setValue(Preference.getPrefInt(pref));
            slide.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor)
                {
                    Preference.setPref(pref, String.valueOf((int)((Slider)actor).getValue()));
                    Preference.save();
                }
            });

        }
        else
        {
            control=Controls.newLabel("");

        }
        control.setHeight(40);
        Label label=Controls.newLabel(name);
        nameGroup.addActor(label);
        settingGroup.addActor(control);
    }
    @Override
    public void ResLoaded()
    {
        Stage = new Stage(new StretchViewport(IntendedWidth,IntendedHeight));
        Background = new Texture( forge.adventure.AdventureApplicationAdapter.CurrentAdapter.GetRes().GetFile("img/title_bg.png"));
        settingGroup=new VerticalGroup();
        nameGroup =new VerticalGroup();
        if(Preference==null)
        {
            Preference = new ForgePreferences();
        }
        Localizer localizer = Localizer.getInstance();

        AddSettingButton("Enable Music", boolean.class,ForgePreferences.FPref.UI_ENABLE_MUSIC, new Object[]{});
        AddSettingButton(localizer.getMessage("lblCardName"), boolean.class,ForgePreferences.FPref.UI_OVERLAY_CARD_NAME, new Object[]{});
        AddSettingButton(localizer.getMessage("cbAdjustMusicVolume"), int.class,ForgePreferences.FPref.UI_VOL_MUSIC, new Object[]{0,100});
        AddSettingButton(localizer.getMessage("cbAdjustSoundsVolume"), int.class,ForgePreferences.FPref.UI_VOL_SOUNDS, new Object[]{0,100});
        AddSettingButton(localizer.getMessage("lblManaCost"), boolean.class,ForgePreferences.FPref.UI_OVERLAY_CARD_MANA_COST, new Object[]{});
        AddSettingButton(localizer.getMessage("lblPowerOrToughness"), boolean.class,ForgePreferences.FPref.UI_OVERLAY_CARD_POWER, new Object[]{});
        AddSettingButton(localizer.getMessage("lblCardID"), boolean.class,ForgePreferences.FPref.UI_OVERLAY_CARD_ID, new Object[]{});
        AddSettingButton(localizer.getMessage("lblAbilityIcon"), boolean.class,ForgePreferences.FPref.UI_OVERLAY_ABILITY_ICONS, new Object[]{});



        //settingGroup.pack();
       // nameGroup.pack();
        nameGroup.setPosition(130,600);
        settingGroup.setPosition(500,600);

        nameGroup.addActor(Controls.newTextButton("Return",()->Back()));

        Stage.addActor(settingGroup);
        Stage.addActor(nameGroup);
    }
    @Override
    public void create() {

    }
}
