package forge.game.ability.effects;

import forge.game.card.Card;

//Class for an effect that acts as its own card instead of being attached to a card
//Example: Commander Effect
public class DetachedCardEffect extends Card {
    private Card card; //card linked to effect

    public DetachedCardEffect(Card card0, String name0) {
        super(card0.getOwner().getGame().nextCardId(), card0.getPaperCard(), card0.getOwner().getGame());
        card = card0;

        setName(name0);
        setOwner(card0.getOwner());
        setImmutable(true);

        setEffectSource(card0);
    }

    @Override
    public Card getCardForUi() {
        return card; //use linked card for the sake of UI display logic
    }
}
