package forge.gamesimulationtests.util.player;

import org.apache.commons.lang3.StringUtils;

public class PlayerSpecificationBuilder {
	private final String name;
	private Integer life;
	private Integer poison;

	public PlayerSpecificationBuilder( final String name ) {
		this.name = name;
	}

	public static PlayerSpecificationBuilder player1() {
		return new PlayerSpecificationBuilder( PlayerSpecification.PLAYER_1_NAME );
	}

	public static PlayerSpecificationBuilder player2() {
		return new PlayerSpecificationBuilder( PlayerSpecification.PLAYER_2_NAME );
	}

	public PlayerSpecificationBuilder life( final Integer life ) {
		this.life = life;
		return this;
	}

	public PlayerSpecificationBuilder poison( final Integer poison ) {
		this.poison = poison;
		return this;
	}

	public PlayerSpecification build() {
		if( StringUtils.isBlank( name ) ) {
			throw new IllegalStateException( "Must have a name [" + name + "]" );
		}
		if( poison != null && poison < 0 ) {
			throw new IllegalStateException( "Can't have negative poison counters" );
		}
		return new PlayerSpecification( name, life, poison );
	}
}
