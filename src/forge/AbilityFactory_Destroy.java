package forge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class AbilityFactory_Destroy {
	// An AbilityFactory subclass for destroying permanents
	
	public static SpellAbility createAbilityDestroy(final AbilityFactory AF){

		final SpellAbility abDestroy = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -4153613567150919283L;
			
			final AbilityFactory af = AF;
			final HashMap<String,String> params = af.getMapParams();

			final boolean noRegen = params.containsKey("NoRegen");
		
			@Override
			public String getStackDescription(){
				return destroyStackDescription(af, this, noRegen);
			}
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();	
			}
			
			public boolean canPlayAI()
			{
				return destroyCanPlayAI(af, this, noRegen);
			}
			
			@Override
			public void resolve() {
				destroyResolve(af, this, noRegen);
			}
			
		};
		return abDestroy;
	}
	
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
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();	
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
		return abDestroyAll;
	}

	public static SpellAbility createSpellDestroy(final AbilityFactory AF){
		final SpellAbility spDestroy = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -317810567632846523L;
			
			final AbilityFactory af = AF;
			final HashMap<String,String> params = af.getMapParams();
			
			final boolean noRegen = params.containsKey("NoRegen");
		
			@Override
			public String getStackDescription(){
				return destroyStackDescription(af, this, noRegen);
			}
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();	
			}
			
			public boolean canPlayAI()
			{
				return destroyCanPlayAI(af, this, noRegen);
			}
			
			@Override
			public void resolve() {
				destroyResolve(af, this, noRegen);
			}
			
		};
		return spDestroy;
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
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();	
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
	
	public static boolean destroyCanPlayAI(final AbilityFactory af, final SpellAbility sa, final boolean noRegen){
		// AI needs to be expanded, since this function can be pretty complex based on what the expected targets could be
		Random r = new Random();
		Ability_Cost abCost = sa.getPayCosts();
		Target abTgt = sa.getTarget();
		final Card source = sa.getSourceCard();
		CardList list;
		
		list = new CardList(AllZone.getZone(Constant.Zone.Battlefield, AllZone.HumanPlayer).getCards());
		list = list.getTargetableCards(source);
		
		if (abTgt != null){
			list = list.getValidCards(abTgt.getValidTgts(), source.getController(), source);
			list = list.getNotKeyword("Indestructible");
			
			//TODO: Check for Regeneration 

			if (list.size() == 0)
				return false;
		}
		
		if (abCost != null){
			// AI currently disabled for some costs
			if (abCost.getSacCost()){ 
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
						// todo is this good enough? for up to amounts?
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
						// todo is this good enough? for up to amounts?
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
	
	public static boolean destroyAllCanPlayAI(final AbilityFactory af, final SpellAbility sa, final boolean noRegen){
		// AI needs to be expanded, since this function can be pretty complex based on what the expected targets could be
		Random r = new Random();
		Ability_Cost abCost = sa.getPayCosts();
		final Card source = sa.getSourceCard();
		final HashMap<String,String> params = af.getMapParams();
		String Valid = "";
		
		if(params.containsKey("ValidCards")) 
			Valid = params.get("ValidCards");
		
		CardList humanlist = new CardList(AllZone.getZone(Constant.Zone.Battlefield, AllZone.HumanPlayer).getCards());
		CardList computerlist = new CardList(AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer).getCards());
		
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
			if (abCost.getDiscardCost()) //OK
			
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
		 } // otherwise evaluate both lists by CMC and pass only if human permanents are more valuable
		 else if(CardFactoryUtil.evaluatePermanentList(computerlist) + 3 >= CardFactoryUtil.evaluateCreatureList(humanlist))
			 return false;
		 
		 Ability_Sub subAb = sa.getSubAbility();
		 if (subAb != null)
		 	chance &= subAb.chkAI_Drawback();
		 
		 return ((r.nextFloat() < .6667) && chance);
	}
	
	
	public static void destroyResolve(final AbilityFactory af, final SpellAbility sa, final boolean noRegen){
		HashMap<String,String> params = af.getMapParams();
		String DrawBack = params.get("SubAbility");
		Card card = sa.getSourceCard();
		
		ArrayList<Card> tgtCards;

		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtCards = tgt.getTargetCards();
		else{
			tgtCards = new ArrayList<Card>();
			tgtCards.add(card);
		}
		
		Card firstTarget = tgtCards.get(0);
		
		for(Card tgtC : tgtCards){
	 	   if(AllZone.GameAction.isCardInPlay(tgtC) && (tgt == null || CardFactoryUtil.canTarget(card, tgtC))) {
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
				CardFactoryUtil.doDrawBack(DrawBack, 0, card.getController(), card.getController().getOpponent(), card.getController(), card, firstTarget, sa);
		}
     }
	
	public static void destroyAllResolve(final AbilityFactory af, final SpellAbility sa, final boolean noRegen){
		HashMap<String,String> params = af.getMapParams();
		String DrawBack = params.get("SubAbility");
		Card card = sa.getSourceCard();
		
		String Valid = "";
		
		if(params.containsKey("ValidCards")) 
			Valid = params.get("ValidCards");
		
		CardList list = new CardList(AllZone.getZone(Constant.Zone.Battlefield, AllZone.HumanPlayer).getCards());
		list.addAll(AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer).getCards());
		
		list = list.getValidCards(Valid.split(","), card.getController(), card);

	 	if(noRegen) 
	 		for(int i = 0; i < list.size(); i++) AllZone.GameAction.destroyNoRegeneration(list.get(i));
	 	else
	 		for(int i = 0; i < list.size(); i++) AllZone.GameAction.destroy(list.get(i));
		
	 	if (af.hasSubAbility()){
	 		Ability_Sub abSub = sa.getSubAbility();
	 		if (abSub != null){
	 		   abSub.resolve();
	 		}
	 		else
	 			CardFactoryUtil.doDrawBack(DrawBack, 0, card.getController(), card.getController().getOpponent(), card.getController(), card, null, sa);
	 	}
     }
	
	
	public static String destroyStackDescription(final AbilityFactory af, SpellAbility sa, boolean noRegen){
		// when getStackDesc is called, just build exactly what is happening

		 StringBuilder sb = new StringBuilder();
		 String name = af.getHostCard().getName();

		 ArrayList<Card> tgtCards;

		 Target tgt = af.getAbTgt();
		 if (tgt != null)
		 	tgtCards = tgt.getTargetCards();
		 else{
		 	tgtCards = new ArrayList<Card>();
		 	tgtCards.add(sa.getSourceCard());
		 }
		 
		 sb.append(name).append(" - Destroy ");
		 
		 for(Card c : tgtCards)
			 sb.append(c.getName()).append(" ");
						 
		 if(noRegen){
			 sb.append(". ");
			 if (tgtCards.size() == 1)
				 sb.append("It");
			 else
				 sb.append("They");
			 sb.append(" can't be regenerated");
		 }
		
		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null) {
			sb.append(abSub.getStackDescription());
		}
		 
		 return sb.toString();
	}
	
	public static String destroyAllStackDescription(final AbilityFactory af, SpellAbility sa, boolean noRegen){
		// when getStackDesc is called, just build exactly what is happening

		 StringBuilder sb = new StringBuilder();
		 String name = af.getHostCard().getName();

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
}
