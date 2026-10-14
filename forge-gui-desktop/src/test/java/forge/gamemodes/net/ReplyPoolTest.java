package forge.gamemodes.net;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ReplyPoolTest {

    @Test(timeOut = 5_000)
    public void lateReplyAfterCancellationDoesNotBreakNewReply() {
        ReplyPool pool = new ReplyPool();
        Object staleReply = new Object();
        Object currentReply = new Object();

        pool.initialize(201);
        pool.cancelAll();
        pool.initialize(202);

        pool.complete(201, staleReply);
        pool.complete(202, currentReply);

        Assert.assertSame(pool.get(202), currentReply);
    }
}
