package forge.card;

import java.util.Map;
import java.util.HashMap;

/** 
 * Preferences associated with individual cards
 *
 */
public class CardPreferences {
    private static Map<String, CardPreferences> allPrefs = new HashMap<String, CardPreferences>();

    public static CardPreferences getPrefs(String key) {
        CardPreferences prefs = allPrefs.get(key);
        if (prefs == null) {
            prefs = new CardPreferences();
            allPrefs.put(key, prefs);
        }
        return prefs;
    }

    private int starCount;

    private CardPreferences() {
    }

    public int getStarCount() {
        return this.starCount;
    }

    public void setStarCount(int starCount0) {
        this.starCount = starCount0;
    }
}