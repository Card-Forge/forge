package forge.gui.input;

import java.util.List;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public interface InputSelectMany<T> extends InputSynchronized { 
    boolean hasCancelled();
    List<T> getSelected();
}