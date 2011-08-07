package forge.card.abilityFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.ComputerUtil;
import forge.Constant;
import forge.Counters;
import forge.MyRandom;
import forge.Player;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Cost;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;

public class AbilityFactory_AlterLife {
	// An AbilityFactory subclass for Gaining, Losing, or Setting Life totals.
	
	// *************************************************************************
	// ************************* GAIN LIFE *************************************
	// *************************************************************************
	
	public static SpellAbility createAbilityGainLife(final AbilityFactory AF){

		final SpellAbility abGainLife = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 8869422603616247307L;
			
			final AbilityFactory af = AF;
		
			@Override
			public String getStackDescription(){
				// when getStackDesc is called, just build exactly what is happening
				return gainLifeStackDescription(af, this);
			}

			public boolean canPlayAI()
			{
				return gainLifeCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				gainLifeResolve(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return gainLifeDoTriggerAI(af, this, mandatory);
			}
			
		};
		return abGainLife;
	}
	
	public static SpellAbility createSpellGainLife(final AbilityFactory AF){
		final SpellAbility spGainLife = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 6631124959690157874L;
			
			final AbilityFactory af = AF;
		
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
				return gainLifeCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				gainLifeResolve(af, this);
			}
			
		};
		return spGainLife;
	}
	
	public static SpellAbility createDrawbackGainLife(final AbilityFactory AF){
		final SpellAbility dbGainLife = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()){
			private static final long serialVersionUID = 6631124959690157874L;
			
			final AbilityFactory af = AF;
		
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
				return gainLifeCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				gainLifeResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return true;
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return gainLifeDoTriggerAI(af, this, mandatory);
			}
			
		};
		return dbGainLife;
	}

	public static String gainLifeStackDescription(AbilityFactory af, SpellAbility sa){
		StringBuilder sb = new StringBuilder();
		int amount = AbilityFactory.calculateAmount(af.getHostCard(), af.getMapParams().get("LifeAmount"), sa);

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
		
		sb.append("gains ").append(amount).append(" life.");

		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null) {
			sb.append(abSub.getStackDescription());
		}

		return sb.toString();
	}
	
	public static boolean gainLifeCanPlayAI(final AbilityFactory af, final SpellAbility sa){
		Random r = MyRandom.random;
		HashMap<String,String> params = af.getMapParams();
		Cost abCost = sa.getPayCosts();
		final Card source = sa.getSourceCard();
		int life = AllZone.ComputerPlayer.getLife();
		int lifeAmount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("LifeAmount"), sa);
		String amountStr = af.getMapParams().get("LifeAmount");
		
		//don't use it if no life to gain
		if (lifeAmount <= 0) return false;

		if (abCost != null){
			// AI currently disabled for these costs
			if (abCost.getSacCost()){
				if (life > 4)
					return false;
			}
			if (abCost.getLifeCost() && life > 5)	 return false;
			if (abCost.getDiscardCost() && life > 5) return false;

			if (abCost.getSubCounter()){
				//non +1/+1 counters should be used
				if (abCost.getCounterType().equals(Counters.P1P1)){
					// A card has a 25% chance per counter to be able to pass through here
					// 4+ counters will always pass. 0 counters will never
				int currentNum = source.getCounters(abCost.getCounterType());
				double percent = .25 * (currentNum / abCost.getCounterNum());
				if (percent <= r.nextFloat())
					return false;
				}
			}
		}

		if (!ComputerUtil.canPayCost(sa))
			return false;

		if (!AllZone.ComputerPlayer.canGainLife())
			return false;
		
		//Don't use lifegain before main 2 if possible
		if(AllZone.Phase.isBefore(Constant.Phase.Main2) && !params.containsKey("ActivatingPhases"))
        	return false;

		// TODO handle proper calculation of X values based on Cost and what would be paid
		//final int amount = calculateAmount(af.getHostCard(), amountStr, sa);

		// prevent run-away activations - first time will always return true
		boolean chance = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed());

		Target tgt = sa.getTarget();
		if (tgt != null){
			tgt.resetTargets();
			if (tgt.canOnlyTgtOpponent())
				tgt.addTarget(AllZone.HumanPlayer);
			else
				tgt.addTarget(AllZone.ComputerPlayer);		
		}

		if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")){
			// Set PayX here to maximum value.
			int xPay = ComputerUtil.determineLeftoverMana(sa);
			source.setSVar("PayX", Integer.toString(xPay));
		}
		
		boolean randomReturn = r.nextFloat() <= .6667;
		if (AbilityFactory.playReusable(sa))
			randomReturn = true;

		return (randomReturn && chance);
	}
	
	public static boolean gainLifeDoTriggerAI(AbilityFactory af, SpellAbility sa, boolean mandatory){
		if (!ComputerUtil.canPayCost(sa) && !mandatory)	// If there is a cost payment it's usually not mandatory
			return false;

		// If the Target is gaining life, target self.
		// if the Target is modifying how much life is gained, this needs to be handled better
		Target tgt = sa.getTarget();
		if (tgt != null){
			tgt.resetTargets();
			if (tgt.canOnlyTgtOpponent())
				tgt.addTarget(AllZone.HumanPlayer);
			else
				tgt.addTarget(AllZone.ComputerPlayer);		
		}

		Card source = sa.getSourceCard();
		String amountStr = af.getMapParams().get("LifeAmount");
		if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")){
			// Set PayX here to maximum value.
			int xPay = ComputerUtil.determineLeftoverMana(sa);
			source.setSVar("PayX", Integer.toString(xPay));
		}

		// check SubAbilities DoTrigger?
		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null) {
			return abSub.doTrigger(mandatory);
		}

		return true;
	}
	
	public static void gainLifeResolve(final AbilityFactory af, final SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		Card card = af.getHostCard();
		int lifeAmount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("LifeAmount"), sa);
		ArrayList<Player> tgtPlayers;

		Target tgt = af.getAbTgt();
		if (tgt != null && !params.containsKey("Defined"))
			tgtPlayers = tgt.getTargetPlayers();
		else
			tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
		
		for(Player p : tgtPlayers)
			if (tgt == null || p.canTarget(af.getHostCard()))
				p.gainLife(lifeAmount, sa.getSourceCard());
		
		if (af.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
	     	   abSub.resolve();
	        }
			else{
				String DrawBack = params.get("SubAbility");
				if (af.hasSubAbility())
					 CardFactoryUtil.doDrawBack(DrawBack, lifeAmount, card.getController(), card.getController().getOpponent(), tgtPlayers.get(0), card, null, sa);
			}
		}
	}
	
	// *************************************************************************
	// ************************* LOSE LIFE *************************************
	// *************************************************************************
	
	public static SpellAbility createAbilityLoseLife(final AbilityFactory AF){
		final SpellAbility abLoseLife = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 1129762905315395160L;
			
			final AbilityFactory af = AF;
		
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
				return loseLifeCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				loseLifeResolve(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return loseLifeDoTriggerAI(af, this, mandatory);
			}
		};
		return abLoseLife;
	}
	
	public static SpellAbility createSpellLoseLife(final AbilityFactory AF){
		final SpellAbility spLoseLife = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -2966932725306192437L;
			
			final AbilityFactory af = AF;
		
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
				return loseLifeCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				loseLifeResolve(af, this);
			}
		};
		return spLoseLife;
	}
	
	public static SpellAbility createDrawbackLoseLife(final AbilityFactory AF){
		final SpellAbility dbLoseLife = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()){
			private static final long serialVersionUID = -2966932725306192437L;
			
			final AbilityFactory af = AF;

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
				return loseLifeCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				loseLifeResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return true;
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return loseLifeDoTriggerAI(af, this, mandatory);
			}
		};
		return dbLoseLife;
	}
	
	static String loseLifeStackDescription(AbilityFactory af, SpellAbility sa) {
		StringBuilder sb = new StringBuilder();
		int amount = AbilityFactory.calculateAmount(af.getHostCard(), af.getMapParams().get("LifeAmount"), sa);

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

		sb.append("loses ").append(amount).append(" life.");

		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null) {
			sb.append(abSub.getStackDescription());
		}

		return sb.toString();
	}
	
	public static boolean loseLifeCanPlayAI(final AbilityFactory af, final SpellAbility sa){
		Random r = MyRandom.random;
		Cost abCost = sa.getPayCosts();
		final Card source = sa.getSourceCard();
		HashMap<String,String> params = af.getMapParams();
		int humanLife = AllZone.HumanPlayer.getLife();
		int aiLife = AllZone.ComputerPlayer.getLife();

		String amountStr = params.get("LifeAmount");
		
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
		
		if (!AllZone.HumanPlayer.canLoseLife())
			return false;
		
		//Don't use loselife before main 2 if possible
		if(AllZone.Phase.isBefore(Constant.Phase.Main2) && !params.containsKey("ActivatingPhases"))
        	return false;
		
		 // prevent run-away activations - first time will always return true
		 boolean chance = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed());
		 
		 Target tgt = sa.getTarget();
		 
		 if (sa.getTarget() != null){
			 tgt.resetTargets();
			 sa.getTarget().addTarget(AllZone.HumanPlayer);
		 }
		 
		 if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")){
			 // Set PayX here to maximum value.
			 int xPay = ComputerUtil.determineLeftoverMana(sa);
			 source.setSVar("PayX", Integer.toString(xPay));
		 }
		 
			boolean randomReturn = r.nextFloat() <= .6667;
			if (AbilityFactory.playReusable(sa))
				randomReturn = true;

		 return (randomReturn && chance);
	}
	
	public static boolean loseLifeDoTriggerAI(AbilityFactory af, SpellAbility sa, boolean mandatory){
		if (!ComputerUtil.canPayCost(sa) && !mandatory)	// If there is a cost payment it's usually not mandatory
			return false;

		 Target tgt = sa.getTarget();
		 if (tgt != null){
			 tgt.addTarget(AllZone.HumanPlayer);
		 }
		 
		 Card source = sa.getSourceCard();
		 String amountStr = af.getMapParams().get("LifeAmount");
		 int amount = 0;
		 if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")){
			 // Set PayX here to maximum value.
			 int xPay = ComputerUtil.determineLeftoverMana(sa);
			 source.setSVar("PayX", Integer.toString(xPay));
			 amount = xPay;
		 }
		 else
			 amount = AbilityFactory.calculateAmount(source, amountStr, sa);
		 
		ArrayList<Player> tgtPlayers;
		if (tgt != null)
			tgtPlayers = tgt.getTargetPlayers();
		else
			tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
		
		
		if (tgtPlayers.contains(AllZone.ComputerPlayer)){
			// For cards like Foul Imp, ETB you lose life
			if (amount + 3 > AllZone.ComputerPlayer.getLife())
				return false;
		}
		 
		// check SubAbilities DoTrigger?
		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null) {
			return abSub.doTrigger(mandatory);
		}
		
		return true;
	}
	
	public static void loseLifeResolve(final AbilityFactory af, final SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		Card card = af.getHostCard();
		int lifeAmount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("LifeAmount"), sa);
		
		ArrayList<Player> tgtPlayers;
		
		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtPlayers = tgt.getTargetPlayers();
		else
			tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
		
		for(Player p : tgtPlayers)
			if (tgt == null || p.canTarget(af.getHostCard()))
				p.loseLife(lifeAmount, sa.getSourceCard());
		
		if (af.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
	     	   abSub.resolve();
	        }
			else{
				String DrawBack = params.get("SubAbility");
				if (af.hasSubAbility())
					 CardFactoryUtil.doDrawBack(DrawBack, lifeAmount, card.getController(), card.getController().getOpponent(), tgtPlayers.get(0), card, null, sa);
			}
		}	
	}
	
	// *************************************************************************
	// ************************** Poison Counters ******************************
	// *************************************************************************
	//
	// Made more sense here than in AF_Counters since it affects players and their health
	
	public static SpellAbility createAbilityPoison(final AbilityFactory AF){

		final SpellAbility abPoison = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 6598936088284756268L;
			final AbilityFactory af = AF;
			final HashMap<String,String> params = af.getMapParams();
		
			@Override
			public String getStackDescription(){
				// when getStackDesc is called, just build exactly what is happening
				return poisonStackDescription(af, this);
			}
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();	
			}
			
			public boolean canPlayAI()
			{
				return poisonCanPlayAI(af, this, params.get("Num"));
			}
			
			@Override
			public void resolve() {
				int amount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("Num"), this);
				poisonResolve(af, this, amount);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return poisonDoTriggerAI(af, this, mandatory);
			}
			
		};
		return abPoison;
	}
	
	public static SpellAbility createSpellPoison(final AbilityFactory AF){
		final SpellAbility spPoison = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -1495708415138457833L;
			final AbilityFactory af = AF;
			final HashMap<String,String> params = af.getMapParams();
		
			@Override
			public String getStackDescription(){
				return poisonStackDescription(af, this);
			}
			
			public boolean canPlay(){
				return super.canPlay();	
			}
			
			public boolean canPlayAI() {
				// if X depends on abCost, the AI needs to choose which card he would sacrifice first
				// then call xCount with that card to properly calculate the amount
				// Or choosing how many to sacrifice 
				return poisonCanPlayAI(af, this, params.get("Num"));
			}
			
			@Override
			public void resolve() {
				int amount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("Num"), this);
				poisonResolve(af, this, amount);
			}
			
		};
		return spPoison;
	}
	
	public static SpellAbility createDrawbackPoison(final AbilityFactory AF){
		final SpellAbility dbPoison = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()){
			private static final long serialVersionUID = -1173479041548558016L;
			final AbilityFactory af = AF;
			final HashMap<String,String> params = af.getMapParams();
		
			@Override
			public String getStackDescription(){
				return poisonStackDescription(af, this);
			}
			
			public boolean canPlayAI()
			{
				// if X depends on abCost, the AI needs to choose which card he would sacrifice first
				// then call xCount with that card to properly calculate the amount
				// Or choosing how many to sacrifice 
				return poisonCanPlayAI(af, this, params.get("Num"));
			}
			
			@Override
			public void resolve() {
				int amount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("Num"), this);
				poisonResolve(af, this, amount);
			}

			@Override
			public boolean chkAI_Drawback() {
				return true;
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return poisonDoTriggerAI(af, this, mandatory);
			}
			
		};
		return dbPoison;
	}
	
	public static boolean poisonDoTriggerAI(AbilityFactory af, SpellAbility sa, boolean mandatory){
		if (!ComputerUtil.canPayCost(sa) && !mandatory)	// If there is a cost payment it's usually not mandatory
			return false;

		 Target tgt = sa.getTarget();
		 if (tgt != null){
			 tgt.addTarget(AllZone.HumanPlayer);
		 }
		 else{
			 ArrayList<Player> players = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
			 for(Player p : players)
				 if (!mandatory && p.isComputer() && p.getPoisonCounters() > p.getOpponent().getPoisonCounters())
					 return false;
		 }
		 
		// check SubAbilities DoTrigger?
		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null) {
			return abSub.doTrigger(mandatory);
		}
		
		return true;
	}
	
	private static void poisonResolve(final AbilityFactory af, final SpellAbility sa, int num){
		HashMap<String,String> params = af.getMapParams();
		Card card = af.getHostCard();
		
		ArrayList<Player> tgtPlayers;

		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtPlayers = tgt.getTargetPlayers();
		else
			tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
		
		for(Player p : tgtPlayers)
			if (tgt == null || p.canTarget(af.getHostCard()))
				p.addPoisonCounters(num);
		
		
		if (af.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
	     	   abSub.resolve();
	        }
			else{
				String DrawBack = params.get("SubAbility");
				if (af.hasSubAbility())
					 CardFactoryUtil.doDrawBack(DrawBack, num, card.getController(), card.getController().getOpponent(), tgtPlayers.get(0), card, null, sa);
			}
		}
	}
	
	private static String poisonStackDescription(AbilityFactory af, SpellAbility sa){
		StringBuilder sb = new StringBuilder();
		int amount = AbilityFactory.calculateAmount(af.getHostCard(), af.getMapParams().get("Num"), sa);

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
		
		sb.append("gets ").append(amount).append(" poison counter");
		if(amount != 1) sb.append("s.");
		else sb.append(".");

		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null) {
			sb.append(abSub.getStackDescription());
		}

		return sb.toString();
	}
	
	private static boolean poisonCanPlayAI(final AbilityFactory af, final SpellAbility sa, final String amountStr){
		Cost abCost = sa.getPayCosts();
		HashMap<String,String> params = af.getMapParams();
		//int humanPoison = AllZone.HumanPlayer.getPoisonCounters();
		//int humanLife = AllZone.HumanPlayer.getLife();
		//int aiPoison = AllZone.ComputerPlayer.getPoisonCounters();
		int aiLife = AllZone.ComputerPlayer.getLife();

		// TODO handle proper calculation of X values based on Cost and what would be paid
		//final int amount = AbilityFactory.calculateAmount(af.getHostCard(), amountStr, sa);
		
		if (abCost != null){
			// AI currently disabled for these costs
			if (abCost.getSacCost()){
				if (amountStr.contains("X"))
					return false;
			}
			if(abCost.getLifeCost() && aiLife - abCost.getLifeAmount() <= 0) return false;
		}
		
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		//Don't use poison before main 2 if possible
		if(AllZone.Phase.isBefore(Constant.Phase.Main2) && !params.containsKey("ActivatingPhases"))
        	return false;
		 
		 Target tgt = sa.getTarget();
		 
		 if (sa.getTarget() != null){
			 tgt.resetTargets();
			 sa.getTarget().addTarget(AllZone.HumanPlayer);
		 }
		 
		 return true;
	}

	// *************************************************************************
	// ************************** SET LIFE *************************************
	// *************************************************************************

	public static SpellAbility createAbilitySetLife(final AbilityFactory AF) {
		final SpellAbility abSetLife = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
			private static final long serialVersionUID = -7375434097541097668L;
			final AbilityFactory af = AF;

			@Override
			public String getStackDescription() {
				return setLifeStackDescription(af, this);
			}

			public boolean canPlayAI() {
				return setLifeCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				setLifeResolve(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return setLifeDoTriggerAI(af, this, mandatory);
			}

		};
		return abSetLife;
	}

	public static SpellAbility createSpellSetLife(final AbilityFactory AF) {
		final SpellAbility spSetLife = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
			private static final long serialVersionUID = -94657822256270222L;
			final AbilityFactory af = AF;

			@Override
			public String getStackDescription() {
				return setLifeStackDescription(af, this);
			}

			public boolean canPlayAI() {
				// if X depends on abCost, the AI needs to choose which card he would sacrifice first
				// then call xCount with that card to properly calculate the amount
				// Or choosing how many to sacrifice 
				return setLifeCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				setLifeResolve(af, this);
			}

		};
		return spSetLife;
	}

	public static SpellAbility createDrawbackSetLife(final AbilityFactory AF) {
		final SpellAbility dbSetLife = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()) {
			private static final long serialVersionUID = -7634729949893534023L;
			final AbilityFactory af = AF;

			@Override
			public String getStackDescription() {
				return setLifeStackDescription(af, this);
			}

			public boolean canPlayAI() {
				// if X depends on abCost, the AI needs to choose which card he would sacrifice first
				// then call xCount with that card to properly calculate the amount
				// Or choosing how many to sacrifice 
				return setLifeCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				setLifeResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return true;
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return setLifeDoTriggerAI(af, this, mandatory);
			}

		};
		return dbSetLife;
	}

	private static String setLifeStackDescription(AbilityFactory af, SpellAbility sa) {
		StringBuilder sb = new StringBuilder();
		int amount = AbilityFactory.calculateAmount(af.getHostCard(), af.getMapParams().get("LifeAmount"), sa);

		if (!(sa instanceof Ability_Sub))
			sb.append(sa.getSourceCard().getName()).append(" -");

		sb.append(" ");

		ArrayList<Player> tgtPlayers;

		Target tgt = af.getAbTgt();
		if(tgt != null)
			tgtPlayers = tgt.getTargetPlayers();
		else
			tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);

		for(Player player : tgtPlayers)
			sb.append(player).append(" ");

		sb.append("life total becomes ").append(amount).append(".");

		Ability_Sub abSub = sa.getSubAbility();
		if(abSub != null) {
			sb.append(abSub.getStackDescription());
		}

		return sb.toString();
	}

	private static boolean setLifeCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
		Random r = MyRandom.random;
		//Ability_Cost abCost = sa.getPayCosts();
		final Card source = sa.getSourceCard();
		int life = AllZone.ComputerPlayer.getLife();
		int hlife = AllZone.HumanPlayer.getLife();
		HashMap<String,String> params = af.getMapParams();
		String amountStr = params.get("LifeAmount");

		if(!ComputerUtil.canPayCost(sa))
			return false;

		if(!AllZone.ComputerPlayer.canGainLife())
			return false;
		
		//Don't use setLife before main 2 if possible
		if(AllZone.Phase.isBefore(Constant.Phase.Main2) && !params.containsKey("ActivatingPhases"))
        	return false;

		// TODO handle proper calculation of X values based on Cost and what would be paid
		int amount;
		//we shouldn't have to worry too much about PayX for SetLife
		if(amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
			// Set PayX here to maximum value.
			int xPay = ComputerUtil.determineLeftoverMana(sa);
			source.setSVar("PayX", Integer.toString(xPay));
			amount = xPay;
		}
		else
			amount = AbilityFactory.calculateAmount(af.getHostCard(), amountStr, sa);

		// prevent run-away activations - first time will always return true
		boolean chance = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed());

		Target tgt = sa.getTarget();
		if(tgt != null){
			tgt.resetTargets();
			if(tgt.canOnlyTgtOpponent()) {
				tgt.addTarget(AllZone.HumanPlayer);
				//if we can only target the human, and the Human's life would go up, don't play it.
				//possibly add a combo here for Magister Sphinx and Higedetsu's (sp?) Second Rite
				if(amount > hlife || !AllZone.HumanPlayer.canLoseLife()) return false;
			}
			else {
				if(amount > life && life <= 10) tgt.addTarget(AllZone.ComputerPlayer);
				else if(hlife > amount) tgt.addTarget(AllZone.HumanPlayer);
				else if(amount > life) tgt.addTarget(AllZone.ComputerPlayer);
				else return false;
			}
		}
		else {
			if(params.containsKey("Each") && params.get("Defined").equals("Each")) {
				if(amount == 0) return false;
				else if(life > amount) { //will decrease computer's life
					if( life < 5 || ((life - amount) > (hlife - amount))) return false;
				}
			}
			if(amount < life) return false;
		}
		
		//if life is in danger, always activate
		if(life < 3 && amount > life) return true;
		
		return ((r.nextFloat() < .6667) && chance);
	}

	private static boolean setLifeDoTriggerAI(AbilityFactory af, SpellAbility sa, boolean mandatory){
		int life = AllZone.ComputerPlayer.getLife();
		int hlife = AllZone.HumanPlayer.getLife();
		Card source = sa.getSourceCard();
		String amountStr = af.getMapParams().get("LifeAmount");
		if (!ComputerUtil.canPayCost(sa) && !mandatory)   // If there is a cost payment it's usually not mandatory
			return false;
		
		int amount;
		if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")){
			// Set PayX here to maximum value.
			int xPay = ComputerUtil.determineLeftoverMana(sa);
			source.setSVar("PayX", Integer.toString(xPay));
			amount = xPay;
		}
		else
			amount = AbilityFactory.calculateAmount(af.getHostCard(), amountStr, sa);
		
		if(source.getName().equals("Eternity Vessel") && 
				(AllZoneUtil.isCardInPlay("Vampire Hexmage", AllZone.HumanPlayer) || (source.getCounters(Counters.CHARGE) == 0))) return false;

		// If the Target is gaining life, target self.
		// if the Target is modifying how much life is gained, this needs to be handled better
		Target tgt = sa.getTarget();
		if (tgt != null){
			tgt.resetTargets();
			if (tgt.canOnlyTgtOpponent())
				tgt.addTarget(AllZone.HumanPlayer);
			else {
				if(amount > life && life <= 10) tgt.addTarget(AllZone.ComputerPlayer);
				else if(hlife > amount) tgt.addTarget(AllZone.HumanPlayer);
				else if(amount > life) tgt.addTarget(AllZone.ComputerPlayer);
				else return false;
			}
		}

		// check SubAbilities DoTrigger?
		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null) {
			return abSub.doTrigger(mandatory);
		}

		return true;
	}

	private static void setLifeResolve(final AbilityFactory af, final SpellAbility sa) {
		HashMap<String,String> params = af.getMapParams();
		Card card = af.getHostCard();
		int lifeAmount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("LifeAmount"), sa);
		ArrayList<Player> tgtPlayers;

		Target tgt = af.getAbTgt();
		if(tgt != null && !params.containsKey("Defined"))
			tgtPlayers = tgt.getTargetPlayers();
		else
			tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);

		for(Player p : tgtPlayers)
			if(tgt == null || p.canTarget(af.getHostCard()))
				p.setLife(lifeAmount, sa.getSourceCard());

		if(af.hasSubAbility()) {
			Ability_Sub abSub = sa.getSubAbility();
			if(abSub != null) {
				abSub.resolve();
			}
			else {
				String DrawBack = params.get("SubAbility");
				if(af.hasSubAbility())
					CardFactoryUtil.doDrawBack(DrawBack, lifeAmount, card.getController(), card.getController().getOpponent(), tgtPlayers.get(0), card, null, sa);
			}
		}
	}

}//end class AbilityFactory_AlterLife
