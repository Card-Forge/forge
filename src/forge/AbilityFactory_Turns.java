package forge;

import java.util.ArrayList;
import java.util.HashMap;

public class AbilityFactory_Turns {
	// *************************************************************************
	// ************************* ADD TURN **************************************
	// *************************************************************************
	
	public static SpellAbility createAbilityAddTurn(final AbilityFactory AF){

		final SpellAbility abAddTurn = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -3526200766738015688L;
			final AbilityFactory af = AF;
		
			@Override
			public String getStackDescription(){
				return addTurnStackDescription(af, this);
			}

			public boolean canPlayAI() {
				return addTurnCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				addTurnResolve(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return addTurnTriggerAI(af, this, mandatory);
			}
			
		};
		return abAddTurn;
	}
	
	public static SpellAbility createSpellAddTurn(final AbilityFactory AF){
		final SpellAbility spAddTurn = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -3921131887560356006L;
			final AbilityFactory af = AF;
		
			@Override
			public String getStackDescription(){
				return addTurnStackDescription(af, this);
			}
			
			public boolean canPlayAI()
			{
				// if X depends on abCost, the AI needs to choose which card he would sacrifice first
				// then call xCount with that card to properly calculate the amount
				// Or choosing how many to sacrifice 
				return addTurnCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				addTurnResolve(af, this);
			}
			
		};
		return spAddTurn;
	}
	
	public static SpellAbility createDrawbackAddTurn(final AbilityFactory AF){
		final SpellAbility dbAddTurn = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()){
			private static final long serialVersionUID = -562517287448810951L;
			final AbilityFactory af = AF;
		
			@Override
			public String getStackDescription(){
				return addTurnStackDescription(af, this);
			}
			
			@Override
			public void resolve() {
				addTurnResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return true;
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return addTurnTriggerAI(af, this, mandatory);
			}
			
		};
		return dbAddTurn;
	}

	private static String addTurnStackDescription(AbilityFactory af, SpellAbility sa){
		StringBuilder sb = new StringBuilder();
		int numTurns = AbilityFactory.calculateAmount(af.getHostCard(), af.getMapParams().get("NumTurns"), sa);

		if (!(sa instanceof Ability_Sub))
			sb.append(sa.getSourceCard().getName()).append(" - ");
		else
			sb.append(" ");

		ArrayList<Player> tgtPlayers;

		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtPlayers = tgt.getTargetPlayers();
		else
			tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
		
		for(Player player : tgtPlayers)
			sb.append(player).append(" ");
		
		sb.append("takes ");
		if(numTurns > 1) {
			sb.append(numTurns);
		}
		else {
			sb.append("an");
		}
		sb.append(" extra turn");
		if(numTurns > 1) sb.append("s");
		sb.append(" after this one.");

		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null) {
			sb.append(abSub.getStackDescription());
		}

		return sb.toString();
	}
	
	private static boolean addTurnCanPlayAI(final AbilityFactory af, final SpellAbility sa){
		if (!ComputerUtil.canPayCost(sa))
			return false;

		Target tgt = sa.getTarget();

		if (sa.getTarget() != null){
			tgt.resetTargets();
			sa.getTarget().addTarget(AllZone.ComputerPlayer);
		}
		else{
			ArrayList<Player> tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
			for (Player p : tgtPlayers)
				if (p.isHuman())
					return false;
			// not sure if the AI should be playing with cards that give the Human more turns.
		}
		return true;
	}
	
	private static boolean addTurnTriggerAI(final AbilityFactory af, final SpellAbility sa, boolean mandatory){
		if (!ComputerUtil.canPayCost(sa))
			return false;

		Target tgt = sa.getTarget();

		if (sa.getTarget() != null){
			tgt.resetTargets();
			sa.getTarget().addTarget(AllZone.ComputerPlayer);
		}
		else{
			ArrayList<Player> tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
			for (Player p : tgtPlayers)
				if (p.isHuman() && !mandatory)
					return false;
			// not sure if the AI should be playing with cards that give the Human more turns.
		}
		return true;
	}
	
	private static void addTurnResolve(final AbilityFactory af, final SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		Card card = af.getHostCard();
		int numTurns = AbilityFactory.calculateAmount(af.getHostCard(), params.get("NumTurns"), sa);
		
		ArrayList<Player> tgtPlayers;

		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtPlayers = tgt.getTargetPlayers();
		else
			tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
		
		for(Player p : tgtPlayers) {
			if (tgt == null || p.canTarget(af.getHostCard())) {
				for(int i = 0; i < numTurns; i++) {
					AllZone.Phase.addExtraTurn(p);
				}
			}
		}
		
		if (af.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
	     	   abSub.resolve();
	        }
			else{
				String DrawBack = params.get("SubAbility");
				if (af.hasSubAbility())
					 CardFactoryUtil.doDrawBack(DrawBack, numTurns, card.getController(), card.getController().getOpponent(), tgtPlayers.get(0), card, null, sa);
			}
		}
	}
	
}
