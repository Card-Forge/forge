package forge.gamesimulationtests.util.playeractions;

import java.util.List;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.gamesimulationtests.util.card.CardSpecification;
import forge.gamesimulationtests.util.card.CardSpecificationHandler;
import forge.gamesimulationtests.util.player.PlayerSpecification;

//TODO: almost everything (picking the right ability to activate, choosing targets, paying costs, ...)
public class ActivateAbilityAction extends BasePlayerAction {
	private final CardSpecification cardWithAbility;

	public ActivateAbilityAction( PlayerSpecification player, CardSpecification cardWithAbility ) {
		super( player );
		this.cardWithAbility = cardWithAbility;
	}

	public void activateAbility( Player player, Game game ) {
		Card actualCardWithAbility = CardSpecificationHandler.INSTANCE.find( game, cardWithAbility );
		
		List<SpellAbility> abilities = actualCardWithAbility.getAllPossibleAbilities( player, true );
		if( abilities.isEmpty() ) {
			throw new IllegalStateException( "No abilities found for " + actualCardWithAbility );
		}
		if( abilities.size() > 1 ) {
			throw new IllegalStateException( "Multiple abilities found for " + actualCardWithAbility );
		}
		
		SpellAbility ability = abilities.get( 0 );
		game.getStack().add( ability );
	}
}
