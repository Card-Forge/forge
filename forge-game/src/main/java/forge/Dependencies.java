package forge;

public class Dependencies {

    public static PreferencesMethods preferences;
    public interface PreferencesMethods {
        @Deprecated public abstract boolean getCloneModeSource();
        @Deprecated public abstract boolean isManaBurnEnabled();
    }
}