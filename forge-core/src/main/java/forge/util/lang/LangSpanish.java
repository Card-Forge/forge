package forge.util.lang;

import forge.util.Lang;

public class LangSpanish extends Lang {
    
    @Override
    public String getOrdinal(final int position) {
        return position + "º";
    }

    @Override
    public String getPossesive(final String name) {
        if ("Tu".equalsIgnoreCase(name)) {
            return name;
        }
        return "de " + name;
    }

    @Override
    public String getPossessedObject(final String owner, final String object) {
        if ("Tu".equalsIgnoreCase(owner)) {
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
