package forge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class AbilityFactory_PermanentState {
	// Untapping
	public static SpellAbility createAbilityUntap(final AbilityFactory AF){
		final SpellAbility abUntap = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 5445572699000471299L;
			
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
			// when getStackDesc is called, just build exactly what is happening
				 StringBuilder sb = new StringBuilder("Untap ");
				 String name = af.getHostCard().getName();
				 Card tgt = getTargetCard();
				 if (tgt != null)
					 sb.append(tgt.getName());
				 else
					 sb.append(name);
				 return sb.toString();
			}
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();		
			}
			
			public boolean canPlayAI()
			{
				return untapCanPlayAI(af,this);
			}
			
			@Override
			public void resolve() {
				untapResolve(af, this);
			}
			
		};
		return abUntap;
	}
	
	public static SpellAbility createSpellUntap(final AbilityFactory AF){
		final SpellAbility spUntap = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -4990932993654533449L;
			
			final AbilityFactory af = AF;
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();	
			}
			
			public boolean canPlayAI()
			{
				return untapCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				untapResolve(af, this);
			}
			
		};
		return spUntap;
	}
	
	public static boolean untapCanPlayAI(final AbilityFactory af, SpellAbility sa){
		// AI cannot use this properly until he can use SAs during Humans turn
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		Target tgt = af.getAbTgt();
		Card source = sa.getSourceCard();
		
		Random r = new Random();
		boolean randomReturn = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed());
		
		if (tgt == null){
			if (sa.getSourceCard().isUntapped())
				return false;
		}
		else{
			CardList untapList = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
			untapList = untapList.filter(AllZoneUtil.tapped);
			untapList = untapList.getValidCards(tgt.getValidTgts(), source.getController());
			// filter out enchantments and planeswalkers, their tapped state doesn't matter.
			String[] tappablePermanents = {"Creature", "Land", "Artifact"}; 
			untapList = untapList.getValidCards(tappablePermanents);

			if (untapList.size() == 0)
				return false;
			
			while(tgt.getNumTargeted() < tgt.getMaxTargets()){ 
				Card choice = null;
				
				if (untapList.size() == 0){
					if (tgt.getNumTargeted() < tgt.getMinTargets() || tgt.getNumTargeted() == 0){
						tgt.resetTargets();
						return false;
					}
					else{
						// todo is this good enough? for up to amounts?
						break;
					}
				}
				
				if (untapList.getNotType("Creature").size() == 0)
	        		choice = CardFactoryUtil.AI_getBestCreature(untapList); //if only creatures take the best
	        	else
	        		choice = CardFactoryUtil.AI_getMostExpensivePermanent(untapList, af.getHostCard(), false);
				
				if (choice == null){	// can't find anything left
					if (tgt.getNumTargeted() < tgt.getMinTargets() || tgt.getNumTargeted() == 0){
						tgt.resetTargets();
						return false;
					}
					else{
						// todo is this good enough? for up to amounts?
						break;
					}
				}
				
				untapList.remove(choice);
				tgt.addTarget(choice);
			}
		}
		
		return randomReturn;
	}
	
	public static void untapResolve(final AbilityFactory af, final SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		Card card = sa.getSourceCard();
		
		ArrayList<Card> tgtCards;
		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtCards = tgt.getTargetCards();
		else{
			tgtCards = new ArrayList<Card>();
			tgtCards.add(card);
		}

		for(Card tgtC : tgtCards){
			if (AllZone.GameAction.isCardInPlay(tgtC) && (tgt == null || CardFactoryUtil.canTarget(af.getHostCard(), tgtC)))
				tgtC.untap();
		}

		Card c = tgtCards.get(0);
		
		String DrawBack = params.get("SubAbility");
		if (af.hasSubAbility())
			 CardFactoryUtil.doDrawBack(DrawBack, 0, card.getController(), card.getController().getOpponent(), card.getController(), card, c, sa);

	}
	
	// ****** Tapping ********
	public static SpellAbility createAbilityTap(final AbilityFactory AF){
		final SpellAbility abTap = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 5445572699000471299L;
			
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
			// when getStackDesc is called, just build exactly what is happening
				 StringBuilder sb = new StringBuilder("Tap ");
				 String name = af.getHostCard().getName();
				 Card tgt = getTargetCard();
				 if (tgt != null)
					 sb.append(tgt.getName());
				 else
					 sb.append(name);
				 return sb.toString();
			}
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();		
			}
			
			public boolean canPlayAI()
			{
				return tapCanPlayAI(af,this);
			}
			
			@Override
			public void resolve() {
				tapResolve(af, this);
			}
			
		};
		return abTap;
	}
	
	public static SpellAbility createSpellTap(final AbilityFactory AF){
		final SpellAbility spTap = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -4990932993654533449L;
			
			final AbilityFactory af = AF;
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();	
			}
			
			public boolean canPlayAI()
			{
				return tapCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				tapResolve(af, this);
			}
			
		};
		return spTap;
	}
	
	public static boolean tapCanPlayAI(final AbilityFactory af, SpellAbility sa){
		// AI cannot use this properly until he can use SAs during Humans turn
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		Target tgt = af.getAbTgt();
		Card source = sa.getSourceCard();
		
		Random r = new Random();
		boolean randomReturn = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed());
		
		
		
		if (tgt == null){
			if (sa.getSourceCard().isTapped())
				return false;
		}
		else{
			CardList tapList = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
			tapList = tapList.filter(AllZoneUtil.untapped);
			tapList = tapList.getValidCards(tgt.getValidTgts(), source.getController());
			// filter out enchantments and planeswalkers, their tapped state doesn't matter.
			String[] tappablePermanents = {"Creature", "Land", "Artifact"}; 
			tapList = tapList.getValidCards(tappablePermanents);

			if (tapList.size() == 0)
				return false;
			
			while(tgt.getNumTargeted() < tgt.getMaxTargets()){ 
				Card choice = null;
				
				if (tapList.size() == 0){
					if (tgt.getNumTargeted() < tgt.getMinTargets() || tgt.getNumTargeted() == 0){
						tgt.resetTargets();
						return false;
					}
					else{
						// todo is this good enough? for up to amounts?
						break;
					}
				}
				
				if (tapList.getNotType("Creature").size() == 0)
	        		choice = CardFactoryUtil.AI_getBestCreature(tapList); //if only creatures take the best
	        	else
	        		choice = CardFactoryUtil.AI_getMostExpensivePermanent(tapList, af.getHostCard(), false);
				
				if (choice == null){	// can't find anything left
					if (tgt.getNumTargeted() < tgt.getMinTargets() || tgt.getNumTargeted() == 0){
						tgt.resetTargets();
						return false;
					}
					else{
						// todo is this good enough? for up to amounts?
						break;
					}
				}
				
				tapList.remove(choice);
				tgt.addTarget(choice);
			}
		}
		
		return randomReturn;
	}
	
	public static void tapResolve(final AbilityFactory af, final SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		Card card = sa.getSourceCard();
		
		ArrayList<Card> tgtCards;
		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtCards = tgt.getTargetCards();
		else{
			tgtCards = new ArrayList<Card>();
			tgtCards.add(card);
		}

		for(Card tgtC : tgtCards){
			if (AllZone.GameAction.isCardInPlay(tgtC) && (tgt == null || CardFactoryUtil.canTarget(af.getHostCard(), tgtC)))
				tgtC.tap();
		}
		
		Card c = tgtCards.get(0);
		
		String DrawBack = params.get("SubAbility");
		if (af.hasSubAbility())
			 CardFactoryUtil.doDrawBack(DrawBack, 0, card.getController(), card.getController().getOpponent(), card.getController(), card, c, sa);

	}
	
	// Untap All/Tap All
	
	//Phasing? Something else? Who knows!
}
