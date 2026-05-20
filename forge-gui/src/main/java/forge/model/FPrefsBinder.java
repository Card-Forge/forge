package forge.model;

import java.util.function.BiConsumer;
import java.util.function.Function;

import forge.localinstance.properties.ForgePreferences;

/**
 * Binds any GUI component to a Forge preference key
 */

public class FPrefsBinder<C, V> implements ModelBinder<ForgePreferences.FPref, C, V> {
  private final ForgePreferences.FPref prefKey;
  private final C component;

  /** Extracts a value from the component */
  private final Function<C, V> extractor;

  /** Pushes a value back into the component */
  private final BiConsumer<C, V> applier;

  /** conversions between pref store value and component value */
  private final Function<V, String> toString;
  private final Function<String, V> fromString;

  public FPrefsBinder(ForgePreferences.FPref prefKey,
                    C component,
                    Function<C, V> extractor,
                    BiConsumer<C, V> applier,
                    Function<V, String> toString,
                    Function<String, V> fromString) {
    this.prefKey = prefKey;
    this.component = component;
    this.extractor = extractor;
    this.applier = applier;
    this.toString = toString;
    this.fromString = fromString;
  }

  public void load() {
    ForgePreferences prefs = FModel.getPreferences();
    String prefValue = prefs.getPref(prefKey);
    if (prefValue != null) {
      V value = fromString.apply(prefValue);
      applier.accept(component, value);
    }
  }

  public void save() {
    ForgePreferences prefs = FModel.getPreferences();
    V value = extractor.apply(component);
    String prefValue = toString.apply(value);
    prefs.setPref(prefKey, prefValue);
    prefs.save();
  }
}
