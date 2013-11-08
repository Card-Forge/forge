package forge.util;

import java.lang.reflect.Constructor;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ReflectionUtil {

    public static <T> T makeDefaultInstanceOf(Class<? extends T> cls) {
        if ( null == cls )
            throw new IllegalArgumentException("Class<? extends T> cls must not be null");
        
        @SuppressWarnings("unchecked")
        Constructor<? extends T>[] cc = (Constructor<? extends T>[]) cls.getConstructors();
        for (Constructor<? extends T> c : cc) {
            Class<?>[] pp = c.getParameterTypes();
            if (pp.length == 0) {
                try {
                    T res = c.newInstance();
                    return res;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        throw new RuntimeException("No default constructor found in class " + cls.getName());
    }

}
