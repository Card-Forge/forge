package forge.game.config;

import java.util.*;

public class Configuration {

    private final HashMap<String, Object> confData;

    public Configuration() {
        this.confData = new HashMap<>();
    }

    public <T> Configuration set(ConfigOption<T> option, T value) {
        setValueInternal(option.key(), value);
        return this;
    }

    public <T> T get(ConfigOption<T> option) {
        return getOptional(option).orElseGet(option::defaultValue);
    }

    public <T> T get(ConfigOption<T> configOption, T overrideDefault) {
        return getOptional(configOption).orElse(overrideDefault);
    }

    public <T> Optional<T> getOptional(ConfigOption<T> option) {
        Optional<Object> rawValue = getRawValueFromOption(option);
        Class<?> clazz = option.getClazz();
        return rawValue.map(v -> convertValue(v, clazz));
    }

    private <T> Optional<Object> getRawValueFromOption(ConfigOption<T> option) {
        if (this.confData.containsKey(option.key())) {
            return Optional.of(confData.get(option.key()));
        }
        return Optional.empty();
    }

    private <T> void setValueInternal(String key, T value) {
        if (key == null) {
            throw new NullPointerException("Key must not be null.");
        }
        if (value == null) {
            throw new NullPointerException("Value must not be null.");
        }

        synchronized (this.confData) {
            this.confData.put(key, value);
        }
    }

    private <T> T convertValue(Object rawValue, Class<?> clazz) {
        if (Integer.class.equals(clazz)) {
            return (T) convertToInt(rawValue);
        } else if (Long.class.equals(clazz)) {
            return (T) convertToLong(rawValue);
        } else if (Boolean.class.equals(clazz)) {
            return (T) convertToBoolean(rawValue);
        } else if (Float.class.equals(clazz)) {
            return (T) convertToFloat(rawValue);
        } else if (Double.class.equals(clazz)) {
            return (T) convertToDouble(rawValue);
        } else if (String.class.equals(clazz)) {
            return (T) convertToString(rawValue);
        } else if (clazz.isEnum()) {
            return (T) convertToEnum(rawValue, (Class<? extends Enum<?>>) clazz);
        }

        throw new IllegalArgumentException("Unsupported type: " + clazz);
    }

    private Integer convertToInt(Object o) {
        if (o.getClass() == Integer.class) {
            return (Integer) o;
        } else if (o.getClass() == Long.class) {
            long value = (Long) o;
            if (value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE) {
                return (int) value;
            } else {
                throw new IllegalArgumentException(
                        String.format(
                                "Configuration value %s overflows/underflows the integer type.",
                                value));
            }
        }

        return Integer.parseInt(o.toString());
    }

    private Long convertToLong(Object o) {
        if (o.getClass() == Long.class) {
            return (Long) o;
        } else if (o.getClass() == Integer.class) {
            return ((Integer) o).longValue();
        }

        return Long.parseLong(o.toString());
    }

    private Boolean convertToBoolean(Object o) {
        if (o.getClass() == Boolean.class) {
            return (Boolean) o;
        }

        return switch (o.toString().toUpperCase()) {
            case "TRUE" -> true;
            case "FALSE" -> false;
            default -> throw new IllegalArgumentException(
                    String.format(
                            "Unrecognized option for boolean: %s. Expected either true or false(case insensitive)",
                            o));
        };
    }

    private Float convertToFloat(Object o) {
        if (o.getClass() == Float.class) {
            return (Float) o;
        } else if (o.getClass() == Double.class) {
            double value = ((Double) o);
            if (value == 0.0
                    || (value >= Float.MIN_VALUE && value <= Float.MAX_VALUE)
                    || (value >= -Float.MAX_VALUE && value <= -Float.MIN_VALUE)) {
                return (float) value;
            } else {
                throw new IllegalArgumentException(
                        String.format(
                                "Configuration value %s overflows/underflows the float type.",
                                value));
            }
        }

        return Float.parseFloat(o.toString());
    }

    static Double convertToDouble(Object o) {
        if (o.getClass() == Double.class) {
            return (Double) o;
        } else if (o.getClass() == Float.class) {
            return ((Float) o).doubleValue();
        }

        return Double.parseDouble(o.toString());
    }

    static String convertToString(Object o) {
        if (o.getClass() == String.class) {
            return (String) o;
        }

        return o.toString();
    }

    @SuppressWarnings("unchecked")
    public static <E extends Enum<?>> E convertToEnum(Object o, Class<E> clazz) {
        if (o.getClass().equals(clazz)) {
            return (E) o;
        }

        return Arrays.stream(clazz.getEnumConstants())
                .filter(e -> e.toString().toUpperCase(Locale.ROOT).equals(o.toString().toUpperCase(Locale.ROOT)))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Could not parse value for enum %s. Expected one of: [%s]",
                                clazz,
                                Arrays.toString(clazz.getEnumConstants())
                        )
                ));
    }

}
