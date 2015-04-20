package forge.net.event;

import java.io.Serializable;

import forge.net.server.RemoteClient;

public final class ReplyEvent implements NetEvent {
    private static final long serialVersionUID = -2814651319617795386L;

    private final int index;
    private final Serializable reply;
    public ReplyEvent(final int index, final Serializable reply) {
        this.index = index;
        this.reply = reply;
    }

    public int getIndex() {
        return index;
    }
    public Object getReply() {
        return reply;
    }

    @Override public void updateForClient(final RemoteClient client) {
    }

    @Override
    public String toString() {
        return String.format("Reply (%d): %s", index, reply);
    }
}
