package forge.gamesimulationtests.util.playeractions;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.gamesimulationtests.util.player.PlayerSpecification;

/**
 * TODO: better rules compliance (sorcery speed stuff, paying mana, ...)
 */
public class CastSpellFromHandAction extends BasePlayerAction {
	private final String spellName;

	public CastSpellFromHandAction(PlayerSpecification player, String spellName0) {
		super(player);
		spellName = spellName0;
	}

	public void castSpellFromHand(Player player, Game game) {
		CardCollectionView cardsInHand = player.getCardsIn(ZoneType.Hand);
		Card cardToPlay = null;
		for (Card card : cardsInHand) {
			if (spellName.equals(card.getName())) {
				cardToPlay = card;
			}
		}
		if (cardToPlay == null) {
			throw new IllegalStateException( "Couldn't find " + spellName );
		}

		SpellAbility spellAbility = cardToPlay.getSpells().get(0);
		spellAbility.setActivatingPlayer(player);
		spellAbility.setHostCard(game.getAction().moveToStack(cardToPlay, spellAbility));
		if (spellAbility.usesTargeting())
			spellAbility.getTargets().add(player);
		game.getStack().freezeStack();
		game.getStack().addAndUnfreeze(spellAbility);
	}
}
