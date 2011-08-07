package forge.card.abilityFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.ComputerUtil;
import forge.MyRandom;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Cost;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;

public class AbilityFactory_Destroy {
	// An AbilityFactory subclass for destroying permanents
	// *********************************************************************************
	// ************************** DESTROY **********************************************
	// *********************************************************************************
	public static SpellAbility createAbilityDestroy(final AbilityFactory AF){
		final SpellAbility abDestroy = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -4153613567150919283L;

			final AbilityFactory af = AF;

			@Override
			public String getStackDescription(){
				return destroyStackDescription(af, this);
			}

			public boolean canPlayAI()
			{
				return destroyCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				destroyResolve(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return destroyDoTriggerAI(af, this, mandatory);
			}

		};
		return abDestroy;
	}

	public static SpellAbility createSpellDestroy(final AbilityFactory AF){
		final SpellAbility spDestroy = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -317810567632846523L;

			final AbilityFactory af = AF;

			@Override
			public String getStackDescription(){
				return destroyStackDescription(af, this);
			}

			public boolean canPlayAI()
			{
				return destroyCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				destroyResolve(af, this);
			}

		};
		return spDestroy;
	}
	
	public static Ability_Sub createDrawbackDestroy(final AbilityFactory AF){
		final Ability_Sub dbDestroy = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()){
			private static final long serialVersionUID = -4153613567150919283L;
	
			final AbilityFactory af = AF;
	
			@Override
			public String getStackDescription(){
				return destroyStackDescription(af, this);
			}
	
			@Override
			public boolean chkAI_Drawback() {
				return false;
			}
	
			@Override
			public void resolve() {
				destroyResolve(af, this);
			}
	
			@Override
			public boolean doTrigger(boolean mandatory) {
				return destroyDoTriggerAI(af, this, mandatory);
			}	
		};
		return dbDestroy;
	}

	public static boolean destroyCanPlayAI(final AbilityFactory af, final SpellAbility sa){
		// AI needs to be expanded, since this function can be pretty complex based on what the expected targets could be
		Random r = MyRandom.random;
		Cost abCost = sa.getPayCosts();
		Target abTgt = sa.getTarget();
		final Card source = sa.getSourceCard();
		final boolean noRegen = af.getMapParams().containsKey("NoRegen");

		CardList list;
		list = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer);
		list = list.getTargetableCards(source);

		if (abTgt != null){
			list = list.getValidCards(abTgt.getValidTgts(), source.getController(), source);
			list = list.getNotKeyword("Indestructible");

			// If NoRegen is not set, filter out creatures that have a regeneration shield
			if (!noRegen){
				// TODO: filter out things that could regenerate in response? might be tougher?
				list = list.filter(new CardListFilter() {
					public boolean addCard(Card c) {
						return (c.getShield() == 0 && !ComputerUtil.canRegenerate(c));
					}
				});
			}
			
			if (list.size() == 0)
				return false;
		}

		if (abCost != null){
			// AI currently disabled for some costs
			if (abCost.getSacCost() && !abCost.getSacThis()){
				//only sacrifice something that's supposed to be sacrificed 
				String sacType = abCost.getSacType();
			    CardList typeList = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
			    typeList = typeList.getValidCards(sacType.split(","), source.getController(), source);
			    if(ComputerUtil.getCardPreference(source, "SacCost", typeList) == null)
			    	return false;
			}
			if (abCost.getLifeCost()){
				if (AllZone.ComputerPlayer.getLife() - abCost.getLifeAmount() < 4)
					return false;
			}
			if (abCost.getDiscardCost()) return false;

			if (abCost.getSubCounter()){
				// OK
			}
		}

		if (!ComputerUtil.canPayCost(sa))
			return false;

		// prevent run-away activations - first time will always return true
		boolean chance = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed());

		// Targeting
		if (abTgt != null){
			abTgt.resetTargets();
			// target loop
			while(abTgt.getNumTargeted() < abTgt.getMaxTargets(sa.getSourceCard(), sa)){ 
				if (list.size() == 0){
					if (abTgt.getNumTargeted() < abTgt.getMinTargets(sa.getSourceCard(), sa) || abTgt.getNumTargeted() == 0){
						abTgt.resetTargets();
						return false;
					}
					else{
						// TODO is this good enough? for up to amounts?
						break;
					}
				}

				Card choice = null;
				if (list.getNotType("Creature").size() == 0)
					choice = CardFactoryUtil.AI_getBestCreature(list); //if the targets are only creatures, take the best
				else 
					choice = CardFactoryUtil.AI_getMostExpensivePermanent(list, af.getHostCard(), true);

				if (choice == null){	// can't find anything left
					if (abTgt.getNumTargeted() < abTgt.getMinTargets(sa.getSourceCard(), sa) || abTgt.getNumTargeted() == 0){
						abTgt.resetTargets();
						return false;
					}
					else{
						// TODO is this good enough? for up to amounts?
						break;
					}
				}
				list.remove(choice);
				abTgt.addTarget(choice);
			}

		}
		else{
			return false;
		}

		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			chance &= subAb.chkAI_Drawback();

		return ((r.nextFloat() < .6667) && chance);
	}

	public static boolean destroyDoTriggerAI(final AbilityFactory af, SpellAbility sa, boolean mandatory){
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		Target tgt = sa.getTarget();
		final Card source = sa.getSourceCard();
		final boolean noRegen = af.getMapParams().containsKey("NoRegen");


		if (tgt != null){
            CardList list;
		    list = AllZoneUtil.getCardsInPlay();
		    list = list.getTargetableCards(source);
		    list = list.getValidCards(tgt.getValidTgts(), source.getController(), source);

			if (list.size() == 0 || list.size() < tgt.getMinTargets(sa.getSourceCard(), sa))
				return false;
			
			tgt.resetTargets();

			CardList preferred = list.getNotKeyword("Indestructible");
			preferred = list.getController(AllZone.HumanPlayer);
			
			// If NoRegen is not set, filter out creatures that have a regeneration shield
			if (!noRegen){
				// TODO: filter out things that could regenerate in response? might be tougher?
				preferred = preferred.filter(new CardListFilter() {
					public boolean addCard(Card c) {
						return c.getShield() == 0;
					}
				});
			}
			
			for(Card c : preferred)
				list.remove(c);
			
			while(tgt.getNumTargeted() < tgt.getMaxTargets(sa.getSourceCard(), sa)){ 
				if (preferred.size() == 0){
					if (tgt.getNumTargeted() == 0 || tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)){
						if (!mandatory){
							tgt.resetTargets();
							return false;
						}
						else
							break;
					}
					else{
						break;
					}
				}
				else{
					Card c;
					if (preferred.getNotType("Creature").size() == 0){
						c = CardFactoryUtil.AI_getBestCreature(preferred);
					}
					else if (preferred.getNotType("Land").size() == 0){
						c = CardFactoryUtil.AI_getBestLand(preferred);
					}
					else{
						c = CardFactoryUtil.AI_getMostExpensivePermanent(preferred, source, false);
					}
					tgt.addTarget(c);
					preferred.remove(c);
				}
			}
				
			while(tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)){ 
				if (list.size() == 0){
					break;
				}
				else{
					Card c;
					if (list.getNotType("Creature").size() == 0){
						c = CardFactoryUtil.AI_getWorstCreature(list);
					}
					else{
						c = CardFactoryUtil.AI_getCheapestPermanent(list, source, false);
					}
					tgt.addTarget(c);
					list.remove(c);
				}
			}
			
			if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa))
				return false;
		}
		else{
			if (!mandatory)
				return false;
		}
		
		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			return subAb.doTrigger(mandatory);
		
		return true;
	}
	
	private static String destroyStackDescription(final AbilityFactory af, SpellAbility sa) {
		final boolean noRegen = af.getMapParams().containsKey("NoRegen");
		StringBuilder sb = new StringBuilder();
		Card host = af.getHostCard();
		
		String conditionDesc = af.getMapParams().get("ConditionDescription");
		if (conditionDesc != null)
			sb.append(conditionDesc).append(" ");

		ArrayList<Card> tgtCards;

		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtCards = tgt.getTargetCards();
		else{
			tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
		}
		
		if (sa instanceof Ability_Sub)
			sb.append(" ");
		else
			sb.append(host).append(" - ");

		sb.append("Destroy ");

		Iterator<Card> it = tgtCards.iterator();
		while(it.hasNext()) {
			Card tgtC = it.next();
			if(tgtC.isFaceDown()) sb.append("Morph ").append("(").append(tgtC.getUniqueNumber()).append(")");
			else sb.append(tgtC);
			
			if(it.hasNext()) sb.append(", ");
		}

		if (noRegen) {
			sb.append(". ");
			if (tgtCards.size() == 1)
				sb.append("It");
			else
				sb.append("They");
			sb.append(" can't be regenerated");
		}
		sb.append(".");
		
		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null) {
			sb.append(abSub.getStackDescription());
		}

		return sb.toString();
	}

	public static void destroyResolve(final AbilityFactory af, final SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		String DrawBack = params.get("SubAbility");
		final boolean noRegen = params.containsKey("NoRegen");
		Card card = sa.getSourceCard();
		
		if (!AbilityFactory.checkConditional(params, sa)){
			AbilityFactory.resolveSubAbility(sa);
			return;
		}

		ArrayList<Card> tgtCards;

		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtCards = tgt.getTargetCards();
		else{
			tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
		}

		for(Card tgtC : tgtCards){
			if(AllZoneUtil.isCardInPlay(tgtC) && (tgt == null || CardFactoryUtil.canTarget(card, tgtC))) {
				if(noRegen) 
					AllZone.GameAction.destroyNoRegeneration(tgtC);
				else
					AllZone.GameAction.destroy(tgtC);
			}
		}

		if (af.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
				abSub.resolve();
			}
			else
				CardFactoryUtil.doDrawBack(DrawBack, 0, card.getController(), card.getController().getOpponent(), card.getController(), card, tgtCards.get(0), sa);
		}
	}
	
	// *********************************************************************************
	// ************************ DESTROY ALL ********************************************
	// *********************************************************************************
	public static SpellAbility createAbilityDestroyAll(final AbilityFactory AF){

		final SpellAbility abDestroyAll = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -1376444173137861437L;
			
			final AbilityFactory af = AF;
			final HashMap<String,String> params = af.getMapParams();

			final boolean noRegen = params.containsKey("NoRegen");
		
			@Override
			public String getStackDescription(){
				return destroyAllStackDescription(af, this, noRegen);
			}
			
			public boolean canPlayAI()
			{
				return destroyAllCanPlayAI(af, this, noRegen);
			}
			
			@Override
			public void resolve() {
				destroyAllResolve(af, this, noRegen);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return destroyAllCanPlayAI(af, this, noRegen);
			}
			
		};
		return abDestroyAll;
	}
	
	public static SpellAbility createSpellDestroyAll(final AbilityFactory AF){
		final SpellAbility spDestroyAll = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -3712659336576469102L;
			
			final AbilityFactory af = AF;
			final HashMap<String,String> params = af.getMapParams();
			
			final boolean noRegen = params.containsKey("NoRegen");
			
			@Override
			public String getStackDescription(){
				if(params.containsKey("SpellDescription"))
					return AF.getHostCard().getName() + " - " + params.get("SpellDescription");
				else
					return destroyAllStackDescription(af, this, noRegen);
			}

			public boolean canPlayAI()
			{
				return destroyAllCanPlayAI(af, this, noRegen);
			}
			
			@Override
			public void resolve() {
				destroyAllResolve(af, this, noRegen);
			}
			
		};
		return spDestroyAll;
	}
	
	public static SpellAbility createDrawbackDestroyAll(final AbilityFactory AF){
		final SpellAbility dbDestroyAll = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()){
			private static final long serialVersionUID = -242160421677518351L;
			final AbilityFactory af = AF;
			final HashMap<String,String> params = af.getMapParams();
			
			final boolean noRegen = params.containsKey("NoRegen");
			
			@Override
			public String getStackDescription(){
				if(params.containsKey("SpellDescription"))
					return AF.getHostCard().getName() + " - " + params.get("SpellDescription");
				else
					return destroyAllStackDescription(af, this, noRegen);
			}
			
			@Override
			public void resolve() {
				destroyAllResolve(af, this, noRegen);
			}
			
			@Override
			public boolean chkAI_Drawback() {
				return true;
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				// TODO Auto-generated method stub
				return false;
			}
			
		};
		return dbDestroyAll;
	}
	
	public static String destroyAllStackDescription(final AbilityFactory af, SpellAbility sa, boolean noRegen){
		// when getStackDesc is called, just build exactly what is happening

		 StringBuilder sb = new StringBuilder();
		 String name = af.getHostCard().getName();
		 
		String conditionDesc = af.getMapParams().get("ConditionDescription");
		if (conditionDesc != null)
			sb.append(conditionDesc).append(" ");

		 ArrayList<Card> tgtCards;

		 Target tgt = af.getAbTgt();
		 if (tgt != null)
		 	tgtCards = tgt.getTargetCards();
		 else{
		 	tgtCards = new ArrayList<Card>();
		 	tgtCards.add(sa.getSourceCard());
		 }
		 
		 sb.append(name).append(" - Destroy permanents");
						 
		 if(noRegen) sb.append(". They can't be regenerated");
		
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null) {
				sb.append(abSub.getStackDescription());
			}
		 
		 return sb.toString();
	}
	
	public static boolean destroyAllCanPlayAI(final AbilityFactory af, final SpellAbility sa, final boolean noRegen){
		// AI needs to be expanded, since this function can be pretty complex based on what the expected targets could be
		Random r = MyRandom.random;
		Cost abCost = sa.getPayCosts();
		final Card source = sa.getSourceCard();
		final HashMap<String,String> params = af.getMapParams();
		String Valid = "";
		
		if(params.containsKey("ValidCards")) 
			Valid = params.get("ValidCards");
		
		if (Valid.contains("X") && source.getSVar("X").equals("Count$xPaid")){
			// Set PayX here to maximum value.
			int xPay = ComputerUtil.determineLeftoverMana(sa);
			source.setSVar("PayX", Integer.toString(xPay));
			Valid = Valid.replace("X", Integer.toString(xPay));
		}
		
		CardList humanlist = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer);
		CardList computerlist = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
		
		humanlist = humanlist.getValidCards(Valid.split(","), source.getController(), source);
		computerlist = computerlist.getValidCards(Valid.split(","), source.getController(), source);
		
		humanlist = humanlist.getNotKeyword("Indestructible");
		computerlist = computerlist.getNotKeyword("Indestructible");
		
		if (abCost != null){
			// AI currently disabled for some costs
			if (abCost.getSacCost()){ 
				//OK
			}
			if (abCost.getLifeCost()){
				if (AllZone.ComputerPlayer.getLife() - abCost.getLifeAmount() < 4)
					return false;
			}
			if (abCost.getDiscardCost()) ;//OK
			
			if (abCost.getSubCounter()){
					// OK
			}
		}
		
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		 // prevent run-away activations - first time will always return true
		 boolean chance = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed());
		 
		 // if only creatures are affected evaluate both lists and pass only if human creatures are more valuable
		 if (humanlist.getNotType("Creature").size() == 0 && computerlist.getNotType("Creature").size() == 0) {
			 if(CardFactoryUtil.evaluateCreatureList(computerlist) + 200 >= CardFactoryUtil.evaluateCreatureList(humanlist))
				 return false;
		 }//only lands involved
		 else if (humanlist.getNotType("Land").size() == 0 && computerlist.getNotType("Land").size() == 0) {
			 if(CardFactoryUtil.evaluatePermanentList(computerlist) + 1 >= CardFactoryUtil.evaluatePermanentList(humanlist))
				 return false;
		 } // otherwise evaluate both lists by CMC and pass only if human permanents are more valuable
		 else if(CardFactoryUtil.evaluatePermanentList(computerlist) + 3 >= CardFactoryUtil.evaluatePermanentList(humanlist))
			 return false;
		 
		 Ability_Sub subAb = sa.getSubAbility();
		 if (subAb != null)
		 	chance &= subAb.chkAI_Drawback();
		 
		 return ((r.nextFloat() < .9667) && chance);
	}
	
	public static void destroyAllResolve(final AbilityFactory af, final SpellAbility sa, final boolean noRegen){
		HashMap<String,String> params = af.getMapParams();
		String DrawBack = params.get("SubAbility");
		Card card = sa.getSourceCard();
		
		if (!AbilityFactory.checkConditional(params, sa)){
			AbilityFactory.resolveSubAbility(sa);
			return;
		}
		
		String Valid = "";
		
		if(params.containsKey("ValidCards")) 
			Valid = params.get("ValidCards");
		
		// Ugh. If calculateAmount needs to be called with DestroyAll it _needs_ to use the X variable
		// We really need a better solution to this
		if (Valid.contains("X"))	
			Valid = Valid.replace("X", Integer.toString(AbilityFactory.calculateAmount(card, "X", sa)));
		
		CardList list = AllZoneUtil.getCardsInPlay();
		
		list = list.getValidCards(Valid.split(","), card.getController(), card);

		boolean remDestroyed = params.containsKey("RememberDestroyed");
		if (remDestroyed)
			card.clearRemembered();
		
	 	if(noRegen){
	 		for(int i = 0; i < list.size(); i++) 
	 			if (AllZone.GameAction.destroyNoRegeneration(list.get(i)) && remDestroyed)
	 				card.addRemembered(list.get(i));
	 	}
	 	else{
	 		for(int i = 0; i < list.size(); i++) 
	 			if (AllZone.GameAction.destroy(list.get(i)) && remDestroyed)
	 				card.addRemembered(list.get(i));
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
