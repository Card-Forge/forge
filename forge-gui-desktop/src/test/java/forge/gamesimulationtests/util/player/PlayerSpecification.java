package forge.gamesimulationtests.util.player;

import forge.game.player.Player;
import forge.gamesimulationtests.util.Specification;

public class PlayerSpecification implements Specification<Player> {
	//Tests currently create player objects of type "Human" (to avoid extra AI logic creeping in), which means the HotSeat logic kicks in and assigns these default names
	public static final String PLAYER_1_NAME = "Player 1";
	public static final String PLAYER_2_NAME = "Player 2";
	public static final PlayerSpecification PLAYER_1 = PlayerSpecificationBuilder.player1().build();
	public static final PlayerSpecification PLAYER_2 = PlayerSpecificationBuilder.player2().build();

	private final String name;
	private final Integer life;
	private final Integer poison;

	/*package-local*/ PlayerSpecification( final String name, final Integer life, final Integer poison ) {
		this.name = name;
		this.life = life;
		this.poison = poison;
	}

	public String getName() {
		return name;
	}

	public Integer getLife() {
		return life;
	}

	public Integer getPoison() {
		return poison;
	}
}
