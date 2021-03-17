package forge.gamesimulationtests.util.playeractions;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import forge.gamesimulationtests.util.card.CardSpecification;
import forge.gamesimulationtests.util.player.PlayerSpecification;

public class DeclareBlockersAction extends BasePlayerAction {
	private final Multimap<CardSpecification, CardSpecification> blockingAssignments;
	
	public DeclareBlockersAction( PlayerSpecification player ) {
		super( player );
		blockingAssignments = ArrayListMultimap.create();
	}
	
	public DeclareBlockersAction block( CardSpecification attacker, CardSpecification blocker ) {
		blockingAssignments.put( attacker, blocker );
		return this;
	}
	
	public Multimap<CardSpecification, CardSpecification> getBlockingAssignments() {
		return blockingAssignments;
	}
}
