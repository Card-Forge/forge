package forge.adventure.util;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.OrderedMap;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;
import forge.Forge;
import forge.adventure.data.UIData;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Group of controls that will be loaded from a configuration file
 */
public class UIActor extends Group {
    UIData data;
    Actor lastActor=null;

    public UIActor(FileHandle handle) {
        data = (new Json()).fromJson(UIData.class, handle);

        setWidth(data.width);
        setHeight(data.height);

        for (OrderedMap<String, String> element : data.elements) {
            String type = element.get("type");
            Actor newActor;
            if (type == null) {
                newActor = new Actor();
            } else {
                switch (type) {
                    case "Selector":
                        newActor = new Selector();
                        readSelectorProperties((Selector) newActor, new OrderedMap.OrderedMapEntries<>(element));
                        break;
                    case "Label":
                        newActor = Controls.newTextraLabel("");
                        readLabelProperties((TextraLabel) newActor, new OrderedMap.OrderedMapEntries<>(element));
                        break;
                    case "Table":
                        newActor = new Table(Controls.getSkin());
                        readTableProperties((Table) newActor, new OrderedMap.OrderedMapEntries<>(element));
                        break;
                    case "Image":
                        newActor = new Image();
                        readImageProperties((Image) newActor, new OrderedMap.OrderedMapEntries<>(element));
                        break;
                    case "ImageButton":
                        newActor = new ImageButton(Controls.getSkin());
                        readImageButtonProperties((ImageButton) newActor, new OrderedMap.OrderedMapEntries<>(element));
                        break;
                    case "Window":
                        newActor = new Window("", Controls.getSkin());
                        readWindowProperties((Window) newActor, new OrderedMap.OrderedMapEntries<>(element));
                        break;
                    case "TextButton":
                        newActor = Controls.newTextButton("");
                        readButtonProperties((TextraButton) newActor, new OrderedMap.OrderedMapEntries<>(element));
                        break;
                    case "TextField":
                        newActor = new TextField("", Controls.getSkin());
                        readTextFieldProperties((TextField) newActor, new OrderedMap.OrderedMapEntries<>(element));
                        break;
                    case "Scroll":
                        newActor = new ScrollPane(null, Controls.getSkin());
                        readScrollPaneProperties((ScrollPane) newActor, new OrderedMap.OrderedMapEntries<>(element));
                        break;
                    case "CheckBox":
                        newActor = new CheckBox("", Controls.getSkin());
                        readCheckBoxProperties((CheckBox) newActor, new OrderedMap.OrderedMapEntries<>(element));
                        break;
                    case "SelectBox":
                        newActor = new SelectBox<>(Controls.getSkin());
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + type);
                }
            }
            //load Actor Properties
            float yValue = 0;
            for (ObjectMap.Entry property : new OrderedMap.OrderedMapEntries<>(element)) {
                switch (property.key.toString()) {
                    case "scale":
                        newActor.setScale((Float) property.value);
                        break;
                    case "width":
                        newActor.setWidth((Float) property.value);
                        break;
                    case "height":
                        newActor.setHeight((Float) property.value);
                        if (data.yDown)
                            newActor.setY(data.height - yValue - newActor.getHeight());
                        break;
                    case "x":
                        newActor.setX((Float) property.value);
                        break;
                    case "y":
                        yValue = (Float) property.value;
                        newActor.setY(data.yDown ? data.height - yValue - newActor.getHeight() : yValue);
                        break;
                    case "yOffset":
                        if(data.yDown)
                        {
                            yValue =  (Float)property.value+((lastActor!=null?(data.height-lastActor.getY()):0f));
                            newActor.setY( data.height - yValue - newActor.getHeight() );
                        }
                        else
                        {

                            yValue =  (Float)property.value+((lastActor!=null?(lastActor.getY()):0f));
                            newActor.setY(yValue);
                        }
                        break;
                    case "xOffset":
                        newActor.setX((Float)property.value+((lastActor!=null?lastActor.getX():0f)));
                        break;
                    case "name":
                        newActor.setName((String) property.value);
                        break;
                }
            }
            lastActor=newActor;
            addActor(newActor);
        }
    }

    private void readScrollPaneProperties(ScrollPane newActor, ObjectMap.Entries<String, String> entries) {
        newActor.setActor(Controls.newTextraLabel(""));
        for (ObjectMap.Entry property : entries) {
            switch (property.key.toString()) {
                case "style":
                    newActor.setStyle(Controls.getSkin().get(property.value.toString(), ScrollPane.ScrollPaneStyle.class));
                    break;
            }
        }
    }

    private void readWindowProperties(Window newActor, ObjectMap.Entries<String, String> entries) {
        for (ObjectMap.Entry property : entries) {
            switch (property.key.toString()) {
                case "style":
                    newActor.setStyle(Controls.getSkin().get(property.value.toString(), Window.WindowStyle.class));
                    break;
            }
        }
    }

    private void readTextFieldProperties(TextField newActor, ObjectMap.Entries<String, String> entries) {
        for (ObjectMap.Entry property : entries) {
            switch (property.key.toString()) {
                case "text":

                    newActor.setText(localize(property.value.toString()));
                    break;
                case "align":
                    newActor.setAlignment(((Float) property.value).intValue());
                    break;
            }
        }
    }

    public static String localize(String str) {
        Pattern regex=Pattern.compile("tr\\([^\\)]*\\)");
        for(int i=0;i<100;i++)
        {
           Matcher matcher= regex.matcher(str);
           if(!matcher.find())
               return str;
            str=matcher.replaceFirst(Forge.getLocalizer().getMessage(matcher.group().substring(3,matcher.group().length()-1)));
        }
        return str;
    }

    private void readImageButtonProperties(ImageButton newActor, ObjectMap.Entries<String, String> entries) {
        for (ObjectMap.Entry property : entries) {
            switch (property.key.toString()) {
                case "style":
                    newActor.setStyle(Controls.getSkin().get(property.value.toString(), ImageButton.ImageButtonStyle.class));
                    break; 
            }
        }
    }

    private void readLabelProperties(TextraLabel newActor, ObjectMap.Entries<String, String> entries) {
        for (ObjectMap.Entry property : entries) {
            switch (property.key.toString()) {
                case "text":
                    newActor.setText(localize(property.value.toString()));
                    break;
                case "font":
                case "fontName":
                    if(!property.value.toString().equals("default"))
                        newActor.setFont(Controls.getTextraFont(property.value.toString()));
                    break;
                case "style":
                    newActor.style=(Controls.getSkin().get(property.value.toString(), Label.LabelStyle.class));
                    break;
                case "color":
                case "fontColor":
                    newActor.layout.setBaseColor(Controls.colorFromString(property.value.toString()));
                    break;
            }
        }
        newActor.setText(newActor.storedText);//necessary if color changes after text inserted
        newActor.layout();
    }

    private void readTableProperties(Table newActor, ObjectMap.Entries<String, String> entries) {
        for (ObjectMap.Entry property : entries) {
            switch (property.key.toString()) {
                case "font":
                    newActor.getSkin().get(Label.LabelStyle.class).font = Controls.getBitmapFont(property.value.toString());
                    if (property.value.toString().contains("black"))
                        newActor.getSkin().get(Label.LabelStyle.class).fontColor = Color.BLACK;
                    if (property.value.toString().contains("big"))
                        newActor.setScale(2, 2);
                    break;
            }
        }
    }

    private void readSelectorProperties(Selector newActor, ObjectMap.Entries<String, String> entries) {
    }

    private void readCheckBoxProperties(CheckBox newActor, ObjectMap.Entries<String, String> entries) {
        for (ObjectMap.Entry property : entries) {
            switch (property.key.toString()) {
                case "text":
                    newActor.setText(localize(property.value.toString()));
                    break;
            }
        }
    }

    private void readButtonProperties(TextraButton newActor, ObjectMap.Entries<String, String> entries) {
        for (ObjectMap.Entry property : entries) {
            switch (property.key.toString()) {
                case "text":
                    newActor.setText(localize(property.value.toString()));
                    break;
                case "style":
                    newActor.setStyle(Controls.getSkin().get(property.value.toString(), TextButton.TextButtonStyle.class));
                    break;
            }
        }
        newActor.layout();
    }

    private void readImageProperties(Image newActor, ObjectMap.Entries<String, String> entries) {
        for (ObjectMap.Entry property : entries) {
            switch (property.key.toString()) {
                case "image":
                    Texture t = new Texture(Config.instance().getFile(property.value.toString()));
                    TextureRegion tr = new TextureRegion(t);
                    newActor.setDrawable(new TextureRegionDrawable(tr));
                    break;
            }
        }
    }

    public void onButtonPress(String name, Runnable func) {

        Actor button = findActor(name);
        assert button != null;
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if(button instanceof Button)
                {
                    if(((Button)button).isDisabled())
                        return;
                }
                func.run();
            }
        });
    }
    static final public int ButtonYes=0x1;
    static final public int ButtonNo=0x2;
    static final public int ButtonOk=0x4;
    static final public int ButtonAbort=0x8;
    public Dialog showDialog(Stage stage, String header, int buttons, Runnable onOkOrYes) {
        Dialog dialog =new Dialog(header, Controls.getSkin())
        {
            protected void result(Object object)
            {
                if(onOkOrYes!=null&&object!=null&&object.equals(true))
                    onOkOrYes.run();
                this.hide();
                removeActor(this);
            }
        };
        if((buttons&ButtonYes)!=0)
            dialog.button(Forge.getLocalizer().getMessage("lblYes"), true);
        if((buttons&ButtonNo)!=0)
            dialog.button(Forge.getLocalizer().getMessage("lblNo"), false);
        if((buttons&ButtonOk)!=0)
            dialog.button(Forge.getLocalizer().getMessage("lblOk"), true);
        if((buttons&ButtonAbort)!=0)
            dialog.button(Forge.getLocalizer().getMessage("lblAbort"), false);
        addActor(dialog);
        dialog.show(stage);
        return dialog;
    }
}
