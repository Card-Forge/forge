package forge.model;

/**
 * Binds any GUI component to a Forge preference key
 */

public interface ModelBinder<K, C, V> {
  public void load();
  public void save();
}
