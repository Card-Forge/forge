package forge.web.protocol;

import forge.game.phase.PhaseType;
import forge.web.protocol.MessageJson.Command;
import forge.web.protocol.MessageJson.Nullable;

import java.util.List;

/** Every message the browser sends the server. src/main/ts/protocol.gen.ts is generated from these. */
public final class FromBrowser {
    private FromBrowser() {
    }

    /** Messages that are only a name. */
    public enum Plain { decks, join, lobby, leaveLobby, addresses, cardSets, netDecks, leave, limitedLeave, poolClose, draftDiscard, gauntletNext, gauntletRestart, eventStart, eventNew,
        ok, cancel, endTurn, stopYield, autoPass, undo, concede, macroRecord, macroRepeat,
        editorClose, editorUndo }

    @Command
    public record Bare(Plain t) {
    }

    /** Takes the host's seat. A browser on another machine gives the host password when one is set. */
    @Command("claimHost")
    public record ClaimHost(@Nullable String password) {
    }

    /** The name and avatar this browser plays under. Both are chosen before anything else. A guest's browser also sends the sleeve it last chose. */
    @Command("setName")
    public record SetName(String name, @Nullable Integer avatar, @Nullable Integer sleeve) {
    }

    /** A script error in the page. Without this message, the server's log would never record it. */
    @Command("pageError")
    public record PageError(String text) {
    }

    @Command("hostChoice")
    public record HostChoiceAnswer(int id, List<Integer> value) {
    }

    @Command("start")
    public record Start(boolean spectate) {
    }

    /** Sets every stop of one row instead of toggling them, so a browser can restore its remembered stops. Null phases means Forge's defaults. */
    @Command("setStops")
    public record SetStops(boolean mine, @Nullable List<PhaseType> phases) {
    }

    @Command("setSetting")
    public record SetSetting(String key, String value) {
    }

    /** Every command record. MessageJson decodes commands by these, and the TypeScript is generated from them. */
    static final List<Class<? extends Record>> COMMANDS = MessageJson.marked(Command.class);
}
