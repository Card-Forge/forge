package forge.util;

import java.io.Serializable;
import java.util.function.Function;

public interface FSerializableFunction<T, V> extends Function<T, V>, Serializable {

}
