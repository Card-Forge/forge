package forge.web;

import com.google.gson.JsonObject;
import forge.web.protocol.ToBrowser.HostChoiceKind;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class HostQuestionsTest {
    @BeforeClass
    public void setUp() {
        WebTestSupport.initModel();
    }

    private static List<Integer> ask(final HostQuestions requests) {
        return requests.ask(HostChoiceKind.confirm, "Title", "Keep going?", List.of("Yes", "No"), 1, 1, null, null);
    }

    // Fails if an unanswered question holds its thread past the time limit, returns an answer, or is asked again of a browser that reloads
    @Test(timeOut = 10_000)
    public void anUnansweredQuestionGivesUpAndIsWithdrawn() throws Exception {
        final HostQuestions requests = new HostQuestions(100);
        final List<JsonObject> asked = new CopyOnWriteArrayList<>();
        requests.setSink(asked::add);
        final CompletableFuture<List<Integer>> waiting = CompletableFuture.supplyAsync(() -> ask(requests));
        Assert.assertNull(waiting.get(5, TimeUnit.SECONDS), "a question nobody answered returned an answer");
        Assert.assertEquals(asked.size(), 1, "the question never reached the browser");

        requests.answer(asked.get(0).get("id").getAsInt(), List.of(0));
        final List<JsonObject> replayed = new CopyOnWriteArrayList<>();
        requests.replay(replayed::add);
        Assert.assertTrue(replayed.isEmpty(), "a question that gave up was asked again");
    }

    // Fails if an answer does not reach the thread that asked, or an open question is not asked again of a browser that reloads
    @Test(timeOut = 10_000)
    public void anOpenQuestionIsAskedAgainAndItsAnswerReturned() throws Exception {
        final HostQuestions requests = new HostQuestions(60_000);
        final List<JsonObject> asked = new CopyOnWriteArrayList<>();
        requests.setSink(asked::add);
        final CompletableFuture<List<Integer>> waiting = CompletableFuture.supplyAsync(() -> ask(requests));
        WebHubTest.awaitTrue(() -> !asked.isEmpty(), "the question never reached the browser");
        final List<JsonObject> replayed = new CopyOnWriteArrayList<>();
        requests.replay(replayed::add);
        Assert.assertEquals(replayed, asked);
        requests.answer(asked.get(0).get("id").getAsInt(), List.of(1));
        Assert.assertEquals(waiting.get(5, TimeUnit.SECONDS), List.of(1));
    }
}
