package forge.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import forge.gamemodes.net.ProtocolMethod;
import forge.gui.interfaces.IGuiGame;
import forge.util.Localizer;
import forge.web.match.WebGuiGame;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class WebGuiGameTest {
    private WebGuiGame gui;
    private TestBrowser browser;

    @BeforeClass
    public void setUpModel() {
        WebTestSupport.initModel();
    }

    @BeforeMethod
    public void setUp() {
        gui = new WebGuiGame(PlayerSettings.saved());
        browser = new TestBrowser();
        gui.attach(browser);
    }

    // Fails if a list choice is not sent to the browser with its labels, or the index the browser replies with does not come back as the matching item
    @Test
    public void getChoicesMapsReplyIndicesBackToObjects() throws Exception {
        final CompletableFuture<List<String>> picked = CompletableFuture.supplyAsync(
                () -> gui.getChoices("Pick one", 1, 1, List.of("a", "b", "c"), null, null));
        final JsonObject request = browser.awaitNewest("request", "no request reached the browser");
        Assert.assertEquals(request.get("kind").getAsString(), "choices");
        Assert.assertEquals(request.getAsJsonArray("options").get(2).getAsJsonObject().get("label").getAsString(), "c");
        final JsonArray choice = new JsonArray();
        choice.add(2);
        TestBrowser.command(gui, TestBrowser.reply(request.get("id").getAsInt(), choice));
        Assert.assertEquals(picked.get(60, TimeUnit.SECONDS), List.of("c"));
    }

    // Fails if a choice of one optional cost is sent as a plain list instead of a cost menu, or declining it does not return an empty choice
    @Test
    public void oneOptionalCostIsAskedAsACostMenu() throws Exception {
        final String title = Localizer.getInstance().getMessage("lblChooseOptionalCosts");
        final CompletableFuture<List<String>> picked = CompletableFuture.supplyAsync(
                () -> gui.getChoices(title, 0, 1, List.of("Flash"), null, null));
        final JsonObject request = browser.awaitNewest("request", "no request reached the browser");
        Assert.assertEquals(request.get("kind").getAsString(), "costs");
        TestBrowser.command(gui, TestBrowser.reply(request.get("id").getAsInt(), new JsonArray()));
        Assert.assertEquals(picked.get(60, TimeUnit.SECONDS), List.of());
    }

    // Fails if a choice of several optional costs is sent as an ordering instead of a cost menu that allows them all, or the picked cost does not come back
    @Test
    public void severalOptionalCostsAreAskedAsACostMenuNotAnOrder() throws Exception {
        final String title = Localizer.getInstance().getMessage("lblChooseOptionalCosts");
        final CompletableFuture<IGuiGame.OrderResult<String>> picked = CompletableFuture.supplyAsync(
                () -> gui.order(title, "Optional Costs", 0, 2, List.of("Kicker", "Flash"), List.of(), null, false, false));
        final JsonObject request = browser.awaitNewest("request", "no request reached the browser");
        Assert.assertEquals(request.get("kind").getAsString(), "costs");
        Assert.assertEquals(request.get("max").getAsInt(), 2);
        final JsonArray choice = new JsonArray();
        choice.add(1);
        TestBrowser.command(gui, TestBrowser.reply(request.get("id").getAsInt(), choice));
        Assert.assertEquals(picked.get(60, TimeUnit.SECONDS).ordered(), List.of("Flash"));
    }

    // Fails if conceding leaves an open request waiting instead of answering it with its default
    @Test
    public void concedeAnswersOpenRequestsWithDefaults() throws Exception {
        final CompletableFuture<List<String>> picked = CompletableFuture.supplyAsync(
                () -> gui.getChoices("Pick one", 1, 1, List.of("a", "b"), null, null));
        browser.awaitNewest("request", "no request reached the browser");
        TestBrowser.command(gui, TestBrowser.action("concede"));
        Assert.assertEquals(picked.get(60, TimeUnit.SECONDS), List.of("a"));
    }

    // Fails if a reloaded browser is not sent the request that is still open, or cannot answer it
    @Test
    public void reloadReplaysTheOpenRequest() throws Exception {
        final CompletableFuture<Boolean> answer = CompletableFuture.supplyAsync(
                () -> gui.showConfirmDialog("Keep?", "Mulligan", "Keep", "Mulligan", true));
        final JsonObject request = browser.awaitNewest("request", "no request reached the browser");
        final TestBrowser reloaded = new TestBrowser();
        gui.attach(reloaded);
        Assert.assertEquals(reloaded.last("request").get("id"), request.get("id"));
        TestBrowser.command(gui, TestBrowser.reply(request.get("id").getAsInt(), new JsonPrimitive(1)));
        Assert.assertFalse(answer.get(60, TimeUnit.SECONDS));
    }

    // Fails if a host question that needs an answer runs the version WebGuiGame inherits instead of asking the browser, so the web player never sees the prompt
    @Test
    public void everyHostCallThatReturnsAValueIsImplementedHere() throws Exception {
        for (final ProtocolMethod pm : ProtocolMethod.values()) {
            final Method m = pm.getMethod();
            if (m == null || m.getDeclaringClass() != IGuiGame.class || m.getReturnType() == void.class) {
                continue;
            }
            final Method impl = WebGuiGame.class.getMethod(m.getName(), m.getParameterTypes());
            Assert.assertEquals(impl.getDeclaringClass(), WebGuiGame.class, pm.name() + " has no WebGuiGame override");
        }
    }
}
