package forge.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class LobbyTeamsTest extends WebHubTest {
    /** Each seat's team, with -1 for a seat that the table shows no team for. */
    private static List<Integer> teams(final JsonObject table) {
        final List<Integer> out = new ArrayList<>();
        for (final JsonElement seat : table.getAsJsonArray("seats")) {
            final JsonElement team = seat.getAsJsonObject().get("team");
            out.add(team == null || team.isJsonNull() ? -1 : team.getAsInt());
        }
        return out;
    }

    // Fails if a free-for-all table shows teams, dealing two teams does not alternate the seats, a seat can be moved to a team the table does not have, a seat added to a table with teams does not join the smaller team, the host can start with every seat on one team, or going back to a free-for-all leaves a team showing
    @Test(timeOut = 120_000)
    public void teamsAreDealtKeptAndCleared() throws Exception {
        // Added seats go to the AI, so every seat plays and the team rule is not hidden behind open seats
        final TestBrowser host = hostAt("lobby");
        sessions.onMessage(host, message("setPlayerCount", "count", 4));
        final JsonObject four = host.awaitLobby(l -> l.getAsJsonArray("seats").size() == 4);
        Assert.assertEquals(teams(four), List.of(-1, -1, -1, -1), "a free-for-all table shows teams");
        Assert.assertEquals(four.get("teams").getAsInt(), 0);

        sessions.onMessage(host, message("setTeams", "count", 2));
        final JsonObject dealt = host.awaitLobby(l -> l.get("teams").getAsInt() == 2);
        Assert.assertEquals(teams(dealt), List.of(0, 1, 0, 1));

        sessions.onMessage(host, message("setTeam", "index", 1, "team", 5));
        sessions.onMessage(host, message("setTeam", "index", 1, "team", 0));
        final JsonObject moved = host.awaitLobby(l -> teams(l).equals(List.of(0, 0, 0, 1)));
        Assert.assertEquals(moved.get("teams").getAsInt(), 2);

        sessions.onMessage(host, message("setPlayerCount", "count", 5));
        final JsonObject five = host.awaitLobby(l -> l.getAsJsonArray("seats").size() == 5);
        Assert.assertEquals(teams(five), List.of(0, 0, 0, 1, 1));
        Assert.assertEquals(five.get("teams").getAsInt(), 2);

        // Another problem can already stop the start, so one side must add a problem of its own
        sessions.onMessage(host, message("setTeam", "index", 3, "team", 0));
        sessions.onMessage(host, message("setTeam", "index", 4, "team", 0));
        final JsonObject oneSide = host.awaitLobby(l -> teams(l).equals(List.of(0, 0, 0, 0, 0)));
        Assert.assertFalse(oneSide.get("canStart").getAsBoolean(), "a table with one side can start");
        Assert.assertTrue(oneSide.getAsJsonArray("problems").size() > five.getAsJsonArray("problems").size(),
                "a table with one side has no more problems than one with two: " + oneSide.getAsJsonArray("problems"));

        sessions.onMessage(host, message("setTeams", "count", 0));
        final JsonObject cleared = host.awaitLobby(l -> l.get("teams").getAsInt() == 0);
        Assert.assertEquals(teams(cleared), List.of(-1, -1, -1, -1, -1), "going back to a free-for-all left a team showing");
    }
}
