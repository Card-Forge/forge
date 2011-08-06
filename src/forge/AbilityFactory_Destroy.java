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
			// when getStackDesc is called, just build exactly what is happening

				 StringBuilder sb = new StringBuilder();
				 String name = af.getHostCard().getName();
				 String targetname = "";
				 
				 Card tgt = getTargetCard();
				 if (tgt != null)
					 targetname = tgt.getName();
				 else
					 targetname = name;

				 sb.append(name).append(" - Destroy ").append(targetname);
								 
				 if(noRegen)
					 sb.append(". It can't be regenerated");
				
				 return sb.toString();
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

	public static SpellAbility createSpellDestroy(final AbilityFactory AF){
		final SpellAbility spDestroy = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -317810567632846523L;
			
			final AbilityFactory af = AF;
			final HashMap<String,String> params = af.getMapParams();
			
			final boolean noRegen = params.containsKey("NoRegen");
		
			@Override
			public String getStackDescription(){
			// when getStackDesc is called, just build exactly what is happening

				 StringBuilder sb = new StringBuilder();
				 String name = af.getHostCard().getName();
				 String targetname = "";
				 
				 Card tgt = getTargetCard();
				 if (tgt != null)
					 targetname = tgt.getName();
				 else
					 targetname = name;

				 sb.append(name).append(" - Destroy ").append(targetname);
								 
				 if(noRegen)
					 sb.append(". It can't be regenerated");
				
				 return sb.toString();
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
	
	public static boolean destroyCanPlayAI(final AbilityFactory af, final SpellAbility sa, final boolean noRegen){
		// AI needs to be expanded, since this function can be pretty complex based on what the expected targets could be
		Random r = new Random();
		Ability_Cost abCost = sa.getPayCosts();
		Target abTgt = sa.getTarget();
		final Card source = sa.getSourceCard();
		CardList list;
		
		list = new CardList(AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer).getCards());
		list = list.filter(new CardListFilter() {
			public boolean addCard(Card c) {
				return CardFactoryUtil.canTarget(source, c);
			}
		});
		
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
			while(abTgt.getNumTargeted() < abTgt.getMaxTargets()){ 
				if (list.size() == 0){
					if (abTgt.getNumTargeted() < abTgt.getMinTargets() || abTgt.getNumTargeted() == 0){
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
					if (abTgt.getNumTargeted() < abTgt.getMinTargets() || abTgt.getNumTargeted() == 0){
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
		
		if (af.hasSubAbility())
			CardFactoryUtil.doDrawBack(DrawBack, 0, card.getController(), card.getController().getOpponent(), card.getController(), card, firstTarget, sa);
     }
}
