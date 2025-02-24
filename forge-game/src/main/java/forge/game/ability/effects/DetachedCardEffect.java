package forge.game.ability.effects;

import forge.card.GamePieceType;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;

//Class for an effect that acts as its own card instead of being attached to a card
//Example: Commander Effect
public class DetachedCardEffect extends Card {
    private Card card; //card linked to effect

    public DetachedCardEffect(Card card0, String name0) {
        super(card0.getOwner().getGame().nextCardId(), card0.getPaperCard(), card0.getOwner().getGame());
        card = card0;

        this.renderForUi = false;
        setName(name0);
        setOwner(card0.getOwner());
        setGamePieceType(GamePieceType.EFFECT);

        setEffectSource(card0);
    }

    public DetachedCardEffect(Player owner, String name) {
        super(owner.getGame().nextCardId(), null, owner.getGame());
        this.card = null;
        this.renderForUi = false;

        this.setName(name);
        this.setOwner(owner);
        this.setGamePieceType(GamePieceType.EFFECT);
    }

    public DetachedCardEffect(DetachedCardEffect from, boolean assignNewId) {
        this(from, from.getGame(), assignNewId);
    }

    public DetachedCardEffect(DetachedCardEffect from, Game game, boolean assignNewId) {
        super(assignNewId ? game.nextCardId() : from.id, from.getPaperCard(), game);
        this.renderForUi = from.renderForUi;
        this.setName(from.getName());
        this.setGamePieceType(GamePieceType.EFFECT);
        if(from.getGame() == game) {
            this.setOwner(from.getOwner());
            this.setEffectSource(from.getEffectSource());
        }
    }

    @Override
    public Card getCardForUi() {
        return card; //use linked card for the sake of UI display logic
    }
}
