package forge.util.lang;

import forge.util.Lang;

public class LangRussian extends Lang {

    @Override
    public String getOrdinal(final int position) {
        return position + "-й";
    }

    // NOTE: Russian possessives require case declension that can't be derived
    // from an arbitrary name automatically, so this uses an undeclined form
    // (common informal practice for foreign/player names in Russian UI text).
    @Override
    public String getPossesive(final String name) {
        if ("You".equalsIgnoreCase(name)) {
            return "Ваш";
        }
        return name;
    }

    @Override
    public String getPossessedObject(final String owner, final String object) {
        if ("You".equalsIgnoreCase(owner)) {
            return getPossesive(owner) + " " + object;
        }
        return object + " (" + owner + ")";
    }

}
