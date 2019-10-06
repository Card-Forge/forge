package forge.util;

import org.apache.commons.lang3.text.WordUtils;

public class WordUtil {
    public static String capitalize(String str) {
        return WordUtils.capitalize(str);
    }

    private WordUtil() {}
}
