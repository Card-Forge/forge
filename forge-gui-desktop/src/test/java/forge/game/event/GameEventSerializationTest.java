package forge.game.event;

import org.testng.annotations.Test;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.testng.Assert.assertTrue;

/**
 * Dynamically scans all GameEvent implementations and verifies that every
 * record component type is serializable. This catches regressions when someone
 * adds a new event with a non-serializable field (e.g. Card instead of CardView).
 */
public class GameEventSerializationTest {

    // Known exceptions: events that intentionally hold non-serializable engine
    // objects (e.g. Game) because they are never sent over the network.
    private static final Set<Class<?>> EXCLUDED_EVENTS = Set.of(
            GameEventSubgameStart.class,
            GameEventSubgameEnd.class
    );

    @Test
    public void testAllGameEventFieldsAreSerializable() throws Exception {
        List<Class<?>> eventClasses = findGameEventClasses();
        assertTrue(eventClasses.size() > 10,
                "Expected to find many GameEvent classes but only found " + eventClasses.size());

        List<String> violations = new ArrayList<>();
        for (Class<?> cls : eventClasses) {
            if (EXCLUDED_EVENTS.contains(cls)) {
                continue;
            }
            if (!cls.isRecord()) {
                violations.add(cls.getSimpleName() + " is not a record");
                continue;
            }
            for (RecordComponent rc : cls.getRecordComponents()) {
                if (!isSerializableType(rc.getGenericType())) {
                    violations.add(cls.getSimpleName() + "." + rc.getName()
                            + " has non-serializable type: " + rc.getGenericType().getTypeName());
                }
            }
        }

        assertTrue(violations.isEmpty(),
                "GameEvent serialization violations:\n  " + String.join("\n  ", violations));
    }

    /**
     * Checks whether a type is serializable for network transport purposes.
     * Primitives, enums, and classes implementing Serializable pass directly.
     * Collection/Map interfaces pass if their type arguments are serializable.
     */
    private boolean isSerializableType(Type type) {
        if (type instanceof Class<?> cls) {
            return cls.isPrimitive()
                    || cls.isEnum()
                    || Serializable.class.isAssignableFrom(cls);
        }
        if (type instanceof ParameterizedType pt) {
            // For generic types like List<CardView> or Multimap<K,V>,
            // check both the raw type container and all type arguments.
            Type rawType = pt.getRawType();
            if (rawType instanceof Class<?> rawCls) {
                // Raw type must be Serializable OR a known collection interface
                // (Iterable, Collection, Map, Multimap — these aren't Serializable
                // at the interface level but their concrete implementations are)
                if (!rawCls.isPrimitive()
                        && !rawCls.isEnum()
                        && !Serializable.class.isAssignableFrom(rawCls)
                        && !rawCls.isInterface()) {
                    return false;
                }
            }
            for (Type arg : pt.getActualTypeArguments()) {
                if (!isSerializableType(arg)) {
                    return false;
                }
            }
            return true;
        }
        // Wildcard types, type variables — conservatively pass
        return true;
    }

    /**
     * Finds all concrete classes implementing GameEvent in the forge.game.event package
     * by scanning the classpath (handles both directories and JARs).
     */
    private List<Class<?>> findGameEventClasses() throws Exception {
        String packageName = "forge.game.event";
        String packagePath = packageName.replace('.', '/');
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = cl.getResources(packagePath);
        List<Class<?>> result = new ArrayList<>();

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            String protocol = resource.getProtocol();
            if ("file".equals(protocol)) {
                File[] files = new File(resource.toURI()).listFiles();
                if (files != null) {
                    for (File file : files) {
                        String name = file.getName();
                        if (name.endsWith(".class") && !name.contains("$")) {
                            loadIfGameEvent(packageName + "." + name.substring(0, name.length() - 6), result);
                        }
                    }
                }
            } else if ("jar".equals(protocol)) {
                String jarPath = resource.getPath();
                jarPath = jarPath.substring(5, jarPath.indexOf("!"));
                try (JarFile jar = new JarFile(new File(jarPath))) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        String entryName = entries.nextElement().getName();
                        if (entryName.startsWith(packagePath + "/")
                                && entryName.endsWith(".class")
                                && !entryName.contains("$")) {
                            String className = entryName.substring(0, entryName.length() - 6).replace('/', '.');
                            loadIfGameEvent(className, result);
                        }
                    }
                }
            }
        }
        return result;
    }

    private void loadIfGameEvent(String className, List<Class<?>> result) throws ClassNotFoundException {
        Class<?> cls = Class.forName(className);
        if (GameEvent.class.isAssignableFrom(cls) && !cls.isInterface() && cls != GameEvent.class) {
            result.add(cls);
        }
    }
}
