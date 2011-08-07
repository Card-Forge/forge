package forge.card.abilityFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardListUtil;
import forge.CombatUtil;
import forge.Command;
import forge.ComputerUtil;
import forge.Constant;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Cost;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;

public class AbilityFactory_Regenerate {
	
	// Ex: A:SP$Regenerate | Cost$W | Tgt$TgtC | SpellDescription$Regenerate target creature.
	// http://www.slightlymagic.net/wiki/Forge_AbilityFactory#Regenerate
	
	//**************************************************************
	// ********************* Regenerate ****************************
	//**************************************************************

	public static SpellAbility getAbilityRegenerate(final AbilityFactory af) {

		final SpellAbility abRegenerate = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
			private static final long serialVersionUID = -6386981911243700037L;

			@Override
			public boolean canPlayAI() {
				return regenerateCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				regenerateResolve(af, this);
				af.getHostCard().setAbilityUsed(af.getHostCard().getAbilityUsed() + 1);
			}
			
			@Override
			public String getStackDescription(){
				return regenerateStackDescription(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return doTriggerAI(af, this, mandatory);
			}
			
		};//Ability_Activated

		return abRegenerate;
	}

	public static SpellAbility getSpellRegenerate(final AbilityFactory af){

		final SpellAbility spRegenerate = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
			private static final long serialVersionUID = -3899905398102316582L;

			@Override
			public boolean canPlayAI() {
				return regenerateCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				regenerateResolve(af, this);
			}
			
			@Override
			public String getStackDescription(){
				return regenerateStackDescription(af, this);
			}
			
		}; // Spell

