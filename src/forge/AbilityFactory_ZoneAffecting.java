package forge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class AbilityFactory_ZoneAffecting {
	public static SpellAbility createAbilityDraw(final AbilityFactory AF){
		final SpellAbility abDraw = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 5445572699000471299L;
			
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
			// when getStackDesc is called, just build exactly what is happening
				Player player = af.getAbTgt() == null ? getActivatingPlayer() : getTargetPlayer(); 
				StringBuilder sb = new StringBuilder();
				
				sb.append(getSourceCard().getName());
				sb.append(" - ");
				sb.append(player.toString());
				sb.append(" draws (");
				sb.append(af.getMapParams().get("NumCards"));
				sb.append(")");
				
				return sb.toString();
			}
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();		
			}
			
			public boolean canPlayAI()
			{
				return drawCanPlayAI(af,this);
			}
			
			@Override
			public void resolve() {
				drawResolve(af, this);
			}
			
		};
		return abDraw;
	}
	
	public static SpellAbility createSpellDraw(final AbilityFactory AF){
		final SpellAbility spDraw = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -4990932993654533449L;
			
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
				// when getStackDesc is called, just build exactly what is happening
				Player player = af.getAbTgt() == null ? getActivatingPlayer() : getTargetPlayer(); 
				StringBuilder sb = new StringBuilder();
				
				sb.append(getSourceCard().getName());
				sb.append(" - ");
				sb.append(player.toString());
				sb.append(" draws (");
				sb.append(af.getMapParams().get("NumCards"));
				sb.append(")");
				
				return sb.toString();
			}
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();	
			}
			
			public boolean canPlayAI()
			{
				return drawCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				drawResolve(af, this);
			}
			
		};
		return spDraw;
	}
	
	public static boolean drawCanPlayAI(final AbilityFactory af, SpellAbility sa){
		// AI cannot use this properly until he can use SAs during Humans turn
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		Target tgt = af.getAbTgt();
		Card source = sa.getSourceCard();
		Ability_Cost abCost = af.getAbCost();
		HashMap<String,String> params = af.getMapParams();
		
		if (abCost != null){
			// AI currently disabled for these costs
			if (abCost.getSacCost()){
				return false;
			}
			if (abCost.getLifeCost()){
				if (AllZone.ComputerPlayer.getLife() - abCost.getLifeAmount() < 4)
					return false;
			}
			if (abCost.getDiscardCost()) 	return false;
			
			if (abCost.getSubCounter()) {
				if (abCost.getCounterType().equals(Counters.P1P1)) return false; // Other counters should be used 
			}
			
		}
		
		if (tgt != null){
			tgt.resetTargets();
			
			// todo: handle deciding what X would be around here for Braingeyser type cards
			int numCards = 1;
			if (params.containsKey("NumCards"))
				numCards = Integer.parseInt(params.get("NumCards"));
			
			if (!AllZone.HumanPlayer.cantLose() && numCards >= AllZoneUtil.getCardsInZone(Constant.Zone.Library, AllZone.HumanPlayer).size()){
				// Deck the Human? DO IT!
				tgt.addTarget(AllZone.HumanPlayer);
				return true;
			}
			
			if (numCards >= AllZoneUtil.getCardsInZone(Constant.Zone.Library, AllZone.ComputerPlayer).size()){
				// Don't deck your self
				return false;
			}
			
			tgt.addTarget(AllZone.ComputerPlayer);
		}
		
		Random r = new Random();
		boolean randomReturn = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed());
		
		// some other variables here, like handsize vs. maxHandSize

		return randomReturn;
	}
	
	public static void drawResolve(final AbilityFactory af, final SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		
		Card source = sa.getSourceCard();
		int numCards = Integer.parseInt(params.get("NumCards"));
		
		ArrayList<Player> tgtPlayers;

		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtPlayers = tgt.getTargetPlayers();
		else{
			tgtPlayers = new ArrayList<Player>();
			tgtPlayers.add(sa.getActivatingPlayer());
		}
		
		for(Player p : tgtPlayers)
			if (tgt == null || p.canTarget(af.getHostCard()))
				p.drawCards(numCards);		

		String DrawBack = params.get("SubAbility");
		if (af.hasSubAbility())
			 CardFactoryUtil.doDrawBack(DrawBack, 0, source.getController(), source.getController().getOpponent(), source.getController(), source, null, sa);

	}
	
	
	// ******************** MILL ****************************
	
	public static SpellAbility createAbilityMill(final AbilityFactory AF){
		final SpellAbility abMill = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 5445572699000471299L;
			
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
			// when getStackDesc is called, just build exactly what is happening
				Player player = af.getAbTgt() == null ? getActivatingPlayer() : getTargetPlayer(); 
				StringBuilder sb = new StringBuilder();
				
				sb.append(getSourceCard().getName());
				sb.append(" - Mills ");
				sb.append(af.getMapParams().get("NumCards"));
				sb.append(" Cards from ");
				sb.append(player.toString());
				sb.append("'s library.");
				
				return sb.toString();
			}
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();		
			}
			
			public boolean canPlayAI()
			{
				return millCanPlayAI(af,this);
			}
			
			@Override
			public void resolve() {
				millResolve(af, this);
			}
			
		};
		return abMill;
	}
	
	public static SpellAbility createSpellMill(final AbilityFactory AF){
		final SpellAbility spMill = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -4990932993654533449L;
			
			final AbilityFactory af = AF;
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();	
			}
			
			public boolean canPlayAI()
			{
				return millCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				millResolve(af, this);
			}
			
		};
		return spMill;
	}
	
	public static boolean millCanPlayAI(final AbilityFactory af, SpellAbility sa){
		// AI cannot use this properly until he can use SAs during Humans turn
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		Target tgt = af.getAbTgt();
		Card source = sa.getSourceCard();
		Ability_Cost abCost = af.getAbCost();
		HashMap<String,String> params = af.getMapParams();
		
		if (abCost != null){
			// AI currently disabled for these costs
			if (abCost.getSacCost()){
				return false;
			}
			if (abCost.getLifeCost()){
				if (AllZone.ComputerPlayer.getLife() - abCost.getLifeAmount() < 4)
					return false;
			}
			if (abCost.getDiscardCost()) 	return false;
			
			if (abCost.getSubCounter()) {
				if (abCost.getCounterType().equals(Counters.P1P1)) return false; // Other counters should be used 
			}
			
		}
		
		if (tgt != null){
			tgt.resetTargets();
			
			// todo: handle deciding what X would be around here for Psychic Drain type cards
			int numCards = 1;
			if (params.containsKey("NumCards"))
				numCards = Integer.parseInt(params.get("NumCards"));
			
			CardList pLibrary = AllZoneUtil.getCardsInZone(Constant.Zone.Library, AllZone.HumanPlayer);
			
			if (pLibrary.size() == 0)	// deck already empty, no need to mill
				return false;
			
			if (numCards >= pLibrary.size()){
				// Can Mill out Human's deck? Do it!
				tgt.addTarget(AllZone.HumanPlayer);
				return true;
			}
			
			// Obscure case when you know what your top card is so you might? want to mill yourself here
			// if (AI wants to mill self)
			// tgt.addTarget(AllZone.ComputerPlayer);
			// else
			tgt.addTarget(AllZone.HumanPlayer);
		}
		
		Random r = new Random();
		boolean randomReturn = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed());
		
		// some other variables here, like deck size, and phase and other fun stuff

		return randomReturn;
	}
	
	public static void millResolve(final AbilityFactory af, final SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		
		Card source = sa.getSourceCard();
		int numCards = Integer.parseInt(params.get("NumCards"));
		
		ArrayList<Player> tgtPlayers;

		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtPlayers = tgt.getTargetPlayers();
		else{
			tgtPlayers = new ArrayList<Player>();
			tgtPlayers.add(sa.getActivatingPlayer());
		}
		
		for(Player p : tgtPlayers)
			if (tgt == null || p.canTarget(af.getHostCard()))
				p.mill(numCards);	

		String DrawBack = params.get("SubAbility");
		if (af.hasSubAbility())
			 CardFactoryUtil.doDrawBack(DrawBack, 0, source.getController(), source.getController().getOpponent(), tgtPlayers.get(0), source, null, sa);

	}
}
