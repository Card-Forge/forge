package forge.gamesimulationtests.util.gamestate;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import forge.gamesimulationtests.util.card.CardSpecification;
import forge.gamesimulationtests.util.player.PlayerSpecification;

public class GameStateSpecification {
	private final List<CardSpecification> cards;
	private final Map<String,PlayerSpecification> playerFacts;
	
	/*package*protected*/ GameStateSpecification( final List<CardSpecification> cards, final Map<String,PlayerSpecification> playerFacts ) {
		this.cards = cards;
		this.playerFacts = playerFacts;
	}
	
	public List<CardSpecification> getCards() {
		return cards;
	}
	
	public Collection<PlayerSpecification> getPlayerFacts() {
		return playerFacts.values();
	}
}
