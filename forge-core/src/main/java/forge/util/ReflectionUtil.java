package forge.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Static utilities related to reflection.
 * 
 * @see java.lang.Class
 */
public final class ReflectionUtil {

    /**
     * Private constructor to prevent instantiation.
     */
    private ReflectionUtil() {
    }

    /**
     * Generates a default instance of the supplied class, using that class'
     * default constructor.
     * 
     * @param cls
     *            a {@link Class}.
     * @return an instance of the supplied class.
     * @throws RuntimeException
     *             if the supplied class has no visible default constructor, or
     *             if an exception is thrown by the constructor.
     * @see Class#newInstance()
     */
    public static <T> T makeDefaultInstanceOf(final Class<? extends T> cls) {
        if (null == cls) {
            throw new IllegalArgumentException("Class<? extends T> cls must not be null");
        }

        try {
            final Constructor<? extends T> c = cls.getConstructor();
            return c.newInstance();
        } catch (final NoSuchMethodException e) {
            throw new RuntimeException(String.format("No default constructor found in class %s", cls.getName()));
        } catch (final SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException(String.format("Can't instantiate class %s using default constructor", cls.getName()));
        }
    }

    /**
     * Cast object to a given type if possible, returning null if not possible
     * 
     * @param obj
     *            an object.
     * @param type
     *            the {@link Class} to which to cast the object.
     * @return a reference to the object if it's an instance of the given class,
     *         or {@code null} if it isn't.
     * @see Class#isInstance(Object)
     */
    @SuppressWarnings("unchecked")
    public static <T> T safeCast(final Object obj, final Class<T> type) {
        if (type.isInstance(obj)) {
            return (T) obj;
        }
        return null;
    }

}
