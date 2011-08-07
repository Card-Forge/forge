
package forge.card.abilityFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
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

public class AbilityFactory_DealDamage {
	private AbilityFactory AF = null;

	private String damage;

	public AbilityFactory_DealDamage(AbilityFactory newAF)
	{
		AF = newAF;

		damage = AF.getMapParams().get("NumDmg");

		// Note: TgtOpp should not be used, Please use ValidTgts$ Opponent instead
	}

	// ******************************************************************************************************
	// ***************************** DAMAGE *****************************************************************
	// ******************************************************************************************************

	public SpellAbility getAbility() {
		final SpellAbility abDamage = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
			private static final long serialVersionUID = -7560349014757367722L;

			@Override
			public boolean canPlayAI() {
				return doCanPlayAI(this);
			}

			@Override
			public String getStackDescription() {
				return damageStackDescription(AF, this);
			}

			@Override
			public void resolve() {
				doResolve(this);
				AF.getHostCard().setAbilityUsed(AF.getHostCard().getAbilityUsed() + 1);

			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return damageDoTriggerAI(AF, this, mandatory);
			}
		};// Ability_Activated

		return abDamage;
	}

	public SpellAbility getSpell() {
		final SpellAbility spDealDamage = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
			private static final long serialVersionUID = 7239608350643325111L;

			@Override
			public boolean canPlayAI() {
				return doCanPlayAI(this);

			}

			@Override
			public String getStackDescription() {
				return damageStackDescription(AF, this);
			}

			@Override
			public void resolve() {
				doResolve(this);
			}

		}; // Spell

		return spDealDamage;
	}

	public SpellAbility getDrawback() {
		final SpellAbility dbDealDamage = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()) {
			private static final long serialVersionUID = 7239608350643325111L;

			@Override
			public boolean chkAI_Drawback() {
				// Make sure there is a valid target
				return damageDrawback(this);
			}

			@Override
			public String getStackDescription() {
				return damageStackDescription(AF, this);
			}

			@Override
			public void resolve() {
				doResolve(this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return damageDoTriggerAI(AF, this, mandatory);
			}

		}; // Drawback

		return dbDealDamage;
	}

	private String damageStackDescription(AbilityFactory af, SpellAbility sa){
		// when damageStackDescription is called, just build exactly what is happening
		StringBuilder sb = new StringBuilder();
		String name = af.getHostCard().toString();
		int dmg = getNumDamage(sa);

		ArrayList<Object> tgts;
		if(sa.getTarget() == null) 
			tgts = AbilityFactory.getDefinedObjects(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
		else 
			tgts = sa.getTarget().getTargets();

		if (!(sa instanceof Ability_Sub))
			sb.append(name).append(" -");
		sb.append(" ");

		String conditionDesc = af.getMapParams().get("ConditionDescription");
		if (conditionDesc != null)
			sb.append(conditionDesc).append(" ");
		
		ArrayList<Card> definedSources = AbilityFactory.getDefinedCards(sa.getSourceCard(), af.getMapParams().get("DamageSource"), sa);
		Card source = definedSources.get(0);
		
		if (source != sa.getSourceCard())
			sb.append(source.toString()).append(" deals");
		else
			sb.append("Deals");
		
		sb.append(" ").append(dmg).append(" damage to ");

		for(int i = 0; i < tgts.size(); i++){
			if (i != 0)
				sb.append(" ");

			Object o = tgts.get(i);
			if (o instanceof Card || o instanceof Player)
				sb.append(o.toString());
		}

		sb.append(". ");

		if (sa.getSubAbility() != null){
			sb.append(sa.getSubAbility().getStackDescription());
		}

		return sb.toString();
	}

	private int getNumDamage(SpellAbility saMe) {
		return AbilityFactory.calculateAmount(saMe.getSourceCard(), damage, saMe);
	}

	private boolean damageDrawback(SpellAbility sa){
		Card source = sa.getSourceCard();
		int dmg;
		if (damage.equals("X") && source.getSVar(damage).equals("Count$xPaid")){
			// Set PayX here to maximum value.
			dmg = ComputerUtil.determineLeftoverMana(sa);
			source.setSVar("PayX", Integer.toString(dmg));
		}
		else
			dmg = getNumDamage(sa);
		return damageTargetAI(sa, dmg);
	}
	
	private boolean doCanPlayAI(SpellAbility saMe)
	{   
		if (!ComputerUtil.canPayCost(saMe))
			return false;
		
		Card source = saMe.getSourceCard();
		
		int dmg = 0;
		if (damage.equals("X") && source.getSVar(damage).equals("Count$xPaid")){
			// Set PayX here to maximum value.
			dmg = ComputerUtil.determineLeftoverMana(saMe);
			source.setSVar("PayX", Integer.toString(dmg));
		}
		else
			dmg = getNumDamage(saMe);
		boolean rr = AF.isSpell();

		// temporarily disabled until better AI
		if (AF.getAbCost().getSacCost())    {
			if(AF.getHostCard().isCreature() && AllZone.HumanPlayer.getLife() - dmg > 0) // only if damage from this ability would kill the human
				return false;
		}
		if (AF.getAbCost().getSubCounter())  {
			// +1/+1 counters only if damage from this ability would kill the human, otherwise ok
			if(AllZone.HumanPlayer.getLife() - dmg > 0 && AF.getAbCost().getCounterType().equals(Counters.P1P1))
				return false;
		}
		if (AF.getAbCost().getLifeCost())    {
			if(AllZone.HumanPlayer.getLife() - dmg > 0) // only if damage from this ability would kill the human
				return false;
		}

		if(source.getName().equals("Stuffy Doll")){
			// Now stuffy sits around for blocking
			// TODO(sol): this should also happen if Stuffy is going to die
			if (AllZone.Phase.is(Constant.Phase.End_Of_Turn, AllZone.HumanPlayer))
				return true;
			else 
				return false;
		}

		if (AF.isAbility())
		{
			Random r = MyRandom.random; // prevent run-away activations
			if(r.nextFloat() <= Math.pow(.6667, AF.getHostCard().getAbilityUsed()))
				rr = true;
		}

		boolean bFlag = damageTargetAI(saMe, dmg);
		if (!bFlag)
			return false;

		if (damage.equals("X") && source.getSVar(damage).equals("Count$xPaid")){
			// If I can kill my target by paying less mana, do it
			Target tgt = saMe.getTarget();
			if (tgt != null){
				int actualPay = 0;
				boolean noPrevention = AF.getMapParams().containsKey("NoPrevention");
				ArrayList<Card> cards = tgt.getTargetCards();
				for(Card c : cards){
					int adjDamage = c.getEnoughDamageToKill(dmg, source, false, noPrevention);
					if (adjDamage > actualPay)
						actualPay = adjDamage;
				}
				
				source.setSVar("PayX", Integer.toString(actualPay));
			}
		}
		
		
		Ability_Sub subAb = saMe.getSubAbility();
		if (subAb != null)
			rr &= subAb.chkAI_Drawback();
		return rr;
	}

	private boolean shouldTgtP(SpellAbility sa, int d, final boolean noPrevention) {
		int restDamage = d;
		Player human = AllZone.HumanPlayer;
		Player comp = AllZone.ComputerPlayer;

		if (!noPrevention)
			restDamage = human.predictDamage(restDamage, AF.getHostCard(), false);
		else restDamage = human.staticReplaceDamage(restDamage, AF.getHostCard(), false);

		if (restDamage == 0) return false;

		if (!human.canLoseLife()) return false;

		CardList hand = AllZoneUtil.getPlayerHand(comp);

		if (AF.isSpell()){
			// If this is a spell, cast it instead of discarding
			if ((AllZone.Phase.is(Constant.Phase.End_Of_Turn) || AllZone.Phase.is(Constant.Phase.Main2)) && 
					AllZone.Phase.isPlayerTurn(comp) && (hand.size() > comp.getMaxHandSize()))
				return true;
		}

		if(human.getLife() - restDamage < 5) // if damage from this spell would drop the human to less than 5 life
			return true;

		return false;
	}

	private Card chooseTgtC(final int d, final boolean noPrevention, final Player pl, final boolean mandatory) {
		Target tgt = AF.getAbTgt();
		final Card source = AF.getHostCard();
		CardList hPlay = AllZoneUtil.getPlayerCardsInPlay(pl);
		hPlay = hPlay.getValidCards(tgt.getValidTgts(), AllZone.ComputerPlayer, source);

		ArrayList<Object> objects = tgt.getTargets();
		for(Object o : objects){
			if (o instanceof Card){
				Card c = (Card)o;
				if (hPlay.contains(c))
					hPlay.remove(c);
			}
		}
		hPlay = hPlay.getTargetableCards(source);

		CardList killables = hPlay.filter(new CardListFilter() {
			public boolean addCard(Card c) {
				return (c.getEnoughDamageToKill(d, source, false, noPrevention) <= d ) 
					&& !(c.getSVar("SacMe").length() > 0);
			}
		});

		Card targetCard;
		if(pl.isHuman() && killables.size() > 0) {
			targetCard = CardFactoryUtil.AI_getBestCreature(killables);

			return targetCard;
		}

		if (!mandatory)
			return null;
		
		if(hPlay.size() > 0) {
			if (pl.isHuman())
				targetCard = CardFactoryUtil.AI_getBestCreature(hPlay);
			else
				targetCard = CardFactoryUtil.AI_getWorstCreature(hPlay);

			return targetCard;
		}

		return null;
	}

	private boolean damageTargetAI(SpellAbility saMe, int dmg) {
		Target tgt = AF.getAbTgt();

		if (tgt == null)
			return damageChooseNontargeted(saMe, dmg);

		return damageChoosingTargets(saMe, tgt, dmg, false);
	}

	private boolean damageChoosingTargets(SpellAbility saMe, Target tgt, int dmg, boolean mandatory){
		boolean noPrevention = AF.getMapParams().containsKey("NoPrevention");

		// target loop
		tgt.resetTargets();

		while (tgt.getNumTargeted() < tgt.getMaxTargets(saMe.getSourceCard(), saMe)) {
			// TODO: Consider targeting the planeswalker
			if (tgt.canTgtCreatureAndPlayer()) {

				if (shouldTgtP(saMe, dmg, noPrevention)) {
					if (tgt.addTarget(AllZone.HumanPlayer))
						continue;
				}

				Card c = chooseTgtC(dmg,noPrevention, AllZone.HumanPlayer, mandatory);
				if (c != null) {
					tgt.addTarget(c);
					continue;
				}
				
				// When giving priority to targeting Creatures for mandatory triggers
				// feel free to add the Human after we run out of good targets
				
				// TODO: add check here if card is about to die from something on the stack
				// or from taking combat damage
				boolean freePing = mandatory || AbilityFactory.playReusable(saMe);

				if (freePing && tgt.addTarget(AllZone.HumanPlayer))
					continue;
			}

			else if (tgt.canTgtCreature()) {
				Card c = chooseTgtC(dmg, noPrevention, AllZone.HumanPlayer, mandatory);
				if (c != null) {
					tgt.addTarget(c);
					continue;
				}
			}

			// TODO: Improve Damage, we shouldn't just target the player just because we can
			else if (tgt.canTgtPlayer()) {
				if (tgt.addTarget(AllZone.HumanPlayer))
					continue;
			}
			// fell through all the choices, no targets left?
			if ((tgt.getNumTargeted() < tgt.getMinTargets(saMe.getSourceCard(), saMe)
					|| tgt.getNumTargeted() == 0)) {
				if (!mandatory){
					tgt.resetTargets();
					return false;
				}
				else{
					// If the trigger is mandatory, gotta choose my own stuff now
					return damageChooseRequiredTargets(saMe, tgt, dmg, mandatory);
				}
			} else {
				// TODO is this good enough? for up to amounts?
				break;
			}
		}
		return true;
	}

	private boolean damageChooseNontargeted(SpellAbility saMe, int dmg){
		// TODO: Improve circumstances where the Defined Damage is unwanted
		ArrayList<Object> objects = AbilityFactory.getDefinedObjects(saMe.getSourceCard(), AF.getMapParams().get("Defined"), saMe);

		for(Object o : objects){
			if (o instanceof Card){
				//Card c = (Card)o;
			}
			else if (o instanceof Player){
				Player p = (Player)o;
				int restDamage = p.predictDamage(dmg, AF.getHostCard(), false);
				if (p.isComputer() && p.canLoseLife() && restDamage + 3 >= p.getLife() && restDamage > 0)	// Damage from this spell will kill me
					return false;
				if (p.isHuman() && !p.canLoseLife())
					return false;
			}			
		}
		return true;
	}

	private boolean damageChooseRequiredTargets(SpellAbility saMe, Target tgt, int dmg, boolean mandatory){
		// this is for Triggered targets that are mandatory
		boolean noPrevention = AF.getMapParams().containsKey("NoPrevention");

		while (tgt.getNumTargeted() < tgt.getMinTargets(saMe.getSourceCard(), saMe)) {
			// TODO: Consider targeting the planeswalker
			if (tgt.canTgtCreature()) {
				Card c = chooseTgtC(dmg, noPrevention, AllZone.ComputerPlayer, mandatory);
				if (c != null) {
					tgt.addTarget(c);
					continue;
				}
			}

			if (tgt.canTgtPlayer()) {
				if (tgt.addTarget(AllZone.ComputerPlayer))
					continue;
			}

			// if we get here then there isn't enough targets, this is the only time we can return false
			return false;
		}
		return true;
	}

	private boolean damageDoTriggerAI(AbilityFactory af, SpellAbility sa, boolean mandatory){
		if (!ComputerUtil.canPayCost(sa) && !mandatory)
			return false;

		Card source = sa.getSourceCard();
		int dmg;
		if (damage.equals("X") && source.getSVar(damage).equals("Count$xPaid")){
			// Set PayX here to maximum value.
			dmg = ComputerUtil.determineLeftoverMana(sa);
			source.setSVar("PayX", Integer.toString(dmg));
		}
		else
			dmg = getNumDamage(sa);
		
		Target tgt = sa.getTarget();
		if(tgt == null) {
			// If it's not mandatory check a few things
			if (!mandatory && !damageChooseNontargeted(sa, dmg)){
				return false;
			}
		}
		else{
			if (!damageChoosingTargets(sa, tgt, dmg, mandatory) && !mandatory)
				return false;
			
			if (damage.equals("X") && source.getSVar(damage).equals("Count$xPaid")){
				// If I can kill my target by paying less mana, do it
				int actualPay = 0;
				boolean noPrevention = AF.getMapParams().containsKey("NoPrevention");
				ArrayList<Card> cards = tgt.getTargetCards();
				for(Card c : cards){
					int adjDamage = c.getEnoughDamageToKill(dmg, source, false, noPrevention);
					if (adjDamage > actualPay)
						actualPay = adjDamage;
				}
				
				source.setSVar("PayX", Integer.toString(actualPay));
			}		
		}


		if (sa.getSubAbility() != null)
			return sa.getSubAbility().doTrigger(mandatory);

		return true;
	}


	private void doResolve(SpellAbility saMe)
	{
		HashMap<String,String> params = AF.getMapParams();
		if (!AbilityFactory.checkConditional(params, saMe)){
			AbilityFactory.resolveSubAbility(saMe);
			return;
		}
		
		int dmg = getNumDamage(saMe);		

		boolean noPrevention = params.containsKey("NoPrevention");

		ArrayList<Object> tgts;
		if(saMe.getTarget() == null) 
			tgts = AbilityFactory.getDefinedObjects(saMe.getSourceCard(), params.get("Defined"), saMe);
		else 
			tgts = saMe.getTarget().getTargets();

		boolean targeted = (AF.getAbTgt() != null);
	
		ArrayList<Card> definedSources = AbilityFactory.getDefinedCards(saMe.getSourceCard(), params.get("DamageSource"), saMe);
		Card source = definedSources.get(0);

		for(Object o : tgts){
			if (o instanceof Card){
				Card c = (Card)o;
				if(AllZoneUtil.isCardInPlay(c) && (!targeted || CardFactoryUtil.canTarget(AF.getHostCard(), c))) {
					if (noPrevention)
						c.addDamageWithoutPrevention(dmg, source);
					else
						c.addDamage(dmg, source);
				}

			}
			else if (o instanceof Player){
				Player p = (Player) o;
				if (!targeted || p.canTarget(AF.getHostCard())) {
					if (noPrevention)
						p.addDamageWithoutPrevention(dmg, source);
					else
						p.addDamage(dmg, source);
				}
			}
		}

		AbilityFactory.resolveSubAbility(saMe);
	}

	// ******************************************************************************************************
	// ***************************** DAMAGEALL **************************************************************
	// ******************************************************************************************************
	public SpellAbility getAbilityDamageAll(){

		final SpellAbility abDamageAll = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -1831356710492849854L;
			final AbilityFactory af = AF;

			@Override
			public String getStackDescription(){
				return damageAllStackDescription(af, this);
			}

			@Override
			public boolean canPlayAI() {
				return damageAllCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				damageAllResolve(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return damageAllDoTriggerAI(AF, this, mandatory);
			}

		};
		return abDamageAll;
	}

	public SpellAbility getSpellDamageAll(){
		final SpellAbility spDamageAll = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 8004957182752984818L;
			final AbilityFactory af = AF;
			final HashMap<String,String> params = af.getMapParams();

			@Override
			public String getStackDescription(){
				if(params.containsKey("SpellDescription"))
					return AF.getHostCard().getName() + " - " + params.get("SpellDescription");
				else
					return damageAllStackDescription(af, this);
			}

			public boolean canPlayAI()
			{
				return damageAllCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				damageAllResolve(af, this);
			}

		};
		return spDamageAll;
	}

	public SpellAbility getDrawbackDamageAll(){
		final SpellAbility dbDamageAll = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()){
			private static final long serialVersionUID = -6169562107675964474L;
			final AbilityFactory af = AF;

			@Override
			public String getStackDescription(){
				return damageAllStackDescription(af, this);
			}

			@Override
			public void resolve() {
				damageAllResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				//check AI life before playing this drawback?
				return true;
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return damageAllDoTriggerAI(AF, this, mandatory);
			}

		};
		return dbDamageAll;
	}

	private String damageAllStackDescription(final AbilityFactory af, SpellAbility sa){
		StringBuilder sb = new StringBuilder();
		String name = af.getHostCard().getName();
		HashMap<String,String> params = af.getMapParams();
		String desc = "";
		if(params.containsKey("ValidDescription")) 
			desc = params.get("ValidDescription");
		int dmg = getNumDamage(sa);

		sb.append(name).append(" - Deals "+dmg+" damage to "+desc);

		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null) {
			sb.append(abSub.getStackDescription());
		}

		return sb.toString();
	}

	private boolean damageAllCanPlayAI(final AbilityFactory af, final SpellAbility sa){
		// AI needs to be expanded, since this function can be pretty complex based on what the expected targets could be
		Random r = MyRandom.random;
		Cost abCost = sa.getPayCosts();
		final Card source = sa.getSourceCard();
		final HashMap<String,String> params = af.getMapParams();

		String validP = "";

		int dmg;
		if (damage.equals("X") && source.getSVar(damage).equals("Count$xPaid")){
			// Set PayX here to maximum value.
			dmg = ComputerUtil.determineLeftoverMana(sa);
			source.setSVar("PayX", Integer.toString(dmg));
		}
		else
			dmg = getNumDamage(sa);

		if(params.containsKey("ValidPlayers"))
			validP = params.get("ValidPlayers");

		CardList humanList = getKillableCreatures(af, sa, AllZone.HumanPlayer, dmg);
		CardList computerList = getKillableCreatures(af, sa, AllZone.ComputerPlayer, dmg);

		//abCost stuff that should probably be centralized...
		if (abCost != null){
			// AI currently disabled for some costs
			if (abCost.getSacCost()){ 
				//OK
			}
			if (abCost.getLifeCost()){
				if (AllZone.ComputerPlayer.getLife() - abCost.getLifeAmount() < 4)
					return false;
			}
			if (abCost.getDiscardCost()) ; //OK

			if (abCost.getSubCounter()){
				// OK
			}
		}

		if (!ComputerUtil.canPayCost(sa))
			return false;
		/////

		// TODO: if damage is dependant on mana paid, maybe have X be human's max life
		//Don't kill yourself
		if (validP.contains("Each") 
				&& AllZone.ComputerPlayer.getLife() <= AllZone.ComputerPlayer.predictDamage(dmg, source, false))
			return false;

		//if we can kill human, do it
		if((validP.contains("Each") || validP.contains("EachOpponent")) 
				&& AllZone.HumanPlayer.getLife() <= AllZone.HumanPlayer.predictDamage(dmg, source, false))
			return true;

		// prevent run-away activations - first time will always return true
		boolean chance = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed());

		int minGain = 200; //The minimum gain in destroyed creatures
		if (sa.getPayCosts().isReusuableResource()) minGain = 100; 
		// evaluate both lists and pass only if human creatures are more valuable
		if(CardFactoryUtil.evaluateCreatureList(computerList) + minGain >= CardFactoryUtil.evaluateCreatureList(humanList))
			return false;

		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			chance &= subAb.chkAI_Drawback();

		return ((r.nextFloat() < .6667) && chance);
	}

	private CardList getKillableCreatures(final AbilityFactory af, final SpellAbility sa, Player player, final int dmg){
		final HashMap<String,String> params = af.getMapParams();
		final Card source = af.getHostCard();

		String validC = "";
		if(params.containsKey("ValidCards")) 
			validC = params.get("ValidCards");

		//TODO: X may be something different than X paid		
		CardList list = AllZoneUtil.getPlayerCardsInPlay(player);
		list = list.getValidCards(validC.split(","), source.getController(), source);

		CardListFilter filterKillable = new CardListFilter(){
			public boolean addCard(Card c)
			{
				return (c.predictDamage(dmg, source, false) >= c.getKillDamage());
			}
		};

		list = list.getNotKeyword("Indestructible");
		list = list.filter(filterKillable);

		return list;
	}

	private boolean damageAllDoTriggerAI(AbilityFactory af, SpellAbility sa, boolean mandatory){
		if (!ComputerUtil.canPayCost(sa) && !mandatory)
			return false;

		final Card source = sa.getSourceCard();
		final HashMap<String,String> params = af.getMapParams();
		String validP = "";

		int dmg;
		if (damage.equals("X") && source.getSVar(damage).equals("Count$xPaid")){
			// Set PayX here to maximum value.
			dmg = ComputerUtil.determineLeftoverMana(sa);
			source.setSVar("PayX", Integer.toString(dmg));
		}
		else
			dmg = getNumDamage(sa);

		if(params.containsKey("ValidPlayers"))
			validP = params.get("ValidPlayers");

		Target tgt = sa.getTarget();
		do{	// A little trick to still check the SubAbilities, once we know we want to play it
			if(tgt == null) {
				// If it's not mandatory check a few things
				if (mandatory)
					return true;

				else{
					// Don't get yourself killed
					if (validP.contains("Each") 
							&& AllZone.ComputerPlayer.getLife() <= AllZone.ComputerPlayer.predictDamage(dmg, source, false))
						return false;

					//if we can kill human, do it
					if((validP.contains("Each") || validP.contains("EachOpponent")) 
							&& AllZone.HumanPlayer.getLife() <= AllZone.HumanPlayer.predictDamage(dmg, source, false))
						break;

					// Evaluate creatures getting killed
					CardList humanList = getKillableCreatures(af, sa, AllZone.HumanPlayer, dmg);
					CardList computerList = getKillableCreatures(af, sa, AllZone.ComputerPlayer, dmg);
					if(CardFactoryUtil.evaluateCreatureList(computerList) + 50 >= CardFactoryUtil.evaluateCreatureList(humanList))
						return false;
				}
			}
			else{
				// DamageAll doesn't really target right now
			}
		}while(false);

		
		
		if (sa.getSubAbility() != null)
			return sa.getSubAbility().doTrigger(mandatory);

		return true;
	}

	private void damageAllResolve(final AbilityFactory af, final SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		String DrawBack = params.get("SubAbility");
		Card card = sa.getSourceCard();

		int dmg = getNumDamage(sa);

		String valid = "";
		String players = "";

		if(params.containsKey("ValidCards")) 
			valid = params.get("ValidCards");
		if(params.containsKey("ValidPlayers"))
			players = params.get("ValidPlayers");

		CardList list = AllZoneUtil.getCardsInPlay();
		list = list.getValidCards(valid.split(","), card.getController(), card);

		for(Card c:list) c.addDamage(dmg, card);

		if(players.equals("Each")) {
			for(Player p:AllZoneUtil.getPlayersInGame()) {
				p.addDamage(dmg, card);
			}
		}
		else if(players.equals("EachOpponent")) {
			for(Player p:AllZoneUtil.getOpponents(card.getController())) p.addDamage(dmg, card);
		}
		else if(players.equals("Self"))
			card.getController().addDamage(dmg, card);
		else {
			//anything else to go here?
		}

		if (af.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
				abSub.resolve();
			}
			else
				CardFactoryUtil.doDrawBack(DrawBack, 0, card.getController(), card.getController().getOpponent(), card.getController(), card, null, sa);
		}
	}
}
