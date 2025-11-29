package forge.util;

import java.io.Serializable;
import java.util.function.Function;

public interface FSerializableFunction<T> extends Function<T, String>, Serializable {

}
