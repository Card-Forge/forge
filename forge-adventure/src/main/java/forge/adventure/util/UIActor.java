package forge.adventure.util;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
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

    public UIActor(FileHandle handle) {
        data = (new Json()).fromJson(UIData.class, handle);

        setWidth(data.width);
        setHeight(data.height);

        for (OrderedMap<String, String> element : new Array.ArrayIterator<>(data.elements)) {
            String type = element.get("type");
            Actor newActor;
            if(type==null)
            {
                newActor=new Actor();
            }
            else
            {
                switch (type) {
                    case "Selector":
                        newActor = new Selector();
                        readSelectorProperties((Selector) newActor, new OrderedMap.OrderedMapEntries<>(element));
                        break;
                    case "Label":
                        newActor = new Label("", Controls.GetSkin());
                        readLabelProperties((Label) newActor,  new OrderedMap.OrderedMapEntries<>(element));
                        break;
                    case "Image":
                        newActor = new Image();
                        readImageProperties((Image) newActor,  new OrderedMap.OrderedMapEntries<>(element));
                        break;
                    case "ImageButton":
                        newActor = new ImageButton(Controls.GetSkin());
                        readImageButtonProperties((ImageButton) newActor,  new OrderedMap.OrderedMapEntries<>(element));
                        break;
                    case "Window":
                        newActor = new Window("", Controls.GetSkin());
                        readWindowProperties((Window) newActor,  new OrderedMap.OrderedMapEntries<>(element));
                        break;
                    case "TextButton":
                        newActor = new TextButton("", Controls.GetSkin());
                        readButtonProperties((TextButton) newActor,  new OrderedMap.OrderedMapEntries<>(element));
                        break;
                    case "TextField":
                        newActor = new TextField("", Controls.GetSkin());
                        readTextFieldProperties((TextField) newActor,  new OrderedMap.OrderedMapEntries<>(element));
                        break;
                    case "Scroll":
                        newActor = new ScrollPane(null, Controls.GetSkin());
                        readScrollPaneProperties((ScrollPane) newActor,  new OrderedMap.OrderedMapEntries<>(element));
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + type);
                }
            }
            //load Actor Properties
            float yValue = 0;
            for (ObjectMap.Entry property :  new OrderedMap.OrderedMapEntries<>(element)) {
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
                    case "name":
                        newActor.setName((String) property.value);
                        break;
                }
            }
            addActor(newActor);
        }
    }

    private void readScrollPaneProperties(ScrollPane newActor, ObjectMap.Entries<String, String> entries) {
        newActor.setActor(new Label("",Controls.GetSkin()));
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
        for (ObjectMap.Entry property : entries) {
            switch (property.key.toString()) {
                case "text":
                    newActor.setText(property.value.toString());
                    break;
                case "font":
                    Label.LabelStyle style = new Label.LabelStyle(newActor.getStyle());
                    style.font = Controls.GetSkin().getFont(property.value.toString());
                    newActor.setStyle(style);
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
                    newActor.setDrawable(new TextureRegionDrawable(new Texture(Config.instance().getFile(property.value.toString()))));
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
                func.run();
            }
        });
    }
}
