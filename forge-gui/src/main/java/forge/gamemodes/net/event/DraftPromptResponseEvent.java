package forge.gamemodes.net.event;

import java.util.List;

/** Client -> server: the option indices a seat chose for a draft prompt. */
public final class DraftPromptResponseEvent implements NetEvent {
    private static final long serialVersionUID = 1L;
    private final int seatIndex;
    private final int promptId;
    private final List<Integer> chosen;

    public DraftPromptResponseEvent(int seatIndex, int promptId, List<Integer> chosen) {
        this.seatIndex = seatIndex;
        this.promptId = promptId;
        this.chosen = List.copyOf(chosen);
    }

    public int getSeatIndex() { return seatIndex; }
    public int getPromptId() { return promptId; }
    public List<Integer> getChosen() { return chosen; }
}
