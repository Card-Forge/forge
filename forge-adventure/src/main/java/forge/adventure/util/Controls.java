package forge.adventure.util;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import java.util.function.Function;

/**
 * Class to create ui elements in the correct style
 */
public class Controls {
    private static Skin SelectedSkin = null;

    static public TextButton newTextButton(String text) {

        return new TextButton(text, GetSkin());
    }

    static public SelectBox newComboBox(String[] text, String item, Function<Object, Void> func) {

        SelectBox ret = new SelectBox<String>(GetSkin());
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

    static public TextButton newTextButton(String text, Runnable func) {
        TextButton ret = newTextButton(text);
        ret.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                try {
                    func.run();
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
    static public BitmapFont bigFont()
    {
        return SelectedSkin.getFont("big");
    }
    static public BitmapFont font()
    {
        return SelectedSkin.getFont("default");
    }


    static public Skin GetSkin() {

        if (SelectedSkin == null) {
            SelectedSkin = new Skin();



            FileHandle skinFile = Config.instance().getFile(Paths.SKIN);
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
