package forge.game.decision;

import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.testng.Assert.assertTrue;

public class ConfirmationDiagnosticsProfileTest {

    @Test
    public void headerContainsTypedProfileColumnAlongsideExistingConfirmationFields()
            throws ReflectiveOperationException {
        final Field headerField = ConfirmationDiagnostics.class.getDeclaredField("HEADER");
        headerField.setAccessible(true);
        final String header = (String) headerField.get(null);
        final String[] columns = header.split(",", -1);

        assertTrue(Arrays.asList(columns).contains("status"));
        assertTrue(Arrays.asList(columns).contains("reason"));
        assertTrue(Arrays.asList(columns).contains("request_id"));
        assertTrue(Arrays.asList(columns).contains("profile"),
                "confirmation diagnostics must expose a distinct sanitized profile column");
    }
}
