package forge.adventure.util;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.SerializationException;
/**
 * JSONStringLoader
 * Wrapper around Json functions for easier loading of arbitrary JSON strings without
 * having to try/catch every time.
 */
public class JSONStringLoader {
    private static final Json JSON = new Json();
    public static @Null <T> T parse(Class<T> type, String json, String fallback){
        return parse(type, null, json, fallback);
    }

    public static @Null <T> T parse(Class<T> type, Class elementType, String json, String fallback){
        if(json != null && !json.isEmpty()){
            try { return JSON.fromJson(type, elementType, json); }
            catch(SerializationException E) {
                //JSON parsing could fail. Since this an user written part, assume failure is possible (it happens).
                System.err.printf("Error loading JSON string:\n%s\nUsing fallback.", E.getMessage());
            }
        }
        return JSON.fromJson(type, elementType, fallback);
    }
}
