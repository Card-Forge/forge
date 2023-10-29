package forge.gamesimulationtests.util.playeractions;

import java.util.HashMap;
import java.util.Map;

import forge.gamesimulationtests.util.card.CardSpecification;
import forge.gamesimulationtests.util.player.PlayerSpecification;

public class DeclareAttackersAction extends BasePlayerAction {
	private final Map<CardSpecification, PlayerSpecification> playerAttackAssignments;
	private final Map<CardSpecification, CardSpecification> planeswalkerAttackAssignments;
	private final Map<CardSpecification, CardSpecification> battleAttackAssignments;

	public DeclareAttackersAction( PlayerSpecification player ) {
		super( player );
		playerAttackAssignments = new HashMap<>();
		planeswalkerAttackAssignments = new HashMap<>();
		battleAttackAssignments = new HashMap<>();
	}

	/**
	 * Attack the only opponent
	 */
	public DeclareAttackersAction attack( CardSpecification attacker ) {
		playerAttackAssignments.put( attacker, null );
		return this;
	}

	public DeclareAttackersAction attack( CardSpecification attacker, PlayerSpecification player ) {
		playerAttackAssignments.put( attacker, player );
		return this;
	}

	public DeclareAttackersAction attack( CardSpecification attacker, CardSpecification planeswalker ) {
		planeswalkerAttackAssignments.put( attacker, planeswalker );
		return this;
	}

	public Map<CardSpecification, PlayerSpecification> getPlayerAttackAssignments() {
		return playerAttackAssignments;
	}

	public Map<CardSpecification, CardSpecification> getPlaneswalkerAttackAssignments() {
		return planeswalkerAttackAssignments;
	}

	public Map<CardSpecification, CardSpecification> getBattleAttackAssignments() {
		return battleAttackAssignments;
	}
}
