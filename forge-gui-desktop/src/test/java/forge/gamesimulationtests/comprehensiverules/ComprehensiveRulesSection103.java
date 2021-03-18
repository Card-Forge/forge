package forge.gamesimulationtests.comprehensiverules;

import org.testng.annotations.Test;

import forge.game.phase.PhaseType;
import forge.gamesimulationtests.BaseGameSimulationTest;
import forge.gamesimulationtests.util.GameWrapper;
import forge.gamesimulationtests.util.card.CardSpecificationBuilder;
import forge.gamesimulationtests.util.gamestate.GameStateSpecificationBuilder;
import forge.gamesimulationtests.util.player.PlayerSpecification;
import forge.gamesimulationtests.util.player.PlayerSpecificationBuilder;
import forge.gamesimulationtests.util.playeractions.ActionPreCondition;
import forge.gamesimulationtests.util.playeractions.PlayerActions;
import forge.gamesimulationtests.util.playeractions.testactions.CardAssertAction;
import forge.gamesimulationtests.util.playeractions.testactions.EndTestAction;
import forge.gamesimulationtests.util.playeractions.testactions.PlayerAssertAction;

public class ComprehensiveRulesSection103 extends BaseGameSimulationTest {
	@Test
	public void test_103_3_players_start_at_20_life() {
		GameWrapper gameWrapper = new GameWrapper( 
				null, 
				new PlayerActions( 
						new PlayerAssertAction( PlayerSpecificationBuilder.player1().life( 20 ) ),
						new PlayerAssertAction( PlayerSpecificationBuilder.player2().life( 20 ) ),
						new EndTestAction( PlayerSpecification.PLAYER_1 )
				)
		);
		runGame( gameWrapper, PlayerSpecification.PLAYER_2, 1 );
	}
	
	@Test
	public void test_103_7a_first_player_skips_draw_step_of_first_turn() {
		GameWrapper gameWrapper = new GameWrapper(
				new GameStateSpecificationBuilder()
						.addCard( new CardSpecificationBuilder( "Plains" ).owner( PlayerSpecification.PLAYER_1 ).library() )
						.addCard( new CardSpecificationBuilder( "Forest" ).owner( PlayerSpecification.PLAYER_2 ).library() )
						.build(),
				new PlayerActions(
						new CardAssertAction( new CardSpecificationBuilder( "Plains" ).owner( PlayerSpecification.PLAYER_1 ).library() )
								.when( new ActionPreCondition().turn( 2 ).phase( PhaseType.END_OF_TURN ) ),
						new CardAssertAction( new CardSpecificationBuilder( "Plains" ).owner( PlayerSpecification.PLAYER_1 ).hand() )
								.when( new ActionPreCondition().turn( 3 ).phase( PhaseType.MAIN1 ) ),
						new EndTestAction( PlayerSpecification.PLAYER_1 )
				)
		);
		runGame( gameWrapper, PlayerSpecification.PLAYER_2, 3 );
	}
}
