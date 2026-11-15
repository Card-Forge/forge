package forge.web.protocol;

import forge.web.protocol.ToBrowser.ErrorMessage;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Tests the rules used to write and read every protocol record. The tests run with assertions on, as surefire does. */
public class MessageJsonTest {

    // Fails if a record with a null in a field not marked nullable is written without an error
    @Test
    public void aNullInAFieldNotMarkedNullableIsCaught() {
        Assert.expectThrows(AssertionError.class, () -> MessageJson.encode(new ErrorMessage(null)));
    }
}
