package forge.gamemodes.match;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DeclineScopeShould {

    @Test
    public void defaultToNeverWhenGivenANullPreferenceValue() {
        assertThat(DeclineScope.fromPref(null)).isEqualTo(DeclineScope.NEVER);
    }

    @Test
    public void defaultToNeverWhenGivenAnEmptyPreferenceValue() {
        assertThat(DeclineScope.fromPref("")).isEqualTo(DeclineScope.NEVER);
    }

    @Test
    public void defaultToNeverForUnrecognizedPreferenceValues() {
        assertThat(DeclineScope.fromPref("BOGUS")).isEqualTo(DeclineScope.NEVER);
        assertThat(DeclineScope.fromPref("never")).isEqualTo(DeclineScope.NEVER); // case-sensitive
    }

    @Test
    public void roundTripThroughItsOwnName() {
        // This is the positive counterpart to the fallback tests above: every persisted FPref value
        // is written as scope.name(), so fromPref() must accept every name() it ever produces.
        // It guards the happy path of fromPref()'s hand-written parsing (not Java's valueOf, which
        // fromPref wraps in null/empty/unrecognized handling) and would catch a name that the
        // fallback logic mistakenly swallowed into NEVER.
        for (DeclineScope scope : DeclineScope.values()) {
            // Given a scope serialized via its name (e.g. stored in FPref)
            String serialized = scope.name();
            // When parsed back
            DeclineScope parsed = DeclineScope.fromPref(serialized);
            // Then it should recover the original scope
            assertThat(parsed).isEqualTo(scope);
        }
    }
}
