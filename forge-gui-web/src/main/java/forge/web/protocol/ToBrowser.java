package forge.web.protocol;

import forge.gamemodes.net.DeltaPacket;
import forge.web.protocol.MessageJson.Event;
import forge.web.protocol.MessageJson.Message;
import forge.web.protocol.MessageJson.Request;

import java.util.List;

/** Every message the server sends the browser, as records. The build generates src/main/ts/protocol.gen.ts from this file, so a changed record fails the build wherever the browser reads it. */
public final class ToBrowser {
    private ToBrowser() {
    }

    /** A pointer to an object in the game's table, by its delta key. */
    public record Ref(int ref) {
        public static Ref card(final int id) {
            return new Ref(DeltaPacket.makeDeltaKey(DeltaPacket.TYPE_CARD_VIEW, id));
        }

        public static Ref player(final int id) {
            return new Ref(DeltaPacket.makeDeltaKey(DeltaPacket.TYPE_PLAYER_VIEW, id));
        }
    }

    static final List<Class<? extends Record>> EVENTS = MessageJson.marked(Event.class);

    /** Every message record. The TypeScript is generated from these. */
    static final List<Class<? extends Record>> MESSAGES = MessageJson.marked(Message.class);

    static final List<Class<? extends Record>> REQUESTS = MessageJson.marked(Request.class);
}
