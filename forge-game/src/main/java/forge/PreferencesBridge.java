package forge;


public class PreferencesBridge {
    public static PreferencesSet Instance;
    
    public interface PreferencesSet {

        public abstract boolean getEnableAiCheats();

        public abstract boolean getCloneModeSource();

        public abstract String getLogEntryType();

        public abstract String getCurrentAiProfile();

        public abstract boolean canRandomFoil();

        public abstract boolean isManaBurnEnabled();

        public abstract boolean areBlocksFree();

    }
    
}