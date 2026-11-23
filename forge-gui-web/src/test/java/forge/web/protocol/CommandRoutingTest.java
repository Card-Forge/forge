package forge.web.protocol;

import com.google.gson.JsonObject;
import forge.web.TableCommands;
import forge.web.WebSession;
import forge.web.deck.DeckWorkspace;
import forge.web.limited.OfflineLimited;
import forge.web.match.WebGuiGame;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/** The session passes each command to the one of these that owns it, so each command must have exactly one owner. */
public class CommandRoutingTest {
    private static final List<Commands<?>> OWNERS = List.of(WebSession.COMMANDS, TableCommands.COMMANDS, DeckWorkspace.COMMANDS, OfflineLimited.COMMANDS,
            WebGuiGame.COMMANDS);

    // Fails if a command name is read by another record or does not survive a round trip, so one record takes another's messages, or a command has no owner or two, so it is dropped or handled by the first owner only
    @Test
    public void everyCommandIsReadByItsRecordAndHasOneOwner() {
        final List<String> wrong = new ArrayList<>();
        for (final Class<? extends Record> c : FromBrowser.COMMANDS) {
            for (final String name : MessageJson.commandNames(c)) {
                final JsonObject message = new JsonObject();
                message.addProperty("t", name);
                final Record command = MessageJson.decode(message);
                Assert.assertEquals(command.getClass(), c, name);
                Assert.assertEquals(MessageJson.commandName(command), name);
                final long owners = OWNERS.stream().filter(o -> o.handles(command)).count();
                if (owners != 1) {
                    wrong.add(name + " has " + owners + " owners");
                }
            }
        }
        Assert.assertTrue(wrong.isEmpty(), String.join(", ", wrong));
    }
}
