package forge.game.config;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import static forge.game.config.ConfigOption.key;

public class ConfigurationTest {

    @Test
    public void testConfiguration() {

        enum TestEnum {
            FOO, BAR, BAT
        }

        ConfigOption<Boolean> BOOLEAN_CONFIG_TEST =
                key("test.boolean-value")
                        .booleanType()
                        .defaultValue(true)
                        .withDescription("a test for boolean value")
                        .build();

        ConfigOption<Integer> INT_CONFIG_TEST =
                key("test.integer-value")
                        .intType()
                        .defaultValue(Integer.MAX_VALUE)
                        .withDescription("a test for integers")
                        .build();

        ConfigOption<Long> LONG_CONFIG_TEST =
                key("test.long-value")
                        .longType()
                        .defaultValue(Long.MAX_VALUE)
                        .withDescription("a test for longs")
                        .build();

        ConfigOption<Float> FLOAT_CONFIG_TEST =
                key("test.float-value")
                        .floatType()
                        .defaultValue(Float.MAX_VALUE)
                        .withDescription("a test for integers")
                        .build();

        ConfigOption<Double> DOUBLE_CONFIG_TEST =
                key("test.double-value")
                        .doubleType()
                        .defaultValue(Double.MAX_VALUE)
                        .withDescription("a test for integers")
                        .build();

        ConfigOption<String> STRING_CONFIG_TEST =
                key("test.string-value")
                        .stringType()
                        .defaultValue("A String Value")
                        .withDescription("a test for Strings")
                        .build();

        ConfigOption<TestEnum> ENUM_CONFIG_TEST =
                key("test.enum-value")
                        .enumType(TestEnum.class)
                        .defaultValue(TestEnum.FOO)
                        .withDescription("a test for enums")
                        .build();

        final Configuration config = new Configuration();

        // Check default values
        AssertJUnit.assertSame(Boolean.TRUE, config.get(BOOLEAN_CONFIG_TEST));
        AssertJUnit.assertEquals(Integer.MAX_VALUE, (int) config.get(INT_CONFIG_TEST));
        AssertJUnit.assertEquals(Long.MAX_VALUE, (long) config.get(LONG_CONFIG_TEST));
        AssertJUnit.assertEquals(Float.MAX_VALUE, config.get(FLOAT_CONFIG_TEST));
        AssertJUnit.assertEquals(Double.MAX_VALUE, config.get(DOUBLE_CONFIG_TEST));
        AssertJUnit.assertEquals("A String Value", config.get(STRING_CONFIG_TEST));
        AssertJUnit.assertEquals(TestEnum.FOO, config.get(ENUM_CONFIG_TEST));

        // Check overrides
        AssertJUnit.assertSame(Boolean.FALSE, config.get(BOOLEAN_CONFIG_TEST, false));
        AssertJUnit.assertEquals(666, (int) config.get(INT_CONFIG_TEST, 666));
        AssertJUnit.assertEquals(1_000_000_000L, (long) config.get(LONG_CONFIG_TEST, 1_000_000_000L));
        AssertJUnit.assertEquals(6.02e23f, config.get(FLOAT_CONFIG_TEST, 6.02e23f));
        AssertJUnit.assertEquals(3.141592653589793, config.get(DOUBLE_CONFIG_TEST, 3.141592653589793));
        AssertJUnit.assertEquals("A Different Value", config.get(STRING_CONFIG_TEST, "A Different Value"));
        AssertJUnit.assertEquals(TestEnum.BAR, config.get(ENUM_CONFIG_TEST, TestEnum.BAR));

        config.set(BOOLEAN_CONFIG_TEST, Boolean.FALSE);
        config.set(INT_CONFIG_TEST, 666);
        config.set(LONG_CONFIG_TEST, 1_000_000_000L);
        config.set(FLOAT_CONFIG_TEST, 6.02e23f);
        config.set(DOUBLE_CONFIG_TEST, 3.141592653589793);
        config.set(STRING_CONFIG_TEST, "A Different Value");
        config.set(ENUM_CONFIG_TEST, TestEnum.BAR);

        // Check set values
        AssertJUnit.assertSame(Boolean.FALSE, config.get(BOOLEAN_CONFIG_TEST));
        AssertJUnit.assertEquals(666, (int) config.get(INT_CONFIG_TEST));
        AssertJUnit.assertEquals(1_000_000_000L, (long) config.get(LONG_CONFIG_TEST));
        AssertJUnit.assertEquals(6.02e23f, config.get(FLOAT_CONFIG_TEST));
        AssertJUnit.assertEquals(3.141592653589793, config.get(DOUBLE_CONFIG_TEST));
        AssertJUnit.assertEquals("A Different Value", config.get(STRING_CONFIG_TEST));
        AssertJUnit.assertEquals(TestEnum.BAR, config.get(ENUM_CONFIG_TEST));
    }
}
