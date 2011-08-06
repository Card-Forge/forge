package forge;

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
			untapList = untapList.getTargetableCards(source);
			untapList = untapList.filter(AllZoneUtil.tapped);
			
			if (tgt.doesTarget()){
    			untapList = untapList.getValidCards(tgt.getValidTgts(), source.getController());
    			
    			if (untapList.size() == 0)
    				return false;
    			
				Card c = null;
				CardList dChoices = new CardList();
				String[] Tgts = tgt.getValidTgts();

				for(int i = 0; i < Tgts.length; i++) {
					if (Tgts[i].startsWith("Creature")) {
						c = CardFactoryUtil.AI_getBestCreature(untapList);
						if (c != null)
							dChoices.add(c);
					}

					CardListUtil.sortByTextLen(untapList);
					dChoices.add(untapList.get(0));

					CardListUtil.sortCMC(untapList);
					dChoices.add(untapList.get(0));
				}

				c = dChoices.get(CardUtil.getRandomIndex(dChoices));
				sa.setTargetCard(c);
			}
		}
		
		return randomReturn;
	}
	
	public static void untapResolve(final AbilityFactory af, final SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		Card card = sa.getSourceCard();
		
		Card target = sa.getTargetCard();

		if (af.getAbTgt() == null)
			card.untap();
		else if(AllZone.GameAction.isCardInPlay(target) && CardFactoryUtil.canTarget(card, target))
			target.untap();
		else	// Fizzle?
			return;
		
		String DrawBack = params.get("SubAbility");
		if (af.hasSubAbility())
			 CardFactoryUtil.doDrawBack(DrawBack, 0, card.getController(), card.getController().getOpponent(), card.getController(), card, null, sa);

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
			CardList tapList = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer);
			tapList = tapList.getTargetableCards(source);
			tapList = tapList.filter(AllZoneUtil.untapped);
			
			if (tgt.doesTarget()){
    			tapList = tapList.getValidCards(tgt.getValidTgts(), source.getController());
    			
    			if (tapList.size() == 0)
    				return false;
    			
				Card c = null;
				CardList dChoices = new CardList();
				String[] Tgts = tgt.getValidTgts();

				for(int i = 0; i < Tgts.length; i++) {
					if (Tgts[i].startsWith("Creature")) {
						c = CardFactoryUtil.AI_getBestCreature(tapList);
						if (c != null)
							dChoices.add(c);
					}

					CardListUtil.sortByTextLen(tapList);
					dChoices.add(tapList.get(0));

					CardListUtil.sortCMC(tapList);
					dChoices.add(tapList.get(0));
				}

				c = dChoices.get(CardUtil.getRandomIndex(dChoices));
				sa.setTargetCard(c);
			}
		}
		
		return randomReturn;
	}
	
	public static void tapResolve(final AbilityFactory af, final SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		Card card = sa.getSourceCard();
		
		Card target = sa.getTargetCard();

		if (af.getAbTgt() == null)
			card.tap();
		else if(AllZone.GameAction.isCardInPlay(target) && CardFactoryUtil.canTarget(card, target))
			target.tap();
		else	// Fizzle?
			return;
		
		String DrawBack = params.get("SubAbility");
		if (af.hasSubAbility())
			 CardFactoryUtil.doDrawBack(DrawBack, 0, card.getController(), card.getController().getOpponent(), card.getController(), card, null, sa);

	}
	
	// Untap All/Tap All
	
	//Phasing? Something else? Who knows!
}
