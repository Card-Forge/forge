package forge.control.input;

import java.util.List;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public interface InputSelectList<T> extends InputSynchronized { 
    boolean hasCancelled();
    List<T> getSelected();
}