package forge.game.event;

import forge.game.GameEntity;
import forge.game.card.Card;

public class GameEventCardAttachment extends GameEvent {

    public final Card equipment;
    public final GameEntity newTarget; // can enchant player, I'm ssaving a class to enchants - it could be incorrect.
    public final GameEntity oldEntiy;

    public GameEventCardAttachment(Card attachment, GameEntity formerEntity, GameEntity newEntity) {
        this.equipment = attachment;
        this.newTarget = newEntity;
        this.oldEntiy = formerEntity;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
