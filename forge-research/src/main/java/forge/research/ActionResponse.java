package forge.research;

/**
 * Payload sent from gRPC thread to game thread with the chosen action.
 */
public class ActionResponse {

    private final int actionIndex;

    public ActionResponse(int actionIndex) {
        this.actionIndex = actionIndex;
    }

    public int getActionIndex() {
        return actionIndex;
    }
}
