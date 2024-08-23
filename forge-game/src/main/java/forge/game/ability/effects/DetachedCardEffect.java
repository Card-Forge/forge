package forge.game.ability.effects;

import forge.card.GamePieceType;
import forge.game.card.Card;
import forge.game.player.Player;

//Class for an effect that acts as its own card instead of being attached to a card
//Example: Commander Effect
public class DetachedCardEffect extends Card {
    private Card card; //card linked to effect

    public DetachedCardEffect(Card card0, String name0) {
        super(card0.getOwner().getGame().nextCardId(), card0.getPaperCard(), card0.getOwner().getGame());
        card = card0;

        setName(name0);
        setOwner(card0.getOwner());
        setGamePieceType(GamePieceType.EFFECT);

        setEffectSource(card0);
    }

    public DetachedCardEffect(Player owner, String name) {
        super(owner.getGame().nextCardId(), null, owner.getGame());
        this.card = null;

        this.setName(name);
        this.setOwner(owner);
        this.setGamePieceType(GamePieceType.EFFECT);
    }

    @Override
    public Card getCardForUi() {
        return card; //use linked card for the sake of UI display logic
    }
}
