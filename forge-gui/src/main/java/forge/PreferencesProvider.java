package forge;

import forge.PreferencesBridge.PreferencesSet;
import forge.properties.ForgePreferences.FPref;

public class PreferencesProvider implements PreferencesSet {
    /* (non-Javadoc)
     * @see forge.IPreferencesForGame#getEnableAiCheats()
     */
    @Override
    public boolean getEnableAiCheats() {
        return Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_ENABLE_AI_CHEATS);
    }

    /* (non-Javadoc)
     * @see forge.IPreferencesForGame#getCloneModeSource()
     */
    @Override
    public boolean getCloneModeSource() {
        return Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_CLONE_MODE_SOURCE);
    }

    /* (non-Javadoc)
     * @see forge.IPreferencesForGame#getLogEntryType()
     */
    @Override
    public String getLogEntryType() {
        return Singletons.getModel().getPreferences().getPref(FPref.DEV_LOG_ENTRY_TYPE);
    }

    /* (non-Javadoc)
     * @see forge.IPreferencesForGame#getCurrentAiProfile()
     */
    @Override
    public String getCurrentAiProfile() {
        return Singletons.getModel().getPreferences().getPref(FPref.UI_CURRENT_AI_PROFILE);
    }

    /* (non-Javadoc)
     * @see forge.IPreferencesForGame#canRandomFoil()
     */
    @Override
    public boolean canRandomFoil() {
        return Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_RANDOM_FOIL);
    }

    /* (non-Javadoc)
     * @see forge.IPreferencesForGame#isManaBurnEnabled()
     */
    @Override
    public boolean isManaBurnEnabled() {
        // TODO Auto-generated method stub
        return Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_MANABURN);
    }

    /* (non-Javadoc)
     * @see forge.IPreferencesForGame#areBlocksFree()
     */
    @Override
    public boolean areBlocksFree() {
        return Singletons.getModel().getPreferences().getPrefBoolean(FPref.MATCHPREF_PROMPT_FREE_BLOCKS);
    }
}
