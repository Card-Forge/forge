package forge;

public class Dependencies {

    public static PreferencesMethods preferences;
    public interface PreferencesMethods {
        @Deprecated public abstract boolean getEnableAiCheats();
        @Deprecated public abstract boolean getCloneModeSource();
        @Deprecated public abstract String getLogEntryType();
        @Deprecated public abstract String getCurrentAiProfile();
        @Deprecated public abstract boolean isManaBurnEnabled();
        @Deprecated public abstract boolean areBlocksFree();
    }
}