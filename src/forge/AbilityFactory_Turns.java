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
			final HashMap<String,String> params = af.getMapParams();
		
			@Override
			public String getStackDescription(){
				return addTurnStackDescription(af, this);
			}
			
			public boolean canPlay(){
				return super.canPlay();	
			}
			
			public boolean canPlayAI() {
				return addTurnCanPlayAI(af, this, params.get("NumTurns"));
			}
			
			@Override
			public void resolve() {
				int amount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("NumTurns"), this);
				addTurnResolve(af, this, amount);
			}
			
		};
		return abAddTurn;
	}
	
	public static SpellAbility createSpellAddTurn(final AbilityFactory AF){
		final SpellAbility spAddTurn = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -3921131887560356006L;
			final AbilityFactory af = AF;
			final HashMap<String,String> params = af.getMapParams();
		
			@Override
			public String getStackDescription(){
				return addTurnStackDescription(af, this);
			}
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();	
			}
			
			public boolean canPlayAI()
			{
				// if X depends on abCost, the AI needs to choose which card he would sacrifice first
				// then call xCount with that card to properly calculate the amount
				// Or choosing how many to sacrifice 
				return addTurnCanPlayAI(af, this, params.get("NumTurns"));
			}
			
			@Override
			public void resolve() {
				int amount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("NumTurns"), this);
				addTurnResolve(af, this, amount);
			}
			
		};
		return spAddTurn;
	}
	
	public static SpellAbility createDrawbackAddTurn(final AbilityFactory AF){
		final SpellAbility dbAddTurn = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()){
			private static final long serialVersionUID = -562517287448810951L;
			final AbilityFactory af = AF;
			final HashMap<String,String> params = af.getMapParams();
		
			@Override
			public String getStackDescription(){
				return addTurnStackDescription(af, this);
			}
			
			public boolean canPlayAI() {
				return addTurnCanPlayAI(af, this, params.get("NumTurns"));
			}
			
			@Override
			public void resolve() {
				int amount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("NumTurns"), this);
				addTurnResolve(af, this, amount);
			}

			@Override
			public boolean chkAI_Drawback() {
				return true;
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
	
	private static boolean addTurnCanPlayAI(final AbilityFactory af, final SpellAbility sa, final String amountStr){
		Ability_Cost abCost = sa.getPayCosts();
		int life = AllZone.ComputerPlayer.getLife();

		if (abCost != null){
			// AI currently disabled for these costs
			if (abCost.getSacCost()){
				if (amountStr.contains("X"))
					return false;
				if (life > 4)
					return false;
			}
			if (abCost.getLifeCost() && life > 50)	 return false;
			if (abCost.getDiscardCost() && life > 50) return false;
		}
		
		if (!ComputerUtil.canPayCost(sa))
			return false;
		else {
			Target tgt = sa.getTarget();

			if (sa.getTarget() != null){
				tgt.resetTargets();
				sa.getTarget().addTarget(AllZone.ComputerPlayer);
			}
			return true;
		}
	}
	
	private static void addTurnResolve(final AbilityFactory af, final SpellAbility sa, int numTurns){
		HashMap<String,String> params = af.getMapParams();
		Card card = af.getHostCard();
		
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
