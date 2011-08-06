package forge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class AbilityFactory_Bounce {
	// An AbilityFactory subclass for bouncing and exiling permanents
	
	public static SpellAbility createAbilityBounce(final AbilityFactory AF){

		final SpellAbility abBounce = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -6373172571372374109L;
			
			final AbilityFactory af = AF;
			final HashMap<String,String> params = af.getMapParams();

			final String destination = params.get("Destination");
		
			@Override
			public String getStackDescription(){
			// when getStackDesc is called, just build exactly what is happening
				return bounceStackDescription(af, destination);
			}
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();	
			}
			
			public boolean canPlayAI()
			{
				return bounceCanPlayAI(af, this, destination);
			}
			
			@Override
			public void resolve() {
				bounceResolve(af, this, destination);
			}
			
		};
		return abBounce;
	}
	
	public static SpellAbility createSpellBounce(final AbilityFactory AF){
		final SpellAbility spBounce = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -743871647358194679L;
			
			final AbilityFactory af = AF;
			final HashMap<String,String> params = af.getMapParams();
			
			final String destination = params.get("Destination");
		
			@Override
			public String getStackDescription(){
				// when getStackDesc is called, just build exactly what is happening
				return bounceStackDescription(af, destination);
			}
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();	
			}
			
			public boolean canPlayAI()
			{
				return bounceCanPlayAI(af, this, destination);
			}
			
			@Override
			public void resolve() {
				bounceResolve(af, this, destination);
			}
			
		};
		return spBounce;
	}
	
	public static String bounceStackDescription(AbilityFactory af, String destination){
		 StringBuilder sb = new StringBuilder();
		 Card host = af.getHostCard();
		 
		 sb.append(host.getName()).append(" - ");
		 
		 StringBuilder sbTargets = new StringBuilder();
		 
		 ArrayList<Card> tgts = af.getAbTgt().getTargetCards();
		 if (tgts.size() == 0)
			 tgts.add(af.getHostCard());
		 
		 for(Card c : tgts)
			 sbTargets.append(" ").append(c.getName());
		 
		 String targetname = sbTargets.toString();
		 
		 String pronoun = tgts.size() > 1 ? " their " : " its ";
		 
		 if(destination.equals("Hand"))
			 sb.append("Return").append(targetname).append(" to").append(pronoun).append("owners hand.");
		 
		 if(destination.equals("TopofLibrary"))
			 sb.append("Put").append(targetname).append(" on top of").append(pronoun).append("owner's library.");
		 
		 if(destination.equals("BottomofLibrary"))
			 sb.append("Put").append(targetname).append(" on the bottom of").append(pronoun).append("owner's library.");
		 
		 if(destination.equals("ShuffleIntoLibrary"))
			 sb.append("Shuffle").append(targetname).append(" into").append(pronoun).append("owner's library.");
		 
		 if(destination.equals("Exile"))
			 sb.append("Exile").append(targetname);
		 
		 return sb.toString();
	}
	
	public static boolean bounceCanPlayAI(final AbilityFactory af, final SpellAbility sa, final String destination){
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

			if (list.size() == 0)
				return false;
		}
		
		if (abCost != null){
			// AI currently disabled for these costs
			if (abCost.getSacCost()){ 
				return false;
			}
			if (abCost.getLifeCost()){
				if (AllZone.ComputerPlayer.getLife() - abCost.getLifeAmount() < 4)
					return false;
			}
			if (abCost.getDiscardCost()) return false;
			
			if (abCost.getSubCounter()){
				// A card has a 25% chance per counter to be able to pass through here
				// 8+ counters will always pass. 0 counters will never
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
			 // todo: Bounce ~ if about to die
			 return false;
		 }
		 
		 return ((r.nextFloat() < .6667) && chance);
	}
	
	public static void bounceResolve(final AbilityFactory af, final SpellAbility sa, final String Destination){
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
	         	if(tgtC.isToken())
	         		AllZone.getZone(tgtC).remove(tgtC);
	         	else 
	         	{  
	         		if(Destination.equals("TopofLibrary"))
	         			AllZone.GameAction.moveToTopOfLibrary(tgtC);
	         		else if(Destination.equals("BottomofLibrary")) 
	         			AllZone.GameAction.moveToBottomOfLibrary(tgtC);
	         		else if(Destination.equals("ShuffleIntoLibrary"))
	         		{
	         			AllZone.GameAction.moveToTopOfLibrary(tgtC);
	         			tgtC.getOwner().shuffle();
	         		}
	         		else if(Destination.equals("Exile"))
	         			AllZone.GameAction.exile(tgtC); 
	         		else if(Destination.equals("Hand"))
	         		{
	             		PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, tgtC.getOwner());
	            	 		AllZone.GameAction.moveTo(hand, tgtC);
	         		}
	         	}
	         }
		}
		
		if (af.hasSubAbility())
			CardFactoryUtil.doDrawBack(DrawBack, 0, card.getController(), card.getController().getOpponent(), card.getController(), card, firstTarget, sa);
     }
}
