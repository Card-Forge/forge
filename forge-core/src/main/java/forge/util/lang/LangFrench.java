package forge.util.lang;

import forge.util.Lang;

public class LangFrench extends Lang {

    @Override
    public String getOrdinal(final int position) {
        if (position == 1) {
            return position + "ᵉʳ";
        } else {
            return position + "ᵉ";
        }
    }

    @Override
    public String getPossesive(final String name) {
        if ("Vous".equalsIgnoreCase(name)) {
            return name;
        }
        return "de " + name;
    }

    @Override
    public String getPossessedObject(final String owner, final String object) {
        if ("Vous".equalsIgnoreCase(owner)) {
            return getPossesive(owner) + " " + object;
        }
        return object + " " + getPossesive(owner);
    }

    @Override
    public String getNickName(final String name) {
        if (name.contains(",")) {
            return name.split(",")[0];
        } else {
            return name.split(" ")[0];
        }
    }
}
