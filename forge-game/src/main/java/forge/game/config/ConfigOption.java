package forge.game.config;

import java.util.Objects;

public class ConfigOption<T> {

    public static OptionBuilder key(String key) {
        if (key == null) {
            throw new NullPointerException();
        }
        return new OptionBuilder(key);
    }

    public static class OptionBuilder {
        private final String key;

        OptionBuilder(String key) {
            this.key = key;
        }

        public TypedConfigOptionBuilder<Boolean> booleanType() {
            return new TypedConfigOptionBuilder<>(key, Boolean.class);
        }

        public TypedConfigOptionBuilder<Integer> intType() {
            return new TypedConfigOptionBuilder<>(key, Integer.class);
        }

        public TypedConfigOptionBuilder<Long> longType() {
            return new TypedConfigOptionBuilder<>(key, Long.class);
        }

        public TypedConfigOptionBuilder<Float> floatType() {
            return new TypedConfigOptionBuilder<>(key, Float.class);
        }

        public TypedConfigOptionBuilder<Double> doubleType() {
            return new TypedConfigOptionBuilder<>(key, Double.class);
        }

        public TypedConfigOptionBuilder<String> stringType() {
            return new TypedConfigOptionBuilder<>(key, String.class);
        }

        public <T extends Enum<T>> TypedConfigOptionBuilder<T> enumType(Class<T> enumClass) {
            return new TypedConfigOptionBuilder<>(key, enumClass);
        }
    }

    public static class TypedConfigOptionBuilder<T> {
        private final String key;
        private final Class<T> clazz;
        private T defaultValue;
        private String description;

        TypedConfigOptionBuilder(String key, Class<T> clazz) {
            this.key = key;
            this.clazz = clazz;
        }

        public ListConfigOptionBuilder<T> asList() {
            return new ListConfigOptionBuilder<>(key, clazz);
        }

        public TypedConfigOptionBuilder<T> defaultValue(T value) {
            this.defaultValue = value;
            return this;
        }

        public TypedConfigOptionBuilder<T> withDescription(String description) {
            this.description = description;
            return this;
        }

        public ConfigOption<T> build() {
            return new ConfigOption<>(this);
        }
    }

    public static class ListConfigOptionBuilder<E> {
        private final String key;
        private final Class<E> clazz;

        ListConfigOptionBuilder(String key, Class<E> clazz) {
            this.key = key;
            this.clazz = clazz;
        }
    }

    private final String key;
    private final T defaultValue;
    private final String description;
    private final Class<?> clazz;

    public ConfigOption(TypedConfigOptionBuilder<T> builder) {
        this.key = builder.key;
        this.description = builder.description;
        this.defaultValue = builder.defaultValue;
        this.clazz = builder.clazz;
    }
    ConfigOption(String key, String description, T defaultValue, Class<?> clazz) {
        this.key = key;
        this.description = description;
        this.defaultValue = defaultValue;
        this.clazz = clazz;
    }

    public String key() {
        return key;
    }

    public boolean hasDefaultValue() {
        return defaultValue != null;
    }

    public T defaultValue() {
        return defaultValue;
    }

    public String description() {
        return description;
    }

    Class<?> getClazz() {
        return clazz;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ConfigOption<?> that = (ConfigOption<?>) o;
        return Objects.equals(key, that.key) && Objects.equals(defaultValue, that.defaultValue) && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, defaultValue, description);
    }

    @Override
    public String toString() {
        return String.format("Key: '%s' , default: %s", key, defaultValue);
    }
}
