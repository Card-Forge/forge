package forge.util.lang;

import forge.util.Lang;

public class LangItalian extends Lang {
    
    @Override
    public String getOrdinal(final int position) {
        return position + "º";
    }

    // TODO: Please update this when you modified lblYou in it-IT.properties
    @Override
    public String getPossesive(final String name) {
        if ("You".equalsIgnoreCase(name)) {
            return name + "r"; // to get "your"
        }
        return name.endsWith("s") ? name + "'" : name + "'s";
    }

    @Override
    public String getPossessedObject(final String owner, final String object) {
        return getPossesive(owner) + " " + object;
    }

    @Override
    public String getNickName(final String name) {
        return name.split(" ")[0].replace(",", "");
    }

}
