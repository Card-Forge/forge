package forge.gamesimulationtests.util.gamestate;

import forge.gamesimulationtests.util.card.CardSpecification;
import forge.gamesimulationtests.util.card.CardSpecificationBuilder;
import forge.gamesimulationtests.util.player.PlayerSpecification;
import forge.gamesimulationtests.util.player.PlayerSpecificationBuilder;

import java.util.*;

public class GameStateSpecificationBuilder {
	private final List<CardSpecification> cards;
	private final Map<String,PlayerSpecification> playerFacts;
	
	public GameStateSpecificationBuilder() {
		cards = new ArrayList<CardSpecification>();
		playerFacts = new HashMap<String,PlayerSpecification>();
	}
	
	public GameStateSpecificationBuilder addCard( final CardSpecification cardSpecification ) {
		cards.add( cardSpecification );
		return this;
	}
	
	public GameStateSpecificationBuilder addCard( final CardSpecificationBuilder cardSpecification ) {
		return addCard( cardSpecification.build() );
	}
	
	public GameStateSpecificationBuilder addPlayerFact( final PlayerSpecification playerSpecification ) {
		if( playerFacts.containsKey( playerSpecification.getName() ) ) {
			throw new IllegalStateException( "If you want to specify multiple things about the same player, do it in 1 call" );
		}
		playerFacts.put( playerSpecification.getName(), playerSpecification );
		return this;
	}
	
	public GameStateSpecificationBuilder addPlayerFact( final PlayerSpecificationBuilder playerSpecification ) {
		return addPlayerFact( playerSpecification.build() );
	}
	
	public GameStateSpecification build() {
		return new GameStateSpecification( Collections.unmodifiableList( cards ), Collections.unmodifiableMap( playerFacts ) );
	}
}
