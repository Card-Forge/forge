package forge.gamesimulationtests.util.card;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.gamesimulationtests.util.IntegerConstraint;
import forge.gamesimulationtests.util.SpecificationHandler;
import forge.gamesimulationtests.util.player.PlayerSpecification;
import forge.gamesimulationtests.util.player.PlayerSpecificationHandler;

public class CardSpecificationHandler extends SpecificationHandler<Card, CardSpecification> {
    public static final CardSpecificationHandler INSTANCE = new CardSpecificationHandler();

    public Card find(Game game, final CardSpecification cardSpecification) {
        return find(getCardsToSearch(game, cardSpecification), cardSpecification);
    }

    public Card find(Game game, final CardSpecification cardSpecification, final IntegerConstraint expectedNumberOfResults) {
        return find(getCardsToSearch(game, cardSpecification), cardSpecification, expectedNumberOfResults);
    }

    public boolean matches(Card card, final CardSpecification cardSpecification) {
        return card.getName().equals(cardSpecification.getName())
                && (cardSpecification.getZoneType() == null || cardSpecification.getZoneType() == card.getZone().getZoneType())
                && (cardSpecification.getController() == null || PlayerSpecificationHandler.INSTANCE.matches(card.getController(), cardSpecification.getController()))
                && (cardSpecification.getOwner() == null || PlayerSpecificationHandler.INSTANCE.matches(card.getOwner(), cardSpecification.getOwner()));
    }

    private CardCollectionView getCardsToSearch(Game game, final CardSpecification cardSpecification) {
        if ((cardSpecification.getController() == null && cardSpecification.getOwner() == null) || game.getPlayers().size() == game.getRegisteredPlayers().size()) {
            return game.getCardsInGame();
        }
        
        if (cardSpecification.getController() != null) {
            return getCardsToSearch(game, cardSpecification.getController());
        } else if (cardSpecification.getOwner() != null) {
            return getCardsToSearch(game, cardSpecification.getOwner());
        }
        
        throw new IllegalStateException("Can't handle this case");
    }

    private CardCollectionView getCardsToSearch(Game game, final PlayerSpecification relevantPlayer) {
        return PlayerSpecificationHandler.INSTANCE.find(game, relevantPlayer).getAllCards();
    }
}
