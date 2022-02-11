package forge.adventure.data;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.OrderedMap;

/**
 * Data class that will be used to read Json configuration files
 * UIData
 * contains a GUI definition, used for most user interfaces to customize the UI
 */
public class UIData {
    public int width;
    public int height;
    public boolean yDown;
    public Array<OrderedMap<String,String>> elements;
}
