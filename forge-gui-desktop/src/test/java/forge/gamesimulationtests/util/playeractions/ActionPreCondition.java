package forge.gamesimulationtests.util.playeractions;

import forge.game.Game;
import forge.game.phase.PhaseType;

/** This class allows you to specify that an action should only happen on turn X, during phase Y, ...
 */
public class ActionPreCondition {
	private Integer requiredTurn;
	private PhaseType requiredPhaseType;

	public boolean isApplicable( Game game ) {
		if( requiredTurn != null ) {
			if( requiredTurn != game.getPhaseHandler().getTurn() ) {
				return false;
			}
			if( requiredTurn < game.getPhaseHandler().getTurn() ) {
				throw new IllegalStateException( "Mock action " + this + " can only trigger during turn " + requiredTurn + " but it is already turn " + game.getPhaseHandler().getTurn() );
			}
		}

        return requiredPhaseType == null || requiredPhaseType == game.getPhaseHandler().getPhase();
    }

	public ActionPreCondition turn( int turn ) {
		requiredTurn = turn;
		return this;
	}

	public ActionPreCondition phase( PhaseType phaseType ) {
		requiredPhaseType = phaseType;
		return this;
	}
}
