package forge.game.event;

import forge.game.GameEntity;
import forge.game.card.Card;

public class GameEventCardAttachment extends GameEvent {

    public final Card equipment;
    public final GameEntity newTarget; // can enchant player, I'm saving a class to enchants - it could be incorrect.
    public final GameEntity oldEntity;

    public GameEventCardAttachment(Card attachment, GameEntity formerEntity, GameEntity newEntity) {
        this.equipment = attachment;
        this.newTarget = newEntity;
        this.oldEntity = formerEntity;
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
