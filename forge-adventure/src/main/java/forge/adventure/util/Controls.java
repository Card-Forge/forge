package forge.adventure.util;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Scaling;

import java.util.concurrent.Callable;
import java.util.function.Function;

public class Controls {
    private static  Skin SelectedSkin=null;
    public static int DEFAULTHEIGHT=40;
    static public TextButton newTextButton(String text)
    {
        TextButton ret=new TextButton(text,GetSkin());

        return ret;
    }
    static public SelectBox newComboBox(Object[] text,Object item, Function<Object, Void> func)
    {

        SelectBox ret=new SelectBox(GetSkin());
        ret.setItems(text);
        ret.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor)
            {
                try {
                    func.apply(((SelectBox)actor).getSelected());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        func.apply(item);

        ret.setSelected(item);
        return ret;
    }
    static public SelectBox newComboBoxInt(Object[] text, Function<Integer, Void> func)
    {
        SelectBox ret=new SelectBox(GetSkin());
        ret.setItems(text);
        ret.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor)
            {
                try {
                    func.apply(((SelectBox)actor).getSelectedIndex());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        return ret;
    }
    static public TextField newTextField(String text)
    {
        return new TextField(text,GetSkin());
    }
    static public TextField newTextField(String text, Function<String, Void> func)
    {
        TextField ret=new TextField(text,GetSkin());
        ret.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor)
            {
                try {
                    func.apply(((TextField)actor).getText());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        return ret;
    }
    static public TextButton newTextButton(String text, Callable func)
    {
        TextButton ret=new TextButton(text,GetSkin());
        ret.addListener(new ClickListener(){
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
    static public Slider newSlider(float min,float max,float step,boolean vertical)
    {
        Slider ret=new Slider(min,max,step,vertical,GetSkin());
        ret.getStyle().knob.setMinHeight(DEFAULTHEIGHT);
        return ret;
    }
    static public CheckBox newCheckBox(String text)
    {
        CheckBox ret=new CheckBox(text,GetSkin());
        ret.getImage().setScaling(Scaling.fill);
        ret.getImageCell().size(DEFAULTHEIGHT);
        return ret;
    }
    static public Skin GetSkin() {

        if(SelectedSkin==null)
        {
            SelectedSkin=new Skin(Res.CurrentRes.GetFile("skin/uiskin.json"));
            SelectedSkin.getFont("default-font").getData().setScale(2);
        }
        return SelectedSkin;
    }

    public static Label newLabel(String name) {

        Label ret=new Label(name,GetSkin());
        return ret;
    }

    public static Dialog newDialog(String title) {
        Dialog ret=new Dialog(title,GetSkin());

        return ret;
    }
}
