package forge.gamesimulationtests.util.card;

import forge.game.card.Card;
import forge.game.zone.ZoneType;
import forge.gamesimulationtests.util.Specification;
import forge.gamesimulationtests.util.player.PlayerSpecification;

/**
 * Applies to tokens as well...
 */
public class CardSpecification implements Specification<Card> {
	private final String name;
	private final ZoneType zoneType;
	private final PlayerSpecification owner;
	private final PlayerSpecification controller;
	private final CardSpecification target;

	/*package-local*/ CardSpecification( final String name, final ZoneType zoneType, final PlayerSpecification owner, final PlayerSpecification controller, final CardSpecification target ) {
		this.name = name;
		this.zoneType = zoneType;
		this.owner = owner;
		this.controller = controller;
		this.target = target;
	}

	public String getName() {
		return name;
	}

	public ZoneType getZoneType() {
		return zoneType;
	}

	public PlayerSpecification getOwner() {
		return owner;
	}

	public PlayerSpecification getController() {
		return controller;
	}

	public CardSpecification getTarget() {
		return target;
	}
}
