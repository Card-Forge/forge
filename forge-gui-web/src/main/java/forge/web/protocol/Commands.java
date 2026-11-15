package forge.web.protocol;

import java.util.HashMap;
import java.util.Map;

/** One owner's handlers for the browser's commands. Each handler is found by the name its command is sent under. */
public final class Commands<O> {
    /** Runs one command for its owner. channel is the browser that sent it. */
    @FunctionalInterface
    public interface Handler<O, T extends Record> {
        void run(O owner, BrowserChannel channel, T command);
    }

    private final Map<String, Handler<O, Record>> handlers = new HashMap<>();

    /** Handles every command name the record covers: its one name, or each constant of its t component. */
    public <T extends Record> Commands<O> on(final Class<T> type, final Handler<O, T> handler) {
        for (final String name : MessageJson.commandNames(type)) {
            add(name, handler);
        }
        return this;
    }

    /** Handles one of the commands that are only a name, such as a {@link FromBrowser.Plain} constant. */
    public Commands<O> on(final Enum<?> name, final Handler<O, Record> handler) {
        add(name.name(), handler);
        return this;
    }

    @SuppressWarnings("unchecked")
    private void add(final String name, final Handler<O, ? extends Record> handler) {
        if (handlers.put(name, (Handler<O, Record>) handler) != null) {
            throw new IllegalStateException(name + " is handled twice");
        }
    }

    public boolean handles(final Record command) {
        return handlers.containsKey(MessageJson.commandName(command));
    }

    /** Runs the command's handler. Returns false when this owner has no handler for it. */
    public boolean run(final O owner, final BrowserChannel channel, final Record command) {
        final Handler<O, Record> handler = handlers.get(MessageJson.commandName(command));
        if (handler == null) {
            return false;
        }
        handler.run(owner, channel, command);
        return true;
    }
}
