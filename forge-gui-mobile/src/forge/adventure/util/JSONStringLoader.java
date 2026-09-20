package forge.adventure.util;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.SerializationException;

import java.util.HashMap;

/**
 * JSONStringLoader
 * Wrapper around Json functions for easier loading of arbitrary JSON strings without
 * having to try/catch every time.
 */
public class JSONStringLoader {
    private static final Json JSON = new Json();

    private static final HashMap<String, Object> compiledJsonCacheMap = new HashMap<>(512);

    public static @Null <T> T parse(Class<T> type, String json, String fallback){
        return parse(type, null, json, fallback);
    }

    @SuppressWarnings("unchecked")
    public static @Null <T> T parse(Class<T> type, Class elementType, String json, String fallback){
        final String cacheKey = (json != null && !json.isEmpty()) ? json : (fallback != null ? fallback : "");
        if (compiledJsonCacheMap.containsKey(cacheKey)) {
            return (T) compiledJsonCacheMap.get(cacheKey);
        }

        T result = null;
        if (json != null && !json.isEmpty()){
            try {
                result = JSON.fromJson(type, elementType, json);
            } catch(SerializationException E) {
                //JSON parsing could fail. Since this an user written part, assume failure is possible (it happens).
                System.err.printf("Error loading JSON string:\n%s\nUsing fallback.", E.getMessage());
            }
        }

        // fallback
        if (result == null) {
            result = JSON.fromJson(type, elementType, fallback);
        }

        if (result != null) {
            compiledJsonCacheMap.put(cacheKey, result);
        }

        return result;
    }

    /**
     * Call this when changing world planes,
     * loading entirely new save profiles, or cleaning up main menu states.
     */
    public static void clearCache() {
        compiledJsonCacheMap.clear();
    }
}
