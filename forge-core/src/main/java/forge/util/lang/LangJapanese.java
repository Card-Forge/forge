package forge.util.lang;

import forge.util.Lang;

public class LangJapanese extends Lang {
    
    @Override
    public String getOrdinal(final int position) {
        return position + "番";
    }

    @Override
    public String getPossesive(final String name) {
        return name + "の";
    }

    @Override
    public String getPossessedObject(final String owner, final String object) {
        return getPossesive(owner) + object;
    }

    @Override
    public String getNickName(final String name) {
        String [] splitName = name.split("、");
        if (splitName.length > 1) return splitName[1];
        return name;
    }

}
