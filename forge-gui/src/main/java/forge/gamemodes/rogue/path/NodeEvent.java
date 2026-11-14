package forge.gamemodes.rogue.path;

import forge.gamemodes.rogue.RogueEvent;
import forge.gamemodes.rogue.RoguePlanebound;

/**
 * Represents an Event node in a Rogue Commander path.
 * Events present the player with choices that have various outcomes.
 */
public class NodeEvent extends RoguePathNode {

    private RogueEvent event;
    private transient RoguePlanebound eventPlanebound;

    public NodeEvent() {
        super();
    }

    public NodeEvent(RogueEvent event) {
        super();
        this.event = event;
    }

    public RogueEvent getEvent() { return event; }
    public void setEvent(RogueEvent event) { this.event = event; }

    public RoguePlanebound getEventPlanebound() { return eventPlanebound; }
    public void setEventPlanebound(RoguePlanebound pb) { this.eventPlanebound = pb; }

    @Override
    public boolean isSideNode() {
        return true;
    }

    @Override
    public String toString() {
        return event != null ? "Event (" + event.getDisplayName() + ")" : "Event (???)";
    }
}
