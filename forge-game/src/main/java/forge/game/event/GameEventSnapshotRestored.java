package forge.game.event;

import forge.util.TextUtil;

public record GameEventSnapshotRestored(boolean start) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        if (start) {
            return TextUtil.concatWithSpace("Undo Snapshot Restoration Started");
        }

        return TextUtil.concatWithSpace("Undo Snapshot Restored");
    }
}
