package forge.game.event;

import forge.game.GameEntity;
import forge.game.GameEntityView;
import forge.game.card.Card;
import forge.game.card.CardView;

public record GameEventCardAttachment(CardView equipment, GameEntityView oldEntity, GameEntityView newTarget) implements GameEvent {

    public GameEventCardAttachment(Card equipment, GameEntity oldEntity, GameEntity newTarget) {
        this(CardView.get(equipment), GameEntityView.get(oldEntity), GameEntityView.get(newTarget));
    }

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
