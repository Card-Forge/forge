package forge;

import forge.properties.ForgePreferences.FPref;

public class PreferencesProvider implements Dependencies.PreferencesMethods {
    @Override
    public boolean getCloneModeSource() {
        return Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_CLONE_MODE_SOURCE);
    }

    @Override
    public boolean isManaBurnEnabled() {
        // TODO Auto-generated method stub
        return Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_MANABURN);
    }

    @Override
    public boolean areBlocksFree() {
        return Singletons.getModel().getPreferences().getPrefBoolean(FPref.MATCHPREF_PROMPT_FREE_BLOCKS);
    }
}