package forge.gamesimulationtests.util.card;

import org.apache.commons.lang3.StringUtils;

import forge.game.zone.ZoneType;
import forge.gamesimulationtests.util.player.PlayerSpecification;

public class CardSpecificationBuilder {
	private final String name;
	private ZoneType zoneType;
	private PlayerSpecification owner;
	private PlayerSpecification controller;
	private CardSpecification target;
	
	public CardSpecificationBuilder( final String name ) {
		this.name = name;
	}
	
	public CardSpecificationBuilder zone( final ZoneType zoneType ) {
		this.zoneType = zoneType;
		return this;
	}
	
	public CardSpecificationBuilder hand() {
		return zone( ZoneType.Hand );
	}
	
	public CardSpecificationBuilder battlefield() {
		return zone( ZoneType.Battlefield );
	}
	
	public CardSpecificationBuilder library() {
		return zone( ZoneType.Library );
	}
	
	public CardSpecificationBuilder graveyard() {
		return zone( ZoneType.Graveyard );
	}
	
	public CardSpecificationBuilder owner( final PlayerSpecification owner ) {
		this.owner = owner;
		return this;
	}
	
	public CardSpecificationBuilder controller( final PlayerSpecification controller ) {
		this.controller = controller;
		return this;
	}
	
	public CardSpecificationBuilder target( final CardSpecification target ) {
		this.target = target;
		return this;
	}
	
	public CardSpecification build() {
		if( StringUtils.isBlank( name ) ) {
			throw new IllegalStateException( "Must have a name [" + name + "]" );
		}
		return new CardSpecification( name, zoneType, owner, controller, target );
	}
}
