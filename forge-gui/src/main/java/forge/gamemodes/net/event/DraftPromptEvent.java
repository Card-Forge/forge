package forge.gamemodes.net.event;

import forge.gamemodes.limited.DraftPrompt;
import forge.gamemodes.net.server.RemoteClient;

/** Server -> one seat: a choice the draft asks that seat to make. */
public final class DraftPromptEvent implements NetEvent {
    private static final long serialVersionUID = 1L;
    private final DraftPrompt prompt;

    public DraftPromptEvent(DraftPrompt prompt) {
        this.prompt = prompt;
    }

    public DraftPrompt getPrompt() { return prompt; }

    @Override
    public void updateForClient(RemoteClient client) { }
}