		return spRegenerate;
	}
	
	public static SpellAbility createDrawbackRegenerate(final AbilityFactory af) {
		final SpellAbility dbRegen = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
			private static final long serialVersionUID = -2295483806708528744L;

			@Override
			public String getStackDescription(){
				return regenerateStackDescription(af, this);
			}

			@Override
			public void resolve() {
				regenerateResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return true;
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return doTriggerAI(af, this, mandatory);
			}

		};
		return dbRegen;
	}
	
	private static String regenerateStackDescription(AbilityFactory af, SpellAbility sa){
		final HashMap<String,String> params = af.getMapParams();
		StringBuilder sb = new StringBuilder();
		Card host = af.getHostCard();

		ArrayList<Card> tgtCards;
		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtCards = tgt.getTargetCards();
		else
			tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);

		if(tgtCards.size() > 0) {
			if (sa instanceof Ability_Sub)
				sb.append(" ");
			else
				sb.append(host).append(" - ");

			sb.append("Regenerate ");
			Iterator<Card> it = tgtCards.iterator();
			while(it.hasNext()) {
				Card tgtC = it.next();
				if(tgtC.isFaceDown()) sb.append("Morph");
				else sb.append(tgtC);
				
				if(it.hasNext()) sb.append(" ");
			}
		}
		sb.append(".");

		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null) {
			sb.append(abSub.getStackDescription());
		}

		return sb.toString();
	}

	private static boolean regenerateCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
		final HashMap<String,String> params = af.getMapParams();
		final Card hostCard = af.getHostCard();
		boolean chance = false;
		Cost abCost = af.getAbCost();
		if (abCost != null){
			// AI currently disabled for these costs
			if (abCost.getSacCost() && !abCost.getSacThis()){
				//only sacrifice something that's supposed to be sacrificed 
				String type = abCost.getSacType();
			    CardList typeList = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
			    typeList = typeList.getValidCards(type.split(","), hostCard.getController(), hostCard);
			    if(ComputerUtil.getCardPreference(hostCard, "SacCost", typeList) == null)
			    	return false;
			}
			if (abCost.getLifeCost()){
				if (AllZone.ComputerPlayer.getLife() - abCost.getLifeAmount() < 4)
					return false;
			}
		}

		if (!ComputerUtil.canPayCost(sa))
			return false;

		Target tgt = sa.getTarget();
		if (tgt == null){
			// As far as I can tell these Defined Cards will only have one of them
			ArrayList<Card> list = AbilityFactory.getDefinedCards(hostCard, params.get("Defined"), sa);
			
			if (AllZone.Stack.size() > 0){
			// check stack for something that will kill this
			}
			else{
				if (AllZone.Phase.is(Constant.Phase.Combat_Declare_Blockers_InstantAbility)){
					boolean flag = false;
					
					for(Card c : list){
						if (c.getShield() == 0)
							flag |= CombatUtil.combatantWouldBeDestroyed(c);
					}
					
					chance = flag;
				}
				else{	// if nothing on the stack, and it's not declare blockers. no need to regen
					return false;
				}
			}
		}
		else{
			tgt.resetTargets();
			// filter AIs battlefield by what I can target
			CardList targetables = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
			targetables = targetables.getValidCards(tgt.getValidTgts(), AllZone.ComputerPlayer, hostCard);
			
			if (targetables.size() == 0)
				return false;
			
			if (AllZone.Stack.size() > 0){
				// check stack for something on the stack will kill anything i control
			}
			else{
				if (AllZone.Phase.is(Constant.Phase.Combat_Declare_Blockers_InstantAbility)){
					CardList combatants = targetables.getType("Creature");
					CardListUtil.sortByEvaluateCreature(combatants);

					for(Card c : combatants){
						if (c.getShield() == 0 && CombatUtil.combatantWouldBeDestroyed(c)){
							tgt.addTarget(c);
							chance = true;
							break;
						}
					}
				}
			}
		}
		
		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			chance &= subAb.chkAI_Drawback();
		
		return chance;
	}//regenerateCanPlayAI

	private static boolean doTriggerAI(final AbilityFactory af, final SpellAbility sa, boolean mandatory) {
		boolean chance = false;

		if (!ComputerUtil.canPayCost(sa))
			return false;

		Target tgt = sa.getTarget();
		if (tgt == null){
			// If there's no target on the trigger, just say yes.
			chance = true;
		}
		else{
			chance = regenMandatoryTarget(af, sa, mandatory);
		}
		
		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			chance &= subAb.doTrigger(mandatory);
		
		return chance;
	}
	
	private static boolean regenMandatoryTarget(AbilityFactory af, SpellAbility sa, boolean mandatory){
		final Card hostCard = af.getHostCard();
		Target tgt = sa.getTarget();
		tgt.resetTargets();
		// filter AIs battlefield by what I can target
		CardList targetables = AllZoneUtil.getCardsInPlay();
		targetables = targetables.getValidCards(tgt.getValidTgts(), AllZone.ComputerPlayer, hostCard);
		CardList compTargetables = targetables.getController(AllZone.ComputerPlayer);
		
		if (targetables.size() == 0)
			return false;
		
		if (!mandatory && compTargetables.size() == 0)
			return false;
		
		if (compTargetables.size() > 0){
			CardList combatants = compTargetables.getType("Creature");
			CardListUtil.sortByEvaluateCreature(combatants);
			if (AllZone.Phase.is(Constant.Phase.Combat_Declare_Blockers_InstantAbility)){
				for(Card c : combatants){
					if (c.getShield() == 0 && CombatUtil.combatantWouldBeDestroyed(c)){
						tgt.addTarget(c);
						return true;
					}
				}
			}
	
			// TODO see if something on the stack is about to kill something i can target
			
			// choose my best X without regen
			if (compTargetables.getNotType("Creature").size() == 0){
				for(Card c : combatants){
					if (c.getShield() == 0){
						tgt.addTarget(c);
						return true;
					}
				}
				tgt.addTarget(combatants.get(0));
				return true;
			}
			else{
				CardListUtil.sortByMostExpensive(compTargetables);
				for(Card c : compTargetables){
					if (c.getShield() == 0){
						tgt.addTarget(c);
						return true;
					}
				}
				tgt.addTarget(compTargetables.get(0));
				return true;
			}
		}

		tgt.addTarget(CardFactoryUtil.AI_getCheapestPermanent(targetables, hostCard, true));
		return true;
	}
	
	private static void regenerateResolve(final AbilityFactory af, final SpellAbility sa) {
		Card hostCard = af.getHostCard();
		final HashMap<String,String> params = af.getMapParams();
		
		ArrayList<Card> tgtCards;
		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtCards = tgt.getTargetCards();
		else
			tgtCards = AbilityFactory.getDefinedCards(hostCard, params.get("Defined"), sa);
		
		for(final Card tgtC : tgtCards){
			final Command untilEOT = new Command() {
				private static final long serialVersionUID = 1922050611313909200L;

				public void execute() {
					tgtC.resetShield();
				}
			};
			
			if (AllZoneUtil.isCardInPlay(tgtC) && (tgt == null || CardFactoryUtil.canTarget(hostCard, tgtC))){
				tgtC.addShield();
				AllZone.EndOfTurn.addUntil(untilEOT);
			}
		}
	}//regenerateResolve

	//**************************************************************
	// ********************* RegenerateAll *************************
	//**************************************************************

	public static SpellAbility getAbilityRegenerateAll(final AbilityFactory af) {

		final SpellAbility abRegenerateAll = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
			private static final long serialVersionUID = -3001272997209059394L;

			@Override
			public boolean canPlayAI() {
				return regenerateAllCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				regenerateAllResolve(af, this);
				af.getHostCard().setAbilityUsed(af.getHostCard().getAbilityUsed() + 1);
			}
			
			@Override
			public String getStackDescription(){
				return regenerateAllStackDescription(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return regenerateAllDoTriggerAI(af, this, mandatory);
			}
			
		};//Ability_Activated

		return abRegenerateAll;
	}

	public static SpellAbility getSpellRegenerateAll(final AbilityFactory af){

		final SpellAbility spRegenerateAll = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
			private static final long serialVersionUID = -4185454527676705881L;

			@Override
			public boolean canPlayAI() {
				return regenerateAllCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				regenerateAllResolve(af, this);
			}
			
			@Override
			public String getStackDescription(){
				return regenerateAllStackDescription(af, this);
			}
			
		}; // Spell

		return spRegenerateAll;
	}
	
	public static SpellAbility createDrawbackRegenerateAll(final AbilityFactory af) {
		final SpellAbility dbRegenAll = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
			private static final long serialVersionUID = 4777861790603705572L;

			@Override
			public String getStackDescription(){
				return regenerateAllStackDescription(af, this);
			}

			@Override
			public void resolve() {
				regenerateAllResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return true;
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return regenerateAllDoTriggerAI(af, this, mandatory);
			}

		};
		return dbRegenAll;
	}
	
	private static String regenerateAllStackDescription(AbilityFactory af, SpellAbility sa){
		final HashMap<String,String> params = af.getMapParams();
		StringBuilder sb = new StringBuilder();
		Card host = af.getHostCard();

		if (sa instanceof Ability_Sub)
			sb.append(" ");
		else
			sb.append(host).append(" - ");

		String desc = "";
		if(params.containsKey("SpellDescription")) {
			desc = params.get("SpellDescription");
		}
		else {
			desc = "Regenerate all valid cards.";
		}

		sb.append(desc);

		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null) {
			sb.append(abSub.getStackDescription());
		}

		return sb.toString();
	}

	private static boolean regenerateAllCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
		final HashMap<String,String> params = af.getMapParams();
		final Card hostCard = af.getHostCard();
		boolean chance = false;
		Cost abCost = af.getAbCost();
		if (abCost != null){
			// AI currently disabled for these costs
			if (abCost.getSacCost() && !abCost.getSacThis()){
				//only sacrifice something that's supposed to be sacrificed 
				String type = abCost.getSacType();
			    CardList typeList = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
			    typeList = typeList.getValidCards(type.split(","), hostCard.getController(), hostCard);
			    if(ComputerUtil.getCardPreference(hostCard, "SacCost", typeList) == null)
			    	return false;
			}
			if (abCost.getLifeCost()){
				if (AllZone.ComputerPlayer.getLife() - abCost.getLifeAmount() < 4)
					return false;
			}
		}

		if (!ComputerUtil.canPayCost(sa))
			return false;


		// filter AIs battlefield by what I can target
		String valid = "";

		if(params.containsKey("ValidCards")) 
			valid = params.get("ValidCards");
    	
    	CardList list = AllZoneUtil.getCardsInPlay();
		list = list.getValidCards(valid.split(","), hostCard.getController(), hostCard);

		if (list.size() == 0)
			return false;

		int numSaved = 0;
		if (AllZone.Stack.size() > 0){
			//TODO - check stack for something on the stack will kill anything i control
		}
		else{

			if (AllZone.Phase.is(Constant.Phase.Combat_Declare_Blockers_InstantAbility)){
				CardList combatants = list.getType("Creature");

				for(Card c : combatants){
					if (c.getShield() == 0 && CombatUtil.combatantWouldBeDestroyed(c)){
						numSaved++;
					}
				}
			}
		}
		
		if(numSaved > 1) {
			chance = true;
		}

		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			chance &= subAb.chkAI_Drawback();

		return chance;
	}

	private static boolean regenerateAllDoTriggerAI(final AbilityFactory af, final SpellAbility sa, boolean mandatory) {
		boolean chance = true;

		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			chance &= subAb.doTrigger(mandatory);
		
		return chance;
	}
	
	private static void regenerateAllResolve(final AbilityFactory af, final SpellAbility sa) {
		Card hostCard = af.getHostCard();
		final HashMap<String,String> params = af.getMapParams();
		String valid = "";

		if(params.containsKey("ValidCards")) 
			valid = params.get("ValidCards");
    	
    	CardList list = AllZoneUtil.getCardsInPlay();
		list = list.getValidCards(valid.split(","), hostCard.getController(), hostCard);
		
		for(final Card c : list){
			final Command untilEOT = new Command() {
				private static final long serialVersionUID = 259368227093961103L;

				public void execute() {
					c.resetShield();
				}
			};
			
			if (AllZoneUtil.isCardInPlay(c)){
				c.addShield();
				AllZone.EndOfTurn.addUntil(untilEOT);
			}
		}
	}//regenerateAllResolve
	
}//end class AbilityFactory_Regenerate
