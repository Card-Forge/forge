package forge.adventure.util;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.OrderedMap;
import forge.adventure.data.UIData;

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

        for (OrderedMap<String, String> element : new Array.ArrayIterator<>(data.elements)) {
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
                        newActor = new Label("", Controls.GetSkin());
                        readLabelProperties((Label) newActor, new OrderedMap.OrderedMapEntries<>(element));
                        break;
                    case "Table":
                        newActor = new Table(Controls.GetSkin());
                        readTableProperties((Table) newActor, new OrderedMap.OrderedMapEntries<>(element));
                        break;
                    case "Image":
                        newActor = new Image();
                        readImageProperties((Image) newActor, new OrderedMap.OrderedMapEntries<>(element));
                        break;
                    case "ImageButton":
                        newActor = new ImageButton(Controls.GetSkin());
                        readImageButtonProperties((ImageButton) newActor, new OrderedMap.OrderedMapEntries<>(element));
                        break;
                    case "Window":
                        newActor = new Window("", Controls.GetSkin());
                        readWindowProperties((Window) newActor, new OrderedMap.OrderedMapEntries<>(element));
                        break;
                    case "TextButton":
                        newActor = new TextButton("", Controls.GetSkin());
                        readButtonProperties((TextButton) newActor, new OrderedMap.OrderedMapEntries<>(element));
                        break;
                    case "TextField":
                        newActor = new TextField("", Controls.GetSkin());
                        readTextFieldProperties((TextField) newActor, new OrderedMap.OrderedMapEntries<>(element));
                        break;
                    case "Scroll":
                        newActor = new ScrollPane(null, Controls.GetSkin());
                        readScrollPaneProperties((ScrollPane) newActor, new OrderedMap.OrderedMapEntries<>(element));
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + type);
                }
            }
            //load Actor Properties
            float yValue = 0;
            for (ObjectMap.Entry property : new OrderedMap.OrderedMapEntries<>(element)) {
                switch (property.key.toString()) {
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
        newActor.setActor(new Label("", Controls.GetSkin()));
        for (ObjectMap.Entry property : entries) {
            switch (property.key.toString()) {
                case "style":
                    newActor.setStyle(Controls.GetSkin().get(property.value.toString(), ScrollPane.ScrollPaneStyle.class));
                    break;
            }
        }
    }

    private void readWindowProperties(Window newActor, ObjectMap.Entries<String, String> entries) {
        for (ObjectMap.Entry property : entries) {
            switch (property.key.toString()) {
                case "style":
                    newActor.setStyle(Controls.GetSkin().get(property.value.toString(), Window.WindowStyle.class));
                    break;
            }
        }
    }

    private void readTextFieldProperties(TextField newActor, ObjectMap.Entries<String, String> entries) {
        for (ObjectMap.Entry property : entries) {
            switch (property.key.toString()) {
                case "text":
                    newActor.setText(property.value.toString());
                    break;
                case "align":
                    newActor.setAlignment(((Float) property.value).intValue());
                    break;
            }
        }
    }

    private void readImageButtonProperties(ImageButton newActor, ObjectMap.Entries<String, String> entries) {
        for (ObjectMap.Entry property : entries) {
            switch (property.key.toString()) {
                case "style":
                    newActor.setStyle(Controls.GetSkin().get(property.value.toString(), ImageButton.ImageButtonStyle.class));
                    break; 
            }
        }
    }

    private void readLabelProperties(Label newActor, ObjectMap.Entries<String, String> entries) {
        Label.LabelStyle style = new Label.LabelStyle(newActor.getStyle());
        for (ObjectMap.Entry property : entries) {
            switch (property.key.toString()) {
                case "text":
                    newActor.setText(property.value.toString());
                    break;
                case "font"://legacy
                    style.font = Controls.getBitmapFont(property.value.toString());
                    if (property.value.toString().contains("black"))
                        style.fontColor = Color.BLACK;
                    if (property.value.toString().contains("big"))
                        newActor.setFontScale(2, 2);
                    newActor.setStyle(style);
                    break;
                case "fontSize":
                    newActor.setFontScale((Float)property.value, (Float)property.value);
                    break;
                case "fontName":
                    style.font = Controls.getBitmapFont(property.value.toString());
                    newActor.setStyle(style);
                    break;
                case "fontColor":
                    newActor.setColor(new Color(Integer.decode(property.value.toString()) ));
                    break;
            }
        }
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

    private void readButtonProperties(TextButton newActor, ObjectMap.Entries<String, String> entries) {
        for (ObjectMap.Entry property : entries) {
            switch (property.key.toString()) {
                case "text":
                    newActor.setText(property.value.toString());
                    break;
            }
        }
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
}
