package forge.adventure.util;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import java.util.concurrent.Callable;
import java.util.function.Function;

public class Controls {
    private static Skin SelectedSkin = null;

    static public TextButton newTextButton(String text) {
        TextButton ret = new TextButton(text, GetSkin());

        return ret;
    }

    static public SelectBox newComboBox(Object[] text, Object item, Function<Object, Void> func) {

        SelectBox ret = new SelectBox(GetSkin());
        ret.setItems(text);
        ret.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    func.apply(((SelectBox) actor).getSelected());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        func.apply(item);

        ret.setSelected(item);
        return ret;
    }

    static public SelectBox newComboBoxInt(Object[] text, Function<Integer, Void> func) {
        SelectBox ret = new SelectBox(GetSkin());
        ret.setItems(text);
        ret.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    func.apply(((SelectBox) actor).getSelectedIndex());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        return ret;
    }

    static public TextField newTextField(String text) {
        return new TextField(text, GetSkin());
    }

    static public TextField newTextField(String text, Function<String, Void> func) {
        TextField ret = new TextField(text, GetSkin());
        ret.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    func.apply(((TextField) actor).getText());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        return ret;
    }

    static public TextButton newTextButton(String text, Callable func) {
        TextButton ret = newTextButton(text);
        ret.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                try {
                    func.call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        return ret;
    }

    static public Slider newSlider(float min, float max, float step, boolean vertical) {
        Slider ret = new Slider(min, max, step, vertical, GetSkin());
        return ret;
    }

    static public CheckBox newCheckBox(String text) {
        CheckBox ret = new CheckBox(text, GetSkin());
        return ret;
    }

    static public Skin GetSkin() {

        if (SelectedSkin == null) {
            SelectedSkin = new Skin();


   /*
            FreeTypeFontGenerator generator=new FreeTypeFontGenerator(Res.CurrentRes.GetFile(AdventureApplicationAdapter.CurrentAdapter.GetRes().GetConfigData().font));
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = 20; // font size
            parameter.color=new Color(Integer.parseInt(AdventureApplicationAdapter.CurrentAdapter.GetRes().GetConfigData().fontColor,16));
            BitmapFont font = generator.generateFont(parameter);

            generator.dispose();
            SelectedSkin.add("default",font);

   * */
            FileHandle skinFile = Res.CurrentRes.GetFile("skin/uiskin.json");
            FileHandle atlasFile = skinFile.sibling(skinFile.nameWithoutExtension() + ".atlas");
            TextureAtlas atlas = new TextureAtlas(atlasFile);
            SelectedSkin.addRegions(atlas);

            SelectedSkin.load(skinFile);
        }
        return SelectedSkin;
    }

    public static Label newLabel(String name) {

        Label ret = new Label(name, GetSkin());
        return ret;
    }

    public static Dialog newDialog(String title) {
        Dialog ret = new Dialog(title, GetSkin());

        return ret;
    }
}
