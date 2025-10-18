package forge.game.event;

import forge.game.GameEntity;
import forge.game.card.Card;

public record GameEventCardAttachment(Card equipment, GameEntity oldEntity, GameEntity newTarget) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return newTarget == null ? "Detached " + equipment + " from " + oldEntity : "Attached " + equipment + (oldEntity == null ? "" : " from " + oldEntity) + " to " + newTarget;
    }
}
