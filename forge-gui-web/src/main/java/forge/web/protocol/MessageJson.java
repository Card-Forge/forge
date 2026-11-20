package forge.web.protocol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.SerializedName;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/** Turns the message records into JSON and back. A null component is left out of the JSON. Only components marked {@link Nullable} may be null. */
public final class MessageJson {
    /** The classes whose nested records make up the protocol. */
    private static final List<Class<?>> HOLDERS = List.of(FromBrowser.class, ToBrowser.class);
    private static final Gson GSON = new Gson();
    /** Writes every message to the browser. Nulls are kept, because a property sent as null has gone back to its default. */
    public static final Gson WRITER = new GsonBuilder().serializeNulls().create();
    private static final Map<Class<?>, RecordComponent[]> COMPONENTS = new ConcurrentHashMap<>();

    private MessageJson() {
    }

    /** A message to the browser, sent as {@code {"t": value, ...}}. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Message {
        String value();
    }

    /** A message from the browser. The value is empty when the record has a {@code t} component that names several commands. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Command {
        String value() default "";
    }

    /**
     * A question the game waits on. The browser answers with a {@code reply} command that carries the request's id.
     * The value is empty when a {@code kind} component names several kinds that share one shape.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Request {
        String value() default "";
    }

    /** Something that happened in the game, sent as {@code {"kind": value, ...}} inside a state message. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Event {
        String value();
    }

    /** A component that may be null, and is then left out. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.RECORD_COMPONENT)
    @interface Nullable {
    }

    /** The field's TypeScript type, for a component whose Java type is not specific enough (a raw JSON tree). */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.RECORD_COMPONENT)
    @interface Ts {
        String value();
    }

    /** The protocol's records that carry a mark, sorted by name. The browser's types are written in this order. */
    @SuppressWarnings("unchecked")
    static List<Class<? extends Record>> marked(final Class<? extends Annotation> mark) {
        return HOLDERS.stream().flatMap(h -> Stream.of(h.getDeclaredClasses())).filter(c -> c.isAnnotationPresent(mark))
                .sorted(Comparator.comparing(Class::getSimpleName)).<Class<? extends Record>>map(c -> (Class<? extends Record>) c).toList();
    }

    /** The JSON for a message to the browser, with its {@code t}. */
    public static JsonObject encode(final Record message) {
        final JsonObject out = new JsonObject();
        final Message m = message.getClass().getAnnotation(Message.class);
        if (m != null) {
            out.addProperty("t", m.value());
        }
        fill(out, message);
        return out;
    }

    /** The JSON for a request's own fields, without the id that MatchQuestions adds. */
    public static JsonObject encodeRequest(final Record request) {
        final JsonObject out = new JsonObject();
        out.addProperty("t", "request");
        final Request r = request.getClass().getAnnotation(Request.class);
        if (r != null && !r.value().isEmpty()) {
            out.addProperty("kind", r.value());
        }
        fill(out, request);
        return out;
    }

    private static void fill(final JsonObject out, final Record record) {
        for (final RecordComponent c : components(record.getClass())) {
            final Object value = read(c, record);
            if (value == null) {
                assert c.isAnnotationPresent(Nullable.class)
                        : record.getClass().getSimpleName() + "." + c.getName() + " is null but not @Nullable";
                continue;
            }
            out.add(nameOf(c), toJson(value));
        }
    }

    static JsonElement toJson(final Object value) {
        if (value == null) {
            return JsonNull.INSTANCE;
        }
        if (value instanceof JsonElement e) {
            return e;
        }
        if (value instanceof Record r) {
            final JsonObject o = new JsonObject();
            final Event event = r.getClass().getAnnotation(Event.class);
            if (event != null) {
                o.addProperty("kind", event.value());
            }
            fill(o, r);
            return o;
        }
        if (value instanceof Boolean b) {
            return new JsonPrimitive(b);
        }
        if (value instanceof Number n) {
            return new JsonPrimitive(n);
        }
        if (value instanceof String s) {
            return new JsonPrimitive(s);
        }
        if (value instanceof Enum<?> e) {
            return new JsonPrimitive(e.name());
        }
        if (value instanceof Collection<?> list) {
            final JsonArray a = new JsonArray();
            list.forEach(item -> a.add(toJson(item)));
            return a;
        }
        if (value instanceof Map<?, ?> map) {
            final JsonObject o = new JsonObject();
            map.forEach((k, v) -> o.add(String.valueOf(k), toJson(v)));
            return o;
        }
        throw new IllegalArgumentException("No JSON form for " + value.getClass().getName());
    }

    /** Decodes a message from the browser into the record its {@code t} names. Returns null when no record has that name. */
    public static Record decode(final JsonObject message) {
        final JsonElement t = message.get("t");
        final Class<? extends Record> type = t == null || !t.isJsonPrimitive() ? null : ByName.COMMANDS.get(t.getAsString());
        return type == null ? null : GSON.fromJson(message, type);
    }

    /** The {@code t} a command was sent under. */
    public static String commandName(final Record command) {
        final Command c = command.getClass().getAnnotation(Command.class);
        if (!c.value().isEmpty()) {
            return c.value();
        }
        return ((Enum<?>) read(components(command.getClass())[0], command)).name();
    }

    /** Every {@code t} a command record handles: its own, or each constant of its {@code t} component. */
    static List<String> commandNames(final Class<?> type) {
        final Command c = type.getAnnotation(Command.class);
        if (c == null) {
            return List.of();
        }
        if (!c.value().isEmpty()) {
            return List.of(c.value());
        }
        return constantsOf(type, "t");
    }

    private static List<String> constantsOf(final Class<?> type, final String component) {
        for (final RecordComponent rc : components(type)) {
            if (rc.getName().equals(component) && rc.getType().isEnum()) {
                final List<String> names = new ArrayList<>();
                for (final Object constant : rc.getType().getEnumConstants()) {
                    names.add(((Enum<?>) constant).name());
                }
                return names;
            }
        }
        throw new IllegalStateException(type.getSimpleName() + " names no " + component + " and has no enum " + component + " component");
    }

    static RecordComponent[] components(final Class<?> type) {
        return COMPONENTS.computeIfAbsent(type, Class::getRecordComponents);
    }

    static String nameOf(final RecordComponent c) {
        // SerializedName cannot target a record component, so Java copies it onto the component's field and accessor
        final SerializedName n = c.getAccessor().getAnnotation(SerializedName.class);
        return n == null ? c.getName() : n.value();
    }

    private static Object read(final RecordComponent c, final Record record) {
        try {
            return c.getAccessor().invoke(record);
        } catch (final IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Cannot read " + c.getName(), e);
        }
    }

    /** The command records by each name they handle. Built on first use, because FromBrowser's list is made through this class. */
    private static final class ByName {
        static final Map<String, Class<? extends Record>> COMMANDS = new HashMap<>();

        static {
            for (final Class<? extends Record> type : FromBrowser.COMMANDS) {
                commandNames(type).forEach(name -> COMMANDS.put(name, type));
            }
        }
    }
}
