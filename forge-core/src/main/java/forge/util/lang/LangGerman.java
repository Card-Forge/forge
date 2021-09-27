package forge.util.lang;

import forge.util.Lang;

public class LangGerman extends Lang {
    
    @Override
    public String getOrdinal(final int position) {
        if (position < 20) {
            return position + "te";
        }
        return position + "ste";
    }

    // TODO: Please update this when you modified lblYou in de-DE.properties
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
