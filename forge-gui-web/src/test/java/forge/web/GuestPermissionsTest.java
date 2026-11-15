package forge.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import forge.deck.Deck;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.Localizer;
import forge.util.storage.IStorage;
import forge.web.protocol.ToBrowser.HostChoiceKind;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Tests that a browser on a guest's link is refused what only the host may do. */
public class GuestPermissionsTest extends WebHubTest {
    private WebGuiBase ui;

    @Override
    WebGuiBase gui() {
        ui = new WebGuiBase();
        return ui;
    }

    // Fails if a guest can change the host's deck generation, open the host's Limited pages, or change the host's preferences through a setting
    @Test(timeOut = 60_000)
    public void aGuestCannotUseTheHostsProfile() throws Exception {
        final String singletons = FModel.getPreferences().getPref(FPref.DECKGEN_SINGLETONS);
        final String artifacts = FModel.getPreferences().getPref(FPref.DECKGEN_ARTIFACTS);
        final String volume = FModel.getPreferences().getPref(FPref.UI_VOL_SOUNDS);
        final TestBrowser guest = connect("guest");
        guest.awaitNewest("hello", "the guest was never greeted");
        sessions.onMessage(guest, message("setDeckGen", "singletons", !Boolean.parseBoolean(singletons), "noArtifacts", !Boolean.parseBoolean(artifacts)));
        sessions.onMessage(guest, message("limitedOpen", "kind", "sealed"));
        sessions.onMessage(guest, message("setSetting", "key", "soundVolume", "value", "37".equals(volume) ? "38" : "37"));
        // The host writes its preferences on its UI thread, so any write a guest caused has happened once that thread is idle
        onUi(() -> { });
        Assert.assertEquals(FModel.getPreferences().getPref(FPref.DECKGEN_SINGLETONS), singletons);
        Assert.assertEquals(FModel.getPreferences().getPref(FPref.DECKGEN_ARTIFACTS), artifacts);
        Assert.assertEquals(FModel.getPreferences().getPref(FPref.UI_VOL_SOUNDS), volume, "a guest's setting changed the host's preferences");
        Assert.assertNotEquals(guest.hello.get("screen").getAsString(), "event", "a guest opened the host's Limited pages");
    }

    // Fails if a guest's answer to a question for the host counts, or the host's own answer does not
    @Test(timeOut = 60_000)
    public void onlyTheHostAnswersTheHostsQuestion() throws Exception {
        final TestBrowser host = connect("host");
        sessions.onMessage(host, TestBrowser.action("claimHost"));
        host.awaitMatching("hello", h -> h.get("host").getAsBoolean(), "the host's seat was not taken");
        final TestBrowser guest = connect("guest");
        final CompletableFuture<List<Integer>> waiting = CompletableFuture.supplyAsync(() ->
                ui.hostQuestions().ask(HostChoiceKind.confirm, "Title", "Keep going?", List.of("Yes", "No"), 1, 1, null, null));
        final int id = host.awaitMatching("hostChoice", m -> true, "the question never reached the host").get("id").getAsInt();
        sessions.onMessage(guest, answer(id, 1));
        sessions.onMessage(host, answer(id, 0));
        Assert.assertEquals(waiting.get(10, TimeUnit.SECONDS), List.of(0), "the guest's answer was taken");
    }

    // Fails if a guest can delete a deck from the host's own storage, or the host cannot
    @Test(timeOut = 60_000)
    public void onlyTheHostDeletesTheHostsDeck() throws Exception {
        final IStorage<Deck> stored = FModel.getDecks().getConstructed();
        final Deck deck = TestDecks.of("Guest Delete Test", "Forest", 60);
        stored.add(deck);
        try {
            final TestBrowser guest = connect("guest");
            guest.awaitNewest("hello", "the guest was never greeted");
            final String key = listedKey(guest, deck.getName());
            send(guest, message("deckDelete", "key", key));
            // A refused delete sends its notice before the deck list, which is not sent again when it has not changed
            guest.awaitMatching("notice", n -> Localizer.getInstance().getMessage("lblWebEditorOnlyOwnDelete").equals(str(n, "title")),
                    "the guest's delete was never answered");
            Assert.assertTrue(stored.contains(deck.getName()), "a guest deleted the host's deck");

            final TestBrowser host = connect("host");
            sessions.onMessage(host, TestBrowser.action("claimHost"));
            host.awaitMatching("hello", h -> h.get("host").getAsBoolean(), "the host's seat was not taken");
            send(host, message("deckDelete", "key", listedKey(host, deck.getName())));
            host.awaitMatching("decks", d -> !d.toString().contains(key), "the host's deck list still shows the deleted deck");
            Assert.assertFalse(stored.contains(deck.getName()), "the host could not delete its own deck");
        } finally {
            if (stored.contains(deck.getName())) {
                stored.delete(deck.getName());
            }
        }
    }

    /** Asks for the browser's deck list and returns the key of the host's deck with this name. A list identical to one already sent is not sent again, so earlier lists count. */
    private String listedKey(final TestBrowser browser, final String name) throws InterruptedException {
        sessions.onMessage(browser, TestBrowser.action("decks"));
        final JsonObject list = browser.awaitMatching("decks", d -> d.toString().contains(name), "the deck list never showed " + name);
        for (final var d : list.getAsJsonArray("decks")) {
            if (name.equals(str(d.getAsJsonObject(), "name")) && str(d.getAsJsonObject(), "key").startsWith("yours:")) {
                return str(d.getAsJsonObject(), "key");
            }
        }
        throw new AssertionError("the host's deck " + name + " is not in the list");
    }

    private static JsonObject answer(final int id, final int choice) {
        final JsonObject m = message("hostChoice", "id", id);
        final JsonArray value = new JsonArray();
        value.add(choice);
        m.add("value", value);
        return m;
    }
}
