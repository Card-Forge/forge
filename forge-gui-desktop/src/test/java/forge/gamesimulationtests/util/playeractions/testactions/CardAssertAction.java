package forge.gamesimulationtests.util.playeractions.testactions;

import org.testng.Assert;

import forge.game.Game;
import forge.game.card.Card;
import forge.gamesimulationtests.util.IntegerConstraint;
import forge.gamesimulationtests.util.card.CardSpecification;
import forge.gamesimulationtests.util.card.CardSpecificationBuilder;
import forge.gamesimulationtests.util.card.CardSpecificationHandler;

public class CardAssertAction extends AssertAction {
	private final CardSpecification cardRequirements;
	
	public CardAssertAction( final CardSpecification cardRequirements ) {
		this.cardRequirements = cardRequirements;
	}
	
	public CardAssertAction( final CardSpecificationBuilder cardRequirements ) {
		this( cardRequirements.build() );
	}

	@Override
	public void performAssertion( final Game game ) {
		final Card card = CardSpecificationHandler.INSTANCE.find( game, cardRequirements, IntegerConstraint.ZERO_OR_ONE );
		Assert.assertNotNull( card );
	}
}
