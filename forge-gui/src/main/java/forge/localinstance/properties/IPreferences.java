package forge.localinstance.properties;

public interface IPreferences<T extends IPreferences.IPref> {

    void save();

    void reset();

    void setPref(T q0, String s0);

    default void setPref(T q0, boolean val) {
        setPref(q0, String.valueOf(val));
    }
    
    default void togglePrefBoolean(final T q0) {
        setPref(q0, !getPrefBoolean(q0));
    }

    String getPref(T fp0);

    default String getPrefDefault(T key) {
        return key.getDefault();
    }

    default int getPrefInt(T fp0) {
        try {
            return Integer.parseInt(getPref(fp0));
        } catch (NumberFormatException e) {
            return Integer.parseInt(getPrefDefault(fp0));
        }
    }

    default boolean getPrefBoolean(final T fp0) {
        return Boolean.parseBoolean(getPref(fp0));
    }

    default double getPrefDouble(final T fp0) {
        return Double.parseDouble(getPref(fp0));
    }

    public interface IPref  {
        String getDefault(); // Common method for getting the default value
    }
}