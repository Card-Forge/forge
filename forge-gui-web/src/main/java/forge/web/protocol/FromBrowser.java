package forge.web.protocol;

import forge.web.protocol.MessageJson.Command;

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

    /** Every command record. MessageJson decodes commands by these, and the TypeScript is generated from them. */
    static final List<Class<? extends Record>> COMMANDS = MessageJson.marked(Command.class);
}
