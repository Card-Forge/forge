package forge.gamesimulationtests.util.playeractions;

import java.util.List;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.gamesimulationtests.util.player.PlayerSpecification;

/**
 * TODO: better rules compliance (sorcery speed stuff, paying mana, ...)
 */
public class CastSpellFromHandAction extends BasePlayerAction {
	private final String spellName;

	public CastSpellFromHandAction( PlayerSpecification player, String spellName ) {
		super( player );
		this.spellName = spellName;
	}

	public void castSpellFromHand( Player player, Game game ) {
		List<Card> cardsInHand = player.getCardsIn( ZoneType.Hand );
		Card cardToPlay = null;
		for( Card card : cardsInHand ) {
			if( spellName.equals( card.getName() ) ) {
				cardToPlay = card;
			}
		}
		if( cardToPlay == null ) {
			throw new IllegalStateException( "Couldn't find " + spellName );
		}
		
		SpellAbility spellAbility = cardToPlay.getSpells().get( 0 );
		spellAbility.setActivatingPlayer( player );
		spellAbility.setSourceCard( game.getAction().moveToStack( cardToPlay ) );
		spellAbility.getTargets().add( player );//TODO
		game.getStack().freezeStack();
		game.getStack().addAndUnfreeze( spellAbility );
	}
}
