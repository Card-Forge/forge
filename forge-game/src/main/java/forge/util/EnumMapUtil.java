package forge.util;

import java.util.HashMap;
import java.util.Map;

public final class EnumMapUtil {
    private EnumMapUtil() { };

    public static <K extends Enum<K>, V> HashMap<String, V> toStringMap(Map<K, V> map) {
        HashMap<String, V> output = new HashMap<>(map.size());

        for (Map.Entry<K, V> entry : map.entrySet()) {
            output.put(entry.getKey().toString(), entry.getValue());
        }

        return output;
    }
}
