package forge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class AbilityFactory_AlterLife {
	// An AbilityFactory subclass for Gaining, Losing, or Setting Life totals.
	
	public static SpellAbility createAbilityGainLife(final AbilityFactory AF){

		final SpellAbility abGainLife = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 8869422603616247307L;
			
			final AbilityFactory af = AF;
			final HashMap<String,String> params = af.getMapParams();
		
			@Override
			public String getStackDescription(){
				// when getStackDesc is called, just build exactly what is happening
				return gainLifeStackDescription(af, this);
			}
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();	
			}
			
			public boolean canPlayAI()
			{
				return gainLifeCanPlayAI(af, this, params.get("LifeAmount"));
			}
			
			@Override
			public void resolve() {
				int amount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("LifeAmount"), this);
				gainLifeResolve(af, this, amount);
			}
			
		};
		return abGainLife;
	}
	
	public static SpellAbility createSpellGainLife(final AbilityFactory AF){
		final SpellAbility spGainLife = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 6631124959690157874L;
			
			final AbilityFactory af = AF;
			final HashMap<String,String> params = af.getMapParams();
		
			@Override
			public String getStackDescription(){
			// when getStackDesc is called, just build exactly what is happening
				return gainLifeStackDescription(af, this);
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
				return gainLifeCanPlayAI(af, this, params.get("LifeAmount"));
			}
			
			@Override
			public void resolve() {
				int amount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("LifeAmount"), this);
				gainLifeResolve(af, this, amount);
			}
			
		};
		return spGainLife;
	}
	
	public static SpellAbility createDrawbackGainLife(final AbilityFactory AF){
		final SpellAbility dbGainLife = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()){
			private static final long serialVersionUID = 6631124959690157874L;
			
			final AbilityFactory af = AF;
			final HashMap<String,String> params = af.getMapParams();
		
			@Override
			public String getStackDescription(){
			// when getStackDesc is called, just build exactly what is happening
				return gainLifeStackDescription(af, this);
			}
			
			public boolean canPlayAI()
			{
				// if X depends on abCost, the AI needs to choose which card he would sacrifice first
				// then call xCount with that card to properly calculate the amount
				// Or choosing how many to sacrifice 
				return gainLifeCanPlayAI(af, this, params.get("LifeAmount"));
			}
			
			@Override
			public void resolve() {
				int amount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("LifeAmount"), this);
				gainLifeResolve(af, this, amount);
			}

			@Override
			public boolean chkAI_Drawback() {
				return true;
			}
			
		};
		return dbGainLife;
	}
	
	public static SpellAbility createAbilityLoseLife(final AbilityFactory AF){
		final SpellAbility abLoseLife = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 1129762905315395160L;
			
			final AbilityFactory af = AF;
			final HashMap<String,String> params = af.getMapParams();
		
			@Override
			public String getStackDescription(){
				// when getStackDesc is called, just build exactly what is happening
				return loseLifeStackDescription(af, this);
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
				return loseLifeCanPlayAI(af, this, params.get("LifeAmount"));
			}
			
			@Override
			public void resolve() {
				int amount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("LifeAmount"), this);
				loseLifeResolve(af, this, amount);
			}
		};
		return abLoseLife;
	}
	
	public static SpellAbility createSpellLoseLife(final AbilityFactory AF){
		final SpellAbility spLoseLife = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -2966932725306192437L;
			
			final AbilityFactory af = AF;
			final HashMap<String,String> params = af.getMapParams();
		
			@Override
			public String getStackDescription(){
				// when getStackDesc is called, just build exactly what is happening
				return loseLifeStackDescription(af, this);
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
				return loseLifeCanPlayAI(af, this, params.get("LifeAmount"));
			}
			
			@Override
			public void resolve() {
				int amount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("LifeAmount"), this);
				loseLifeResolve(af, this, amount);
			}
		};
		return spLoseLife;
	}
	
	public static SpellAbility createDrawbackLoseLife(final AbilityFactory AF){
		final SpellAbility dbLoseLife = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()){
			private static final long serialVersionUID = -2966932725306192437L;
			
			final AbilityFactory af = AF;
			final HashMap<String,String> params = af.getMapParams();
		
			@Override
			public String getStackDescription(){
				// when getStackDesc is called, just build exactly what is happening
				return loseLifeStackDescription(af, this);
			}
			
			public boolean canPlayAI()
			{
				// if X depends on abCost, the AI needs to choose which card he would sacrifice first
				// then call xCount with that card to properly calculate the amount
				// Or choosing how many to sacrifice 
				return loseLifeCanPlayAI(af, this, params.get("LifeAmount"));
			}
			
			@Override
			public void resolve() {
				int amount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("LifeAmount"), this);
				loseLifeResolve(af, this, amount);
			}

			@Override
			public boolean chkAI_Drawback() {
				return true;
			}
		};
		return dbLoseLife;
	}

	public static String gainLifeStackDescription(AbilityFactory af, SpellAbility sa){
		StringBuilder sb = new StringBuilder();
		int amount = AbilityFactory.calculateAmount(af.getHostCard(), af.getMapParams().get("LifeAmount"), sa);

		if (!(sa instanceof Ability_Sub))
			sb.append(sa.getSourceCard().getName()).append(" - ");
		else
			sb.append(" ");

		String player = "You gain ";
		if (af.getAbTgt() != null)
			player = sa.getTargetPlayer() + " gains ";
		sb.append(player).append(amount).append(" life.");

		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null) {
			abSub.setParent(sa);
			sb.append(abSub.getStackDescription());
		}

		return sb.toString();
	}
	
	static String loseLifeStackDescription(AbilityFactory af, SpellAbility sa){
		 StringBuilder sb = new StringBuilder();
		 int amount = AbilityFactory.calculateAmount(af.getHostCard(), af.getMapParams().get("LifeAmount"), sa);
	
		 Player player = sa.getActivatingPlayer().getOpponent();
	
			if (!(sa instanceof Ability_Sub))
				sb.append(sa.getSourceCard().getName()).append(" - ");
			else
				sb.append(" ");
		 
		 if (af.getAbTgt() != null)
			 player = sa.getTargetPlayer();
		 sb.append(player).append(" loses ").append(amount).append(" life.");
	
		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null) {
			abSub.setParent(sa);
			sb.append(abSub.getStackDescription());
		}
		 
		 return sb.toString();
	}
	
	public static boolean gainLifeCanPlayAI(final AbilityFactory af, final SpellAbility sa, final String amountStr){
		Random r = new Random();
		Ability_Cost abCost = sa.getPayCosts();
		final Card source = sa.getSourceCard();
		int life = AllZone.ComputerPlayer.getLife();

		if (abCost != null){
			// AI currently disabled for these costs
			if (abCost.getSacCost()){
				if (amountStr.contains("X"))
					return false;
				if (life > 4)
					return false;
			}
			if (abCost.getLifeCost() && life > 5)	 return false;
			if (abCost.getDiscardCost() && life > 5) return false;
			
			if (abCost.getSubCounter() && life > 7){
				// A card has a 25% chance per counter to be able to pass through here
				// 4+ counters will always pass. 0 counters will never
				int currentNum = source.getCounters(abCost.getCounterType());
				double percent = .25 * (currentNum / abCost.getCounterNum());
				if (percent <= r.nextFloat())
					return false;
			}
		}
		
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		// TODO handle proper calculation of X values based on Cost and what would be paid
		//final int amount = calculateAmount(af.getHostCard(), amountStr, sa);
		
		 // prevent run-away activations - first time will always return true
		 boolean chance = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed());
		 
		 Target tgt = sa.getTarget();
		 
		 if (sa.getTarget() != null){
			 tgt.resetTargets();
			 sa.getTarget().addTarget(AllZone.ComputerPlayer);
		 }

		 return ((r.nextFloat() < .6667) && chance);
	}
	
	public static void gainLifeResolve(final AbilityFactory af, final SpellAbility sa, int lifeAmount){
		HashMap<String,String> params = af.getMapParams();
		Card card = af.getHostCard();
		
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
				p.gainLife(lifeAmount, sa.getSourceCard());
		
		
		if (af.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
	     	   if (abSub.getParent() == null)
	     		  abSub.setParent(sa);
	     	   abSub.resolve();
	        }
			else{
				String DrawBack = params.get("SubAbility");
				if (af.hasSubAbility())
					 CardFactoryUtil.doDrawBack(DrawBack, lifeAmount, card.getController(), card.getController().getOpponent(), tgtPlayers.get(0), card, null, sa);
			}
		}
	}
	
	public static boolean loseLifeCanPlayAI(final AbilityFactory af, final SpellAbility sa, final String amountStr){
		Random r = new Random();
		Ability_Cost abCost = sa.getPayCosts();
		final Card source = sa.getSourceCard();
		int humanLife = AllZone.HumanPlayer.getLife();
		int aiLife = AllZone.ComputerPlayer.getLife();

		// TODO handle proper calculation of X values based on Cost and what would be paid
		final int amount = AbilityFactory.calculateAmount(af.getHostCard(), amountStr, sa);
		
		if (abCost != null){
			// AI currently disabled for these costs
			if (abCost.getSacCost()){
				if (amountStr.contains("X"))
					return false;
			}
			if (abCost.getLifeCost() && aiLife - abCost.getLifeAmount() < humanLife - amount)	 return false;
			if (abCost.getDiscardCost()) return false;
			
			if (abCost.getSubCounter()){
				// A card has a 25% chance per counter to be able to pass through here
				// 4+ counters will always pass. 0 counters will never
				int currentNum = source.getCounters(abCost.getCounterType());
				double percent = .25 * (currentNum / abCost.getCounterNum());
				if (percent <= r.nextFloat())
					return false;
			}
		}
		
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		 // prevent run-away activations - first time will always return true
		 boolean chance = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed());
		 
		 Target tgt = sa.getTarget();
		 
		 if (sa.getTarget() != null){
			 tgt.resetTargets();
			 sa.getTarget().addTarget(AllZone.HumanPlayer);
		 }
		 
		 return ((r.nextFloat() < .6667) && chance);
	}
	
	public static void loseLifeResolve(final AbilityFactory af, final SpellAbility sa, int lifeAmount){
		HashMap<String,String> params = af.getMapParams();
		Card card = af.getHostCard();
		
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
				p.loseLife(lifeAmount, sa.getSourceCard());
		
		if (af.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
	     	   if (abSub.getParent() == null)
	     		  abSub.setParent(sa);
	     	   abSub.resolve();
	        }
			else{
				String DrawBack = params.get("SubAbility");
				if (af.hasSubAbility())
					 CardFactoryUtil.doDrawBack(DrawBack, lifeAmount, card.getController(), card.getController().getOpponent(), tgtPlayers.get(0), card, null, sa);
			}
		}	
	}
}
