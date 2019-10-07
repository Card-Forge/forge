package forge.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import forge.model.FModel;
import forge.properties.ForgeConstants;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;

import java.util.ArrayList;
import java.util.List;

public class FLanguage {

    public static void changeLanguage(final String languageName) {
        final ForgePreferences prefs = FModel.getPreferences();
        if (languageName.equals(prefs.getPref(FPref.UI_LANGUAGE))) { return; }

        //save language preference
        prefs.setPref(FPref.UI_LANGUAGE, languageName);
        prefs.save();
    }

    /**
     * Gets the languages.
     *
     * @return the languages
     */
    public static Iterable<String> getAllLanguages() {
        final List<String> allLanguages = new ArrayList<>();

        final FileHandle dir = Gdx.files.absolute(ForgeConstants.LANG_DIR);
        for (FileHandle languageFile : dir.list()) {
            String languageName = languageFile.name();
            if (!languageName.endsWith(".properties")) { continue; }
            allLanguages.add(languageName.replace(".properties", ""));
        }

        return allLanguages;
    }

}
