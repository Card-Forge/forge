package forge.gamemodes.match;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SuggestionTypeShould {

    @Test
    public void persistEachTypeToItsOwnPreferenceKey() {
        // scopePref() is a plain getter, so we do not pin the exact constant→FPref pairing (that
        // only re-states the enum's own declaration). What matters as an invariant is that the keys
        // are unique: two SuggestionTypes sharing a scopePref() would make them overwrite each
        // other's persisted decline scope. This scales as new constants are added.
        assertThat(SuggestionType.values())
                .extracting(SuggestionType::scopePref)
                .doesNotHaveDuplicates();
    }
}
