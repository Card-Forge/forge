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
  private boolean loading;

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

  public C getComponent() {
    return component;
  }

  public void load() {
    String prefValue = FModel.getPreferences().getPref(prefKey);
    if (prefValue != null) {
      loading = true;
      try {
        applier.accept(component, fromString.apply(prefValue));
      } finally {
        loading = false;
      }
    }
  }

  public void save() {
    if (loading) return;   // ignore events caused by load()
    ForgePreferences prefs = FModel.getPreferences();
    prefs.setPref(prefKey, toString.apply(extractor.apply(component)));
    prefs.save();
  }

  /** Subclasses attach their component listener here. */
  public void bind() { }
}
